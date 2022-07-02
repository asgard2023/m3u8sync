package org.ccs.m3u8sync.downup.down;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class TsKey {
    String iv="";
    String method;
    String key;
}
