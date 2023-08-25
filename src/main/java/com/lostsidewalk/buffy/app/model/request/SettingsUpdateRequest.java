package com.lostsidewalk.buffy.app.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.FrameworkConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
@JsonInclude(NON_EMPTY)
public class SettingsUpdateRequest {

    @Email(message = "{settings.update.error.email-invalid}")
    @Size(max = 512, message = "{settings.update.error.email-too-long}")
    String emailAddress;

    @Valid
    FrameworkConfig frameworkConfig;
}
