package io.mamish.serverbot2.urlshortener;

public class UrlRevokedException extends RuntimeException {

    public UrlRevokedException(String message) {
        super(message);
    }

    public UrlRevokedException(String message, Throwable cause) {
        super(message, cause);
    }

}
