package com.lostsidewalk.buffy.app;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_KEY_HEADER_NAME;
import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_SECRET_HEADER_NAME;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY;

@OpenAPIDefinition(
        info = @Info(
                title = "ComposableRSS API",
                version = "1.0",
                description = "ComposableRSS API",
                contact = @Contact(name = "Lost Sidewalk Software LLC", url = "https://www.lostsidewalk.com", email = "meh@lostsidewalk.com")
        ),
        servers = {@Server(url = "https://api.composablerss.com", description = "ComposableRSS API Gateway")},
        externalDocs = @ExternalDocumentation(url = "https://www.composablerss.com", description = "Composable RSS Official Documentation"),
        security = {
                @SecurityRequirement(name = "apikey"),
                @SecurityRequirement(name = "apisecret")
        })
@SecuritySchemes({
        @SecurityScheme(type = APIKEY, name = "apikey", paramName = API_KEY_HEADER_NAME, in = HEADER),
        @SecurityScheme(type = APIKEY, name = "apisecret", paramName = API_SECRET_HEADER_NAME, in = HEADER)
})
@SpringBootApplication
@EnableAutoConfiguration
@EnableScheduling
@EnableConfigurationProperties
@EnableTransactionManagement
@EnableCaching
@PropertySource("classpath:secret.properties")
@ComponentScan({
        "com.lostsidewalk.buffy",
        "com.listsidewalk.buffy.app",
        "com.lostsidewalk.buffy.rss",
        "com.lostsidewalk.buffy.mail",
        "com.lostsidewalk.buffy.order",
})
@Configuration
public class Application {

    public static void main(String[] args) {
        //
        // set global timeouts and startup/context config
        //
        System.setProperty("sun.net.client.defaultConnectTimeout", "2000");
        System.setProperty("sun.net.client.defaultReadTimeout", "4000");
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
        System.setProperty("http.keepAlive", "true");
        SpringApplication.run(Application.class, args);
    }
}
