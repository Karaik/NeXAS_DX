package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.dto.bsdx.spm.Spm;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/** Pages consumed by BSDX 7655A0 for every direction of one animation. */
public final class SpmAnimationPages {
    // Original x86 table at 867B58; mode 7 bypasses both this table and mirroring.
    private static final int[] DIRECTIONS = {1, 2, 4, 8, 16, 24, 40};

    private SpmAnimationPages() { }

    public static Set<Integer> reachablePages(Spm spm, Spm.SPMAnimData animation) {
        if (spm == null || spm.getPageData() == null || animation == null || animation.getPatData() == null) {
            throw invalid("missing SPM page or animation data");
        }
        Set<Integer> offsets = directionOffsets(animation.getAnimRotateDirection(), animation.getAnimReverseDirection());
        Set<Integer> pages = new LinkedHashSet<>();
        for (var pattern : animation.getPatData()) {
            if (pattern == null || pattern.getPageNo() == null) throw invalid("missing pattern pages");
            for (Integer base : pattern.getPageNo()) {
                if (base == null || base < -1) throw invalid("invalid base page: " + base);
                for (int offset : offsets) {
                    long page = (long) base + offset;
                    // 7655A0 adds the direction even to raw -1; 60EA60 filters the resulting negative page.
                    if (page < 0) continue;
                    if (page >= spm.getPageData().size()) {
                        throw invalid("derived page " + page + " exceeds page count " + spm.getPageData().size());
                    }
                    pages.add((int) page);
                }
            }
        }
        return Collections.unmodifiableSet(pages);
    }

    private static Set<Integer> directionOffsets(Integer rotation, Integer mirror) {
        if (rotation == null || rotation < 0 || rotation > 7) throw invalid("undefined rotation mode: " + rotation);
        if (rotation == 7) return Set.of(0);
        if (mirror == null) throw invalid("missing mirror mode");
        // 7655A0 returns -1 (no page) for other mirror modes; it does not draw the base page.
        if (mirror < 0 || mirror > 2) return Set.of();
        int count = DIRECTIONS[rotation];
        if (mirror == 2 && count < 4) throw invalid("quarter mirroring would divide by zero for rotation " + rotation);
        int step = 3600 / count / 10;
        Set<Integer> offsets = new TreeSet<>();
        // Normalization and half-step rounding permute integer angles 0..359.
        // Keep native integer division: count=16 has step=22 and can select direction 16.
        for (int direction = 0; direction <= 359 / step; direction++) {
            int offset;
            if (mirror == 0) {
                offset = direction;
            } else if (mirror == 1) {
                int half = count >> 1;
                offset = rotation == 1 ? 0 : direction <= half ? direction : 2 * half - direction;
            } else {
                int quarter = count >> 2;
                // Java division, like the native signed IDIV, truncates toward zero.
                offset = switch ((direction - 1) / quarter) {
                    case 0 -> direction;
                    case 1 -> 2 * quarter - direction;
                    case 2 -> direction - 2 * quarter;
                    case 3 -> quarter - direction % quarter;
                    default -> -1;
                };
            }
            if (offset >= 0) offsets.add(offset);
        }
        return offsets;
    }

    private static IllegalStateException invalid(String reason) {
        return new IllegalStateException("SPM directional pages: " + reason);
    }
}
