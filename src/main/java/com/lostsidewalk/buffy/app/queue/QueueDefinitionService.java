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
import com.lostsidewalk.buffy.app.model.v1.response.ExportConfigDTO;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import com.lostsidewalk.buffy.queue.QueueDefinitionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

@Service
public class QueueDefinitionService {

    @Autowired
    QueueDefinitionDao queueDefinitionDao;

    public QueueDefinition findByQueueId(String username, Long id) throws DataAccessException {
        return queueDefinitionDao.findByQueueId(username, id);
    }

    public List<QueueDefinition> findByUser(String username) throws DataAccessException {
        List<QueueDefinition> list = queueDefinitionDao.findByUser(username);
        if (list != null) {
            return list;
        }
        return emptyList();
    }

    public Long createQueue(String username, QueueConfigRequest queueConfigRequest) throws DataAccessException, DataUpdateException, DataConflictException {
        QueueDefinition newQueueDefinition = QueueDefinition.from(
                queueConfigRequest.getIdent(),
                queueConfigRequest.getTitle(),
                queueConfigRequest.getDescription(),
                queueConfigRequest.getGenerator(),
                getNewTransportIdent().toString(),
                username,
                serializeExportConfig(queueConfigRequest),
                queueConfigRequest.getCopyright(),
                getLanguage(queueConfigRequest.getLanguage()),
                queueConfigRequest.getImgSrc(),
                false
            );
        return queueDefinitionDao.add(newQueueDefinition);
    }

    private Serializable getNewTransportIdent() {
        return UUID.randomUUID().toString();
    }

    public QueueDefinition updateQueue(String username, Long id, QueueConfigRequest queueConfigRequest, boolean mergeUpdate) throws DataAccessException, DataUpdateException, DataConflictException {
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

    public String updateQueueIdent(String username, Long id, String ident) throws DataAccessException, DataUpdateException, DataConflictException {
        queueDefinitionDao.updateQueueIdent(username, id, ident);
        return queueDefinitionDao.findByQueueId(username, id).getIdent();
    }

    public String updateQueueTitle(String username, Long id, String title) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateQueueTitle(username, id, title);
        return queueDefinitionDao.findByQueueId(username, id).getTitle();
    }

