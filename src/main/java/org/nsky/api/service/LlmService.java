package org.nsky.api.service;

import lombok.AllArgsConstructor;
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

@Service
@AllArgsConstructor
public class LlmService {
    private final MessageRepository messageRepository;
    private final ChatService chatService;
    private final WebClient client = WebClient.create("http://localhost:11434");
    private final String SYSTEM_PROMPT = "Always structure your output using the Markdown syntax";

    public Flux<String> stream(String prompt, Long chatId) {
        Mono<OllamaStreamRequestDTO> request = Mono.just(new OllamaStreamRequestDTO(
            "mistral",
            List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt)
            )
        ));

        return chatId == null
            ? chatService.createChat(prompt).flatMapMany(chat -> saveUserPromptAndStream(request, chat.getId(), prompt))
            : saveUserPromptAndStream(request, chatId, prompt);
    }

    private Flux<String> saveUserPromptAndStream(Mono<OllamaStreamRequestDTO> request, Long chatId, String prompt) {
        Message userPrompt = new Message();
        userPrompt.setAuthor("user");
        userPrompt.setContent(prompt);
        userPrompt.setChatId(chatId);

        Flux<String> llmResponse = messageRepository.save(userPrompt)
            .thenMany(
                client
                    .post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request, OllamaStreamRequestDTO.class)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .map(node -> node.get("message").get("content").asString())
            )
            .startWith("chatId: " + chatId.toString())
            .cache();

        Mono<Void> saveLlmResponse = llmResponse
            .reduce(new StringBuilder(), (builder, chunk) -> {
                if (!chunk.startsWith("chatId")) return builder.append(chunk);
                return builder;
            })
            .map(StringBuilder::toString)
            .flatMap(result -> {
                Message llmResonse = new Message();
                llmResonse.setChatId(chatId);
                llmResonse.setAuthor("model");
                llmResonse.setContent(result);

                return messageRepository.save(llmResonse);
            })
            .then();

        return llmResponse.concatWith(saveLlmResponse.then(Mono.empty()));
    }
}
