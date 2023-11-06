package com.lostsidewalk.buffy.app.queue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.Atom10Config;
import com.lostsidewalk.buffy.app.model.v1.RSS20Config;
import com.lostsidewalk.buffy.app.model.v1.request.ExportConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.request.QueueConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.QueueStatusResponse;
import com.lostsidewalk.buffy.post.StagingPost.PostPubStatus;
import com.lostsidewalk.buffy.post.StagingPostDao;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import com.lostsidewalk.buffy.queue.QueueDefinitionDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;


@Slf4j
@Service
public class QueueDefinitionService {

    @Autowired
    QueueDefinitionDao queueDefinitionDao;

    @Autowired
    StagingPostDao stagingPostDao;

    public final QueueDefinition findByQueueId(String username, Long id) throws DataAccessException {
        return queueDefinitionDao.findByQueueId(username, id);
    }

    public final List<QueueDefinition> findByUser(String username) throws DataAccessException {
        List<QueueDefinition> list = queueDefinitionDao.findByUser(username);
        if (list != null) {
            return list;
        }
        return emptyList();
    }

    @SuppressWarnings("NestedMethodCall")
    public final Long createQueue(String username, QueueConfigRequest queueConfigRequest) throws DataAccessException, DataUpdateException, DataConflictException {
        Serializable newTransportIdent = getNewTransportIdent();
        QueueDefinition newQueueDefinition = QueueDefinition.from(
                queueConfigRequest.getIdent(),
                queueConfigRequest.getTitle(),
                queueConfigRequest.getDescription(),
                queueConfigRequest.getGenerator(),
                newTransportIdent.toString(),
                username,
                serializeExportConfig(queueConfigRequest),
                queueConfigRequest.getCopyright(),
                getLanguage(queueConfigRequest.getLanguage()),
                queueConfigRequest.getImgSrc(),
                false
        );
        return queueDefinitionDao.add(newQueueDefinition);
    }

    private static Serializable getNewTransportIdent() {
        return UUID.randomUUID().toString();
    }

    @SuppressWarnings("NestedMethodCall")
    public final QueueDefinition updateQueue(String username, Long id, QueueConfigRequest queueConfigRequest, Boolean mergeUpdate) throws DataAccessException, DataUpdateException, DataConflictException {
        queueDefinitionDao.updateQueue(username, id,
                queueConfigRequest.getIdent(),
                queueConfigRequest.getDescription(),
                queueConfigRequest.getTitle(),
                queueConfigRequest.getGenerator(),
                serializeExportConfig(queueConfigRequest),
                queueConfigRequest.getCopyright(),
                getLanguage(queueConfigRequest.getLanguage()),
                queueConfigRequest.getImgSrc(),
                false
        );
        return queueDefinitionDao.findByQueueId(username, id);
    }

    public final QueueDefinition updateQueueIdent(String username, Long id, String ident) throws DataAccessException, DataUpdateException, DataConflictException {
        queueDefinitionDao.updateQueueIdent(username, id, ident);
        return queueDefinitionDao.findByQueueId(username, id);
    }

