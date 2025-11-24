package com.example.demo.service;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final CacheManager cacheManager;
    private static final String CACHE_NAME = "emailAuthCodes";

    private Cache getCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            throw new IllegalStateException("'" + CACHE_NAME + "' 캐시가 설정되지 않았습니다.");
        }
        return cache;
    }

    /**
     * [저장] 이메일(Key)을 소문자/공백 제거 후 캐시에 저장해요.
     */
    public void saveCode(String email, String code) {
        Cache cache = getCache();
        // ⭐️ 키 불일치 방지를 위해 .trim().toLowerCase() 적용
        cache.put(email.trim().toLowerCase(), code);
    }

    /**
     * [조회] 이메일(Key)을 소문자/공백 제거 후 캐시에서 인증 코드(Value)를 불러와요.
     */
    public String getCode(String email) {
        Cache cache = getCache();
        // ⭐️ 조회 시: 저장된 키를 찾기 위해 동일하게 .trim().toLowerCase() 적용
        return cache.get(email.trim().toLowerCase(), String.class);
    }

    public boolean verifyCode(String email, String code) {
        // getCode() 내부에서 키를 처리하므로, 저장된 코드와 정확히 비교 가능
        String savedCode = this.getCode(email);

        if (savedCode == null) {
            return false; // 코드가 없거나 만료됨
        }

        // 저장된 코드와 사용자가 입력한 코드를 비교
        return savedCode.equals(code);
    }

    /**
     * [삭제] 사용이 끝난 인증 코드를 캐시에서 즉시 삭제해요.
     */
    public void deleteCode(String email) {
        Cache cache = getCache();
        cache.evict(email.trim().toLowerCase());
    }
}