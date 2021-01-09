package com.admiralbot.urlshortener.shortener;

public class UrlRevokedException extends RuntimeException {

    public UrlRevokedException(String message) {
        super(message);
    }

    public UrlRevokedException(String message, Throwable cause) {
        super(message, cause);
    }

}
