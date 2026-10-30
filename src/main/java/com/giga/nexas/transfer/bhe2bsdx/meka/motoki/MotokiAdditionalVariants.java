package com.giga.nexas.transfer.bhe2bsdx.meka.motoki;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;


/** Motoki専用。既存空投の二技能と新五武装を分け、公共payloadを主WAZへ移さない。 */
final class MotokiAdditionalVariants {
    private static final List<String> NEW_NAMES = List.of("ワンツー・離脱", "ドリル・確定突き", "排熱弾・即発", "ガンポッド・三連点射", "ビーム・短照射");
    private static final List<String> NEW_ENGLISH = List.of("DX_MOTOKI_ONE_TWO_EXIT", "DX_MOTOKI_DRILL_CONFIRM", "DX_MOTOKI_QUICK_EXHAUST", "DX_MOTOKI_THREE_SHOT_GUNPOD", "DX_MOTOKI_SHORT_BEAM");
    private MotokiAdditionalVariants() { }

    private static void appendStaged(Mek mek, Waz waz) {
        var punch = donor(waz, "ワンツーブロー", "0017", 2, "0=13:0:4,13:5:11,13:12:19~;6=14:8:19~;8=9:0:0~;10=3:0:0,3:1:1~;15=10:8:19~;24=0:0:0~;26=0:0:0~;33=12:0:0~;35=0:0:0~;36=0:0:0~;45=17:0:19~;52=23:0:0~;70=0:0:0~;71=34:19:19~|0=13:0:9,13:10:12,13:13:17,13:18:18,13:19:20,13:21:22,13:23:24,13:25:31,13:32:34,13:35:37,13:38:41,13:42:42,13:43:44,13:45:46,13:47:48,13:49:57,13:58:58,13:59:59,13:60:81,13:82:89~;6=14:2:17,14:18:24,14:32:41,14:42:59~;10=3:0:0,3:18:18,3:35:35,3:48:48~;15=10:15:41~;18=3:15:22,3:36:46~;35=0:0:0~;36=0:56:56~;41=16:18:22,16:42:46~;45=17:0:55~;46=18:48:55~;52=23:0:0,23:13:13,23:13:13,23:24:24,23:42:42,23:42:42,23:60:60~;53=24:9:9,24:38:38~;70=0:22:22~;71=34:55:55,34:92:92~");
        var drill = donor(waz, "吸い込み：ドリル", "0020", 5, "0=13:0:3,13:4:7,13:8:15,13:16:29,13:30:37,13:38:44,13:45:50,13:51:55,13:56:59,13:60:62,13:63:64~;6=14:2:37~;10=3:0:0~;24=0:0:0~;27=0:0:33~;28=0:0:33~;33=12:0:0~;51=22:0:0~;52=23:7:7,23:29:29,23:37:37~;53=24:55:55~;70=0:0:0~;71=34:64:64~|0=13:0:1,13:2:3~255:0:3;6=14:0:138~255:0:138;10=3:0:0~;15=10:0:0~;19=3:0:0~;21=2:0:138~255:0:138;30=0:0:0~;41=16:0:138~255:0:138;45=17:0:138~255:0:138;48=19:0:138,19:0:138,19:0:138~255:0:138;50=21:0:0~255:0:2;52=23:0:138,23:0:138~255:0:138;71=34:0:138,34:0:138,34:0:138~255:0:138|0=13:0:1,13:2:3~255:0:3;6=14:0:138~255:0:138;10=3:0:0~;15=10:0:0~;19=3:0:0~;21=2:0:138~255:0:138;41=16:0:138~255:0:138;45=17:0:138~255:0:138;48=19:0:138~255:0:138;50=21:0:0~255:0:0;52=23:0:138,23:0:138~255:0:138;70=0:0:0~;71=34:0:138,34:0:138~255:0:138|0=13:0:2,13:3:6,13:7:11,13:12:17,13:18:24,13:25:32,13:33:82,13:83:88,13:89:98,13:99:118,13:119:126,13:127:132~;6=14:89:105~;36=0:0:0~;41=16:0:2~;52=23:0:0,23:88:88~;71=34:132:132~|0=13:0:1,13:2:3,13:4:6,13:7:9,13:10:13,13:14:18,13:19:24,13:25:30,13:31:40,13:41:50,13:51:56,13:57:60~;6=14:31:47~;36=0:0:0~;41=16:0:3~;52=23:0:0,23:30:30~;71=34:60:60~");
        var exhaust = donor(waz, "吸い込み：エグゾーストキャノン", "E_FINGER04", 8, "0=13:0:29~;6=14:8:29~;10=3:0:0~;24=0:0:0~;26=0:0:0~;28=0:29:29~;36=0:0:0~;45=17:8:29~;52=23:0:0~;53=24:10:10~;70=0:0:0~;71=34:29:29~|0=13:0:1,13:2:3~255:0:3;6=14:1:6~;10=3:0:0~;19=3:0:0~;35=0:0:0,0:40:40,0:79:79,0:124:124~;48=19:0:0,19:1:20,19:21:76,19:77:124,19:1:20,19:21:76,19:77:124~;50=21:5:5~255:5:6;52=23:0:124,23:0:0~;71=34:5:123,34:5:123,34:5:123,34:124:124~|48=19:0:0,19:0:0,19:0:0,19:0:0~;52=23:0:0~;71=34:10:10,34:10:10,34:10:10,34:10:10~|0=13:0:1,13:2:4,13:5:9~;6=14:2:9~;10=3:0:0~;15=10:2:2~;16=3:2:2~;18=3:2:9~;19=3:5:5~;27=0:5:10~;28=0:5:5~;41=16:1:1~;48=19:1:1,19:1:1,19:1:1,19:1:1~;52=23:0:0~;66=31:0:10~;70=0:0:0~;71=34:10:10~|0=13:0:1,13:2:4,13:5:11~;6=14:2:14~;10=3:0:0~;15=10:2:2~;16=3:2:2~;18=3:2:14~;19=3:5:5~;27=0:5:15~;28=0:5:5~;41=16:1:1~;48=19:1:1,19:1:1,19:1:1,19:1:1,19:1:1,19:1:1~;50=21:0:0~;52=23:0:0~;60=25:1:10~;66=31:0:15~;69=0:0:1~;70=0:0:0~;71=34:15:15~|0=13:0:1,13:2:4,13:5:11~;6=14:2:19~;10=3:0:0~;15=10:2:2~;16=3:2:2~;18=3:2:19~;19=3:5:5~;27=0:5:20~;28=0:5:5~;41=16:1:1~;48=19:1:1,19:1:1,19:1:1,19:1:1,19:1:1,19:1:1,19:1:1,19:1:1~;50=21:0:0~;52=23:0:0~;60=25:1:15~;66=31:0:20~;69=0:0:1~;70=0:0:0~;71=34:20:20~|0=13:0:1,13:2:4,13:5:11~;6=14:2:24~;10=3:0:0~;15=10:2:2~;16=3:2:2~;18=3:2:20~;19=3:5:5~;27=0:5:25~;28=0:5:5~;41=16:1:1~;48=19:1:1,19:1:1,19:1:1,19:1:1,19:1:1,19:1:1,19:1:1,19:1:1~;50=21:0:0~;52=23:0:0~;53=24:0:0~;60=25:1:20~;61=26:0:3,26:4:24~;65=30:0:6~;66=31:0:25~;70=0:0:0~;71=34:25:25~|0=13:0:17,13:18:29~;7=15:0:15~;28=0:0:0~;36=0:0:0~;71=34:29:29~");
        var pod = donor(waz, "ガンポッド", "GATRING04", 3, "0=13:0:3,13:4:11,13:12:18,13:19:24,13:25:29,13:30:33,13:34:36,13:37:38,13:39:39,13:40:40,13:41:41,13:42:42,13:43:43,13:44:44~;10=3:0:0~;19=3:0:0~;24=0:0:19~;27=0:0:19~;35=0:0:0~;36=0:0:0~;45=17:29:44~;52=23:0:0,23:14:14~;53=24:3:3~;70=0:0:0~;71=34:44:44~|0=13:0:1,13:2:3~;6=14:0:2~;10=3:0:0~;35=0:0:0~;40=0:0:0~;41=16:0:2~;45=17:0:2~;46=18:2:2~;48=19:0:0,19:0:0,19:0:0,19:0:0,19:2:2,19:2:2,19:2:2,19:2:2~;52=23:0:0,23:0:2~255:0:2;60=25:0:0~;71=34:2:2,34:2:2~|0=13:0:1,13:2:4,13:5:8,13:9:13,13:14:19,13:20:26,13:27:36,13:37:56~;10=3:0:31~;24=0:27:56~;27=0:0:25~;36=0:0:0~;47=0:0:20,0:21:21~;52=23:0:0~;70=0:0:0~;71=34:56:56~");
        var beam = donor(waz, "ビーム", "0021", 1, "0=13:0:5,13:6:35,13:36:65,13:66:73,13:74:77,13:78:107,13:108:137,13:138:167~;6=14:6:20,14:78:95~;10=3:0:35~;24=0:0:0~;45=17:0:36~;47=0:137:137~;48=19:36:65,19:108:137~;52=23:0:0,23:26:26,23:66:66,23:100:100,23:120:120~;53=24:30:30,24:103:103~;70=0:0:0,0:36:36~;71=34:167:167~");

        append(mek, waz, punch, "ワンツーブロー", "0001", 0, 18,
                "二発で打ち切るワンツー。押し続けても繰り返さず、二発目から追撃かダッシュでの離脱を選べる。", s -> {
            requireNormalEnd(at(s, 0, 71, CEventChange.class, 19, 19));
            hitPower(at(s, 1, 41, CEventHit.class, 18, 22), 18, 22, 40, 0);
            hitPower(at(s, 1, 41, CEventHit.class, 42, 46), 42, 46, 40, 1);
            var oldEnd = at(s, 1, 71, CEventChange.class, 92, 92);
            condition(oldEnd, List.of(0), List.of()); destination(oldEnd, List.of(0, 2), List.of());
            attackEntries(s); counter(s, 0); counter(s, 1); clear(s, 1, 46);
            gate(s, 1, 47, 63); set(s, 1, 71, List.of(end(64))); clip(s, 1, 64);
        });

        append(mek, waz, drill, "ドリル", "0003", 1, 12,
                "短く突き出したドリルが当たると、締めの一突きへ移る。空振りでは追い続けず、素早く構え直す。", s -> {
            hitPower(at(s, 1, 41, CEventHit.class, 0, 138), 0, 138, 20, 0);
            hitPower(at(s, 3, 41, CEventHit.class, 0, 2), 0, 2, 20, 1);
            var e = effects(s, 1); if (e.size() != 3) throw invalid("钻头吸附及视觉发射组数量漂移");
            emitter(e.get(0), 0, 138, 138, 1, 0, -1, -1, 1, 0);
            for (int i : List.of(1, 2)) emitter(e.get(i), 0, 138, 138, 0, 0, 300, -1, 1, 0);
            attackEntries(s);
            poses(s, 0, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 10, 11, 12);
            window(at(s, 0, 52, CEventSe.class, 7, 7), 2, 2);
            window(at(s, 0, 52, CEventSe.class, 29, 29), 6, 6);
            window(at(s, 0, 52, CEventSe.class, 37, 37), 8, 8);
            window(at(s, 0, 53, CEventVoice.class, 55, 55), 3, 3);
            set(s, 0, 71, List.of(input(12, 1))); clip(s, 0, 12);
            poses(s, 1, new int[]{0, 1}, 0, 3, 4, 8);
            var contact = at(s, 1, 41, CEventHit.class, 0, 138); window(contact, 0, 8); contact.setHitCount(1);
            List<SkillInfoObject> visual = new ArrayList<>();
            for (int i : List.of(1, 2)) {
                var v = e.get(i); window(v, 0, 8);
                for (var u : v.getCeventEffectUnitList()) window(u.getData(), 0, 8); visual.add(v);
            }
            set(s, 1, 48, visual); set(s, 1, 71, List.of(route(0, 8, hit(), List.of(0, 1), List.of(3)), input(9, 4)));
            clip(s, 1, 9);
            limitedRecovery(s, 2, 1, 4);
            set(s, 3, 71, List.of(input(4, 4))); clip(s, 3, 4);
            limitedRecovery(s, 4, 20, -1); gate(s, 4, 0, 19);
        });

        append(mek, waz, exhaust, "エグゾーストキャノン", "0005", 2, 18,
                "吸い込みや溜めを挟まず、低出力の排熱弾をすぐに放つ。近距離の追撃と短い連係の締めに使う。", s -> {
            hitPower(at(s, 3, 41, CEventHit.class, 1, 1), 1, 1, 20, 1);
            var e = effects(s, 3); if (e.size() != 4) throw invalid("低档排热两组爆点/视觉数量漂移");
            for (int i = 0; i < 4; i++) emitter(e.get(i), 1, 1, 0, i % 2 == 0 ? 1 : 0, i % 2 == 0 ? 16 : 0, -1, -1, 1, 0);
            attackEntries(s);
            poses(s, 0, new int[]{0}, 0, 10); set(s, 0, 71, List.of(input(10, 3))); counter(s, 0); clip(s, 0, 10);
            var fallbackPose = copy(sprites(s, 7).get(0), CEventSprite.class);
            for (int p : List.of(1, 2, 4, 5, 6)) {
                if (sprites(s, p).isEmpty()) set(s, p, 0, List.of(copy(fallbackPose, CEventSprite.class)));
                limitedRecovery(s, p, 1, 7);
            }
            set(s, 3, 71, List.of(input(10, 7))); counter(s, 3); clip(s, 3, 10);
            poses(s, 7, new int[]{0, 1}, 0, 11, 12, 18);
            physics(s, 7); setArmor(s, 7, 0, 0); counter(s, 7);
            gate(s, 7, 0, 17); set(s, 7, 71, List.of(end(18))); clip(s, 7, 18);
        });

        append(mek, waz, pod, "ガンポッド", "0006", 3, 12,
                "三度だけ点射して構え直すガンポッド。撃ち続けず、既に捕らえた相手への追撃に使う。", s -> {
            var e = effects(s, 1); if (e.size() != 8) throw invalid("枪口双批数量漂移");
            for (int i = 0; i < 8; i++) emitter(e.get(i), i < 4 ? 0 : 2, i < 4 ? 0 : 2, 0,
                    i % 4 == 0 ? 1 : 0, i % 4 == 0 || i % 4 == 2 ? 16 : 0,
                    i % 4 == 2 ? 300 : -1, i % 4 == 2 ? 100 : -1, 1, 0);
            hitPower(at(s, 1, 41, CEventHit.class, 0, 2), 0, 2, 0, 1);
            attackEntries(s);
            poses(s, 0, new int[]{0, 1, 2, 3, 4, 5, 6, 13}, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 6, 7, 9, 10, 10);
            window(at(s, 0, 52, CEventSe.class, 14, 14), 7, 7);
            set(s, 0, 71, List.of(next(10))); counter(s, 0); clip(s, 0, 10);
            List<SkillInfoObject> shots = new ArrayList<>();
            for (int frame : List.of(0, 2, 4)) for (int i = 0; i < 4; i++) shots.add(movedEmitter(e.get((frame == 2 ? 4 : 0) + i), frame, false));
            set(s, 1, 48, shots); clear(s, 1, 40, 41, 46); counter(s, 1);
            var originalPoses = sprites(s, 1); var lastPose = copy(originalPoses.get(0), CEventSprite.class); window(lastPose, 4, 5);
            unit(s, 1, 0).getSkillInfoObjectList().add(lastPose);
            window(at(s, 1, 6, CEventSpriteYure.class, 0, 2), 0, 4);
            var sound = at(s, 1, 52, CEventSe.class, 0, 0); List<SkillInfoObject> sounds = new ArrayList<>();
            for (int frame : List.of(0, 2, 4)) { var v = copy(sound, CEventSe.class); window(v, frame, frame); sounds.add(v); }
            set(s, 1, 52, sounds); set(s, 1, 71, List.of(next(5))); clip(s, 1, 5);
            poses(s, 2, new int[]{0, 1, 2, 3, 4, 5, 6, 7}, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16);
            set(s, 2, 10, List.of(six(3, 0, 0, 0, 0, 0, 0, 0, 0)));
            set(s, 2, 24, List.of(val(0, 0, 100))); counter(s, 2);
            gate(s, 2, 0, 15); set(s, 2, 71, List.of(end(16))); clip(s, 2, 16);
        });

        append(mek, waz, beam, "ビーム", "0010", 4, 24,
                "短く構えて一度だけビームを照射し、構え直す。長く二度撃たず、相手を捕らえた後の追撃に使う。", MotokiAdditionalVariants::shortBeam);
    }

