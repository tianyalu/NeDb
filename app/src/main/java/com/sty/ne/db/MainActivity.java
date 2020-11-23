package com.sty.ne.db;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.sty.ne.db.db.BaseDao;
import com.sty.ne.db.db.BaseDaoFactory;
import com.sty.ne.db.db.OrderDao;
import com.sty.ne.db.model.User;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button btnInsert;
    private Button btnSelect;
    private Button btnUpdate;
    private Button btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        addListeners();
    }

    private void addListeners() {
        btnInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseDao baseDao = BaseDaoFactory.getInstance().getBaseDao(BaseDao.class, User.class);
                //OrderDao orderDao = BaseDaoFactory.getInstance().getBaseDao(OrderDao.class, User.class);

                baseDao.insert(new User(1, "sty1", "21212"));
                baseDao.insert(new User(2, "sty2", "111"));
                baseDao.insert(new User(3, "sty3", "21212"));
                baseDao.insert(new User(4, "sty4", "1111"));
                baseDao.insert(new User(5, "sty5", "21212"));
                baseDao.insert(new User(6, "sty6", "111"));
            }
        });

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseDao baseDao = BaseDaoFactory.getInstance().getBaseDao(BaseDao.class, User.class);
                User where = new User();
                //where.setPwd("111");
                List<User> list = baseDao.query(where);
                Log.e("sty", "list size is " + list.size());
                for (int i = 0; i < list.size(); i++) {
                    System.out.println(list.get(i).toString());
                }
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseDao baseDao = BaseDaoFactory.getInstance().getBaseDao(BaseDao.class, User.class);
                User user = new User(2, "xxxxxx", "abcdefg");

                User where = new User();
                where.setId(2);
                baseDao.update(user, where);
                Toast.makeText(MainActivity.this, "执行成功", Toast.LENGTH_SHORT).show();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseDao baseDao = BaseDaoFactory.getInstance().getBaseDao(BaseDao.class, User.class);
                User where = new User();
                where.setName("sty6");
                baseDao.delete(where);
            }
        });
    }

    /**
     * 如何自动创建数据库
     * 如何自动创建数据表
     * 如何让用户在使用的时候非常方便
     * 将User对象里面的类名、属性转换为创建数据库表的sql语句：create table user(id integer, name text, pwd text);
     */
    private void initView() {
        btnInsert = findViewById(R.id.btn_insert);
        btnSelect = findViewById(R.id.btn_select);
        btnUpdate = findViewById(R.id.btn_update);
        btnDelete = findViewById(R.id.btn_delete);
    }
}