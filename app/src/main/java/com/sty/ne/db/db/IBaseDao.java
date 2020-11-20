package com.sty.ne.db.db;

/**
 * @Author: tian
 * @UpdateDate: 2020/11/19 9:40 PM
 */
public interface IBaseDao<T> {

    long insert(T entity);
}
