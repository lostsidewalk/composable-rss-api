package com.lostsidewalk.buffy.app.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.FrameworkConfig;
import jakarta.validation.Valid;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
@JsonInclude(NON_EMPTY)
public class SettingsUpdateRequest {

    @Valid
    FrameworkConfig frameworkConfig;
}
