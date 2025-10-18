package com.example.filter;

import java.util.UUID;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.util.Objects;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

@SuppressWarnings("unused")
@Provider
@PreMatching
public final class CorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger logger = Logger.getLogger(CorrelationFilter.class);

    public static final String KEY = "cid";
    public static final String HDR1 = "X-Request-ID";
    public static final String HDR2 = "X-Correlation-ID";
    public static final String HDR3 = "traceparent";
    public static final String STATE = "state";
    public static final String NONCE = "nonce";
    public static final String START_TIME = "start_time";

    public CorrelationFilter() {
        // default constructor
    }

    @Override
    public void filter(ContainerRequestContext req) {
        try {
            var cid = firstNonBlank(
                    req.getHeaderString(HDR1),
                    req.getHeaderString(HDR2),
                    extractFromTraceparent(req.getHeaderString(HDR3)));
            if (cid == null) {
                cid = UUID.randomUUID().toString();
            } else {
                logger.infof("CorrelationFilter found existing correlation ID: %s", cid);
            }

            MDC.put(KEY, cid);
            req.setProperty(KEY, cid);

            var state = req.getHeaderString("state");
            if (state != null) {
                MDC.put(STATE, state);
                req.setProperty(STATE, state);
            }
            var nonce = req.getHeaderString("nonce");
            if (nonce != null) {
                MDC.put(NONCE, nonce);
                req.setProperty(NONCE, nonce);
            }
            req.setProperty(START_TIME, System.currentTimeMillis());

            // print request method, path, and parameters
            logger.infof("Processing request: %s %s %s",
                    req.getMethod(),
                    req.getUriInfo().getPath(),
                    req.getUriInfo().getQueryParameters().toString());
        } catch (Exception e) {
            logger.errorf(e, "CorrelationFilter error processing request caused by %s", e.getMessage());
        }
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        try {
            var cid = Objects.toString(req.getProperty(KEY), null);
            if (cid != null) {
                res.getHeaders().putSingle(HDR1, cid);
                res.getHeaders().putSingle(HDR2, cid);
            } else {
                logger.warn("CorrelationFilter no correlation ID found in request context");
            }

            var startTime = (Long) req.getProperty(START_TIME);
            if (startTime != null) {
                var duration = System.currentTimeMillis() - startTime;
                logger.infof("Request completed request in %d ms", duration);
            }

            MDC.remove(KEY);
            MDC.remove(STATE);
            MDC.remove(NONCE);
        } catch (Exception e) {
            logger.errorf(e, "CorrelationFilter error processing response caused by %s", e.getMessage());
        }
    }

    static String firstNonBlank(String... v) {
        for (var s : v)
            if (s != null && !s.isBlank())
                return s;

        return null;
    }

    static String extractFromTraceparent(String tp) {
        // format: version-traceid-spanid-flags -> kita pakai traceid (elemen ke-2)
        if (tp == null || tp.isBlank())
            return null;
        var p = tp.split("-");

        return p.length >= 2 ? p[1] : null;
    }
}
