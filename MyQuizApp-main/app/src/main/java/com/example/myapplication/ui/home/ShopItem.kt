package com.example.myapplication.ui.home

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.util.CharacterManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 상점 아이템 데이터
data class ShopItem(val imageRes: Int, val price: Int, val index: Int, var isOwned: Boolean, var isSelected: Boolean)

class ShopDialog(
    private val context: Context,
    private val userId: Long,
    private val currentPoints: Int,
    private val ownedIndices: List<Int>, // 서버에서 받은 소유 목록
    private val selectedIndex: Int,      // 현재 장착 중인 번호
    private val onCharacterChanged: (Int, Int) -> Unit // 콜백
) {
    private val dialog = Dialog(context)

    // 🔥 [수정] CharacterManager를 사용해 대표 이미지 가져오기
    private val allCharacters = listOf(
        // 0번: 펭귄 (기본 지급)
        ShopItem(CharacterManager.getPreviewImage(0), 0, 0, false, false),

        // 1번: 토끼 (1000포인트) -> 여기 뒤에 쉼표(,)가 빠져 있었습니다!
        ShopItem(CharacterManager.getPreviewImage(1), 1000, 1, false, false),

        // 2번: 판다 (2000포인트)
        ShopItem(CharacterManager.getPreviewImage(2), 2000, 2, false, false)
    )

    fun show() {
        dialog.setContentView(R.layout.dialog_shop)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 🔥 다이얼로그가 켜질 때 '소유 여부'와 '장착 여부'를 갱신
        allCharacters.forEach {
            it.isOwned = ownedIndices.contains(it.index) // 서버 목록에 있으면 소유 중
            it.isSelected = (it.index == selectedIndex)  // 현재 번호와 같으면 장착 중
        }

        val rvShop = dialog.findViewById<RecyclerView>(R.id.rvShop)
        val tvMyPoints = dialog.findViewById<TextView>(R.id.tvShopPoints)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btnCloseShop)

        tvMyPoints.text = "보유 포인트: $currentPoints P"

        rvShop.layoutManager = GridLayoutManager(context, 2)
        rvShop.adapter = ShopAdapter(allCharacters, currentPoints)

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // 🔥 [핵심 수정] 어댑터 로직
    inner class ShopAdapter(val items: List<ShopItem>, var myPoints: Int) : RecyclerView.Adapter<ShopAdapter.Holder>() {

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val img = itemView.findViewById<ImageView>(R.id.itemImage)
            val btnAction = itemView.findViewById<MaterialButton>(R.id.itemBtnAction)
            val tvPrice = itemView.findViewById<TextView>(R.id.itemPrice)

            fun bind(item: ShopItem) {
                img.setImageResource(item.imageRes)

                // ------------------------------------------------------------
                // 🔥 상태에 따른 버튼 UI 분기 처리
                // ------------------------------------------------------------
                if (item.isSelected) {
                    // [상태 1] 현재 장착 중인 캐릭터
                    btnAction.text = "장착중"
                    btnAction.isEnabled = false // 이미 장착했으니 클릭 불가
                    btnAction.setBackgroundColor(Color.GRAY)
                    tvPrice.visibility = View.GONE // 가격 숨김
                }
                else if (item.isOwned) {
                    // [상태 2] 구매는 했지만, 장착은 안 한 캐릭터 -> '장착하기' 버튼 노출
                    btnAction.text = "장착하기"
                    btnAction.isEnabled = true
                    btnAction.setBackgroundColor(Color.parseColor("#57419d")) // 브랜드 보라색
                    tvPrice.visibility = View.GONE // 이미 샀으니 가격 숨김

                    // 클릭 시 -> 장착 완료 처리
                    btnAction.setOnClickListener {
                        onCharacterChanged(item.index, myPoints) // 콜백 호출
                        dialog.dismiss() // 창 닫기
                    }
                }
                else {
                    // [상태 3] 구매하지 않은 캐릭터 -> '구매' 버튼 노출
                    btnAction.text = "구매"

                    // 돈이 부족하면 버튼 비활성화
                    val canBuy = myPoints >= item.price
                    btnAction.isEnabled = canBuy
                    btnAction.setBackgroundColor(if (canBuy) Color.parseColor("#FF5252") else Color.LTGRAY)

                    tvPrice.visibility = View.VISIBLE
                    tvPrice.text = "${item.price} P"

                    // 클릭 시 -> 구매 로직 실행
                    btnAction.setOnClickListener {
                        buy(item)
                    }
                }
            }

            private fun buy(item: ShopItem) {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            RetrofitClient.problemApiService.buyCharacter(userId, item.index, item.price)
                        }

                        if (response.isSuccessful) {
                            val body = response.body()
                            val success = body?.get("success") as? Boolean ?: false

                            if (success) {
                                // [정상 구매 성공]
                                Toast.makeText(context, "구매 성공!", Toast.LENGTH_SHORT).show()
                                val newPoints = (body?.get("newPoints") as? Number)?.toInt() ?: 0

                                // UI 갱신
                                myPoints = newPoints
                                item.isOwned = true
                                notifyDataSetChanged() // 버튼 상태 변경 (구매 -> 장착하기)

                                dialog.findViewById<TextView>(R.id.tvShopPoints).text = "보유 포인트: $myPoints P"
                            } else {
                                // [구매 실패] (포인트 부족, 이미 보유 등)
                                val msg = body?.get("message") as? String ?: "구매 실패"

                                // 🔥 [핵심 수정] "이미 보유" 메시지가 오면 -> 소유 상태로 강제 변경!
                                if (msg.contains("이미 보유")) {
                                    item.isOwned = true
                                    notifyDataSetChanged() // 버튼을 '장착하기'로 즉시 변경
                                    Toast.makeText(context, "이미 보유 중인 캐릭터입니다.", Toast.LENGTH_SHORT).show()
                                } else {
                                    // 포인트 부족 등 다른 에러
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "네트워크 오류", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_shop_character, parent, false)
            return Holder(view)
        }
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    }
}