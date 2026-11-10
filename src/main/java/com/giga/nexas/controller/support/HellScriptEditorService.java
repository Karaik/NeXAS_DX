package com.giga.nexas.controller.support;

import com.giga.nexas.controller.model.BsdxOverlayResourceSession;
import com.giga.nexas.controller.model.HellStageDescriptor;
import com.giga.nexas.controller.model.HellStageMetadataDraft;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessPipeline;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.BinaryExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.BinaryOperator;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.CallExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.CallStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.ConstExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.Expression;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.GotoStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.IfGotoStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.LabelStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.MarkerStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.RegisterExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.RegisterId;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.Statement;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.StringRefExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.StringRefKind;
import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIrDataSection;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.service.BsdxBinService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Hell Script Editor 的数据写回服务。
 *
 * <p>这一层负责把右侧面板里的元数据草稿写回到 `mod` 层资源，
 * 控制器通过它读写 `Dat` 和 `Bin`。
 *
 * <p>输入是覆盖会话、字符集和关卡元数据草稿；
 * 输出是刷新后的覆盖会话，调用方据此重载关卡树和来源状态。
 */
public class HellScriptEditorService {

    private static final String HELL_CONFIG_FILE = "HellConfig.dat";
    private static final String HELL_BIN_FILE = "Hell.bin";

    /**
     * `Hell.bin` 字符串表里，关卡脚本分发表的起始偏移。
     *
     * <p>这一规则来自已确认的 BSDX `Hell.bin` 实际结构：
     * `stringTable[3]` 对应 `HellConfig` 第 0 行。
     */
    private static final int HELL_BIN_STRING_OFFSET = 3;

    /**
     * `Hell.bin` 中负责按阶段索引分派子脚本的入口 bank。
     */
    private static final int HELL_DISPATCH_BANK = 9;

    /**
     * 编辑器生成的新脚本文件统一使用的前缀。
     *
     * <p>它只用于新蓝本副本文件，不改动原始游戏文件名。
     */
    private static final String DUPLICATE_SCRIPT_PREFIX = "HellMOD";

    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final BsdxBinLosslessPipeline losslessPipeline = new BsdxBinLosslessPipeline();

    /**
     * 把右侧元数据草稿写回 `mod/HellConfig.dat`。
     *
     * <p>写回目标是 `mod/HellConfig.dat`。
     * 根目录原件保持不变。
     * `mod` 中缺少目标文件时，这里会复制当前命中的源文件。
     * `HellConfig.dat` 的未编辑列保持原值。
     */
    public BsdxOverlayResourceSession saveMetadata(
            BsdxOverlayResourceSession session,
            String charset,
            HellStageMetadataDraft draft
    ) throws Exception {
        if (session == null) {
            throw new IllegalArgumentException("BSDX overlay session is required.");
        }
        if (draft == null) {
            throw new IllegalArgumentException("Hell stage metadata draft is required.");
        }

        Dat dat = loadDat(session, charset);
        List<List<Object>> rows = dat.getData();
        if (draft.getIndex() < 0 || draft.getIndex() >= rows.size()) {
            throw new IllegalArgumentException("Stage index is outside HellConfig.dat range: " + draft.getIndex());
        }

        List<Object> updatedRow = ensureRowWidth(dat, rows.get(draft.getIndex()));
        writeString(updatedRow, HellConfigLayout.TITLE, draft.getTitle());
        writeInteger(updatedRow, HellConfigLayout.HELL_LEVEL, draft.getHellLevel());
        writeInteger(updatedRow, HellConfigLayout.MAP_ID, draft.getMapId());
        writeSlots(updatedRow, HellConfigLayout.ENEMY_TYPE_START, draft.getEnemyTypes());
        writeSlots(updatedRow, HellConfigLayout.ENEMY_COUNT_START, draft.getEnemyCounts());
        writeInteger(updatedRow, HellConfigLayout.SHOP_ID, draft.getShopId());
        writeInteger(updatedRow, HellConfigLayout.PORTRAIT_ID, draft.getPortraitId());
        writeInteger(updatedRow, HellConfigLayout.BALLOON_STYLE_ID, draft.getBalloonStyleId());
        writeString(updatedRow, HellConfigLayout.DESCRIPTION, draft.getDescription());
        rows.set(draft.getIndex(), updatedRow);

        Path writablePath = session.prepareWritablePath(HELL_CONFIG_FILE);
        dat.setExtensionName("dat");
        bsdxBinService.generate(writablePath.toString(), dat, charset);
        return session.refresh();
    }

