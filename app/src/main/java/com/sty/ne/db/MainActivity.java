package com.sty.ne.db;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.sty.ne.db.db.BaseDao;
import com.sty.ne.db.db.BaseDaoFactory;
import com.sty.ne.db.model.User;

public class MainActivity extends AppCompatActivity {
    private Button btnInsert;

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
                BaseDao baseDao = BaseDaoFactory.getInstance().getBaseDao(User.class);
                baseDao.insert(new User(1, "sty", "21212"));
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
    }
}