    private static void shortBeam(Waz.Skill s) {
        var e = effects(s, 0); if (e.size() != 2) throw invalid("源beam双批数量漂移");
        emitter(e.get(0), 36, 65, 29, 1, 16, -1, -1, 5, 30);
        emitter(e.get(1), 108, 137, 29, 1, 16, -1, -1, 5, -30);
        requireSix(effectData(e.get(0), 4), 0, 1, 0, 0, 1, 0);
        requireSix(effectData(e.get(0), 6), 0, 5, 0, 0, 5, 0);
        requireSix(effectData(e.get(0), 31), 0, 0, 0, 0, 24, 0);
        if (!(effectData(e.get(0), 7) instanceof CEventTerm position)
                || position.getBsdxInfoCollectionList().size() != 1
                || !List.of(0, 3).equals(position.getBsdxInfoCollectionList().get(0).getIntList3()))
            throw invalid("beam动态枪口[0,3]漂移");
        requireNormalEnd(at(s, 0, 71, CEventChange.class, 167, 167));
        noCharge(s); physics(s, 0); setArmor(s, 0, 0, 0);
        poses(s, 0, new int[]{0, 1, 2, 3, 7}, 0, 5, 6, 17, 18, 47, 48, 55, 56, 77);
        var vibration = at(s, 0, 6, CEventSpriteYure.class, 6, 20); window(vibration, 6, 17); set(s, 0, 6, List.of(vibration));
        var turn = at(s, 0, 10, CEventValRandom.class, 0, 35); requireSix(turn, 0, 50, 0, 0, 5, 0); window(turn, 0, 17);
        set(s, 0, 10, List.of(turn, six(3, 48, 48, 0, 0, 0, 0, 0, 0)));
        var lockedMove = at(s, 0, 24, CEventVal.class, 0, 0);
        if (!List.of(0, 0, 0, 0).equals(Arrays.asList(lockedMove.getInt1(), lockedMove.getInt2(), lockedMove.getInt3(), lockedMove.getInt4())))
            throw invalid("beam源移动锁漂移");
        set(s, 0, 24, List.of(lockedMove, val(48, 48, 100)));
        window(at(s, 0, 45, CEventEscape.class, 0, 36), 0, 18);
        window(e.get(0), 18, 47); set(s, 0, 48, List.of(e.get(0))); gate(s, 0, 48, 76);
        var initialSe = at(s, 0, 52, CEventSe.class, 0, 0); var launchSe = at(s, 0, 52, CEventSe.class, 26, 26); window(launchSe, 8, 8);
        set(s, 0, 52, List.of(initialSe, launchSe));
        var voice = at(s, 0, 53, CEventVoice.class, 30, 30); window(voice, 12, 12); set(s, 0, 53, List.of(voice));
        window(at(s, 0, 70, CEventVal.class, 36, 36), 18, 18);
        unit(s, 0, 21).getSkillInfoObjectList().add(six(2, 48, 48, 0, 50, 0, 0, 50, 0));
        for (int slot : List.of(27, 28)) unit(s, 0, slot).getSkillInfoObjectList().add(val(48, 48, 100));
        unit(s, 0, 36).getSkillInfoObjectList().add(val(48, 48, 0));
        set(s, 0, 71, List.of(end(77))); clip(s, 0, 77);
        for (var p : s.getPhasesInfo()) for (var u : p.getSkillUnitCollection()) for (var v : u.getSkillInfoObjectList())
            if (v.getStartFrame() > 77 || v.getEndFrame() > 77) throw invalid("beam旧后半事件未完全裁除");
    }

