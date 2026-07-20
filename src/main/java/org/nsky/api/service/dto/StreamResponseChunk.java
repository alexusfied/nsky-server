package org.nsky.api.service.dto;

public record StreamResponseChunk(
    String type,
    String content
) {
}
