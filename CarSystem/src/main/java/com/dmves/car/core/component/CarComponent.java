package com.dmves.car.core.component;

import com.dmves.car.core.blackboard.IBlackboard;
import com.dmves.car.core.model.Car;
import com.dmves.car.core.model.CarStatusEnum;
import com.dmves.car.core.model.Point;
import com.dmves.car.core.movement.PathExecutor;
import com.dmves.car.core.movement.CollisionDetector;
import com.dmves.car.core.state.CarStateManager;
import cn.edu.ncepu.Util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

/**
 * 小车组件
 * 只负责更新心跳、执行导航器传来的路径和点亮地图
 */
@Slf4j
public class CarComponent implements IComponent {
    private final String carId;
    private final IBlackboard blackboard;
    private final ObjectMapper objectMapper;
    private final Car car;
    private final CollisionDetector collisionDetector;
    private final PathExecutor pathExecutor;
    private final CarStateManager stateManager;
    private final RedisUtil redisUtil;
    private final UUID uuid;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> heartbeatFuture;
    private ScheduledFuture<?> statusCheckFuture;

    // Redis键前缀
    private static final String CAR_POSITION_KEY_PREFIX = "car:position:";
    private static final String MAP_LIGHT_KEY_PREFIX = "map:light:";

    public CarComponent(String carId, IBlackboard blackboard) {
        this.carId = carId;
        this.blackboard = blackboard;
        this.objectMapper = new ObjectMapper();
        this.uuid = UUID.randomUUID();

        // 获取RedisUtil实例
        this.redisUtil = RedisUtil.getInstance();

        try {
            // 建立Redis连接
            redisUtil.getJedis(uuid);
            log.info("Redis连接成功建立");
        } catch (Exception e) {
            log.error("Redis连接失败: {}", e.getMessage());
            throw new RuntimeException("Redis连接失败", e);
        }

        // 从Redis读取小车实例
        Car loadedCar = loadCarFromRedis(carId);

        if (loadedCar != null) {
            // 如果Redis中存在小车信息，使用Redis中的数据
            this.car = loadedCar;
            log.info("从Redis成功加载小车 {}", carId);
        } else {
            // 如果Redis中不存在小车信息，创建新的小车对象
            this.car = Car.builder()
                    .carId(carId)
                    .carStatus(CarStatusEnum.FREE)
                    .carPosition(new Point(0, 0)) // 默认位置
                    .carStatusCnt(0)
                    .carLastRunTime(System.currentTimeMillis())
                    .carPath("")
                    .carColor("blue") // 默认颜色，因为RedisUtil的Car类可能没有getCarColor方法
                    .build();
            log.info("Redis中不存在小车 {}，创建新的小车实例", carId);
        }

        // 初始化碰撞检测器和路径执行器
        this.collisionDetector = new CollisionDetector(blackboard);
        this.pathExecutor = new PathExecutor(car, collisionDetector);

        // 初始化状态管理器
        this.stateManager = new CarStateManager(car, blackboard);

        // 初始化时持久化状态
        persistState();

        // 初始化时更新位置到Redis
        updateCarPositionToRedis();
    }

    /**
     * 从Redis加载小车实例
     * 
     * @param carId 小车ID
     * @return 小车实例，如果不存在则返回null
     */
    private Car loadCarFromRedis(String carId) {
        try {
            // 使用RedisUtil获取小车数据
            // 注意：RedisUtil的Car类与当前项目的Car类不同，需要进行转换
            cn.edu.ncepu.Model.Car redisCar = redisUtil.getCar(Integer.parseInt(carId));

            if (redisCar == null) {
                return null;
            }

            // 将RedisUtil的Car对象转换为当前项目的Car对象
            Car loadedCar = Car.builder()
                    .carId(carId)
                    .carStatus(convertCarStatus(redisCar.getCarStatus()))
                    .carPosition(convertPoint(redisCar.getCarPosition()))
                    .carTarget(convertPoint(redisCar.getCarTarget()))
                    .carPath(redisCar.getCarPath())
                    .carStatusCnt(redisCar.getCarStatusCnt())
                    .carLastRunTime(redisCar.getCarLastRunTime())
                    .carColor("blue") // 默认颜色，因为RedisUtil的Car类可能没有getCarColor方法
                    .build();

            // 更新心跳时间为当前时间
            loadedCar.setCarLastRunTime(System.currentTimeMillis());
            return loadedCar;
        } catch (Exception e) {
            log.error("从Redis加载小车 {} 失败", carId, e);
            return null;
        }
    }

