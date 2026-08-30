package org.nsky.api.provider.impl;

import java.util.List;

import org.nsky.api.controller.dto.GetChatMessagesResponseDTO;
import org.nsky.api.provider.LlmProvider;
import org.nsky.api.service.DateTimeService;
import org.nsky.api.service.SearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

@Component
public class OllamaProvider implements LlmProvider {
    private final SearchService searchService;
    private final DateTimeService dateTimeService;
    private final ChatClient chatClient;

    public OllamaProvider(
        SearchService searchService,
        @Qualifier("ollamaChatClient")
        ChatClient chatClient,
        DateTimeService dateTimeService
    ) {
        this.searchService = searchService;
        this.chatClient = chatClient;
        this.dateTimeService = dateTimeService;
    }

    @Override
    public String getProviderKey() {
        return "ollama";
    }

    @Override
    public Flux<ChatResponse> stream(List<GetChatMessagesResponseDTO> messages) {
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
}
