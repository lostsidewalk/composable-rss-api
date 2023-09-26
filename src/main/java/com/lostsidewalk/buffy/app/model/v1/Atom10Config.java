package com.lostsidewalk.buffy.app.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

/**
 * Configuration options for ATOM 1.0 export.
 */
@Data
@NoArgsConstructor
@JsonInclude(NON_ABSENT)
public class Atom10Config {

    /**
     * A human-readable name for the author of the feed.
     */
    String authorName;

    /**
     * An email address for the author of the feed.
     */
    String authorEmail;

    /**
     * A homepage for the author of the feed.
     */
    String authorUri;

    /**
     * A human-readable name for the contributor to the feed.
     */
    String contributorName;

    /**
     * An email address for the contributor to the feed.
     */
    String contributorEmail;

    /**
     * A homepage for the contributor to the feed.
     */
    String contributorUri;

    /**
     * A term that identifies the category.
     */
    String categoryTerm;

    /**
     * A human-read label for the category.
     */
    String categoryLabel;

    /**
     * A URI that identifies the feed categorization scheme.
     */
    String categoryScheme;

    private Atom10Config(String authorName, String authorEmail, String authorUri,
                         String contributorName, String contributorEmail, String contributorUri,
                         String categoryTerm, String categoryLabel, String categoryScheme) {
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.authorUri = authorUri;
        this.contributorName = contributorName;
        this.contributorEmail = contributorEmail;
        this.contributorUri = contributorUri;
        this.categoryTerm = categoryTerm;
        this.categoryLabel = categoryLabel;
        this.categoryScheme = categoryScheme;
    }

    /**
     * Static factory method to create an Atom10Config object from the supplied parameters.
     */
    public static Atom10Config from(
            String authorName, String authorEmail, String authorUri,
            String contributorName, String contributorEmail, String contributorUri,
            String categoryTerm, String categoryLabel, String categoryScheme
    ) {
        return new Atom10Config(authorName, authorEmail, authorUri,
                contributorName, contributorEmail, contributorUri,
                categoryTerm, categoryLabel, categoryScheme);
    }
}
