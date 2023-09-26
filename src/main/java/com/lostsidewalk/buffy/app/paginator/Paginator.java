package com.lostsidewalk.buffy.app.paginator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class Paginator {

    public <T> List<T> paginate(List<T> list, Integer offset, Integer limit) {
        // adjust for 0-based indexing
        if (offset != null) {
            offset -= 1;
        }
        // constrain offset and toIndex to prevent index out of bounds
        int sz = list.size();
        if (offset == null) {
            offset = 0;
        } else if (offset > sz) {
            offset = sz - 1;
        }
        int toIndex;
        if (limit == null) {
            toIndex = sz;
        } else {
            toIndex = offset + limit;
        }
        if (toIndex > sz) {
            toIndex = sz - 1;
        }
        if (offset > toIndex) {
            toIndex = offset;
        }
        // return the sublist
        return list.subList(offset, toIndex);
    }
}
