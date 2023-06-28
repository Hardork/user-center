package com.yupi.usercenter.utils;

import com.yupi.usercenter.model.domain.User;

import java.lang.reflect.Field;

/**
 * @Author:HWQ
 * @DateTime:2023/6/27 0:11
 * @Description:
 **/
public class MyBeanUtils {
    public static void copyProperties(Object source, Object target) throws Exception {
        Class sourceClass = source.getClass();
        Class targetClass = target.getClass();

        Field[] sourceFields = sourceClass.getDeclaredFields();
        Field[] targetFields = targetClass.getDeclaredFields();

        for (Field sourceField : sourceFields) {
            for (Field targetField : targetFields) {
                if (sourceField.getName().equals(targetField.getName()) && sourceField.getType() == targetField.getType()) {
                    sourceField.setAccessible(true);
                    targetField.setAccessible(true);
                    Object value = sourceField.get(source);
                    targetField.set(target, value);
                    break;
                }
            }
        }
    }
}
