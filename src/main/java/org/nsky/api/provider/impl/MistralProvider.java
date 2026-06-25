package org.nsky.api.provider.impl;

import org.nsky.api.provider.LlmProvider;
import org.nsky.api.service.dto.MistralStreamRequestDTO;
import org.nsky.api.service.dto.StreamResponseChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MistralProvider implements LlmProvider {
    private final WebClient client;
    private final String apiKey;

    public MistralProvider(
        @Value("${mistral.base.url}") String mistralBaseUrl,
        @Value("${mistral.api.key}") String apiKey
    ) {
        this.client = WebClient.create(mistralBaseUrl);
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderKey() {
        return "mistral";
    }

    @Override
    public Flux<StreamResponseChunk> stream(String userPrompt, String systemPrompt) {
        List<Map<String, String>> messages = new ArrayList<>(List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
        ));

        Mono<MistralStreamRequestDTO> request = Mono.just(new MistralStreamRequestDTO(
            messages,
            "mistral-medium-3-5",
            true
        ));

        return client
            .post()
            .uri("/v1/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + apiKey)
            .body(request, MistralStreamRequestDTO.class)
            .retrieve()
            // TODO: This throws an error at the moment because of the [DONE] event sent by mistral
            .bodyToFlux(JsonNode.class)
            .map(result -> new StreamResponseChunk("token", result.get("choices").get(0).get("delta").get("content").asString()));
    }
}
