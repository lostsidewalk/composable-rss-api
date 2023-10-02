package com.lostsidewalk.buffy.app.audit;

import com.lostsidewalk.buffy.app.model.request.SettingsUpdateRequest;
import com.lostsidewalk.buffy.app.model.v1.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.auth.User;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.lang.System.arraycopy;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j(topic = "appLog")
@Service
public class AppLogService {

    public void logQueueFetch(String username, StopWatch stopWatch, int queueCt) {
        auditLog("queue-fetch", "queueCt={}", username, stopWatch, queueCt);
    }

    public void logQueueUpdate(String username, StopWatch stopWatch, Long id, Map<String, PubResult> pubResults) {
        auditLog("queue-update", "id={}, pubResults={}", username, stopWatch, id, pubResults);
    }

    public void logQueueCreate(String username, StopWatch stopWatch, Map<String, PubResult> pubResults) {
        auditLog("queue-create", "pubResults={}", username, stopWatch, pubResults);
    }

    public void logQueueDelete(String username, StopWatch stopWatch, int deleteCt) {
        auditLog("queue-delete", "deleteCt={}", username, stopWatch, deleteCt);
    }

    public void logQueueAttributeFetch(String username, StopWatch stopWatch, Long id, String attrName) {
        auditLog("queue-attribute-fetch", "id={}, attrName={}", username, stopWatch, id, attrName);
    }

    public void logQueueAttributeUpdate(String username, StopWatch stopWatch, Long id, String attrName, Map<String, PubResult> pubResults) {
        auditLog("queue-attribute-update", "id={}, attrName={}, pubResults={}", username, stopWatch, id, attrName, pubResults);
    }

    public void logQueueAttributeDelete(String username, StopWatch stopWatch, Long id, String attrName, Map<String, PubResult> pubResults) {
        auditLog("queue-attribute-delete", "id={}, attrName={}, pubResults={}", username, stopWatch, id, attrName, pubResults);
    }

    public void logQueueCredentialCreate(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-credential-create", "id={}", username, stopWatch, id);
    }

    public void logQueueCredentialsFetch(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-credentials-fetch", "id={}", username, stopWatch, id);
    }

    public void logQueueCredentialFetch(String username, StopWatch stopWatch, Long queueId, Long credentialId) {
        auditLog("queue-credential-fetch", "queueId={}, credentialId={}", username, stopWatch, queueId, credentialId);
    }

    public void logQueueCredentialUpdate(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-credential-update", "id={}", username, stopWatch, id);
    }

    public void logQueueCredentialsDelete(String username, StopWatch stopWatch, Long queueId) {
        auditLog("queue-credentials-delete", "queueId={}", username, stopWatch, queueId);
    }

    public void logQueueCredentialDelete(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-credential-delete", "id={}", username, stopWatch, id);
    }

//    public void logOpmlExport(String username, StopWatch stopWatch) {
//        auditLog("opml-export", null, username, stopWatch);
//    }

    public void logSettingsFetch(String username, StopWatch stopWatch) {
        auditLog("settings-fetch", null, username, stopWatch);
    }

    public void logSettingsUpdate(String username, StopWatch stopWatch, SettingsUpdateRequest settingsUpdateRequest) {
        auditLog("settings-update", "settingsUpdateRequest={}", username, stopWatch, settingsUpdateRequest);
    }

//    public void logDisplaySettingsFetch(String username, StopWatch stopWatch) {
//        auditLog("display-settings-fetch", null, username, stopWatch);
//    }
//
//    public void logDisplaySettingsUpdate(String username, StopWatch stopWatch) {
//        auditLog("display-settings-update", "displaySettingsUpdateRequest={}", username, stopWatch);
//    }

    public void logStagingPostFetch(String username, StopWatch stopWatch, int queueIdCt, int stagingPostCt) {
        auditLog("staging-post-fetch", "queueIdCt={}, stagingPostCt={}, queryMetricsCt={}", username, stopWatch, queueIdCt, stagingPostCt);
    }

    public void logStagingPostCreate(String username, StopWatch stopWatch, int postConfigRequestCt, int stagingPostCt) {
        auditLog("staging-post-create", "queueIdCt={}, postConfigRequestCt={}, stagingPostCt={}", username, stopWatch, postConfigRequestCt, stagingPostCt);
    }

    public void logStagingPostUpdate(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-update", "id={}", username, stopWatch, id);
    }

    public void logStagingPostsDelete(String username, StopWatch stopWatch, Long queueId) {
        auditLog("staging-posts-delete", "queueId={}", username, stopWatch, queueId);
    }

    public void logStagingPostDelete(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-delete", "id={}", username, stopWatch, id);
    }

    public void logStagingPostPubStatusUpdate(String username, StopWatch stopWatch, Long id, PostStatusUpdateRequest postStatusUpdateRequest, Map<String, PubResult> pubResults) {
        auditLog("staging-post-pub-status-update", "id={}, postStatusUpdateRequest={}, pubResults={}", username, stopWatch, id, postStatusUpdateRequest, pubResults);
    }

