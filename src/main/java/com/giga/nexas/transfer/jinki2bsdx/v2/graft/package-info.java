/**
 * 通用 graft 主线包。
 *
 * <p>这里放源资产加载、baseline 加载、资源闭包、GRP append/index mapping、
 * MEK/WAZ 重绑、ProgramMaterial/MapGroup/MekMaterial 对齐，以及最终输出目录沉淀。</p>
 *
 * <p>这个包的目标是后续 BHE 也能复用的主线能力。
 * 角色名、菜单 slot、MOD PNG、exe patch 位点等客制化事实不应该散进这里。</p>
 */
package com.giga.nexas.transfer.jinki2bsdx.v2.graft;
