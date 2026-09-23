package org.nsky.api.model;

import lombok.Data;

import org.nsky.api.enums.LlmProvider;
import org.nsky.api.enums.Theme;
import org.springframework.data.annotation.Id;

@Data
public class Setting {
    @Id
    private Long id;
    private LlmProvider provider;
    private Theme theme;
    private Boolean think;
}

