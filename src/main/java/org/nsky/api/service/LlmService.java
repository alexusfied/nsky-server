package org.nsky.api.service;

import lombok.extern.slf4j.Slf4j;
import org.nsky.api.factory.LlmProviderFactory;
import org.nsky.api.model.Message;
import org.nsky.api.provider.LlmProvider;
import org.nsky.api.repository.MessageRepository;
import org.nsky.api.service.dto.StreamResponseChunk;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class LlmService {
    private final MessageRepository messageRepository;
    private final ChatService chatService;
    private final LlmProviderFactory providerFactory;

    public LlmService(
        MessageRepository messageRepository,
        ChatService chatService,
        LlmProviderFactory providerFactory
    ) {
        this.chatService = chatService;
        this.messageRepository = messageRepository;
        this.providerFactory = providerFactory;
    }
    private final String SYSTEM_PROMPT = """
    When the user asks for up-to-date information or when you are unsure about facts, you MUST use the web search tool.
    Do not answer without searching if you are unsure about something. Do not use the web search tool for general knowledge
    questions or anything you are confident about. Do not directly quote the search results, but rather give an answer to
    the users question based on the web search results. Remember that the web search tool call has to be present in your
    response JSON as a tool_calls node
    """;

    public Flux<StreamResponseChunk> stream(String prompt, Long chatId, String providerKey) {
        LlmProvider provider = providerFactory.getProvider(providerKey);

        return chatId == null
            ? chatService.createChat(prompt).flatMapMany(chat -> saveUserPromptAndStream(provider, chat.getId(), prompt))
            : saveUserPromptAndStream(provider, chatId, prompt);
    }

    private Flux<StreamResponseChunk> saveUserPromptAndStream(LlmProvider provider, Long chatId, String prompt) {
        Message userPrompt = new Message();
        userPrompt.setAuthor("user");
        userPrompt.setContent(prompt);
        userPrompt.setChatId(chatId);

        Flux<StreamResponseChunk> llmResponse = messageRepository.save(userPrompt)
            .thenMany(
                provider.stream(prompt, SYSTEM_PROMPT)
            )
            .startWith(new StreamResponseChunk("chat-id", chatId.toString()))
            .cache();

        Mono<Void> saveLlmResponse = llmResponse
            .reduce(new StringBuilder(), (builder, chunk) -> {
                if (!chunk.type().equals("chat-id")) return builder.append(chunk.content());
                return builder;
            })
            .map(StringBuilder::toString)
            .flatMap(result -> {
                Message response = new Message();
                response.setChatId(chatId);
                response.setAuthor("model");
                response.setContent(result);

                return messageRepository.save(response);
            })
            .then();

        return llmResponse.concatWith(saveLlmResponse.then(Mono.empty()));
    }
}
