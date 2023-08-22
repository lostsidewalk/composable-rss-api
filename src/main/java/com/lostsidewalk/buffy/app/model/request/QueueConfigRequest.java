package com.lostsidewalk.buffy.app.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.app.model.response.ExportConfigDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

/**
 * A request model for configuring a queue.
 */
@Data
@NoArgsConstructor
@JsonInclude(NON_ABSENT)
public class QueueConfigRequest {

    /**
     * The identifier of the queue.
     */
    @NotBlank(message = "{queue.config.error.ident-is-blank}")
    @Size(max = 256, message = "{queue.config.error.ident-too-long}")
    String ident;

    /**
     * The title of the queue (optional).
     */
    @Size(max = 512, message = "{queue.config.error.title-too-long}")
    String title;

    /**
     * The description of the queue (optional).
     */
    @Size(max = 1024, message = "{queue.config.error.description-too-long}")
    String description;

    /**
     * The generator of the queue.
     */
    @Size(max = 512, message = "{queue.config.error.generator-too-long}")
    String generator;

    /**
     * The export configuration of the queue.
     */
    @Valid
    ExportConfigDTO exportConfig;

    /**
     * The copyright information of the queue.
     */
    @Size(max = 1024, message = "{queue.config.error.copyright-too-long}")
    String copyright;

    /**
     * The language of the queue.
     */
    @Size(max = 16, message = "{queue.config.error.language-too-long}")
    String language;

    /**
     * The image source of the queue.
     */
    @Size(max = 16384, message = "{queue.config.error.img-src-too-long}")
    String imgSrc;

    private QueueConfigRequest(String ident, String title, String description, String generator,
                               ExportConfigDTO exportConfig,
                               String copyright, String language, String imgSrc)
    {
        this.ident = ident;
        this.title = title;
        this.description = description;
        this.generator = generator;
        this.exportConfig = exportConfig;
        this.copyright = copyright;
        this.language = language;
        this.imgSrc = imgSrc;
    }

    /**
     * Static factory method to create a Queue data transfer object.
     */
    public static QueueConfigRequest from(String ident, String title, String description, String generator,
                                          ExportConfigDTO exportConfig,
                                          String copyright, String language, String imgSrc)
    {
        return new QueueConfigRequest(
                ident,
                title,
                description,
                generator,
                exportConfig,
                copyright,
                language,
                imgSrc
        );
    }
}
