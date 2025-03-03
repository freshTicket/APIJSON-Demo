/*Copyright ©2016 TommyLemon(https://github.com/TommyLemon/APIJSON)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package apijson.demo;

import static apijson.framework.APIJSONConstant.ID;
import static apijson.framework.APIJSONConstant.PRIVACY_;
import static apijson.framework.APIJSONConstant.USER_;
import static apijson.framework.APIJSONConstant.USER_ID;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;

import apijson.RequestMethod;
import apijson.StringUtil;
import apijson.column.ColumnUtil;
import apijson.framework.APIJSONSQLConfig;
import apijson.orm.AbstractSQLConfig;


/**SQL 配置
 * TiDB 用法和 MySQL 一致
 * @author Lemon
 */
public class DemoSQLConfig extends APIJSONSQLConfig {

	public DemoSQLConfig() {
		super();
	}
	public DemoSQLConfig(RequestMethod method, String table) {
		super(method, table);
	}

	static {
		DEFAULT_DATABASE = DATABASE_MYSQL;  //TODO 默认数据库类型，改成你自己的。TiDB, MariaDB, OceanBase 这类兼容 MySQL 的可当做 MySQL 使用
		DEFAULT_SCHEMA = "apijson";  //TODO 默认模式名，改成你自己的，默认情况是 MySQL: sys, PostgreSQL: public, SQL Server: dbo, Oracle:

		//表名和数据库不一致的，需要配置映射关系。只使用 APIJSONORM 时才需要；
		//这个 Demo 用了 apijson-framework 且调用了 APIJSONApplication.init
		//(间接调用 DemoVerifier.init 方法读取数据库 Access 表来替代手动输入配置)，所以不需要。
		//但如果 Access 这张表的对外表名与数据库实际表名不一致，仍然需要这里注册。例如
		//		TABLE_KEY_MAP.put(Access.class.getSimpleName(), "access");

		//表名映射，隐藏真实表名，对安全要求很高的表可以这么做
		//		TABLE_KEY_MAP.put(User.class.getSimpleName(), "apijson_user");
		//		TABLE_KEY_MAP.put(Privacy.class.getSimpleName(), "apijson_privacy");

		//主键名映射
		SIMPLE_CALLBACK = new SimpleCallback() {

			@Override
			public AbstractSQLConfig getSQLConfig(RequestMethod method, String database, String schema, String table) {
				return new DemoSQLConfig(method, table);
			}

			//取消注释来实现自定义各个表的主键名
			//			@Override
			//			public String getIdKey(String database, String schema, String table) {
			//				return StringUtil.firstCase(table + "Id");  // userId, comemntId ...
			//				//		return StringUtil.toLowerCase(t) + "_id";  // user_id, comemnt_id ...
			//				//		return StringUtil.toUpperCase(t) + "_ID";  // USER_ID, COMMENT_ID ...
			//			}

			@Override
			public String getUserIdKey(String database, String schema, String table) {
				return USER_.equals(table) || PRIVACY_.equals(table) ? ID : USER_ID; // id / userId
			}

			//取消注释来实现数据库自增 id
			//			@Override
			//			public Object newId(RequestMethod method, String database, String schema, String table) {
			//				return null; // return null 则不生成 id，一般用于数据库自增 id
			//			}

			//			@Override
			//			public void onMissingKey4Combine(String name, JSONObject request, String combine, String item, String key) throws Exception {
			////				super.onMissingKey4Combine(name, request, combine, item, key);
			//			}
		};

		// 自定义原始 SQL 片段，其它功能满足不了时才用它，只有 RAW_MAP 配置了的 key 才允许前端传
		RAW_MAP.put("`to`.`id`", "");  // 空字符串 "" 表示用 key 的值 `to`.`id`
		RAW_MAP.put("to.momentId", "`to`.`momentId`");  // 最终以 `to`.`userId` 拼接 SQL，相比以上写法可以让前端写起来更简单
		RAW_MAP.put("(`Comment`.`userId`=`to`.`userId`)", "");  // 已经是一个条件表达式了，用 () 包裹是为了避免 JSON 中的 key 拼接在前面导致 SQL 出错
		RAW_MAP.put("sum(if(userId%2=0,1,0))", "");  // 超过单个函数的 SQL 表达式
		RAW_MAP.put("sumUserIdIsEven", "sum(if(`userId`%2=0,1,0)) AS sumUserIdIsEven");  // 简化前端传参
		RAW_MAP.put("substring_index(substring_index(content,',',1),',',-1)", "");  // APIAuto 不支持 '，可以用 Postman 测
		RAW_MAP.put("substring_index(substring_index(content,'.',1),'.',-1) AS subContent", "");  // APIAuto 不支持 '，可以用 Postman 测
		RAW_MAP.put("commentWhereItem1","(`Comment`.`userId` = 38710 AND `Comment`.`momentId` = 470)");
		RAW_MAP.put("to_days(now())-to_days(`date`)<=7","");  // 给 @having 使用


		// 取消注释支持 !key 反选字段 和 字段名映射，需要先依赖插件 https://github.com/APIJSON/apijson-column

		// 反选字段配置
		Map<String, List<String>> tableColumnMap = new HashMap<>();
		tableColumnMap.put("User", Arrays.asList(StringUtil.split("id,sex,name,tag,head,contactIdList,pictureList,date")));
		// 需要对应方法传参也是这样拼接才行，例如 ColumnUtil.compatInputColumn(column, getSQLDatabase() + "-" + getSQLSchema() + "-" + getTable(), getMethod());
		tableColumnMap.put("MYSQL-sys-Privacy", Arrays.asList(StringUtil.split("id,certified,phone,balance,_password,_payPassword")));
		ColumnUtil.VERSIONED_TABLE_COLUMN_MAP.put(null, tableColumnMap);

		// 字段名映射配置 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
		Map<String, Map<String, String>> tableKeyColumnMap = new HashMap<>();

		Map<String, String> userKeyColumnMap = new HashMap<>();
		userKeyColumnMap.put("gender", "sex");
		userKeyColumnMap.put("createTime", "date");
		tableKeyColumnMap.put("User", userKeyColumnMap);

		Map<String, String> privacyKeyColumnMap = new HashMap<>();
		privacyKeyColumnMap.put("rest", "balance");
		// 需要对应方法传参也是这样拼接才行，例如 ColumnUtil.compatInputKey(super.getKey(key), getSQLDatabase() + "-" + getSQLSchema() + "-" + getTable(), getMethod());
		tableKeyColumnMap.put("MYSQL-sys-Privacy", privacyKeyColumnMap);

		ColumnUtil.VERSIONED_KEY_COLUMN_MAP.put(null, tableKeyColumnMap);
		// 字段名映射配置 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

		ColumnUtil.init();
	}


