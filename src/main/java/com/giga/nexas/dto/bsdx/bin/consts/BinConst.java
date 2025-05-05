package com.giga.nexas.dto.bsdx.bin.consts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/4/24
 * @Description opcode mapping by:@koukdw
 */
public class BinConst {

    public final static Map<Integer, String> OPCODE_MNEMONIC_MAP;
    public final static Map<String, Integer> MNEMONIC_OPCODE_MAP;

    public final static Map<Integer, String> OPERAND_MNEMONIC_MAP;
    public final static Map<String, Integer> MNEMONIC_OPERAND_MAP;

    static {
        Map<Integer, String> tmp = new HashMap<>(128);

        // 基本运算
        tmp.put(0,  "VAL");
        tmp.put(4,  "PUSH");
        tmp.put(5,  "PARAM");
        tmp.put(6,  "POP");
        tmp.put(7,  "CALL");
        tmp.put(8,  "LOAD");
        tmp.put(9,  "ADD");
        tmp.put(10, "SUB");
        tmp.put(11, "MUL");
        tmp.put(12, "DIV");
        tmp.put(13, "MOD");
        tmp.put(14, "STORE");

        // 逻辑 / 位运算
        tmp.put(15, "OR");
        tmp.put(16, "AND");
        tmp.put(17, "BIT_OR");
        tmp.put(18, "BIT_AND");
        tmp.put(19, "XOR");
        tmp.put(20, "NOT");

        // 比较
        tmp.put(21, "CMP_LE");
        tmp.put(22, "CMP_GE");
        tmp.put(23, "CMP_LT");
        tmp.put(24, "CMP_GT");
        tmp.put(25, "CMP_EQ");
        tmp.put(26, "CMP_NE");

        // 流程控制 / 标记
        tmp.put(27, "START");
        tmp.put(28, "END");
        tmp.put(29, "MARKER");

        // 自增自减
        tmp.put(34, "INC");
        tmp.put(35, "DEC");

        // 比零
        tmp.put(43, "CMP_ZERO");

        // 行号
        tmp.put(44, "LINENO");

        // 位移
        tmp.put(45, "SAR");
        tmp.put(46, "SHL");

        // 复合赋值
        tmp.put(47, "LD_ADD");
        tmp.put(48, "LD_SUB");
        tmp.put(49, "LD_MUL");
        tmp.put(50, "LD_DIV");
        tmp.put(51, "LD_MOD");
        tmp.put(52, "LD_OR");
        tmp.put(53, "LD_AND");
        tmp.put(54, "LD_XOR");
        tmp.put(55, "LD_SAR");
        tmp.put(56, "LD_SHL");

        // 其他
        tmp.put(61, "TO_STRING");
        tmp.put(62, "RETURN");
        tmp.put(63, "JMP");
        tmp.put(64, "JMP_IF_FALSE");
        tmp.put(65, "JMP_IF_TRUE");

        OPCODE_MNEMONIC_MAP = Collections.unmodifiableMap(tmp);

        // mnemonic → opcode
        Map<String, Integer> rev = new HashMap<>();
        for (Map.Entry<Integer, String> e : tmp.entrySet()) {
            rev.put(e.getValue(), e.getKey());
        }
        MNEMONIC_OPCODE_MAP = Collections.unmodifiableMap(rev);
    }

