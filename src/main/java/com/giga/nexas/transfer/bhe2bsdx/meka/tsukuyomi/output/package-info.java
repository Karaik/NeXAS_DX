/**
 * 输出审计和 sidecar 边界包。
 *
 * <p>这里不承担主 graft 逻辑，只记录和比较“最终输出了什么”：
 * sidecar 输出、manifest、同名覆盖记录、输出目录和解包目录的文件集合/byte 比较。</p>
 *
 * <p>最终验收使用相对路径作为文件身份。
 * 少文件、多文件、路径层级不同、同名文件 byte 不同都必须视为失败。</p>
 */
package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.output;
