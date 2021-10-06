package com.yangzinan;

/**
 * 用于存储tbIdx
 */
public class DBRouterBase {

    private String tbIdx;

    public String getTbIdx(){
        return DBContextHolder.getTbKey();
    }
}
