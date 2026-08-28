package com.ailene.lms.common.openai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

    private final RestClient restClient = RestClient.create();
    private final String apiKey;

    public OpenAiClient(@Value("${openai.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    // json_schema mode guarantees message.content is a JSON string matching jsonSchema.
    public String createStructuredCompletion(String model, String systemPrompt, String userPrompt, String schemaName,
            Map<String, Object> jsonSchema) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI is not configured (OPENAI_API_KEY missing)");
        }

        Map<String, Object> body = Map.of("model", model, "messages",
                List.of(Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "response_format",
                Map.of("type", "json_schema", "json_schema",
                        Map.of("name", schemaName, "strict", true, "schema", jsonSchema)));

        OpenAiChatResponse response;
        try {
            response = restClient.post()
                    .uri(CHAT_COMPLETIONS_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(OpenAiChatResponse.class);
        } catch (RestClientResponseException e) {
            log.error("OpenAI chat completion rejected: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("OpenAI request failed: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.error("OpenAI chat completion call failed", e);
            throw new IllegalStateException("OpenAI request failed", e);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI returned an empty response.");
        }
        String content = response.choices().get(0).message().content();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OpenAI returned an empty response.");
        }
        return content;
    }

    private record OpenAiChatResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}
