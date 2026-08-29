package org.nsky.api.provider.impl;

import java.util.List;
import java.util.Map;

import org.nsky.api.controller.dto.GetChatMessagesResponseDTO;
import org.nsky.api.provider.LlmProvider;
import org.nsky.api.service.DateTimeService;
import org.nsky.api.service.SearchService;
import org.nsky.api.service.dto.MistralStreamRequestDTO;
import org.nsky.api.service.dto.StreamResponseChunk;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Component
public class MistralProvider implements LlmProvider {
    private static final String DONE_EVENT = "[DONE]";

    private final WebClient client;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;
    private final SearchService searchService;
    private final DateTimeService dateTimeService;

    public MistralProvider(
        @Value("${mistral.base.url}") String mistralBaseUrl,
        @Value("${mistral.api.key}") String apiKey,
        ObjectMapper objectMapper,
        @Qualifier("mistralChatClient")
        ChatClient chatClient,
        SearchService searchService,
        DateTimeService dateTimeService

    ) {
        this.client = WebClient.create(mistralBaseUrl);
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.chatClient = chatClient;
        this.searchService = searchService;
        this.dateTimeService = dateTimeService;
    }

    @Override
    public String getProviderKey() {
        return "mistral";
    }

    @Override
    public Flux<ChatResponse> streamSpringAi(List<GetChatMessagesResponseDTO> messages) {
        List<Message> msg = messages.stream().<Message>map(message -> {
            return message.author().equals("assistant")
                ? new AssistantMessage(message.content())
                : new UserMessage(message.content());
        }).toList();

        return chatClient
            .prompt(new Prompt(msg))
            .tools(List.of(searchService, dateTimeService))
            .stream()
            .chatResponse();
    }

    @Override
    public Flux<StreamResponseChunk> stream(List<Map<String, String>> messages) {
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
            .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .filter(event -> event.data() != null && !DONE_EVENT.equals(event.data()))
            .map(event -> objectMapper.readTree(event.data()))
            .map(result -> new StreamResponseChunk("token", result.get("choices").get(0).get("delta").get("content").asString()));
    }
}
