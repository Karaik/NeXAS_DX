package com.giga.nexasdxeditor.dto.bsdx.waz.generator;

import cn.hutool.core.date.DateUtil;
import com.giga.nexasdxeditor.dto.bsdx.waz.Waz;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.GenerateUtil.putString;

@Slf4j
public class WazGenerator {

    public static boolean generate(String outputPath, Waz waz, String charset) {
        File originalFile = new File(outputPath);
        String parentDir = originalFile.getParent();
        String fileNameWithoutExt = originalFile.getName().replaceFirst("\\.waz$", "");
        String newFileName = fileNameWithoutExt + DateUtil.current() + ".waz";
        File newFile = new File(parentDir, newFileName);

        List<byte[]> blocks = new ArrayList<>();

        for (Waz.Skill block : waz.getSkillList()) {
            blocks.add(serializeSkill(block, charset));
        }

        try (FileOutputStream fos = new FileOutputStream(newFile)) {
            for (byte[] block : blocks) {
                fos.write(block);
            }
            return true;
        } catch (IOException e) {
            log.error("Error writing Waz file: {}", e.getMessage());
            return false;
        }
    }

    private static byte[] serializeSkill(Waz.Skill skill, String charset) {
        ByteBuffer buffer;
        if (skill.isEmpty()) {
            buffer = ByteBuffer.allocate(4);
            buffer.putInt(0);
            return buffer.array();
        }
        buffer = ByteBuffer.allocate(skill.getPhaseQuantity()*4040);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(1); // flag
        putString(buffer, skill.getSkillNameJapanese(), charset);
        putString(buffer, skill.getSkillNameEnglish(), charset);
        buffer.putInt(skill.getPhaseQuantity());

        for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
            serializeSkillPhase(buffer, phase);
        }

//        buffer.putInt(skill.getSkillSuffix().getCount());
//        buffer.putInt(skill.getSkillSuffix().getInt1());
//        buffer.putInt(skill.getSkillSuffix().getInt2());

        return buffer.array();
    }

    private static void serializeSkillPhase(ByteBuffer buffer, Waz.Skill.SkillPhase phase) {
        buffer.putInt(phase.getSkillUnitCollection().size());

        phase.getSkillUnitCollection().forEach(unit -> {
            buffer.putInt(unit.getSkillInfoObjectList().size());
            unit.getSkillInfoObjectList().forEach(info -> info.writeInfo(buffer.array()));
            buffer.putInt(unit.getSkillInfoUnknownList().size());
            unit.getSkillInfoUnknownList().forEach(info -> info.writeInfo(buffer.array()));
        });
    }

}
