package org.nsky.api.controller;

import lombok.AllArgsConstructor;
import org.nsky.api.controller.dto.StreamRequestDTO;
import org.nsky.api.service.LlmService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/llm")
@AllArgsConstructor
public class LlmController {
    private final LlmService llmService;

    @PostMapping("/stream")
    public Flux<String> stream(
        @RequestBody StreamRequestDTO request) {
        return llmService.stream(request.prompt(), request.chatId(), request.provider());
    }
}