    private static final ObjectMapper JSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static void appendAll(Mek mek, Waz waz) {
        Mek stagedMek = copy(mek, Mek.class); Waz stagedWaz = copy(waz, Waz.class);
        boolean existing = removeNewBatch(stagedMek, stagedWaz);
        MotokiFlightBombVariant.append(stagedMek, stagedWaz);
        appendStaged(stagedMek, stagedWaz);
        if (existing) {
            if (!payload(mek).equals(payload(stagedMek)) || !payload(waz).equals(payload(stagedWaz)))
                throw invalid("既有六武装、私有技能或Material载荷已漂移");
            return;
        }
        int oldWeapons = mek.getMekWeaponInfoMap().size(), oldSkills = waz.getSkillList().size();
        int added = stagedMek.getMekWeaponInfoMap().size() - oldWeapons;
        int extraSkills = stagedWaz.getSkillList().size() - oldSkills;
        if ((added != 5 && added != 6) || extraSkills != (added == 6 ? 7 : 5))
            throw invalid("完整六武装追加数量与私有技能不对应");
        Map<Integer, Mek.MekWeaponInfo> weapons = new LinkedHashMap<>(mek.getMekWeaponInfoMap());
        for (int i = oldWeapons; i < stagedMek.getMekWeaponInfoMap().size(); i++) weapons.put(i, stagedMek.getMekWeaponInfoMap().get(i));
        List<Waz.Skill> skills = new ArrayList<>(waz.getSkillList());
        skills.addAll(stagedWaz.getSkillList().subList(oldSkills, stagedWaz.getSkillList().size()));
        var block = mek.getMekMaterialBlock();
        var regular = new ArrayList<>(block.getRegularEntries()); var trailing = new ArrayList<>(block.getTrailingEntries());
        trailing.addAll(stagedMek.getMekMaterialBlock().getTrailingEntries().subList(oldWeapons, stagedMek.getMekWeaponInfoMap().size()));
        var entries = new ArrayList<>(regular); entries.addAll(trailing);
        // All validation, copying and list construction has finished. Preserve every original row/object.
        mek.setMekWeaponInfoMap(weapons); waz.setSkillList(skills);
        block.setRegularEntries(regular); block.setTrailingEntries(trailing); block.setEntries(entries);
    }

