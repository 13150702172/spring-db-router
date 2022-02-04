

<aside>
💡 项目参考

</aside>

主要参考：[https://zhuanlan.zhihu.com/p/401273115](https://zhuanlan.zhihu.com/p/401273115) 文章《基于AOP和HashMap原理学习，开发Mysql分库分表路由组件！》

<aside>
💡 技术分析

</aside>

- 数据库技术

       垂直拆分

 水平拆分

- AOP
- 数据源切换
- 散列算法
- 哈希寻址
- ThreadLocal

<aside>
💡 技术调研

</aside>

ArrayList、LinkedList、Queue、Stack都是顺序存储，并没有使用哈希索引方式，***`HashMap、ThreadLocal`***使用了哈希索引、散列算法以及在数据膨胀时候的拉链寻址和开放寻址

- ThreadLocal

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/1b632595-849a-45c8-b1ae-4061429436e7/Untitled.png)

实现斐波那契散列算法

***`(i*HASH_INCREMENT + HASH_INCREMENT) & 16 -1`***

***`HASH_INCREMENT = 0x61c88647`***

```java
    private final static Integer HASH_INCREMENT = 0x61c88647;

    /**
     * 斐波那契算法
     */
    @Test
    void contextLoads() {
        Integer number = 0;
        for(int i = 0;i < 16;i++){
            number = (i * HASH_INCREMENT + HASH_INCREMENT) & (16 - 1);
            System.out.println("斐波那契："+number+",普通散列："+ (String.valueOf(i).hashCode() & (16-1)));
        }
    }

斐波那契：7,普通散列：0
斐波那契：14,普通散列：1
斐波那契：5,普通散列：2
斐波那契：12,普通散列：3
斐波那契：3,普通散列：4
斐波那契：10,普通散列：5
斐波那契：1,普通散列：6
斐波那契：8,普通散列：7
斐波那契：15,普通散列：8
斐波那契：6,普通散列：9
斐波那契：13,普通散列：15
斐波那契：4,普通散列：0
斐波那契：11,普通散列：1
斐波那契：2,普通散列：2
斐波那契：9,普通散列：3
斐波那契：0,普通散列：4
```

- **数据结构**：散列表的数组结构
- **散列算法**：斐波那契（Fibonacci）散列法
- **寻址方式**：Fibonacci 散列法可以让数据更加分散，在发生数据碰撞时进行开放寻址，从碰撞节点向后寻找位置进行存放元素。公式：`f(k) = ((k * 2654435769) >> X) << Y对于常见的32位整数而言，也就是 f(k) = (k * 2654435769) >> 28`，黄金分割点：`(√5 - 1) / 2 = 0.61803398871.618:1 == 1:0.618`
- **学到什么**：可以参考寻址方式和散列算法，但这种数据结构与要设计实现作用到数据库上的结构相差较大，不过 ThreadLocal 可以用于存放和传递数据索引信息。

- HashMap

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/e6cbf855-1371-4ab5-99f4-dbe132feb2aa/Untitled.png)

```java
public int disturbHashIdx(String key,Integer size){
   return (size - 1)&(key.hashCode() ^ (key.hashCode() >>> 16));
}
```

- **数据结构**：哈希桶数组 + 链表 + 红黑树
- **散列算法**：扰动函数、哈希索引，可以让数据更加散列的分布
- **寻址方式**：通过拉链寻址的方式解决数据碰撞，数据存放时会进行索引地址，遇到碰撞产生数据链表，在一定容量超过8个元素进行扩容或者树化。
- **学到什么**：可以把散列算法、寻址方式都运用到数据库路由的设计实现中，还有整个数组+链表的方式其实库+表的方式也有类似之处。

<aside>
💡 设计

</aside>

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/97b896e7-227b-4195-8a7a-d5d54f9c57ca/Untitled.png)

根据上图，我们进行对重要点分析：

- `***连接池：***`它主要是池化的作用，可以在里面保存多个DB数据源，可以按需加载，以及切换
- ***`AbstracRoutingDataSource：`***用于动态切换数据源Spring的服务类，它提供了获取数据源的抽象方法**determineCurrentLookupKey**，它主要用于返回数据库的key，用于切换数据源
- `***路由计算：***`将数据均匀的分布在数据库表中

<aside>
🌕 具体实现

