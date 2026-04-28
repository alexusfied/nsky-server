package org.nsky.api.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class SearchService {
    private final String SEARCH_ENGINE_URL = "http://localhost:8200";
    private final WebClient client = WebClient.create(SEARCH_ENGINE_URL);

    public Flux<String> performWebSearch(String query) {
        log.info("Performing web search for query: {}", query);
        MultiValueMap<String, String> body = MultiValueMap.fromSingleValue(Map.of("q", query, "format", "json"));

        return client
            .post()
            .uri("/search")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(JsonNode.class)
            .map(node -> node.get("results").toString());
    }
}
