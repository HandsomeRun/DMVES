package com.dmves.car.core.movement;

import com.dmves.car.core.model.Car;
import com.dmves.car.core.model.Point;
import com.dmves.car.core.connector.RedisConnector;
import lombok.extern.slf4j.Slf4j;

/**
 * 路径执行器
 * 负责执行小车的移动路径
 */
@Slf4j
public class PathExecutor {
    private final Car car;
    private final CollisionDetector collisionDetector;
    private String currentPath;

    public PathExecutor(Car car, CollisionDetector collisionDetector) {
        this.car = car;
        this.collisionDetector = collisionDetector;
        this.currentPath = "";
    }

    /**
     * 执行单步移动
     * 
     * @param direction 移动方向
     * @return 是否执行成功
     */
    public boolean executeMove(String direction) {
        if (!collisionDetector.canMoveToDirection(car, direction)) {
            return false;
        }

        Point currentPosition = car.getCarPosition();
        Point newPosition = calculateNewPosition(currentPosition, direction);
        car.setCarPosition(newPosition);

        // 更新Redis中的位置信息
        RedisConnector.updateCarPosition(car.getCarId(), newPosition);

        return true;
    }

    /**
     * 设置路径
     * 
     * @param path 路径字符串
     * @return 是否设置成功
     */
    public boolean setPath(String path) {
        if (path == null || path.isEmpty()) {
            log.warn("小车 {} 的路径为空", car.getCarId());
            return false;
        }

        this.currentPath = path;
        log.info("小车 {} 设置路径: {}", car.getCarId(), path);
        return true;
    }

    /**
     * 接收MQ执行命令，执行下一步移动
     * 
     * @return 是否成功执行
     */
    public boolean executeNextStep() {
        if (currentPath.isEmpty()) {
            log.warn("小车 {} 没有可执行的路径或已执行完毕", car.getCarId());
            return false;
        }

        // 获取最后一个字符作为移动方向
        char direction = currentPath.charAt(currentPath.length() - 1);
        boolean result = executeMove(String.valueOf(direction));

        // 移除已执行的字符
        currentPath = currentPath.substring(0, currentPath.length() - 1);

        // 检查是否执行完毕
        if (currentPath.isEmpty()) {
            log.info("小车 {} 的路径执行完毕", car.getCarId());
        }

        return result;
    }

    /**
     * 清除当前路径
     */
    public void clearPath() {
        this.currentPath = "";
        log.info("小车 {} 的路径已清除", car.getCarId());
    }

    /**
     * 计算新位置
     */
    private Point calculateNewPosition(Point current, String direction) {
        int x = current.getX();
        int y = current.getY();

        switch (direction.toUpperCase()) {
            case "U":
                y--;
                break;
            case "D":
                y++;
                break;
            case "L":
                x--;
                break;
            case "R":
                x++;
                break;
        }

        return new Point(x, y);
    }

    /**
     * 检查是否还有路径需要执行
     * 
     * @return 是否有剩余路径
     */
    public boolean hasRemainingPath() {
        return !currentPath.isEmpty();
    }

    /**
     * 获取当前路径信息
     * 
     * @return 当前路径信息
     */
    public String getPathInfo() {
        if (currentPath.isEmpty()) {
            return "无路径";
        }
        return String.format("路径: %s, 剩余步数: %d", currentPath, currentPath.length());
    }
}