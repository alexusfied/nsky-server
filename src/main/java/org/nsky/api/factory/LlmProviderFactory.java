package org.nsky.api.factory;

import org.nsky.api.provider.LlmProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LlmProviderFactory {
    private final Map<String, LlmProvider> providers;

    public LlmProviderFactory(List<LlmProvider> providerList) {
        this.providers = providerList.stream()
            .collect(Collectors.toMap(LlmProvider::getProviderKey, Function.identity()));
    }

    public LlmProvider getProvider(String key) {
        LlmProvider provider = providers.get(key.toLowerCase());

        if (provider == null) {
            throw new IllegalArgumentException("Unknown LLM provider: " + key);
        }

        return provider;
    }
}
