package org.ccs.m3u8sync;

import cn.hutool.core.codec.Morse;
import org.junit.jupiter.api.Test;

class HuToolTest {
    @Test
    void morse() {
        Morse morseCoder = new Morse();
        System.out.println(morseCoder.encode("abcdefgt"));
        System.out.println(morseCoder.encode("ABCDEFGT"));
    }
}
