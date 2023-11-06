package com.lostsidewalk.buffy.app.paginator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class Paginator {

    public static <T> List<T> paginate(List<T> list, Integer offset, Integer limit) {
        // adjust for 0-based indexing
        Integer o = offset;
        if (o != null) {
            o -= 1;
        }
        // constrain offset and toIndex to prevent index out of bounds
        int sz = list.size();
        if (o == null) {
            o = 0;
        } else if (o > sz) {
            o = sz - 1;
        }
        int toIndex;
        if (limit == null) {
            toIndex = sz;
        } else {
            toIndex = o + limit;
        }
        if (toIndex > sz) {
            toIndex = sz - 1;
        }
        if (o > toIndex) {
            toIndex = o;
        }
        // return the sublist
        return list.subList(o, toIndex);
    }
}
