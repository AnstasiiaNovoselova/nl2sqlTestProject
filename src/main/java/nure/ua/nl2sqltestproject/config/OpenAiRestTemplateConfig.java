package nure.ua.nl2sqltestproject.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiRestTemplateConfig {

    @Bean
    public RestTemplate openAiRestTemplate(OpenAiProperties props) {
        var rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) props.connectTimeout().toMillis());
        rf.setReadTimeout((int) props.readTimeout().toMillis());

        var rt = new RestTemplate(rf);
        rt.setUriTemplateHandler(new DefaultUriBuilderFactory(props.baseUrl()));

        rt.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().setBearerAuth(props.apiKey()); // Authorization: Bearer ...
//            request.getHeaders().add("Content-Type", "application/json");
            return execution.execute(request, body);
        });

        return rt;
    }
}