    /**
     * 以当前选中的 Hell Stage 为蓝本，在 `mod` 层追加一个新关卡。
     *
     * <p>这个流程同时处理三层数据：
     * `HellConfig.dat` 新行、
     * 蓝本脚本副本、
     * `Hell.bin` 的字符串表与显式 dispatch 分支。
     *
     * <p>所有输出文件位于 `mod`。
     */
    public DuplicateStageResult duplicateStage(
            BsdxOverlayResourceSession session,
            String charset,
            HellStageDescriptor blueprint
    ) throws Exception {
        return duplicateStage(session, charset, blueprint, null);
    }

    /**
     * 以当前选中的 Hell Stage 为蓝本，在 `mod` 层追加一个新关卡。
     *
     * <p>这个重载入口接收主界面输入的新关卡标题。
     */
    public DuplicateStageResult duplicateStage(
            BsdxOverlayResourceSession session,
            String charset,
            HellStageDescriptor blueprint,
            String targetTitle
    ) throws Exception {
        if (session == null) {
            throw new IllegalArgumentException("BSDX overlay session is required.");
        }
        if (blueprint == null || blueprint.getIndex() < 0) {
            throw new IllegalArgumentException("A valid blueprint stage is required.");
        }
        if (blueprint.getScriptFileName() == null || blueprint.getScriptFileName().isBlank()) {
            throw new IllegalArgumentException("Blueprint stage has no script file mapping.");
        }

        Dat hellConfig = loadDat(session, charset);
        if (blueprint.getIndex() >= hellConfig.getData().size()) {
            throw new IllegalArgumentException("Blueprint stage index is outside HellConfig.dat range: " + blueprint.getIndex());
        }

        String newScriptFileName = allocateDuplicateScriptFileName(session);
        int newStageIndex = hellConfig.getData().size();

        List<Object> copiedRow = new ArrayList<>(ensureRowWidth(hellConfig, hellConfig.getData().get(blueprint.getIndex())));
        writeString(copiedRow, HellConfigLayout.TITLE, buildDuplicatedTitle(copiedRow.get(HellConfigLayout.TITLE), targetTitle));
        hellConfig.getData().add(copiedRow);

        Bin duplicatedHellBin = buildDuplicatedHellBin(session, charset, newStageIndex, newScriptFileName);

        session.copyIntoMod(blueprint.getScriptFileName(), newScriptFileName);
        writeDat(session, charset, hellConfig);
        writeBin(session, charset, HELL_BIN_FILE, duplicatedHellBin);

        return new DuplicateStageResult(session.refresh(), newStageIndex, newScriptFileName);
    }

    /**
     * 读取当前覆盖会话命中的 `HellConfig.dat`。
     *
     * <p>读取顺序是 `mod` 后 `root`。
     */
    private Dat loadDat(BsdxOverlayResourceSession session, String charset) throws Exception {
        Path path = session.resolveReadPath(HELL_CONFIG_FILE)
                .orElseThrow(() -> new IllegalStateException("Missing " + HELL_CONFIG_FILE));
        ResponseDTO<?> response = bsdxBinService.parse(path.toString(), charset);
        Dat dat = (Dat) response.getData();
        dat.setExtensionName("dat");
        return dat;
    }

    /**
     * 读取当前覆盖会话命中的 `Hell.bin`。
     */
    private Bin loadBin(BsdxOverlayResourceSession session, String charset, String fileName) throws Exception {
        Path path = session.resolveReadPath(fileName)
                .orElseThrow(() -> new IllegalStateException("Missing " + fileName));
        ResponseDTO<?> response = bsdxBinService.parse(path.toString(), charset);
        Bin bin = (Bin) response.getData();
        bin.setExtensionName("bin");
        bin.setCharset(charset);
        return bin;
    }

    /**
     * 把已修改的 `Dat` 写回 `mod`。
     */
    private void writeDat(BsdxOverlayResourceSession session, String charset, Dat dat) throws Exception {
        Path writablePath = session.prepareWritablePath(HELL_CONFIG_FILE);
        dat.setExtensionName("dat");
        bsdxBinService.generate(writablePath.toString(), dat, charset);
    }

    /**
     * 把已修改的 `Bin` 写回 `mod`。
     */
    private void writeBin(BsdxOverlayResourceSession session, String charset, String fileName, Bin bin) throws Exception {
        Path writablePath = session.prepareWritablePath(fileName);
        bin.setExtensionName("bin");
        bin.setCharset(charset);
        bsdxBinService.generate(writablePath.toString(), bin, charset);
    }

