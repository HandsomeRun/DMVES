# 分布式多车探索

## 构建

1. 控制器
2. 小车系统
3. 导航器
4. 回放器
5. View / UI

## 数据结构

### Redis

```javascript
{
	// 地图
	mapHeight : String ,
	mapWidth :  String ,
	mapBarrier : String ,  // 障碍地图 0 可 1 障碍
	mapExplore : String ,  // 探索地图 0 未 1 探索
	
	// 小车集合 Json 序列化 存到
	cars : {
		car[Id] : {
			carId : string ,
			carState : CAR_STATE , 
			carPosition : Point , 
			carPath : String 
		}
	}
	
	// 心跳
	CarSystem : String , // CarSystem : 123154565
	Navigator : String ,
	View : String , 
	Controller : String 
	
}
```

### 基础数据类型

#### 系统日志

```javascript
SystemLogs : {
	SystemLog : {
		Type 		: USER | SYSTEM
		Content 	:
		User		:
		TimeStemp 	:
	}
}
```

#### 多车探索日志

```javascript
// 多车探索日志 C2风格，使用 MQ 处理分布式问题，小车完成移动后记录日志
carRunLog[yyyy-mm-dd-hh:mm:ss] : 
{
	config : {
		// 地图
		mapHeight : String ,
		mapWidth :  String ,
		mapBarrier : String ,  // 障碍地图 0 可 1 障碍
		
		// 小车 
		cars : {
			car[id] : {} 
		}
	} , 
	
	runLog : {
		{
			Type   		: Move | Navigate ,
			carId  		: int ,
			Data  		: String ,  // 路径或者移动方向
			TimeStemp 	: Long
		}
	}
}
```

#### 用户

```javascript
User : {
	id : String , 
	userName : String , 
	userPassWord : String , // MD5 加密
	userRole : UserRoleEnum 
}
```

#### 用户级别

```javascript
UserRoleEnum : {
	REGULAR_USER , 
	CONFIGURATOR , 
	ADMIN
}
```

#### 小车

```javascript
{
	carId : string ,
	carStatus : CarStatusEnum , 
	carPosition : Point , 
	carPath : String , // U D L R
	carAlgorithm : CarAlgorithmEnum ,
	carWaitCnt : int , // 小车等待时间，仅在 carStatus 为 WAITNG 时起作用，减到 0 时表示等待结束，仅在 Controller 中被改变
	carColor : String | Color ,
    carStatusTime : Long 
}
```

#### 小车状态

```javascript
CarStatusEnum : {
	FREE ,       // 空闲
	RUNNING ,    // 运行
	REQUESTING , // 请求导航中 = 导航中
	AVOIDING   , // 避障中(需要重新请求导航)
	WAITING    , // 遇到小车等待中
}
```

#### 小车移动算法

```javascript
{
	ASTART , // A*
	BFS  //  BFS
}
```

### 消息格式

#### 心跳消息

**构件存活了就周期发（100ms/send)**

每一种构建在 Redis 当中存在一个键值对 `<Name , TimeStemp>` 来标识存活时间，以下为举例。

```javascript
{
	CarSystem : String , // CarSystem : 123154565
	Navigator : String ,
	View : String , 
	Controller : String ,
}
```

#### 实验始终

**前台定时读 Redis 中的 isWork**

1. 实验开始 前台 向 Redis 发 `isWork = 运行中`

2. 实验结束 导航器 向 Redis 发 `isWork = 已完成`

3. 实验过程中 Controller 向 Redis 发 `isWork = 故障` , 且此时 前台 从 Redis 中的 `ErrorData` 读取故障原因。

4. 实验暂停 前台 向 Redis 发 `isWork = 未运行`

isWork 的所有状态：
- 未运行
- 运行中
- 故障
- 已完成

#### Controller 发 请求导航消息

```javascript
1_NavigatorQueue
{
	car[Id] : int
}
```

#### Controller 发 小车移动消息

```javascript
1_CarMoveQueue
{
	car[Id] : int
}
```

#### 导航器和小车系统 发 View 更新消息

```javascript
1_UpdateView
{
	car[Id] : int
}
```

#### 导航器和小车系统 发 探索日志 消息

**(100ms转存一次)**

```javascript
1_RunLogQueue
{
	Type   		: Move | Navigate ,
	carId  		: int ,
	Data  		: String ,  // 路径或者移动方向
	TimeStemp 	: Long
}
```

### 关系数据库

#### 用户

User

|字段名|数据类型|说明|
|:-:|:-:|:-:|
|userId| int |自增变量（从 10001 开始）|
|userName|varchar(50)|用户名|
|userPassWord|varchar(50)|MD5加密|
|userRole| 外键 |用户级别表|

UserRoleEnum 

|字段名|数据类型|说明|
|:-:|:-:|:-:|
|用户角色|REGULAR_USER , CONFIGURATOR , ADMIN| 用户角色=级别|

#### 探索日志

ExploreLog

|字段名|数据类型|说明|
|:-:|:-:|:-:|
|userID| int | 外键|
|exporeTime| DateTime | 探索时间|
|exporeName| varChar(50) | 探索日志名|
|exporePath| varChar(50) | 探索日志 txt 绝对路径名 |

#### 系统日志

SystemLog

|字段名|数据类型|说明|
|:-:|:-:|:-:|
|timeStemp|  | 时间戳 |
|type | "USER, SYSTEM" | 类型 |
|userID| int | 外键 |
|content| varChar(2000) | 日志内容 |
