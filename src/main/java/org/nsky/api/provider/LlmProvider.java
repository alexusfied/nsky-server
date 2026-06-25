package org.nsky.api.provider;

import org.nsky.api.service.dto.StreamResponseChunk;
import reactor.core.publisher.Flux;

public interface LlmProvider {
    String getProviderKey();
    Flux<StreamResponseChunk> stream(String userPrompt, String systemPrompt);
}