</aside>

> 目录结构
> 

```java
|____src
| |____main
| | |____resources
| | | |____META-INF
| | | | |____spring.factories
| | |____java
| | | |____com
| | | | |____yangzinan
| | | | | |____dynamic
| | | | | | |____DynamicDataSource.java
| | | | | |____util
| | | | | | |____PropertyUtil.java
| | | | | |____config
| | | | | | |____DataSourceAutoConfig.java
| | | | | |____annotation
| | | | | | |____DBRouter.java
| | | | | |____DBRouterConfig.java
| | | | | |____DBRouterBase.java
| | | | | |____DBContextHolder.java
| | | | | |____DBRouterJoinPoint.java

```

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/84aff5bf-ac86-4ddd-af7a-fb9f1e708850/Untitled.png)

> 基本分析
> 
- **依赖**

```xml
       <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!--读取配置文件-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!--mybatis-->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>2.1.4</version>
        </dependency>

        <!--mysql-->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.19</version>
        </dependency>

        <!--测试-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>commons-beanutils</groupId>
            <artifactId>commons-beanutils</artifactId>
            <version>1.9.4</version>
        </dependency>
        <dependency>
            <groupId>commons-lang</groupId>
            <artifactId>commons-lang</artifactId>
            <version>2.6</version>
        </dependency>

        <!--json相关-->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.75</version>
        </dependency>

        <!--单元测试-->
            <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope>
        </dependency>
```

根据代码结构作如下分析：

- **DBContextHolder**

给主要是为其他类提供tbkey dbkey支持，相当于媒介类

我们可以思考一个问题：`他的tbkey dbkey是从什么地方来的？`  它是从`DBRouterJoinPoint`计算而来

```java
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
```

- **DBRouterBase**

该类主要是为了数据表的索引

主要用途，就是一个实体继承DBRouterBase，在Mybatis操作中，将tbidx作为sql语句的一部分

```java
/**
 * 用于存储tbIdx
 */
public class DBRouterBase {

    private String tbIdx;

    public String getTbIdx(){
        return DBContextHolder.getTbKey();
    }
}
```

***具体使用是这样的：***

```java
public class User extends ***DBRouterBase*** {

    private Long id;
    private String userId;          // 用户ID
    private String userNickName;    // 昵称
    private String userHead;        // 头像
    private String userPassword;    // 密码
    private Date createTime;        // 创建时间
    private Date updateTime;        // 更新时间

		//getter setter
}
```

```java
<select id="queryUserInfoByUserId" parameterType="com.yangzinan.entity.User"
            resultType="com.yangzinan.entity.User">

        SELECT id, userId, userNickName, userHead, userPassword, createTime
        FROM ***user_${tbIdx}***
        WHERE userId = #{userId}

    </select>
```

- DynamicDataSource

该类继承`**AbstractRoutingDataSource`**  主要是为了将所有的数据源包装成HashMap的形式，里面的**`determineCurrentLookupKey`** 方法，是根据用户指定的数据源名称进行切换

又有一个思考：**为什么会进行切换呢？**

原因就是因为我们将数据源封装为`**HashMap<String,DataSource>**`形式，我们只需要给**`determineCurrentLookupKey`** 传递一个key,就可以获取DataSource，具体请看源码分析

***源码分析：***

```java
protected DataSource determineTargetDataSource() {
        Assert.notNull(this.resolvedDataSources, "DataSource router not initialized");
       
        //这个类就是我们需要实现的抽象方法，他返回一个key
        Object lookupKey = this.determineCurrentLookupKey();
				
        //这里是从resolvedDataSources，也就是HashMap中获取DataSource
        DataSource dataSource = (DataSource)this.resolvedDataSources.get(lookupKey);
        
        if (dataSource == null && (this.lenientFallback || lookupKey == null)) {
            dataSource = this.resolvedDefaultDataSource;
        }

        if (dataSource == null) {
            throw new IllegalStateException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
        } else {
            return dataSource;
        }
    }

    //需要实现的抽象方法
    @Nullable
    protected abstract Object determineCurrentLookupKey();
```

***我们的实现：***

```java
/**
 * 用于选择数据源
 * 因为我们注入的数据源为HashMap,所以我们只需要返回key就可以找到对应的数据源
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

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
```

