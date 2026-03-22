package org.nsky.api.service;

import lombok.AllArgsConstructor;
import org.nsky.api.controller.dto.GetChatResponseDTO;
import org.nsky.api.model.Chat;
import org.nsky.api.repository.ChatRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;

    public Flux<GetChatResponseDTO> findAllChats() {
        Flux<Chat> chats = chatRepository.findAll();

        return chats.map(chat -> new GetChatResponseDTO(chat.getId(), chat.getName()));
    }

    public Mono<Chat> createChat(String name) {
        Chat chat = new Chat();
        chat.setName(name);

        return chatRepository.save(chat);
    }

    public Mono<Void> deleteChat(Long id) {
        return chatRepository.deleteById(id);
    }
}
