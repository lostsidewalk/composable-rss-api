package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Data
public class NewPasswordRequest {

    @NotBlank(message = "{new.password.error.new-password-is-blank}")
    @Size(min = 6, max = 256, message = "{new.password.error.new-password-length}")
    private String newPassword;

    @NotBlank(message = "{new.password.error.new-password-confirmed-is-blank}")
    @Size(min = 6, max = 256, message = "{new.password.error.new-password-confirmed-length}")
    private String newPasswordConfirmed;
}
