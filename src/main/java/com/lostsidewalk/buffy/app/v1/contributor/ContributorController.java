package com.lostsidewalk.buffy.app.v1.contributor;

import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.paginator.Paginator;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller class for managing contributor-related operations.
 * <p>
 * This controller provides endpoints for managing contributor objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
class ContributorController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    Paginator paginator;

    @Autowired
    Validator validator;
}
