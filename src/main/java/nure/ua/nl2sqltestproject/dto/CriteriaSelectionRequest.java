package nure.ua.nl2sqltestproject.dto;

import java.util.List;

public record CriteriaSelectionRequest(
        List<String> selectedValues
) {}
