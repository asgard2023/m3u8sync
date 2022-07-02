package org.ccs.m3u8sync.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class M3u8InfoVo {
    private Integer duration;
    private String flvUrl;
    private String vodUrl;
    private String vodTryUrl;
}
