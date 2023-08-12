package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.queue.QueueDefinition;
import lombok.Data;

import java.util.List;

@Data
public class QueueFetchResponse {
    List<QueueDefinition> queueDefinitions;

    private QueueFetchResponse(List<QueueDefinition> queueDefinitions)
    {
        this.queueDefinitions = queueDefinitions;
    }

    public static QueueFetchResponse from(List<QueueDefinition> queueDefinitions)
    {
        return new QueueFetchResponse(
                queueDefinitions
        );
    }
}