    private static boolean removeNewBatch(Mek mek, Waz waz) {
        long names = mek.getMekWeaponInfoMap().values().stream().filter(w -> NEW_NAMES.contains(w.getWeaponName())).count();
        long identities = waz.getSkillList().stream().filter(Objects::nonNull)
                .filter(s -> s.getSkillNameJapanese() != null && NEW_NAMES.contains(s.getSkillNameJapanese())
                        || s.getSkillNameEnglish() != null && NEW_ENGLISH.contains(s.getSkillNameEnglish())).count();
        if (names == 0 && identities == 0) return false;
        if (names != 5 || identities != 5) throw invalid("新增五武装身份缺失、重复或只存在一半");
        int firstWeapon = mek.getMekWeaponInfoMap().size() - 5, firstSkill = waz.getSkillList().size() - 5;
        var b = mek.getMekMaterialBlock();
        if (firstWeapon < 0 || firstSkill < 0 || b == null || b.getTrailingEntries().size() != mek.getMekWeaponInfoMap().size())
            throw invalid("新增五武装尾表或Material布局不完整");
        var combined = new ArrayList<>(b.getRegularEntries()); combined.addAll(b.getTrailingEntries());
        if (!payload(combined).equals(payload(b.getEntries()))) throw invalid("Material三视图漂移");
        for (int i = 0; i < 5; i++) {
            var w = mek.getMekWeaponInfoMap().get(firstWeapon + i); var s = waz.getSkillList().get(firstSkill + i);
            if (w == null || !NEW_NAMES.get(i).equals(w.getWeaponName()) || !Integer.valueOf(firstSkill + i).equals(w.getWazSequence())
                    || !NEW_NAMES.get(i).equals(s.getSkillNameJapanese()) || !NEW_ENGLISH.get(i).equals(s.getSkillNameEnglish()))
                throw invalid("新增五武装必须保持完整双名、引用和尾部顺序");
        }
        for (int i = 0; i < 5; i++) mek.getMekWeaponInfoMap().remove(firstWeapon + i);
        waz.getSkillList().subList(firstSkill, firstSkill + 5).clear();
        b.getTrailingEntries().subList(firstWeapon, firstWeapon + 5).clear();
        b.getEntries().subList(b.getRegularEntries().size() + firstWeapon, b.getEntries().size()).clear();
        return true;
    }