- **DataRoutingConfig**

该类主要是为了存储表的数量、数据库的数量，也是一个媒介类，*`**他的信息是从配置文件中获取**`*

```java
package com.yangzinan;

public class DBRouterConfig {

    private int dbCount;  //分库数
    private int tbCount;  //分表数

    public DBRouterConfig() {
    }
    public DBRouterConfig(int dbCount, int tbCount) {
        this.dbCount = dbCount;
        this.tbCount = tbCount;
    }
    public int getDbCount() {
        return dbCount;
    }
    public void setDbCount(int dbCount) {
        this.dbCount = dbCount;
    }
    public int getTbCount() {
        return tbCount;
    }
    public void setTbCount(int tbCount) {
        this.tbCount = tbCount;
    }

}
```

- **DBRouter**

`***注解***`，用于标识需要数据库路由的方法以及哪一个字段需要用于计算

```java
/**
 * 自定义注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface DBRouter {
    String key() default "";
}
```

- **DataSourceAutoConfig**

该类用于读取配置文件数据源相关信息

在DataSourceAutoConfig我们还需要先写一个工具类，用于读取配置信息，将配置信息读取为map对象，比如配置文件为：

```xml
db01:
	username: a
	password: b
	url: c
	driver-class-name: d
```

我们就需要读取为：**`Map<String,Map<String,String>>`**，我们拆解一下：

我们现将`**Map<String,String>**`看作**`Object`**，那么结构就是`**Map<String,Object>**`

1. key是什么？根据上面的配置文件来讲，**`key`**就是**`db01`**
2. `**Object——>Map<String,String>**`：这里的key value分别是什么？key也就是username、password、driver-class-name；value就是a、b、c、d

  ***工具类:***

```java
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
```

***DataSourceAutoConfig*:**

```java

package com.yangzinan.config;

import com.yangzinan.DBRouterConfig;
import com.yangzinan.dynamic.DynamicDataSource;
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

        DynamicDataSource dynaminDataSource = new DynamicDataSource();
        dynaminDataSource.setTargetDataSources(targetDataSources);
        return dynaminDataSource;
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

}
```

- **DBRouterJoinPoint**

该类为aspect，最根本的操作就是在这里，他计算了db、tb的索引

```java
package com.yangzinan;

import com.yangzinan.annotation.DBRouter;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component("db-router-point")
public class DBRouterJoinPoint {

    private Logger log = LoggerFactory.getLogger(DBRouterJoinPoint.class);

    @Autowired
    private DBRouterConfig dbRouterConfig;

    @Pointcut("@annotation(com.yangzinan.annotation.DBRouter)")
    public void aopPoint() {
    }

    @Around("aopPoint() && @annotation(dbRouter)")
    public Object doRouter(ProceedingJoinPoint jp, DBRouter dbRouter) throws Throwable {
        //这个是我们在注解中的属性key,用于配置哪一个字段参与路由计算
        String dbKey = dbRouter.key();

        if(StringUtils.isBlank(dbKey)) throw new RuntimeException("annotation DBRouter key is null！");
        //计算路由
        String dbKeyAttr = getAttrValue(dbKey,jp.getArgs());

        /**
         * 公式为(size - 1)&(key.hashCode() ^ (key.hashCode() >>> 16))
         */
        Integer size = dbRouterConfig.getTbCount()*dbRouterConfig.getDbCount();
        // 扰动函数
        int idx = (size - 1) & (dbKeyAttr.hashCode() ^ (dbKeyAttr.hashCode() >>> 16));
        // 库表索引
        int dbIdx = idx / dbRouterConfig.getTbCount() + 1;
        int tbIdx = idx - dbRouterConfig.getTbCount() * (dbIdx - 1);
        // 设置到 ThreadLocal
        DBContextHolder.setDbkey(String.format("%02d", dbIdx));
        DBContextHolder.setTbKey(String.format("%02d", tbIdx));
        log.info("数据库路由 method：{} dbIdx：{} tbIdx：{}", getMethod(jp).getName(), dbIdx, tbIdx);
        // 返回结果
        try {
            return jp.proceed();
        } finally {
            DBContextHolder.clearDbkey();
            DBContextHolder.clearTbKey();
        }
    }

    /**
     * 获取方法信息
     * @param jp
     * @return
     * @throws NoSuchMethodException
     */
    private Method getMethod(JoinPoint jp) throws NoSuchMethodException {
        Signature sig = jp.getSignature();
        MethodSignature methodSignature = (MethodSignature) sig;
        return jp.getTarget().getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
    }

    /**
     * 获取属性值
     * @param attr
     * @param args
     * @return
     */
    public String getAttrValue(String attr, Object[] args) {
        String filedValue = null;
        for(Object arg : args){
            try {
                if (StringUtils.isNotBlank(filedValue)) break;
                filedValue = BeanUtils.getProperty(arg, attr);
            } catch (Exception e) {
                log.error("获取路由属性值失败 attr：{}", attr, e);
            }
        }
        return filedValue;
    }

}
```

