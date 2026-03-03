package nure.ua.nl2sqltestproject.controller;

import nure.ua.nl2sqltestproject.dto.AiQueryRequest;
import nure.ua.nl2sqltestproject.service.AiSqlService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiSqlController {

    private final AiSqlService service;

    public AiSqlController(AiSqlService service) {
        this.service = service;
    }

    @PostMapping("/query")
    public List<Map<String, Object>> query(@RequestBody AiQueryRequest request) throws Exception {
        return service.runClientQuery(request.query());
    }
}
