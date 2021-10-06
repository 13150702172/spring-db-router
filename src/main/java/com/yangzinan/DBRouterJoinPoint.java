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

        if (StringUtils.isBlank(dbKey)) throw new RuntimeException("annotation DBRouter key is null！");
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
