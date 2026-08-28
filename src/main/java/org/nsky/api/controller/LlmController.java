package org.nsky.api.controller;

import lombok.AllArgsConstructor;
import org.nsky.api.controller.dto.StreamRequestDTO;
import org.nsky.api.controller.dto.TokenEventDTO;
import org.nsky.api.service.LlmService;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalTime;

@RestController
@RequestMapping("/api/llm")
@AllArgsConstructor
public class LlmController {
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @PostMapping("/stream")
    public Flux<ServerSentEvent<String>> stream(
        @RequestBody StreamRequestDTO request) {
        return llmService
            .stream(request.prompt(), request.chatId(), request.provider())
            .map(responseChunk -> ServerSentEvent.<String> builder()
                .id(LocalTime.now().toString())
                .event(responseChunk.type())
                .data(writeJson(new TokenEventDTO(responseChunk.content())))
                .build()
            );
    }

    @PostMapping("/stream-spring-ai")
    public Flux<ServerSentEvent<String>> streamSpringAi(
        @RequestBody StreamRequestDTO request) {
        return llmService.streamSpringAi(request.prompt(), request.provider())
            .map(chunk -> ServerSentEvent.<String> builder()
                .id(LocalTime.now().toString())
                .event(chunk.type())
                .data(writeJson(new TokenEventDTO(chunk.content())))
                .build()
                );
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize SSE payload", e);
        }
    }
}
