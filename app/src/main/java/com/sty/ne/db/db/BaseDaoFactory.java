package com.sty.ne.db.db;

import android.database.sqlite.SQLiteDatabase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    //设计一个数据库连接池，new 一个容器，只创建一次，下次就不会再创建了，考虑多线程问题
    protected Map<String, BaseDao> map = Collections.synchronizedMap(new HashMap<String, BaseDao>());

    protected BaseDaoFactory() {
        sqLitePath = "data/data/com.sty.ne.db/ne.db";
        sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(sqLitePath, null);
    }

    public static BaseDaoFactory getInstance() {
        return LazyHolder.instance;
    }

    //生产BaseDao对象
    public <T extends BaseDao<M>, M> T getBaseDao(Class<T> daoClass, Class<M> entityClass) {
        BaseDao baseDao = map.get(daoClass.getSimpleName());
        if(baseDao != null) {
            return (T) baseDao;
        }
        try {
            //baseDao = BaseDao.class.newInstance();
            baseDao = daoClass.newInstance();
            baseDao.init(sqLiteDatabase, entityClass);
            map.put(daoClass.getSimpleName(), baseDao);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return (T) baseDao;
    }
}
