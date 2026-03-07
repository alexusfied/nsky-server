package org.nsky.api.controller;

import lombok.AllArgsConstructor;
import org.nsky.api.controller.dto.StreamRequestDTO;
import org.nsky.api.service.MessageService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/message")
@AllArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @PostMapping("/{chatId}/stream")
    public Flux<String> stream(
        @PathVariable Long chatId,
        @RequestBody StreamRequestDTO request) {
        return messageService.stream(request.prompt(), chatId);
    }
}
