package org.nsky.api.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nsky.api.model.Message;
import org.nsky.api.repository.MessageRepository;
import org.nsky.api.service.dto.OllamaStreamRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LlmService {
    private final MessageRepository messageRepository;
    private final ChatService chatService;
    private final SearchService searchService;
    private final WebClient client;

    public LlmService(
        @Value("${ollama.base.url}") String ollamaBaseUrl,
        MessageRepository messageRepository,
        ChatService chatService,
        SearchService searchService
    ) {
        this.searchService = searchService;
        this.chatService = chatService;
        this.messageRepository = messageRepository;

        this.client = WebClient.create(ollamaBaseUrl);
    }
    private final String SYSTEM_PROMPT = """
    When the user asks for up-to-date information or when you are unsure about facts, you MUST use the web search tool.
    Do not answer without searching if you are unsure about something. Do not use the web search tool for general knowledge
    questions or anything you are confident about. Do not directly quote the search results, but rather give an answer to
    the users question based on the web search results. Remember that the web search tool call has to be present in your
    response JSON as a tool_calls node
    """;

    public Flux<String> stream(String prompt, Long chatId) {
        List<Map<String, String>> messages = new ArrayList<>(List.of(
            Map.of("role", "system", "content", SYSTEM_PROMPT),
            Map.of("role", "user", "content", prompt)
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
                    .flatMap(node -> {
                        if (node.get("message").has("tool_calls")) {
                            log.info("Tool called: {}", node.get("message").get("tool_calls"));

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

                                        log.info("Calling llm with tool call response...");

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
                    })
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
