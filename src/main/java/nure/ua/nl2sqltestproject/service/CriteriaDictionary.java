package nure.ua.nl2sqltestproject.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CriteriaDictionary {

    private static final Map<String, CriterionItem> ITEMS = createItems();

    public List<String> toPromptItems(List<String> codes) {
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
                .filter(item -> !"Not Set".equalsIgnoreCase(item.englishName()))
                .map(item -> item.groupTitle() + ": " + item.englishName())
                .collect(Collectors.toList());
    }

    private static Map<String, CriterionItem> createItems() {
        Map<String, CriterionItem> map = new LinkedHashMap<>();

        map.put("v1", new CriterionItem("Skin Type", "Oily"));
        map.put("v2", new CriterionItem("Skin Type", "Dry"));
        map.put("v3", new CriterionItem("Skin Type", "Normal"));
        map.put("v4", new CriterionItem("Skin Type", "Combination"));
        map.put("v5", new CriterionItem("Skin Type", "Not Set"));

        map.put("v6", new CriterionItem("Skin Sensitivity", "Sensitive"));
        map.put("v7", new CriterionItem("Skin Sensitivity", "Non-Sensitive"));
        map.put("v8", new CriterionItem("Skin Sensitivity", "Not Set"));

        map.put("v9", new CriterionItem("Age Group", "18-25"));
        map.put("v10", new CriterionItem("Age Group", "26-44"));
        map.put("v11", new CriterionItem("Age Group", "45+"));
        map.put("v12", new CriterionItem("Age Group", "Not Set"));

        map.put("v13", new CriterionItem("Climate", "Cold"));
        map.put("v14", new CriterionItem("Climate", "Moderate"));
        map.put("v15", new CriterionItem("Climate", "Hot"));
        map.put("v16", new CriterionItem("Climate", "Not Set"));

        map.put("v17", new CriterionItem("Season", "Summer"));
        map.put("v18", new CriterionItem("Season", "Winter"));
        map.put("v19", new CriterionItem("Season", "All Season"));
        map.put("v20", new CriterionItem("Season", "Not Set"));

        map.put("v21", new CriterionItem("Time of Day", "Day"));
        map.put("v22", new CriterionItem("Time of Day", "Night"));
        map.put("v23", new CriterionItem("Time of Day", "Day/Night"));
        map.put("v24", new CriterionItem("Time of Day", "Not Set"));

        map.put("v25", new CriterionItem("Price", "< 1000"));
        map.put("v26", new CriterionItem("Price", "< 2000"));
        map.put("v27", new CriterionItem("Price", "≤ 3000"));
        map.put("v28", new CriterionItem("Price", "> 3000"));
        map.put("v29", new CriterionItem("Price", "Not Set"));

        map.put("v30", new CriterionItem("Manufacturer's Brand", "Chanel"));
        map.put("v31", new CriterionItem("Manufacturer's Brand", "Vichy"));
        map.put("v32", new CriterionItem("Manufacturer's Brand", "Nivea"));
        map.put("v33", new CriterionItem("Manufacturer's Brand", "Hillary"));
        map.put("v34", new CriterionItem("Manufacturer's Brand", "Not Set"));

        return map;
    }

    private record CriterionItem(String groupTitle, String englishName) {}
}