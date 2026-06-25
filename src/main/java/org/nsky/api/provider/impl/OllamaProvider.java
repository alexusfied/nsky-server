package org.nsky.api.provider.impl;

import org.nsky.api.provider.LlmProvider;
import org.nsky.api.service.SearchService;
import org.nsky.api.service.dto.OllamaStreamRequestDTO;
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
public class OllamaProvider implements LlmProvider {
    private final WebClient client;
    private final SearchService searchService;

    public OllamaProvider(
        @Value("${ollama.base.url}") String ollamaBaseUrl,
        SearchService searchService
    ) {
        this.searchService = searchService;
        this.client = WebClient.create(ollamaBaseUrl);
    }

    @Override
    public String getProviderKey() {
        return "ollama";
    }

    @Override
    public Flux<String> stream(String userPrompt, String systemPrompt) {
        List<Map<String, String>> messages = new ArrayList<>(List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
        ));

        List<Map<String, Object>> tools = List.of(
            Map.of("type", "function", "function", Map.of(
                "name", "perform_web_search",
                "description", "Perform a web search to get up-to-date and missing information",
                "parameters", Map.of(
                    "type", "object",
                    "required", List.of("query"),
                    "properties", Map.of(
                        "query", Map.of(
                            "type", "string",
                            "description", "The query which is used for the web search"
                        )
                    )
                )
            ))
        );

        Mono<OllamaStreamRequestDTO> request = Mono.just(new OllamaStreamRequestDTO(
            "mistral",
            messages,
            tools
        ));

        return client
            .post()
            .uri("/api/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request, OllamaStreamRequestDTO.class)
            .retrieve()
            .bodyToFlux(JsonNode.class)
            .flatMap(node -> {
                if (node.get("message").has("tool_calls")) {

                    Flux<String> webSearchSignal = Flux.just("WEB_SEARCH_STARTED");

                    String query = node.get("message").get("tool_calls").get(0).get("function").get("arguments").get("query").asString();

                    return Flux.concat(
                        webSearchSignal,
                        searchService
                            .performWebSearch(query)
                            .flatMap(searchResult -> request.flatMapMany(req -> {
                                req.messages().add(Map.of("role", "tool", "tool_name", "perform_web_search", "content", searchResult));

                                Mono<OllamaStreamRequestDTO> updatedRequest = Mono.just(new OllamaStreamRequestDTO(
                                    "mistral",
                                    req.messages(),
                                    req.tools()
                                ));

                                return client
                                    .post()
                                    .uri("/api/chat")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(updatedRequest, OllamaStreamRequestDTO.class)
                                    .retrieve()
                                    .bodyToFlux(JsonNode.class)
                                    .map(_node -> _node.get("message").get("content").asString());
                            }))
                    );
                }
                return Flux.just(node.get("message").get("content").asString());
            });
    }
}