- ***自动注入***

我们需要在`***src/main/resources***`下创建META-INF，最后创建**`*spring.factories*`**

```java
org.springframework.boot.autoconfigure.EnableAutoConfiguration=com.yangzinan.config.DataSourceAutoConfig
```

<aside>
💡 测试

</aside>

测试之前，我们需要将上面的代码打包，创建新的项目`***spring-router-test***`

代码打包之后，

```bash
mvn install
```

我们只需要要在测试项目中引入依赖即可 ：

```xml
				<!--引入自定义的项目-->
        <dependency>
            <groupId>com.yangzinan</groupId>
            <artifactId>spring-db-router</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
```

- **依赖**

```xml
				<!--引入自定义的项目-->
        <dependency>
            <groupId>com.yangzinan</groupId>
            <artifactId>spring-db-router</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.19</version>
        </dependency>

        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>2.1.4</version>
        </dependency>

        <dependency>
            <groupId>commons-beanutils</groupId>
            <artifactId>commons-beanutils</artifactId>
            <version>1.8.3</version>
        </dependency>
        <dependency>
            <groupId>commons-lang</groupId>
            <artifactId>commons-lang</artifactId>
            <version>2.6</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.75</version>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope>
        </dependency>
```

- 创建数据库、数据表

**数据库：db01、db02**

**数据表: user_01、user_02、user_03**

```sql
create database `db_01`;
CREATE TABLE user_01 ( id bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID', userId varchar(9) COMMENT '用户ID', userNickName varchar(32) COMMENT '用户昵称', userHead varchar(16) COMMENT '用户头像', userPassword varchar(64) COMMENT '用户密码', createTime datetime COMMENT '创建时间', updateTime datetime COMMENT '更新时间', PRIMARY KEY (id) ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE user_02 ( id bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID', userId varchar(9) COMMENT '用户ID', userNickName varchar(32) COMMENT '用户昵称', userHead varchar(16) COMMENT '用户头像', userPassword varchar(64) COMMENT '用户密码', createTime datetime COMMENT '创建时间', updateTime datetime COMMENT '更新时间', PRIMARY KEY (id) ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE user_03 ( id bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID', userId varchar(9) COMMENT '用户ID', userNickName varchar(32) COMMENT '用户昵称', userHead varchar(16) COMMENT '用户头像', userPassword varchar(64) COMMENT '用户密码', createTime datetime COMMENT '创建时间', updateTime datetime COMMENT '更新时间', PRIMARY KEY (id) ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE user_04 ( id bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID', userId varchar(9) COMMENT '用户ID', userNickName varchar(32) COMMENT '用户昵称', userHead varchar(16) COMMENT '用户头像', userPassword varchar(64) COMMENT '用户密码', createTime datetime COMMENT '创建时间', updateTime datetime COMMENT '更新时间', PRIMARY KEY (id) ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create database `db_02`;
CREATE TABLE user_01 ( id bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID', userId varchar(9) COMMENT '用户ID', userNickName varchar(32) COMMENT '用户昵称', userHead varchar(16) COMMENT '用户头像', userPassword varchar(64) COMMENT '用户密码', createTime datetime COMMENT '创建时间', updateTime datetime COMMENT '更新时间', PRIMARY KEY (id) ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE user_02 ( id bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID', userId varchar(9) COMMENT '用户ID', userNickName varchar(32) COMMENT '用户昵称', userHead varchar(16) COMMENT '用户头像', userPassword varchar(64) COMMENT '用户密码', createTime datetime COMMENT '创建时间', updateTime datetime COMMENT '更新时间', PRIMARY KEY (id) ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE user_03 ( id bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID', userId varchar(9) COMMENT '用户ID', userNickName varchar(32) COMMENT '用户昵称', userHead varchar(16) COMMENT '用户头像', userPassword varchar(64) COMMENT '用户密码', createTime datetime COMMENT '创建时间', updateTime datetime COMMENT '更新时间', PRIMARY KEY (id) ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE user_04 ( id bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID', userId varchar(9) COMMENT '用户ID', userNickName varchar(32) COMMENT '用户昵称', userHead varchar(16) COMMENT '用户头像', userPassword varchar(64) COMMENT '用户密码', createTime datetime COMMENT '创建时间', updateTime datetime COMMENT '更新时间', PRIMARY KEY (id) ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

- 创建配置文件

```yaml
# 路由配置
router:
  jdbc:
    datasource:
      dbCount: 2
      tbCount: 4
      list: db01,db02
      db01:
        driver-class-name: com.mysql.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/db01?useUnicode=true
        username: root
        password: yang19960127
      db02:
        driver-class-name: com.mysql.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/db02?useUnicode=true
        username: root
        password: yang19960127

mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath:mapper/*.xml
```

- 创建实体类

需要继承***DBRouterBase***

```java
package com.yangzinan.entity;

import com.yangzinan.DBRouterBase;

import java.util.Date;

public class User extends ***DBRouterBase*** {

    private Long id;
    private String userId;          // 用户ID
    private String userNickName;    // 昵称
    private String userHead;        // 头像
    private String userPassword;    // 密码
    private Date createTime;        // 创建时间
    private Date updateTime;        // 更新时间

    public User() {
    }

    public User(String userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserNickName() {
        return userNickName;
    }

    public void setUserNickName(String userNickName) {
        this.userNickName = userNickName;
    }

    public String getUserHead() {
        return userHead;
    }

    public void setUserHead(String userHead) {
        this.userHead = userHead;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

}
```

- Dao

```java
@Mapper
public interface IUserDao {

    @DBRouter(key = "userId")
    User queryUserInfoByUserId(User req);

    @DBRouter(key = "userId")
    void insertUser(User req);

}
```

- mapper

```java
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.yangzinan.mapper.IUserDao">

    <select id="queryUserInfoByUserId" parameterType="com.yangzinan.entity.User"
            resultType="com.yangzinan.entity.User">
        SELECT id, userId, userNickName, userHead, userPassword, createTime
        FROM user_**${tbIdx}**
        where userId = #{userId}
    </select>

    <insert id="insertUser" parameterType="com.yangzinan.entity.User">
        insert into user_**${tbIdx}** (id, userId, userNickName, userHead, userPassword,createTime, updateTime)
        values (#{id},#{userId},#{userNickName},#{userHead},#{userPassword},now(),now())
    </insert>

</mapper>
```

- 启动类

```java
package com.yangzinan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication
@MapperScan(basePackages = "com.yangzinan.mapper")
public class SpringDbRouterTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringDbRouterTestApplication.class, args);
    }

}
```

- 测试 类

```java
package com.yangzinan;

import com.yangzinan.entity.User;
import com.yangzinan.mapper.IUserDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
class SpringDbRouterTestApplicationTests {

    @Autowired
    private IUserDao userDao;

    @Test
    void contextLoads() {
        User user = new User();
        user.setId(3L);
        user.setUserId("100");
        user.setUserHead("hello");
        user.setUserNickName("mic");
        user.setUserPassword("1234");

        userDao.insertUser(user);
    }

}
```

- 最终结果

```java
数据库路由 method：insertUser dbIdx：1 tbIdx：1

SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@4bdb04c8] was not registered for synchronization because synchronization is not active
JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@795faad] will not be managed by Spring
==>  Preparing: insert into user_01 (id, userId, userNickName, userHead, userPassword,createTime, updateTime) values (?,?,?,?,?,now(),now())
==> Parameters: 3(Long), 100(String), mic(String), hello(String), 1234(String)
<==    Updates: 1
```

我们做一下总结，在测试中我们可以看到，数据库路由是由DynamicDataSource帮助我们完成，但是数据表的路由还需要我们在sql语句中完成路由

再会！
