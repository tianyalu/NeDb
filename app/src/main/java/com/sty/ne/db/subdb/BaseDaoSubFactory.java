package com.sty.ne.db.subdb;

import android.database.sqlite.SQLiteDatabase;

import com.sty.ne.db.db.BaseDao;
import com.sty.ne.db.db.BaseDaoFactory;

/**
 * @Author: tian
 * @UpdateDate: 2020/11/23 10:14 PM
 */
public class BaseDaoSubFactory extends BaseDaoFactory {
    //定义一个用来实现分库的数据库实现
    protected SQLiteDatabase subSqLiteDatabase;
    private static class LazyHolder {
        private static BaseDaoSubFactory instance = new BaseDaoSubFactory();
    }

    private BaseDaoSubFactory() {
        super();
    }

    public static BaseDaoSubFactory getInstance() {
        return LazyHolder.instance;
    }

    //生产BaseDao对象
    public <T extends BaseDao<M>, M> T getBaseDao(Class<T> daoClass, Class<M> entityClass) {
        BaseDao baseDao = map.get(PrivateDatabaseEnums.database.getValue());
        if(baseDao != null) {
            return (T) baseDao;
        }
        subSqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(PrivateDatabaseEnums.database.getValue(), null);
        try {
            //baseDao = BaseDao.class.newInstance();
            baseDao = daoClass.newInstance();
            baseDao.init(subSqLiteDatabase, entityClass);
            map.put(PrivateDatabaseEnums.database.getValue(), baseDao);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return (T) baseDao;
    }
}
