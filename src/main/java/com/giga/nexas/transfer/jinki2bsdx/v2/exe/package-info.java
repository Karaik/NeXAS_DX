/**
 * EXE 兼容 patch 包。
 *
 * <p>这里把 patch site/profile/audit/apply 分开。
 * patch 执行只处理字节位点，容量和菜单行数需求由 plan step 计算。</p>
 *
 * <p>为了支持 {@code BSDX -> BSDX+JINKI -> BSDX+JINKI+BHE} 的链式成果物，
 * 每个 patch site 都接受 expected bytes 或 target bytes；
 * 已经 patch 过的上一层成果物可以继续作为下一层 baseline。</p>
 */
package com.giga.nexas.transfer.jinki2bsdx.v2.exe;