    public void logStagingPostAttributeFetch(String username, StopWatch stopWatch, Long id, String attrName) {
        auditLog("staging-post-attribute-fetch", "id={}, attrName={}", username, stopWatch, id, attrName);
    }

    public void logStagingPostAttributeUpdate(String username, StopWatch stopWatch, Long id, String attrName) {
        auditLog("staging-post-attribute-update", "id={}, attrName={}", username, stopWatch, id, attrName);
    }

    public void logStagingPostAttributeDelete(String username, StopWatch stopWatch, Long id, String attrName) {
        auditLog("staging-post-attribute-delete", "id={}, attrName={}", username, stopWatch, id, attrName);
    }

    public void logStagingPostAddContent(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-add-content", "id={}", username, stopWatch, id);
    }

    public void logStagingPostContentsFetch(String username, StopWatch stopWatch, Long id, int contentCt) {
        auditLog("staging-post-contents-fetch", "id={}, contentCt={}", username, stopWatch, id, contentCt);
    }

    public void logStagingPostContentFetch(String username, StopWatch stopWatch, Long id, String contentIdent) {
        auditLog("staging-post-content-fetch", "id={}, contentIdent={}", username, stopWatch, id, contentIdent);
    }

    public void logStagingPostContentsUpdate(String username, StopWatch stopWatch, Long id, int contentCt) {
        auditLog("staging-post-contents-update", "id={}, contentCt={}", username, stopWatch, id, contentCt);
    }

    public void logStagingPostContentUpdate(String username, StopWatch stopWatch, Long id, String contentIdent) {
        auditLog("staging-post-content-update", "id={}, contentIdent={}", username, stopWatch, id, contentIdent);
    }

    public void logStagingPostContentsDelete(String username, StopWatch stopWatch, Long postId) {
        auditLog("staging-post-contents-delete", "id={}", username, stopWatch, postId);
    }

    public void logStagingPostContentDelete(String username, StopWatch stopWatch, Long id, String contentIdent) {
        auditLog("staging-post-content-delete", "id={}, contentIdent={}", username, stopWatch, id, contentIdent);
    }

    public void logStagingPostAddUrl(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-add-url", "id={}", username, stopWatch, id);
    }

    public void logStagingPostUrlsFetch(String username, StopWatch stopWatch, Long id, int urlCt) {
        auditLog("staging-post-urls-fetch", "id={}, urlCt={}", username, stopWatch, id, urlCt);
    }

    public void logStagingPostUrlFetch(String username, StopWatch stopWatch, Long id, String urlIdent) {
        auditLog("staging-post-url-fetch", "id={}, urlIdent={}", username, stopWatch, id, urlIdent);
    }

    public void logStagingPostUrlsUpdate(String username, StopWatch stopWatch, Long id, int urlCt) {
        auditLog("staging-post-urls-update", "id={}, urlCt={}", username, stopWatch, id, urlCt);
    }

    public void logStagingPostUrlUpdate(String username, StopWatch stopWatch, Long id, String urlIdent) {
        auditLog("staging-post-url-update", "id={}, urlIdent={}", username, stopWatch, id, urlIdent);
    }

    public void logStagingPostUrlsDelete(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-urls-delete", "id={}, urlIdent={}", username, stopWatch, id);
    }

    public void logStagingPostUrlDelete(String username, StopWatch stopWatch, Long id, String urlIdent) {
        auditLog("staging-post-url-delete", "id={}, urlIdent={}", username, stopWatch, id, urlIdent);
    }

    public void logStagingPostAddContributor(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-add-contributor", "id={}", username, stopWatch, id);
    }

    public void logStagingPostContributorsFetch(String username, StopWatch stopWatch, Long id, int contributorCt) {
        auditLog("staging-post-contributors-fetch", "id={}, contributorCt={}", username, stopWatch, id, contributorCt);
    }

    public void logStagingPostContributorFetch(String username, StopWatch stopWatch, Long id, String contributorIdent) {
        auditLog("staging-post-contributor-fetch", "id={}, contributorIdent={}", username, stopWatch, id, contributorIdent);
    }

    public void logStagingPostContributorsUpdate(String username, StopWatch stopWatch, Long id, int contributorCt) {
        auditLog("staging-post-contributors-update", "id={}, contributorCt={}", username, stopWatch, id, contributorCt);
    }

    public void logStagingPostContributorUpdate(String username, StopWatch stopWatch, Long id, String contributorIdent) {
        auditLog("staging-post-contributor-update", "id={}, contributorIdent={}", username, stopWatch, id, contributorIdent);
    }

    public void logStagingPostContributorsDelete(String username, StopWatch stopWatch, Long postId) {
        auditLog("staging-post-contributors-delete", "id={}", username, stopWatch, postId);
    }

    public void logStagingPostContributorDelete(String username, StopWatch stopWatch, Long id, String contributorIdent) {
        auditLog("staging-post-contributor-delete", "id={}, contributorIdent={}", username, stopWatch, id, contributorIdent);
    }

