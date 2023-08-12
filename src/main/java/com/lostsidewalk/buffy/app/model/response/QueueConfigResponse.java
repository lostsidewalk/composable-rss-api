package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.queue.QueueDefinition;
import lombok.Data;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;

@Data
public class QueueConfigResponse {

    QueueDefinition queueDefinition;
    String queueImgSrc;

    private QueueConfigResponse(QueueDefinition queueDefinition) {
        this.queueDefinition = queueDefinition;
    }

    public static QueueConfigResponse from(QueueDefinition queueDefinition, byte[] feedImg) {
        QueueConfigResponse f = new QueueConfigResponse(queueDefinition);
        f.queueImgSrc = encodeBase64String(feedImg);

        return f;
    }
}
