package nure.ua.nl2sqltestproject.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import nure.ua.nl2sqltestproject.client.OpenAiClient;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class AiSqlService {

    private static final String SYSTEM_INSTRUCTIONS = """
            You generate SQL queries for an application.
            You must return strictly valid JSON only, without markdown and without extra explanations.
            """;

    private static final String PROMPT_RULES = """
            You are a SQL generator for an application. You will receive:
            1) dbType: "postgres" or "mysql"
            2) database schema definition (tables, columns, keys)
            3) a client request in natural language

            Rules:
            - Generate exactly one SELECT statement only. No INSERT/UPDATE/DELETE/DDL.
            - Do not include semicolons. No multiple statements.
            - Never use SELECT *; return only required columns.
            - Always use named parameters like :paramName (no string concatenation).
            - If the client did not ask for all rows, add LIMIT 200.
            - Return strictly valid JSON only, without markdown.

            Response JSON format:
            {
              "sql": "...",
              "params": { "paramName": 123 },
              "resultColumns": ["id","name","price"],
              "notes": "optional"
            }
            """;

    private final OpenAiClient openAiClient;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper om;
    private final MySqlSchemaLoader schemaLoader;

    public AiSqlService(OpenAiClient openAiClient, NamedParameterJdbcTemplate jdbc, MySqlSchemaLoader schemaLoader) {
        this.openAiClient = openAiClient;
        this.jdbc = jdbc;
        this.om = new ObjectMapper();
        this.schemaLoader = schemaLoader;
    }

    public List<Map<String, Object>> runClientQuery(String clientQuery) throws Exception {
        String schema = schemaLoader.loadSchemaDefinition();

        var userInputJson = om.writeValueAsString(Map.of(
                "dbType", "mysql",
                "ddl", schema,
                "clientQuery", clientQuery
        ));

        var rawJson = openAiClient.createJsonResponse(SYSTEM_INSTRUCTIONS, PROMPT_RULES
                + "\n\nINPUT:\n" + userInputJson);

        JsonNode root = om.readTree(rawJson);
        String sql = root.path("sql").asText(null);

        if (sql == null) throw new IllegalArgumentException("OpenAI returned no sql");

        validateSql(sql);

        MapSqlParameterSource params = new MapSqlParameterSource();
        JsonNode p = root.path("params");
        if (p.isObject()) {
            var names = p.fieldNames();
            while (names.hasNext()) {
                String k = names.next();
                JsonNode v = p.get(k);

                if (v == null || v.isNull()) params.addValue(k, null);
                else if (v.isNumber()) params.addValue(k, v.numberValue());
                else if (v.isBoolean()) params.addValue(k, v.booleanValue());
                else if (v.isTextual()) params.addValue(k, v.asText());
                else params.addValue(k, v.toString());
            }
        }
//        if (p.isObject()) {
//            for (JsonNode jsonNode : p) {
//                String k = jsonNode.asString();
//                JsonNode v = p.get(k);
//                // прототип: кладём как есть (число/строка/boolean)
//                if (v.isNumber()) params.addValue(k, v.numberValue());
//                else if (v.isBoolean()) params.addValue(k, v.booleanValue());
//                else if (v.isString()) params.addValue(k, v.asString());
//                else params.addValue(k, v.toString());
//            }
//        }

        System.out.println("SQL:");
        System.out.println(sql);
        System.out.println("PARAMS:");
        System.out.println(om.writeValueAsString(p));

        // Возвращаем список Map – Spring сам сериализует в JSON
        return jdbc.query(sql, params, (rs, rowNum) -> {
            // Достаём именно id/name/price если такие колонки в SQL
            var row = new java.util.LinkedHashMap<String, Object>();
            row.put("id", rs.getObject("id"));
            row.put("name", rs.getObject("name"));
            row.put("price", rs.getObject("price"));
            return row;
        });
    }

    private static void validateSql(String sql) {
        String s = sql.trim().toLowerCase();

        // 1) один statement – без ';'
        if (s.contains(";")) throw new IllegalArgumentException("SQL must not contain ';'");

        // 2) только select
        if (!s.startsWith("select")) throw new IllegalArgumentException("Only SELECT is allowed");

        // 3) грубые стоп-слова (прототип)
        String[] banned = {"insert", "update", "delete", "drop", "alter", "create", "truncate", "grant", "revoke"};
        for (String b : banned) {
            if (s.contains(b)) throw new IllegalArgumentException("Banned keyword detected: " + b);
        }

        // 4) запрет SELECT *
        if (s.contains("select *")) throw new IllegalArgumentException("SELECT * is not allowed");
    }
}
