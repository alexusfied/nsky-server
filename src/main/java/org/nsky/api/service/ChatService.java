package org.nsky.api.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nsky.api.controller.dto.GetChatMessagesResponseDTO;
import org.nsky.api.controller.dto.GetChatResponseDTO;
import org.nsky.api.model.Chat;
import org.nsky.api.model.Message;
import org.nsky.api.repository.ChatRepository;
import org.nsky.api.repository.MessageRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    public Flux<GetChatResponseDTO> findAllChats() {
        Flux<Chat> chats = chatRepository.findAll();

        return chats.map(chat -> new GetChatResponseDTO(chat.getId(), chat.getName()));
    }

    public Flux<GetChatMessagesResponseDTO> findAllMessagesForChat(Long chatId) {
        Flux<Message> messages = messageRepository.findAllByChatIdOrderById(chatId);

        return messages.map(message ->
            new GetChatMessagesResponseDTO(message.getAuthor(), message.getContent())
        );
    }

    public Mono<Chat> createChat(String name) {
        Chat chat = new Chat();
        chat.setName(name);

        return chatRepository.save(chat);
    }

    public Mono<Void> deleteChat(Long id) {
        return chatRepository.deleteById(id);
    }

    public Mono<Void> renameChat(Long id, String updatedName) {
        return chatRepository.updateNameById(id, updatedName);

    }
}
