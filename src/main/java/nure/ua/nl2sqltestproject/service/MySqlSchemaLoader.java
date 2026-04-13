package nure.ua.nl2sqltestproject.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MySqlSchemaLoader implements SchemaLoader {

    public static final String TARGET_VIEW = "cosmetic_product_view";
    private static final String DB_NAME = "cosmetic_shop_ontology";

    private final JdbcTemplate jdbc;

    public MySqlSchemaLoader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String loadSchemaDefinition() {
        List<ViewColumn> columns = jdbc.query("""
                select c.table_name,
                       c.column_name,
                       c.column_type,
                       c.is_nullable,
                       c.ordinal_position
                from information_schema.columns c
                join information_schema.tables t
                  on t.table_schema = c.table_schema
                 and t.table_name = c.table_name
                where c.table_schema = ?
                  and t.table_type = 'VIEW'
                  and c.table_name = ?
                order by c.ordinal_position
                """,
                (rs, rowNum) -> new ViewColumn(
                        rs.getString("table_name"),
                        rs.getString("column_name"),
                        rs.getString("column_type"),
                        "YES".equalsIgnoreCase(rs.getString("is_nullable")),
                        rs.getInt("ordinal_position")
                ),
                DB_NAME,
                TARGET_VIEW
        );

        if (columns.isEmpty()) {
            throw new IllegalStateException("View not found: " + TARGET_VIEW);
        }

        StringBuilder sb = new StringBuilder(4096);
        sb.append("-- DBMS: MySQL\n");
        sb.append("-- Database: ").append(DB_NAME).append("\n");
        sb.append("-- Use only this VIEW, do not use base tables.\n\n");

        sb.append("VIEW ").append(TARGET_VIEW).append(" (\n");

        for (int i = 0; i < columns.size(); i++) {
            ViewColumn c = columns.get(i);
            sb.append("  ")
                    .append(c.columnName())
                    .append(" ")
                    .append(c.columnType());

            if (!c.nullable()) {
                sb.append(" NOT NULL");
            }

            if (i < columns.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append(");\n");

        return sb.toString();
    }

    private record ViewColumn(
            String viewName,
            String columnName,
            String columnType,
            boolean nullable,
            int ordinalPosition
    ) {}
}