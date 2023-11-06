package com.lostsidewalk.buffy.app.etag;

import com.lostsidewalk.buffy.Auditable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.time.Instant.ofEpochMilli;

@Slf4j
@Component
public class ETagger {

    public static String computeEtag(Auditable entity) {
        Date lastModified = entity.getLastModified();
        long t = lastModified == null ? 0L : lastModified.getTime();
        Instant instant = ofEpochMilli(t);
        return instant.toString();
    }

    public static String computeEtag(Collection<? extends Auditable> entities) {
        List<Long> lastModified = entities.stream()
                .map(Auditable::getLastModified)
                .filter(Objects::nonNull)
                .map(Date::getTime)
                .toList();
        Stream<Long> stream = lastModified.stream();
        LongStream longStream = stream.mapToLong(Long::valueOf);
        long t = longStream.sum();
        Instant sumInstant = ofEpochMilli(t);
        return sumInstant.toString();
    }
}
