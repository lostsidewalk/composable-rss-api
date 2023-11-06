package com.lostsidewalk.buffy.app.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Slf4j
@Data
@JsonInclude(NON_EMPTY)
public class ErrorDetails {
    private Date timestamp;
    private String message;
    private String details;

    public ErrorDetails(Date timestamp, String message, String details) {
        long time = timestamp.getTime();
        this.timestamp = new Date(time);
        this.message = message;
        this.details = details;
    }
}
