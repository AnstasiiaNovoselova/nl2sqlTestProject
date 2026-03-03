package nure.ua.nl2sqltestproject.client;

import nure.ua.nl2sqltestproject.config.OpenAiProperties;
import nure.ua.nl2sqltestproject.dto.OpenAiDtos;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private final RestTemplate openAiRestTemplate;
    private final OpenAiProperties props;

    public OpenAiClient(RestTemplate openAiRestTemplate, OpenAiProperties props) {
        this.openAiRestTemplate = openAiRestTemplate;
        this.props = props;
    }

    public String createJsonResponse(String systemInstructions, String userInputJson) {
        // Responses API: POST /v1/responses с model + input :contentReference[oaicite:2]{index=2}
        var req = new OpenAiDtos.ResponseCreateRequest(
                props.model(),
                List.of(
                        Map.of("role", "system", "content", systemInstructions),
                        Map.of("role", "user", "content", userInputJson)
                ),
                // Structured-ish: просим JSON в ответе (без лишнего текста)
                Map.of("format", Map.of("type", "json_object"))
        );

        var resp = openAiRestTemplate.postForObject("/responses", req, OpenAiDtos.ResponseCreateResponse.class);
        if (resp == null || resp.output() == null) {
            throw new IllegalStateException("OpenAI response is empty");
        }

        // Достаём первый text
        for (var item : resp.output()) {
            if (item == null || item.content() == null) continue;
            for (var c : item.content()) {
                if (c != null && "output_text".equals(c.type()) && c.text() != null) {
                    return c.text();
                }
            }
        }
        throw new IllegalStateException("No output_text in OpenAI response");
    }
}
