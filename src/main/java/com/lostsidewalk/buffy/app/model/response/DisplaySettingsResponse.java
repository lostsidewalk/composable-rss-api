package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.ThemeConfig;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Data
class DisplaySettingsResponse {

    final Map<String, String> displayConfig;

    @Valid
    final ThemeConfig themeConfig;

    private DisplaySettingsResponse(Map<String, String> displayConfig, ThemeConfig themeConfig) {
        this.displayConfig = displayConfig;
        this.themeConfig = themeConfig;
    }
}
