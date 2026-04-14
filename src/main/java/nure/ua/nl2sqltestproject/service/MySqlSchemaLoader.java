package nure.ua.nl2sqltestproject.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MySqlSchemaLoader implements SchemaLoader {

    public static final String TARGET_VIEW = "cosmetic_product_view";

    private final JdbcTemplate jdbc;

    public MySqlSchemaLoader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String loadSchemaDefinition() {
        String dbName = jdbc.queryForObject("select database()", String.class);

        if (dbName == null || dbName.isBlank()) {
            throw new IllegalStateException("Cannot determine database name");
        }

        List<ViewColumn> columns = jdbc.query("""
                SELECT column_name,
                       column_type,
                       is_nullable,
                       ordinal_position
                FROM information_schema.columns
                WHERE table_schema = ?
                  AND table_name = ?
                ORDER BY ordinal_position
                """,
                (rs, rowNum) -> new ViewColumn(
                        rs.getString("column_name"),
                        rs.getString("column_type"),
                        "YES".equalsIgnoreCase(rs.getString("is_nullable"))
                ),
                dbName,
                TARGET_VIEW
        );

        if (columns.isEmpty()) {
            throw new IllegalStateException("View not found: " + TARGET_VIEW);
        }

        StringBuilder sb = new StringBuilder(2048);

        sb.append("VIEW ").append(TARGET_VIEW).append(" (\n");

        for (int i = 0; i < columns.size(); i++) {
            ViewColumn c = columns.get(i);

            sb.append("  ")
                    .append(c.name)
                    .append(" ")
                    .append(c.type);

            if (!c.nullable) {
                sb.append(" NOT NULL");
            }

            if (i < columns.size() - 1) {
                sb.append(",");
            }

            sb.append("\n");
        }

        sb.append(")");

        return sb.toString();
    }

    private record ViewColumn(
            String name,
            String type,
            boolean nullable
    ) {}
}