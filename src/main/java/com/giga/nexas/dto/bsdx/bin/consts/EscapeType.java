package com.giga.nexas.dto.bsdx.bin.consts;

/**
 * 文本内的转义符
 * 逆向于 青夏轨迹体验版中文版本
 */
public enum EscapeType {

    NEW_LINE("@n"),
    PAUSE("@p"),
    FONT_SIZE("@s"),
    FONT_COLOR("@i"),
    TEXT_DISP_SPEED("@m"),
    TEXT_FADE_TIME("@f"),
    TEXT_CENTER("@c"),
    VOICE_VOLUME("@o"),
    WAIT("@w"),
    VOICE_WAIT("@t"),
    VOICE("@v"),
    FACE_TYPE_H("@h"),
    RUBY("@r"),
    NO_KEY_WAIT("@k"),
    NO_KEY_WAIT_E("@e"),
    FORCE_AUTO("@a"),
    GAIJI("@g"),
    TEXT_NO_DISP("@d"),
    VOICE_OVER_PLAY("@l"),
    ESCAPE("@@"),

    FACE_TYPE_CAPITAL_H("@H"),
    FACE_TYPE_J("@j");

    public final String escapeChar;

    EscapeType(String escapeChar) {
        this.escapeChar = escapeChar;
    }
}
