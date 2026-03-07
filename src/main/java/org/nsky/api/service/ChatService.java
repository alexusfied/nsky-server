package org.nsky.api.service;

import lombok.AllArgsConstructor;
import org.nsky.api.model.Chat;
import org.nsky.api.repository.ChatRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;

    public Mono<Chat> createChat(String name) {
        Chat chat = new Chat();
        chat.setName(name);

        return chatRepository.save(chat);
    }
}
