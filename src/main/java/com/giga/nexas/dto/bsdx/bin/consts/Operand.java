package com.giga.nexas.dto.bsdx.bin.consts;

/**
 * 定义了游戏脚本系统中的操作数函数（Operand），
 * 每个操作数编号对应一个具体的引擎内部函数调用，
 * 另，还有许多游戏内针对游戏行为的函数，均待逆向测试
 */
@OperandDocSource(classpath = "/research/bin立即数.csv")
public enum Operand {
    InitSystem(0),
    Exit(11),
    // CSV: 生成随机整数
    Random(13),
    GetCharCode(14),
    PlayMovie(15),
    Wait(16),
    Loading(17),
    ShowCursor(18),
    HideCursor(19),
    ShowMenu(20),
    HideMenu(21),
    MessageBox(22),
    GetMouseMoveFlg(49),
    GetMouseX(50),
    GetMouseY(51),
    DebugOut(52),
    ShellExecute(62),
    IsExistFile(65),
    GetFontColorR(67),
    GetFontColorG(68),
    GetFontColorB(69),
    SetObject(70),
    SetAnimeObject(71),
    SetSpriteObject(72),
    SetFontObject(73),
    SetCharcodeObject(74),
    SetFillObject(75),
    SetCopyObject(76),
    SetCopyStandObject(77),
    SetCopyScreenObject(78),
    SetFaceObject(79),
    DelObject(80),
    MoveObject(81),
    MoveSpeedObject(82),
    MoveVectorObject(83),
    ViewObject(84),
    ZoomObject(85),
    ZoomCycleObject(86),
    RotateObject(87),
    RotateSpeedObject(88),
    RotateCycleObject(89),
    TurnObject(90),
    TurnSpeedObject(91),
    TurnCycleObject(92),
    ShakeObject(93),
    RasterXObject(94),
    RasterYObject(95),
    WaveXObject(96),
    WaveYObject(97),
    NoiseXObject(98),
    NoiseYObject(99),
    PinbokeObject(100),
    AfterimageObject(101),
    FadeObject(102),
    BlinkObject(103),
    ClipObject(104),
    ScrollLinkObject(105),
    BGMLinkObject(106),
    RotateLinkObject(107),
    MirrorObject(108),
    TileObject(109),
    ButtonObject(110),
    SetAttributeObject(111),
    WaitObject(112),
    WaitMoveObject(113),
    WaitMoveSpeedObject(114),
    WaitMoveVectorObject(115),
    WaitViewObject(116),
    WaitZoomObject(117),
    WaitRotateObject(118),
    WaitRotateSpeedObject(119),
    WaitTurnObject(120),
    WaitTurnSpeedObject(121),
    WaitShakeObject(122),
    WaitRasterObject(123),
    WaitWaveObject(124),
    WaitNoiseObject(125),
    WaitPinbokeObject(126),
    WaitAfterimageObject(127),
    WaitFadeObject(128),
    WaitClipObject(129),
    StopObject(130),
    SetObjectOrigin(131),
    SetObjectZoomCenter(132),
    SetObjectRotateCenter(133),
    SetObjectAnimeNo(134),
    GetObjectAnimeNo(135),
    GetObjectX(136),
    GetObjectY(137),
    GetObjectActX(138),
    GetObjectActY(139),
    GetObjectOriginX(140),
    GetObjectOriginY(141),
    GetObjectWidth(142),
    GetObjectHeight(143),
    IsExistObject(144),
    SearchEmptyObject(145),
    SetObjectGroup(146),
    DelObjectGroup(147),
    IntervalObjectGroup(148),
    ClipObjectGroup(149),
    MoveObjectGroup(150),
    MoveSpeedObjectGroup(151),
    MoveVectorObjectGroup(152),
    ZoomObjectGroup(153),
    RotateObjectGroup(154),
    RotateSpeedObjectGroup(155),
    ShakeObjectGroup(156),
    FadeObjectGroup(157),
    RotateLinkObjectGroup(158),
    IsExistObjectGroup(159),
    SetCallObject(160),
    DelCallObject(161),
    IsExistCallObject(162),
    SetBuffer(163),
    SetAnimeBuffer(164),
    SetVisualBuffer(165),
    SetStandBuffer(166),
    SetSEBuffer(167),
    DelBuffer(168),
    IsExistBuffer(169),
    SetSelectMenu(170),
    SelectMenu(171),
    SetEventInfo(174),
    DelEventInfo(175),
    SetVisual(177),
    DelVisual(178),
    ZoomVisual(179),
    PinbokeVisual(180),
    FadeVisual(181),
    FlashVisual(182),
    WaitVisual(183),
    WaitZoomVisual(184),
    WaitPinbokeVisual(185),
    WaitFadeVisual(186),
    WaitFlashVisual(187),
    // CSV: 地狱关卡文件不执行的话就显示不出地图
    FadeIn(189),
    FadeOut(190),
    FlashIn(191),
    FlashOut(192),
    AddIn(193),
    AddOut(194),
    CrossFade(195),
    ZoomFadeIn(196),
    ZoomFadeOut(197),
    ZoomFlashIn(198),
    ZoomFlashOut(199),
    ZoomAddIn(200),
    ZoomAddOut(201),
    ZoomCrossFade(202),
    RotateFadeIn(203),
    RotateFadeOut(204),
    RotateFlashIn(205),
    RotateFlashOut(206),
    RotateAddIn(207),
    RotateAddOut(208),
    RotateCrossFade(209),
    MosaicFadeIn(210),
    MosaicFadeOut(211),
    MosaicFlashIn(212),
    MosaicFlashOut(213),
    MosaicAddIn(214),
    MosaicAddOut(215),
    MosaicCrossFade(216),
    ScrollFadeIn(217),
    ScrollFadeOut(218),
    ScrollFlashIn(219),
    ScrollFlashOut(220),
    ScrollAddIn(221),
    ScrollAddOut(222),
    ScrollCrossFade(223),
    ScrollIn(224),
    ScrollOut(225),
    ScrollCross(226),
    CurtainFadeIn(227),
    CurtainFadeOut(228),
    RuleFadeIn(229),
    RuleFadeOut(230),
    RuleFlashIn(231),
    RuleFlashOut(232),
    RuleCrossFade(233),
    TurnFadeIn(234),
    TurnFlashOut(235),
    TurnFlashIn(236),
    TurnFadeOut(237),
    WaveFadeIn(238),
    WaveFadeOut(239),
    WaveFlashIn(240),
    WaveFlashOut(241),
    WaveCrossFade(242),
    WaitFade(243),
    Fade(244),
    Flash(245),
    FaderRGB(246),
    FlashRGB(247),
    Shake(248),
    RasterX(249),
    RasterY(250),
    WaveX(251),
    WaveY(252),
    NoiseX(253),
    NoiseY(254),
    Pinboke(255),
    View(256),
    ViewCenter(257),
    Zoom(258),
    Rotate(259),
    Turn(260),
    Blur(261),
    BlurScroll(262),
    BlurZoom(263),
    Scroll(264),
    Mosaic(265),
    SpeedLine(266),
    RadialLine(267),
    Clip(268),
    BGColor(269),
    WaitFilter(270),
    WaitShake(271),
    WaitRasterX(272),
    WaitRasterY(273),
    WaitWaveX(274),
    WaitWaveY(275),
    WaitNoiseX(276),
    WaitNoiseY(277),
    WaitPinboke(278),
    WaitView(279),
    WaitViewCenter(280),
    WaitZoom(281),
    WaitRotate(282),
    WaitTurn(283),
    WaitBlur(284),
    WaitScroll(285),
    WaitMosaic(286),
    WaitSpeedLine(287),
    WaitRadialLine(288),
    WaitClip(289),
    WaitBGColor(290),
    SetStand(291),
    SetStandPos(292),
    DelStand(293),
    ChangeStand(294),
    SetStandEx(295),
    SetStandPosEx(296),
    DelStandEx(297),
    MoveStand(298),
    ViewStand(299),
    ZoomStand(300),
    ZoomCycleStand(301),
    RotateStand(302),
    RotateSpeedStand(303),
    RotateCycleStand(304),
    TurnStand(305),
    TurnSpeedStand(306),
    TurnCycleStand(307),
    ShakeStand(308),
    RasterXStand(309),
    RasterYStand(310),
    WaveXStand(311),
    WaveYStand(312),
    NoiseXStand(313),
    NoiseYStand(314),
    PinbokeStand(315),
    AfterimageStand(316),
    FadeStand(317),
    BlinkStand(318),
    WaitStand(319),
    WaitMoveStand(320),
    WaitZoomStand(321),
    WaitViewStand(322),
    WaitRotateStand(323),
    WaitRotateSpeedStand(324),
    WaitTurnStand(325),
    WaitTurnSpeedStand(326),
    WaitShakeStand(327),
    WaitRasterStand(328),
    WaitWaveStand(329),
    WaitNoiseStand(330),
    WaitPinbokeStand(331),
    WaitAfterimageStand(332),
    WaitFadeStand(333),
    GetStandNo(334),
    GetStandX(335),
    GetStandY(336),
    GetStandActX(337),
    GetStandActY(338),
    GetStandPosX(339),
    GetStandPosY(340),
    GetStandMoveX(341),
    GetStandMoveY(342),
    GetStandWidth(343),
    GetStandHeight(344),
    IsExistStand(345),
    SetWindow(346),
    DelWindow(347),
    SetMessage(348),
    AddMessage(349),
    DelMessage(350),
    WaitMessage(351),
    MessagePos(352),
    SetFlg(353),
    GetFlg(354),
    SetSystemFlg(355),
    GetSystemFlg(356),
    SetCGFlg(357),
    GetCGFlg(358),
    SetBGMFlg(359),
    GetBGMFlg(360),
    SetReplayFlg(361),
    GetReplayFlg(362),
    SetEventFlg(363),
    GetEventFlg(364),
    LoadScript(365),
    LoadEvent(366),
    ChangeBank(367),
    CallScript(368),
    CallBank(369),
    Return(370),
    VoicePlay(371),
    VoiceStop(372),
    SetVoiceVolume(373),
    WaitVoice(374),
    // CSV: 播放SE
    SEPlay(375),
    SELoopPlay(376),
    SEFadePlay(377),
    SEFadeOut(378),
    SEFade(379),
    SEPan(380),
    SESpeed(381),
    SEStop(382),
    WaitSE(383),
    IsExistSE(384),
    BGMPlay(385),
    BGMPlay2(386),
    BGMFadePlay(387),
    BGMFadePlay2(388),
    BGMFadeOut(389),
    BGMFadeOut2(390),
    BGMFade(391),
    BGMFade2(392),
    BGMSyncCrossFade(393),
    BGMSyncCrossFade2(394),
    BGMLoop(395),
    BGMLoop2(396),
    BGMPan(397),
    BGMPan2(398),
    BGMSpeed(399),
    BGMSpeed2(400),
    BGMStop(401),
    BGMStop2(402),
    BGMRestart(403),
    BGMRestart2(404),
    WaitBGM(405),
    GetBGMNo(406),
    // Generated Tier 2 opcode mapping for case 0X19A (410)
    /**
     * Syscall: 设置机体脚本调用对象（绑定脚本至指定机体槽位）
     */
    SetMekaCallObject(410),
    // Generated Tier 2 opcode mapping for case 0X19B (411)
    /**
     * Syscall: 设置机体脚本调用对象扩展版（绑定并初始化附加参数）
     */
    SetMekaCallObjectEx(411),
    // Generated Tier 2 opcode mapping for case 0X19C (412)
    /**
     * Syscall: 删除机体脚本调用对象
     */
    DelMekaCallObject(412),
    // Generated Tier 2 opcode mapping for case 0X19D (413)
    /**
     * Syscall: 设置机体脚本调用对象的偏移参数A
     */
    SetMekaCallObjectParamA(413),
    // Generated Tier 2 opcode mapping for case 0X19E (414)
    /**
     * Syscall: 设置机体脚本调用对象的偏移参数B
     */
    SetMekaCallObjectParamB(414),
    // Generated Tier 2 opcode mapping for case 0X19F (415)
    /**
     * Syscall: 设置机体脚本调用对象的偏移参数C
     */
    SetMekaCallObjectParamC(415),
    // Generated Tier 2 opcode mapping for case 0X1A0 (416)
    /**
     * Syscall: 获取机体脚本调用对象的参数A值
     */
    GetMekaCallObjectParamA(416),
    // Generated Tier 2 opcode mapping for case 0X1A1 (417)
    /**
     * Syscall: 获取机体脚本调用对象的参数C值
     */
    GetMekaCallObjectParamC(417),
    // Generated Tier 2 opcode mapping for case 0X1A2 (418)
    /**
     * Syscall: 检查机体脚本调用对象是否存在于指定槽位
     */
    IsExistMekaCallObject(418),
    // Generated Tier 2 opcode mapping for case 0X1A3 (419)
    /**
     * Syscall: 初始化关卡舞台（重置全局标志与转场参数）
     */
    PrepareStage(419),
    // Generated Tier 2 opcode mapping for case 0X1A4 (420)
    /**
     * Syscall: 初始化并加载关卡UI横幅提示
     */
    InitStageUI(420),
    // Generated Tier 2 opcode mapping for case 0X1A5 (421)
    /**
     * Syscall: 启动关卡计时并记录当前关卡的进入时间
     */
    StartStage(421),
    // Generated Tier 2 opcode mapping for case 0X1A6 (422)
    /**
     * Syscall: 未使用的占位符操作数422
     */
    Unused_422(422),
    // Generated Tier 2 opcode mapping for case 0X1A7 (423)
    /**
     * Syscall: 未使用的占位符操作数423
     */
    Unused_423(423),
    // Generated Tier 2 opcode mapping for case 0X1A8 (424)
    /**
     * Syscall: 未使用的占位符操作数424
     */
    Unused_424(424),
    // Generated Tier 2 opcode mapping for case 0X1A9 (425)
    /**
     * Syscall: 未使用的占位符操作数425
     */
    Unused_425(425),
    // Generated Tier 2 opcode mapping for case 0X1AA (426)
    /**
     * Syscall: 设置关卡场景激活状态（控制战斗流程暂停与继续）
     */
    SetStageActiveState(426),
    // Generated Tier 2 opcode mapping for case 0X1AB (427)
    /**
     * Syscall: 未使用的占位符操作数427
     */
    Unused_427(427),
    // Generated Tier 2 opcode mapping for case 0X1AF (431)
    /**
     * Syscall: 重置所有战斗关卡进度
     */
    ResetAllStageProgress(431),
    // Generated Tier 2 opcode mapping for case 0X1B1 (433)
    /**
     * Syscall: 显示任务/战斗开始通知横幅
     */
    ShowQuestStartBanner(433),
    // Generated Tier 2 opcode mapping for case 0X1B2 (434)
    /**
     * Syscall: 显示任务/战斗结束通知横幅
     */
    ShowQuestEndBanner(434),
    // Generated Tier 2 opcode mapping for case 0X1B3 (435)
    /**
     * Syscall: 显示关卡挑战成功（Clear）通知横幅
     */
    ShowQuestClearBanner(435),
    // Generated Tier 2 opcode mapping for case 0X1B4 (436)
    /**
     * Syscall: 显示关卡挑战失败（Failed）通知横幅
     */
    ShowQuestFailedBanner(436),
    // Generated Tier 2 opcode mapping for case 0X1B5 (437)
    /**
     * Syscall: 销毁并清理关卡提示横幅资源
     */
    ClearQuestBanners(437),
    // Generated Tier 2 opcode mapping for case 0X1B6 (438)
    /**
     * Syscall: 显示关卡/剧本Logo
     */
    ShowStageLogo(438),
    // Generated Tier 2 opcode mapping for case 0X1B7 (439)
    /**
     * Syscall: 隐藏关卡/剧本Logo
     */
    HideStageLogo(439),
    // Generated Tier 2 opcode mapping for case 0X1B8 (440)
    /**
     * Syscall: 设置关卡Logo的显示坐标与大小
     */
    SetStageLogoPos(440),
    // Generated Tier 2 opcode mapping for case 0X1B9 (441)
    /**
     * Syscall: 销毁关卡演出插槽A的对象
     */
    DeleteQuestCinematicSlotA(441),
    // Generated Tier 2 opcode mapping for case 0X1BA (442)
    /**
     * Syscall: 检查关卡演出插槽A是否活跃
     */
    IsQuestCinematicSlotAActive(442),
    // Generated Tier 2 opcode mapping for case 0X1BB (443)
    /**
     * Syscall: 创建关卡演出插槽B的对象
     */
    CreateQuestCinematicSlotB(443),
    // Generated Tier 2 opcode mapping for case 0X1BC (444)
    /**
     * Syscall: 销毁关卡演出插槽B的对象
     */
    DeleteQuestCinematicSlotB(444),
    // Generated Tier 2 opcode mapping for case 0X1BD (445)
    /**
     * Syscall: 创建关卡演出插槽A的对象
     */
    CreateQuestCinematicSlotA(445),
    // Generated Tier 2 opcode mapping for case 0X1BE (446)
    /**
     * Syscall: 销毁关卡演出插槽C的对象
     */
    DeleteQuestCinematicSlotC(446),
    // Generated Tier 2 opcode mapping for case 0X1BF (447)
    /**
     * Syscall: 记录关卡已通关状态并保存通关时刻的系统时间
     */
    RecordQuestCleared(447),
    // Generated Tier 2 opcode mapping for case 0X1C0 (448)
    /**
     * Syscall: 触发战斗转场场景（战斗开始切场）
     */
    TriggerCombatTransition(448),
    // Generated Tier 2 opcode mapping for case 0X1C1 (449)
    /**
     * Syscall: 触发向冒险/终端界面转移的转场动作A
     */
    TriggerAdventureTransitionA(449),
    // Generated Tier 2 opcode mapping for case 0X1C3 (451)
    /**
     * Syscall: 触发向冒险/终端界面转移的转场动作B
     */
    TriggerAdventureTransitionB(451),
    // Generated Tier 2 opcode mapping for case 0X1C4 (452)
    /**
     * Syscall: 在战斗转场中打开整备/装备选项单
     */
    OpenSetupMenu(452),
    // Generated Tier 2 opcode mapping for case 0X1C5 (453)
    /**
     * Syscall: 设置战斗结算界面模式（成功关卡结算）
     */
    SetCombatResultMode(453),
    // Generated Tier 2 opcode mapping for case 0X1C6 (454)
    /**
     * Syscall: 触发向冒险/终端界面转移的转场动作C
     */
    TriggerAdventureTransitionC(454),
    // Generated Tier 2 opcode mapping for case 0X1C7 (455)
    /**
     * Syscall: 在整备菜单中打开插件装备详情界面
     */
    OpenPluginMenu(455),
    // Generated Tier 2 opcode mapping for case 0X1C8 (456)
    /**
     * Syscall: 设置战斗失败/游戏结束结算模式（Game Over）
     */
    SetCombatFailureMode(456),
    // Generated Tier 2 opcode mapping for case 0X1C9 (457)
    /**
     * Syscall: 触发向冒险/终端界面转移的转场动作D
     */
    TriggerAdventureTransitionD(457),
    // Generated Tier 2 opcode mapping for case 0X1CA (458)
    /**
     * Syscall: 为关卡配置指定文本参数
     */
    SetStageParamString(458),
    // Generated Tier 2 opcode mapping for case 0X1CC (460)
    /**
     * Syscall: 在战斗场景中呼出关卡详情菜单
     */
    OpenStageMenu(460),
    // Generated Tier 2 opcode mapping for case 0X1CD (461)
    /**
     * Syscall: 设置关卡附加控制参数A
     */
    SetStageParamA(461),
    // Generated Tier 2 opcode mapping for case 0X1CE (462)
    /**
     * Syscall: 未使用的占位符操作数462
     */
    Unused_462(462),
    // Generated Tier 2 opcode mapping for case 0X1CF (463)
    /**
     * Syscall: 未使用的占位符操作数463
     */
    Unused_463(463),
    // Generated Tier 2 opcode mapping for case 0X1D0 (464)
    /**
     * Syscall: 设置关卡附加控制参数B
     */
    SetStageParamB(464),
    // Generated Tier 2 opcode mapping for case 0X1D1 (465)
    /**
     * Syscall: 设置关卡附加控制参数C
     */
    SetStageParamC(465),
    // Generated Tier 2 opcode mapping for case 0X1D2 (466)
    /**
     * Syscall: 未使用的占位符操作数466
     */
    Unused_466(466),
    // Generated Tier 2 opcode mapping for case 0X1D3 (467)
    /**
     * Syscall: 未使用的占位符操作数467
     */
    Unused_467(467),
    // Generated Tier 2 opcode mapping for case 0X1D4 (468)
    /**
     * Syscall: 未使用的占位符操作数468
     */
    Unused_468(468),
    // Generated Tier 2 opcode mapping for case 0X1D5 (469)
    /**
     * Syscall: 未使用的占位符操作数469
     */
    Unused_469(469),
    // Generated Tier 2 opcode mapping for case 0X1D6 (470)
    /**
     * Syscall: 设置关卡附加控制参数D
     */
    SetStageParamD(470),
    // 自己写的
    // CSV: 设置倒计时
    StartCountdown(471),
    // Generated Tier 2 opcode mapping for case 0X1D8 (472)
    /**
     * Syscall: 设置战斗区域风向向量
     */
    SetBattleWindDirection(472),
    // Generated Tier 2 opcode mapping for case 0X1D9 (473)
    /**
     * Syscall: 设置战斗区域风力强度
     */
    SetBattleWindForce(473),
    // Generated Tier 2 opcode mapping for case 0X1DA (474)
    /**
     * Syscall: 设置战斗区域重力加速度
     */
    SetBattleGravity(474),
    // Generated Tier 2 opcode mapping for case 0X1DB (475)
    /**
     * Syscall: 设置战斗场景雾效颜色
     */
    SetBattleFogColor(475),
    // Generated Tier 2 opcode mapping for case 0X1DC (476)
    /**
     * Syscall: 设置战斗场景雾效浓度
     */
    SetBattleFogDensity(476),
    // Generated Tier 2 opcode mapping for case 0X1DD (477)
    /**
     * Syscall: 设置战斗场景雾效可视范围
     */
    SetBattleFogRange(477),
    // Generated Tier 2 opcode mapping for case 0X1DE (478)
    /**
     * Syscall: 启用/禁用战斗场景雾效
     */
    EnableBattleFog(478),
    // Generated Tier 2 opcode mapping for case 0X1DF (479)
    /**
     * Syscall: 更改战斗地图天空盒贴图
     */
    SetBattleSkybox(479),
    // Generated Tier 2 opcode mapping for case 0X1E0 (480)
    /**
     * Syscall: 设置战斗场景主光源颜色
     */
    SetBattleLightColor(480),
    // Generated Tier 2 opcode mapping for case 0X1E1 (481)
    /**
     * Syscall: 设置战斗场景主光源方向
     */
    SetBattleLightDir(481),
    // Generated Tier 2 opcode mapping for case 0X1E2 (482)
    /**
     * Syscall: 设置战斗场景主光源强度
     */
    SetBattleLightIntensity(482),
    // CSV: ES/剧情进入战斗
    EnterBattleEsStory(483),
    // Generated Tier 2 opcode mapping for case 0X1E4 (484)
    /**
     * Syscall: 设置战斗环境光色彩
     */
    SetBattleAmbientColor(484),
    // Generated Tier 2 opcode mapping for case 0X1E5 (485)
    /**
     * Syscall: 设置战斗环境光强度
     */
    SetBattleAmbientIntensity(485),
    // CSV: 显示OPEN COMBAT
    ShowOpenCombat(486),
    // Generated Tier 2 opcode mapping for case 0X1E7 (487)
    /**
     * Syscall: 启用/禁用战斗阴影渲染
     */
    EnableBattleShadows(487),
    // Generated Tier 2 opcode mapping for case 0X1E8 (488)
    /**
     * Syscall: 设置战斗阴影浓度
     */
    SetBattleShadowDensity(488),
    // Generated Tier 2 opcode mapping for case 0X1E9 (489)
    /**
     * Syscall: 设置战斗地图水面高度
     */
    SetBattleWaterLevel(489),
    // CSV: 返回难度，0 VE, 1 E, 2 N, 3 H, 4 VH
    GetDifficulty(490),
    // Generated Tier 2 opcode mapping for case 0X1EB (491)
    /**
     * Syscall: 应用全屏视觉特技效果A
     */
    SetScreenEffectA(491),
    // Generated Tier 2 opcode mapping for case 0X1EC (492)
    /**
     * Syscall: 应用全屏视觉特技效果B
     */
    SetScreenEffectB(492),
    // Generated Tier 2 opcode mapping for case 0X1ED (493)
    /**
     * Syscall: 设置屏幕褪色/黑屏过渡颜色
     */
    SetScreenFadeColor(493),
    // Generated Tier 2 opcode mapping for case 0X1EE (494)
    /**
     * Syscall: 设置屏幕褪色/黑屏过渡持续时间
     */
    SetScreenFadeTime(494),
    // Generated Tier 2 opcode mapping for case 0X1EF (495)
    /**
     * Syscall: 开始执行全屏褪色/闪烁动画
     */
    StartScreenFade(495),
    // Generated Tier 2 opcode mapping for case 0X1F0 (496)
    /**
     * Syscall: 停止全屏褪色过渡
     */
    StopScreenFade(496),
    // Generated Tier 2 opcode mapping for case 0X1F1 (497)
    /**
     * Syscall: 设置全屏振动摇晃力度
     */
    SetScreenShakeForce(497),
    // CSV: 地狱21D、21E加载机体后面跟着
    HellLoadMekFollow21D21E(498),
    // Generated Tier 2 opcode mapping for case 0X1F3 (499)
    /**
     * Syscall: 设置全屏振动摇晃持续帧数
     */
    SetScreenShakeTime(499),
    // CSV: 地狱21D加载机体后面跟着
    HellLoadMekFollow21D(500),
    // Generated Tier 2 opcode mapping for case 0X1F5 (501)
    /**
     * Syscall: 开始全屏摇晃震动特效
     */
    StartScreenShake(501),
    // Generated Tier 2 opcode mapping for case 0X1F6 (502)
    /**
     * Syscall: 停止全屏摇晃震动特效
     */
    StopScreenShake(502),
    // Generated Tier 2 opcode mapping for case 0X1F7 (503)
    /**
     * Syscall: 设置战斗场景画面渲染质量等级
     */
    SetRenderQuality(503),
    // Generated Tier 2 opcode mapping for case 0X1F8 (504)
    /**
     * Syscall: 启用/禁用画面高光溢出（Bloom）滤镜
     */
    SetRenderBloom(504),
    // Generated Tier 2 opcode mapping for case 0X1F9 (505)
    /**
     * Syscall: 启用/禁用画面运动模糊效果
     */
    SetRenderMotionBlur(505),
    // CSV: 加载地图
    LoadMap(506),
    // Generated Tier 2 opcode mapping for case 0X1FB (507)
    /**
     * Syscall: 设置画面景深参数
     */
    SetRenderDepthOfField(507),
    // Generated Tier 2 opcode mapping for case 0X1FC (508)
    /**
     * Syscall: 应用画面色彩风格滤镜
     */
    SetRenderColorFilter(508),
    // Generated Tier 2 opcode mapping for case 0X1FD (509)
    /**
     * Syscall: 设置画面对比度
     */
    SetRenderContrast(509),
    // Generated Tier 2 opcode mapping for case 0X1FE (510)
    /**
     * Syscall: 设置画面亮度校正值
     */
    SetRenderBrightness(510),
    // Generated Tier 2 opcode mapping for case 0X1FF (511)
    /**
     * Syscall: 设置画面伽马校正
     */
    SetRenderGamma(511),
    // Generated Tier 2 opcode mapping for case 0X200 (512)
    /**
     * Syscall: 启用/禁用画面暗角特效
     */
    SetRenderVignette(512),
    // Generated Tier 2 opcode mapping for case 0X201 (513)
    /**
     * Syscall: 设置场景摄像机观察跟随模式
     */
    SetCameraMode(513),
    // Generated Tier 2 opcode mapping for case 0X202 (514)
    /**
     * Syscall: 设置摄像机强制锁定的机体目标
     */
    SetCameraTargetMek(514),
    // CSV: waz/mapobj登场
    InitDeployTama(515),
    // CSV: 激活地图自带的waz/mapobj的场地编号，使其变得可被207之类的函数调整
    ActivateMapObjSlot(516),
    // Generated Tier 2 opcode mapping for case 0X205 (517)
    /**
     * Syscall: 设置摄像机平滑插值移动速度
     */
    SetCameraInterpolation(517),
    // Generated Tier 2 opcode mapping for case 0X206 (518)
    /**
     * Syscall: 重置摄像机至默认观察角度和高度
     */
    ResetCameraToDefault(518),
    // CSV: 设置waz/mapobj锁定优先
    SetMapObjLockPriority(519),
    // CSV: 设置waz/mapobj是否可被武装干涉
    SetMapObjWeaponInterference(520),
    // Generated Tier 2 opcode mapping for case 0X209 (521)
    /**
     * Syscall: 设置场景摄像机视场角（FOV/焦距大小）
     */
    SetCameraFOV(521),
    // CSV: 返回waz/mapobj血量百分比
    GetMapObjHealthPercent(522),
    // CSV: 移动waz/mapobj？
    MoveMapObj(523),
    // CSV: MapObj版256
    MapObjVariant256(524),
    // CSV: MapObj版257
    MapObjVariant257(525),
    // CSV: MapObj版281
    MapObjVariant281(526),
    // CSV: MapObj版28B
    MapObjVariant28B(527),
    // CSV: 使waz/mapobject隐形
    HideMapObj(528),
    // CSV: 制造重影
    CreateMapObjAfterimage(529),
    // CSV: 在机体列表中创建机体，但不立刻登场，与214、29C之类的设置登场函数配合使用
    CreateMekWithoutDeploy(530),
    // CSV: 清除机体
    ClearMekBySlot(531),
    // CSV: 让已创建的机体登场
    DeployCreatedMek(532),
    // Generated Tier 2 opcode mapping for case 0X215 (533)
    /**
     * Syscall: 播放指定的关卡背景音乐（BGM）
     */
    PlayStageBgm(533),
    // CSV: 设置血量，不超过上限，和234或246有微妙差异
    SetMekHealthClampedA(534),
    // CSV: 设置血量，不超过上限，和234或247有微妙差异
    SetMekHealthClampedATimes10(535),
    // CSV: 基于等级设置血量，自机还要加上插件提供的血量
    SetMekHealthByLevelA(536),
    // CSV: 基于等级设置血量，自机还要加上插件提供的血量
    SetMekHealthByLevelATimes10(537),
    // CSV: 返回机体血量值
    GetMekHealthValue(538),
    // CSV: 返回机体血量百分比
    GetMekHealthPercent(539),
    // CSV: 返回机体血量上限
    GetMekHealthMax(540),
    // CSV: 加载机体（比21E优先）
    LoadMek(541),
    // CSV: 删除机体
    EraseMek(542),
    // Generated Tier 2 opcode mapping for case 0X21F (543)
    /**
     * Syscall: 在指定空间坐标播放3D角色语音
     */
    PlayVoice3D(543),
    // Generated Tier 2 opcode mapping for case 0X220 (544)
    /**
     * Syscall: 停止当前角色的语音播放
     */
    StopVoice(544),
    // CSV: 指定自机武装位
    SetPlayerWeaponSlot(545),
    // CSV: 清空自机武装位
    ClearPlayerWeaponSlot(546),
    // Generated Tier 2 opcode mapping for case 0X223 (547)
    /**
     * Syscall: 设置角色语音的播放音量大小
     */
    SetVoiceVolumeAlt(547),
    // Generated Tier 2 opcode mapping for case 0X224 (548)
    /**
     * Syscall: 暂停所有的音频播放（包括BGM、SE与语音）
     */
    PauseAllAudio(548),
    // Generated Tier 2 opcode mapping for case 0X225 (549)
    /**
     * Syscall: 恢复所有暂停的音频播放
     */
    ResumeAllAudio(549),
    // Generated Tier 2 opcode mapping for case 0X226 (550)
    /**
     * Syscall: 为当前战斗区域启用并配置环境混响特效
     */
    SetAudioReverb(550),
    // Generated Tier 2 opcode mapping for case 0X227 (551)
    /**
     * Syscall: 应用音频低通滤波器（如模拟水下/隔墙听音效果）
     */
    SetAudioLowPassFilter(551),
    // Generated Tier 2 opcode mapping for case 0X228 (552)
    /**
     * Syscall: 调整全局音轨的播放音调与变调速度
     */
    SetAudioPitch(552),
    // CSV: 机体登场
    InitDeployMek(553),
    // CSV: 清理机体信息
    ClearMekInfo(554),
    // CSV: 返回机体是否存活，0代表阵亡，1代表存活
    IsMekAlive(555),
    // CSV: 设置机体位置
    SetMekPosition(556),
    // CSV: 设置机体角度，b、c参数好像和是否调整机体图像有关，但懒得研究了
    SetMekAngle(557),
    // CSV: 可能是贴图
    SetMekTextureModeA(558),
    // CSV: 可能是贴图
    SetMekTextureModeB(559),
    // CSV: 可能是贴图
    SetMekTextureModeC(560),
    // Generated Tier 2 opcode mapping for case 0X231 (561)
    /**
     * Syscall: 设置机体当前表面贴图材质模式E
     */
    SetMekTextureModeE(561),
    // CSV: 放大/缩小
    ScaleMek(562),
    // CSV: 可能是贴图
    SetMekTextureModeD(563),
    // CSV: 设置血量（和236、237有微妙的不同）
    SetMekHealthVariantA(564),
    // CSV: 设置血量（和236、237有微妙的不同）
    SetMekHealthVariantATimes10(565),
    // CSV: 设置血量
    SetMekHealth(566),
    // CSV: 设置血量（地狱默认使用）
    SetMekHealthHellDefault(567),
    // CSV: 增加血量
    AddMekHealth(568),
    // CSV: 增加血量
    AddMekHealthTimes10(569),
    // CSV: 增加血量上限
    AddMekHealthMax(570),
    // CSV: 增加血量上限
    AddMekHealthMaxTimes10(571),
    // CSV: 设置能量
    SetMekEnergy(572),
    // CSV: 设置机体AI
    SetMekAi(573),
    // CSV: 设置等级
    SetMekLevel(574),
    // CSV: 设置自动悬浮高度
    SetMekAutoHoverHeight(575),
    // CSV: 让机体变得透明?
    SetMekTransparencyMode(576),
    // CSV: 设置机体透明度
    SetMekTransparency(577),
    // CSV: 设置机体是否受到武装影响
    SetMekWeaponAffectMode(578),
    // Generated Tier 2 opcode mapping for case 0X243 (579)
    /**
     * Syscall: 设置机体的刷怪组别标识
     */
    SetMekSpawnGroup(579),
    // CSV: 设置机体在一定时间后爆炸
    SetMekExplodeAfterTime(580),
    // CSV: 设置机体在被击败时是否爆炸
    SetMekExplodeOnDeathMode(581),
    // Generated Tier 2 opcode mapping for case 0X246 (582)
    /**
     * Syscall: 设置机体物理碰撞箱的交互类型
     */
    SetMekCollisionType(582),
    // Generated Tier 2 opcode mapping for case 0X247 (583)
    /**
     * Syscall: 设置机体的无敌状态持续时间
     */
    SetMekInvincibleTime(583),
    // CSV: 设置机体锁定优先
    SetMekLockPriority(584),
    // Generated Tier 2 opcode mapping for case 0X249 (585)
    /**
     * Syscall: 设置机体模型渲染的可见性
     */
    SetMekVisibleState(585),
    // CSV: 设置Buff值
    SetMekBuff(586),
    // CSV: 添加Buff值
    AddMekBuff(587),
    // CSV: 返回机体血量百分比
    GetMekHealthPercentAlt(588),
    // Generated Tier 2 opcode mapping for case 0X24D (589)
    /**
     * Syscall: 获取机体当前生命值的绝对数值
     */
    GetMekHealthValueAlt(589),
    // CSV: 返回初始横坐标
    GetMekInitialX(590),
    // CSV: 返回初始纵坐标
    GetMekInitialY(591),
    // CSV: 返回初始高度
    GetMekInitialZ(592),
    // Generated Tier 2 opcode mapping for case 0X251 (593)
    /**
     * Syscall: 向地图组件中注册新的机体出生坐标点
     */
    RegisterMekSpawnPoint(593),
    // Generated Tier 2 opcode mapping for case 0X252 (594)
    /**
     * Syscall: 清空已注册的机体出生坐标点
     */
    ClearMekSpawnPoint(594),
    // Generated Tier 2 opcode mapping for case 0X253 (595)
    /**
     * Syscall: 获取机体当前位置的X坐标
     */
    GetMekX(595),
    // Generated Tier 2 opcode mapping for case 0X254 (596)
    /**
     * Syscall: 获取机体当前位置的Y坐标
     */
    GetMekY(596),
    // Generated Tier 2 opcode mapping for case 0X255 (597)
    /**
     * Syscall: 获取机体当前位置的Z坐标
     */
    GetMekZ(597),
    // Generated Tier 2 opcode mapping for case 0X256 (598)
    /**
     * Syscall: 获取机体当前偏航朝向角度
     */
    GetMekAngle(598),
    // Generated Tier 2 opcode mapping for case 0X257 (599)
    /**
     * Syscall: 设置机体移动在X轴方向的分量速度
     */
    SetMekSpeedX(599),
    // Generated Tier 2 opcode mapping for case 0X258 (600)
    /**
     * Syscall: 设置机体移动在Y轴方向的分量速度
     */
    SetMekSpeedY(600),
    // CSV: 和降落动画有关
    SetMekLandingMotion(601),
    // Generated Tier 2 opcode mapping for case 0X25A (602)
    /**
     * Syscall: 设置机体着陆动作参数A
     */
    SetMekLandingMotionParamA(602),
    // Generated Tier 2 opcode mapping for case 0X25B (603)
    /**
     * Syscall: 设置机体着陆动作参数B
     */
    SetMekLandingMotionParamB(603),
    // Generated Tier 2 opcode mapping for case 0X25C (604)
    /**
     * Syscall: 设置机体着陆动作参数C
     */
    SetMekLandingMotionParamC(604),
    // Generated Tier 2 opcode mapping for case 0X25D (605)
    /**
     * Syscall: 设置机体着陆动作参数D
     */
    SetMekLandingMotionParamD(605),
    // Generated Tier 2 opcode mapping for case 0X25E (606)
    /**
     * Syscall: 设置机体着陆动作参数E
     */
    SetMekLandingMotionParamE(606),
    // Generated Tier 2 opcode mapping for case 0X25F (607)
    /**
     * Syscall: 设置机体着陆动作参数F
     */
    SetMekLandingMotionParamF(607),
    // CSV: 使机体隐形，但做出动作后会解除
    HideMekUntilAction(608),
    // CSV: 制造重影
    CreateMekAfterimage(609),
    // Generated Tier 2 opcode mapping for case 0X262 (610)
    /**
     * Syscall: 设置残影材质混合参数A
     */
    SetMekAfterimageParamA(610),
    // Generated Tier 2 opcode mapping for case 0X263 (611)
    /**
     * Syscall: 设置残影材质混合参数B
     */
    SetMekAfterimageParamB(611),
    // Generated Tier 2 opcode mapping for case 0X264 (612)
    /**
     * Syscall: 初始化并登记地图静态模型实例
     */
    InitMapObjectInstance(612),
    // Generated Tier 2 opcode mapping for case 0X265 (613)
    /**
     * Syscall: 设置地图物体的触发事件和边界条件
     */
    SetMapObjectTrigger(613),
    // Generated Tier 2 opcode mapping for case 0X266 (614)
    /**
     * Syscall: 在场景中销毁指定的地图模型实例
     */
    DeleteMapObject(614),
    // Generated Tier 2 opcode mapping for case 0X267 (615)
    /**
     * Syscall: 重置地图模型的状态至初始配置
     */
    ResetMapObjectState(615),
    // Generated Tier 2 opcode mapping for case 0X268 (616)
    /**
     * Syscall: 在指定坐标实例化一个地图物体（如可破坏的箱子或障碍物）
     */
    CreateMapObject(616),
    // Generated Tier 2 opcode mapping for case 0X269 (617)
    /**
     * Syscall: 依据ID在场景树中检索地图物体的实例地址
     */
    FindMapObjectInstance(617),
    // Generated Tier 2 opcode mapping for case 0X26A (618)
    /**
     * Syscall: 设置地图物体的世界坐标位置
     */
    SetMapObjectPos(618),
    // Generated Tier 2 opcode mapping for case 0X26B (619)
    /**
     * Syscall: 设置地图物体的空间旋转偏航角度
     */
    SetMapObjectAngle(619),
    // Generated Tier 2 opcode mapping for case 0X26C (620)
    /**
     * Syscall: 设置地图物体三轴网格模型的缩放比例
     */
    SetMapObjectScale(620),
    // Generated Tier 2 opcode mapping for case 0X26D (621)
    /**
     * Syscall: 设置地图物体的网格混合透明度值（Alpha）
     */
    SetMapObjectAlpha(621),
    // Generated Tier 2 opcode mapping for case 0X26E (622)
    /**
     * Syscall: 设置地图物体材质的着色器叠加颜色
     */
    SetMapObjectColor(622),
    // Generated Tier 2 opcode mapping for case 0X26F (623)
    /**
     * Syscall: 设置地图物体是否处于运行交互状态（激活/静止）
     */
    SetMapObjectActive(623),
    // Generated Tier 2 opcode mapping for case 0X270 (624)
    /**
     * Syscall: 更改地图物体的材质表面纹理贴图
     */
    SetMapObjectTexture(624),
    // Generated Tier 2 opcode mapping for case 0X271 (625)
    /**
     * Syscall: 为地图物体指定播放的默认三维模型动画
     */
    SetMapObjectAnim(625),
    // Generated Tier 2 opcode mapping for case 0X272 (626)
    /**
     * Syscall: 设置地图物体动画播放的帧速率倍数
     */
    SetMapObjectAnimSpeed(626),
    // Generated Tier 2 opcode mapping for case 0X273 (627)
    /**
     * Syscall: 设置地图物体动画强制跳转到的帧序号
     */
    SetMapObjectAnimFrame(627),
    // Generated Tier 2 opcode mapping for case 0X274 (628)
    /**
     * Syscall: 设置地图物体动画的循环播放模式
     */
    SetMapObjectAnimLoop(628),
    // Generated Tier 2 opcode mapping for case 0X275 (629)
    /**
     * Syscall: 启用/禁用地图物体的物理碰撞体积
     */
    SetMapObjectCollision(629),
    // Generated Tier 2 opcode mapping for case 0X276 (630)
    /**
     * Syscall: 设置地图物体受物理重力/阻力控制的标志
     */
    SetMapObjectPhysics(630),
    // Generated Tier 2 opcode mapping for case 0X277 (631)
    /**
     * Syscall: 将地图物体挂载到另一父级节点下建立坐标关联
     */
    SetMapObjectParent(631),
    // Generated Tier 2 opcode mapping for case 0X278 (632)
    /**
     * Syscall: 控制地图物体在画面中的是否隐藏渲染
     */
    SetMapObjectVisible(632),
    // Generated Tier 2 opcode mapping for case 0X279 (633)
    /**
     * Syscall: 设置地图物体的局部受重力缩放因子
     */
    SetMapObjectGravity(633),
    // Generated Tier 2 opcode mapping for case 0X27A (634)
    /**
     * Syscall: 设置地图物体的AI移动朝向的目标机体
     */
    SetMapObjectTarget(634),
    // Generated Tier 2 opcode mapping for case 0X27B (635)
    /**
     * Syscall: 设置地图物体沿X轴运动的线速度分量
     */
    SetMapObjectSpeedX(635),
    // Generated Tier 2 opcode mapping for case 0X27C (636)
    /**
     * Syscall: 设置地图物体沿Y轴运动的线速度分量
     */
    SetMapObjectSpeedY(636),
    // Generated Tier 2 opcode mapping for case 0X27D (637)
    /**
     * Syscall: 设置地图物体沿Z轴运动的线速度分量
     */
    SetMapObjectSpeedZ(637),
    // Generated Tier 2 opcode mapping for case 0X27E (638)
    /**
     * Syscall: 设置地图物体的旋转角速度分量
     */
    SetMapObjectAngleSpeed(638),
    // Generated Tier 2 opcode mapping for case 0X27F (639)
    /**
     * Syscall: 设置残影材质混合参数C
     */
    SetMekAfterimageParamC(639),
    // Generated Tier 2 opcode mapping for case 0X280 (640)
    /**
     * Syscall: 设置残影材质混合参数D
     */
    SetMekAfterimageParamD(640),
    // Generated Tier 2 opcode mapping for case 0X281 (641)
    /**
     * Syscall: 设置残影材质混合参数E
     */
    SetMekAfterimageParamE(641),
    // Generated Tier 2 opcode mapping for case 0X282 (642)
    /**
     * Syscall: 设置机体边缘发光轮廓的颜色与线宽
     */
    SetMekVisualOutline(642),
    // Generated Tier 2 opcode mapping for case 0X283 (643)
    /**
     * Syscall: 清除机体表面的所有动态粒子和附加特效
     */
    ClearMekVisualEffects(643),
    // Generated Tier 2 opcode mapping for case 0X284 (644)
    /**
     * Syscall: 设置残影材质混合参数F
     */
    SetMekAfterimageParamF(644),
    // CSV: 设置机体的残影特效
    SetMekAfterimageEffectRecovery(645),
    // CSV: 设置机体的残影特效
    SetMekAfterimageEffectRetreat(646),
    // Generated Tier 2 opcode mapping for case 0X287 (647)
    /**
     * Syscall: 设置残影材质混合参数G
     */
    SetMekAfterimageParamG(647),
    // Generated Tier 2 opcode mapping for case 0X288 (648)
    /**
     * Syscall: 设置残影材质混合参数H
     */
    SetMekAfterimageParamH(648),
    // Generated Tier 2 opcode mapping for case 0X289 (649)
    /**
     * Syscall: 设置残影材质混合参数I
     */
    SetMekAfterimageParamI(649),
    // Generated Tier 2 opcode mapping for case 0X28A (650)
    Syscall_650(650),
    // Generated Tier 2 opcode mapping for case 0X28B (651)
    /**
     * Syscall: 设置残影材质混合参数J
     */
    SetMekAfterimageParamJ(651),
    // Generated Tier 2 opcode mapping for case 0X28C (652)
    /**
     * Syscall: 设置残影材质混合参数K
     */
    SetMekAfterimageParamK(652),
    // Generated Tier 2 opcode mapping for case 0X28D (653)
    /**
     * Syscall: 设置残影材质混合参数L
     */
    SetMekAfterimageParamL(653),
    // Generated Tier 2 opcode mapping for case 0X28E (654)
    /**
     * Syscall: 设置残影材质混合参数M
     */
    SetMekAfterimageParamM(654),
    // Generated Tier 2 opcode mapping for case 0X28F (655)
    /**
     * Syscall: 设置残影材质混合参数N
     */
    SetMekAfterimageParamN(655),
    // Generated Tier 2 opcode mapping for case 0X290 (656)
    /**
     * Syscall: 设置残影材质混合参数O
     */
    SetMekAfterimageParamO(656),
    // Generated Tier 2 opcode mapping for case 0X291 (657)
    /**
     * Syscall: 设置残影材质混合参数P
     */
    SetMekAfterimageParamP(657),
    // Generated Tier 2 opcode mapping for case 0X292 (658)
    /**
     * Syscall: 设置残影材质混合参数Q
     */
    SetMekAfterimageParamQ(658),
    // Generated Tier 2 opcode mapping for case 0X293 (659)
    /**
     * Syscall: 设置残影材质混合参数R
     */
    SetMekAfterimageParamR(659),
    // Generated Tier 2 opcode mapping for case 0X294 (660)
    /**
     * Syscall: 卸载/清理联手/支援队友槽位0中的机体及资源
     */
    DeleteAllySlot0(660),
    // Generated Tier 2 opcode mapping for case 0X295 (661)
    /**
     * Syscall: 卸载/清理联手/支援队友槽位1中的机体及资源
     */
    DeleteAllySlot1(661),
    // Generated Tier 2 opcode mapping for case 0X296 (662)
    /**
     * Syscall: 卸载/清理联手/支援队友槽位2中的机体及资源
     */
    DeleteAllySlot2(662),
    // Generated Tier 2 opcode mapping for case 0X297 (663)
    /**
     * Syscall: 卸载/清理联手/支援队友槽位3中的机体及资源
     */
    DeleteAllySlot3(663),
    // Generated Tier 2 opcode mapping for case 0X298 (664)
    /**
     * Syscall: 卸载/清理联手/支援队友槽位4中的机体及资源
     */
    DeleteAllySlot4(664),
    // Generated Tier 2 opcode mapping for case 0X299 (665)
    /**
     * Syscall: 卸载/清理联手/支援队友槽位5中的机体及资源
     */
    DeleteAllySlot5(665),
    // Generated Tier 2 opcode mapping for case 0X29A (666)
    /**
     * Syscall: 卸载/清理联手/支援队友槽位6中的机体及资源
     */
    DeleteAllySlot6(666),
    // Generated Tier 2 opcode mapping for case 0X29B (667)
    /**
     * Syscall: 卸载/清理联手/支援队友槽位7中的机体及资源
     */
    DeleteAllySlot7(667),
    // CSV: 机体中途登场
    MidDeployMek(668),
    // CSV: 机体中途登场，地狱与212一起用
    MidDeployPreparedMek(669),
    // Generated Tier 2 opcode mapping for case 0X29E (670)
    /**
     * Syscall: 为支援队友设定刷出坐标和入场模式
     */
    SetAllySpawnPoint(670),
    // CSV: 设置条件（例如胜利条件）
    SetCondition(671),
    // Generated Tier 2 opcode mapping for case 0X2A0 (672)
    /**
     * Syscall: 强制命令当前槽位的队友机体释放指定攻击/掩护动作
     */
    TriggerAllyAction(672),
    // CSV: 和条件有关
    GetConditionRelated(673),
    // Generated Tier 2 opcode mapping for case 0X2A2 (674)
    /**
     * Syscall: 配置当前关卡的战斗时限（倒计时值，秒）
     */
    SetBattleLimitTimer(674),
    // Generated Tier 2 opcode mapping for case 0X2A3 (675)
    /**
     * Syscall: 获取当前关卡战斗剩余的时限值
     */
    GetBattleLimitTimer(675),
    // Generated Tier 2 opcode mapping for case 0X2A4 (676)
    /**
     * Syscall: 将战斗限时器复位
     */
    ResetBattleLimitTimer(676),
    // CSV: 返回29F设置的胜利条件是否已被满足，1代表满足
    IsConditionSatisfied(677),
    // Generated Tier 2 opcode mapping for case 0X2A6 (678)
    /**
     * Syscall: 注册地图中可破坏的围墙/防爆墙体实例
     */
    RegisterDestructibleWall(678),
    // Generated Tier 2 opcode mapping for case 0X2A7 (679)
    /**
     * Syscall: 设置可破坏墙体的最大和当前生命值
     */
    SetDestructibleWallHealth(679),
    // Generated Tier 2 opcode mapping for case 0X2A8 (680)
    /**
     * Syscall: 读取当前可破坏墙体的生命健康度
     */
    GetDestructibleWallHealth(680),
    // Generated Tier 2 opcode mapping for case 0X2A9 (681)
    /**
     * Syscall: 将指定的破坏墙体复原至未受损状态
     */
    ResetDestructibleWall(681),
    // Generated Tier 2 opcode mapping for case 0X2AA (682)
    /**
     * Syscall: 设置本关卡的积分倍率或战后评级加成参数
     */
    SetStageScoreMultiplier(682),
    // Generated Tier 2 opcode mapping for case 0X2AB (683)
    /**
     * Syscall: 直接为当前战局累加积分点数
     */
    AddStageScorePoints(683),
    // Generated Tier 2 opcode mapping for case 0X2AC (684)
    /**
     * Syscall: 读取当前战局已累计的得分类得分
     */
    GetStageScorePoints(684),
    // Generated Tier 2 opcode mapping for case 0X2AD (685)
    /**
     * Syscall: 初始化/绑定UI的连击（Combo）计数器渲染逻辑
     */
    RegisterComboCounter(685),
    // Generated Tier 2 opcode mapping for case 0X2AE (686)
    /**
     * Syscall: 获取玩家当前累积的连续击中次数
     */
    GetComboCount(686),
    // Generated Tier 2 opcode mapping for case 0X2AF (687)
    /**
     * Syscall: 强制改变玩家当前的连击计数值
     */
    SetComboCount(687),
    // Generated Tier 2 opcode mapping for case 0X2B0 (688)
    /**
     * Syscall: 开启全局火焰灼烧/热载超负荷环境状态
     */
    SetCombustMode(688),
    // Generated Tier 2 opcode mapping for case 0X2B1 (689)
    /**
     * Syscall: 为特定位置的机体应用高温超载受损特效
     */
    TriggerCombustEffect(689),
    // Generated Tier 2 opcode mapping for case 0X2B2 (690)
    /**
     * Syscall: 设置并显示战斗教学提示栏位中的指示文字
     */
    ShowTutorialMessage(690),
    // Generated Tier 2 opcode mapping for case 0X2B3 (691)
    /**
     * Syscall: 设置教学目标的状态标志（控制教学前进一步）
     */
    SetTutorialTargetState(691),
    // Generated Tier 2 opcode mapping for case 0X2B4 (692)
    /**
     * Syscall: 设置教学目标需达成的完成次数（如连击次数、技能释放次数）
     */
    SetTutorialTargetCount(692),
    // Generated Tier 2 opcode mapping for case 0X2B5 (693)
    /**
     * Syscall: 获取当前教学指引的步进进度值
     */
    GetTutorialProgress(693),
    // Generated Tier 2 opcode mapping for case 0X2B6 (694)
    /**
     * Syscall: 清空战斗教学系统的所有状态和缓存提示
     */
    ClearTutorialState(694),
    // Generated Tier 2 opcode mapping for case 0X2B7 (695)
    /**
     * Syscall: 应用战局特殊限制规则A
     */
    SetBattleSpecialRuleA(695),
    // Generated Tier 2 opcode mapping for case 0X2B8 (696)
    /**
     * Syscall: 应用战局特殊限制规则B
     */
    SetBattleSpecialRuleB(696),
    // Generated Tier 2 opcode mapping for case 0X2B9 (697)
    /**
     * Syscall: 应用战局特殊限制规则C
     */
    SetBattleSpecialRuleC(697),
    // Generated Tier 2 opcode mapping for case 0X2BA (698)
    /**
     * Syscall: 应用战局特殊限制规则D
     */
    SetBattleSpecialRuleD(698),
    // Generated Tier 2 opcode mapping for case 0X2BB (699)
    /**
     * Syscall: 查询当前处于激活状态的特殊战局规则数量
     */
    GetBattleSpecialRuleCount(699),
    // Generated Tier 2 opcode mapping for case 0X2BC (700)
    /**
     * Syscall: 基于当下的特殊战局规则配置触发脚本分支跳转
     */
    TriggerScriptBranchOnRule(700),
    // Generated Tier 2 opcode mapping for case 0X2BD (701)
    /**
     * Syscall: 将特定机体槽位注册为关卡的首领（Boss），启用Boss专属血条UI
     */
    RegisterQuestBossState(701),
    // Generated Tier 2 opcode mapping for case 0X2BE (702)
    /**
     * Syscall: 查询被标记的Boss当前是否处于被消灭状态（关卡完成前置判定）
     */
    GetQuestBossState(702);

    public final int code;

    Operand(int code) {
        this.code = code;
    }
}
