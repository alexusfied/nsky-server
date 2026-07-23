package org.nsky.api.provider.impl;

import org.nsky.api.provider.LlmProvider;
import org.nsky.api.service.SearchService;
import org.nsky.api.service.dto.OllamaStreamRequestDTO;
import org.nsky.api.service.dto.StreamResponseChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@Component
public class OllamaProvider implements LlmProvider {
    private final WebClient client;
    private final SearchService searchService;
    private final String model;

    private static final String SYSTEM_PROMPT = """
    When the user asks for up-to-date information or when you are unsure about facts, you MUST use the web search tool.
    Do not answer without searching if you are unsure about something. Do not use the web search tool for general knowledge
    questions or anything you are confident about. Do not directly quote the search results, but rather give an answer to
    the users question based on the web search results. Remember that the web search tool call has to be present in your
    response JSON as a tool_calls node
    """;

    public OllamaProvider(
        @Value("${ollama.model.name}") String model,
        @Value("${ollama.base.url}") String ollamaBaseUrl,
        SearchService searchService
    ) {
        this.model = model;
        this.searchService = searchService;
        this.client = WebClient.create(ollamaBaseUrl);
    }

    @Override
    public String getProviderKey() {
        return "ollama";
    }

    @Override
    public Flux<StreamResponseChunk> stream(List<Map<String, String>> messages) {
        messages.addFirst(Map.of("role", "system", "content", SYSTEM_PROMPT));

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
            model,
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

                    Flux<StreamResponseChunk> webSearchSignal = Flux.just(new StreamResponseChunk("web-search", ""));

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
                            .map(result -> new StreamResponseChunk("token", result))
                    );
                }
                return Flux.just(new StreamResponseChunk("token", node.get("message").get("content").asString()));
            });
    }
}
