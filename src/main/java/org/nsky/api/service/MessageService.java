package org.nsky.api.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nsky.api.model.Message;
import org.nsky.api.repository.MessageRepository;
import org.nsky.api.service.dto.OllamaStreamRequestDTO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final WebClient client = WebClient.create("http://localhost:11434");

    public Flux<String> stream(String prompt, Long chatId) {
        Message userPrompt = new Message();
        userPrompt.setAuthor("user");
        userPrompt.setContent(prompt);
        userPrompt.setChatId(chatId);

        Mono<OllamaStreamRequestDTO> request = Mono.just(new OllamaStreamRequestDTO(
            "mistral",
            List.of(Map.of("role", "user", "content", prompt))
        ));

        return messageRepository.save(userPrompt)
            .thenMany(
                client
                    .post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request, OllamaStreamRequestDTO.class)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .map(node -> node.get("message").get("content").asString())
            );
    }
}
