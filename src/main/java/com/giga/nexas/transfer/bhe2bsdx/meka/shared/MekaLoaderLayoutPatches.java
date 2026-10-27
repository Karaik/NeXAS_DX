package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * 同步原生CMekaLoader的内联数组、尾字段与分配大小。
 * 保留线程、队列和100MiB节流逻辑；费用DAT的ID覆盖须由资源输出另行保证。
 */
public final class MekaLoaderLayoutPatches {

    private MekaLoaderLayoutPatches() {
    }

    public static List<Site> forCapacities(int sourceCapacity, int targetCapacity) {
        if (sourceCapacity < 103 || targetCapacity <= sourceCapacity || targetCapacity > 127) {
            throw new IllegalArgumentException("CMekaLoader布局只支持已审计的103..127递增容量");
        }
        List<Site> sites = new ArrayList<>();
        int sourceState = 132 + 4 * sourceCapacity;
        int targetState = 132 + 4 * targetCapacity;
        int sourceTotal = 132 + 8 * sourceCapacity;
        int targetTotal = 132 + 8 * targetCapacity;
        int sourceActive = sourceTotal + 4;
        int targetActive = targetTotal + 4;

        // 每个位点包含完整指令上下文，拒绝在操作码或其他立即数不同的EXE上改位移。
        add(sites, 0x0638D7, "83bc81", "01", sourceState, targetState, "查询排队状态");
        add(sites, 0x063A44, "8db7", "", sourceState, targetState, "预算扫描起点");
        add(sites, 0x063AFF, "c7848e", "00000000", sourceState, targetState, "清理状态");
        add(sites, 0x063BFA, "c7848e", "02000000", sourceState, targetState, "清理路径同步加载");
        add(sites, 0x063CFB, "c78486", "03000000", sourceState, targetState, "清理路径完成加载");
        add(sites, 0x063F28, "c784bb", "04000000", sourceState, targetState, "线程开始卸载");
        add(sites, 0x063FAD, "89acbb", "", sourceState, targetState, "线程完成卸载");
        add(sites, 0x06401F, "c784bb", "02000000", sourceState, targetState, "线程开始加载");
        add(sites, 0x0640AF, "c784bb", "03000000", sourceState, targetState, "线程完成加载");
        add(sites, 0x064188, "c78482", "00000000", sourceState, targetState, "同步路径取消卸载");
        add(sites, 0x0642AF, "c78481", "02000000", sourceState, targetState, "同步路径开始加载");
        add(sites, 0x0643B5, "c7848d", "03000000", sourceState, targetState, "同步路径完成加载");
        add(sites, 0x064487, "83bc81", "02", sourceState, targetState, "查询加载中状态");
        add(sites, 0x06453A, "c78486", "01000000", sourceState, targetState, "加入加载队列");
        add(sites, 0x06458F, "c78486", "00000000", sourceState, targetState, "释放最后引用");
        add(sites, 0x0648A6, "8d86", "", sourceState, targetState, "构造器状态数组起点");

        add(sites, 0x0638FE, "81b9", "00004006", sourceTotal, targetTotal, "查询预算阈值");
        add(sites, 0x06396C, "81be", "00004006", sourceTotal, targetTotal, "恢复线程预算检查");
        add(sites, 0x063A02, "0196", "", sourceTotal, targetTotal, "线程加载费用累加");
        add(sites, 0x063A0F, "8b86", "", sourceTotal, targetTotal, "线程加载费用读取");
        add(sites, 0x063CB8, "0196", "", sourceTotal, targetTotal, "清理路径费用累加");
        add(sites, 0x063CC5, "8b86", "", sourceTotal, targetTotal, "清理路径费用读取");
        add(sites, 0x064371, "0195", "", sourceTotal, targetTotal, "同步路径费用累加");
        add(sites, 0x06437E, "8b85", "", sourceTotal, targetTotal, "同步路径费用读取");
        add(sites, 0x06468D, "298e", "", sourceTotal, targetTotal, "卸载费用扣除");
        add(sites, 0x06469A, "8b86", "", sourceTotal, targetTotal, "卸载预算检查");
        add(sites, 0x0648A0, "899e", "", sourceTotal, targetTotal, "构造器费用清零");

        add(sites, 0x0639A5, "80be", "00", sourceActive, targetActive, "查询线程活动标志");
        add(sites, 0x0639D3, "c686", "01", sourceActive, targetActive, "设置线程活动标志");
        add(sites, 0x063AA5, "80be", "00", sourceActive, targetActive, "清理查询活动标志");
        add(sites, 0x063AB6, "c686", "00", sourceActive, targetActive, "清理活动标志");
        add(sites, 0x0640DE, "c683", "00", sourceActive, targetActive, "空队列结束线程");
        add(sites, 0x06489A, "889e", "", sourceActive, targetActive, "构造器活动标志清零");

        add(sites, 0x063A38, "b9", "", -sourceState, -targetState, "预算扫描反向基址");
        add(sites, 0x0648AB, "b9", "", sourceCapacity, targetCapacity, "构造器清零次数");
        add(sites, 0x0648B2, "8998", "", -4 * sourceCapacity, -4 * targetCapacity, "构造器引用数组反向位移");
        add(sites, 0x1E5840, "68", "", 140 + 8 * sourceCapacity, 140 + 8 * targetCapacity, "对象分配大小");
        // 063A85的扫描上限仍由两个既有profile的机体容量位点持有，避免重复写入。
        return List.copyOf(sites);
    }

    private static void add(List<Site> sites, int operandOffset, String prefixHex, String suffixHex,
                            int expected, int target, String label) {
        byte[] prefix = HexFormat.of().parseHex(prefixHex);
        byte[] suffix = HexFormat.of().parseHex(suffixHex);
        sites.add(new Site(operandOffset - prefix.length,
                instruction(prefix, expected, suffix), instruction(prefix, target, suffix),
                "CMekaLoader " + label));
    }

    private static byte[] instruction(byte[] prefix, int operand, byte[] suffix) {
        return ByteBuffer.allocate(prefix.length + Integer.BYTES + suffix.length)
                .order(ByteOrder.LITTLE_ENDIAN).put(prefix).putInt(operand).put(suffix).array();
    }

    public record Site(int offset, byte[] expectedBytes, byte[] targetBytes, String label) {
    }
}
