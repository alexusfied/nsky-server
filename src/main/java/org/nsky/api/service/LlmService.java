package org.nsky.api.service;

import lombok.extern.slf4j.Slf4j;
import org.nsky.api.controller.dto.GetChatMessagesResponseDTO;
import org.nsky.api.factory.LlmProviderFactory;
import org.nsky.api.model.Message;
import org.nsky.api.model.Setting;
import org.nsky.api.provider.LlmProvider;
import org.nsky.api.provider.impl.OllamaProvider;
import org.nsky.api.repository.MessageRepository;
import org.nsky.api.service.dto.StreamResponseChunk;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class LlmService {
    private final MessageRepository messageRepository;
    private final ChatService chatService;
    private final LlmProviderFactory providerFactory;
    private final OllamaProvider ollamaProvider;
    private final SettingsService settingsService;

    public LlmService(
        MessageRepository messageRepository,
        ChatService chatService,
        LlmProviderFactory providerFactory,
        OllamaProvider ollamaProvider,
        SettingsService settingsService
    ) {
        this.chatService = chatService;
        this.messageRepository = messageRepository;
        this.providerFactory = providerFactory;
        this.ollamaProvider = ollamaProvider;
        this.settingsService = settingsService;
    }

    public Flux<StreamResponseChunk> stream(String prompt, Long chatId, String providerKey) {
        LlmProvider provider = providerFactory.getProvider(providerKey);

        return chatId == null
            ? chatService.createChat(prompt).flatMapMany(chat -> saveMessagesAndStream(provider, chat.getId(), prompt))
            : saveMessagesAndStream(provider, chatId, prompt);

    }

    private Flux<StreamResponseChunk> saveMessagesAndStream(LlmProvider provider, Long chatId, String prompt) {
        Message userPrompt = new Message();
        userPrompt.setAuthor("user");
        userPrompt.setContent(prompt);
        userPrompt.setChatId(chatId);

        Mono<Setting> setting = settingsService.getSettings();

        Flux<StreamResponseChunk> llmResponse = messageRepository.save(userPrompt)
            .thenMany(chatService.findAllMessagesForChat(chatId))
            .collectList()
            .zipWith(setting)
            .flatMapMany(tuple -> provider.stream(tuple.getT1(), tuple.getT2().getThink()))
            .map(response -> {
                String thinking = response.getResult().getMetadata().get("thinking");

                if (thinking != null && !thinking.isEmpty()) return new StreamResponseChunk("think", thinking);

                return new StreamResponseChunk("token", response.getResult().getOutput().getText());
            })
            .startWith(new StreamResponseChunk("chat-id", chatId.toString()))
            .cache();

        Mono<Void> saveLlmResponse = llmResponse
            .reduce(new StringBuilder(), (builder, chunk) -> {
                if (!(chunk.type().equals("chat-id") || chunk.type().equals("think")) ) return builder.append(chunk.content());
                return builder;
            })
            .map(StringBuilder::toString)
            .flatMap(result -> {
                Message response = new Message();
                response.setChatId(chatId);
                response.setAuthor("assistant");
                response.setContent(result);

                return messageRepository.save(response);
            })
            .then();

        return llmResponse.concatWith(saveLlmResponse.then(Mono.empty()));
    }
}
