package com.dmves.car.core.movement;

import com.dmves.car.core.model.Car;
import com.dmves.car.core.blackboard.IBlackboard;
import cn.edu.ncepu.RedisUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 避障检测器
 * 负责检测小车移动路径上是否存在障碍物或其他小车
 */
@Slf4j
public class CollisionDetector {
    private final RedisUtil redisUtil;
    private static final String MAP_BARRIER_KEY = "mapBarrier";
    private static final String MAP_WIDTH_KEY = "mapWidth";
    private static final String MAP_HEIGHT_KEY = "mapHeight";
    private static final String CAR_POSITION_KEY_PREFIX = "car:position:";

    public CollisionDetector(IBlackboard blackboard) {
        this.redisUtil = RedisUtil.getInstance();
    }

    /**
     * 检查指定方向是否可以移动
     * 
     * @param car       小车对象
     * @param direction 移动方向
     * @return 是否可以移动
     */
    public boolean canMoveToDirection(Car car, String direction) {
        try {
            // 获取地图尺寸
            int mapWidth = redisUtil.getInt("mapWidth");
            int mapHeight = redisUtil.getInt("mapHeight");

            // 获取障碍物地图
            int[][] barrierMap = redisUtil.getMap("mapBarrier", mapHeight, mapWidth);

            // 计算目标位置
            int targetX = car.getCarPosition().getX();
            int targetY = car.getCarPosition().getY();

            switch (direction.toUpperCase()) {
                case "U":
                    targetY--;
                    break;
                case "D":
                    targetY++;
                    break;
                case "L":
                    targetX--;
                    break;
                case "R":
                    targetX++;
                    break;
                default:
                    log.error("无效的移动方向: {}", direction);
                    return false;
            }

            // 检查边界
            if (targetX < 0 || targetX >= mapWidth || targetY < 0 || targetY >= mapHeight) {
                log.debug("目标位置 ({}, {}) 超出边界", targetX, targetY);
                return false;
            }

            // 检查障碍物
            if (barrierMap[targetY][targetX] == 1) {
                log.debug("在位置 ({}, {}) 检测到障碍物", targetX, targetY);
                return false;
            }

            // 检查其他小车
            if (hasOtherCar(car.getCarId(), targetX, targetY)) {
                log.debug("在位置 ({}, {}) 检测到其他小车", targetX, targetY);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("检查移动方向时出错: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查指定位置是否有其他小车
     * 
     * @param currentCarId 当前小车ID
     * @param x            目标X坐标
     * @param y            目标Y坐标
     * @return 是否存在其他小车
     */
    private boolean hasOtherCar(String currentCarId, int x, int y) {
        try {
            // 获取小车数量
            int carNumber = redisUtil.getInt("carNum");

            // 遍历所有小车
            for (int i = 1; i <= carNumber; i++) {
                String carId = String.valueOf(i);

                // 跳过当前小车
                if (carId.equals(currentCarId)) {
                    continue;
                }

                // 首先尝试通过RedisUtil直接获取小车对象
                cn.edu.ncepu.Model.Car redisCar = redisUtil.getCar(i);
                if (redisCar != null && redisCar.getCarPosition() != null) {
                    java.awt.Point position = redisCar.getCarPosition();
                    if (position.x == x && position.y == y) {
                        return true;
                    }
                } else {
                    // 如果获取不到小车对象，尝试从car:position:carId键获取位置信息
                    String positionStr = redisUtil.getString(CAR_POSITION_KEY_PREFIX + carId);
                    if (positionStr != null && !positionStr.isEmpty()) {
                        String[] parts = positionStr.split(",");
                        if (parts.length == 2) {
                            int carX = Integer.parseInt(parts[0]);
                            int carY = Integer.parseInt(parts[1]);
                            if (carX == x && carY == y) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            log.error("检查其他小车位置时出错: {}", e.getMessage(), e);
            return false;
        }
    }
}