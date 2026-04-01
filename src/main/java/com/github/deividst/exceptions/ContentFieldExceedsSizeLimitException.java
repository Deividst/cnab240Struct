package com.github.deividst.exceptions;

public class ContentFieldExceedsSizeLimitException extends RuntimeException {

    public ContentFieldExceedsSizeLimitException(String message) {
        super(message);
    }
}
