package com.lostsidewalk.buffy.app.audit;

import com.lostsidewalk.buffy.app.model.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.QueueStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.SettingsUpdateRequest;
import com.lostsidewalk.buffy.auth.User;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.System.arraycopy;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j(topic = "appLog")
@Service
public class AppLogService {

    public void logQueueFetch(String username, StopWatch stopWatch, int queueCt) {
        auditLog("queue-fetch", "queueCt={}", username, stopWatch, queueCt);
    }

    public void logQueueStatusUpdate(String username, StopWatch stopWatch, Long id, QueueStatusUpdateRequest queueStatusUpdateRequest, int rowsUpdated) {
        auditLog("queue-status-update", "id={}, feedStatusUpdateRequest={}, rowsUpdated={}", username, stopWatch, id, queueStatusUpdateRequest, rowsUpdated);
    }

    public void logQueueUpdate(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-update", "id={}", username, stopWatch, id);
    }

    public void logQueueCreate(String username, StopWatch stopWatch, int length, int size) {
        auditLog("queue-create", "length={}, size={}", username, stopWatch, length, size);
    }

    public void logQueueDelete(String username, StopWatch stopWatch, int deleteCt) {
        auditLog("queue-delete", "deleteCt={}", username, stopWatch, deleteCt);
    }

    public void logQueueAttributeFetch(String username, StopWatch stopWatch, Long id, String attrName) {
        auditLog("queue-attribute-fetch", "id={}, attrName={}", username, stopWatch, id, attrName);
    }

    public void logQueueAttributeUpdate(String username, StopWatch stopWatch, Long id, String attrName) {
        auditLog("queue-attribute-update", "id={}, attrName={}", username, stopWatch, id, attrName);
    }

    public void logQueueAttributeDelete(String username, StopWatch stopWatch, Long id, String attrName) {
        auditLog("queue-attribute-delete", "id={}, attrName={}", username, stopWatch, id, attrName);
    }

    public void logQueueCredentialCreate(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-credential-create", "id={}", username, stopWatch, id);
    }

    public void logQueueCredentialsFetch(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-credentials-fetch", "id={}", username, stopWatch, id);
    }

    public void logQueueCredentialUpdate(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-credential-update", "id={}", username, stopWatch, id);
    }

    public void logQueueCredentialDelete(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-credential-delete", "id={}", username, stopWatch, id);
    }

    public void logCheckoutSessionCreate(String username, StopWatch stopWatch) {
        auditLog("checkout-session-create", null, username, stopWatch);
    }

    public void logSubscriptionFetch(String username, StopWatch stopWatch, int subscriptionCt) {
        auditLog("subscription-fetch", "subscriptionCt={}", username, stopWatch, subscriptionCt);
    }

    public void logSubscriptionCancel(String username, StopWatch stopWatch) {
        auditLog("subscription-cancel", null, username, stopWatch);
    }

