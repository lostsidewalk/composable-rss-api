package com.lostsidewalk.buffy.app.cache;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import redis.clients.jedis.JedisPool;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Configuration
public class CacheConfig {

    // short-lived caches

    @CacheEvict(value = {"thumbnailRefreshCache"})
    @Scheduled(fixedDelay=10_000, initialDelay=480_000)
    public void clearThumbnailRefreshCache() {
        log.trace("Thumbnail refresh cache cleared");
    }

    // long-lived caches

    @CacheEvict(allEntries = true, value = {"thumbnailCache"})
    @Scheduled(fixedDelay=10_800_000, initialDelay=480_000)
    public void clearThumbnailCache() {
        log.trace("Thumbnail cache cleared");
    }

    @Bean
    public JedisPool jedisPool() {
        JedisPool jedisPool = null;
        try {
            URI redisUri = new URI("redis://feedgears-cache01:6379");
            jedisPool = new JedisPool(redisUri, 20000, null, null, null);
        } catch (URISyntaxException e) {
            log.error("Failed to create the Jedis pool for feedgears-cache01: ", e);
        }
        return jedisPool;
    }

    @Bean
    JedisBasedProxyManager proxyManager() {
        return JedisBasedProxyManager
                .builderFor(jedisPool())
                .withExpirationStrategy(ExpirationAfterWriteStrategy.none())
                .build();
    }
}
