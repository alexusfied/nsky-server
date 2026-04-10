package org.nsky.api.controller;

import lombok.AllArgsConstructor;
import org.nsky.api.controller.dto.*;
import org.nsky.api.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chats")
@AllArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @GetMapping("/all")
    public Flux<GetChatResponseDTO> findAll() {
        return chatService.findAllChats();
    }

    @GetMapping("/{chatId}/messages")
    public Flux<GetChatMessagesResponseDTO> findAllMessagesForChat(@PathVariable Long chatId) {
        return chatService.findAllMessagesForChat(chatId);
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<CreateChatResponseDTO>> createChat(@RequestBody CreateChatRequestDTO request) {
        return chatService
            .createChat(request.name())
            .map(savedChat -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateChatResponseDTO(savedChat.getId()))
            );
    }

    @DeleteMapping("/{id}/delete")
    public Mono<ResponseEntity<Void>> deleteChat(@PathVariable Long id) {
        return chatService
            .deleteChat(id)
            .then(Mono.just(
                ResponseEntity.ok().build()
            ));
    }

    @PatchMapping("/{id}/rename")
    public Mono<ResponseEntity<Void>> renameChat(
        @PathVariable Long id,
        @RequestBody RenameChatRequestDTO request
    ) {
        return chatService
            .renameChat(id, request.updatedName())
            .then(Mono.just(
                ResponseEntity.ok().build()
            ));
    }
}
