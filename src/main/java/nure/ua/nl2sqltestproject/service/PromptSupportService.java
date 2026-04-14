package nure.ua.nl2sqltestproject.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptSupportService {

    private static final String SYSTEM_ROLE =
            "You are a policy-constrained SQL generator for a MySQL application.";

    private static final String GENERAL_RULES =
            "Return strictly valid JSON only without markdown or extra explanations. Generate exactly one SELECT statement only. Use only VIEW %s. Use only columns from this VIEW. Do not use base tables. Do not use DDL or DML. Do not use semicolon. Use named parameters like :paramName. Return all selected column names in resultColumns.";

    private static final String POLICY_RULES =
            "Allowed requests: product lookup, filtering rows by fields existing in the VIEW, sorting, limiting returned rows. Forbidden requests: profit, revenue, sales analytics, turnover, margins, KPI, statistics, trends, forecasting, reports, any business or financial analytics, any request requiring aggregation or calculated metrics, and any request outside direct row-level retrieval from the VIEW.";

    private static final String SQL_RESTRICTIONS =
            "Do not use joins, subqueries, CTE, SUM, AVG, COUNT, MIN, MAX, GROUP BY, HAVING, or expressions like price * amount.";

    private static final String DENY_RESPONSE_RULE =
            "If the client request is forbidden or does not match the allowed policy, return exactly: {\"sql\": null, \"params\": {}, \"resultColumns\": [], \"notes\": \"Request is outside the allowed read-only product policy\"}.";

    public String buildTextQueryPrompt(String viewDdl) {
        return (SYSTEM_ROLE + " "
                + GENERAL_RULES.formatted(MySqlSchemaLoader.TARGET_VIEW) + " "
                + POLICY_RULES + " "
                + SQL_RESTRICTIONS + " "
                + DENY_RESPONSE_RULE + " "
                + "Client request will be provided in JSON field clientQuery. "
                + "VIEW SQL: " + oneLine(viewDdl))
                .trim();
    }

    public String buildCriteriaPrompt(String viewDdl, List<String> selectedCriteria) {
        String criteriaText = selectedCriteria.stream()
                .collect(Collectors.joining(", "));

        return (SYSTEM_ROLE + " "
                + GENERAL_RULES.formatted(MySqlSchemaLoader.TARGET_VIEW) + " "
                + POLICY_RULES + " "
                + SQL_RESTRICTIONS + " "
                + DENY_RESPONSE_RULE + " "
                + "User criteria: " + criteriaText + ". "
                + "Multiple values of the same class mean OR inside that class. "
                + "Different classes mean AND between classes. "
                + "Ignore Not Set values. "
                + "VIEW SQL: " + oneLine(viewDdl))
                .trim();
    }

    private String oneLine(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}