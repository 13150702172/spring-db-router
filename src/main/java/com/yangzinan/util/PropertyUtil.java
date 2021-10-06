package com.yangzinan.util;

import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 获取配置文件的工具类
 * TODO:该方法需要深入理解
 */
public class PropertyUtil {

    //默认版本
    private static int springBootVersion = 1;

    static {
        try {
            //这个是通过反射获取RelaxedPropertyResolver，用于读取配置文件
            //通过查阅资料：该类已经不再使用，但是为了兼容Springboot 1.X的版本，所以我们在这里需要判断
            //如果不存在，就说明Springboot为2.X版本，我们就把springBootVersion = 2
            //TODO：RelaxedPropertyResolver需要进一步了解
            Class.forName("org.springframework.boot.bind.RelaxedPropertyResolver");
        } catch (ClassNotFoundException e) {
            springBootVersion = 2;
        }
    }

    /**
     * 根据不同的Springboot版本获取不同的处理方法
     * @param environment
     * @param prefix
     * @param targetClass
     * @param <T>
     * @return
     */
    public static <T> T handle(final Environment environment, final String prefix, final Class<T> targetClass) {
        switch (springBootVersion){
            case 1:
                return (T) v1(environment, prefix);
            default:
                return (T) v2(environment,prefix,targetClass);
        }
    }

    /**
     * springboot 1.x 执行的方法
     * @param environment
     * @param prefix
     * @return
     */
    private static Object v1(Environment environment,final String prefix){
        try {
            //反射获取RelaxedPropertyResolver对象
            Class<?> resolverClass = Class.forName("org.springframework.boot.bind.RelaxedPropertyResolver");
            Constructor<?> resolverConstructor = resolverClass.getDeclaredConstructor(PropertyResolver.class);
            //获取getSubProperties方法信息
            Method getSubPropertiesMethod = resolverClass.getDeclaredMethod("getSubProperties", String.class);
            Object resolverObject = resolverConstructor.newInstance(environment);
            String prefixParam = prefix.endsWith(".") ? prefix : prefix + ".";
            return getSubPropertiesMethod.invoke(resolverObject, prefixParam);
        } catch (final ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
                | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * springboot 2.X执行的方法
     * @param environment
     * @param prefix
     * @param targetClass
     * @return
     */
    private static Object v2(final Environment environment, final String prefix, final Class<?> targetClass){
        try {
            //反射获取Binder对象
            Class<?> binderClass = Class.forName("org.springframework.boot.context.properties.bind.Binder");
            //获取相应的方法信息
            Method getMethod = binderClass.getDeclaredMethod("get", Environment.class);
            Method bindMethod = binderClass.getDeclaredMethod("bind", String.class, Class.class);

            Object binderObject = getMethod.invoke(null, environment);
            String prefixParam = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
            Object bindResultObject = bindMethod.invoke(binderObject, prefixParam, targetClass);
            Method resultGetMethod = bindResultObject.getClass().getDeclaredMethod("get");
            return resultGetMethod.invoke(bindResultObject);
        } catch (final ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }




}
