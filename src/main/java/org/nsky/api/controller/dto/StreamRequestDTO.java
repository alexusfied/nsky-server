package org.nsky.api.controller.dto;

public record StreamRequestDTO(
    Long chatId,
    String prompt,
    String provider
) {}
