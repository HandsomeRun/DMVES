package com.dmves.car.core.state;

import com.dmves.car.core.blackboard.IBlackboard;
import com.dmves.car.core.model.Car;
import com.dmves.car.core.model.CarStatusEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 小车状态管理器
 * 负责管理小车状态的转换、持久化和恢复
 */
@Slf4j
public class CarStateManager {
    private final Car car;
    private final IBlackboard blackboard;
    private final ObjectMapper objectMapper;
    private final Map<CarStatusEnum, Map<CarStatusEnum, Predicate<Car>>> stateTransitions;
    private final StateTransitionHandler transitionHandler;

    public CarStateManager(Car car, IBlackboard blackboard) {
        this.car = car;
        this.blackboard = blackboard;
        this.objectMapper = new ObjectMapper();
        this.stateTransitions = new HashMap<>();
        this.transitionHandler = new StateTransitionHandler();

        initializeStateTransitions();
    }

    /**
     * 初始化状态转换规则
     */
    private void initializeStateTransitions() {
        // 从 FREE 状态的转换
        Map<CarStatusEnum, Predicate<Car>> fromFree = new HashMap<>();
        fromFree.put(CarStatusEnum.RUNNING, c -> true); // 可以直接开始运行
        fromFree.put(CarStatusEnum.SEARCHING, c -> true); // 可以开始寻找目标
        fromFree.put(CarStatusEnum.DISCONNECTING, c -> true); // 可以断开连接
        stateTransitions.put(CarStatusEnum.FREE, fromFree);

        // 从 RUNNING 状态的转换
        Map<CarStatusEnum, Predicate<Car>> fromRunning = new HashMap<>();
        fromRunning.put(CarStatusEnum.FREE, c -> true); // 运行完成后回到空闲
        fromRunning.put(CarStatusEnum.WAITING, c -> true); // 遇到障碍物等待
        fromRunning.put(CarStatusEnum.DISCONNECTING, c -> true); // 可以断开连接
        stateTransitions.put(CarStatusEnum.RUNNING, fromRunning);

        // 从 SEARCHING 状态的转换
        Map<CarStatusEnum, Predicate<Car>> fromSearching = new HashMap<>();
        fromSearching.put(CarStatusEnum.WAIT_NAV, c -> c.getCarTarget() != null); // 找到目标后等待导航
        fromSearching.put(CarStatusEnum.FREE, c -> true); // 搜索失败回到空闲
        fromSearching.put(CarStatusEnum.DISCONNECTING, c -> true); // 可以断开连接
        stateTransitions.put(CarStatusEnum.SEARCHING, fromSearching);

        // 从 WAIT_NAV 状态的转换
        Map<CarStatusEnum, Predicate<Car>> fromWaitNav = new HashMap<>();
        fromWaitNav.put(CarStatusEnum.NAVIGATING, c -> true); // 开始导航
        fromWaitNav.put(CarStatusEnum.FREE, c -> true); // 导航取消回到空闲
        fromWaitNav.put(CarStatusEnum.DISCONNECTING, c -> true); // 可以断开连接
        stateTransitions.put(CarStatusEnum.WAIT_NAV, fromWaitNav);

        // 从 NAVIGATING 状态的转换
        Map<CarStatusEnum, Predicate<Car>> fromNavigating = new HashMap<>();
        fromNavigating.put(CarStatusEnum.RUNNING, c -> c.getCarPath() != null && !c.getCarPath().isEmpty()); // 导航完成后开始运行
        fromNavigating.put(CarStatusEnum.FREE, c -> true); // 导航失败回到空闲
        fromNavigating.put(CarStatusEnum.DISCONNECTING, c -> true); // 可以断开连接
        stateTransitions.put(CarStatusEnum.NAVIGATING, fromNavigating);

        // 从 WAITING 状态的转换
        Map<CarStatusEnum, Predicate<Car>> fromWaiting = new HashMap<>();
        fromWaiting.put(CarStatusEnum.RUNNING, c -> true); // 障碍物消除后继续运行
        fromWaiting.put(CarStatusEnum.FREE, c -> true); // 等待超时回到空闲
        fromWaiting.put(CarStatusEnum.DISCONNECTING, c -> true); // 可以断开连接
        stateTransitions.put(CarStatusEnum.WAITING, fromWaiting);

        // 从 DISCONNECTING 状态的转换
        Map<CarStatusEnum, Predicate<Car>> fromDisconnecting = new HashMap<>();
        fromDisconnecting.put(CarStatusEnum.FREE, c -> true); // 重新连接后回到空闲
        stateTransitions.put(CarStatusEnum.DISCONNECTING, fromDisconnecting);
    }

    /**
     * 尝试转换状态
     * 
     * @param targetState 目标状态
     * @return 是否转换成功
     */
    public boolean changeState(CarStatusEnum targetState) {
        CarStatusEnum currentState = car.getCarStatus();

        // 检查是否允许此状态转换
        if (!isTransitionAllowed(currentState, targetState)) {
            log.warn("State transition from {} to {} is not allowed for car {}",
                    currentState, targetState, car.getCarId());
            return false;
        }

        // 执行状态转换前的操作
        transitionHandler.beforeStateChange(car, currentState, targetState);

        // 更新状态
        car.setCarStatus(targetState);

        // 执行状态转换后的操作
        transitionHandler.afterStateChange(car, currentState, targetState);

        // 持久化状态
        persistState();

        log.info("Car {} state changed from {} to {}", car.getCarId(), currentState, targetState);
        return true;
    }

    /**
     * 检查状态转换是否允许
     */
    private boolean isTransitionAllowed(CarStatusEnum fromState, CarStatusEnum toState) {
        Map<CarStatusEnum, Predicate<Car>> allowedTransitions = stateTransitions.get(fromState);
        if (allowedTransitions == null) {
            return false;
        }

        Predicate<Car> condition = allowedTransitions.get(toState);
        return condition != null && condition.test(car);
    }

    /**
     * 持久化小车状态到黑板
     */
    public void persistState() {
        try {
            String carJson = objectMapper.writeValueAsString(car);
            blackboard.write("car:" + car.getCarId(), carJson);
            blackboard.expire("car:" + car.getCarId(), 1); // 1秒过期
        } catch (Exception e) {
            log.error("Failed to persist state for car {}", car.getCarId(), e);
        }
    }

    /**
     * 从黑板恢复小车状态
     */
    public boolean restoreState() {
        try {
            String carJson = blackboard.read("car:" + car.getCarId());
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
     * 减少状态计数器，如果计数器为0则回退到上一个状态
     */
    public void decrementStatusCounter() {
        int counter = car.getCarStatusCnt();
        if (counter > 0) {
            car.setCarStatusCnt(counter - 1);
            if (car.getCarStatusCnt() == 0) {
                // 状态计数器归零，需要回退状态
                handleStatusCounterZero();
            }
            persistState();
        }
    }

    /**
     * 处理状态计数器归零的情况
     */
    private void handleStatusCounterZero() {
        CarStatusEnum currentState = car.getCarStatus();

        switch (currentState) {
            case WAITING:
                // 等待超时，回到FREE状态
                changeState(CarStatusEnum.FREE);
                break;
            case SEARCHING:
                // 搜索超时，回到FREE状态
                changeState(CarStatusEnum.FREE);
                break;
            case NAVIGATING:
                // 导航超时，回到FREE状态
                changeState(CarStatusEnum.FREE);
                break;
            default:
                // 其他状态不处理
                break;
        }
    }
}