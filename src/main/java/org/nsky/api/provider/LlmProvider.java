package org.nsky.api.provider;

import org.nsky.api.service.dto.StreamResponseChunk;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface LlmProvider {
    String getProviderKey();
    Flux<StreamResponseChunk> stream(List<Map<String, String>> messages);
}
