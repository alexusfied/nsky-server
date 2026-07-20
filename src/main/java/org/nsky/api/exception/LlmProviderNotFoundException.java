package org.nsky.api.exception;

public class LlmProviderNotFoundException extends IllegalArgumentException {
    public LlmProviderNotFoundException() {}
    public LlmProviderNotFoundException(String message) {
        super(message);
    }
}
