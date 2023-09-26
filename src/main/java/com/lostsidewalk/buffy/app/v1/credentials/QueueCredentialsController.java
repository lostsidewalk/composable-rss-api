package com.lostsidewalk.buffy.app.v1.credentials;

import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.credentials.QueueCredentialsService;
import com.lostsidewalk.buffy.app.etag.ETagger;
import com.lostsidewalk.buffy.app.paginator.Paginator;
import com.lostsidewalk.buffy.app.queue.QueueDefinitionService;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller class for managing credential-related operations.
 *
 * This controller provides endpoints for managing credential objects within queues. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
class QueueCredentialsController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    QueueCredentialsService queueCredentialsService;

    @Autowired
    QueueDefinitionService queueDefinitionService;

    @Autowired
    Paginator paginator;

    @Autowired
    Validator validator;

    @Autowired
    ETagger eTagger;
}
