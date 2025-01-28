package com.giga.nexasdxeditor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO<T> implements Serializable {
    private T data;
    private String msg;

    public ResponseDTO(String msg) {
        this.msg = msg;
    }
}
