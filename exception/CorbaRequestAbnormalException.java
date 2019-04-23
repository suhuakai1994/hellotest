package com.huawei.oss.at.corba.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class CorbaRequestAbnormalException extends RuntimeException {

    public CorbaRequestAbnormalException(String message) {
        super(message);
    }
}
