package org.nsky.api.provider;

import java.util.List;

import org.nsky.api.controller.dto.GetChatMessagesResponseDTO;
import org.springframework.ai.chat.model.ChatResponse;

import reactor.core.publisher.Flux;

public interface LlmProvider {
    String getProviderKey();
    Flux<ChatResponse> stream(List<GetChatMessagesResponseDTO> messages);
}
