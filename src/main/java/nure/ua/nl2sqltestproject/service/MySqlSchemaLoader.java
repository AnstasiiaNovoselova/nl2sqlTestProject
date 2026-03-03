package nure.ua.nl2sqltestproject.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MySqlSchemaLoader implements SchemaLoader {

    private final JdbcTemplate jdbc;

    public MySqlSchemaLoader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String loadSchemaDefinition() {
        String dbName = jdbc.queryForObject("select database()", String.class);
        if (dbName == null || dbName.isBlank()) {
            throw new IllegalStateException("Unable to resolve current database()");
        }

        List<TableColumn> cols = jdbc.query("""
                select table_name, column_name, column_type, is_nullable, ordinal_position
                from information_schema.columns
                where table_schema = ?
                order by table_name, ordinal_position
                """, (rs, rowNum) -> new TableColumn(
                rs.getString("table_name"),
                rs.getString("column_name"),
                rs.getString("column_type"),
                "YES".equalsIgnoreCase(rs.getString("is_nullable")),
                rs.getInt("ordinal_position")
        ), dbName);

        Set<String> tables = new LinkedHashSet<>();
        for (TableColumn c : cols) tables.add(c.tableName());

        Map<String, List<String>> pkByTable = loadPrimaryKeys(dbName);
        List<ForeignKey> fks = loadForeignKeys(dbName);

        StringBuilder sb = new StringBuilder(16_384);
        sb.append("-- DBMS: MySQL\n");
        sb.append("-- Database: ").append(dbName).append("\n\n");

        Map<String, List<TableColumn>> colsByTable = new LinkedHashMap<>();
        for (String t : tables) colsByTable.put(t, new ArrayList<>());
        for (TableColumn c : cols) colsByTable.get(c.tableName()).add(c);

        for (String table : tables) {
            sb.append("TABLE ").append(table).append(" (\n");

            List<TableColumn> tableCols = colsByTable.getOrDefault(table, List.of());
            for (int i = 0; i < tableCols.size(); i++) {
                TableColumn c = tableCols.get(i);
                sb.append("  ").append(c.columnName()).append(" ").append(c.columnType());
                if (!c.nullable()) sb.append(" NOT NULL");
                if (i < tableCols.size() - 1) sb.append(",");
                sb.append("\n");
            }

            List<String> pkCols = pkByTable.getOrDefault(table, List.of());
            if (!pkCols.isEmpty()) {
                sb.append("  ,PRIMARY KEY (").append(String.join(", ", pkCols)).append(")\n");
            }

            for (ForeignKey fk : fks) {
                if (fk.tableName().equals(table)) {
                    sb.append("  ,FOREIGN KEY (").append(String.join(", ", fk.columns())).append(")")
                            .append(" REFERENCES ").append(fk.refTable()).append(" (").append(String.join(", ", fk.refColumns())).append(")\n");
                }
            }

            sb.append(");\n\n");
        }

        return sb.toString();
    }

    private Map<String, List<String>> loadPrimaryKeys(String dbName) {
        List<Map.Entry<String, String>> rows = jdbc.query("""
                select table_name, column_name, ordinal_position
                from information_schema.key_column_usage
                where table_schema = ?
                  and constraint_name = 'PRIMARY'
                order by table_name, ordinal_position
                """, (rs, rowNum) -> Map.entry(rs.getString("table_name"), rs.getString("column_name")), dbName);

        Map<String, List<String>> pkByTable = new LinkedHashMap<>();
        for (var e : rows) {
            pkByTable.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue());
        }
        return pkByTable;
    }

    private List<ForeignKey> loadForeignKeys(String dbName) {
        return jdbc.query("""
                select
                  table_name,
                  column_name,
                  referenced_table_name,
                  referenced_column_name,
                  constraint_name,
                  ordinal_position
                from information_schema.key_column_usage
                where table_schema = ?
                  and referenced_table_name is not null
                order by table_name, constraint_name, ordinal_position
                """, rs -> {
            record Key(String table, String constraint) {}
            Map<Key, ForeignKeyBuilder> map = new LinkedHashMap<>();
            while (rs.next()) {
                String table = rs.getString("table_name");
                String constraint = rs.getString("constraint_name");
                String col = rs.getString("column_name");
                String refTable = rs.getString("referenced_table_name");
                String refCol = rs.getString("referenced_column_name");

                Key key = new Key(table, constraint);
                ForeignKeyBuilder b = map.computeIfAbsent(key, k -> new ForeignKeyBuilder(table, refTable));
                b.columns.add(col);
                b.refColumns.add(refCol);
            }
            return map.values().stream().map(ForeignKeyBuilder::build).toList();
        }, dbName);
    }

    private record TableColumn(String tableName, String columnName, String columnType, boolean nullable, int ordinal) {}

    private record ForeignKey(String tableName, List<String> columns, String refTable, List<String> refColumns) {}

    private static class ForeignKeyBuilder {
        final String tableName;
        final String refTable;
        final List<String> columns = new ArrayList<>();
        final List<String> refColumns = new ArrayList<>();

        ForeignKeyBuilder(String tableName, String refTable) {
            this.tableName = tableName;
            this.refTable = refTable;
        }

        ForeignKey build() {
            return new ForeignKey(tableName, List.copyOf(columns), refTable, List.copyOf(refColumns));
        }
    }
}
