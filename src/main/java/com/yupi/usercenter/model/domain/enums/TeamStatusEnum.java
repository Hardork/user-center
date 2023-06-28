package com.yupi.usercenter.model.domain.enums;

import lombok.Data;

/**
 * @Author:HWQ
 * @DateTime:2023/5/3 19:01
 * @Description: 枚举队伍状态
 **/
public enum TeamStatusEnum {
    PUBLIC(0,"公开"),
    PRIVATE(1,"私有"),
    SECRET(2,"加密"),
    ALL(3,"全部")
    ;


    public static TeamStatusEnum getEnumByValue(Integer value){
        if(value == null){
            return null;
        }
        TeamStatusEnum[] values = TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : values) {
            if(teamStatusEnum.value == value){
                return teamStatusEnum;
            }
        }
        return null;
    }

    private int value;
    private String text;

    TeamStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue(){
        return value;
    }

    public String getText(){
        return text;
    }
}
