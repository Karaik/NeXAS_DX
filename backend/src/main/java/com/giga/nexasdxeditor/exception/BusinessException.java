package com.giga.nexasdxeditor.exception;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BusinessException extends RuntimeException {
    private Integer code;
    private String msg;

    public BusinessException(String message, Integer code, String msg) {
        super(message);
        this.code = code;
        this.msg = msg;
    }
}
