package org.nsky.api.controller;

import lombok.AllArgsConstructor;
import org.nsky.api.controller.dto.CreateChatRequestDTO;
import org.nsky.api.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chats")
@AllArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/create")
    public Mono<ResponseEntity<Void>> createChat(@RequestBody CreateChatRequestDTO request) {
        return chatService
            .createChat(request.name())
            .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).build()));
    }
}
