package com.ailene.lms.common.deepseek;

import com.ailene.lms.common.exception.BadGatewayException;
import com.ailene.lms.common.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);
    private static final String CHAT_COMPLETIONS_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL = "deepseek-chat";

    private final RestClient restClient;
    private final String apiKey;

    public DeepSeekClient(@Value("${deepseek.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    // json_object mode only guarantees valid JSON, not a shape: the prompt must say "json" and show an example.
    public String createJsonCompletion(String systemPrompt, String userPrompt, int maxTokens, double temperature) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DeepSeek call skipped: DEEPSEEK_API_KEY is not set");
            throw new ServiceUnavailableException("AI draft generation is not available right now");
        }

        Map<String, Object> body = Map.of("model", MODEL, "messages",
                List.of(Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "response_format", Map.of("type", "json_object"), "temperature", temperature, "max_tokens", maxTokens);

        DeepSeekChatResponse response;
        try {
            response = restClient.post()
                    .uri(CHAT_COMPLETIONS_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(DeepSeekChatResponse.class);
        } catch (RestClientResponseException e) {
            log.error("DeepSeek chat completion rejected: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadGatewayException("The AI service failed to generate a draft, please try again");
        } catch (RestClientException e) {
            log.error("DeepSeek chat completion call failed", e);
            throw new BadGatewayException("The AI service failed to generate a draft, please try again");
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().get(0).message() == null) {
            log.error("DeepSeek returned no choices");
            throw new BadGatewayException("The AI service returned an empty draft, please try again");
        }
        String content = response.choices().get(0).message().content();
        if (content == null || content.isBlank()) {
            log.error("DeepSeek returned empty content");
            throw new BadGatewayException("The AI service returned an empty draft, please try again");
        }
        return content;
    }

    private record DeepSeekChatResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}
