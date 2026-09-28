package org.nsky.api.controller.dto;

import java.util.List;

public record DeleteChatsRequestDTO(
    List<Long> chatIds
) {}
