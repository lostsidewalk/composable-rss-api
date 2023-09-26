package com.lostsidewalk.buffy.app.v1.options;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lostsidewalk.buffy.PostPublisher;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.v1.response.ExportConfigDTO;
import com.lostsidewalk.buffy.app.model.v1.response.QueueDTO;
import com.lostsidewalk.buffy.app.queue.QueueDefinitionService;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;

/**
 * Controller class for managing queue export options-related operations.
 *
 * This controller provides endpoints for managing export options objects within queues. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
public class QueueOptionsController {

    protected static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .create();

    @Autowired
    AppLogService appLogService;

    @Autowired
    QueueDefinitionService queueDefinitionService;

    @Autowired
    PostPublisher postPublisher;

    @Autowired
    Validator validator;

    protected static QueueDTO prepareQueueDTO(QueueDefinition q) {
        Serializable exportConfig = q.getExportConfig();
        return QueueDTO.from(q.getId(),
                q.getIdent(),
                q.getTitle(),
                q.getDescription(),
                q.getGenerator(),
                q.getTransportIdent(),
                q.getIsAuthenticated(),
                exportConfig == null ? null : GSON.fromJson(GSON.toJson(exportConfig), ExportConfigDTO.class),
                q.getCopyright(),
                q.getLanguage(),
                q.getQueueImgSrc(),
                q.getLastDeployed(),
                q.getIsAuthenticated()
        );
    }
}
