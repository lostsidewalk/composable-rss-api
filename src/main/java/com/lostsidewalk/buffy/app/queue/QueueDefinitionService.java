package com.lostsidewalk.buffy.app.queue;

import com.google.gson.Gson;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.response.ExportConfigDTO;
import com.lostsidewalk.buffy.app.model.request.QueueStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.QueueConfigRequest;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import com.lostsidewalk.buffy.queue.QueueDefinition.QueueStatus;
import com.lostsidewalk.buffy.queue.QueueDefinitionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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

    public Long createQueue(String username, QueueConfigRequest queueConfigRequest) throws DataAccessException, DataUpdateException {
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

    public QueueDefinition updateQueue(String username, Long id, QueueConfigRequest queueConfigRequest) throws DataAccessException, DataUpdateException {
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

    public void updateQueueStatus(String username, Long id, QueueStatusUpdateRequest queueStatusUpdateRequest) throws DataAccessException, DataUpdateException {
        QueueStatus newStatus = null;
        if (isNotBlank(queueStatusUpdateRequest.getNewStatus())) {
            newStatus = QueueStatus.valueOf(queueStatusUpdateRequest.getNewStatus());
        }
        //
        // perform the update
        //
        queueDefinitionDao.updateQueueStatus(username, id, newStatus);
    }

    public String updateQueueIdent(String username, Long id, String ident) throws DataAccessException, DataUpdateException {
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

    private String getLanguage(String lang) {
        return "en-US";
    }

    private static final Gson GSON = new Gson();

    private Serializable serializeExportConfig(QueueConfigRequest queueConfigRequest) {
        ExportConfigDTO e = queueConfigRequest.getExportConfig();
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
}