    private static Waz.Skill donor(Waz waz, String jp, String en, int phases, String expectedShape) {
        Waz.Skill s = skill(waz, jp, en, phases);
        if (!Integer.valueOf(phases).equals(s.getPhaseQuantity()) || s.getSkillSuffixList().size() != phases)
            throw invalid(jp + " phase/suffix数量漂移");
        for (int i = 0; i < phases; i++) if (!Integer.valueOf(i).equals(s.getSkillSuffixList().get(i).getInt1())
                || !Integer.valueOf(0).equals(s.getSkillSuffixList().get(i).getInt2())) throw invalid(jp + " suffix入口漂移");
        if (!expectedShape.equals(shape(s))) throw invalid(jp + " 原事件类型、帧窗或未知循环记录漂移");
        return s;
    }
    /** Resource-independent shape signature: final group/skill/SPM numbers remain late-bound. */
    private static String shape(Waz.Skill s) {
        List<String> phases = new ArrayList<>();
        for (var p : s.getPhasesInfo()) {
            List<String> units = new ArrayList<>();
            for (var u : p.getSkillUnitCollection().stream().sorted(Comparator.comparing(SkillUnit::getUnitQuantity)).toList())
                if (!u.getSkillInfoObjectList().isEmpty() || !u.getSkillInfoUnknownList().isEmpty())
                    units.add(u.getUnitQuantity() + "=" + eventShape(u.getSkillInfoObjectList()) + "~" + eventShape(u.getSkillInfoUnknownList()));
            phases.add(String.join(";", units));
        }
        return String.join("|", phases);
    }
    private static String eventShape(List<? extends SkillInfoObject> events) {
        return String.join(",", events.stream().map(e -> e.getTypeId() + ":" + e.getStartFrame() + ":" + e.getEndFrame()).toList());
    }
    private static void append(Mek mek, Waz waz, Waz.Skill source, String sourceName, String sequence,
                               int variant, int heat, String description, Consumer<Waz.Skill> editor, Mek.MekWeaponInfo... materials) {
        CharacterWeaponVariants.append(mek, waz, weapon(mek, waz, source, sourceName, sequence), source,
                NEW_NAMES.get(variant), NEW_ENGLISH.get(variant), heat, description, editor, materials);
    }
    private static <T extends SkillInfoObject> T at(Waz.Skill s, int p, int slot, Class<T> type, int start, int end) {
        return event(s, p + 1, slot, type, start, end, start, end);
    }
    private static SkillUnit unit(Waz.Skill s, int p, int slot) {
        var units = s.getPhasesInfo().get(p).getSkillUnitCollection();
        var found = units.stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).toList();
        if (found.size() > 1) throw invalid("槽位重复");
        if (!found.isEmpty()) return found.get(0);
        SkillUnit u = new SkillUnit(slot, "追加武装専用"); units.add(u); units.sort(Comparator.comparing(SkillUnit::getUnitQuantity)); return u;
    }
    private static void set(Waz.Skill s, int p, int slot, List<? extends SkillInfoObject> events) {
        SkillUnit u = unit(s, p, slot); u.setSkillInfoObjectList(new ArrayList<>(events)); u.setSkillInfoUnknownList(new ArrayList<>());
    }
    private static void clear(Waz.Skill s, int p, int... slots) { for (int slot : slots) s.getPhasesInfo().get(p).getSkillUnitCollection().removeIf(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())); }
    private static List<CEventEffect> effects(Waz.Skill s, int p) { return unit(s, p, 48).getSkillInfoObjectList().stream().map(e -> {
        if (!(e instanceof CEventEffect v)) throw invalid("Effect类型漂移"); return v;
    }).toList(); }
    private static List<CEventSprite> sprites(Waz.Skill s, int p) { return unit(s, p, 0).getSkillInfoObjectList().stream().map(e -> {
        if (!(e instanceof CEventSprite v)) throw invalid("Sprite类型漂移"); return v;
    }).toList(); }
    private static void window(SkillInfoObject e, int start, int end) { e.setStartFrame(start); e.setEndFrame(end); }
    private static void poses(Waz.Skill s, int p, int[] indices, int... windows) {
        var source = sprites(s, p); List<SkillInfoObject> result = new ArrayList<>();
        if (windows.length != indices.length * 2) throw invalid("姿势时间表不完整");
        for (int i = 0; i < indices.length; i++) { var v = copy(source.get(indices[i]), CEventSprite.class); window(v, windows[i * 2], windows[i * 2 + 1]); result.add(v); }
        set(s, p, 0, result);
    }
    private static void clip(Waz.Skill s, int p, int last) {
        for (var u : s.getPhasesInfo().get(p).getSkillUnitCollection()) {
            u.setSkillInfoUnknownList(new ArrayList<>());
            u.getSkillInfoObjectList().removeIf(e -> e.getStartFrame() > last);
            for (var e : u.getSkillInfoObjectList()) if (e.getEndFrame() > last) {
                if (e instanceof CEventEffect) throw invalid("多帧Effect必须显式校准内层时间");
                e.setEndFrame(last);
            }
        }
        // Only new private copies call clip: remove units emptied by this batch's Charge/loop removal or timing edit.
        s.getPhasesInfo().get(p).getSkillUnitCollection().removeIf(u ->
                u.getSkillInfoObjectList().isEmpty() && u.getSkillInfoUnknownList().isEmpty());
    }
    private static void noCharge(Waz.Skill s) {
        for (var p : s.getPhasesInfo()) for (var u : p.getSkillUnitCollection()) {
            u.getSkillInfoObjectList().removeIf(CEventCharge.class::isInstance); u.setSkillInfoUnknownList(new ArrayList<>());
        }
    }
    private static CEventVal val(int frame, int end, int value) { var v = new CEventVal(0); window(v, frame, end); v.setInt1(0); v.setInt2(value); v.setInt3(0); v.setInt4(value); return v; }
    private static CEventValRandom six(int type, int start, int end, int a, int b, int c, int d, int e, int f) {
        var v = new CEventValRandom(type); window(v, start, end); v.setInt1(a); v.setInt2(b); v.setInt3(c); v.setInt4(d); v.setInt5(e); v.setInt6(f); return v;
    }
    private static void physics(Waz.Skill s, int p) {
        set(s, p, 21, List.of(six(2, 0, 0, 0, 50, 0, 0, 50, 0)));
        set(s, p, 27, List.of(val(0, 0, 100))); set(s, p, 28, List.of(val(0, 0, 100)));
    }
    private static void attackEntries(Waz.Skill s) {
        noCharge(s);
        for (int p = 0; p < s.getPhasesInfo().size(); p++) { physics(s, p); setArmor(s, p, 0, 0); }
    }
    private static void setArmor(Waz.Skill s, int p, int frame, int value) {
        var v = val(frame, frame, value); v.setInt2(0); set(s, p, 36, List.of(v));
    }
    private static void counter(Waz.Skill s, int p) { set(s, p, 35, List.of(val(0, 0, 0))); }
    private static void gate(Waz.Skill s, int p, int frame, int sdLast) {
        List<SkillInfoObject> result = new ArrayList<>(); result.add(val(frame, frame, 1));
        if (sdLast > frame) result.add(val(frame + 1, sdLast, 3)); set(s, p, 47, result);
    }
    private static void speedZero(Waz.Skill s, int p, int frame) { var v = six(3, frame, frame, 0, 0, 0, 0, 0, 0); unit(s, p, 18).getSkillInfoObjectList().add(v); }
    private static BsdxInfoCollection term(int root, List<Integer> types, List<Integer> params, List<Integer> objects) {
        var v = new BsdxInfoCollection(); v.setInt1(root); v.setInt2(0); v.setTypeList(new ArrayList<>(types)); v.setParamList(new ArrayList<>(params)); v.setIntList3(new ArrayList<>(objects)); return v;
    }
    private static CEventChange route(int start, int end, BsdxInfoCollection condition, List<Integer> types, List<Integer> params) {
        var c = new CEventChange(34); window(c, start, end); c.setFlag(1); c.setInt1(0);
        c.setBsdxInfoCollectionList1(new ArrayList<>(List.of(condition))); c.setBsdxInfoCollectionList2(new ArrayList<>(List.of(term(16, types, params, List.of())))); return c;
    }
    private static BsdxInfoCollection always() { return term(0, List.of(0), List.of(), List.of()); }
    private static BsdxInfoCollection hit() { return term(0, List.of(2, 6, 0, 0), List.of(0), List.of(0, 0)); }
    private static CEventChange next(int frame) { return route(frame, frame, always(), List.of(0, 0), List.of()); }
    private static CEventChange end(int frame) { return route(frame, frame, always(), List.of(0, 2), List.of()); }
    private static CEventChange input(int frame, int entry) { return route(frame, frame, always(), List.of(0, 1), List.of(entry)); }
    private static CEventChange jump(int frame, int group, int index) { return route(frame, frame, always(), List.of(1), List.of(group, index)); }
    private static void condition(CEventChange c, List<Integer> types, List<Integer> params) {
        if (!Integer.valueOf(1).equals(c.getFlag()) || c.getBsdxInfoCollectionList1().size() != 1)
            throw invalid("条件分支数量或flag漂移");
        var v = c.getBsdxInfoCollectionList1().get(0);
        if (!Integer.valueOf(0).equals(v.getInt1()) || !types.equals(v.getTypeList()) || !params.equals(v.getParamList()))
            throw invalid("条件语义漂移");
    }
    private static void destination(CEventChange c, List<Integer> types, List<Integer> params) {
        if (c.getBsdxInfoCollectionList2().size() != 1) throw invalid("出口数量漂移");
        var d = c.getBsdxInfoCollectionList2().get(0);
        if (!Integer.valueOf(16).equals(d.getInt1()) || !types.equals(d.getTypeList()) || !params.equals(d.getParamList()))
            throw invalid("出口语义漂移");
    }
    private static void requireJump(CEventChange c, int group, int index) {
        condition(c, List.of(0), List.of());
        if (c.getBsdxInfoCollectionList2().size() != 1) throw invalid("WAZA出口数量漂移");
        var d = c.getBsdxInfoCollectionList2().get(0);
        if (!Integer.valueOf(16).equals(d.getInt1()) || !List.of(1).equals(d.getTypeList()) || !List.of(group, index).equals(d.getParamList()))
            throw invalid("WAZA出口身份漂移");
    }
    private static void hitPower(CEventHit h, int start, int end, int power, int cancel) {
        attack(h, start, end, power, start, end, power);
        if (!Integer.valueOf(cancel).equals(h.getSelfStunFrame())) throw invalid("Hit int23取消旗漂移");
    }
    private static SkillInfoObject effectData(CEventEffect e, int slot) {
        var found = e.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum()) && Integer.valueOf(1).equals(u.getBuffer())).toList();
        if (found.size() != 1 || found.get(0).getData() == null) throw invalid("Effect内层槽位漂移"); return found.get(0).getData();
    }
    private static void emitter(CEventEffect e, int start, int end, int innerEnd, int int1, int flags, int lifespan, int int6, int rate, int direction) {
        if (!frames(e, start, end) || !List.of(int1, 0, flags, 0, lifespan, int6, -1, 0, rate, 0, direction).equals(Arrays.asList(
                e.getInt1(), e.getInt2(), e.getInt3(), e.getInt4(), e.getInt5(), e.getInt6(), e.getInt7(), e.getInt8(), e.getInt9(), e.getInt10(), e.getInt11())))
            throw invalid("Effect外层发射合同漂移");
        for (var u : e.getCeventEffectUnitList()) if (!Integer.valueOf(1).equals(u.getBuffer()) || u.getData() == null || !frames(u.getData(), 0, innerEnd))
            throw invalid("Effect内层发射时间漂移");
        if (!(effectData(e, 3) instanceof CEventWazaSelect r) || r.getWazFileNo() < 0 || r.getWazSequenceNo() < 0) throw invalid("Effect资源引用类型漂移");
    }
    private static void requireSix(SkillInfoObject event, int... values) {
        if (!(event instanceof CEventValRandom v) || !Arrays.stream(values).boxed().toList().equals(Arrays.asList(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4(), v.getInt5(), v.getInt6())))
            throw invalid("六整数参数漂移");
    }
    private static CEventEffect movedEmitter(CEventEffect source, int frame, boolean single) {
        var e = copy(source, CEventEffect.class); window(e, frame, frame);
        if (single) for (var u : e.getCeventEffectUnitList()) window(u.getData(), 0, 0);
        return e;
    }
    private static void limitedRecovery(Waz.Skill s, int p, int frames, int nextEntry) {
        var pose = copy(sprites(s, p).get(0), CEventSprite.class); window(pose, 0, frames);
        s.getPhasesInfo().get(p).setSkillUnitCollection(new ArrayList<>());
        set(s, p, 0, List.of(pose)); physics(s, p); setArmor(s, p, 0, 0); counter(s, p); speedZero(s, p, 0);
        set(s, p, 71, List.of(nextEntry < 0 ? end(frames) : input(frames, nextEntry)));
    }
    private static <T> T copy(T value, Class<T> type) { try { return JSON.readValue(JSON.writeValueAsBytes(value), type); } catch (IOException e) { throw new IllegalStateException("追加武装DTO复制失败", e); } }
    private static JsonNode payload(Object value) { JsonNode n = JSON.valueToTree(value); clean(n); return n; }
    private static void clean(JsonNode n) { if (n instanceof ObjectNode o) o.remove(List.of("offset", "length", "slotNum", "unitDescription", "description")); n.forEach(MotokiAdditionalVariants::clean); }
    private static IllegalStateException invalid(String message) { return new IllegalStateException("Motoki新六武装输入不符: " + message); }

}
