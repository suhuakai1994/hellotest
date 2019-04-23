package com.huawei.oss.at.corba.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenAbnormalException extends RuntimeException {

    public TokenAbnormalException(String message) {
        super(message);
    }
}
