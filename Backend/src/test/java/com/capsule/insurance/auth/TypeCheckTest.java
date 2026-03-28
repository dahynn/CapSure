package com.capsule.insurance.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class TypeCheckTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void findEnumType() {
        String sql = "SELECT n.nspname as schema, t.typname as name " +
                     "FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace " +
                     "WHERE t.typtype = 'e'";
        List<Map<String, Object>> types = jdbcTemplate.queryForList(sql);
        System.out.println("ENUM_TYPES_START");
        for (Map<String, Object> type : types) {
            System.out.println("TYPE:" + type.get("schema") + "." + type.get("name"));
        }
        System.out.println("ENUM_TYPES_END");
    }
}
