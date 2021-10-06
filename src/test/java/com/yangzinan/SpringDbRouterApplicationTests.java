package com.yangzinan;

import com.yangzinan.annotation.DBRouter;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;


class SpringDbRouterApplicationTests {

    public static void main(String[] args) throws NoSuchMethodException {
        test_annotation();
    }

    public static void test_annotation() throws NoSuchMethodException {
        Class<IUserDao> iUserDaoClass = IUserDao.class;
        Method method = iUserDaoClass.getMethod("insertUser", String.class);
        DBRouter dbRouter = method.getAnnotation(DBRouter.class);
        System.out.println(dbRouter.key());
    }

}
