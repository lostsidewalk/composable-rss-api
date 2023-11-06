package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.request.SettingsUpdateRequest;
import com.lostsidewalk.buffy.app.model.response.SettingsResponse;
import com.lostsidewalk.buffy.app.settings.SettingsService;
import com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.ResponseMessage;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@Validated
public class SettingsController {

    @Autowired
    SettingsService settingsService;

    @Autowired
    Validator validator;

    @SuppressWarnings("DesignForExtension")
    @PreAuthorize("hasAuthority('ROLE_UNVERIFIED')")
    @GetMapping(value = "/settings", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<SettingsResponse> getSettings(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        StopWatch stopWatch = StopWatch.createStarted();
        SettingsResponse settingsResponse = settingsService.getFrameworkConfig(username);
        stopWatch.stop();
        AppLogService.logSettingsFetch(username, stopWatch);
        //
        if (settingsResponse != null) {
            validator.validate(settingsResponse);
        }
        return ok(settingsResponse);
    }

    @SuppressWarnings("DesignForExtension")
    @PreAuthorize("hasAuthority('ROLE_UNVERIFIED')")
    @Transactional
    @PutMapping(value = "/settings", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseMessage> updateSettings(@Valid @RequestBody SettingsUpdateRequest settingsUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException, DataConflictException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        StopWatch stopWatch = StopWatch.createStarted();
        settingsService.updateFrameworkConfig(username, settingsUpdateRequest);
        stopWatch.stop();
        AppLogService.logSettingsUpdate(username, stopWatch, settingsUpdateRequest);
        ResponseEntity.BodyBuilder responseBuilder = ok();
        ResponseMessage body = buildResponseMessage(EMPTY);
        return responseBuilder.body(body);
    }

    @Override
    public final String toString() {
        return "SettingsController{" +
                "settingsService=" + settingsService +
                ", validator=" + validator +
                '}';
    }
}
