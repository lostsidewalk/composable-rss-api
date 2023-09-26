package com.lostsidewalk.buffy.app.model.v1.request;

import com.lostsidewalk.buffy.post.ContentObject;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A request model for configuring a new content object.
 */
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
    public ContentObject toContentObject() {
        return ContentObject.from(type, value);
    }
}
