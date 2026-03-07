package org.nsky.api.service;

import lombok.AllArgsConstructor;
import org.nsky.api.model.Message;
import org.nsky.api.repository.MessageRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@AllArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;

    public Flux<String> stream(String prompt, Long chatId) {
        Flux<String> llmResponseChunks = Flux
            // Mock the llm response for now
            .just("Hello, the weather is great today")
            .delayElements(Duration.ofSeconds(1));

        Mono<Void> saveMessages = llmResponseChunks
            .collectList()
            .map(list -> String.join(" ", list))
            .flatMap(aggregatedResponse -> saveMessagesAfterStreaming(prompt, aggregatedResponse, chatId).then());

        return llmResponseChunks.doOnSubscribe(sub -> saveMessages.subscribe());
    }

    private Flux<Message> saveMessagesAfterStreaming(String prompt, String response, Long chatId) {
        Message userPrompt = new Message();
        Message llmResponse = new Message();

        userPrompt.setAuthor("user");
        userPrompt.setContent(prompt);
        userPrompt.setChatId(chatId);

        llmResponse.setAuthor("model");
        llmResponse.setContent(response);
        llmResponse.setChatId(chatId);

        return messageRepository.saveAll(Flux.just(userPrompt, llmResponse));
    }
}
