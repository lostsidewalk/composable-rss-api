package com.lostsidewalk.buffy.app.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
@ConfigurationProperties(prefix = "mail.config")
public class MailConfigProps {

    private String pwResetEmailSender;

    private String pwResetEmailSubject;

    private String pwResetEmailUrlTemplate;

    private String pwResetEmailBodyTemplate;

    private String apiKeyRecoveryEmailSender;

    private String apiKeyRecoveryEmailSubject;

    private String apiKeyRecoveryEmailBodyTemplate;

    private String verificationEmailSender;

    private String verificationEmailSubject;

    private String verificationEmailUrlTemplate;

    private String verificationEmailBodyTemplate;

    private boolean disabled;

    private boolean logMessages;

    final String getPwResetEmailSender() {
        return pwResetEmailSender;
    }

    @SuppressWarnings("unused")
    public final void setPwResetEmailSender(String pwResetEmailSender) {
        this.pwResetEmailSender = pwResetEmailSender;
    }

    final String getPwResetEmailSubject() {
        return pwResetEmailSubject;
    }

    @SuppressWarnings("unused")
    public final void setPwResetEmailSubject(String pwResetEmailSubject) {
        this.pwResetEmailSubject = pwResetEmailSubject;
    }

    final String getPwResetEmailUrlTemplate() {
        return pwResetEmailUrlTemplate;
    }

    @SuppressWarnings("unused")
    public final void setPwResetEmailUrlTemplate(String pwResetEmailUrlTemplate) {
        this.pwResetEmailUrlTemplate = pwResetEmailUrlTemplate;
    }

    final String getPwResetEmailBodyTemplate() {
        return pwResetEmailBodyTemplate;
    }

    @SuppressWarnings("unused")
    public final void setPwResetEmailBodyTemplate(String pwResetEmailBodyTemplate) {
        this.pwResetEmailBodyTemplate = pwResetEmailBodyTemplate;
    }

    final String getApiKeyRecoveryEmailSender() {
        return apiKeyRecoveryEmailSender;
    }

    @SuppressWarnings("unused")
    public final void setApiKeyRecoveryEmailSender(String apiKeyRecoveryEmailSender) {
        this.apiKeyRecoveryEmailSender = apiKeyRecoveryEmailSender;
    }

    final String getApiKeyRecoveryEmailSubject() {
        return apiKeyRecoveryEmailSubject;
    }

    @SuppressWarnings("unused")
    public final void setApiKeyRecoveryEmailSubject(String apiKeyRecoveryEmailSubject) {
        this.apiKeyRecoveryEmailSubject = apiKeyRecoveryEmailSubject;
    }

    final String getApiKeyRecoveryEmailBodyTemplate() {
        return apiKeyRecoveryEmailBodyTemplate;
    }

    @SuppressWarnings("unused")
    public final void setApiKeyRecoveryEmailBodyTemplate(String apiKeyRecoveryEmailBodyTemplate) {
        this.apiKeyRecoveryEmailBodyTemplate = apiKeyRecoveryEmailBodyTemplate;
    }

    final String getVerificationEmailSender() {
        return verificationEmailSender;
    }

    @SuppressWarnings("unused")
    public final void setVerificationEmailSender(String verificationEmailSender) {
        this.verificationEmailSender = verificationEmailSender;
    }

    final String getVerificationEmailSubject() {
        return verificationEmailSubject;
    }

    @SuppressWarnings("unused")
    public final void setVerificationEmailSubject(String verificationEmailSubject) {
        this.verificationEmailSubject = verificationEmailSubject;
    }

    final String getVerificationEmailUrlTemplate() {
        return verificationEmailUrlTemplate;
    }

    @SuppressWarnings("unused")
    public final void setVerificationEmailUrlTemplate(String verificationEmailUrlTemplate) {
        this.verificationEmailUrlTemplate = verificationEmailUrlTemplate;
    }

    final String getVerificationEmailBodyTemplate() {
        return verificationEmailBodyTemplate;
    }

    @SuppressWarnings("unused")
    public final void setVerificationEmailBodyTemplate(String verificationEmailBodyTemplate) {
        this.verificationEmailBodyTemplate = verificationEmailBodyTemplate;
    }

    final boolean getDisabled() {
        return disabled;
    }

    @SuppressWarnings("unused")
    public final void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    final boolean getLogMessages() {
        return logMessages;
    }

    @SuppressWarnings("unused")
    public final void setLogMessages(boolean logMessages) {
        this.logMessages = logMessages;
    }

    @Override
    public final String toString() {
        return "MailConfigProps{" +
                "pwResetEmailSender='" + pwResetEmailSender + '\'' +
                ", pwResetEmailSubject='" + pwResetEmailSubject + '\'' +
                ", pwResetEmailUrlTemplate='" + pwResetEmailUrlTemplate + '\'' +
                ", pwResetEmailBodyTemplate='" + pwResetEmailBodyTemplate + '\'' +
                ", apiKeyRecoveryEmailSender='" + apiKeyRecoveryEmailSender + '\'' +
                ", apiKeyRecoveryEmailSubject='" + apiKeyRecoveryEmailSubject + '\'' +
                ", apiKeyRecoveryEmailBodyTemplate='" + apiKeyRecoveryEmailBodyTemplate + '\'' +
                ", verificationEmailSender='" + verificationEmailSender + '\'' +
                ", verificationEmailSubject='" + verificationEmailSubject + '\'' +
                ", verificationEmailUrlTemplate='" + verificationEmailUrlTemplate + '\'' +
                ", verificationEmailBodyTemplate='" + verificationEmailBodyTemplate + '\'' +
                ", disabled=" + disabled +
                ", logMessages=" + logMessages +
                '}';
    }
}
