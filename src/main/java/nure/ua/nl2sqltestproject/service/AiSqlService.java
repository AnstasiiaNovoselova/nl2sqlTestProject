package nure.ua.nl2sqltestproject.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import nure.ua.nl2sqltestproject.client.OpenAiClient;
import nure.ua.nl2sqltestproject.dto.OpenAiDtos;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static nure.ua.nl2sqltestproject.service.MySqlSchemaLoader.TARGET_VIEW;

@Service
public class AiSqlService {

    private final OpenAiClient openAiClient;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper om;
    private final SchemaLoader schemaLoader;
    private final PromptSupportService promptSupportService;

    public AiSqlService(OpenAiClient openAiClient,
                        NamedParameterJdbcTemplate jdbc,
                        SchemaLoader schemaLoader,
                        PromptSupportService promptSupportService) {
        this.openAiClient = openAiClient;
        this.jdbc = jdbc;
        this.om = new ObjectMapper();
        this.schemaLoader = schemaLoader;
        this.promptSupportService = promptSupportService;
    }

    public List<Map<String, Object>> runClientQuery(String clientQuery) throws Exception {
        String viewDdl = schemaLoader.loadSchemaDefinition();
        String fullPrompt = promptSupportService.buildTextQueryPrompt(viewDdl);

        String userInputJson = om.writeValueAsString(Map.of(
                "clientQuery", clientQuery
        ));

        System.out.println("\n========== FULL PROMPT ==========");
        System.out.println(fullPrompt + " INPUT: " + userInputJson);

        OpenAiDtos.SqlGenResponse response = openAiClient.createJsonResponse(fullPrompt, userInputJson);

        String sql = response.sql();
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("OpenAI returned no sql");
        }

        validateSql(sql);

        Map<String, Object> paramMap = response.params() == null ? Map.of() : response.params();
        MapSqlParameterSource params = new MapSqlParameterSource(paramMap);

        System.out.println("\n========== GENERATED SQL ==========");
        System.out.println(sql);

        System.out.println("\n========== RENDERED SQL ==========");
        System.out.println(renderSqlForDebug(sql, paramMap));

        return jdbc.query(sql, params, (rs, rowNum) -> {
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= count; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
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
        if (!normalized.matches(".*\\b" + TARGET_VIEW.toLowerCase() + "\\b.*")) {
            throw new IllegalArgumentException("SQL must use only " + TARGET_VIEW);
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
    }

    private static String renderSqlForDebug(String sql, Map<String, Object> params) {
        String rendered = sql;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            rendered = rendered.replace(":" + entry.getKey(), toSqlLiteral(entry.getValue()));
        }
        return rendered;
    }

    private static String toSqlLiteral(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof BigDecimal) return value.toString();
        if (value instanceof Boolean b) return b ? "true" : "false";
        if (value instanceof LocalDate || value instanceof LocalDateTime || value instanceof Timestamp) {
            return "'" + value + "'";
        }
        return "'" + String.valueOf(value).replace("'", "''") + "'";
    }
}