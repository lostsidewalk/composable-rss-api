package com.lostsidewalk.buffy.app.model.v1.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.app.model.v1.Atom10Config;
import com.lostsidewalk.buffy.app.model.v1.RSS20Config;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

/**
 * A request model for configuring queue export (publishing) options.
 */
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

    private ExportConfigRequest(Atom10Config atomConfig, RSS20Config rssConfig) {
        this.atomConfig = atomConfig;
        this.rssConfig = rssConfig;
    }

    /**
     * Static factory method to create an ExportConfigRequest data transfer object from the supplied parameters.
     *
     * @param atomConfig an ATOM 1.0 configuration object
     * @param rssConfig a RSS 2.0 configuration object
     * @return an ExportConfigRequest built from the supplied parameters
     */
    public static ExportConfigRequest from(Atom10Config atomConfig, RSS20Config rssConfig) {
        return new ExportConfigRequest(atomConfig, rssConfig);
    }
}
