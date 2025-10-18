package com.example.resources.dto;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ResponseModelUtil {

    private ResponseModelUtil() {
    }

    public static <T> XDataMapResponse<T> createDataMapResponse(Map<String, T> responseData, Integer status) {
        return new XDataMapResponse<>(responseData, status);
    }

    public static XDataMapResponse<String> createDataMapResponse(String message, int statusCode, String correlationId) {
        var responseData = new HashMap<String, String>();
        responseData.put("correlationId", correlationId);
        responseData.put("message", message);
        responseData.put("dateTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        return createDataMapResponse(responseData, statusCode);
    }
}