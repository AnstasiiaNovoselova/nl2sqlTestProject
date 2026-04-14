package nure.ua.nl2sqltestproject.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CriteriaDictionary {

    private static final Map<String, CriterionItem> ITEMS = createItems();

    public Map<String, List<CriterionItem>> groupSelected(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new IllegalArgumentException("selectedValues must not be empty");
        }

        return codes.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .distinct()
                .map(code -> {
                    CriterionItem item = ITEMS.get(code);
                    if (item == null) {
                        throw new IllegalArgumentException("Unknown criterion code: " + code);
                    }
                    return item;
                })
                .collect(Collectors.groupingBy(
                        CriterionItem::groupCode,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public String toPromptConditionsText(List<String> codes) {
        Map<String, List<CriterionItem>> grouped = groupSelected(codes);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<CriterionItem>> entry : grouped.entrySet()) {
            String groupCode = entry.getKey();
            List<CriterionItem> items = entry.getValue();

            sb.append(groupTitle(groupCode)).append(": ");

            String values = items.stream()
                    .map(i -> i.englishName() + " [" + i.code() + "]")
                    .collect(Collectors.joining(", "));

            sb.append(values).append("\n");
        }

        return sb.toString().trim();
    }

    private static String groupTitle(String groupCode) {
        return switch (groupCode) {
            case "skin_type" -> "Skin Type";
            case "skin_sensitivity" -> "Skin Sensitivity";
            case "age_group" -> "Age Group";
            case "climate" -> "Climate";
            case "season" -> "Season";
            case "time_of_day" -> "Time of Day";
            case "price" -> "Price";
            case "brand" -> "Manufacturer's Brand";
            default -> groupCode;
        };
    }

    private static Map<String, CriterionItem> createItems() {
        Map<String, CriterionItem> map = new LinkedHashMap<>();

        map.put("v1", new CriterionItem("v1", "skin_type", "Oily"));
        map.put("v2", new CriterionItem("v2", "skin_type", "Dry"));
        map.put("v3", new CriterionItem("v3", "skin_type", "Normal"));
        map.put("v4", new CriterionItem("v4", "skin_type", "Combination"));
        map.put("v5", new CriterionItem("v5", "skin_type", "Not Set"));

        map.put("v6", new CriterionItem("v6", "skin_sensitivity", "Sensitive"));
        map.put("v7", new CriterionItem("v7", "skin_sensitivity", "Non-Sensitive"));
        map.put("v8", new CriterionItem("v8", "skin_sensitivity", "Not Set"));

        map.put("v9", new CriterionItem("v9", "age_group", "[18-25]"));
        map.put("v10", new CriterionItem("v10", "age_group", "[26-44]"));
        map.put("v11", new CriterionItem("v11", "age_group", "[45+]"));
        map.put("v12", new CriterionItem("v12", "age_group", "Not Set"));

        map.put("v13", new CriterionItem("v13", "climate", "Cold"));
        map.put("v14", new CriterionItem("v14", "climate", "Moderate"));
        map.put("v15", new CriterionItem("v15", "climate", "Hot"));
        map.put("v16", new CriterionItem("v16", "climate", "Not Set"));

        map.put("v17", new CriterionItem("v17", "season", "Summer"));
        map.put("v18", new CriterionItem("v18", "season", "Winter"));
        map.put("v19", new CriterionItem("v19", "season", "All Season"));
        map.put("v20", new CriterionItem("v20", "season", "Not Set"));

        map.put("v21", new CriterionItem("v21", "time_of_day", "Day"));
        map.put("v22", new CriterionItem("v22", "time_of_day", "Night"));
        map.put("v23", new CriterionItem("v23", "time_of_day", "Day/Night"));
        map.put("v24", new CriterionItem("v24", "time_of_day", "Not Set"));

        map.put("v25", new CriterionItem("v25", "price", "< 1000"));
        map.put("v26", new CriterionItem("v26", "price", "< 2000"));
        map.put("v27", new CriterionItem("v27", "price", "≤ 3000"));
        map.put("v28", new CriterionItem("v28", "price", "> 3000"));
        map.put("v29", new CriterionItem("v29", "price", "Not Set"));

        map.put("v30", new CriterionItem("v30", "brand", "Chanel"));
        map.put("v31", new CriterionItem("v31", "brand", "Vichy"));
        map.put("v32", new CriterionItem("v32", "brand", "Nivea"));
        map.put("v33", new CriterionItem("v33", "brand", "Hillary"));
        map.put("v34", new CriterionItem("v34", "brand", "Not Set"));

        return map;
    }

    public record CriterionItem(
            String code,
            String groupCode,
            String englishName
    ) {
    }
}