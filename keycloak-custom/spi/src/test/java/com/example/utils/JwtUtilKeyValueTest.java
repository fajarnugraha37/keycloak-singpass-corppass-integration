package com.example.utils;

//import org.junit.jupiter.api.Test;
//import java.util.HashMap;
//import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtil key-value string parsing methods
 */
class JwtUtilKeyValueTest {
//
//    @Test
//    void testSubToKeyValue_ValidInput() {
//        // Arrange
//        String input = "s=S8979373D,uuid=a9865837-7bd7-46ac-bef4-42a76a946424,u=123456789AS8979373D,c=SG";
//
//        // Act
//        Map<String, String> result = JwtUtil.subToKeyValue(input);
//
//        // Assert
//        assertEquals(4, result.size());
//        assertEquals("S8979373D", result.get("s"));
//        assertEquals("a9865837-7bd7-46ac-bef4-42a76a946424", result.get("uuid"));
//        assertEquals("123456789AS8979373D", result.get("u"));
//        assertEquals("SG", result.get("c"));
//    }
//
//    @Test
//    void testSubToKeyValue_WithSpaces() {
//        // Arrange
//        String input = " s = S8979373D , uuid = a9865837-7bd7-46ac-bef4-42a76a946424 , u = 123456789AS8979373D , c = SG ";
//
//        // Act
//        Map<String, String> result = JwtUtil.subToKeyValue(input);
//
//        // Assert
//        assertEquals(4, result.size());
//        assertEquals("S8979373D", result.get("s"));
//        assertEquals("a9865837-7bd7-46ac-bef4-42a76a946424", result.get("uuid"));
//        assertEquals("123456789AS8979373D", result.get("u"));
//        assertEquals("SG", result.get("c"));
//    }
//
//    @Test
//    void testSubToKeyValueString_ValueWithEquals() {
//        // Arrange
//        String input = "key1=value1,key2=value=with=equals,key3=value3";
//
//        // Act
//        Map<String, String> result = JwtUtil.subToKeyValue(input);
//
//        // Assert
//        assertEquals(3, result.size());
//        assertEquals("value1", result.get("key1"));
//        assertEquals("value=with=equals", result.get("key2"));
//        assertEquals("value3", result.get("key3"));
//    }
//
//    @Test
//    void testSubToKeyValueString_Empty() {
//        // Arrange
//        String input = "";
//
//        // Act
//        Map<String, String> result = JwtUtil.subToKeyValue(input);
//
//        // Assert
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void testSubToKeyValueString_Null() {
//        // Arrange
//        String input = null;
//
//        // Act
//        Map<String, String> result = JwtUtil.subToKeyValue(input);
//
//        // Assert
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void testSubToKeyValue_InvalidPairs() {
//        // Arrange - string with some invalid pairs
//        String input = "valid=value,invalid_pair,another=valid,=empty_key,empty_value=";
//
//        // Act
//        Map<String, String> result = JwtUtil.subToKeyValue(input);
//
//        // Assert
//        assertEquals(3, result.size()); // Only valid pairs should be included
//        assertEquals("value", result.get("valid"));
//        assertEquals("valid", result.get("another"));
//        assertEquals("", result.get("empty_value")); // Empty value is allowed
//        assertFalse(result.containsKey("")); // Empty key should be skipped
//        assertFalse(result.containsKey("invalid_pair")); // Invalid format should be skipped
//    }
//
//    @Test
//    void testSubToKeyValue_SinglePair() {
//        // Arrange
//        String input = "key=value";
//
//        // Act
//        Map<String, String> result = JwtUtil.subToKeyValue(input);
//
//        // Assert
//        assertEquals(1, result.size());
//        assertEquals("value", result.get("key"));
//    }
//
//    @Test
//    void testMapToKeyValueString_ValidMap() {
//        // Arrange
//        Map<String, String> input = new HashMap<>();
//        input.put("s", "S8979373D");
//        input.put("uuid", "a9865837-7bd7-46ac-bef4-42a76a946424");
//        input.put("u", "123456789AS8979373D");
//        input.put("c", "SG");
//
//        // Act
//        String result = JwtUtil.keyValueToSub(input);
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.contains("s=S8979373D"));
//        assertTrue(result.contains("uuid=a9865837-7bd7-46ac-bef4-42a76a946424"));
//        assertTrue(result.contains("u=123456789AS8979373D"));
//        assertTrue(result.contains("c=SG"));
//
//        // Count commas to verify structure
//        long commaCount = result.chars().filter(ch -> ch == ',').count();
//        assertEquals(3, commaCount); // 4 pairs = 3 commas
//    }
//
//    @Test
//    void testMapToKeyValueString_EmptyMap() {
//        // Arrange
//        Map<String, String> input = new HashMap<>();
//
//        // Act
//        String result = JwtUtil.keyValueToSub(input);
//
//        // Assert
//        assertEquals("", result);
//    }
//
//    @Test
//    void testMapToKeyValueString_NullMap() {
//        // Arrange
//        Map<String, String> input = null;
//
//        // Act
//        String result = JwtUtil.keyValueToSub(input);
//
//        // Assert
//        assertEquals("", result);
//    }
//
//    @Test
//    void testRoundTrip_ParseAndConvertBack() {
//        // Arrange
//        String original = "s=S8979373D,uuid=a9865837-7bd7-46ac-bef4-42a76a946424,u=123456789AS8979373D,c=SG";
//
//        // Act
//        Map<String, String> parsed = JwtUtil.subToKeyValue(original);
//        String reconstructed = JwtUtil.keyValueToSub(parsed);
//        Map<String, String> reparsed = JwtUtil.subToKeyValue(reconstructed);
//
//        // Assert
//        assertEquals(parsed.size(), reparsed.size());
//        for (Map.Entry<String, String> entry : parsed.entrySet()) {
//            assertEquals(entry.getValue(), reparsed.get(entry.getKey()));
//        }
//    }
//
//    @Test
//    void testSubToKeyValue_ComplexValues() {
//        // Arrange - test with complex values that might appear in real scenarios
//        String input = "email=user@domain.com,path=/api/v1/users,query=name=john&age=30,encoded=%20hello%20world";
//
//        // Act
//        Map<String, String> result = JwtUtil.subToKeyValue(input);
//
//        // Assert
//        assertEquals(4, result.size());
//        assertEquals("user@domain.com", result.get("email"));
//        assertEquals("/api/v1/users", result.get("path"));
//        assertEquals("name=john&age=30", result.get("query"));
//        assertEquals("%20hello%20world", result.get("encoded"));
//    }
}
