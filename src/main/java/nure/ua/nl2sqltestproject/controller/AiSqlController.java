package nure.ua.nl2sqltestproject.controller;

import nure.ua.nl2sqltestproject.dto.AiQueryRequest;
import nure.ua.nl2sqltestproject.dto.CriteriaSelectionRequest;
import nure.ua.nl2sqltestproject.dto.CriteriaSqlDebugResponse;
import nure.ua.nl2sqltestproject.dto.OpenAiDtos;
import nure.ua.nl2sqltestproject.service.AiSqlService;
import nure.ua.nl2sqltestproject.service.CriteriaSearchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiSqlController {

    private final AiSqlService service;
    private final CriteriaSearchService criteriaSearchService;

    public AiSqlController(AiSqlService service, CriteriaSearchService criteriaSearchService) {
        this.service = service;
        this.criteriaSearchService = criteriaSearchService;
    }

    @PostMapping("/query")
    public OpenAiDtos.SqlQueryApiResponse query(@RequestBody AiQueryRequest request) throws Exception {
        return service.runClientQuery(request.query());
    }

    @PostMapping("/criteria-query")
    public CriteriaSqlDebugResponse criteriaQuery(@RequestBody CriteriaSelectionRequest request) throws Exception {
        return criteriaSearchService.searchByCriteria(request.selectedValues());
    }
}