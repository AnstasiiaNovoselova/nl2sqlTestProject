package nure.ua.nl2sqltestproject.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import nure.ua.nl2sqltestproject.client.OpenAiClient;
import nure.ua.nl2sqltestproject.dto.CriteriaSqlDebugResponse;
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
public class CriteriaSearchService {

    private final OpenAiClient openAiClient;
    private final NamedParameterJdbcTemplate jdbc;
    private final SchemaLoader schemaLoader;
    private final CriteriaDictionary criteriaDictionary;
    private final CriteriaPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public CriteriaSearchService(OpenAiClient openAiClient,
                                 NamedParameterJdbcTemplate jdbc,
                                 SchemaLoader schemaLoader,
                                 CriteriaDictionary criteriaDictionary,
                                 CriteriaPromptBuilder promptBuilder) {
        this.openAiClient = openAiClient;
        this.jdbc = jdbc;
        this.schemaLoader = schemaLoader;
        this.criteriaDictionary = criteriaDictionary;
        this.promptBuilder = promptBuilder;
        this.objectMapper = new ObjectMapper();
    }

    public CriteriaSqlDebugResponse searchByCriteria(List<String> selectedValues) throws Exception {
        String viewDefinition = schemaLoader.loadSchemaDefinition();
        String selectedCriteriaText = criteriaDictionary.toPromptConditionsText(selectedValues);
        String fullPrompt = promptBuilder.buildPrompt(viewDefinition, selectedCriteriaText);

        System.out.println("\n========== FULL CRITERIA PROMPT ==========");
        System.out.println(fullPrompt);

        OpenAiDtos.SqlGenResponse response = openAiClient.createJsonResponse(fullPrompt, "{}");

        if (response == null || response.sql() == null || response.sql().isBlank()) {
            throw new IllegalArgumentException("OpenAI returned no sql");
        }

        String sql = response.sql();
        validateSql(sql);

        Map<String, Object> paramMap = response.params() == null ? Map.of() : response.params();
        String renderedSql = renderSqlForDebug(sql, paramMap);

        System.out.println("\n========== GENERATED SQL ==========");
        System.out.println(sql);

        System.out.println("\n========== SQL PARAMS ==========");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(paramMap));

        System.out.println("\n========== RENDERED SQL ==========");
        System.out.println(renderedSql);

        List<Map<String, Object>> rows = jdbc.query(sql, new MapSqlParameterSource(paramMap), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getObject("id"));
            row.put("name", rs.getObject("name"));
            row.put("price", rs.getObject("price"));
            return row;
        });

        return new CriteriaSqlDebugResponse(
                fullPrompt,
                sql,
                renderedSql,
                paramMap,
                rows
        );
    }

    private static void validateSql(String sql) {
        String normalized = sql.trim().toLowerCase().replaceAll("\\s+", " ");

        if (normalized.contains(";")) {
            throw new IllegalArgumentException("SQL must not contain ';'");
        }

        if (!normalized.startsWith("select")) {
            throw new IllegalArgumentException("Only SELECT is allowed");
        }

        if (normalized.contains("select *")) {
            throw new IllegalArgumentException("SELECT * is not allowed");
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

        if (!normalized.matches(".*\\b" + TARGET_VIEW.toLowerCase() + "\\b.*")) {
            throw new IllegalArgumentException("SQL must use only " + TARGET_VIEW);
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
        if (value == null) {
            return "null";
        }

        if (value instanceof Number) {
            return value.toString();
        }

        if (value instanceof Boolean b) {
            return b ? "true" : "false";
        }

        if (value instanceof LocalDate || value instanceof LocalDateTime || value instanceof Timestamp) {
            return "'" + value + "'";
        }

        String text = String.valueOf(value).replace("'", "''");
        return "'" + text + "'";
    }
}