package com.lostsidewalk.buffy.app.v1.content;

import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.paginator.Paginator;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller class for managing content-related operations.
 * <p>
 * This controller provides endpoints for managing content objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
class ContentController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    Paginator paginator;

    @Autowired
    Validator validator;
}
