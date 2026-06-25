package org.nsky.api.controller;

import lombok.AllArgsConstructor;
import org.nsky.api.controller.dto.StreamRequestDTO;
import org.nsky.api.service.LlmService;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalTime;

@RestController
@RequestMapping("/api/llm")
@AllArgsConstructor
public class LlmController {
    private final LlmService llmService;

    @PostMapping("/stream")
    public Flux<ServerSentEvent<String>> stream(
        @RequestBody StreamRequestDTO request) {
        return llmService
            .stream(request.prompt(), request.chatId(), request.provider())
            .map(responseChunk -> ServerSentEvent.<String> builder()
                .id(LocalTime.now().toString())
                .event(responseChunk.type())
                .data(responseChunk.content())
                .build()
            );
    }
}