	// 如果 DemoSQLExecutor.getConnection 能拿到连接池的有效 Connection，则这里不需要配置 dbVersion, dbUri, dbAccount, dbPassword

	@Override
	public String getDBVersion() {
		if (isMySQL()) {
			return "8.0.23"; //"8.0.11"; //TODO 改成你自己的 MySQL 或 PostgreSQL 数据库版本号 //MYSQL 8 和 7 使用的 JDBC 配置不一样
		}
		if (isPostgreSQL()) {
			return "9.6.15"; //TODO 改成你自己的
		}
		if (isSQLServer()) {
			return "2016"; //TODO 改成你自己的
		}
		if (isOracle()) {
			return "18c"; //TODO 改成你自己的
		}
		if (isDb2()) {
			return "11.5"; //TODO 改成你自己的
		}
		return null;
	}

	@JSONField(serialize = false)  // 不在日志打印 账号/密码 等敏感信息，用了 UnitAuto 则一定要加
	@Override
	public String getDBUri() {
		if (isMySQL()) {
			// 这个是 MySQL 8.0 及以上，要加 userSSL=false  return "jdbc:mysql://localhost:3306?userSSL=false&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=UTF-8";
			return "jdbc:mysql://192.168.45.128:3306?userSSL=false&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=UTF-8";
			// 以下是 MySQL 5.7 及以下
//			return "jdbc:mysql://localhost:3306?serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=UTF-8"; //TODO 改成你自己的，TiDB 可以当成 MySQL 使用，默认端口为 4000
		}
		if (isPostgreSQL()) {
			return "jdbc:postgresql://localhost:5432/postgres"; //TODO 改成你自己的
		}
		if (isSQLServer()) {
			return "jdbc:jtds:sqlserver://localhost:1433/pubs;instance=SQLEXPRESS"; //TODO 改成你自己的
		}
		if (isOracle()) {
			return "jdbc:oracle:thin:@localhost:1521:orcl"; //TODO 改成你自己的
		}
		if (isDb2()) {
			return "jdbc:db2://localhost:50000/BLUDB"; //TODO 改成你自己的
		}
		return null;
	}