    public String updateQueueDescription(String username, Long id, String description) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateQueueDescription(username, id, description);
        return queueDefinitionDao.findByQueueId(username, id).getDescription();
    }

    public String updateQueueGenerator(String username, Long id, String generator) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateQueueGenerator(username, id, generator);
        return queueDefinitionDao.findByQueueId(username, id).getGenerator();
    }

    public String updateQueueCopyright(String username, Long id, String copyright) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateCopyright(username, id, copyright);
        return queueDefinitionDao.findByQueueId(username, id).getCopyright();
    }

    public String updateQueueLanguage(String username, Long id, String language) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateLanguage(username, id, language);
        return queueDefinitionDao.findByQueueId(username, id).getLanguage();
    }

    public Boolean updateQueueAuthenticationRequirement(String username, Long id, Boolean isRequired) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateQueueAuthenticationRequirement(username, id, isRequired);
        return queueDefinitionDao.findByQueueId(username, id).getIsAuthenticated();
    }

    public String updateQueueImageSource(String username, Long id, String queueImgSource) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateQueueImageSource(username, id, queueImgSource);
        return queueDefinitionDao.findByQueueId(username, id).getQueueImgSrc();
    }

    public Serializable updateExportConfig(String username, Long id, ExportConfigRequest exportConfigRequest, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateExportConfig(username, id, GSON.toJson(exportConfigRequest));
        return queueDefinitionDao.findByQueueId(username, id).getExportConfig();
    }

    public Atom10Config updateAtomExportConfig(String username, Long id, Atom10Config atomConfig, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        QueueDefinition queueDefinition = queueDefinitionDao.findByQueueId(username, id);
        JsonObject exportConfig;
        Serializable s = queueDefinition.getExportConfig();
        if (s != null) {
            exportConfig = GSON.fromJson(s.toString(), JsonObject.class);
        } else {
            exportConfig = new JsonObject();
        }
        exportConfig.add("atomConfig", GSON.toJsonTree(atomConfig));
        queueDefinitionDao.updateExportConfig(username, id, exportConfig.toString());
        ExportConfigDTO updatedExportConfig = GSON.fromJson(
                queueDefinitionDao.findByQueueId(username, id).getExportConfig().toString(),
                ExportConfigDTO.class);
        return updatedExportConfig.getAtomConfig();
    }

    public RSS20Config updateRssExportConfig(String username, Long id, RSS20Config rssConfig, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        QueueDefinition queueDefinition = queueDefinitionDao.findByQueueId(username, id);
        JsonObject exportConfig;
        Serializable s = queueDefinition.getExportConfig();
        if (s != null) {
            exportConfig = GSON.fromJson(s.toString(), JsonObject.class);
        } else {
            exportConfig = new JsonObject();
        }
        exportConfig.add("rssConfig", GSON.toJsonTree(rssConfig));
        queueDefinitionDao.updateExportConfig(username, id, exportConfig.toString());
        ExportConfigDTO updatedExportConfig = GSON.fromJson(
                queueDefinitionDao.findByQueueId(username, id).getExportConfig().toString(),
                ExportConfigDTO.class);
        return updatedExportConfig.getRssConfig();
    }

    private String getLanguage(String lang) {
        return "en-US";
    }

    private static final Gson GSON = new Gson();

    private Serializable serializeExportConfig(QueueConfigRequest queueConfigRequest) {
        ExportConfigRequest e = queueConfigRequest.getOptions();
        return e == null ? null : GSON.toJson(e);
    }

    public void deleteById(String username, Long id) throws DataAccessException, DataUpdateException {
        // delete this queue
        queueDefinitionDao.deleteById(username, id);
    }

    public void clearQueueTitle(String username, Long id) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.clearQueueTitle(username, id);
    }

    public void clearQueueDescription(String username, Long id) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.clearQueueDescription(username, id);
    }

    public void clearQueueGenerator(String username, Long id) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.clearQueueGenerator(username, id);
    }

    public void clearQueueCopyright(String username, Long id) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.clearQueueCopyright(username, id);
    }

    public void clearQueueImageSource(String username, Long id) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.clearQueueImageSource(username, id);
    }

    public void clearExportConfig(String username, Long id) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.clearExportConfig(username, id);
    }

    public void clearAtomExportConfig(String username, Long id) throws DataAccessException, DataUpdateException {
        QueueDefinition queueDefinition = queueDefinitionDao.findByQueueId(username, id);
        Serializable s = queueDefinition.getExportConfig();
        if (s != null) {
            JsonObject exportConfig = GSON.fromJson(s.toString(), JsonObject.class);
            if (exportConfig.has("atomConfig")) {
                exportConfig.remove("atomConfig");
                queueDefinitionDao.updateExportConfig(username, id, exportConfig.toString());
            }
        }
    }

    public void clearRssExportConfig(String username, Long id) throws DataAccessException, DataUpdateException {
        QueueDefinition queueDefinition = queueDefinitionDao.findByQueueId(username, id);
        Serializable s = queueDefinition.getExportConfig();
        if (s != null) {
            JsonObject exportConfig = GSON.fromJson(s.toString(), JsonObject.class);
            if (exportConfig.has("rssConfig")) {
                exportConfig.remove("rssConfig");
                queueDefinitionDao.updateExportConfig(username, id, exportConfig.toString());
            }
        }
    }

    public long resolveQueueId(String username, String queueIdent) throws DataAccessException {
        return queueDefinitionDao.resolveId(username, queueIdent);
    }

    public String resolveQueueIdent(String username, Long queueId) throws DataAccessException {
        return queueDefinitionDao.resolveIdent(username, queueId);
    }
}
