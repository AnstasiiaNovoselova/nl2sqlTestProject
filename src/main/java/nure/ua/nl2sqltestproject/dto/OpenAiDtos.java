package nure.ua.nl2sqltestproject.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

public class OpenAiDtos {

    public record ResponseCreateRequest(
            String model,
            Object input,
            Map<String, Object> text
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseCreateResponse(
            String id,
            List<OutputItem> output
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutputItem(
            String type,
            List<Content> content
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            String type,
            String text
    ) {}
}
