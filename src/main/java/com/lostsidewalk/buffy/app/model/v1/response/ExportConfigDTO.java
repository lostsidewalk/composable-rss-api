package com.lostsidewalk.buffy.app.model.v1.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.app.model.v1.Atom10Config;
import com.lostsidewalk.buffy.app.model.v1.RSS20Config;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

/**
 * A response model for the export configuration of a queue.
 */
@Slf4j
@Data
@JsonInclude(NON_ABSENT)
public class ExportConfigDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 23948712239048L;

    /**
     * Configuration options for ATOM 1.0 export.
     */
    Atom10Config atomConfig;

    /**
     * Configuration options for RSS 2.0 export.
     */
    RSS20Config rssConfig;

    /**
     * The maximum allowable number of published posts in the queue.
     */
    Integer maxPublished;

    /**
     * t/f whether this queue should auto-deploy.
     */
    Boolean isAutoDeploy;

    private ExportConfigDTO(Atom10Config atomConfig, RSS20Config rssConfig, Integer maxPublished, Boolean isAutoDeploy) {
        this.atomConfig = atomConfig;
        this.rssConfig = rssConfig;
        this.maxPublished = maxPublished;
        this.isAutoDeploy = isAutoDeploy;
    }

    /**
     * Static factory method to create a serialized export configuration from the supplied parameters.
     *
     * @param atomConfig        An ATOM 1.0 configuration data transfer object.
     * @param rssConfig         An RSS 2.0 configuration data transfer object.
     * @param maxPublished      The maximum allowable number of published posts in the queue.
     * @param isAutoDeploy      t/f whether this queue should auto-deploy
     * @return am Ex[prtConfigDTO built from parameters, in serialized form
     */
    public static Serializable from(Atom10Config atomConfig, RSS20Config rssConfig, Integer maxPublished, Boolean isAutoDeploy) {
        return new ExportConfigDTO(atomConfig, rssConfig, maxPublished, isAutoDeploy);
    }
}
