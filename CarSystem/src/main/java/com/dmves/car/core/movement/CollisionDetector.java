package com.dmves.car.core.movement;

import com.dmves.car.core.model.Car;
import com.dmves.car.core.blackboard.IBlackboard;
import lombok.extern.slf4j.Slf4j;

/**
 * 避障检测器
 * 负责检测小车移动路径上是否存在障碍物或其他小车
 */
@Slf4j
public class CollisionDetector {
    private final IBlackboard blackboard;
    private static final String MAP_BARRIER_KEY = "mapBarrier";
    private static final String MAP_WIDTH_KEY = "mapWidth";
    private static final String MAP_HEIGHT_KEY = "mapHeight";

    public CollisionDetector(IBlackboard blackboard) {
        this.blackboard = blackboard;
    }

    /**
     * 检查指定方向是否可以移动
     * 
     * @param car       小车对象
     * @param direction 移动方向
     * @return 是否可以移动
     */
    public boolean canMoveToDirection(Car car, String direction) {
        // 获取地图信息
        int mapWidth = Integer.parseInt(blackboard.read(MAP_WIDTH_KEY));
        int mapHeight = Integer.parseInt(blackboard.read(MAP_HEIGHT_KEY));
        String barrierMap = blackboard.read(MAP_BARRIER_KEY);

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
        if (hasBarrier(barrierMap, targetX, targetY, mapWidth)) {
            log.debug("在位置 ({}, {}) 检测到障碍物", targetX, targetY);
            return false;
        }

        // 检查其他小车
        if (hasOtherCar(car.getCarId(), targetX, targetY)) {
            log.debug("在位置 ({}, {}) 检测到其他小车", targetX, targetY);
            return false;
        }

        return true;
    }

    /**
     * 检查指定位置是否有障碍物
     */
    private boolean hasBarrier(String barrierMap, int x, int y, int mapWidth) {
        int index = y * mapWidth + x;
        return barrierMap.charAt(index) == '1';
    }

    /**
     * 检查指定位置是否有其他小车
     */
    private boolean hasOtherCar(String currentCarId, int x, int y) {
        // TODO: 从Redis中获取其他小车位置信息并检查
        return false;
    }
}