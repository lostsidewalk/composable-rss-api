package com.lostsidewalk.buffy.app.mail;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.auth.ApiKey;
import com.lostsidewalk.buffy.auth.User;
import com.lostsidewalk.buffy.auth.UserDao;
import com.lostsidewalk.buffy.app.audit.MailException;
import com.lostsidewalk.buffy.app.model.AppToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
public class MailService {

    @Autowired
    UserDao userDao;

    @Autowired
    MailConfigProps configProps;

    @Autowired
    JavaMailSender mailSender;

    //
    // password reset
    //

    public final void sendPasswordResetEmail(String username, AppToken passwordResetToken) throws MailException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String emailAddress = user.getEmailAddress();
        if (isBlank(emailAddress)) {
            throw new MailException("Unable to send password reset email because user has no known email address");
        }
        try {
            generatePasswordResetEmail(username, emailAddress, passwordResetToken);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new MailException("Unable to send password reset email due to: " + message);
        }
    }

    private void generatePasswordResetEmail(String username, String emailAddress, AppToken passwordResetToken) {
        String from = configProps.getPwResetEmailSender();
        String subject = configProps.getPwResetEmailSubject();
        String pwResetEmailUrlTemplate = configProps.getPwResetEmailUrlTemplate();
        String pwResetUrl = String.format(pwResetEmailUrlTemplate, passwordResetToken.authToken);
        String pwResetEmailBodyTemplate = configProps.getPwResetEmailBodyTemplate();
        String body = String.format(pwResetEmailBodyTemplate, username, pwResetUrl);
        sendSimpleMessage(from, emailAddress, subject, body);
    }

    //
    // verification
    //

    public final void sendVerificationEmail(String username, AppToken verificationToken, ApiKey apiKey) throws MailException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String emailAddress = user.getEmailAddress();
        if (isBlank(emailAddress)) {
            throw new MailException("Unable to send verification email because user has no known email address");
        }
        generateVerificationEmail(username, emailAddress, verificationToken, apiKey);
    }

    private void generateVerificationEmail(String username, String emailAddress, AppToken verificationToken, ApiKey apiKey) {
        String from = configProps.getVerificationEmailSender();
        String subject = configProps.getVerificationEmailSubject();
        String verificationEmailUrlTemplate = configProps.getVerificationEmailUrlTemplate();
        String verificationUrl = String.format(verificationEmailUrlTemplate, verificationToken.authToken);
        String verificationEmailBodyTemplate = configProps.getVerificationEmailBodyTemplate();
        String key = apiKey.getApiKey();
        String secret = apiKey.getApiSecret();
        String body = String.format(verificationEmailBodyTemplate, username, verificationUrl, key, secret);
        sendSimpleMessage(from, emailAddress, subject, body);
    }

    //
    // API key recovery
    //

    public final void sendApiKeyRecoveryEmail(String username, ApiKey apiKey) throws DataAccessException, MailException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String emailAddress = user.getEmailAddress();
        if (isBlank(emailAddress)) {
            throw new MailException("Unable to send API recovery email because user has no known email address");
        }
        try {
            generateApiKeyRecoveryEmail(username, emailAddress, apiKey);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new MailException("Unable to send API key recovery email due to: " + message);
        }
    }

    private void generateApiKeyRecoveryEmail(String username, String emailAddress, ApiKey apiKey) {
        String from = configProps.getApiKeyRecoveryEmailSender();
        String subject = configProps.getApiKeyRecoveryEmailSubject();
        String apiKeyRecoveryEmailBodyTemplate = configProps.getApiKeyRecoveryEmailBodyTemplate();
        String key = apiKey.getApiKey();
        String secret = apiKey.getApiSecret();
        String body = String.format(apiKeyRecoveryEmailBodyTemplate, username, key, secret);
        sendSimpleMessage(from, emailAddress, subject, body);
    }

    //
    //
    //

    private void sendSimpleMessage(String from, String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        if (configProps.getLogMessages()) {
            log.info("Sending mail message={}", message);
        }

        if (configProps.getDisabled()) {
            log.warn("Mail sender is administratively disabled");
            return;
        }

        mailSender.send(message);
    }

    @Override
    public final String toString() {
        return "MailService{" +
                "userDao=" + userDao +
                ", configProps=" + configProps +
                ", mailSender=" + mailSender +
                '}';
    }
}
