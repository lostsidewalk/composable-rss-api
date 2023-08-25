package com.lostsidewalk.buffy.app.model.response;


import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Data
public class DeployResponse {

    @NotNull(message = "{deploy.response.error.timestamp-is-null}")
    Date timestamp;

    private DeployResponse(Date timestamp) {
        this.timestamp = timestamp;
    }

    public static DeployResponse from(List<PubResult> publicationResults) {
        return new DeployResponse(
                publicationResults.stream()
                        .map(PubResult::getPubDate)
                        .max(Comparator.comparing(Date::getTime))
                        .orElse(null)
        );
    }
}
