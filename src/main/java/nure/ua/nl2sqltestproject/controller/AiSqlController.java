package nure.ua.nl2sqltestproject.controller;

import nure.ua.nl2sqltestproject.dto.AiQueryRequest;
import nure.ua.nl2sqltestproject.service.AiSqlService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    // Пример вызова:
    // /api/ai/query?dbType=postgres&q=выведи%20все%20продукты%20которые%20стоят%20меньше%20100%20гривен
    @PostMapping("/query")
    public List<Map<String, Object>> query(@RequestBody AiQueryRequest request) throws Exception {
        return service.runClientQuery(request.query());
    }
}
