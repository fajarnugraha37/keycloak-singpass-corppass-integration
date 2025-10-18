package com.example.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static com.example.utils.JsonUtil.*;
import static com.example.utils.JsonUtil.flattenJsonNode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonUtilTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getValueHandlesNullSplitChar() throws Exception {
        var json = """
                {
                    "key1":{
                        "key2": "value"
                    }
                }
                """;
        var jsonNode = objectMapper.readTree(json);
        var result = getValue(jsonNode, "key1.key2", null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getValueHandlesEmptySplitChar() throws Exception {
        var json = """
                {
                    "key1":{
                        "key2":"value"
                    }
                }
                """;
        var jsonNode = objectMapper.readTree(json);
        var result = getValue(jsonNode, "key1.key2", "");

        assertTrue(result.isEmpty());
    }

    @Test
    void getValueHandlesInvalidSplitChar() throws Exception {
        var json = """
                {
                    "key1":{
                        "key2":"value"
                    }
                }
                """;
        var jsonNode = objectMapper.readTree(json);
        var result = getValue(jsonNode, "key1.key2", "/");

        assertTrue(result.isEmpty());
    }

    @Test
    void printAllHandlesNullJsonNode() {
        printAll(null, "root");
        // Verify logs manually or use a logging framework with test appenders
    }

    @Test
    void flattenJsonNodeHandlesEmptyParentKey() throws Exception {
        var json = """
                {
                    "key1": "value"
                }
                """;
        var jsonNode = objectMapper.readTree(json);
        var resultMap = new HashMap<String, String>();
        var result = flattenJsonNode(jsonNode, "", resultMap);

        assertEquals(1, result.size());
        assertEquals("value", result.get("key1"));
    }

    @Test
    void flattenJsonNodeHandlesNullParentKey() throws Exception {
        var json = """
                {
                    "key1": "value"
                }
                """;
        var jsonNode = objectMapper.readTree(json);
        var resultMap = new HashMap<String, String>();
        var result = flattenJsonNode(jsonNode, null, resultMap);

        assertEquals(1, result.size());
        assertEquals("value", result.get("key1"));
    }

    @Test
    void getValueReturnsValueForValidKeyPath() throws Exception {
        var json = """
                {
                    "key1":{
                        "key2":"value"
                    }
                }
                """;
        var jsonNode = objectMapper.readTree(json);
        var result = getValue(jsonNode, "key1.key2", ".");

        assertTrue(result.isPresent());
        assertEquals("value", result.get().asText());
    }

    @Test
    void flattenJsonNodeHandlesNestedObjects() throws Exception {
        var json = """
                {
                    "key1": {
                        "key2": {
                            "key3": "value"
                        }
                    }
                }
                """;
        var jsonNode = objectMapper.readTree(json);
        var resultMap = new HashMap<String, String>();
        var result = flattenJsonNode(jsonNode, "", resultMap);

        assertEquals(1, result.size());
        assertEquals("value", result.get("key1.key2.key3"));
    }
}
