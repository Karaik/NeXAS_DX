package com.giga.nexas.dto.bsdx;

import java.io.IOException;

public interface BsdxGenerator<T extends Bsdx> {
    String supportExtension();
    void generate(String path, T t, String charset) throws IOException;
}
