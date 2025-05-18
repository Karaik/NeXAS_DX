package com.giga.nexas.dto.bhe;

import java.io.IOException;

public interface BheGenerator<T extends Bhe> {
    String supportExtension();
    void generate(String path, T t, String charset) throws IOException;
}