    public void logStagingPostAddAuthor(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-add-author", "id={}", username, stopWatch, id);
    }

    public void logStagingPostAuthorsFetch(String username, StopWatch stopWatch, Long id, int authorCt) {
        auditLog("staging-post-authors-fetch", "id={}, authorCt={}", username, stopWatch, id, authorCt);
    }

    public void logStagingPostAuthorFetch(String username, StopWatch stopWatch, Long id, String authorIdent) {
        auditLog("staging-post-author-fetch", "id={}, authorIdent={}", username, stopWatch, id, authorIdent);
    }

    public void logStagingPostAuthorsUpdate(String username, StopWatch stopWatch, Long id, int authorCt) {
        auditLog("staging-post-authors-update", "id={}, authorCt={}", username, stopWatch, id, authorCt);
    }

    public void logStagingPostAuthorUpdate(String username, StopWatch stopWatch, Long id, String authorIdent) {
        auditLog("staging-post-author-update", "id={}, authorIdent={}", username, stopWatch, id, authorIdent);
    }

    public void logStagingPostAuthorsDelete(String username, StopWatch stopWatch, Long postId) {
        auditLog("staging-post-authors-delete", "id={}", username, stopWatch, postId);
    }

    public void logStagingPostAuthorDelete(String username, StopWatch stopWatch, Long id, String authorIdent) {
        auditLog("staging-post-author-delete", "id={}, authorIdent={}", username, stopWatch, id, authorIdent);
    }

    public void logStagingPostAddEnclosure(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-add-enclosure", "id={}", username, stopWatch, id);
    }

    public void logStagingPostEnclosuresFetch(String username, StopWatch stopWatch, Long id, int enclosureCt) {
        auditLog("staging-post-enclosures-fetch", "id={}, enclosureCt={}", username, stopWatch, id, enclosureCt);
    }

    public void logStagingPostEnclosureFetch(String username, StopWatch stopWatch, Long id, String enclosureIdent) {
        auditLog("staging-post-enclosure-fetch", "id={}, enclosureIdent={}", username, stopWatch, id, enclosureIdent);
    }

    public void logStagingPostEnclosuresUpdate(String username, StopWatch stopWatch, Long id, int enclosureCt) {
        auditLog("staging-post-enclosures-update", "id={}, enclosureCt={}", username, stopWatch, id, enclosureCt);
    }

    public void logStagingPostEnclosureUpdate(String username, StopWatch stopWatch, Long id, String enclosureIdent) {
        auditLog("staging-post-enclosure-update", "id={}, enclosureIdent={}", username, stopWatch, id, enclosureIdent);
    }

    public void logStagingPostEnclosuresDelete(String username, StopWatch stopWatch, Long postId) {
        auditLog("staging-post-enclosures-delete", "id={}", username, stopWatch, postId);
    }

    public void logStagingPostEnclosureDelete(String username, StopWatch stopWatch, Long id, String enclosureIdent) {
        auditLog("staging-post-enclosure-delete", "id={}, enclosureIdent={}", username, stopWatch, id, enclosureIdent);
    }

    public void logPasswordResetInit(String username, StopWatch stopWatch) {
        auditLog("password-reset-init", null, username, stopWatch);
    }

    public void logPasswordResetContinue(String username, StopWatch stopWatch) {
        auditLog("password-reset-continue", null, username, stopWatch);
    }

    public void logPasswordResetFinalize(String username, StopWatch stopWatch) {
        auditLog("password-reset-finalize", null, username, stopWatch);
    }

    public void logApiKeyRecoveryInit(String username, StopWatch stopWatch) {
        auditLog("api-key-recovery-init", null, username, stopWatch);
    }

    public void logUserRegistration(String username, StopWatch stopWatch) {
        auditLog("user-registration", null, username, stopWatch);
    }

    public void logUserVerification(String username, StopWatch stopWatch) {
        auditLog("user-verification", null, username, stopWatch);
    }

    public void logUserDeregistration(String username, StopWatch stopWatch) {
        auditLog("user-deregistration", null, username, stopWatch);
    }

    public void logUserUpdate(User user, StopWatch stopWatch) {
        auditLog("user-update", null, user.getUsername(), stopWatch);
    }

    //

    private static void auditLog(String logTag, String formatStr, String username, StopWatch stopWatch, Object... args) {
        String fullFormatStr = "eventType={}, username={}, startTime={}, endTime={}, duration={}";
        if (isNotEmpty(formatStr)) {
            fullFormatStr += (", " + formatStr);
        }
        Object[] allArgs = new Object[args.length + 5];
        allArgs[0] = logTag;
        allArgs[1] = username;
        allArgs[2] = stopWatch.getStartTime();
        allArgs[3] = stopWatch.getStopTime();
        allArgs[4] = stopWatch.getTime();
        arraycopy(args, 0, allArgs, 5, args.length);
        log.info(fullFormatStr, allArgs);
    }
}
