package com.yangzinan;

import com.yangzinan.annotation.DBRouter;

public interface IUserDao {

    @DBRouter(key = "userId")
    public int insertUser(String req);
}
