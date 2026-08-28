package org.nsky.api.provider;

import java.util.List;
import java.util.Map;

import org.nsky.api.service.dto.StreamResponseChunk;
import org.springframework.ai.chat.model.ChatResponse;

import reactor.core.publisher.Flux;

public interface LlmProvider {
    String getProviderKey();
    Flux<StreamResponseChunk> stream(List<Map<String, String>> messages);
    Flux<ChatResponse> streamSpringAi(String prompt);
}
