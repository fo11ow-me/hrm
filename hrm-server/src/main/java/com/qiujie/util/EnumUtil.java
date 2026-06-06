package com.qiujie.util;


import com.qiujie.enums.BaseEnum;

import java.util.*;
import java.util.stream.Collectors;

public class EnumUtil {

    /**
     * 获取所有枚举项
     *
     * @param enumType 枚举类型
     * @param <T>      BaseEnum
     * @return 枚举项列表
     */
    public static <T extends BaseEnum<?>> List<T> getEnumItems(Class<T> enumType) {
        return Arrays.asList(enumType.getEnumConstants());
    }

    /**
     * 获取key为code的map
     *
     * @param enumType 枚举类型
     * @param <V>      code泛型
     * @param <T>      BaseEnum
     * @return
     */
    public static <V, T extends BaseEnum<V>> Map<V, String> getEnumMapCode(Class<T> enumType) {
        Map<V, String> map = new HashMap<>();
        for (T enumItem : enumType.getEnumConstants()) {
            map.put((V) enumItem.getCode(), enumItem.getMessage());
        }
        return map;
    }

    /**
     * 获取key为value的map
     *
     * @param enumType 枚举类型
     * @param <V>      code泛型
     * @param <T>      BaseEnum
     * @return
     */
    public static <V, T extends BaseEnum<V>> Map<String, V> getEnumMapValue(Class<T> enumType) {
        Map<String, V> map = new HashMap<>();
        for (T enumItem : enumType.getEnumConstants()) {
            map.put(enumItem.getMessage(), (V) enumItem.getCode());
        }
        return map;
    }

    /**
     * 获取枚举所有项的列表
     *
     * @param enumType 枚举类型
     * @param <T>      BaseEnum
     * @return
     */
    public static <T extends BaseEnum<?>> List<Map<String, Object>> getEnumList(Class<T> enumType) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!enumType.isEnum()) {
            return new ArrayList<>();
        }
        T[] enums = enumType.getEnumConstants();
        if (enums == null || enums.length <= 0) {
            return new ArrayList<>();
        }
        for (T enumItem : enums) {
            Map<String, Object> map = new HashMap<>();
            map.put("code", enumItem.getCode());
            map.put("message", enumItem.getMessage());
            list.add(map);
        }
        return list;
    }

    /**
     * 获取枚举的所有Code值
     *
     * @param enumType 枚举类型
     * @param <T>      BaseEnum
     * @param <V>      code泛型
     * @return 枚举code值列表
     */
    public static <V, T extends BaseEnum<V>> List<V> getEnumCodes(Class<T> enumType) {
        return (List<V>) Arrays.stream(enumType.getEnumConstants()).map(BaseEnum::getCode).collect(Collectors.toList());
    }

    /**
     * 获取枚举的所有Value值
     *
     * @param enumType 枚举类型
     * @param <T>      BaseEnum
     * @return 枚举value值列表
     */
    public static <T extends BaseEnum<?>> List<String> getEnumValues(Class<T> enumType) {
        return Arrays.stream(enumType.getEnumConstants()).map(BaseEnum::getMessage).collect(Collectors.toList());
    }

    /**
     * 根据Code值查询对应的枚举项
     *
     * @param enumType 枚举类型
     * @param code     枚举code
     * @param <T>      BaseEnum
     * @param <V>      code泛型
     * @return 枚举项
     */
    public static <V, T extends BaseEnum<V>> T fromCode(Class<T> enumType, V code) {
        if (code == null) {
            return null;
        }
        for (T enumItem : enumType.getEnumConstants()) {
            if (Objects.equals(code, enumItem.getCode())) {
                return enumItem;
            }
        }
        return null;

    }

    /**
     * 根据message获取对应的枚举项
     *
     * @param enumType
     * @param message
     * @param <T>
     * @return
     */
    public static <T extends BaseEnum<?>> T fromValue(Class<T> enumType, String message) {
        if (message == null) {
            return null;
        }
        for (T enumItem : enumType.getEnumConstants()) {
            if (Objects.equals(message, enumItem.getMessage())) {
                return enumItem;
            }
        }
        return null;

    }


    /**
     * 获取枚举元素列表
     *
     *
     * @param enumType 枚举类型
     * @param <T>      BaseEnum
     * @return
     */
    public static <T extends BaseEnum<?>> List<Map<String, Map<String, Object>>> getEnumItemList(Class<T> enumType) {
        List<Map<String, Map<String, Object>>> list = new ArrayList<>();
        if (!enumType.isEnum()) {
            return new ArrayList<>();
        }
        T[] enums = enumType.getEnumConstants();
        if (enums == null || enums.length <= 0) {
            return new ArrayList<>();
        }
        for (T enumItem : enums) {
            Map<String, Object> map = new HashMap<>();
            map.put("code", enumItem.getCode());
            map.put("message", enumItem.getMessage());
            Map<String, Map<String, Object>> map1 = new HashMap<>();
            map1.put(enumItem.toString(),map);
            list.add(map1);
        }
        return list;
    }

}
