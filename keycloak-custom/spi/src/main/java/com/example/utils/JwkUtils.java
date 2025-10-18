package com.example.utils;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.jboss.logging.Logger;

public class JwkUtils {
    private static final Logger logger = Logger.getLogger(JwkUtils.class);

    private JwkUtils() {}

    public static JWK parsePrivateJwk(String json) {
        if (json == null || json.isBlank()) {
            logger.warnf("empty JWK json");
            return null;
        }
        try {
            if (json.trim().startsWith("{") && json.contains("\"keys\"")) {
                var set = JWKSet.parse(json);
                if (!set.getKeys().isEmpty()) {
                    var key = set.getKeys().get(0);
                    logger.infof("parsed JWK set with %d keys, using first key with kid=%s", set.getKeys().size(), key.getKeyID());
                    return key;
                }

                return null;
            }
            logger.infof("parsing single JWK");
            var set = JWK.parse(json);
            logger.infof("parsed single JWK with kid=%s", set.getKeyID());
            return set;
        } catch (Exception e) {
            logger.errorf(e, "failed to parse JWK json");
            throw new IllegalArgumentException("invalid JWK json", e);
        }
    }
}
