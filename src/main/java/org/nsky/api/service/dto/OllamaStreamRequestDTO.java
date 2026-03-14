package org.nsky.api.service.dto;

import java.util.List;
import java.util.Map;

public record OllamaStreamRequestDTO(
    String model,
    List<Map<String, String>> messages
) {}
