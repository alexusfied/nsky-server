package org.nsky.api.controller.dto;

public record SaveSettingsRequestDTO(
    String provider,
    String theme,
    Boolean think
) {}
