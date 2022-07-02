package org.ccs.m3u8sync.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class CallbackVo implements Serializable {
    private String baseUrl;
    private String paramUrl;
    private Integer count=0;
}
