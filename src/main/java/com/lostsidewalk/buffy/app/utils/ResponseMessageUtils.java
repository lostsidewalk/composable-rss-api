package com.lostsidewalk.buffy.app.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

public class ResponseMessageUtils {

    @Data
    @AllArgsConstructor
    public static class ResponseMessage {

        @NotNull(message = "{response.message.message-is-null}")
        Object message;
    }

    public static ResponseMessage buildResponseMessage(Object body) {
        return new ResponseMessage(body);
    }
}
