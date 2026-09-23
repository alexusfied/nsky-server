package org.nsky.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import org.nsky.api.controller.dto.GetSettingsResponseDTO;
import org.nsky.api.controller.dto.SaveSettingsRequestDTO;
import org.nsky.api.enums.LlmProvider;
import org.nsky.api.enums.Theme;
import org.nsky.api.service.SettingsService;

@RestController
@RequestMapping("/api/settings")
@AllArgsConstructor
public class SettingsController {
    private final SettingsService settingsService;

    @GetMapping("")
    public Mono<GetSettingsResponseDTO> getSettings() {
        return settingsService.getSettings();
    }

    @PostMapping("/save")
    public Mono<ResponseEntity<Void>> saveSettings(@RequestBody SaveSettingsRequestDTO request) {
        return settingsService.saveSettings(LlmProvider.valueOf(request.provider()), Theme.valueOf(request.theme()), request.think()).then(Mono.just(ResponseEntity.ok().build()));
    

    }
}
