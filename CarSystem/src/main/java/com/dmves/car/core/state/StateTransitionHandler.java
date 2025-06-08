package com.dmves.car.core.state;

import com.dmves.car.core.model.Car;
import com.dmves.car.core.model.CarStatusEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 状态转换处理器
 * 负责处理状态转换前后的操作
 */
@Slf4j
public class StateTransitionHandler {

    /**
     * 状态转换前的处理
     * 
     * @param car       小车对象
     * @param fromState 原状态
     * @param toState   目标状态
     */
    public void beforeStateChange(Car car, CarStatusEnum fromState, CarStatusEnum toState) {
        log.debug("Before state change for car {}: {} -> {}", car.getCarId(), fromState, toState);

        // 根据不同的状态转换执行不同的操作
        switch (toState) {
            case RUNNING:
                // 转换到运行状态前的准备
                prepareForRunning(car);
                break;
            case WAITING:
                // 转换到等待状态前的准备
                prepareForWaiting(car);
                break;
            case SEARCHING:
                // 转换到搜索状态前的准备
                prepareForSearching(car);
                break;
            case WAIT_NAV:
                // 转换到等待导航状态前的准备
                prepareForWaitNav(car);
                break;
            case NAVIGATING:
                // 转换到导航状态前的准备
                prepareForNavigating(car);
                break;
            case DISCONNECTING:
                // 转换到断开连接状态前的准备
                prepareForDisconnecting(car);
                break;
            case FREE:
                // 转换到空闲状态前的准备
                prepareForFree(car);
                break;
        }
    }

    /**
     * 状态转换后的处理
     * 
     * @param car       小车对象
     * @param fromState 原状态
     * @param toState   目标状态
     */
    public void afterStateChange(Car car, CarStatusEnum fromState, CarStatusEnum toState) {
        log.debug("After state change for car {}: {} -> {}", car.getCarId(), fromState, toState);

        // 根据不同的状态转换执行不同的操作
        switch (toState) {
            case RUNNING:
                // 转换到运行状态后的操作
                handleRunningState(car);
                break;
            case WAITING:
                // 转换到等待状态后的操作
                handleWaitingState(car);
                break;
            case SEARCHING:
                // 转换到搜索状态后的操作
                handleSearchingState(car);
                break;
            case WAIT_NAV:
                // 转换到等待导航状态后的操作
                handleWaitNavState(car);
                break;
            case NAVIGATING:
                // 转换到导航状态后的操作
                handleNavigatingState(car);
                break;
            case DISCONNECTING:
                // 转换到断开连接状态后的操作
                handleDisconnectingState(car);
                break;
            case FREE:
                // 转换到空闲状态后的操作
                handleFreeState(car);
                break;
        }
    }

    // 各状态转换前的准备方法

    private void prepareForRunning(Car car) {
        // 运行前的准备，例如清除等待状态的计数器
        car.setCarStatusCnt(0);
    }

    private void prepareForWaiting(Car car) {
        // 设置等待超时计数器，例如50个周期后自动回到FREE状态
        car.setCarStatusCnt(50);
    }

    private void prepareForSearching(Car car) {
        // 设置搜索超时计数器
        car.setCarStatusCnt(100);
        // 清除目标和路径
        car.setCarTarget(null);
        car.setCarPath("");
    }

    private void prepareForWaitNav(Car car) {
        // 设置等待导航超时计数器
        car.setCarStatusCnt(30);
    }

    private void prepareForNavigating(Car car) {
        // 设置导航超时计数器
        car.setCarStatusCnt(50);
        // 清除路径，等待导航结果
        car.setCarPath("");
    }

    private void prepareForDisconnecting(Car car) {
        // 断开连接前的准备
        car.setCarStatusCnt(0);
    }

    private void prepareForFree(Car car) {
        // 回到空闲状态前的准备，清除状态计数器
        car.setCarStatusCnt(0);
    }

    // 各状态转换后的处理方法

    private void handleRunningState(Car car) {
        // 运行状态的后续处理
        log.info("Car {} is now running", car.getCarId());
    }

    private void handleWaitingState(Car car) {
        // 等待状态的后续处理
        log.info("Car {} is now waiting, will timeout after {} cycles",
                car.getCarId(), car.getCarStatusCnt());
    }

    private void handleSearchingState(Car car) {
        // 搜索状态的后续处理
        log.info("Car {} is now searching for target, will timeout after {} cycles",
                car.getCarId(), car.getCarStatusCnt());
    }

    private void handleWaitNavState(Car car) {
        // 等待导航状态的后续处理
        log.info("Car {} is now waiting for navigation, will timeout after {} cycles",
                car.getCarId(), car.getCarStatusCnt());
    }

    private void handleNavigatingState(Car car) {
        // 导航状态的后续处理
        log.info("Car {} is now navigating, will timeout after {} cycles",
                car.getCarId(), car.getCarStatusCnt());
    }

    private void handleDisconnectingState(Car car) {
        // 断开连接状态的后续处理
        log.info("Car {} is now disconnecting", car.getCarId());
    }

    private void handleFreeState(Car car) {
        // 空闲状态的后续处理
        log.info("Car {} is now free", car.getCarId());
    }
}