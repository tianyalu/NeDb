package com.sty.ne.db.db;

import android.database.sqlite.SQLiteDatabase;

/**
 * 创建数据库文件，初始化数据库
 * @Author: tian
 * @UpdateDate: 2020/11/19 10:32 PM
 */
public class BaseDaoFactory {
    private SQLiteDatabase sqLiteDatabase;
    private String sqLitePath;
    private static class LazyHolder {
        private static BaseDaoFactory instance = new BaseDaoFactory();
    }

    private BaseDaoFactory() {
        sqLitePath = "data/data/com.sty.ne.db/ne.db";
        sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(sqLitePath, null);
    }

    public static BaseDaoFactory getInstance() {
        return LazyHolder.instance;
    }

    //生产BaseDao对象
    public <T> BaseDao<T> getBaseDao(Class<T> entityClass) {
        BaseDao baseDao = null;
        try {
            baseDao = BaseDao.class.newInstance();
            baseDao.init(sqLiteDatabase, entityClass);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return baseDao;
    }
}
