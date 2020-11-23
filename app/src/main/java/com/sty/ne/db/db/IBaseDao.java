package com.sty.ne.db.db;

import java.util.List;

/**
 * @Author: tian
 * @UpdateDate: 2020/11/19 9:40 PM
 */
public interface IBaseDao<T> {

    long insert(T entity);

    long update(T entity, T where);

    int delete(T where);

    List<T> query(T where);
    List<T> query(T where, String orderBy, Integer startIndex, Integer limit);
}
