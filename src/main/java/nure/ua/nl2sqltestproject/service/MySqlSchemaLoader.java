package nure.ua.nl2sqltestproject.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MySqlSchemaLoader implements SchemaLoader {

    public static final String TARGET_VIEW = "cosmetic_product_view";

    private final JdbcTemplate jdbc;

    public MySqlSchemaLoader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String loadSchemaDefinition() {
        return jdbc.queryForObject(
                "SHOW CREATE VIEW " + TARGET_VIEW,
                (rs, rowNum) -> rs.getString("Create View")
        );
    }
}