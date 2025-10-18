package com.example.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class JsonUtil {
    private static final Logger logger = Logger.getLogger(JsonUtil.class);

    private JsonUtil() {
        // Private constructor to prevent instantiation
    }

    public static Optional<JsonNode> getValue(JsonNode jsonNode, String keyPath) {
        return getValue(jsonNode, keyPath, ".");
    }

    public static Optional<JsonNode> getValue(JsonNode jsonNode, String keyPath, String splitChar) {
        if (jsonNode == null || keyPath == null || keyPath.isEmpty() || splitChar == null || splitChar.isEmpty()) {
            logger.warnf("[JsonNode] Invalid input: jsonNode or keyPath or splitChar is null/empty");
            return Optional.empty();
        }

        var keys = keyPath.split(Pattern.quote( splitChar));
        var currentNode = jsonNode;
        for (var key : keys) {
            if (currentNode.has(key)) {
                currentNode = currentNode.get(key);
            } else {
                logger.warnf("[JsonNode] Key %s not found", key);
                return Optional.empty();
            }
        }
        logger.infof("[JsonNode] Value for key path %s: %s", keyPath, currentNode.asText());

        return Optional.of(currentNode);
    }

    public static void printAll(JsonNode jsonNode, String prefKey) {
            if (jsonNode == null) {
                return;
            }

            if (jsonNode.isObject()) {
                jsonNode.fields()
                        .forEachRemaining(entry -> printAll(entry.getValue(), prefKey + "." + entry.getKey()));
            } else if (jsonNode.isArray()) {
                for (var i = 0; i < jsonNode.size(); i++) {
                    logger.infof("[JsonNode] Array element [%d]: %s", i, jsonNode.get(i));
                    printAll(jsonNode.get(i), prefKey);
                }
            } else {
                logger.infof("[JsonNode] Key %s Value: %s", prefKey, jsonNode.asText());
            }
    }

    public static Map<String, String> flattenJsonNode(JsonNode jsonNode, String parentKey, Map<String, String> resultMap) {
        if (jsonNode == null) {
            return resultMap;
        }

        if (jsonNode.isObject()) {
            jsonNode.fields().forEachRemaining(entry -> {
                var newKey = parentKey == null || parentKey.isEmpty()
                        ? entry.getKey()
                        : parentKey + "." + entry.getKey();
                flattenJsonNode(entry.getValue(), newKey, resultMap);
            });
        } else if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                var newKey = parentKey + "[" + i + "]";
                flattenJsonNode(jsonNode.get(i), newKey, resultMap);
            }
        } else {
            resultMap.put(parentKey, jsonNode.asText());
        }

        return resultMap;
    }
}
