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
        return contentValues;
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

### 2.4 分库

分库是针对类似`QQ`等多用户登录同一个`APP`的应用场景的，分库可以使数据更纯粹，更易于维护，尤其对于大型应用而言。

#### 2.4.1 实现思路

首先定义一张`user`表作为主表，记录用户登录状态，当某个用户登录后，修改其他用户的登录状态为“未登录”，该用户登录状态为“已登录”。随后以根据该用户的`id创建一个唯一的数据库文件（如果已存在则无需创建），之后的操作仅对该数据库进行。

#### 2.4.2 `UserDao`

```java
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
```

#### 2.4.3 `BaseDaoSubFactory`

```java
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
```

#### 2.4.4 使用

```java
btnInsertByDb.setOnClickListener(new View.OnClickListener() {
  @Override
  public void onClick(View v) {
    Photo photo = new Photo();
    photo.setPath("/data/data/xxx.jpg");
    photo.setTime(new Date().toString());
    PhotoDao photoDao = BaseDaoSubFactory.getInstance().getBaseDao(PhotoDao.class, Photo.class);
    photoDao.insert(photo);
  }
});
```

### 2.5 数据库升级

#### 2.5.1 升级思路

本地`sp`维护当前数据库版本号，如`V002`，每次登录从后台获取最新的版本号，如`V003`；当需要升级时，从后台下载数据库升级`xml`文件`updateXml.xml`；解析该`xml`文件，然后执行解析`xml`文件得到的`sql`语句：**备份数据库，创建新数据库，将备份表中的数据插入新表中，删除备份数据库**，完成升级。

#### 2.5.2 `updateXml.xml`

```xml
<!--要保证该文件一定是UTF-8编码-->
<updateXml>
    <updateStep
        versionFrom="V002"
        versionTo="V003">
        <updateDb>
            <!-- 对备份的表重新命名-->
            <sql_rename>alter table tb_photo rename to bak_tb_photo;</sql_rename>
            <!-- 创建一个新表 -->
            <sql_create>
                create table if not exists tb_photo(
                    time TEXT,
                    path TEXT,
                    name TEXT,
                    lastUpdateTime TEXT,
                    user_id Integer primary key
                );
            </sql_create>
            <!-- 将备份表中的数据插入到新表中 -->
            <sql_insert>
                insert into tb_photo(time, path) select time,path from bak_tb_photo;
            </sql_insert>
            <!-- 删除之前的备份表 -->
            <sql_delete>
                drop table if exists bak_tb_photo;
            </sql_delete>
        </updateDb>
    </updateStep>
</updateXml>
```

#### 2.5.3 更新数据库管理类`UpdateManager`

```java
public class UpdateManager {
    private static final String TAG = UpdateManager.class.getSimpleName();
    private List<User> userList;

    public void startUpdateDb(Context context) {
        UserDao userDao = BaseDaoFactory.getInstance().getBaseDao(UserDao.class, User.class);
        userList = userDao.query(new User());
        //解析xml文件
        UpdateXml updateXml = readDbXml(context);
        //拿到当前的版本信息
        UpdateStep updateStep = analyseUpdateStep(updateXml);
        if(updateStep == null) {
            return;
        }
        //获取更新的对象
        List<UpdateDb> updateDbs = updateStep.getUpdateDbs();
        for (User user : userList) {
            //得到每个用户的数据库对象
            SQLiteDatabase database = getDb(user.getId());
            if(database == null) {
                return;
            }
            for (UpdateDb updateDb : updateDbs) {
                String sql_rename = updateDb.getSql_rename();
                String sql_create = updateDb.getSql_create();
                String sql_insert = updateDb.getSql_insert();
                String sql_delete = updateDb.getSql_delete();

                String[] sqls = new String[] {sql_rename, sql_create, sql_insert, sql_delete};
                executeSql(database, sqls);
                Log.i(TAG, user.getId() + "用户数据库升级成功");
            }
        }
    }

    private void executeSql(SQLiteDatabase database, String[] sqls) {
        if(sqls == null || sqls.length == 0) {
            return;
        }
        //事务
        database.beginTransaction();
        for (String sql : sqls) {
            sql = sql.replace("\r\n", " ");
            sql = sql.replace("\n", " ");
            if(!"".equals(sql.trim())) {
                database.execSQL(sql);
            }
        }
        database.setTransactionSuccessful();;
        database.endTransaction();
    }

    private SQLiteDatabase getDb(Integer id) {
        SQLiteDatabase sqlDb = null;
        File file = new File("data/data/com.sty.ne.db/u_" + id + "_private.db");
        if(!file.exists()) {
            Log.e(TAG, file.getAbsolutePath() + "数据库不存在");
            return null;
        }
        return SQLiteDatabase.openOrCreateDatabase(file, null);
    }

    private UpdateStep analyseUpdateStep(UpdateXml updateXml) {
        UpdateStep thisStep = null;
        if(updateXml == null) {
            return null;
        }
        List<UpdateStep> steps = updateXml.getUpdateSteps();
        if(steps == null || steps.size() == 0) {
            return null;
        }
        for (UpdateStep step : steps) {
            if(step.getVersionFrom() == null || step.getVersionTo() == null) {
                //do nothing
            }else {
                String[] versionArray = step.getVersionFrom().split(",");
                if(versionArray != null && versionArray.length > 0) {
                    for (int i = 0; i < versionArray.length; i++) {
                        //数据保存在sp里面或者文本文件中，V002代表当前版本信息
                        //V003应该从服务器获取
                        if("V002".equalsIgnoreCase(versionArray[i]) &&
                            step.getVersionTo().equalsIgnoreCase("V003")) {
                            thisStep = step;
                            break;
                        }
                    }
                }
            }
        }
        return thisStep;
    }

    private UpdateXml readDbXml(Context context) {
        InputStream is = null;
        Document document = null;
        try {
            is = context.getAssets().open("updateXml.xml");
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.parse(is);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(document == null) {
                return null;
            }
        }

        UpdateXml xml = new UpdateXml(document);
        return xml;
    }
}
```

