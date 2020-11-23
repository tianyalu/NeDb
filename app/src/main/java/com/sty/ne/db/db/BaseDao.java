package com.sty.ne.db.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.sty.ne.db.annotation.DbField;
import com.sty.ne.db.annotation.DbTable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对象注解解析以及生成SQL语句
 * @Author: tian
 * @UpdateDate: 2020/11/19 9:41 PM
 */
public class BaseDao<T> implements IBaseDao<T> {
    // 持有数据库操作的引用
    private SQLiteDatabase sqLiteDatabase;
    //表名
    private String tableName;
    //操作数据库所对应的JAVA类型
    private Class<T> entityClass;
    //标识，用来标识是否已经做过初始化
    private boolean isInit = false;
    //定义一个缓存空间（key:字段名 value:成员变量）
    private HashMap<String, Field> cacheMap;

    protected boolean init(SQLiteDatabase sqLiteDatabase, Class<T> entityClass) {
        this.sqLiteDatabase = sqLiteDatabase;
        this.entityClass = entityClass;
        if(!isInit) {
            //根据传入的Class进行数据表的创建，本例子中对应的是User对象
            DbTable dt = entityClass.getAnnotation(DbTable.class);
            if (dt != null && !"".equals(dt.value())) {
                tableName = dt.value();
            } else {
                tableName = entityClass.getName();
            }

            if(!sqLiteDatabase.isOpen()) {
                return false;
            }
            String createTableSql = getCreateTableSql();
            sqLiteDatabase.execSQL(createTableSql);
            cacheMap = new HashMap<>();
            initCacheMap();
            isInit = true;
        }
        return  isInit;
    }

    private void initCacheMap() {
        //取得所有的列名
        String sql = "select * from " + tableName + " limit 1,0"; //从第一个数据开始取0条数据-->得到表结构
        Cursor cursor = sqLiteDatabase.rawQuery(sql, null);
        String[] columnNames = cursor.getColumnNames();
        //获取所有的成员变量
        Field[] columnFields = entityClass.getDeclaredFields();
        //将字段访问权限打开
        for (Field field : columnFields) {
            field.setAccessible(true);
        }
        for (String columnName : columnNames) {
            Field columnField = null;
            for (Field field : columnFields) {
                String fieldName = field.getName();
                if(field.getAnnotation(DbField.class) != null) {
                    fieldName = field.getAnnotation(DbField.class).value();
                }
                if(columnName.equals(fieldName)) {
                    columnField = field;
                    break;
                }
            }
            if(columnField != null) {
                cacheMap.put(columnName, columnField);
            }
        }

    }

    private String getCreateTableSql() {
        StringBuffer sb = new StringBuffer();
        sb.append("create table if not exists ");
        sb.append(tableName + "(");
        //反射得到所有的成员变量
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            Class<?> type = field.getType();
            DbField dbField = field.getAnnotation(DbField.class);
            String columnName = field.getName();
            if(dbField != null && !"".equals(dbField.value())) {
               columnName = dbField.value();
            }

            if (type == String.class) {
                sb.append(columnName + " TEXT,");
            } else if (type == Integer.class) {
                sb.append(columnName + " INTEGER,");
            } else if (type == Long.class) {
                sb.append(columnName + " BIGINT,");
            } else if (type == Double.class) {
                sb.append(columnName + " DOUBLE,");
            } else if (type == byte[].class) {
                sb.append(columnName + " BLOB,");
            } else {
                //不支持的数据类型
                continue;
            }
        }

        if(sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(")");

        return sb.toString();
    }

    @Override
    public long insert(T entity) {
        //user对象，转换为ContentValues  new User(1, "sty", "123")
        Map<String, String> map = getValues(entity);
        ContentValues values = getContentValues(map);
        return sqLiteDatabase.insert(tableName, null, values);
    }

    @Override
    public long update(T entity, T where) {
        //将传进来的对象的成员变量和其值转为map
        Map map = getValues(entity);
        ContentValues values = getContentValues(map);

        Map whereMap = getValues(where);
        Condition condition = new Condition(whereMap);

        return sqLiteDatabase.update(tableName, values, condition.whereCause, condition.whereArgs);
    }

