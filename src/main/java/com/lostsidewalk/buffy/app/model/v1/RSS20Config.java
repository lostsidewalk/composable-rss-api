package com.lostsidewalk.buffy.app.model.v1;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

/**
 * Configuration options for RSS 2.0 export.
 */
@Slf4j
@Data
@NoArgsConstructor
@JsonInclude(NON_ABSENT)
public class RSS20Config implements Serializable {

    @Serial
    private static final long serialVersionUID = 239442356612239048L;

    /**
     * Email address for person responsible for editorial content.
     * <p>
     * Example:
     */
    String managingEditor;

    /**
     * Email address for person responsible for technical issues relating to channel.
     */
    String webMaster;

    /**
     * Specify one or more categories that the channel belongs to.
     * <p>
     * Example: MSFT
     */
    String categoryValue;

    /**
     * A string that identifies a categorization taxonomy.
     * <p>
     * Example: http://www.fool.com/cusips
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    String categoryDomain;

    /**
     * A URL that points to the documentation for the format used in the RSS file.
     * <p>
     * Example: https://www.rssboard.org/rss-specification
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    String docs;

    /**
     * The domain of a web service that supports the rssCloud interface (which can be implemented in HTTP-POST, XML-RPC or SOAP 1.1).
     * <p>
     * Example: rpc.sys.com
     */
    String cloudDomain;

    /**
     * The protocol of a web service that supports the rssCloud interface (which can be implemented in HTTP-POST, XML-RPC or SOAP 1.1).
     * <p>
     * Example: xml-rpc
     */
    String cloudProtocol;

    /**
     * The register procedure a web service that supports the rssCloud interface (which can be implemented in HTTP-POST, XML-RPC or SOAP 1.1).
     * <p>
     * Example: myCloud.rssPleaseNotify
     */
    String cloudRegisterProcedure;

    /**
     * The port of a web service that supports the rssCloud interface (which can be implemented in HTTP-POST, XML-RPC or SOAP 1.1).
     * <p>
     * Example: 80
     */
    Integer cloudPort;

    /**
     * TTL stands for time to live. It's a number of minutes that indicates how long a channel can be cached before
     * refreshing from the source. This makes it possible for RSS sources to be managed by a file-sharing network such as
     * Gnutella.
     * <p>
     * Example: 60
     */
    Integer ttl;

    /**
     * The PICS rating for the channel.
     */
    String rating;

    /**
     * The label of the Submit button in the (optional) text input area.
     * <p>
     * Example:
     */
    String textInputTitle;

    /**
     * The description of the (optional) text input area.
     */
    String textInputDescription;

    /**
     * The name of the text object in the (optional) text input area.
     */
    String textInputName;

    /**
     * The URL of the server process that the text input requests
     */
    String textInputLink;

    /**
     * A hint for aggregators telling them which hours they can skip. This element contains up to 24 'hour' sub-elements
     * whose value is a number between 0 and 23, representing a time in GMT, when aggregators, if they support the feature,
     * may not read the channel on hours listed in the 'skipHours' element. The hour beginning at midnight is hour zero.
     */
    String skipHours;

    /**
     * A hint for aggregators telling them which days they can skip. This element contains up to seven 'day' sub-elements
     * whose value is Monday, Tuesday, Wednesday, Thursday, Friday, Saturday or Sunday. Aggregators may not read the channel
     * during days listed in the 'skipDays' element.
     */
    String skipDays;

    private RSS20Config(String managingEditor, String webMaster, String categoryValue, String categoryDomain,
                        String docs, String cloudDomain, String cloudProtocol, String cloudRegisterProcedure,
                        Integer cloudPort, Integer ttl, String rating, String textInputTitle, String textInputDescription,
                        String textInputName, String textInputLink, String skipHours, String skipDays) {
        this.managingEditor = managingEditor;
        this.webMaster = webMaster;
        this.categoryValue = categoryValue;
        this.categoryDomain = categoryDomain;
        this.docs = docs;
        this.cloudDomain = cloudDomain;
        this.cloudProtocol = cloudProtocol;
        this.cloudRegisterProcedure = cloudRegisterProcedure;
        this.cloudPort = cloudPort;
        this.ttl = ttl;
        this.rating = rating;
        this.textInputTitle = textInputTitle;
        this.textInputDescription = textInputDescription;
        this.textInputName = textInputName;
        this.textInputLink = textInputLink;
        this.skipHours = skipHours;
        this.skipDays = skipDays;
    }

    // TODO: add channel link to RSS config object

    /**
     * Static factory method to create an RSS20Config object from the supplied parameters.
     */
    public static RSS20Config from(
            String managingEditor,
            String webMaster,
            String categoryValue,
            String categoryDomain,
            String docs,
            String cloudDomain,
            String cloudProtocol,
            String cloudRegisterProcedure,
            Integer cloudPort,
            Integer ttl,
            String rating,
            String textInputTitle,
            String textInputDescription,
            String textInputName,
            String textInputLink,
            String skipHours,
            String skipDays
    ) {
        return new RSS20Config(managingEditor, webMaster, categoryValue, categoryDomain, docs, cloudDomain,
                cloudProtocol, cloudRegisterProcedure, cloudPort, ttl, rating, textInputTitle, textInputDescription,
                textInputName, textInputLink, skipHours, skipDays);
    }

    @JsonAnySetter
    public static void handleUnrecognizedField(String key, Object value) {
        throw new IllegalArgumentException("Unrecognized field: " + key);
    }
}
