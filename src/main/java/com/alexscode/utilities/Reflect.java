package com.alexscode.utilities;

import java.lang.reflect.Field;

public class Reflect {

    /**
     * Gets private fields from any object
     * @param target the object to steal a field from
     * @param fieldName the name of the field
     * @return the field in question (you'll have to cast it yourself)
     */
    public static Object getField(Object target, String fieldName){

        Field field = null;
        try {
            field = target.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }

            field.setAccessible(true);


        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }


}
