package com.dmves.car.core.state;

import com.dmves.car.core.blackboard.IBlackboard;
import com.dmves.car.core.model.Car;
import com.dmves.car.core.model.CarStatusEnum;
import cn.edu.ncepu.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * 小车状态管理器
 * 负责管理状态计数器和超时状态转换
 */
@Slf4j
public class CarStateManager {
    private final Car car;
    private final IBlackboard blackboard;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    public CarStateManager(Car car, IBlackboard blackboard) {
        this.car = car;
        this.blackboard = blackboard;
        this.redisUtil = RedisUtil.getInstance();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 更新小车状态
     * 由控制器通过消息控制状态转换
     */
    public void updateState(CarStatusEnum newState) {
        CarStatusEnum oldState = car.getCarStatus();
        car.setCarStatus(newState);
        setStatusCounter(newState);
        persistState();
        log.info("Car {} state changed from {} to {}", car.getCarId(), oldState, newState);
    }

    /**
     * 设置状态计数器
     */
    private void setStatusCounter(CarStatusEnum state) {
        switch (state) {
            case NAVIGATING:
                car.setCarStatusCnt(10);
                break;
            case SEARCHING:
                car.setCarStatusCnt(10);
                break;
            case WAITING:
                car.setCarStatusCnt(10);
                break;
            default:
                car.setCarStatusCnt(0);
                break;
        }
    }

    /**
     * 持久化小车状态到Redis
     */
    public void persistState() {
        try {
            String carJson = objectMapper.writeValueAsString(car);
            redisUtil.setString("car:" + car.getCarId(), carJson);
            log.debug("Car {} state persisted to Redis", car.getCarId());
        } catch (Exception e) {
            log.error("Failed to persist state for car {}", car.getCarId(), e);
        }
    }

    /**
     * 从Redis恢复小车状态
     */
    public boolean restoreState() {
        try {
            String carJson = redisUtil.getString("car:" + car.getCarId());
            if (carJson == null || carJson.isEmpty()) {
                log.warn("No state found for car {}", car.getCarId());
                return false;
            }

            Car restoredCar = objectMapper.readValue(carJson, Car.class);
            updateCarFromRestored(restoredCar);
            log.info("Car {} state restored to {}", car.getCarId(), car.getCarStatus());
            return true;
        } catch (Exception e) {
            log.error("Failed to restore state for car {}", car.getCarId(), e);
            return false;
        }
    }

    /**
     * 从恢复的Car对象更新当前Car
     */
    private void updateCarFromRestored(Car restoredCar) {
        car.setCarStatus(restoredCar.getCarStatus());
        car.setCarPosition(restoredCar.getCarPosition());
        car.setCarTarget(restoredCar.getCarTarget());
        car.setCarPath(restoredCar.getCarPath());
        car.setCarAlgorithm(restoredCar.getCarAlgorithm());
        car.setCarStatusCnt(restoredCar.getCarStatusCnt());
        car.setCarColor(restoredCar.getCarColor());
        car.setCarLastRunTime(System.currentTimeMillis()); // 更新为当前时间
    }

    /**
     * 减少状态计数器，如果计数器为0则处理超时状态转换
     * 在非RUNNING状态下才减少计数器
     */
    public void decrementStatusCounter() {
        CarStatusEnum currentState = car.getCarStatus();

        // 只在非RUNNING状态下减少计数器
        if (currentState != CarStatusEnum.RUNNING && car.getCarStatusCnt() > 0) {
            car.setCarStatusCnt(car.getCarStatusCnt() - 1);

            // 计数器为0时，处理状态转换
            if (car.getCarStatusCnt() == 0) {
                handleStatusTimeout();
            }

            persistState();
        }
    }

    /**
     * 处理状态超时
     * 只处理SEARCHING、NAVIGATING和WAITING状态的超时转换
     */
    private void handleStatusTimeout() {
        CarStatusEnum currentState = car.getCarStatus();

        switch (currentState) {
            case SEARCHING:
                // 搜索超时，回到FREE状态
                log.info("Car {} SEARCHING timeout, changing to FREE", car.getCarId());
                updateState(CarStatusEnum.FREE);
                break;

            case NAVIGATING:
                // 导航超时，回到WAIT_NAV状态
                log.info("Car {} NAVIGATING timeout, changing to WAIT_NAV", car.getCarId());
                updateState(CarStatusEnum.WAIT_NAV);
                break;

            case WAITING:
                // 等待超时，回到WAIT_NAV状态
                log.info("Car {} WAITING timeout, changing to WAIT_NAV", car.getCarId());
                updateState(CarStatusEnum.WAIT_NAV);
                break;

            default:
                // 其他状态不处理
                break;
        }
    }
}