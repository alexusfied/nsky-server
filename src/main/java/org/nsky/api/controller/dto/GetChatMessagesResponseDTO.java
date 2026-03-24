package org.nsky.api.controller.dto;

public record GetChatMessagesResponseDTO(
    String author,
    String content
) {
}
