package nure.ua.nl2sqltestproject.service;

import org.springframework.stereotype.Component;

import static nure.ua.nl2sqltestproject.service.MySqlSchemaLoader.TARGET_VIEW;

@Component
public class CriteriaPromptBuilder {

    public String buildPrompt(String viewDefinitionSql, String selectedCriteriaText) {
        return """
                You are a professional expert in generating safe MySQL SELECT queries for cosmetic product search.

                Your task is to generate exactly one valid SQL SELECT query for the provided VIEW only.

                Important requirements:
                - Use only the VIEW named %s
                - Do not use base tables
                - Do not use INSERT, UPDATE, DELETE, DROP, ALTER, CREATE, SHOW, DESCRIBE, EXPLAIN
                - Do not use SELECT *
                - Use named parameters like :paramName
                - Return only these columns:
                  - id
                  - name
                  - price
                - The query must work against the VIEW definition provided below
                - A user may select multiple values inside the same category
                - If multiple values are selected inside one category, treat them as OR within that category
                - If values are selected across different categories, combine categories with AND
                - Ignore values marked as "Not Set"
                - If the request is broad, add LIMIT 200
                - Return strictly valid JSON only without markdown

                VIEW DEFINITION SQL:
                %s

                Selected user criteria:
                %s

                Response JSON format:
                {
                  "sql": "...",
                  "params": { "paramName": 123 },
                  "resultColumns": ["id", "name", "price"],
                  "notes": "optional"
                }

                If SQL cannot be generated, return:
                {
                  "sql": null,
                  "params": {},
                  "resultColumns": [],
                  "notes": "reason"
                }
                """.formatted(TARGET_VIEW, viewDefinitionSql, selectedCriteriaText);
    }
}