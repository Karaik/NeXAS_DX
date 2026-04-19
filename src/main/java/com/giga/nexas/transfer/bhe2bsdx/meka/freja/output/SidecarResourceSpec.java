package com.giga.nexas.transfer.bhe2bsdx.meka.freja.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SidecarResourceSpec {

    
    private boolean includeWeaponEquipDat;

    
    private String weaponEquipFileName;

    
    private String externalProgramMaterialFileName;

    public static SidecarResourceSpec defaultFrejaSidecars() {
        return SidecarResourceSpec.builder()
                .includeWeaponEquipDat(true)
                .weaponEquipFileName("WeaponEquip.dat")
                .externalProgramMaterialFileName("ProgramMaterial.grp")
                .build();
    }
}
