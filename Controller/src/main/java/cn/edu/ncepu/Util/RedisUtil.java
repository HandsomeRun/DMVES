package cn.edu.ncepu.Util;

import cn.edu.ncepu.Model.Car;
import com.google.gson.Gson;
import com.sun.tools.javac.Main;
import redis.clients.jedis.Jedis;

import java.io.InputStream;
import java.util.*;

public class RedisUtil {
    private static final RedisUtil redisUtil = new RedisUtil();
    private static Jedis _jedis;
    private static String groupId;
    /*
        构件id
         */
    private UUID uuid;

    public static RedisUtil getInstance() {
        return redisUtil;
    }

    /**
     * 创建Redis连接
     *
     * @throws Exception 抛出的异常
     */
    public void getJedis(UUID uuid) throws Exception {
        this.uuid = uuid;
        if (null != _jedis && _jedis.isConnected()) {
            return;
        } else {
            Properties props = new Properties();
            try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    throw new Exception("Redis连接未配置参数！");
                }
                props.load(input);
                String host = props.getProperty("redis.host");
                String port = props.getProperty("redis.port");
                int db = Integer.parseInt(props.getProperty("redis.db"));
                groupId = props.getProperty("groupId");

                _jedis = new Jedis(host, Integer.parseInt(port), 2000);//两秒超时

                System.out.println("连接到Redis: " + _jedis.ping());
                _jedis.select(db);
            } catch (Exception e) {
                close();
                throw new Exception("Redis服务不可用", e);
            }
        }
    }

    /**
     * 关闭Redis连接
     *
     * @throws Exception 抛出的异常
     */
    public void close() throws Exception {
        try {
            if (_jedis != null && _jedis.isConnected()) {
                _jedis.close();
            }
        } catch (Exception e) {
            throw new Exception("关闭Redis连接时出错", e);
        } finally {
            _jedis = null;
        }
    }

    /**
     * 获取当前系统状态
     *
     * @return Redis中isWork的值，未运行、运行中、故障、已完成
     */
    public String getIsWork() {
        waitWrite("isWork");
        String res = _jedis.get(groupId + "_isWork");
        signalWrite("isWork");
        return res;
    }

    /**
     * 通过key读取在Redis中的对应值并将之转化为int类型
     *
     * @param key 键，mapHeight，mapWidth,carNum,
     * @return 一个int值
     */
    public int getInt(String key) {
        return Integer.parseInt(_jedis.get(groupId + "_" + key));
    }

    /**
     * 通过key读取在Redis中的对应值并将之转化为int类型，用于需要互斥的key
     *
     * @param key 键
     * @return 一个int值
     */
    public int getIntByLock(String key) {
        waitWrite(key);
        String res = _jedis.get(groupId + "_" + key);
        signalWrite(key);
        return Integer.parseInt(res);
    }

    /**
     * 将Redis中的指定地图从String转换成int[][]
     *
     * @param key       地图名称
     * @param mapHeight 地图高度
     * @param mapWidth  地图宽度
     * @return 二维数组，表示地图；障碍地图 0为非障碍，1为障碍物；探索地图 0为未探索，1为已探索
     */
    public int[][] getMap(String key, int mapHeight, int mapWidth) {
        byte[] bytes = Base64.getDecoder().decode(_jedis.get(groupId + "_" + key));
        int[][] map = new int[mapHeight][mapWidth];
        for (int i = 0; i < mapHeight; i++) {
            for (int j = 0; j < mapWidth; j++) {
                int mapIndex = i * mapWidth + j;
                int byteIndex = mapIndex / 8;
                int bitIndex = mapIndex % 8;
                if (byteIndex >= bytes.length) continue;  //超出索引，直接跳过（因为BitSet转Byte[]时只之保存到最后一个1）
                map[i][j] = (bytes[byteIndex] >> bitIndex) & 1;  //向左移位bitIndex，获取最后一位
            }
        }
        return map;
    }

    /**
     * 通过小车Id获取Redis中对应小车的String，并将其反序列化为Car对象
     *
     * @param carId 小车Id
     * @return 一个Car对象
     */
    public Car getCar(int carId) {
        String key = "Car" + String.valueOf(carId);
        Gson gson = new Gson();
        waitRead(key);  //等待读锁
        String res = _jedis.get(groupId + "_" + key);
        signalRead(key);  //释放读锁
        return gson.fromJson(res, Car.class);
    }

    /**
     * 获取Redis中的僵尸队列，将其转成Set<Integer>
     *
     * @return 僵尸队列
     */
    public Set<Integer> getDisConnectCars() {
        waitWrite("disConnectCars");
        Set<String> stringSet = _jedis.smembers(groupId + "_disConnectCars");
        signalWrite("disConnectCars");
        Set<Integer> disConnectCars = new HashSet<>();
        for (String s : stringSet) {
            try {
                disConnectCars.add(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                System.err.println("无法转换为整数：" + s);
            }
        }
        return disConnectCars;
    }

    /**
     * 获取Redis中的时间戳
     *
     * @param key 键
     * @return long类型的时间戳
     */
    public long getTimeStamp(String key) {
        return Long.parseLong(_jedis.get(groupId + "_" + key));
    }

    /**
     * 读取redis中的<key,value>
     *
     * @param key 键
     * @return 值
     */
    public String getString(String key) {
        return _jedis.get(groupId + "_" + key);
    }

    /**
     * 设置Redis中isWork的状态
     *
     * @param isWork 处于工作的某个状态
     */
    public void setIsWork(String isWork) {
        _jedis.set(groupId + "_isWork", isWork);
    }

    /**
     * 将value转为String并写入key
     *
     * @param key   键，mapHeight，mapWidth,carNum,
     * @param value 需要写入的值
     */
    public void setInt(String key, int value) {
        _jedis.set(groupId + "_" + key, String.valueOf(value));
    }

    /**
     * 将value转为String并写入key，用于需要互斥的key
     *
     * @param key   键
     * @param value 需要写入的值，int
     */
    public void setIntByLock(String key, int value) {
        waitWrite(key);
        _jedis.set(groupId + "_" + key, String.valueOf(value));
        signalWrite(key);
    }

    /**
     * 将二维int数组转成String并写入指定Redis键值对
     *
     * @param key 地图名称
     * @param map 保存地图信息的二维数组
     */
    public void setMap(String key, int[][] map) {
        int mapHeight = map.length;
        int mapWidth = map[0].length;
        int totalBite = mapHeight * mapWidth;
        BitSet bitSet = new BitSet(totalBite);
        for (int i = 0; i < mapHeight; i++) {
            for (int j = 0; j < mapWidth; j++) {
                if (map[i][j] == 1) {
                    bitSet.set(i * mapWidth + j);
                }
            }
        }
        byte[] bytes = bitSet.toByteArray();
        String stringMap = Base64.getEncoder().encodeToString(bytes);

        _jedis.set(groupId + "_" + key, stringMap);
    }

    /**
     * 修改对应小车的Redis
     *
     * @param car 小车信息，包含carId
     */
    public void setCar(Car car) {
        int carId = car.getCarId();
        String key = "Car" + String.valueOf(carId);
        waitWrite(key);
        Gson gson = new Gson();
        _jedis.set(groupId + "_" + key, gson.toJson(car));
        signalWrite(key);
    }

    /**
     * 将最新的僵尸队列更新到Redis
     *
     * @param disConnectCars 最新的僵尸队列
     */
    public void setDisConnectCars(Set<Integer> disConnectCars) {
        waitWrite("disConnectCars");
        String key = groupId + "_disConnectCars";
        _jedis.del(key); //删除set中的已有内容
        // 将每个元素添加到Redis集合中
        for (Integer carId : disConnectCars) {
            _jedis.sadd(key, carId.toString());
        }
        signalWrite("disConnectCars");
    }

    /**
     * 新增一辆僵尸小车
     *
     * @param carId 小车Id
     */
    public void addDisConnectCar(int carId) {
        waitWrite("disConnectCars");
        _jedis.sadd(groupId + "_disConnectCars", String.valueOf(carId));
        signalWrite("disConnectCars");
    }

    /**
     * 更对应的时间戳
     *
     * @param key       键
     * @param timeStamp long类型的时间戳
     */
    public void setTimeStamp(String key, long timeStamp) {
        _jedis.set(groupId + "_" + key, String.valueOf(timeStamp));
    }

    /**
     * 将<key,value>存到redis
     *
     * @param key   键
     * @param value 值
     */
    public void setString(String key, String value) {
        _jedis.set(groupId + "_" + key, value);
    }

    /**
     * P操作，写锁  普通互斥锁用writeLock即可
     *
     * @param key 对应键
     */
    public void waitWrite(String key) {
        int cnt = 40;
        String writeLockName = groupId + "_" + key + "_writeLock";
        if (_jedis.exists(writeLockName)) {
            while (cnt > 0 && Objects.equals(_jedis.get(writeLockName), "0")) {
                try {
                    cnt--;
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        _jedis.set(writeLockName, "0");
    }

    /**
     * P操作，写锁
     *
     * @param key 对应键
     */
    public void waitRead(String key) {
        String readLockName = groupId + "_" + key + "_readLock";
        if (_jedis.scard(readLockName) == 0) waitWrite(key);  //没有人在读，就需要获取写锁
        _jedis.sadd(readLockName, uuid.toString());
    }

    /**
     * V操作，写锁
     *
     * @param key 对应键
     */
    public void signalWrite(String key) {
        _jedis.set(groupId + "_" + key + "_writeLock", "1");
    }

    /**
     * V操作，读锁
     *
     * @param key 对应键
     */
    public void signalRead(String key) {
        String readLockName = groupId + "_" + key + "_readLock";
        _jedis.srem(readLockName, uuid.toString());
        if (_jedis.scard(readLockName) == 0) signalWrite(key);
    }
}
