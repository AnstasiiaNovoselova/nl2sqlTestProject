package nure.ua.nl2sqltestproject.client;

import nure.ua.nl2sqltestproject.dto.OpenAiDtos;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;

@Component
public class OpenAiClient {

    private final ChatClient chatClient;

    public OpenAiClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public OpenAiDtos.SqlGenResponse createJsonResponse(String systemInstructions, String userInputJson) {
        var options = OpenAiChatOptions.builder()
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_OBJECT)
                        .build())
                .temperature(0.0)
                .build();

        var resp = chatClient
                .prompt()
                .options(options)
                .user(userInputJson)
                .system(systemInstructions)
                .call()
                .entity(OpenAiDtos.SqlGenResponse.class);

        if (resp == null) {
            throw new IllegalStateException("OpenAI response is empty");
        }
        return resp;
    }
}
