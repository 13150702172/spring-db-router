package com.yangzinan;

/**
 * 用于存储dbkey tbkey
 */
public class DBContextHolder {
    private  static ThreadLocal<String> dbkey = new ThreadLocal<>();
    private static ThreadLocal<String> tbkey = new ThreadLocal<>();

    public static void setDbkey(String dbKeyIdx){
        dbkey.set(dbKeyIdx);
    }

    public static void setTbKey(String tbKeyIdx){
        tbkey.set(tbKeyIdx);
    }

    public static String getDbKey(){
        return dbkey.get();
    }

    public static String getTbKey(){
        return tbkey.get();
    }

    public static void clearDbkey(){
        dbkey.remove();
    }

    public static void clearTbKey(){
        tbkey.remove();
    }
}
