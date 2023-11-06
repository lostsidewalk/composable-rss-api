package com.lostsidewalk.buffy.app.model.v1.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.lostsidewalk.buffy.post.ContentObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

/**
 * A request model for configuring a new content object.
 */
@Slf4j
@Data
@NoArgsConstructor
public class ContentObjectConfigRequest {

    /**
     * The type of the content object.
     */
    String type;

    /**
     * The value of the content object.
     */
    String value;

    /**
     * Method to convert this data transfer object into a content object instance.
     *
     * @return a ContentObject instance built from this DTO
     */
    public final ContentObject toContentObject() {
        return ContentObject.from(randomAlphanumeric(8), type, value);
    }

    @JsonAnySetter
    public static void handleUnrecognizedField(String key, Object value) {
        throw new IllegalArgumentException("Unrecognized field: " + key);
    }
}
