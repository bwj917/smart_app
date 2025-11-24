package com.example.demo.repository;

import com.example.demo.domain.Member;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcTemplateMemberRepository implements MemberRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final String TABLE_NAME = "USERS";
    private static final String SEQUENCE_NAME = "USERS_SEQ";

    public JdbcTemplateMemberRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // ... (save 메서드는 기존 유지, INSERT 시 POINTS는 default 0으로 들어감) ...
    @Override
    public void save(Member member) {
        // 기존 코드 유지 (INSERT 문에 POINTS 명시 안 해도 DB Default값 0 적용됨)
        String sql = "INSERT INTO " + TABLE_NAME + " (ID, USER_ID, PW, NAME, EMAIL, PHONE, JOIN_DATE) " +
                "VALUES (" + SEQUENCE_NAME + ".NEXTVAL, ?, ?, ?, ?, ?, SYSDATE)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"ID"});
            ps.setString(1, member.getUserid());
            ps.setString(2, member.getPw());
            ps.setString(3, member.getName());
            ps.setString(4, member.getEmail());
            ps.setString(5, member.getPhone());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            member.setId(keyHolder.getKey().longValue());
        }
    }



    @Override
    public Optional<Member> findByUserName(String name) {
        try {
            Member member = jdbcTemplate.queryForObject("SELECT * FROM " + TABLE_NAME + " WHERE NAME = ?", memberRowMapper(), name);
            return Optional.ofNullable(member);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        try {
            Member member = jdbcTemplate.queryForObject("SELECT * FROM " + TABLE_NAME + " WHERE EMAIL = ?", memberRowMapper(), email);
            return Optional.ofNullable(member);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    @Override
    public List<Member> findAll() {
        return jdbcTemplate.query("SELECT * FROM " + TABLE_NAME, memberRowMapper());
    }



    private RowMapper<Member> memberRowMapper() {
        return (rs, rowNum) -> {
            Member member = new Member();
            member.setId(rs.getLong("ID"));
            member.setUserid(rs.getString("USER_ID"));
            member.setPw(rs.getString("PW"));
            member.setName(rs.getString("NAME"));
            member.setEmail(rs.getString("EMAIL"));
            member.setPhone(rs.getString("PHONE"));
            member.setJoinDate(rs.getTimestamp("JOIN_DATE").toLocalDateTime());

            // 🔥 [추가] DB에서 포인트 값 가져와서 매핑
            member.setPoints(rs.getInt("POINTS"));
            return member;
        };
    }

    // 🔥 [추가 1] ID(Long)로 회원 찾기 구현
    @Override
    public Optional<Member> findByUserId(String user_id) { // ✅ 수정 완료: User
        try {
            Member member = jdbcTemplate.queryForObject("SELECT * FROM " + TABLE_NAME + " WHERE USER_ID = ?", memberRowMapper(), user_id);
            return Optional.ofNullable(member);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // 🔥 [추가 2] 포인트 업데이트 구현
    // (MemberRepository 인터페이스에 updatePoints를 추가하지 않았다면 이 부분은 빼셔도 됩니다)
    @Override
    public void updatePoints(Long id, int newPoints) {
        String sql = "UPDATE USERS SET POINTS = ? WHERE ID = ?";
        jdbcTemplate.update(sql, newPoints, id);
    }

    @Override
    public Optional<Member> findById(long id) {
        try {
            // USERS 테이블의 PK인 'ID' 컬럼으로 조회
            String sql = "SELECT * FROM USERS WHERE ID = ?";
            Member member = jdbcTemplate.queryForObject(sql, memberRowMapper(), id);
            return Optional.ofNullable(member);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

}