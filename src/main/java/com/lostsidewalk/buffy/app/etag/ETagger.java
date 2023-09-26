package com.lostsidewalk.buffy.app.etag;

import com.lostsidewalk.buffy.Auditable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.time.Instant.ofEpochMilli;

@Slf4j
@Component
public class ETagger {

    public String computeEtag(Auditable entity) {
        Date lastModified = entity.getLastModified();
        long t = lastModified == null ? 0L : lastModified.getTime();
        Instant instant = ofEpochMilli(t);
        return instant.toString();
    }

    public String computeEtag(List<? extends Auditable> entities) {
        List<Long> lastModified = entities.stream()
                .map(Auditable::getLastModified)
                .filter(Objects::nonNull)
                .map(Date::getTime)
                .toList();
        long t = lastModified.stream().mapToLong(Long::valueOf).sum();
        Instant sumInstant = ofEpochMilli(t);
        return sumInstant.toString();
    }
}
