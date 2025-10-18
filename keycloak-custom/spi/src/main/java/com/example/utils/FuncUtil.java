package com.example.utils;

public class FuncUtil {
    private FuncUtil() {
        // Private constructor to prevent instantiation
    }

    public static <T> T invoke(Supplier<T> supplier) {
        return invoke(supplier, null);
    }

    public static <T> T invoke(Supplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            return defaultValue;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FunctionalInterface
    public interface Supplier<T> {
        T get() throws Exception;
    }
}
