package nure.ua.nl2sqltestproject.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptSupportService {

    private final String BASE_PROMPT = "Generate one safe MySQL SELECT query for the provided VIEW only. Use only VIEW %s. Return strict JSON only. Query must select all columns from the VIEW explicitly, not base tables, not DDL, not INSERT/UPDATE/DELETE, no semicolon, use named parameters like :paramName, return all selected column names in resultColumns.".formatted(MySqlSchemaLoader.TARGET_VIEW);

    public String buildTextQueryPrompt(String viewDdl) {
        return BASE_PROMPT + " VIEW SQL: %s".formatted(oneLine(viewDdl));
    }

    public String buildCriteriaPrompt(String viewDdl, List<String> selectedCriteria) {
        String criteriaText = selectedCriteria.stream()
                .collect(Collectors.joining(", "));

        return BASE_PROMPT + "User criteria: %s. Multiple values of the same class mean OR inside that class. Different classes mean AND between classes. Ignore Not Set values. VIEW SQL: %s"
                .formatted(criteriaText, oneLine(viewDdl)
        );
    }

    private String oneLine(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}