package nure.ua.nl2sqltestproject.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptSupportService {

    private static final String ROLE_PREFIX =
            "You are a professional expert in generating safe MySQL SELECT queries for cosmetic product search. ";

    private static final String COMMON_REQUIREMENTS =
            "Generate exactly one valid SQL SELECT query only for the provided VIEW. " +
                    "Use only VIEW %s. " +
                    "Do not use base tables, INSERT, UPDATE, DELETE, DDL, SHOW, DESCRIBE, EXPLAIN, or semicolon. " +
                    "Use named parameters like :paramName. " +
                    "Select all VIEW columns explicitly, not SELECT *. " +
                    "Return strict JSON only without markdown. " +
                    "Return all selected VIEW columns in resultColumns. ";

    private static final String RESPONSE_FORMAT =
            "Response format: " +
                    "{\"sql\":\"...\",\"params\":{\"paramName\":123},\"resultColumns\":[\"...\"],\"notes\":\"optional\"}. " +
                    "If SQL cannot be generated return: " +
                    "{\"sql\":null,\"params\":{},\"resultColumns\":[],\"notes\":\"reason\"}. ";

    private static final String TEXT_QUERY_PART =
            "User request: %s. ";

    private static final String CRITERIA_QUERY_PART =
            "Selection criteria: %s. " +
                    "If multiple values belong to the same class, use OR inside that class. " +
                    "If values belong to different classes, use AND between classes. " +
                    "Ignore values marked as Not Set. ";

    private static final String VIEW_PART =
            "VIEW SQL: %s";

    public String buildTextQueryPrompt(String viewDdl, String clientQuery) {
        return oneLine(
                ROLE_PREFIX +
                        COMMON_REQUIREMENTS.formatted(MySqlSchemaLoader.TARGET_VIEW) +
                        TEXT_QUERY_PART.formatted(clientQuery) +
                        RESPONSE_FORMAT +
                        VIEW_PART.formatted(viewDdl)
        );
    }

    public String buildCriteriaPrompt(String viewDdl, List<String> selectedCriteria) {
        String criteriaText = selectedCriteria.stream().collect(Collectors.joining(", "));
        return oneLine(
                ROLE_PREFIX +
                        COMMON_REQUIREMENTS.formatted(MySqlSchemaLoader.TARGET_VIEW) +
                        CRITERIA_QUERY_PART.formatted(criteriaText) +
                        RESPONSE_FORMAT +
                        VIEW_PART.formatted(viewDdl)
        );
    }

    private String oneLine(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}