package com.lostsidewalk.buffy.app.v1.enclosure;

import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.paginator.Paginator;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller class for managing enclosure-related operations.
 *
 * This controller provides endpoints for managing enclosure objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
class EnclosureController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    Paginator paginator;

    @Autowired
    Validator validator;
}