    /**
     * 转换RedisUtil的Point对象为当前项目的Point对象
     */
    private Point convertPoint(java.awt.Point awtPoint) {
        if (awtPoint == null) {
            return null;
        }
        return new Point(awtPoint.x, awtPoint.y);
    }

    /**
     * 转换RedisUtil的CarStatusEnum为当前项目的CarStatusEnum
     */
    private CarStatusEnum convertCarStatus(cn.edu.ncepu.Model.CarStatusEnum redisStatus) {
        if (redisStatus == null) {
            return CarStatusEnum.FREE;
        }

        switch (redisStatus) {
            case FREE:
                return CarStatusEnum.FREE;
            case RUNNING:
                return CarStatusEnum.RUNNING;
            case SEARCHING:
                return CarStatusEnum.SEARCHING;
            case WAIT_NAV:
                return CarStatusEnum.WAIT_NAV;
            case NAVIGATING:
                return CarStatusEnum.NAVIGATING;
            case WAITING:
                return CarStatusEnum.WAITING;
            case DISCONNECTING:
                return CarStatusEnum.DISCONNECTING;
            default:
                return CarStatusEnum.FREE;
        }
    }

    /**
     * 转换当前项目的CarStatusEnum为RedisUtil的CarStatusEnum
     */
    private cn.edu.ncepu.Model.CarStatusEnum convertToRedisCarStatus(CarStatusEnum status) {
        if (status == null) {
            return cn.edu.ncepu.Model.CarStatusEnum.FREE;
        }

        switch (status) {
            case FREE:
                return cn.edu.ncepu.Model.CarStatusEnum.FREE;
            case RUNNING:
                return cn.edu.ncepu.Model.CarStatusEnum.RUNNING;
            case SEARCHING:
                return cn.edu.ncepu.Model.CarStatusEnum.SEARCHING;
            case WAIT_NAV:
                return cn.edu.ncepu.Model.CarStatusEnum.WAIT_NAV;
            case NAVIGATING:
                return cn.edu.ncepu.Model.CarStatusEnum.NAVIGATING;
            case WAITING:
                return cn.edu.ncepu.Model.CarStatusEnum.WAITING;
            case DISCONNECTING:
                return cn.edu.ncepu.Model.CarStatusEnum.DISCONNECTING;
            default:
                return cn.edu.ncepu.Model.CarStatusEnum.FREE;
        }
    }

    @Override
    public void initialize() {
        scheduler = Executors.newScheduledThreadPool(2);
        log.info("小车组件 {} 已初始化", carId);

        // 如果有路径，则设置到路径执行器
        if (car.getCarPath() != null && !car.getCarPath().isEmpty()) {
            pathExecutor.setPath(car.getCarPath());
        }
    }

    @Override
    public void start() {
        // 启动心跳任务
        startHeartbeat();

        // 启动状态检查任务
        startStatusCheck();

        log.info("小车组件 {} 已启动", carId);
    }

