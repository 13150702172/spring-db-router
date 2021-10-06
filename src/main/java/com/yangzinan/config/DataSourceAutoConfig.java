package com.yangzinan.config;

import com.yangzinan.DBRouterConfig;
import com.yangzinan.dynamic.DynaminDataSource;
import com.yangzinan.util.PropertyUtil;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 读取配置文件的信息
 */
@Configuration
public class DataSourceAutoConfig implements EnvironmentAware {

    //存储配置文件中数据源的信息
    private Map<String, Map<String, Object>> dataSourceMap = new HashMap<>();

    private int dbCount;  //分库数
    private int tbCount;  //分表数

    @Bean
    public DBRouterConfig dbRouterConfig(){
        return new DBRouterConfig(dbCount,tbCount);
    }

    @Override
    public void setEnvironment(Environment environment) {
        //配置文件中的前缀
        String prefix = "router.jdbc.datasource.";

        //配置文件中获取dbCount tbCount
        dbCount = Integer.valueOf(environment.getProperty(prefix+"dbCount"));
        tbCount = Integer.valueOf(environment.getProperty(prefix+"tbCount"));

        //配置文件中获取list数据
        //因为我们的list: db01,db02
        //所以我们需要以逗号分隔为集合
        String dataSources = environment.getProperty(prefix + "list");
        for(String dbInfo : dataSources.split(",")){
            /**
             * 获取数据源信息，因为我们配置信息如下：
             * db01:
             *    url: ******
             *    user:******
             *    driver-class-name: **********
             *    password:***********
             * 所以我们需要将它转换为Map
             */
            Map<String,Object> dataSourceProps = PropertyUtil.handle(environment, prefix + dbInfo, Map.class);
            //存储到Map中
            dataSourceMap.put(dbInfo,dataSourceProps);
        }
    }


    /**
     * 配置数据源信息
     */
    @Bean
    public DataSource dataSource(){
        //创建数据源
        Map<Object, Object> targetDataSources = new HashMap<>();
        for(String dbInfo : dataSourceMap.keySet()){
            Map<String,Object> objMap = dataSourceMap.get(dbInfo);
            //是将url、username、password封装到targetDataSources中
            targetDataSources.put(dbInfo,new DriverManagerDataSource(objMap.get("url").toString(),objMap.get("username").toString(),objMap.get("password").toString()));
        }

        DynaminDataSource dynaminDataSource = new DynaminDataSource();
        dynaminDataSource.setDefaultTargetDataSource(targetDataSources);
        return dynaminDataSource;
    }
}
