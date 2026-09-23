package org.nsky.api.controller.dto;

public record GetSettingsResponseDTO(
    String provider,
    String theme,
    Boolean think
) {}
