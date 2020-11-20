# Android数据库注解封装

[TOC]

## 一、概念

### 1.1 数据库的作用

在`Android`开发中，数据库在小中型`app`中使用并不广泛，但其对应数据管理的方便性是其它数据存储工具无法取代的。

数据库特定：**数据集中管理，控制冗余，提高数据的利用率和一致性，又利用应用程序的开发和维护**。

### 1.2 数据库设计三大范式

* 第一范式：原子性（省市区尽量拆分）；
* 第二范式：唯一性（`ID`）；
* 第三范式：避免冗余性（避免在不同的表中设计冗余的字段）。

### 1.3 `Web`数据库与`Android`数据库

* **`Web数据库`**：该类型数据库在设计的时候基于三范式，由于`Web`数据库面对的是广大网络用户，所以安全性需要特别注意（`SQL`注入）；
* **`Android数据库`**：同样基于三范式，但它的安全性在大部分情况下不需要像`Web`一样考虑特别严谨。

## 二、实践

本文通过使用注解的方式对`Android SQLite`数据库进行封装，使其按照面向对象的方式进行使用，更加简便易用。

### 2.1 定义注解

表名注解: `DbTable`

```java
//用来控制表名叫什么
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbTable {
    String value();
}
```

列名注解：`DbField`

```java
//用来控制列名叫什么
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbField {
    String value();
}
```

### 2.2 使用注解

定义实体类：`User`

```java
//得到User对应表名
@DbTable("tb_user")
public class User {
    //得到User对象对应列名
    @DbField("u_id")
    private Integer id;
    private String name;
    private String pwd;

    public User(Integer id, String name, String pwd) {
        this.id = id;
        this.name = name;
        this.pwd = pwd;
    }
		//...省略getter和setter方法
}
```

### 2.3 定义`DAO(Database Access Object)`

#### 2.3.1 `DAO`接口：`IBaseDao`

```java
public interface IBaseDao<T> {
    long insert(T entity);
}
```

#### 2.3.2 对象注解解析以及生成`SQL`语句：`BaseDao`

```java
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
        return null;
    }

    private Map<String, String> getValues(T entity) {
        HashMap<String, String> map = new HashMap<>();
        //得到所有的成员变量，user的成员变量
        Iterator<Field> fieldIterator = cacheMap.values().iterator();
        while (fieldIterator.hasNext()) {
            Field field = fieldIterator.next();
            field.setAccessible(true);
            //获取成员变量的值
            try {
                Object object = field.get(entity);
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
}
```

#### 2.3.3 创建并初始化数据库：`BaseDaoFactory`

```java
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
```

#### 2.3.4 使用

```java
BaseDao baseDao = BaseDaoFactory.getInstance().getBaseDao(User.class);
baseDao.insert(new User(1, "sty", "21212"));
```

