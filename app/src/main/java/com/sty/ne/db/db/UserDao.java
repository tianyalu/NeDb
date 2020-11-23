package com.sty.ne.db.db;

import android.util.Log;

import com.sty.ne.db.model.User;

import java.util.List;

/**
 * 维护用户的公有数据
 * @Author: tian
 * @UpdateDate: 2020/11/23 9:59 PM
 */
public class UserDao extends BaseDao<User> {
    private static final String TAG = UserDao.class.getSimpleName();

    @Override
    public long insert(User entity) {
        //查询该表中所有的用户记录
        List<User> list = query(new User());
        User where;
        for (User user : list) {
            where = new User();
            where.setId(user.getId());
            where.setStatus(0);
            update(user, where);
            Log.e(TAG, "用户 " + user.getName() + " 更改为未登录状态");
        }
        entity.setStatus(1);
        Log.e(TAG, "用户 " + entity.getName() + " 登录");
        return super.insert(entity);
    }

    //获取当前登录的User
    public User getCurrentUser() {
        User user = new User();
        user.setStatus(1);
        List<User> list = query(user);
        if(list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }
}
