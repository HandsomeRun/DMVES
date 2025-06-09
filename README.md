# 分布式多车探索

## 构建

1. 探索子系统

	- 控制器 
	- 导航器
	- 小车
	- View （）
	- 目标器

2. 探索日志

3. 回放子系统
	
	流程:UI在Redis给playbackTime,回放器周期监听playbackTime,向Redis加载相应的帧信息,加载完成后给view发送update
	- 回放器
	- View（）

4. 用户子系统

	- 登陆
	- 注册
	- 重置密码
	- 修改用户权限
	- 写系统日志

5. 配置子系统

	- 地图大小
	- 小车配置
	- 障碍物配置
	- 写系统日志

6. 分析子系统
	
	- Car的路径生成时间(折线图  横坐标:导航次数  纵坐标:导航时间)
    - 算法的比较(柱状图 横坐标:算法类型  纵坐标:算法平均时间)
    - 不同实验的运行试验比较(配置信息+实验运行时间 只能选择两个实验之间的比较)

### 任务分配

农宇晨 ： UI + View + 前端业务逻辑（包含系统日志）

钱卓敏 : Controller + 回放器 + 探索日志

常润   : 目标器 + 导航器

张城祥 : 小车 + 启动脚本

### 避障

运动后检测下一步

## 数据结构

### Redis

```javascript
{
	//实验信息
	isWork 详见“实验始终部分”	
	errorData 故障详情

	// 地图
	mapHeight : String ,
	mapWidth :  String ,
	mapBarrier : String ,  // 障碍地图 0 可 1 障碍
	mapExplore : String ,  // 探索地图 0 未 1 探索
	// 读： 目标器、导航器、小车、前段
	// 写： 小车
	
	carNum : String , // 小车数量，只增变量，小车由系统部署
	// Json 序列化
	car[Id] : {	}
	// 读： 
	
	// 断连小车集合，当控制器检测到一小车的心跳不新之后，会将其状态改为断连中，并将其 id 放到 disConnectCars 集合当中。当新增小车时，前端在 Redis 中创建小车实例，同时修改 carNum ，并将其 id 放到 disConnectCars 当中。当有空闲小车进程时，该进程反复读取 disConnectCars 集合，当存在僵尸小车时，从 disConnectCars 中取出小车，修改小车状态为 FREE 。
	disConnectCars : 集合
	
	// 心跳
	Navigator : String ,
	View : String , 
	Controller : String ,
	Target : String 
	
	//回放时间
	playbackTime : String
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
	information : {
		//实验时长
		expDuration : long
	
		// 地图	
		mapHeight : int ,
		mapWidth :  int ,
		mapBarrier : int[][] ,  // 障碍地图 0 可 1 障碍
		
		// 小车 
		cars : {
			car[id] : {} 
		}
	} , 
	
	runLog : {
		{
			timeStamp : long
			mapExplore : String
			cars : {
				car[id] : {} 
			}
		}
	}
	
	analysisLog : {
		{
			carId : int
			carAlgorithm : CarAlgorithmEnum
			navTime : long
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
	REGULAR_USER ,  // 只能看到实验进行
	CONFIGURATOR , 	// 能配置也能看到实验运行 
	ADMIN		    // 只能修改用户的密码
}
```

#### 小车

```javascript
{
	carId : string ,
	carStatus : CarStatusEnum , 
	carPosition : Point ,
	carTarget : Point , // 小车目标 
	carPath : String , // U D L R
	carAlgorithm : CarAlgorithmEnum ,
	carStatusCnt : int , // 小车状态保持剩余周期数，减为 0 时，回退到上一状态，在小车进程中检测 
	carColor : String | Color ,
	carLastRunTime : Long  // 小车心跳,控制器需要每个周期都检测
}

```

#### 小车状态

```javascript
CarStatusEnum : {
	DISCONNECTING,//断开连接中
	FREE ,       // 空闲
	RUNNING ,    // 运行
	SEARCHING ,	 // 寻找目标中
	WAIT_NAV , // 待导航
	NAVIGATING , // 导航中
	WAITING     // 遇到小车等待中
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

2. 实验结束 目标起 向 Redis 发 `isWork = 已完成`

3. 实验过程中 Controller 向 Redis 发 `isWork = 故障` , 且此时 前台 从 Redis 中的 `errorData` 读取故障原因。

4. 实验暂停 前台 向 Redis 发 `isWork = 未运行`

isWork 的所有状态：
- 未运行
- 运行中
- 故障
- 已完成

#### Controller 发 请求终点消息

```
1_TargetQueue
{
	car[Id] : int
}
```

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

|      字段名      |    数据类型     |        说明        |
|:-------------:|:-----------:|:----------------:|
|    userId     |     int     | 自增变量（从 10001 开始） |
|   userName    | varchar(50) |       用户名        |
| userPassWord  | varchar(50) |      MD5加密       |
|   userRole    |     外键      |      用户级别表       |
|  userStatus   |   char(3)   |   待审核，已通过，未通过    |
| registerTime  |  DateTime   |       注册时间       |
|  examineTime  |  DateTime   |    审核时间，可null    |
| examineUserId |     外键      |   审核人Id，可null    |

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
