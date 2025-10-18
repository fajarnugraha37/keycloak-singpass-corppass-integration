package com.example.utils;


import org.jboss.logging.Logger;

import java.util.function.BiFunction;

public class ReflectionUtil {
    private static final Logger logger = Logger.getLogger(ReflectionUtil.class);

    private ReflectionUtil() {
        // Private constructor to prevent instantiation
    }

    public static void extract(Class<?> clazz, Object object,
                               BiFunction<String, String, Void> callback) {
        extract(clazz, object, callback, false);
    }

    public static void extract(Class<?> clazz, Object object,
                               BiFunction<String, String, Void> callback,
                               boolean ignoreError) {
        try {
            for (var method : clazz.getMethods()) {
                if (method.getParameterCount() != 0)
                    continue;
                var name = method.getName();
                if (!(name.startsWith("get") || name.startsWith("is")))
                    continue;
                if (name.equals("getClass")) continue;
                try {
                    var value = method.invoke(object);
                    if (value != null) {
                        var prop = name.startsWith("get") ? name.substring(3) : name.substring(2);
                        if (prop.isEmpty())
                            continue;
                        prop = Character.toLowerCase(prop.charAt(0)) + prop.substring(1);
                        callback.apply(prop, String.valueOf(value));
                    }
                } catch (Exception ex) {
                    if (ignoreError) {
                        logger.errorf(ex, "[extract] ERROR SILENT Skipping idToken field via reflection: %s -> %s", name, ex.getMessage());
                    } else {
                        throw ex;
                    }
                }
            }
        } catch (Exception e) {
            if (ignoreError) {
                logger.debugf(e, "[extract] ERROR SILENT Reflection error iterating idToken fields: %s", e.getMessage());
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}
