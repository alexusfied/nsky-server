package org.nsky.api.provider;

import reactor.core.publisher.Flux;

public interface LlmProvider {
    Flux<String> stream(String userPrompt, String systemPrompt);
}
