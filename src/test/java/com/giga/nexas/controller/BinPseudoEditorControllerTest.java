package com.giga.nexas.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BinPseudoEditorControllerTest {

    @Test
    void buildPseudoSourceForCompileKeepsOriginalLosslessTextWhenVisibleTextIsUnchanged() {
        String originalLossless = ""
                + "entry 1 // IRHASH=aaa IR=0@27,1\r\n"
                + "flag += 1 // IRHASH=bbb IR=10@47,0\r\n";
        String currentVisible = ""
                + "entry 1\r\n"
                + "flag += 1\r\n";

        String source = BinPseudoEditorController.buildPseudoSourceForCompile(currentVisible, originalLossless);

        Assertions.assertEquals(originalLossless, source);
    }

    @Test
    void buildPseudoSourceForCompilePreservesUnchangedLosslessLinesWhenTextIsEdited() {
        String originalLossless = ""
                + "entry 1 // IRHASH=aaa IR=0@27,1\r\n"
                + "call LoadMek(21) // IRHASH=bbb IR=10@0,21|11@7,123\r\n"
                + "end 0 // IRHASH=ccc IR=12@62,0\r\n";
        String currentVisible = ""
                + "entry 1\r\n"
                + "call LoadMek(22)\r\n"
                + "end 0\r\n";

        String source = BinPseudoEditorController.buildPseudoSourceForCompile(currentVisible, originalLossless);

        Assertions.assertEquals(
                ""
                        + "entry 1 // IRHASH=aaa IR=0@27,1\r\n"
                        + "call LoadMek(22) // IRHASH=bbb IR=10@0,21|11@7,123\r\n"
                        + "end 0 // IRHASH=ccc IR=12@62,0\r\n",
                source
        );
    }

    @Test
    void buildPseudoSourceForCompileKeepsTrailingLosslessLinesAfterInsertedLine() {
        String originalLossless = ""
                + "entry 1 // IRHASH=aaa IR=0@27,1\r\n"
                + "flag += 1 // IRHASH=bbb IR=10@47,0\r\n"
                + "end 0 // IRHASH=ccc IR=12@62,0\r\n";
        String currentVisible = ""
                + "entry 1\r\n"
                + "flag += 1\r\n"
                + "marker 9\r\n"
                + "end 0\r\n";

        String source = BinPseudoEditorController.buildPseudoSourceForCompile(currentVisible, originalLossless);

        Assertions.assertEquals(
                ""
                        + "entry 1 // IRHASH=aaa IR=0@27,1\r\n"
                        + "flag += 1 // IRHASH=bbb IR=10@47,0\r\n"
                        + "marker 9\r\n"
                        + "end 0 // IRHASH=ccc IR=12@62,0\r\n",
                source
        );
    }

    @Test
    void buildPseudoSourceForCompileKeepsRemainingLosslessLinesAfterDeletedLine() {
        String originalLossless = ""
                + "entry 1 // IRHASH=aaa IR=0@27,1\r\n"
                + "marker 1 // IRHASH=bbb IR=10@29,1\r\n"
                + "flag += 1 // IRHASH=ccc IR=11@47,0\r\n"
                + "end 0 // IRHASH=ddd IR=12@62,0\r\n";
        String currentVisible = ""
                + "entry 1\r\n"
                + "flag += 1\r\n"
                + "end 0\r\n";

        String source = BinPseudoEditorController.buildPseudoSourceForCompile(currentVisible, originalLossless);

        Assertions.assertEquals(
                ""
                        + "entry 1 // IRHASH=aaa IR=0@27,1\r\n"
                        + "flag += 1 // IRHASH=ccc IR=11@47,0\r\n"
                        + "end 0 // IRHASH=ddd IR=12@62,0\r\n",
                source
        );
    }
}