    public void logSubscriptionResume(String username, StopWatch stopWatch) {
        auditLog("subscription-resume", null, username, stopWatch);
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

    public void logDisplaySettingsFetch(String username, StopWatch stopWatch) {
        auditLog("display-settings-fetch", null, username, stopWatch);
    }

    public void logDisplaySettingsUpdate(String username, StopWatch stopWatch) {
        auditLog("display-settings-update", "displaySettingsUpdateRequest={}", username, stopWatch);
    }

    public void logStagingPostFetch(String username, StopWatch stopWatch, int queueIdCt, int stagingPostCt) {
        auditLog("staging-post-fetch", "queueIdCt={}, stagingPostCt={}, queryMetricsCt={}", username, stopWatch, queueIdCt, stagingPostCt);
    }

    public void logStagingPostCreate(String username, StopWatch stopWatch, int postConfigRequestCt, int stagingPostCt) {
        auditLog("staging-post-create", "queueIdCt={}, postConfigRequestCt={}, stagingPostCt={}", username, stopWatch, postConfigRequestCt, stagingPostCt);
    }

    public void logStagingPostUpdate(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-update", "id={}", username, stopWatch, id);
    }

    public void logStagingPostDelete(String username, StopWatch stopWatch, int deleteCt) {
        auditLog("staging-post-delete", "deleteCt={}", username, stopWatch, deleteCt);
    }

    public void logStagingPostPubStatusUpdate(String username, StopWatch stopWatch, Long id, PostStatusUpdateRequest postStatusUpdateRequest, List<PubResult> publicationResults) {
        auditLog("staging-post-pub-status-update", "id={}, postStatusUpdateRequest={}, publicationResults={}", username, stopWatch, id, postStatusUpdateRequest, publicationResults);
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
        auditLog("staging-post-content-fetch", "id={}, contentCt={}", username, stopWatch, id, contentCt);
    }

    public void logStagingPostContentUpdate(String username, StopWatch stopWatch, Long id, int contentIdx) {
        auditLog("staging-post-content-update", "id={}, contentIdx={}", username, stopWatch, id, contentIdx);
    }

    public void logStagingPostContentDelete(String username, StopWatch stopWatch, Long id, int contentIdx) {
        auditLog("staging-post-content-delete", "id={}, contentIdx={}", username, stopWatch, id, contentIdx);
    }

    public void logStagingPostAddUrl(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-add-url", "id={}", username, stopWatch, id);
    }

    public void logStagingPostUrlsFetch(String username, StopWatch stopWatch, Long id, int urlCt) {
        auditLog("staging-post-url-fetch", "id={}, urlCt={}", username, stopWatch, id, urlCt);
    }

    public void logStagingPostUrlUpdate(String username, StopWatch stopWatch, Long id, int urlIdx) {
        auditLog("staging-post-url-update", "id={}, urlIdx={}", username, stopWatch, id, urlIdx);
    }

    public void logStagingPostUrlDelete(String username, StopWatch stopWatch, Long id, int urlIdx) {
        auditLog("staging-post-url-delete", "id={}, urlIdx={}", username, stopWatch, id, urlIdx);
    }

    public void logStagingPostAddContributor(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-add-contributor", "id={}", username, stopWatch, id);
    }

    public void logStagingPostContributorsFetch(String username, StopWatch stopWatch, Long id, int contributorCt) {
        auditLog("staging-post-contributor-fetch", "id={}, contributorCt={}", username, stopWatch, id, contributorCt);
    }

    public void logStagingPostContributorUpdate(String username, StopWatch stopWatch, Long id, int contributorIdx) {
        auditLog("staging-post-contributor-update", "id={}, contributorIdx={}", username, stopWatch, id, contributorIdx);
    }

    public void logStagingPostContributorDelete(String username, StopWatch stopWatch, Long id, int contributorIdx) {
        auditLog("staging-post-contributor-delete", "id={}, contributorIdx={}", username, stopWatch, id, contributorIdx);
    }

    public void logStagingPostAddAuthor(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-add-author", "id={}", username, stopWatch, id);
    }

    public void logStagingPostAuthorsFetch(String username, StopWatch stopWatch, Long id, int authorCt) {
        auditLog("staging-post-author-fetch", "id={}, authorCt={}", username, stopWatch, id, authorCt);
    }

    public void logStagingPostAuthorUpdate(String username, StopWatch stopWatch, Long id, int authorIdx) {
        auditLog("staging-post-author-update", "id={}, authorIdx={}", username, stopWatch, id, authorIdx);
    }

    public void logStagingPostAuthorDelete(String username, StopWatch stopWatch, Long id, int authorIdx) {
        auditLog("staging-post-author-delete", "id={}, authorIdx={}", username, stopWatch, id, authorIdx);
    }

    public void logStagingPostAddEnclosure(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-add-enclosure", "id={}", username, stopWatch, id);
    }

    public void logStagingPostEnclosuresFetch(String username, StopWatch stopWatch, Long id, int enclosureCt) {
        auditLog("staging-post-enclosure-fetch", "id={}, enclosureCt={}", username, stopWatch, id, enclosureCt);
    }

    public void logStagingPostEnclosureUpdate(String username, StopWatch stopWatch, Long id, int enclosureIdx) {
        auditLog("staging-post-enclosure-update", "id={}, enclosureIdx={}", username, stopWatch, id, enclosureIdx);
    }

    public void logStagingPostEnclosureDelete(String username, StopWatch stopWatch, Long id, int enclosureIdx) {
        auditLog("staging-post-enclosure-delete", "id={}, enclosureIdx={}", username, stopWatch, id, enclosureIdx);
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

    public void logCustomerCreated(User user, String emailAddress, String customerId) {
        log.info("Processed customer-created event: Setting customer for userId={}, emailAddress={}, customerId={}",
                user.getId(), emailAddress, customerId);
    }

    public void logCustomerSubscriptionDeleted(User user, String customerId) {
        log.info("Customer subscription deleted userId={}, customerId={}", user.getId(), customerId);
    }

    public void logCustomerSubscriptionUpdated(User user, String customerId, String subStatus) {
        log.info("Processed customer-subscription-updated event: Updating subscription status to subStatus={} for customerId={}, userId={}, emailAddress={}",
                subStatus, customerId, user.getId(), user.getEmailAddress());
    }

    public void logInvoicePaid(User user, String emailAddress) {
        log.info("Processed invoice-paid event: Updating subscription expiration to expDate={} for userId={}, emailAddress={}, status={}",
                user.getSubscriptionExpDate(), user.getId(), emailAddress, user.getSubscriptionStatus());
    }
}
