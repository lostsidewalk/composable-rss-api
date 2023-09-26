package com.lostsidewalk.buffy.app.model.v1.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.app.model.v1.Atom10Config;
import com.lostsidewalk.buffy.app.model.v1.RSS20Config;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static org.springframework.security.core.SpringSecurityCoreVersion.SERIAL_VERSION_UID;

/**
 * A response model for the export configuration of a queue.
 */
@Slf4j
@Data
@JsonInclude(NON_ABSENT)
public class ExportConfigDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = SERIAL_VERSION_UID;

    /**
     * Configuration options for ATOM 1.0 export.
     */
    Atom10Config atomConfig;

    /**
     * Configuration options for RSS 2.0 export.
     */
    RSS20Config rssConfig;

    private ExportConfigDTO(Atom10Config atomConfig, RSS20Config rssConfig) {
        this.atomConfig = atomConfig;
        this.rssConfig = rssConfig;
    }

    /**
     * Static factory method to create a serialized export configuration from the supplied parameters.
     *
     * @param testAtomConfig an ATOM 1.0 configuration data transfer object
     * @param testRssConfig an RSS 2.0 configuration data transfer object
     * @return am Ex[prtConfigDTO built from parameters, in serialized form
     */
    public static Serializable from(Atom10Config testAtomConfig, RSS20Config testRssConfig) {
        return new ExportConfigDTO(testAtomConfig, testRssConfig);
    }
}
