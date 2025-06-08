package com.dmves.car.core.movement;

import com.dmves.car.core.model.Car;
import com.dmves.car.core.model.CarStatusEnum;
import com.dmves.car.core.blackboard.IBlackboard;
import lombok.extern.slf4j.Slf4j;

/**
 * 移动控制器
 * 负责控制小车的移动，包括移动验证、方向控制等
 */
@Slf4j
public class MovementController {
    private final Car car;
    private final CollisionDetector collisionDetector;
    private final PathExecutor pathExecutor;

    public MovementController(Car car, IBlackboard blackboard) {
        this.car = car;
        this.collisionDetector = new CollisionDetector(blackboard);
        this.pathExecutor = new PathExecutor(car, collisionDetector);
    }

    /**
     * 执行移动
     * 
     * @param direction 移动方向 (U:上, D:下, L:左, R:右)
     * @return 是否移动成功
     */
    public boolean move(String direction) {
        // 检查当前状态是否允许移动
        if (!canMove()) {
            log.warn("小车 {} 在当前状态 {} 下无法移动", car.getCarId(), car.getCarStatus());
            return false;
        }

        // 移除碰撞检测，直接执行移动
        boolean moved = pathExecutor.executeMove(direction);
        if (moved) {
            car.setCarStatus(CarStatusEnum.RUNNING);
            log.info("小车 {} 向 {} 方向移动成功", car.getCarId(), direction);
        }

        return moved;
    }

    /**
     * 执行路径
     * 
     * @param path 路径字符串 (例如: "UDLR")
     * @return 是否执行成功
     */
    public boolean executePath(String path) {
        if (path == null || path.isEmpty()) {
            log.warn("小车 {} 的路径为空", car.getCarId());
            return false;
        }

        return pathExecutor.executePath(path);
    }

    /**
     * 检查是否可以移动
     */
    private boolean canMove() {
        return car.getCarStatus() != CarStatusEnum.DISCONNECTING
                && car.getCarStatus() != CarStatusEnum.WAITING;
    }

    /**
     * 停止移动
     */
    public void stop() {
        pathExecutor.stop();
        car.setCarStatus(CarStatusEnum.FREE);
        log.info("小车 {} 已停止移动", car.getCarId());
    }
}