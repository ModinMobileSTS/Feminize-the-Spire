/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.helpers.SuperclassFinder
 */
package monsterFeminization.utils;

import basemod.helpers.SuperclassFinder;
import java.lang.reflect.Field;

public class FeminizationUtil {
    public static <T> T getField(Object o, String name) throws Exception {
        Field field = SuperclassFinder.getSuperclassField(o.getClass(), (String)name);
        field.setAccessible(true);
        return (T)field.get(o);
    }

    public static <T> T getField(Class<?> cla, String name) throws Exception {
        Field field = SuperclassFinder.getSuperclassField(cla, (String)name);
        field.setAccessible(true);
        return (T)field.get(null);
    }

    public static <T> void setField(Object o, String name, T value) throws Exception {
        Field field = SuperclassFinder.getSuperclassField(o.getClass(), (String)name);
        field.setAccessible(true);
        field.set(o, value);
    }
}

