package com.yangzinan.dynamic;

import com.yangzinan.DBContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 用于选择数据源
 * 因为我们注入的数据源为HashMap,所以我们只需要返回key就可以找到对应的数据源
 */
public class DynaminDataSource extends AbstractRoutingDataSource {

    /**
     * 这个方法用于选择数据源
     * 我们在配置文件中设定名称为db01,db02
     * 我们这里只需返回名称，就可以找到指定的数据源
     * @return
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return "db"+ DBContextHolder.getDbKey();
    }
}