    /**
     * 为新蓝本副本分配一个不会撞名的脚本文件名。
     */
    private String allocateDuplicateScriptFileName(BsdxOverlayResourceSession session) {
        for (int i = 1; i < 10_000; i++) {
            String candidate = String.format(Locale.ROOT, "%s%03d.bin", DUPLICATE_SCRIPT_PREFIX, i);
            if (!session.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique duplicated Hell script file name.");
    }

    /**
     * 生成蓝本复制后的 `Hell.bin`。
     *
     * <p>处理链是：
     * `Bin -> Structured IR -> append dispatch -> Bin`。
     */
    private Bin buildDuplicatedHellBin(
            BsdxOverlayResourceSession session,
            String charset,
            int newStageIndex,
            String newScriptFileName
    ) throws Exception {
        Bin hellBin = loadBin(session, charset, HELL_BIN_FILE);
        BsdxBinLosslessProgram program = losslessPipeline.lift(losslessPipeline.toStrictIr(hellBin));

        BsdxBinStrictIrDataSection dataSection = program.dataSection();
        List<String> stringTable = new ArrayList<>(dataSection.stringTable());
        stringTable.add(newScriptFileName);
        int newStringIndex = stringTable.size() - 1;
        int expectedStringIndex = HELL_BIN_STRING_OFFSET + newStageIndex;
        if (newStringIndex != expectedStringIndex) {
            throw new IllegalArgumentException(
                    "Unexpected Hell.bin string index mapping. expected=" + expectedStringIndex + ", actual=" + newStringIndex
            );
        }

        BsdxBinStrictIrDataSection rewrittenDataSection = new BsdxBinStrictIrDataSection(
                List.copyOf(stringTable),
                dataSection.properties(),
                dataSection.properties2(),
                dataSection.tables(),
                dataSection.constants(),
                dataSection.constants2()
        );

        List<Statement> rewrittenStatements = appendHellDispatch(program.statements(), newStageIndex + 1, newStringIndex);
        BsdxBinLosslessProgram rewrittenProgram = new BsdxBinLosslessProgram(
                program.extensionName(),
                program.charset(),
                program.preCount(),
                program.preInstructions(),
                rewrittenDataSection,
                program.tailRaw(),
                rewrittenStatements,
                null
        );

        Bin rebuilt = losslessPipeline.fromStrictIr(losslessPipeline.lower(rewrittenProgram));
        rebuilt.setExtensionName("bin");
        rebuilt.setCharset(charset);
        return rebuilt;
    }

    /**
     * 在 `Hell.bin` 的 entry 9 显式分发表末尾追加一个新分支。
     *
     * <p>实现策略是：
     * 1. 找到 entry 9 内部最长的一段连续 `if (r1 == n) goto Lx`
     * 2. 在默认 `goto returnLabel` 前插入新的 `if`
     * 3. 克隆最后一个 `CallScript(str[n], var[0], ...)` block 的形状，
     *    只替换标签名、marker 序号和字符串表索引
     */
    private List<Statement> appendHellDispatch(List<Statement> sourceStatements, int dispatchValue, int stringIndex) {
        List<Statement> statements = new ArrayList<>(sourceStatements);
        int entryStart = findDispatchEntryStart(statements);
        int entryEnd = findEntryEnd(statements, entryStart);
        String returnLabel = findReturnLabel(statements, entryStart, entryEnd);
        int defaultGotoIndex = findDefaultDispatchGotoIndex(statements, entryStart, entryEnd, returnLabel);
        String newLabel = allocateNextLabel(statements);

        statements.add(defaultGotoIndex, new IfGotoStmt(
                new BinaryExpr(
                        BinaryOperator.EQUAL,
                        new RegisterExpr(RegisterId.R1),
                        new ConstExpr(dispatchValue)
                ),
                newLabel
        ));

        int returnLabelIndex = findLabelIndex(statements, entryStart, entryEnd + 1, returnLabel);
        statements.addAll(returnLabelIndex, buildDispatchBlock(statements, entryStart, returnLabelIndex, returnLabel, newLabel, stringIndex));
        return List.copyOf(statements);
    }

    /**
     * 找到 `Hell.bin` 负责阶段分派的入口。
     */
    private int findDispatchEntryStart(List<Statement> statements) {
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            if (statement instanceof BsdxBinLosslessProgram.EntryStmt entry && entry.bank() == HELL_DISPATCH_BANK) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unable to locate Hell.bin dispatch entry bank " + HELL_DISPATCH_BANK + ".");
    }

    /**
     * 找到某个 entry 的结束位置。
     */
    private int findEntryEnd(List<Statement> statements, int entryStart) {
        for (int i = entryStart + 1; i < statements.size(); i++) {
            if (statements.get(i) instanceof BsdxBinLosslessProgram.EntryStmt) {
                return i;
            }
        }
        return statements.size();
    }

    /**
     * 找到 entry 9 末尾 `Return()` 所在的汇合 label。
     */
    private String findReturnLabel(List<Statement> statements, int entryStart, int entryEnd) {
        for (int i = entryEnd - 1; i >= entryStart; i--) {
            Statement statement = statements.get(i);
            if (statement instanceof CallStmt callStmt
                    && "Return".equalsIgnoreCase(callStmt.call().target().mnemonic())) {
                for (int j = i; j >= entryStart; j--) {
                    if (statements.get(j) instanceof LabelStmt labelStmt) {
                        return labelStmt.name();
                    }
                }
            }
        }
        throw new IllegalArgumentException("Unable to locate Hell.bin dispatch return label.");
    }

    /**
     * 找到显式 dispatch if-chain 之后的默认 `goto returnLabel`。
     */
    private int findDefaultDispatchGotoIndex(List<Statement> statements, int entryStart, int entryEnd, String returnLabel) {
        int bestRunStart = -1;
        int bestRunEnd = -1;
        int currentRunStart = -1;

        for (int i = entryStart; i < entryEnd; i++) {
            if (statements.get(i) instanceof IfGotoStmt) {
                if (currentRunStart < 0) {
                    currentRunStart = i;
                }
                if (bestRunStart < 0 || (i - currentRunStart) > (bestRunEnd - bestRunStart)) {
                    bestRunStart = currentRunStart;
                    bestRunEnd = i;
                }
            } else {
                currentRunStart = -1;
            }
        }

        int candidate = bestRunEnd + 1;
        if (bestRunStart < 0
                || candidate >= entryEnd
                || !(statements.get(candidate) instanceof GotoStmt gotoStmt)
                || !returnLabel.equals(gotoStmt.targetLabel())) {
            throw new IllegalArgumentException("Unable to locate Hell.bin dispatch fallback goto.");
        }
        return candidate;
    }

    /**
     * 基于现有最后一个 `CallScript` block 生成新的 dispatch block。
     */
    private List<Statement> buildDispatchBlock(
            List<Statement> statements,
            int entryStart,
            int returnLabelIndex,
            String returnLabel,
            String newLabel,
            int stringIndex
    ) {
        int templateLabelIndex = -1;
        for (int i = returnLabelIndex - 1; i >= entryStart; i--) {
            if (statements.get(i) instanceof LabelStmt labelStmt && !returnLabel.equals(labelStmt.name())) {
                templateLabelIndex = i;
                break;
            }
        }
        if (templateLabelIndex < 0) {
            throw new IllegalArgumentException("Unable to locate the last Hell.bin dispatch block.");
        }

        CallExpr templateCall = null;
        for (int i = templateLabelIndex; i < returnLabelIndex; i++) {
            if (statements.get(i) instanceof CallStmt callStmt) {
                templateCall = callStmt.call();
                break;
            }
        }
        if (templateCall == null || templateCall.arguments().isEmpty()) {
            throw new IllegalArgumentException("Unable to locate the template CallScript statement.");
        }

        Expression firstArgument = templateCall.arguments().get(0);
        if (!(firstArgument instanceof StringRefExpr stringRef) || stringRef.kind() != StringRefKind.STR) {
            throw new IllegalArgumentException("The template CallScript does not use a STR string table reference.");
        }

        List<Statement> templateBlock = new ArrayList<>(statements.subList(templateLabelIndex, returnLabelIndex));
        List<Statement> clonedBlock = new ArrayList<>(templateBlock.size());
        for (Statement statement : templateBlock) {
            if (statement instanceof LabelStmt) {
                clonedBlock.add(new LabelStmt(newLabel));
                continue;
            }
            if (statement instanceof CallStmt callStmt) {
                List<Expression> newArguments = new ArrayList<>(callStmt.call().arguments());
                newArguments.set(0, new StringRefExpr(StringRefKind.STR, new ConstExpr(stringIndex)));
                clonedBlock.add(new CallStmt(new CallExpr(callStmt.call().target(), List.copyOf(newArguments))));
                continue;
            }
            if (statement instanceof GotoStmt) {
                clonedBlock.add(new GotoStmt(returnLabel));
                continue;
            }
            clonedBlock.add(statement);
        }
        return List.copyOf(clonedBlock);
    }

    /**
     * 分配一个新的 `Lxxx` 标签名。
     */
    private String allocateNextLabel(List<Statement> statements) {
        int max = statements.stream()
                .filter(LabelStmt.class::isInstance)
                .map(LabelStmt.class::cast)
                .map(LabelStmt::name)
                .filter(name -> name != null && name.matches("L\\d+"))
                .map(name -> Integer.parseInt(name.substring(1)))
                .max(Comparator.naturalOrder())
                .orElse(0);
        return "L" + (max + 1);
    }

    /**
     * 在指定范围里定位某个 label 的下标。
     */
    private int findLabelIndex(List<Statement> statements, int fromInclusive, int toExclusive, String labelName) {
        for (int i = fromInclusive; i < Math.min(statements.size(), toExclusive); i++) {
            if (statements.get(i) instanceof LabelStmt labelStmt && labelName.equals(labelStmt.name())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unable to locate label: " + labelName);
    }

    /**
     * 生成蓝本复制后的默认标题。
     */
    private String buildDuplicatedTitle(Object originalTitle, String targetTitle) {
        if (targetTitle != null && !targetTitle.isBlank()) {
            return targetTitle.trim();
        }
        String source = originalTitle == null ? "" : originalTitle.toString();
        return source.isBlank() ? "Duplicated Hell Stage" : source + " Copy";
    }

    /**
     * 把目标行补齐到固定列数。
     *
     * <p>`HellConfig.dat` 是固定 23 列结构。
     * 如果上游某行长度不足，这里按列类型补默认值，避免写回时列位移。
     */
    private List<Object> ensureRowWidth(Dat dat, List<Object> row) {
        List<Object> values = row == null ? new ArrayList<>() : new ArrayList<>(row);
        while (values.size() < HellConfigLayout.COLUMN_COUNT) {
            values.add(defaultValueForColumn(dat, values.size()));
        }
        return values;
    }

    /**
     * 根据列类型生成补位默认值。
     *
     * <p>字符串列补空串，整数列补 0。
     * 这里只用于修复异常短行，不会影响正常样本的原始值。
     */
    private Object defaultValueForColumn(Dat dat, int columnIndex) {
        if (dat.getColumnTypes() != null
                && columnIndex >= 0
                && columnIndex < dat.getColumnTypes().size()
                && "string".equalsIgnoreCase(dat.getColumnTypes().get(columnIndex))) {
            return "";
        }
        return 0;
    }

    /**
     * 写入字符串列。
     */
    private void writeString(List<Object> row, int columnIndex, String value) {
        row.set(columnIndex, value == null ? "" : value);
    }

    /**
     * 写入整数列。
     *
     * <p>元数据面板的数字字段在进入服务前已经完成校验，
     * 因此这里直接按整数写回，不做模糊兜底。
     */
    private void writeInteger(List<Object> row, int columnIndex, Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing required integer column: " + columnIndex);
        }
        row.set(columnIndex, value);
    }

    /**
     * 把固定 8 槽的敌机类型/数量列表写回对应列段。
     *
     * <p>这个过程按槽位逐项落列，不会重新排序，也不会压缩空槽。
     */
    private void writeSlots(List<Object> row, int columnStart, List<Integer> values) {
        if (values == null || values.size() != HellConfigLayout.ENEMY_SLOT_COUNT) {
            throw new IllegalArgumentException("Expected " + HellConfigLayout.ENEMY_SLOT_COUNT + " slot values.");
        }
        for (int i = 0; i < HellConfigLayout.ENEMY_SLOT_COUNT; i++) {
            Integer value = values.get(i);
            if (value == null) {
                throw new IllegalArgumentException("Slot " + i + " is missing.");
            }
            row.set(columnStart + i, value);
        }
    }

    /**
     * 蓝本复制阶段返回的新会话和新关卡索引。
     *
     * <p>controller 用它来刷新树并重新选中新复制出来的关卡。
     */
    public record DuplicateStageResult(
            BsdxOverlayResourceSession session,
            int stageIndex,
            String scriptFileName
    ) {
    }
}
