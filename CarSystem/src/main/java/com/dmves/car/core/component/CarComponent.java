package com.dmves.car.core.component;

import com.dmves.car.core.blackboard.IBlackboard;
import com.dmves.car.core.model.Car;
import com.dmves.car.core.model.CarStatusEnum;
import com.dmves.car.core.model.Point;
import com.dmves.car.core.movement.MovementController;
import com.dmves.car.core.state.CarStateManager;
import com.dmves.car.core.message.CarMessageHandler;
import com.dmves.car.core.message.CarMessageSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

/**
 * 小车组件
 */
@Slf4j
public class CarComponent implements IComponent {
    private final String carId;
    @SuppressWarnings("unused")
    private final IBlackboard blackboard;
    private final ObjectMapper objectMapper;
    private final Car car;
    private final CarStateManager stateManager; // 小车状态管理器
    private final MovementController movementController; // 移动控制器
    private final CarMessageHandler messageHandler; // 消息处理器
    private final CarMessageSender messageSender; // 消息发送器
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> heartbeatFuture;
    private ScheduledFuture<?> statusCounterFuture;

    public CarComponent(String carId, IBlackboard blackboard) {
        this.carId = carId;
        this.blackboard = blackboard;
        this.objectMapper = new ObjectMapper();

        // 初始化小车对象
        this.car = Car.builder()
                .carId(carId)
                .carStatus(CarStatusEnum.FREE)
                .carPosition(new Point(0, 0)) // 默认位置
                .carStatusCnt(0)
                .carLastRunTime(System.currentTimeMillis())
                .carPath("")
                .carColor("blue") // 默认颜色
                .build();

        // 初始化状态管理器和移动控制器
        this.stateManager = new CarStateManager(car, blackboard);
        this.movementController = new MovementController(car, blackboard);

        // 初始化消息处理器和发送器
        this.messageSender = new CarMessageSender(carId, blackboard);
        this.messageHandler = new CarMessageHandler(this, blackboard);
    }

    @Override
    public void initialize() {
        scheduler = Executors.newScheduledThreadPool(2);
        log.info("小车组件 {} 已初始化", carId);

        // 尝试从黑板恢复状态
        if (!stateManager.restoreState()) {
            // 如果无法恢复，则持久化当前状态
            stateManager.persistState();
        }
    }

    @Override
    public void start() {
        // 启动心跳任务
        startHeartbeat();

        // 启动状态计数器检查任务
        startStatusCounterCheck();

        // 启动消息处理器
        messageHandler.start();

        log.info("小车组件 {} 已启动", carId);
    }

    @Override
    public void stop() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }

        if (statusCounterFuture != null) {
            statusCounterFuture.cancel(true);
        }

        if (scheduler != null) {
            scheduler.shutdown();
        }

        // 停止移动
        movementController.stop();

        // 停止消息处理器
        messageHandler.stop();

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

    private void startHeartbeat() {
        heartbeatFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                car.setCarLastRunTime(System.currentTimeMillis());
                stateManager.persistState();
                messageSender.sendHeartbeat();
            } catch (Exception e) {
                log.error("小车 {} 的心跳任务出错", carId, e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void startStatusCounterCheck() {
        statusCounterFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                stateManager.decrementStatusCounter();
            } catch (Exception e) {
                log.error("小车 {} 的状态计数器检查任务出错", carId, e);
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * 更新位置
     */
    public void updatePosition(int x, int y) {
        car.setCarPosition(new Point(x, y));
        stateManager.persistState();

        // 记录移动日志
        messageSender.sendLogMessage("移动", String.format("位置已更新到 (%d,%d)", x, y));
    }

    /**
     * 更新路径
     */
    public void updatePath(String path) {
        car.setCarPath(path);
        stateManager.persistState();

        // 记录路径日志
        messageSender.sendLogMessage("导航", "路径已更新为 " + path);
    }

    /**
     * 更新目标
     */
    public void updateTarget(int x, int y) {
        car.setCarTarget(new Point(x, y));
        stateManager.persistState();

        // 记录目标日志
        messageSender.sendLogMessage("目标", String.format("目标已更新到 (%d,%d)", x, y));
    }

    /**
     * 更新状态
     */
    public boolean updateStatus(CarStatusEnum status) {
        boolean result = stateManager.changeState(status);
        if (result) {
            // 记录状态变更日志
            messageSender.sendLogMessage("状态", "状态已变更为 " + status.name());
        }
        return result;
    }

    /**
     * 执行移动
     */
    public boolean move(String direction) {
        boolean result = movementController.move(direction);
        if (result) {
            // 记录移动日志
            messageSender.sendLogMessage("移动", "已向 " + direction + " 方向移动");
        } else {
            // 发送错误消息
            messageSender.sendErrorMessage("移动错误", "向 " + direction + " 方向移动失败");
        }
        return result;
    }

    /**
     * 执行路径
     */
    public boolean executePath(String path) {
        updatePath(path);
        boolean result = movementController.executePath(path);
        if (result) {
            // 记录路径执行日志
            messageSender.sendLogMessage("导航", "正在执行路径 " + path);
        } else {
            // 发送错误消息
            messageSender.sendErrorMessage("导航错误", "执行路径 " + path + " 失败");
        }
        return result;
    }

    /**
     * 获取当前小车状态
     */
    public CarStatusEnum getCurrentStatus() {
        return car.getCarStatus();
    }

    /**
     * 获取小车对象
     */
    public Car getCar() {
        return car;
    }

    /**
     * 获取消息发送器
     */
    public CarMessageSender getMessageSender() {
        return messageSender;
    }
}