package nure.ua.nl2sqltestproject.dto;

import java.util.List;
import java.util.Map;

public record CriteriaSqlDebugResponse(
        String prompt,
        String generatedSql,
        String renderedSql,
        Map<String, Object> params,
        List<Map<String, Object>> rows
) {}
