package com.example.utils;

import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;

import java.io.IOException;

public class HttpUtil {
    private HttpUtil() {
        // Private constructor to prevent instantiation
    }

    public static SimpleHttp.Response executeRequest(String url, SimpleHttp request) throws IOException {
        var response = request.asResponse();
        if (response.getStatus() != 200) {
            String msg = "failed to invoke url [" + url + "]";
            try {
                var tmp = response.asString();
                if (tmp != null)
                    msg = tmp;
            } catch (IOException ignored) {
                // Ignore
            }
            throw new IdentityBrokerException("Failed to invoke url [" + url + "]: " + msg);
        }
        return  response;
    }
}
