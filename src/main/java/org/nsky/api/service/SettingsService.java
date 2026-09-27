package org.nsky.api.service;

import java.nio.channels.SeekableByteChannel;

import org.nsky.api.controller.dto.GetSettingsResponseDTO;
import org.nsky.api.enums.LlmProvider;
import org.nsky.api.enums.Theme;
import org.nsky.api.model.Setting;
import org.nsky.api.repository.SettingsRepository;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class SettingsService {
    private final SettingsRepository settingsRepository; 

    public Mono<GetSettingsResponseDTO> getSettingsDto() {
        Mono<Setting> setting = this.getSettings();

        return setting.map(savedSetting -> 
                new GetSettingsResponseDTO(
                    savedSetting.getProvider().name(), 
                    savedSetting.getTheme().name(), 
                    savedSetting.getThink()
                )
        );
    }

    public Mono<Setting> getSettings() {
        return settingsRepository.findById(1L);
    }

    public Mono<Setting> saveSettings(LlmProvider provider, Theme theme, Boolean think) {
        return settingsRepository.findById(1L)
            .map(savedSetting -> applyUpdate(savedSetting, provider, theme, think))
            .flatMap(settingsRepository::save);
    }

    private Setting applyUpdate(Setting savedSetting, LlmProvider provider, Theme theme, Boolean think) {
        Setting updated = savedSetting;
        
        if (provider != null) updated.setProvider(provider);
        if (theme != null) updated.setTheme(theme);
        if (think != null) updated.setThink(think);

        return updated;
    }
}
