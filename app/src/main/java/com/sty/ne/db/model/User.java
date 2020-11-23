package com.sty.ne.db.model;

import com.sty.ne.db.annotation.DbField;
import com.sty.ne.db.annotation.DbTable;

/**
 * @Author: tian
 * @UpdateDate: 2020/11/19 9:30 PM
 */
//得到User对应表名
@DbTable("tb_user")
public class User {
    //得到User对象对应列名
    @DbField("u_id")
    private Integer id;
    private String name;
    private String pwd;

    public User() {

    }

    public User(Integer id, String name, String pwd) {
        this.id = id;
        this.name = name;
        this.pwd = pwd;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", pwd='" + pwd + '\'' +
                '}';
    }
}
