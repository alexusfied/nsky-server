package org.nsky.api.service.dto;

import java.util.List;
import java.util.Map;

public record MistralStreamRequestDTO(
    List<Map<String, String>> messages,
    String model
) {}