    @Override
    public int delete(T where) {
        Map map = getValues(where);
        Condition condition = new Condition(map);

        return sqLiteDatabase.delete(tableName, condition.whereCause, condition.whereArgs);
    }

    @Override
    public List<T> query(T where) {
        return query(where, null, null, null);
    }

    @Override
    public List<T> query(T where, String orderBy, Integer startIndex, Integer limit) {
        Map map = getValues(where);
        // select * from tableName limit 0,10;
        String limitString = null;
        if(startIndex != null && limit !=null) {
            limitString = startIndex + " , " + limit;
        }

        //select * from tableName where id=? and name=? ...
        //String selections = "id=? and name=? ..."
        //String selectionArgs = String[]{ "1", "sty", ...}
        Condition condition = new Condition(map);
        Cursor cursor = sqLiteDatabase.query(tableName, null, condition.whereCause,
                condition.whereArgs, null, null, orderBy, limitString);

        //定义解析游标的方法
        List<T> result = getResult(cursor, where);

        return result;
    }

    private List<T> getResult(Cursor cursor, T obj) {
        ArrayList list = new ArrayList();
        Object item = null;  //User user = null;
        while (cursor.moveToNext()) {
            try {
                item = obj.getClass().newInstance(); //user = new User();
                Iterator<String> iterator = cacheMap.keySet().iterator();
                while (iterator.hasNext()) {
                    //获取列名
                    String columnName = iterator.next();
                    //以列名拿到列名在游标中的位置
                    int columnIndex = cursor.getColumnIndex(columnName);
                    //获取成员变量的类型
                    Field field = cacheMap.get(columnName);
                    Class type = field.getType();
                    //cursor.getString(columnIndex);
                    if(columnIndex != -1) {
                        if(type == String.class) {
                            //User user = new User();
                            //user.setId(1); --> id.set(user, 1);
                            field.set(item, cursor.getString(columnIndex));
                        }else if(type == Double.class) {
                            field.set(item, cursor.getDouble(columnIndex));
                        }else if(type == Integer.class) {
                            field.set(item, cursor.getInt(columnIndex));
                        }else if(type == Long.class) {
                            field.set(item, cursor.getLong(columnIndex));
                        }else if(type == byte[].class) {
                            field.set(item, cursor.getBlob(columnIndex));
                        }
                    }
                }
                list.add(item);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
        cursor.close();
        return list;
    }

    private ContentValues getContentValues(Map<String, String> map) {
        ContentValues contentValues = new ContentValues();
        Set keys = map.keySet();
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String value = map.get(key);
            if(value != null) {
                contentValues.put(key, value);
            }
        }
        return contentValues;
    }

    /**
     * 把传入对象的属性解析为Map
     * @param entity
     * @return
     */
    private Map<String, String> getValues(T entity) {
        HashMap<String, String> map = new HashMap<>();
        //得到所有的成员变量，user的成员变量
        Iterator<Field> fieldIterator = cacheMap.values().iterator();
        while (fieldIterator.hasNext()) {
            Field field = fieldIterator.next();
            field.setAccessible(true);
            //获取成员变量的值
            try {
                Object object = field.get(entity); //user.getName() --> field.get(user);
                if(object == null) {
                    continue;
                }
                String value = object.toString();
                //获取列名
                DbField dbField = field.getAnnotation(DbField.class);
                String key = field.getName();
                if(dbField != null && !"".equals(dbField.value())) {
                    key = dbField.value();
                }
                if(!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    map.put(key, value);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    private class Condition {
        private String whereCause;
        private String[] whereArgs;

        public Condition(Map<String, String> whereMap) {
            ArrayList list = new ArrayList();
            StringBuilder sb = new StringBuilder();
            sb.append("1=1");
            //获取所有的字段名
            Set<String> keys = whereMap.keySet();
            Iterator<String> iterator = keys.iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = whereMap.get(key);
                if(value != null) {
                    sb.append(" and ")
                            .append(key)
                            .append(" =?");
                    list.add(value);
                }
            }
            this.whereCause = sb.toString();
            this.whereArgs = (String[]) list.toArray(new String[list.size()]);
        }
    }
}
