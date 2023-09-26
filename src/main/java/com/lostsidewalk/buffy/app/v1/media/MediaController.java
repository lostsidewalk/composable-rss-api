package com.lostsidewalk.buffy.app.v1.media;

import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller class for managing media-related operations.
 *
 * This controller provides endpoints for managing media objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
class MediaController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    Validator validator;
}
