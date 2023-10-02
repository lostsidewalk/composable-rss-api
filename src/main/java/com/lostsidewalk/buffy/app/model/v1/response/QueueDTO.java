package com.lostsidewalk.buffy.app.model.v1.response;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

/**
 * A response model for a queue.  Objects of this type are returned when users invoke the API to fetch all
 * queues defined for a specific user, or to create a new queue, or to update an existing one.
 */
@Slf4j
@Data
@JsonInclude(NON_ABSENT)
public class QueueDTO {

    /**
     * The unique internal identifier of the queue.
     */
    @JsonIgnore
    Long id;

    /**
     * The identifier of the queue.
     */
    @NotBlank(message = "{queue.error.ident-is-blank}")
    @Size(max = 256, message = "{queue.error.ident-too-long}")
    String ident;

    /**
     * The title of the queue.  If you have a website that contains the same information as your queue, the title of your
     * queue should be the same as the title of your website.
     * <p>
     * RSS2: channel -> title
     * ATOM1: feed -> title
     * <p>
     * Example: GoUpstate.com News Headlines
     */
    @NotBlank(message = "{queue.error.title-is-blank}")
    @Size(max = 512, message = "{queue.error.title-too-long}")
    String title;

    /**
     * Phrase or sentence describing the queue.
     * <p>
     * RSS2: channel -> description
     * ATOM1: feed -> subtitle
     * <p>
     * Example: The latest news from GoUpstate.com, a Spartanburg Herald-Journal Website.
     */
    @Size(max = 1024, message = "{queue.error.description-too-long}")
    String description;

    /**
     * A string indicating the program used to generate the syndicated feed artifact.
     * <p>
     * RSS2: channel -> generator
     * ATOM1: feed -> generator
     * <p>
     * Example: MightyInHouse Content System v2.3
     */
    @Size(max = 512, message = "{queue.error.generator-too-long}")
    String generator;

    /**
     * The transport identifier of the queue.  This value is used to uniquely locate this queue within the FeedGears RSS
     * platform.
     */
    @NotBlank(message = "{queue.error.transport-is-blank")
    @Size(max = 256, message = "{queue.error.transport-too-long}")
    String transportIdent;

    /**
     * true if the queue is currently enabled for import.
     * <p>
     * Note: composable-rss queues do not have upstream feed subscriptions, thus are never imported.
     */
    @JsonIgnore
    boolean isEnabled;

    /**
     * Additional configuration settings for publishing feeds built from this queue in various formats.
     */
    ExportConfigDTO options;

    /**
     * Copyright notice for content in the queue.
     * <p>
     * RSS2: channel -> copyright
     * ATOM1: feed -> rights
     * <p>
     * Example: Copyright 2002, Spartanburg Herald-Journal
     */
    @Size(max = 1024, message = "{queue.error.copyright-too-long}")
    String copyright;

    /**
     * The language the queue is published in. This allows aggregators to group all Italian language sites, for example,
     * on a single page. A list of allowable values for this element, as provided by Netscape, is
     * <a href="https://www.rssboard.org/rss-language-codes">here</a>. You may also use
     * <a href="http://www.w3.org/TR/REC-html40/struct/dirlang.html#langcodes">values defined</a> by the W3C.
     * <p>
     * RSS2: channel -> language
     * ATOM1: feed -> language
     * <p>
     * Example: en-us
     */
    @Size(max = 16, message = "{queue.error.language-too-long}")
    String language;

    /**
     * Specifies a GIF, JPEG or PNG image that can be displayed with the channel.
     * <p>
     * This value is used to construct the image sub-element of the RSS 2.0 channel element, as well as the image and
     * logo sub-elements of the ATOM 1.0 feed element.
     * <p>
     * RSS2: channel -> image
     * ATOm1: feed -> icon, feed -> logo
     */
    @Size(max = 10240, message = "{queue.error.queue-img-src-too-long}")
    String queueImgSrc;

    /**
     * The timestamp when the queue was last deployed (built and published).  This, this value corresponds to the RSS
     * 2.0 pubDate and lastBuildDate fields.
     * <p>
     * pubDate represents the publication date for the content in the channel. For example, the New York Times publishes
     * on a daily basis, the publication date flips once every 24 hours. That's when the pubDate of the channel changes.
     * All date-times in RSS conform to the Date and Time Specification of RFC 822, with the exception that the year may
     * be expressed with two characters or four characters (four preferred).
     * <p>
     * lastBuildDate is the last time the content of the channel changed.
     * <p>
     * RSS2: channel -> lastBuildDate
     * ATOM1: feed -> updated
     * <p>
     * Example: Sat, 07 Sep 2002 09:42:31 GMT
     */
    Date lastDeployed;

    /**
     * Whether authentication is required for the queue.  Queues that require authentication are only accessible via BASIC
     * authentication, when the username and password pair correspond to a QueueCredential entity defined for this queue.
     */
    Boolean isAuthenticated;

    private QueueDTO(Long id, String ident, String title, String description, String generator,
                     String transportIdent, boolean isEnabled, ExportConfigDTO options, String copyright,
                     String language, String queueImgSrc, Date lastDeployed, Boolean isAuthenticated) {
        this.id = id;
        this.ident = ident;
        this.title = title;
        this.description = description;
        this.generator = generator;
        this.transportIdent = transportIdent;
        this.isEnabled = isEnabled;
        this.options = options;
        this.copyright = copyright;
        this.language = language;
        this.queueImgSrc = queueImgSrc;
        this.lastDeployed = lastDeployed;
        this.isAuthenticated = isAuthenticated;
    }

    /**
     * Static factory method to create a Queue data transfer object from the supplied parameters.
     */
    public static QueueDTO from(Long id, String ident, String title, String description, String generator,
                                String transportIdent, boolean isEnabled, ExportConfigDTO options, String copyright,
                                String language, String queueImgSrc, Date lastDeployed, Boolean isAuthenticated) {
        return new QueueDTO(id, ident, title, description, generator,
                transportIdent, isEnabled, options, copyright,
                language, queueImgSrc, lastDeployed, isAuthenticated);
    }
}
