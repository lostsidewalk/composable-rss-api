package com.lostsidewalk.buffy.app.audit;

import com.lostsidewalk.buffy.app.model.request.SettingsUpdateRequest;
import com.lostsidewalk.buffy.app.model.v1.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.v1.request.QueueStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.v1.response.QueueStatusResponse;
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

    public static void logQueueFetch(String username, StopWatch stopWatch, int queueCt) {
        auditLog("queue-fetch", "queueCt={}", username, stopWatch, queueCt);
    }

    public static void logQueueCreate(String username, StopWatch stopWatch, Map<String, PubResult> pubResults) {
        auditLog("queue-create", "pubResults={}", username, stopWatch, pubResults);
    }

    public static void logQueueDelete(String username, StopWatch stopWatch, int deleteCt) {
        auditLog("queue-delete", "deleteCt={}", username, stopWatch, deleteCt);
    }

    public static void logQueueAttributeFetch(String username, StopWatch stopWatch, Long id, String attrName) {
        auditLog("queue-attribute-fetch", "id={}, attrName={}", username, stopWatch, id, attrName);
    }

    public static void logQueueStatusFetch(String username, StopWatch stopWatch, long id, QueueStatusResponse queueStatus) {
        auditLog("queue-status-fetch", "id={}, queueStatus={}", username, stopWatch, id, queueStatus);
    }

    public static void logQueueStatusUpdate(String username, StopWatch stopWatch, long id, QueueStatusUpdateRequest queueStatusUpdateRequest) {
        auditLog("queue-status-update", "id={}, queueStatusUpdateRequest={}", username, stopWatch, id, queueStatusUpdateRequest);
    }

    // HERE
    public static void logQueueAttributeUpdate(String username, StopWatch updateTimer, StopWatch finalizeTimer, Long id, String attrName, Map<String, PubResult> pubResults) {
        auditLog("queue-attribute-update", "id={}, attrName={}, pubResults={}", username, updateTimer, finalizeTimer, id, attrName, pubResults);
    }

    // HERE
    public static void logQueueAttributeDelete(String username, StopWatch deleteTimer, StopWatch finalizeTimer, Long id, String attrName, Map<String, PubResult> pubResults) {
        auditLog("queue-attribute-delete", "id={}, attrName={}, pubResults={}", username, deleteTimer, finalizeTimer, id, attrName, pubResults);
    }

    public static void logQueueCredentialCreate(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-credential-create", "id={}", username, stopWatch, id);
    }

    public static void logQueueCredentialsFetch(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-credentials-fetch", "id={}", username, stopWatch, id);
    }

    public static void logQueueCredentialFetch(String username, StopWatch stopWatch, Long queueId, Long credentialId) {
        auditLog("queue-credential-fetch", "queueId={}, credentialId={}", username, stopWatch, queueId, credentialId);
    }

    public static void logQueueCredentialUpdate(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-credential-update", "id={}", username, stopWatch, id);
    }

    public static void logQueueCredentialsDelete(String username, StopWatch stopWatch, Long queueId) {
        auditLog("queue-credentials-delete", "queueId={}", username, stopWatch, queueId);
    }

    public static void logQueueCredentialDelete(String username, StopWatch stopWatch, Long id) {
        auditLog("queue-credential-delete", "id={}", username, stopWatch, id);
    }

    public static void logSettingsFetch(String username, StopWatch stopWatch) {
        auditLog("settings-fetch", null, username, stopWatch);
    }

    public static void logSettingsUpdate(String username, StopWatch stopWatch, SettingsUpdateRequest settingsUpdateRequest) {
        auditLog("settings-update", "settingsUpdateRequest={}", username, stopWatch, settingsUpdateRequest);
    }

    public static void logStagingPostFetch(String username, StopWatch stopWatch, int queueIdCt, int stagingPostCt) {
        auditLog("staging-post-fetch", "queueIdCt={}, stagingPostCt={}, queryMetricsCt={}", username, stopWatch, queueIdCt, stagingPostCt);
    }

    public static void logStagingPostCreate(String username, StopWatch stopWatch, int postConfigRequestCt, int stagingPostCt) {
        auditLog("staging-post-create", "queueIdCt={}, postConfigRequestCt={}, stagingPostCt={}", username, stopWatch, postConfigRequestCt, stagingPostCt);
    }

    public static void logStagingPostUpdate(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-update", "id={}", username, stopWatch, id);
    }

    public static void logStagingPostsDelete(String username, StopWatch stopWatch, Long queueId) {
        auditLog("staging-posts-delete", "queueId={}", username, stopWatch, queueId);
    }

    public static void logStagingPostDelete(String username, StopWatch stopWatch, Long id) {
        auditLog("staging-post-delete", "id={}", username, stopWatch, id);
    }

    public static void logStagingPostPubStatusUpdate(String username, StopWatch stopWatch, Long id, PostStatusUpdateRequest postStatusUpdateRequest, Map<String, PubResult> pubResults) {
        auditLog("staging-post-pub-status-update", "id={}, postStatusUpdateRequest={}, pubResults={}", username, stopWatch, id, postStatusUpdateRequest, pubResults);
    }

    public static void logStagingPostAttributeFetch(String username, StopWatch stopWatch, Long id, String attrName) {
        auditLog("staging-post-attribute-fetch", "id={}, attrName={}", username, stopWatch, id, attrName);
    }

    public static void logStagingPostAttributeUpdate(String username, StopWatch stopWatch, Long id, String attrName) {
        auditLog("staging-post-attribute-update", "id={}, attrName={}", username, stopWatch, id, attrName);
    }

    public static void logStagingPostAttributeDelete(String username, StopWatch stopWatch, Long id, String attrName) {
        auditLog("staging-post-attribute-delete", "id={}, attrName={}", username, stopWatch, id, attrName);
    }

    // HERE
    public static void logStagingPostAddEntity(String username, StopWatch createTimer, StopWatch finalizeTimer, Long id, String entityContext, String entityIdent) {
        auditLog("staging-post-add-" + entityContext, "id={}, entityIdent={}", username, createTimer, finalizeTimer, id, entityIdent);
    }

    // HERE
    public static void logStagingPostEntitiesFetch(String username, StopWatch retrieveTimer, StopWatch finalizeTimer, Long id, String entityContext, int entityCt) {
        auditLog("staging-post-" + entityContext + "-fetch", "id={}, entityCt={}", username, retrieveTimer, finalizeTimer, id, entityCt);
    }

    // HERE
    public static void logStagingPostEntityFetch(String username, StopWatch retrieveTimer, StopWatch finalizeTimer, Long id, String entityContext, String entityIdent) {
        auditLog("staging-post-" + entityContext + "-fetch", "id={}, entityIdent={}", username, retrieveTimer, finalizeTimer, id, entityIdent);
    }

    // HERE
    public static void logStagingPostEntitiesUpdate(String username, StopWatch updateTimer, StopWatch finalizeTimer, Long id, String entityContext, int entityCt) {
        auditLog("staging-post-" + entityContext + "-update", "id={}, entityCt={}", username, updateTimer, finalizeTimer, id, entityCt);
    }

    // HERE
    public static void logStagingPostEntityUpdate(String username, StopWatch updateTimer, StopWatch finalizeTimer, Long id, String entityContext, String entityIdent) {
        auditLog("staging-post-" + entityContext + "-update", "id={}, entityIdent={}", username, updateTimer, finalizeTimer, id, entityIdent);
    }

    // HERE
    public static void logStagingPostEntitiesDelete(String username, StopWatch deleteTimer, StopWatch finalizeTimer, Long postId, String entityContext) {
        auditLog("staging-post-" + entityContext + "-delete", "id={}", username, deleteTimer, finalizeTimer, postId);
    }

    // HERE
    public static void logStagingPostEntityDelete(String username, StopWatch deleteTimer, StopWatch finalizeTimer, Long id, String entityContext, String entityIdent) {
        auditLog("staging-post-" + entityContext + "-delete", "id={}, entityIdent={}", username, deleteTimer, finalizeTimer, id, entityIdent);
    }

    public static void logPasswordResetInit(String username, StopWatch stopWatch) {
        auditLog("password-reset-init", null, username, stopWatch);
    }

    public static void logPasswordResetContinue(String username, StopWatch stopWatch) {
        auditLog("password-reset-continue", null, username, stopWatch);
    }

    public static void logPasswordResetFinalize(String username, StopWatch stopWatch) {
        auditLog("password-reset-finalize", null, username, stopWatch);
    }

    public static void logApiKeyRecoveryInit(String username, StopWatch stopWatch) {
        auditLog("api-key-recovery-init", null, username, stopWatch);
    }

    public static void logUserRegistration(String username, StopWatch stopWatch) {
        auditLog("user-registration", null, username, stopWatch);
    }

    public static void logUserVerification(String username, StopWatch stopWatch) {
        auditLog("user-verification", null, username, stopWatch);
    }

    public static void logUserDeregistration(String username, StopWatch stopWatch) {
        auditLog("user-deregistration", null, username, stopWatch);
    }

    public static void logUserUpdate(User user, StopWatch stopWatch) {
        String username = user.getUsername();
        auditLog("user-update", null, username, stopWatch);
    }

    //

    @SuppressWarnings("OverloadedVarargsMethod")
    private static void auditLog(String logTag, String formatStr, String username, StopWatch startTimer, StopWatch finalizeTimer, Object... args) {
        String fullFormatStr = "eventType={}, username={}, startTime={}, endTime={}, duration={}";
        if (isNotEmpty(formatStr)) {
            fullFormatStr += (", " + formatStr);
        }
        Object[] allArgs = new Object[args.length + 5];
        allArgs[0] = logTag;
        allArgs[1] = username;
        allArgs[2] = startTimer.getStartTime();
        allArgs[3] = finalizeTimer.getStopTime();
        allArgs[4] = finalizeTimer.getStopTime() - startTimer.getStartTime();
        arraycopy(args, 0, allArgs, 5, args.length);
        log.info(fullFormatStr, allArgs);
    }

    @SuppressWarnings("OverloadedVarargsMethod")
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
