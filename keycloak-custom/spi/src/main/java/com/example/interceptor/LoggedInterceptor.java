package com.example.interceptor;

import jakarta.interceptor.Interceptor;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

@jakarta.interceptor.Interceptor
@Logged
@jakarta.annotation.Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class LoggedInterceptor {
    private static final Logger logger = Logger.getLogger(LoggedInterceptor.class);

    @jakarta.inject.Inject
    KeycloakSession session;

    @jakarta.interceptor.AroundInvoke
    Object around(jakarta.interceptor.InvocationContext ctx) throws Exception {
        var t0 = System.nanoTime();
        logger.infof("→ %s.%s",
                ctx.getTarget().getClass().getSimpleName(),
                ctx.getMethod().getName());
        try {
            var out = ctx.proceed();
            logger.infof("← %s.%s ok in %dms",
                    ctx.getTarget().getClass().getSimpleName(),
                    ctx.getMethod().getName(),
                    (System.nanoTime() - t0) / 1_000_000);
            return out;
        } catch (Exception e) {
            logger.errorf(e, "← %s.%s error",
                    ctx.getTarget().getClass().getSimpleName(),
                    ctx.getMethod().getName());
            throw e;
        }
    }
}
