/**
 * TSUKUYOMI -> BSDX 当前 的顶层入口包。
 *
 * <p>根包只放总编排入口和总览文档，不放具体业务步骤。
 * 可复用业务步骤按职责拆到 {@code graft / menu / exe / output / pack} 子包里，
 * 避免再次退化成一层平铺的“新旧 step 对照表”。</p>
 *
 * <p>当前 当前 的硬验收标准是最终生成物与旧 pipeline 完全一致：
 * 输出目录逐文件 byte-identical，打包对象解包后内部文件也逐文件 byte-identical。</p>
 */
package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi;
