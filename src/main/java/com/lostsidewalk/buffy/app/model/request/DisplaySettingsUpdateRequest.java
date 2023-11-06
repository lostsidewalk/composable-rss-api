package com.lostsidewalk.buffy.app.model.request;

import com.lostsidewalk.buffy.ThemeConfig;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
class DisplaySettingsUpdateRequest {

    @Valid
    ThemeConfig themeConfig;
}
