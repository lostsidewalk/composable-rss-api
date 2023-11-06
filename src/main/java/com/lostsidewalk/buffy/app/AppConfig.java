package com.lostsidewalk.buffy.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Configuration
public class AppConfig {

    @SuppressWarnings({"MethodMayBeStatic", "DesignForExtension"})
    @Bean
    ConcurrentHashMap<String, Integer> errorStatusMap() {
        return new ConcurrentHashMap<>(32);
    }

    @SuppressWarnings({"MethodMayBeStatic", "DesignForExtension"})
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @SuppressWarnings("DesignForExtension")
    @Bean
    public LocalValidatorFactoryBean getValidator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        MessageSource messageSource = messageSource();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
