package com.giga.nexas.dto.bsdx.bin.lossless;

import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIr;
import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIrMapper;

/**
 * 新可逆 BIN 主链的门面类。
 *
 * <p>它负责把 Instruction IR、Structured IR、Lossless DSL 这几层串起来，
 * 让测试代码和未来的编辑器接线代码只依赖一个稳定入口，而不用分别认识每个阶段类。
 */
public class BsdxBinLosslessPipeline {

    private final BsdxBinStrictIrMapper strictIrMapper = new BsdxBinStrictIrMapper();
    private final BsdxBinLosslessLifter lifter = new BsdxBinLosslessLifter();
    private final BsdxBinLosslessLowerer lowerer = new BsdxBinLosslessLowerer();
    private final BsdxBinLosslessDslRenderer renderer = new BsdxBinLosslessDslRenderer();
    private final BsdxBinLosslessDslParser parser = new BsdxBinLosslessDslParser();

    /**
     * 执行无损的 {@code bin -> strict IR} 转换。
     */
    public BsdxBinStrictIr toStrictIr(Bin bin) {
        return strictIrMapper.toStrictIr(bin);
    }

    /**
     * 执行无损的 {@code strict IR -> bin} 转换。
     */
    public Bin fromStrictIr(BsdxBinStrictIr strictIr) {
        return strictIrMapper.fromStrictIr(strictIr);
    }

    /**
     * 执行无损的 {@code strict IR -> structured IR} 转换。
     */
    public BsdxBinLosslessProgram lift(BsdxBinStrictIr strictIr) {
        return lifter.lift(strictIr);
    }

    /**
     * 执行无损的 {@code structured IR -> strict IR} 转换。
     */
    public BsdxBinStrictIr lower(BsdxBinLosslessProgram program) {
        return lowerer.lower(program);
    }

    /**
     * 把 structured program 渲染成 lossless pseudo DSL 文本。
     */
    public String render(BsdxBinLosslessProgram program) {
        return renderer.render(program);
    }

    /**
     * 把 lossless pseudo DSL 解析回 structured IR。
     *
     * <p>当前 DSL 还没有独立承载非代码元数据，所以这里需要模板 program 提供那部分上下文。
     */
    public BsdxBinLosslessProgram parse(BsdxBinLosslessProgram template, String source) {
        return parser.parse(template, source);
    }
}
