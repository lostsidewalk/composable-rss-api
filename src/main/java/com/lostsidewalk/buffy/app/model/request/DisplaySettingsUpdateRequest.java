package com.lostsidewalk.buffy.app.model.request;

import com.lostsidewalk.buffy.ThemeConfig;
import jakarta.validation.Valid;
import lombok.Data;

@Data
public class DisplaySettingsUpdateRequest {

    @Valid
    ThemeConfig themeConfig;
}
