package org.mybad.core.exception;

/**
 * 解析异常
 * 在JSON解析过程中发生错误时抛出
 */
public class ParseException extends Exception {
    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
