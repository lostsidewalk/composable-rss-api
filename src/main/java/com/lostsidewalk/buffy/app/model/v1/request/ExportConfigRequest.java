package com.lostsidewalk.buffy.app.model.v1.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.app.model.v1.Atom10Config;
import com.lostsidewalk.buffy.app.model.v1.RSS20Config;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

/**
 * A request model for configuring queue export (publishing) options.
 */
@Slf4j
@Data
@NoArgsConstructor
@JsonInclude(NON_ABSENT)
public class ExportConfigRequest {

    /**
     * Configuration options for ATOM 1.0 export.
     */
    @Valid
    Atom10Config atomConfig;

    /**
     * Configuration options for RSS 2.0 export.
     */
    @Valid
    RSS20Config rssConfig;

    /**
     * The maximum allowable number of published posts in the queue.
     */
    Integer maxPublished;

    /**
     * A queue is always deployed when:
     *   - update queue export config (QueueConfigResponse)
     *   - delete queue export config (QueueConfigResponse)
     *   - delete post (single or batch) (PostDeleteResponse)
     *   - create queue (QueueConfigResponse)
     *   - delete queue (ResponseMessage)
     *   - delete queue properties (QueueConfigResponse)
     *   - update queue properties (QueueConfigResponse)
     * <p>
     * When in 'auto-deploy' mode, a queue is deployed when:
     * <p>
     *   - delete post properties (PostConfigResponse)
     *   - update post properties (PostConfigResponse)
     *   - add post subsidiary entities: author, content, contributor, enclosure, url (PostConfigResponse)
     *   - update post subsidiary entities: author, content, contributor, enclosure, media, url (PostConfigResponse)
     *   - delete post subsidiary entities: author, content, contributor, enclosure, media, url (PostConfigResponse)
     *   - create post (single or batch) (PostCreateResponse)
     * <p>
     * When not in 'auto-deploy' mode, post status is set to PUB_PENDING when the above changes occur.  An additional
     * call to the Queue Status API is required to deploy these changes.
     */
    Boolean isAutoDeploy;

    private ExportConfigRequest(Atom10Config atomConfig, RSS20Config rssConfig, Integer maxPublished, Boolean isAutoDeploy) {
        this.atomConfig = atomConfig;
        this.rssConfig = rssConfig;
        this.maxPublished = maxPublished;
        this.isAutoDeploy = isAutoDeploy;
    }

    /**
     * Static factory method to create an ExportConfigRequest data transfer object from the supplied parameters.
     *
     * @param atomConfig        An ATOM 1.0 configuration object.
     * @param rssConfig         A RSS 2.0 configuration object.
     * @param maxPublished      The maximum allowable number of published posts in the queue.
     * @param isAutoDeploy      t/f whether the queue should auto-deploy.
     * @return an ExportConfigRequest built from the supplied parameters
     */
    public static ExportConfigRequest from(Atom10Config atomConfig, RSS20Config rssConfig, Integer maxPublished, Boolean isAutoDeploy) {
        return new ExportConfigRequest(atomConfig, rssConfig, maxPublished, isAutoDeploy);
    }

    @JsonAnySetter
    public static void handleUnrecognizedField(String key, Object value) {
        throw new IllegalArgumentException("Unrecognized field: " + key);
    }
}
