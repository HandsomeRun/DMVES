package com.dmves.car.core.movement;

import com.dmves.car.core.model.Car;
import com.dmves.car.core.model.Point;
import com.dmves.car.core.connector.RedisConnector;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 路径执行器
 * 负责执行小车的移动路径
 */
@Slf4j
public class PathExecutor {
    private final Car car;
    private final CollisionDetector collisionDetector;
    private final AtomicBoolean isExecuting;
    private CompletableFuture<Void> currentExecution;

    public PathExecutor(Car car, CollisionDetector collisionDetector) {
        this.car = car;
        this.collisionDetector = collisionDetector;
        this.isExecuting = new AtomicBoolean(false);
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
     * 执行路径
     * 
     * @param path 路径字符串
     * @return 是否开始执行
     */
    public boolean executePath(String path) {
        if (path == null || path.isEmpty()) {
            log.warn("小车 {} 的路径为空", car.getCarId());
            return false;
        }

        isExecuting.set(true);
        currentExecution = CompletableFuture.runAsync(() -> {
            try {
                for (char direction : path.toCharArray()) {
                    if (!isExecuting.get()) {
                        log.info("小车 {} 的路径执行已停止", car.getCarId());
                        break;
                    }

                    // 直接执行移动，不检查是否成功
                    executeMove(String.valueOf(direction));

                    // 每步移动后等待一段时间
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                log.error("小车 {} 的路径执行被中断", car.getCarId(), e);
                Thread.currentThread().interrupt();
            } finally {
                isExecuting.set(false);
            }
        });

        return true;
    }

    /**
     * 停止路径执行
     */
    public void stop() {
        isExecuting.set(false);
        if (currentExecution != null) {
            currentExecution.cancel(true);
        }
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
}