    @Override
    public void stop() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }

        if (statusCheckFuture != null) {
            statusCheckFuture.cancel(true);
        }

        if (scheduler != null) {
            scheduler.shutdown();
        }

        // 关闭Redis连接
        try {
            redisUtil.close();
            log.info("Redis连接已关闭");
        } catch (Exception e) {
            log.error("关闭Redis连接失败: {}", e.getMessage());
        }

        log.info("小车组件 {} 已停止", carId);
    }

    @Override
    public String getName() {
        return "Car-" + carId;
    }

    /**
     * 获取小车ID
     */
    public String getCarId() {
        return carId;
    }

    /**
     * 启动心跳任务
     */
    private void startHeartbeat() {
        heartbeatFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                // 更新心跳时间
                car.setCarLastRunTime(System.currentTimeMillis());
                // 持久化状态到Redis
                persistState();
                log.debug("小车 {} 心跳更新", carId);
            } catch (Exception e) {
                log.error("小车 {} 的心跳任务出错", carId, e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * 启动状态检查任务
     */
    private void startStatusCheck() {
        statusCheckFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                // 检查carPath是否为空
                if (car.getCarPath() == null || car.getCarPath().isEmpty()) {
                    if (car.getCarStatus() == CarStatusEnum.RUNNING) {
                        stateManager.changeState(CarStatusEnum.FREE);
                        log.info("小车 {} 路径为空，切换到空闲状态", carId);
                    }
                    return;
                }

                // 检查状态计数器
                if (car.getCarStatus() != CarStatusEnum.RUNNING) {
                    stateManager.decrementStatusCounter();
                }

                // 如果状态为RUNNING，执行路径
                if (car.getCarStatus() == CarStatusEnum.RUNNING && pathExecutor.hasRemainingPath()) {
                    boolean moveSuccess = pathExecutor.executeNextStep();
                    if (moveSuccess) {
                        // 点亮地图
                        updateMap();
                        // 更新Redis中的小车位置
                        updateCarPositionToRedis();
                    } else {
                        // 处理路径执行失败
                        stateManager.changeState(CarStatusEnum.WAITING);
                        log.info("小车 {} 当前步骤不可走，切换到等待状态", carId);
                    }
                }
            } catch (Exception e) {
                log.error("小车 {} 的状态检查任务出错", carId, e);
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * 点亮地图
     * 通过获取迷雾地图，更新小车周围九宫格，然后保存回Redis
     */
    private void updateMap() {
        try {
            Point position = car.getCarPosition();
            int x = position.getX();
            int y = position.getY();

            try {
                // 获取地图尺寸
                int mapWidth = redisUtil.getInt("mapWidth");
                int mapHeight = redisUtil.getInt("mapHeight");

                // 获取迷雾地图
                int[][] fogMap = redisUtil.getMap("mapFog", mapHeight, mapWidth);

                // 更新小车周围的九宫格
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int newX = x + i;
                        int newY = y + j;

                        // 检查坐标是否在地图范围内
                        if (newX >= 0 && newX < mapWidth && newY >= 0 && newY < mapHeight) {
                            fogMap[newY][newX] = 1; // 设置为已探索
                        }
                    }
                }

                // 保存更新后的迷雾地图回Redis
                redisUtil.setMap("mapFog", fogMap);
                log.debug("小车 {} 点亮地图位置 ({},{}) 及周围九宫格", carId, x, y);
            } catch (Exception e) {
                log.warn("使用Redis点亮地图失败: {}", e.getMessage());
            }

            // 同时记录当前位置到Redis
            redisUtil.setString(MAP_LIGHT_KEY_PREFIX + carId, position.getX() + "," + position.getY());
        } catch (Exception e) {
            log.error("小车 {} 点亮地图失败", carId, e);
        }
    }

    /**
     * 更新小车位置到Redis
     */
    private void updateCarPositionToRedis() {
        try {
            Point position = car.getCarPosition();
            // 使用Redis记录位置
            redisUtil.setString(CAR_POSITION_KEY_PREFIX + carId, position.getX() + "," + position.getY());
            log.debug("小车 {} 位置已更新到Redis: ({},{})", carId, position.getX(), position.getY());
        } catch (Exception e) {
            log.error("小车 {} 更新位置到Redis失败", carId, e);
        }
    }

    /**
     * 持久化小车状态到Redis
     */
    public void persistState() {
        try {
            // 将当前项目的Car对象转换为RedisUtil的Car对象
            java.awt.Point position = new java.awt.Point(
                    car.getCarPosition().getX(),
                    car.getCarPosition().getY());

            java.awt.Point target = null;
            if (car.getCarTarget() != null) {
                target = new java.awt.Point(
                        car.getCarTarget().getX(),
                        car.getCarTarget().getY());
            }

            cn.edu.ncepu.Model.Car redisCar = new cn.edu.ncepu.Model.Car(
                    Integer.parseInt(carId),
                    convertToRedisCarStatus(car.getCarStatus()),
                    position,
                    target,
                    car.getCarPath(),
                    null, // 算法暂不设置
                    car.getCarStatusCnt(),
                    car.getCarColor(), // 使用当前小车的颜色
                    car.getCarLastRunTime());

            // 使用RedisUtil保存小车状态
            redisUtil.setCar(redisCar);

            // 同时使用状态管理器持久化状态到Redis
            try {
                String carJson = objectMapper.writeValueAsString(car);
                redisUtil.setString("car:" + carId, carJson);
            } catch (Exception e) {
                log.error("序列化小车状态失败: {}", e.getMessage(), e);
            }

            log.debug("小车 {} 状态已持久化到Redis", carId);
        } catch (Exception e) {
            log.error("小车 {} 持久化状态失败", carId, e);
        }
    }

    /**
     * 设置路径 - 接收导航器传来的路径
     */
    public boolean setPath(String path) {
        if (path == null || path.isEmpty()) {
            log.warn("小车 {} 收到空路径", carId);
            return false;
        }

        // 设置路径到路径执行器
        boolean result = pathExecutor.setPath(path);
        if (result) {
            // 更新小车状态
            car.setCarPath(path);
            stateManager.changeState(CarStatusEnum.RUNNING);
            log.info("小车 {} 设置路径: {}, 切换到运行状态", carId, path);
        }

        return result;
    }

    /**
     * 获取小车当前状态
     */
    public CarStatusEnum getStatus() {
        return car.getCarStatus();
    }

    /**
     * 获取小车当前位置
     */
    public Point getPosition() {
        return car.getCarPosition();
    }

    /**
     * 移动小车（根据方向）
     * 
     * @param direction 移动方向（U, D, L, R）
     * @return 是否移动成功
     */
    public boolean move(String direction) {
        if (direction == null || direction.isEmpty()) {
            return false;
        }

        // 检查是否可以移动到新位置
        if (!collisionDetector.canMoveToDirection(car, direction)) {
            log.warn("小车 {} 无法向 {} 方向移动，该位置不可通行", carId, direction);
            return false;
        }

        // 根据方向计算新位置
        Point currentPosition = car.getCarPosition();
        Point newPosition = new Point(currentPosition.getX(), currentPosition.getY());

        // 根据方向移动
        switch (direction.toUpperCase()) {
            case "U":
                newPosition.setY(newPosition.getY() - 1);
                break;
            case "D":
                newPosition.setY(newPosition.getY() + 1);
                break;
            case "L":
                newPosition.setX(newPosition.getX() - 1);
                break;
            case "R":
                newPosition.setX(newPosition.getX() + 1);
                break;
            default:
                log.warn("小车 {} 收到无效的移动方向: {}", carId, direction);
                return false;
        }

        // 更新位置
        car.setCarPosition(newPosition);

        // 更新地图和Redis
        updateMap();
        updateCarPositionToRedis();
        persistState();

        log.info("小车 {} 移动到位置 ({},{})", carId, newPosition.getX(), newPosition.getY());
        return true;
    }

    /**
     * 更新小车状态
     * 
     * @param status 新状态
     */
    public void updateStatus(CarStatusEnum status) {
        stateManager.changeState(status);
    }

    /**
     * 执行路径
     * 
     * @param path 路径字符串
     * @return 是否成功设置路径
     */
    public boolean executePath(String path) {
        return setPath(path);
    }

    /**
     * 更新目标位置
     * 
     * @param x X坐标
     * @param y Y坐标
     */
    public void updateTarget(int x, int y) {
        Point target = new Point(x, y);
        car.setCarTarget(target);
        persistState();
        log.info("小车 {} 目标位置更新为 ({},{})", carId, x, y);
    }

    /**
     * 获取当前状态
     * 
     * @return 当前状态枚举
     */
    public CarStatusEnum getCurrentStatus() {
        return car.getCarStatus();
    }
}