    public final QueueDefinition updateQueueTitle(String username, Long id, String title) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateQueueTitle(username, id, title);
        return queueDefinitionDao.findByQueueId(username, id);
    }

    public final QueueDefinition updateQueueDescription(String username, Long id, String description) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateQueueDescription(username, id, description);
        return queueDefinitionDao.findByQueueId(username, id);
    }

    public final QueueDefinition updateQueueGenerator(String username, Long id, String generator) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateQueueGenerator(username, id, generator);
        return queueDefinitionDao.findByQueueId(username, id);
    }

    public final QueueDefinition updateQueueCopyright(String username, Long id, String copyright) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateCopyright(username, id, copyright);
        return queueDefinitionDao.findByQueueId(username, id);
    }

    public final QueueDefinition updateQueueLanguage(String username, Long id, String language) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateLanguage(username, id, language);
        return queueDefinitionDao.findByQueueId(username, id);
    }

    public final QueueDefinition updateQueueAuthenticationRequirement(String username, Long id, Boolean isRequired) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateQueueAuthenticationRequirement(username, id, isRequired);
        return queueDefinitionDao.findByQueueId(username, id);
    }

    public final QueueDefinition updateQueueImageSource(String username, Long id, String queueImgSource) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateQueueImageSource(username, id, queueImgSource);
        return queueDefinitionDao.findByQueueId(username, id);
    }

    public final QueueDefinition updateExportConfig(String username, Long id, ExportConfigRequest exportConfigRequest, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        String exportConfig = GSON.toJson(exportConfigRequest);
        queueDefinitionDao.updateExportConfig(username, id, exportConfig);
        return queueDefinitionDao.findByQueueId(username, id);
    }

    @SuppressWarnings("NestedMethodCall")
    public final QueueDefinition updateAtomExportConfig(String username, Long id, Atom10Config atomConfig, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        QueueDefinition queueDefinition = queueDefinitionDao.findByQueueId(username, id);
        JsonObject exportConfig;
        Serializable serializable = queueDefinition.getExportConfig();
        if (serializable != null) {
            exportConfig = GSON.fromJson(serializable.toString(), JsonObject.class);
        } else {
            exportConfig = new JsonObject();
        }
        exportConfig.add("atomConfig", GSON.toJsonTree(atomConfig));
        queueDefinitionDao.updateExportConfig(username, id, exportConfig.toString());
        return queueDefinitionDao.findByQueueId(username, id);
    }

    @SuppressWarnings("NestedMethodCall")
    public final QueueDefinition updateRssExportConfig(String username, Long id, RSS20Config rssConfig, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        QueueDefinition queueDefinition = queueDefinitionDao.findByQueueId(username, id);
        JsonObject exportConfig;
        Serializable serializable = queueDefinition.getExportConfig();
        if (serializable != null) {
            exportConfig = GSON.fromJson(serializable.toString(), JsonObject.class);
        } else {
            exportConfig = new JsonObject();
        }
        exportConfig.add("rssConfig", GSON.toJsonTree(rssConfig));
        queueDefinitionDao.updateExportConfig(username, id, exportConfig.toString());
        return queueDefinitionDao.findByQueueId(username, id);
    }

    // TODO: implement this method
    private String getLanguage(String lang) {
        return "en-US";
    }

    private static final Gson GSON = new Gson();

    private static Serializable serializeExportConfig(QueueConfigRequest queueConfigRequest) {
        ExportConfigRequest e = queueConfigRequest.getOptions();
        return e == null ? null : GSON.toJson(e);
    }

    public final void deleteById(String username, Long id) throws DataAccessException, DataUpdateException {
        // delete this queue
        queueDefinitionDao.deleteById(username, id);
    }

    public final void clearQueueTitle(String username, Long id) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.clearQueueTitle(username, id);
    }

    public final void clearQueueDescription(String username, Long id) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.clearQueueDescription(username, id);
    }

    public final void clearQueueGenerator(String username, Long id) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.clearQueueGenerator(username, id);
    }

    public final void clearQueueCopyright(String username, Long id) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.clearQueueCopyright(username, id);
    }

    public final void clearQueueImageSource(String username, Long id) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.clearQueueImageSource(username, id);
    }

    public final void clearExportConfig(String username, Long id) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.clearExportConfig(username, id);
    }

    @SuppressWarnings("NestedMethodCall")
    public final void clearAtomExportConfig(String username, Long id) throws DataAccessException, DataUpdateException {
        QueueDefinition queueDefinition = queueDefinitionDao.findByQueueId(username, id);
        Serializable serializable = queueDefinition.getExportConfig();
        if (serializable != null) {
            JsonObject exportConfig = GSON.fromJson(serializable.toString(), JsonObject.class);
            if (exportConfig.has("atomConfig")) {
                exportConfig.remove("atomConfig");
                queueDefinitionDao.updateExportConfig(username, id, exportConfig.toString());
            }
        }
    }

    @SuppressWarnings("NestedMethodCall")
    public final void clearRssExportConfig(String username, Long id) throws DataAccessException, DataUpdateException {
        QueueDefinition queueDefinition = queueDefinitionDao.findByQueueId(username, id);
        Serializable serializable = queueDefinition.getExportConfig();
        if (serializable != null) {
            JsonObject exportConfig = GSON.fromJson(serializable.toString(), JsonObject.class);
            if (exportConfig.has("rssConfig")) {
                exportConfig.remove("rssConfig");
                queueDefinitionDao.updateExportConfig(username, id, exportConfig.toString());
            }
        }
    }

    public final long resolveQueueId(String username, String queueIdent) throws DataAccessException {
        return queueDefinitionDao.resolveId(username, queueIdent);
    }

    public final String resolveQueueIdent(String username, Long queueId) throws DataAccessException {
        return queueDefinitionDao.resolveIdent(username, queueId);
    }

    public final boolean isAutoDeploy(String username, long queueId) throws DataAccessException {
        QueueDefinition queueDefinition = queueDefinitionDao.findByQueueId(username, queueId);
        JsonObject exportConfig;
        Serializable serializable = queueDefinition.getExportConfig();
        if (serializable != null) {
            exportConfig = GSON.fromJson(serializable.toString(), JsonObject.class);
            if (exportConfig.has("autoDeploy")) {
                return exportConfig.get("autoDeploy").getAsBoolean();
            }
        }
        return false;
    }

    public final QueueStatusResponse checkStatus(String username, long queueId) throws DataAccessException {
        //
        int publishedCt = stagingPostDao.countPublishedByQueueId(username, queueId);
        //
        Map<PostPubStatus, Integer> countByStatus = stagingPostDao.countStatusByQueueId(username, queueId);
        //
        return QueueStatusResponse.from(publishedCt, countByStatus);
    }

    @Override
    public final String toString() {
        return "QueueDefinitionService{" +
                "queueDefinitionDao=" + queueDefinitionDao +
                ", stagingPostDao=" + stagingPostDao +
                '}';
    }
}
