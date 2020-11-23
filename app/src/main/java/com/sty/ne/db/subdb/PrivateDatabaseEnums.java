package com.sty.ne.db.subdb;

import com.sty.ne.db.db.BaseDaoFactory;
import com.sty.ne.db.db.UserDao;
import com.sty.ne.db.model.User;

import java.io.File;

/**
 * @Author: tian
 * @UpdateDate: 2020/11/23 10:22 PM
 */
public enum PrivateDatabaseEnums {
    database("");

    private String value;
    PrivateDatabaseEnums(String value) {

    }

    public String getValue() {
        UserDao userDao = BaseDaoFactory.getInstance().getBaseDao(UserDao.class, User.class);
        if(userDao != null) {
            User currentUser = userDao.getCurrentUser();
            if(currentUser != null) {
                File file = new File("data/data/com.sty.ne.db/");
                if(!file.exists()) {
                    file.mkdirs();
                }
                return file.getAbsolutePath() + "/u_" + currentUser.getId() + "_private.db";
            }
        }
        return "";
    }
}