    static {
        Map<Integer, String> tmp = new HashMap<>();

        //  // Toodo: find where LoopVoice functions are located
        //  // also need to find whats 11->66 do
        tmp.put(0, "InitSystem");
        tmp.put(11, "Exit");
        //  // 12
        tmp.put(13, "Random");
        tmp.put(14, "GetCharCode");
        tmp.put(15, "PlayMovie");
        tmp.put(16, "Wait");
        tmp.put(17, "Loading");
        //  // Cursor and Menu functions might be wrong haven't tested but seems to be correct
        tmp.put(18, "ShowCursor");
        tmp.put(19, "HideCursor");
        tmp.put(20, "ShowMenu");
        tmp.put(21, "HideMenu");
        tmp.put(22, "MessageBox");
        tmp.put(49, "GetMouseMoveFlg");
        tmp.put(50, "GetMouseX");
        tmp.put(51, "GetMouseY");
        tmp.put(52, "DebugOut");
        tmp.put(62, "ShellExecute");
        tmp.put(65, "IsExistFile");
        tmp.put(67, "GetFontColorR");
        tmp.put(68, "GetFontColorG");
        tmp.put(69, "GetFontColorB");
        tmp.put(70, "SetObject");
        tmp.put(71, "SetAnimeObject");
        tmp.put(72, "SetSpriteObject");
        tmp.put(73, "SetFontObject");
        tmp.put(74, "SetCharcodeObject");
        tmp.put(75, "SetFillObject");
        tmp.put(76, "SetCopyObject");
        tmp.put(77, "SetCopyStandObject");
        tmp.put(78, "SetCopyScreenObject");
        tmp.put(79, "SetFaceObject");
        tmp.put(80, "DelObject");
        tmp.put(81, "MoveObject");
        tmp.put(82, "MoveSpeedObject");
        tmp.put(83, "MoveVectorObject");
        tmp.put(84, "ViewObject");
        tmp.put(85, "ZoomObject");
        tmp.put(86, "ZoomCycleObject");
        tmp.put(87, "RotateObject");
        tmp.put(88, "RotateSpeedObject");
        tmp.put(89, "RotateCycleObject");
        tmp.put(90, "TurnObject");
        tmp.put(91, "TurnSpeedObject");
        tmp.put(92, "TurnCycleObject");
        tmp.put(93, "ShakeObject");
        tmp.put(94, "RasterXObject");
        tmp.put(95, "RasterYObject");
        tmp.put(96, "WaveXObject");
        tmp.put(97, "WaveYObject");
        tmp.put(98, "NoiseXObject");
        tmp.put(99, "NoiseYObject");
        tmp.put(100, "PinbokeObject");
        tmp.put(101, "AfterimageObject");
        tmp.put(102, "FadeObject");
        tmp.put(103, "BlinkObject");
        tmp.put(104, "ClipObject");
        tmp.put(105, "ScrollLinkObject");
        tmp.put(106, "BGMLinkObject");
        tmp.put(107, "RotateLinkObject");
        tmp.put(108, "MirrorObject");
        tmp.put(109, "TileObject");
        tmp.put(110, "ButtonObject");
        tmp.put(111, "SetAttributeObject");
        tmp.put(112, "WaitObject");
        tmp.put(113, "WaitMoveObject");
        tmp.put(114, "WaitMoveSpeedObject");
        tmp.put(115, "WaitMoveVectorObject");
        tmp.put(116, "WaitViewObject");
        tmp.put(117, "WaitZoomObject");
        tmp.put(118, "WaitRotateObject");
        tmp.put(119, "WaitRotateSpeedObject");
        tmp.put(120, "WaitTurnObject");
        tmp.put(121, "WaitTurnSpeedObject");
        tmp.put(122, "WaitShakeObject");
        tmp.put(123, "WaitRasterObject");
        tmp.put(124, "WaitWaveObject");
        tmp.put(125, "WaitNoiseObject");
        tmp.put(126, "WaitPinbokeObject");
        tmp.put(127, "WaitAfterimageObject");
        tmp.put(128, "WaitFadeObject");
        tmp.put(129, "WaitClipObject");
        tmp.put(130, "StopObject");
        tmp.put(131, "SetObjectOrigin");
        tmp.put(132, "SetObjectZoomCenter");
        tmp.put(133, "SetObjectRotateCenter");
        tmp.put(134, "SetObjectAnimeNo");
        tmp.put(135, "GetObjectAnimeNo");
        tmp.put(136, "GetObjectX");
        tmp.put(137, "GetObjectY");
        tmp.put(138, "GetObjectActX");
        tmp.put(139, "GetObjectActY");
        tmp.put(140, "GetObjectOriginX");
        tmp.put(141, "GetObjectOriginY");
        tmp.put(142, "GetObjectWidth");
        tmp.put(143, "GetObjectHeight");
        tmp.put(144, "IsExistObject");
        tmp.put(145, "SearchEmptyObject");
        tmp.put(146, "SetObjectGroup");
        tmp.put(147, "DelObjectGroup");
        tmp.put(148, "IntervalObjectGroup");
        tmp.put(149, "ClipObjectGroup");
        tmp.put(150, "MoveObjectGroup");
        tmp.put(151, "MoveSpeedObjectGroup");
        tmp.put(152, "MoveVectorObjectGroup");
        tmp.put(153, "ZoomObjectGroup");
        tmp.put(154, "RotateObjectGroup");
        tmp.put(155, "RotateSpeedObjectGroup");
        tmp.put(156, "ShakeObjectGroup");
        tmp.put(157, "FadeObjectGroup");
        tmp.put(158, "RotateLinkObjectGroup");
        tmp.put(159, "IsExistObjectGroup"); // Weird no SearchEmptyObjectGroup after IsExistObjectGroup
        tmp.put(160, "SetCallObject");
        tmp.put(161, "DelCallObject");
        tmp.put(162, "IsExistCallObject");
        tmp.put(163, "SetBuffer");
        tmp.put(164, "SetAnimeBuffer");
        tmp.put(165, "SetVisualBuffer");
        tmp.put(166, "SetStandBuffer");
        tmp.put(167, "SetSEBuffer");
        tmp.put(168, "DelBuffer");
        tmp.put(169, "IsExistBuffer");
        tmp.put(170, "SetSelectMenu");
        tmp.put(171, "SelectMenu");
        tmp.put(174, "SetEventInfo");
        tmp.put(175, "DelEventInfo");
        tmp.put(177, "SetVisual");
        tmp.put(178, "DelVisual");
        tmp.put(179, "ZoomVisual");
        tmp.put(180, "PinbokeVisual");
        tmp.put(181, "FadeVisual");
        tmp.put(182, "FlashVisual");
        tmp.put(183, "WaitVisual");
        tmp.put(184, "WaitZoomVisual");
        tmp.put(185, "WaitPinbokeVisual");
        tmp.put(186, "WaitFadeVisual");
        tmp.put(187, "WaitFlashVisual");
        //  // 188
        tmp.put(189, "FadeIn");
        tmp.put(190, "FadeOut");
        tmp.put(191, "FlashIn");
        tmp.put(192, "FlashOut");
        tmp.put(193, "AddIn");
        tmp.put(194, "AddOut");
        tmp.put(195, "CrossFade");
        tmp.put(196, "ZoomFadeIn");
        tmp.put(197, "ZoomFadeOut");
        tmp.put(198, "ZoomFlashIn");
        tmp.put(199, "ZoomFlashOut");
        tmp.put(200, "ZoomAddIn");
        tmp.put(201, "ZoomAddOut");
        tmp.put(202, "ZoomCrossFade");
        tmp.put(203, "RotateFadeIn");
        tmp.put(204, "RotateFadeOut");
        tmp.put(205, "RotateFlashIn");
        tmp.put(206, "RotateFlashOut");
        tmp.put(207, "RotateAddIn");
        tmp.put(208, "RotateAddOut");
        tmp.put(209, "RotateCrossFade");
        tmp.put(210, "MosaicFadeIn");
        tmp.put(211, "MosaicFadeOut");
        tmp.put(212, "MosaicFlashIn");
        tmp.put(213, "MosaicFlashOut");
        tmp.put(214, "MosaicAddIn");
        tmp.put(215, "MosaicAddOut");
        tmp.put(216, "MosaicCrossFade");
        tmp.put(217, "ScrollFadeIn");
        tmp.put(218, "ScrollFadeOut");
        tmp.put(219, "ScrollFlashIn");
        tmp.put(220, "ScrollFlashOut");
        tmp.put(221, "ScrollAddIn");
        tmp.put(222, "ScrollAddOut");
        tmp.put(223, "ScrollCrossFade");
        tmp.put(224, "ScrollIn");
        tmp.put(225, "ScrollOut");
        tmp.put(226, "ScrollCross");
        tmp.put(227, "CurtainFadeIn");
        tmp.put(228, "CurtainFadeOut");
        tmp.put(229, "RuleFadeIn");
        tmp.put(230, "RuleFadeOut");
        tmp.put(231, "RuleFlashIn");
        tmp.put(232, "RuleFlashOut");
        tmp.put(233, "RuleCrossFade");
        tmp.put(234, "TurnFadeIn");
        tmp.put(235, "TurnFlashOut");
        tmp.put(236, "TurnFlashIn");
        tmp.put(237, "TurnFadeOut");
        tmp.put(238, "WaveFadeIn");
        tmp.put(239, "WaveFadeOut");
        tmp.put(240, "WaveFlashIn");
        tmp.put(241, "WaveFlashOut");
        tmp.put(242, "WaveCrossFade");
        tmp.put(243, "WaitFade");
        tmp.put(244, "Fade");
        tmp.put(245, "Flash");
        tmp.put(246, "FaderRGB");
        tmp.put(247, "FlashRGB");
        tmp.put(248, "Shake");
        tmp.put(249, "RasterX");
        tmp.put(250, "RasterY");
        tmp.put(251, "WaveX");
        tmp.put(252, "WaveY");
        tmp.put(253, "NoiseX");
        tmp.put(254, "NoiseY");
        tmp.put(255, "Pinboke");
        tmp.put(256, "View");
        tmp.put(257, "ViewCenter");
        tmp.put(258, "Zoom");
        tmp.put(259, "Rotate");
        tmp.put(260, "Turn");
        tmp.put(261, "Blur");
        tmp.put(262, "BlurScroll");
        tmp.put(263, "BlurZoom");
        tmp.put(264, "Scroll");
        tmp.put(265, "Mosaic");
        tmp.put(266, "SpeedLine");
        tmp.put(267, "RadialLine");
        tmp.put(268, "Clip");
        tmp.put(269, "BGColor");
        tmp.put(270, "WaitFilter");
        tmp.put(271, "WaitShake");
        tmp.put(272, "WaitRasterX");
        tmp.put(273, "WaitRasterY");
        tmp.put(274, "WaitWaveX");
        tmp.put(275, "WaitWaveY");
        tmp.put(276, "WaitNoiseX");
        tmp.put(277, "WaitNoiseY");
        tmp.put(278, "WaitPinboke");
        tmp.put(279, "WaitView");
        tmp.put(280, "WaitViewCenter");
        tmp.put(281, "WaitZoom");
        tmp.put(282, "WaitRotate");
        tmp.put(283, "WaitTurn");
        tmp.put(284, "WaitBlur");
        tmp.put(285, "WaitScroll");
        tmp.put(286, "WaitMosaic");
        tmp.put(287, "WaitSpeedLine");
        tmp.put(288, "WaitRadialLine");
        tmp.put(289, "WaitClip");
        tmp.put(290, "WaitBGColor");
        tmp.put(291, "SetStand");
        tmp.put(292, "SetStandPos");
        tmp.put(293, "DelStand");
        tmp.put(294, "ChangeStand");
        tmp.put(295, "SetStandEx");
        tmp.put(296, "SetStandPosEx");
        tmp.put(297, "DelStandEx");
        tmp.put(298, "MoveStand");
        tmp.put(299, "ViewStand");
        tmp.put(300, "ZoomStand");
        tmp.put(301, "ZoomCycleStand");
        tmp.put(302, "RotateStand");
        tmp.put(303, "RotateSpeedStand");
        tmp.put(304, "RotateCycleStand");
        tmp.put(305, "TurnStand");
        tmp.put(306, "TurnSpeedStand");
        tmp.put(307, "TurnCycleStand");
        tmp.put(308, "ShakeStand");
        tmp.put(309, "RasterXStand");
        tmp.put(310, "RasterYStand");
        tmp.put(311, "WaveXStand");
        tmp.put(312, "WaveYStand");
        tmp.put(313, "NoiseXStand");
        tmp.put(314, "NoiseYStand");
        tmp.put(315, "PinbokeStand");
        tmp.put(316, "AfterimageStand");
        tmp.put(317, "FadeStand");
        tmp.put(318, "BlinkStand");
        tmp.put(319, "WaitStand");
        tmp.put(320, "WaitMoveStand");
        tmp.put(321, "WaitZoomStand");
        tmp.put(322, "WaitViewStand");
        tmp.put(323, "WaitRotateStand");
        tmp.put(324, "WaitRotateSpeedStand");
        tmp.put(325, "WaitTurnStand");
        tmp.put(326, "WaitTurnSpeedStand");
        tmp.put(327, "WaitShakeStand");
        tmp.put(328, "WaitRasterStand");
        tmp.put(329, "WaitWaveStand");
        tmp.put(330, "WaitNoiseStand");
        tmp.put(331, "WaitPinbokeStand");
        tmp.put(332, "WaitAfterimageStand");
        tmp.put(333, "WaitFadeStand");
        //  // Those GetStand function are hard to figure out they don't fit and i'm not sure which one to comment
        tmp.put(334, "GetStandNo");
        //  // 335: GetStandCharNo
        tmp.put(335, "GetStandX");
        tmp.put(336, "GetStandY");
        tmp.put(337, "GetStandActX");
        tmp.put(338, "GetStandActY");
        tmp.put(339, "GetStandPosX");
        tmp.put(340, "GetStandPosY");
        tmp.put(341, "GetStandMoveX");
        tmp.put(342, "GetStandMoveY");
        //  // 343: GetStandFaceX
        //  // 344: GetStandFaceY
        tmp.put(343, "GetStandWidth");
        tmp.put(344, "GetStandHeight");
        tmp.put(345, "IsExistStand");
        tmp.put(346, "SetWindow");
        tmp.put(347, "DelWindow");
        tmp.put(348, "SetMessage");
        tmp.put(349, "AddMessage");
        tmp.put(350, "DelMessage");
        tmp.put(351, "WaitMessage");
        tmp.put(352, "MessagePos");
        tmp.put(353, "SetFlg");
        tmp.put(354, "GetFlg");
        tmp.put(355, "SetSystemFlg");
        tmp.put(356, "GetSystemFlg");
        tmp.put(357, "SetCGFlg");
        tmp.put(358, "GetCGFlg");
        tmp.put(359, "SetBGMFlg");
        tmp.put(360, "GetBGMFlg");
        tmp.put(361, "SetReplayFlg");
        tmp.put(362, "GetReplayFlg");
        tmp.put(363, "SetEventFlg");
        tmp.put(364, "GetEventFlg");
        tmp.put(365, "LoadScript");
        tmp.put(366, "LoadEvent");
        tmp.put(367, "ChangeBank");
        tmp.put(368, "CallScript");
        tmp.put(369, "CallBank");
        tmp.put(370, "Return");
        tmp.put(371, "VoicePlay");
        tmp.put(372, "VoiceStop");
        tmp.put(373, "SetVoiceVolume"); // Not sure about that one but it's the only one that fit
        tmp.put(374, "WaitVoice");
        tmp.put(375, "SEPlay");
        tmp.put(376, "SELoopPlay");
        tmp.put(377, "SEFadePlay");
        tmp.put(378, "SEFadeOut");
        tmp.put(379, "SEFade");
        tmp.put(380, "SEPan");
        tmp.put(381, "SESpeed");
        tmp.put(382, "SEStop");
        tmp.put(383, "WaitSE");
        tmp.put(384, "IsExistSE");
        //  // Toodo, verify the BGM functions (they are kinda hard to guess it's quite different from aonatsu line)
        //  // i kinda have the feeling BGM and LoopVoice are intertwined because there is no CLoopVoice class
        //  // Maybe they use the CBgm class for LoopVoice because it's close enough in functionality
        tmp.put(385, "BGMPlay");
        tmp.put(386, "BGMPlay2");
        tmp.put(387, "BGMFadePlay");
        tmp.put(388, "BGMFadePlay2");
        tmp.put(389, "BGMFadeOut");
        tmp.put(390, "BGMFadeOut2");
        tmp.put(391, "BGMFade");
        tmp.put(392, "BGMFade2");
        tmp.put(393, "BGMSyncCrossFade");
        tmp.put(394, "BGMSyncCrossFade2");
        tmp.put(395, "BGMLoop");
        tmp.put(396, "BGMLoop2");
        tmp.put(397, "BGMPan");
        tmp.put(398, "BGMPan2");
        tmp.put(399, "BGMSpeed");
        tmp.put(400, "BGMSpeed2");
        tmp.put(401, "BGMStop");
        tmp.put(402, "BGMStop2");
        tmp.put(403, "BGMRestart");
        tmp.put(404, "BGMRestart2");
        tmp.put(405, "WaitBGM");
        tmp.put(406, "GetBGMNo");

        OPERAND_MNEMONIC_MAP = Collections.unmodifiableMap(tmp);

        // mnemonic → operand
        Map<String, Integer> revN = new HashMap<>(tmp.size());
        for (Map.Entry<Integer, String> e : tmp.entrySet()) {
            revN.put(e.getValue(), e.getKey());
        }
        MNEMONIC_OPERAND_MAP = Collections.unmodifiableMap(revN);
    }

    private BinConst() {}

}
