package com.lostsidewalk.buffy.app.model.response;


import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class ResponseMessage {

    @NotNull(message = "{response.message.message-is-null}")
    Object message;
}
