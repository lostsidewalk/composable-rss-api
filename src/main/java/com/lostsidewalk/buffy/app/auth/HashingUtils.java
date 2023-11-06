package com.lostsidewalk.buffy.app.auth;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

@Slf4j
class HashingUtils {

    static String sha256(String str, Charset charset) {
        HashFunction hashFunction = Hashing.sha256();
        HashCode hashCode = hashFunction.hashString(str, charset);
        return hashCode.toString();
    }
}
