package com.giga.nexas.transfer.bhe2bsdx.meka.sou.menu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MenuOverrideSpec {

    
    private int visibleSlotIndex;

    
    private Integer stateDonorRowIndex;

    
    private List<String> mekaPilotImageNames;

    
    private List<String> selectMenuMekaImageNames;

    
    private MenuLayoutPolicy mekaPilotLayoutPolicy;

    
    private MenuLayoutPolicy selectMenuMekaLayoutPolicy;

}
