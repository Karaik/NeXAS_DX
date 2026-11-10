/**
 * Transfer 流水线维护 skill。
 *
 * <p>这个包不承载运行时业务代码，主要用于沉淀 transfer 目录下移植流水线的
 * 背景、分层模型、验收标准和维护细则。当前重点服务 JINKI -> BSDX V2，
 * 后续 BHE 接入时也应先阅读同包下的 {@code SKILL.md}，再决定改动应该落在
 * 源游戏转换层、前置客制化输入、通用 graft 主线，还是后置兼容和输出沉淀层。</p>
 */
package com.giga.nexas.transfer.skill;
