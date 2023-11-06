package com.lostsidewalk.buffy.app.model.v1.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * A response model for a queue deployment result.
 */
@Slf4j
@Data
@JsonInclude(NON_EMPTY)
public class DeployResponse {

    /**
     * The timestamp at which the queue was deployed to this publisher.
     */
    @NotNull(message = "{deploy.response.error.timestamp-is-null}")
    Date timestamp;

    /**
     * The identifier of the publisher (i.e., RSS_20 vs ATOM_10, etc.)
     */
    @NotBlank(message = "{deploy.response.error.publisher-ident-is-blank}")
    @Size(max = 64, message = "{deploy.response.error.publisher-ident-is-too-long}")
    String publisherIdent;

    /**
     * The URLs of the deployed artifact.
     */
    List<String> urls;

    /**
     * A list of errors that occurred during the publication, if any.
     */
    List<String> errors;

    private DeployResponse(Date timestamp, String publisherIdent, List<String> urls, List<String> errors) {
        this.timestamp = timestamp;
        this.publisherIdent = publisherIdent;
        this.urls = urls;
        this.errors = errors;
    }

    /**
     * Static factory method to convert PubResults into DeployResponse data transfer objects.
     *
     * @param publicationResults a mapping of publisher identifier to PubResult objects
     * @return a mapping of publisher identifier to DeployResponse objects
     */
    @SuppressWarnings("MethodWithMultipleLoops")
    public static Map<String, DeployResponse> from(Map<String, PubResult> publicationResults) {
        Map<String, DeployResponse> map = new HashMap<>(publicationResults.size());
        for (Map.Entry<String, PubResult> e : publicationResults.entrySet()) {
            String publisherIdent = e.getKey();
            PubResult p = e.getValue();
            List<Throwable> errors = p.getErrors();
            List<String> errorMessages = null;
            if (isNotEmpty(errors)) {
                errorMessages = new ArrayList<>(errors.size());
                for (Throwable throwable : errors) {
                    errorMessages.add(throwable.getMessage());
                }
            }
            DeployResponse deployResponse = new DeployResponse(
                    p.getPubDate(),
                    publisherIdent,
                    List.of(p.getTransportUrl(), p.getUserIdentUrl()),
                    errorMessages
            );
            map.put(publisherIdent, deployResponse);
        }
        return map;
    }
}