	@JSONField(serialize = false)  // 不在日志打印 账号/密码 等敏感信息，用了 UnitAuto 则一定要加
	@Override
	public String getDBAccount() {
		if (isMySQL()) {
			return "root";  //TODO 改成你自己的
		}
		if (isPostgreSQL()) {
			return "postgres";  //TODO 改成你自己的
		}
		if (isSQLServer()) {
			return "sa";  //TODO 改成你自己的
		}
		if (isOracle()) {
			return "scott";  //TODO 改成你自己的
		}
		if (isDb2()) {
			return "db2admin"; //TODO 改成你自己的
		}
		return null;
	}

	@JSONField(serialize = false)  // 不在日志打印 账号/密码 等敏感信息，用了 UnitAuto 则一定要加
	@Override
	public String getDBPassword() {
		if (isMySQL()) {
			return "123456";  //TODO 改成你自己的，TiDB 可以当成 MySQL 使用， 默认密码为空字符串 ""
		}
		if (isPostgreSQL()) {
			return null;  //TODO 改成你自己的
		}
		if (isSQLServer()) {
			return "apijson@123";  //TODO 改成你自己的
		}
		if (isOracle()) {
			return "tiger";  //TODO 改成你自己的
		}
		if (isDb2()) {
			return "123"; //TODO 改成你自己的
		}
		return null;
	}

	//取消注释后，默认的 APIJSON 配置表会由业务表所在 数据库类型 database 和 数据库模式 schema 改为自定义的
	//	@Override
	//	public String getConfigDatabase() {
	//		return DATABASE_POSTGRESQL;
	//	}
	//	@Override
	//	public String getConfigSchema() {
	//		return "apijson";
	//	}

	//取消注释后，默认的数据库类型会由 MySQL 改为 PostgreSQL
	//	@Override
	//	public String getDatabase() {
	//		String db = super.getDatabase();
	//		return db == null ? DATABASE_POSTGRESQL : db;
	//	}

	//如果确定只用一种数据库，可以重写方法，这种数据库直接 return true，其它数据库直接 return false，来减少判断，提高性能
	//	@Override
	//	public boolean isMySQL() {
	//		return true;
	//	}
	//	@Override
	//	public boolean isPostgreSQL() {
	//		return false;
	//	}
	//	@Override
	//	public boolean isSQLServer() {
	//		return false;
	//	}
	//	@Override
	//	public boolean isOracle() {
	//		return false;
	//	}
	//	@Override
	//	public boolean isDb2() {
	//		return false;
	//	}


	// 取消注释支持 !key 反选字段 和 字段名映射，需要先依赖插件 https://github.com/APIJSON/apijson-column
	@Override
	public AbstractSQLConfig setColumn(List<String> column) {
		return super.setColumn(ColumnUtil.compatInputColumn(column, getTable(), getMethod()));
	}
	@Override
	public String getKey(String key) {
		return ColumnUtil.compatInputKey(super.getKey(key), getTable(), getMethod());
	}
}
