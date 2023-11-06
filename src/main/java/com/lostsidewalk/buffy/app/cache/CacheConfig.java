package com.lostsidewalk.buffy.app.cache;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import redis.clients.jedis.JedisPool;

@Slf4j
@Configuration
public class CacheConfig {

    // short-lived caches

    @CacheEvict("thumbnailRefreshCache")
    @Scheduled(fixedDelay = 10_000L, initialDelay = 480_000L)
    public static void clearThumbnailRefreshCache() {
        log.trace("Thumbnail refresh cache cleared");
    }

    // long-lived caches

    @CacheEvict(allEntries = true, value = "thumbnailCache")
    @Scheduled(fixedDelay = 10_800_000L, initialDelay = 480_000L)
    public static void clearThumbnailCache() {
        log.trace("Thumbnail cache cleared");
    }

    @Value("${spring.redis.password}")
    String redisPassword;

    @Value("${spring.redis.port}")
    Integer redisPort;

    @SuppressWarnings("DesignForExtension")
    @Bean
    public JedisPool jedisPool() {
        return new JedisPool("feedgears-cache01", redisPort, null, redisPassword);
    }

    @SuppressWarnings({"ChainedMethodCall", "DesignForExtension"})
    @Bean
    public JedisBasedProxyManager proxyManager() {
        JedisPool jedisPool = jedisPool();
        ExpirationAfterWriteStrategy none = ExpirationAfterWriteStrategy.none();
        return JedisBasedProxyManager
                .builderFor(jedisPool)
                .withExpirationStrategy(none)
                .build();
    }

    @Override
    public final String toString() {
        return "CacheConfig{" +
                "redisPassword='" + redisPassword + '\'' +
                ", redisPort=" + redisPort +
                '}';
    }
}
