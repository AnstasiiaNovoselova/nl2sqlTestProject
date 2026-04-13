package nure.ua.nl2sqltestproject.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import nure.ua.nl2sqltestproject.client.OpenAiClient;
import nure.ua.nl2sqltestproject.dto.OpenAiDtos;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static nure.ua.nl2sqltestproject.service.MySqlSchemaLoader.TARGET_VIEW;

@Service
public class AiSqlService {

    private static final String FULL_PROMPT_TEMPLATE = """
            You generate SQL queries for an application.
            You must return strictly valid JSON only, without markdown and without extra explanations.

            You are a SQL generator for an application.
            You will receive:
            1) dbType: "mysql"
            2) database schema definition containing exactly one VIEW
            3) a client request in natural language

            Rules:
            - Generate exactly one SELECT statement only.
            - No INSERT, UPDATE, DELETE, DDL, SHOW, DESCRIBE, EXPLAIN.
            - Do not include semicolons.
            - Do not use SELECT *.
            - Always use named parameters like :paramName.
            - Use ONLY the provided VIEW.
            - Never reference base tables directly.
            - The main data source is %s.
            - skin_type is already flattened in the VIEW through:
              - skin_type_ids
              - skin_type_names
            - Return columns only as:
              - id
              - name
              - price
            - If the client did not ask for all rows, add LIMIT 200.
            - Return strictly valid JSON only, without markdown.

            Response JSON format:
            {
              "sql": "...",
              "params": { "paramName": 123 },
              "resultColumns": ["id", "name", "price"],
              "notes": "optional"
            }

            If you cannot generate SQL, return:
            {
              "sql": null,
              "params": {},
              "resultColumns": [],
              "notes": "reason"
            }
            """;

    private final OpenAiClient openAiClient;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper om;
    private final SchemaLoader schemaLoader;

    public AiSqlService(OpenAiClient openAiClient,
                        NamedParameterJdbcTemplate jdbc,
                        SchemaLoader schemaLoader) {
        this.openAiClient = openAiClient;
        this.jdbc = jdbc;
        this.om = new ObjectMapper();
        this.schemaLoader = schemaLoader;
    }

    public List<Map<String, Object>> runClientQuery(String clientQuery) throws Exception {
        String schema = schemaLoader.loadSchemaDefinition();
        String fullPrompt = FULL_PROMPT_TEMPLATE.formatted(TARGET_VIEW);

        String userInputJson = om.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                "dbType", "mysql",
                "ddl", schema,
                "clientQuery", clientQuery
        ));

        System.out.println("\n========== FULL PROMPT ==========");
        System.out.println(fullPrompt + "\n\n" + userInputJson);

        OpenAiDtos.SqlGenResponse response = openAiClient.createJsonResponse(fullPrompt, userInputJson);

        if (response == null) {
            throw new IllegalStateException("OpenAI response is empty");
        }

        System.out.println("\n========== RAW MODEL RESPONSE ==========");
        System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(response));

        String sql = response.sql();
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("OpenAI returned no sql");
        }

        validateSql(sql);

        Map<String, Object> paramMap = response.params() == null ? Map.of() : response.params();
        MapSqlParameterSource params = new MapSqlParameterSource(paramMap);

        System.out.println("\n========== SQL WITH NAMED PARAMS ==========");
        System.out.println(sql);

        System.out.println("\n========== SQL PARAMS ==========");
        System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(paramMap));

        System.out.println("\n========== RENDERED SQL ==========");
        System.out.println(renderSqlForDebug(sql, paramMap));

        return jdbc.query(sql, params, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getObject("id"));
            row.put("name", rs.getObject("name"));
            row.put("price", rs.getObject("price"));
            return row;
        });
    }

    private static void validateSql(String sql) {
        String normalized = sql.trim().toLowerCase().replaceAll("\\s+", " ");

        if (normalized.contains(";")) {
            throw new IllegalArgumentException("SQL must not contain ';'");
        }

        if (!normalized.startsWith("select")) {
            throw new IllegalArgumentException("Only SELECT is allowed");
        }

        String[] banned = {
                "insert", "update", "delete", "drop", "alter", "create",
                "truncate", "grant", "revoke", "show", "describe", "explain"
        };

        for (String b : banned) {
            if (normalized.matches(".*\\b" + b + "\\b.*")) {
                throw new IllegalArgumentException("Banned keyword detected: " + b);
            }
        }

        if (normalized.contains("select *")) {
            throw new IllegalArgumentException("SELECT * is not allowed");
        }

        if (!normalized.matches(".*\\b" + TARGET_VIEW.toLowerCase() + "\\b.*")) {
            throw new IllegalArgumentException("SQL must use only " + TARGET_VIEW);
        }

        String[] bannedTables = {
                "cosmetic_product",
                "brand",
                "country",
                "cosmetic_class",
                "cosmetic_age_category",
                "gender",
                "season",
                "application_time",
                "skin_sensitivity",
                "climate_temperature",
                "climate_humidity",
                "acne_tendency",
                "product_skin_type",
                "skin_type",
                "product_texture"
        };

        for (String t : bannedTables) {
            if (normalized.matches(".*\\b" + t + "\\b.*")) {
                throw new IllegalArgumentException("Base table usage is forbidden: " + t);
            }
        }
    }

    private static String renderSqlForDebug(String sql, Map<String, Object> params) {
        String rendered = sql;

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = ":" + entry.getKey();
            String replacement = toSqlLiteral(entry.getValue());
            rendered = rendered.replace(placeholder, replacement);
        }

        return rendered;
    }

    private static String toSqlLiteral(Object value) {
        switch (value) {
            case null -> {
                return "null";
            }
            case Number number -> {
                return value.toString();
            }
            case Boolean b -> {
                return b ? "true" : "false";
            }
            default -> {
            }
        }

        if (value instanceof LocalDate || value instanceof LocalDateTime || value instanceof Timestamp) {
            return "'" + value + "'";
        }

        String text = String.valueOf(value).replace("'", "''");
        return "'" + text + "'";
    }
}