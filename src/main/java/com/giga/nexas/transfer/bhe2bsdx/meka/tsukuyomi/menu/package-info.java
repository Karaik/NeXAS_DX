/**
 * 菜单后置覆盖包。
 *
 * <p>菜单资源不是 MEK/WAZ 主闭包自然能推出的资源链。
 * 本包负责从 BSDX 菜单 dat 关系推导槽位，重建菜单 DAT/SPM/PNG 产物，
 * 并把输出写回同一个 outputRoot。</p>
 *
 * <p>复用边界是 {@code MenuOverrideSpec}：
 * 后续换角色、换 donor、换菜单图片时优先换 spec，不改重建算法。</p>
 */
package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu;
