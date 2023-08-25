package com.lostsidewalk.buffy.app.model.response;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Date;

@Slf4j
@Data
public class QueueDTO {

    /**
     * The unique identifier of the queue.
     */
    Long id;

    /**
     * The identifier of the queue.
     */
    @NotBlank(message = "{queue.error.ident-is-blank}")
    @Size(max = 256, message = "{queue.error.ident-too-long}")
    String ident;

    /**
     * The title of the queue.
     */
    @NotBlank(message = "{queue.error.title-is-blank}")
    @Size(max = 512, message = "{queue.error.title-too-long}")
    String title;

    /**
     * The description of the queue.
     */
    @Size(max = 1024, message = "{queue.error.description-too-long}")
    String description;

    /**
     * The generator information of the queue.
     */
    @Size(max = 512, message = "{queue.error.generator-too-long}")
    String generator;

    /**
     * The transport identifier of the queue.
     */
    @NotBlank(message = "{queue.error.transport-is-blank")
    @Size(max = 256, message = "{queue.error.transport-too-long}")
    String transportIdent;

    /**
     * true if the queue is currently enabled
     */
    boolean isEnabled;

    /**
     * The configuration for exporting the queue.
     */
    Serializable exportConfig;

    /**
     * The copyright information of the queue.
     */
    @Size(max = 1024, message = "{queue.error.copyright-too-long}")
    String copyright;

    /**
     * The language of the queue.
     */
    @Size(max = 16, message = "{queue.error.language-too-long}")
    String language;

    /**
     * The image source of the queue.
     */
    @Size(max = 10240, message = "{queue.error.queue-img-src-too-long}")
    String queueImgSrc;

    /**
     * The timestamp when the queue was last deployed.
     */
    Date lastDeployed;

    /**
     * Whether authentication is required for the queue.
     */
    Boolean isAuthenticated;

    public QueueDTO(Long id, String ident, String title, String description, String generator,
                    String transportIdent, boolean isEnabled, Serializable exportConfig, String copyright,
                    String language, String queueImgSrc, Date lastDeployed, Boolean isAuthenticated) {
        this.id = id;
        this.ident = ident;
        this.title = title;
        this.description = description;
        this.generator = generator;
        this.transportIdent = transportIdent;
        this.isEnabled = isEnabled;
        this.exportConfig = exportConfig;
        this.copyright = copyright;
        this.language = language;
        this.queueImgSrc = queueImgSrc;
        this.lastDeployed = lastDeployed;
        this.isAuthenticated = isAuthenticated;
    }

    public static QueueDTO from(Long id, String ident, String title, String description, String generator,
                                String transportIdent, boolean isEnabled, Serializable exportConfig, String copyright,
                                String language, String queueImgSrc, Date lastDeployed, Boolean isAuthenticated) {
        return new QueueDTO(id, ident, title, description, generator,
                transportIdent, isEnabled, exportConfig, copyright,
                language, queueImgSrc, lastDeployed, isAuthenticated);
    }
}
