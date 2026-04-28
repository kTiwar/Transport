package com.tms.edi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EdiException extends RuntimeException {

    private final HttpStatus httpStatus;

    public EdiException(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public EdiException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
