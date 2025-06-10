package com.dmves.car.core.message;

import com.dmves.car.core.blackboard.IBlackboard;
import com.dmves.car.core.component.CarComponent;
import com.dmves.car.core.model.CarStatusEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 小车消息处理器
 * 负责处理接收到的消息
 */
@Slf4j
public class CarMessageHandler {
    private final CarComponent carComponent;
    private final IBlackboard blackboard;
    private final MessageProcessor messageProcessor;
    private final ExecutorService executorService;
    private boolean isRunning;

    public CarMessageHandler(CarComponent carComponent, IBlackboard blackboard) {
        this.carComponent = carComponent;
        this.blackboard = blackboard;
        this.messageProcessor = new MessageProcessor();
        this.executorService = Executors.newSingleThreadExecutor();
        this.isRunning = false;
    }

    /**
     * 启动消息处理
     */
    public void start() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        executorService.submit(this::messageLoop);
        log.info("Message handler started for car {}", carComponent.getName());
    }

    /**
     * 停止消息处理
     */
    public void stop() {
        isRunning = false;
        executorService.shutdown();
        log.info("Message handler stopped for car {}", carComponent.getName());
    }

    /**
     * 消息循环，不断检查消息队列
     */
    private void messageLoop() {
        String queueKey = carComponent.getCarId() + "_CarMoveQueue";
        String navQueueKey = carComponent.getCarId() + "_NavigatorQueue";
        String targetQueueKey = carComponent.getCarId() + "_TargetQueue";
        String generalQueueKey = carComponent.getCarId() + "_MessageQueue";
        while (isRunning) {
            try {
                // 检查移动消息
                String moveMsg = blackboard.read(queueKey);
                if (moveMsg != null && !moveMsg.isEmpty()) {
                    handleMoveMessage(moveMsg);
                    blackboard.delete(queueKey);
                }

                // 检查导航消息
                String navMsg = blackboard.read(navQueueKey);
                if (navMsg != null && !navMsg.isEmpty()) {
                    handleNavMessage(navMsg);
                    blackboard.delete(navQueueKey);
                }

                // 检查目标消息
                String targetMsg = blackboard.read(targetQueueKey);
                if (targetMsg != null && !targetMsg.isEmpty()) {
                    handleTargetMessage(targetMsg);
                    blackboard.delete(targetQueueKey);
                }

                // 检查通用消息
                String generalMsg = blackboard.read(generalQueueKey);
                if (generalMsg != null && !generalMsg.isEmpty()) {
                    handleGeneralMessage(generalMsg);
                    blackboard.delete(generalQueueKey);
                }

                // 短暂休眠，避免CPU占用过高
                Thread.sleep(50);
            } catch (Exception e) {
                log.error("Error in message loop for car {}: {}", carComponent.getName(), e.getMessage());
            }
        }
    }

    /**
     * 处理移动消息
     */
    private void handleMoveMessage(String message) {
        try {
            Message msg = messageProcessor.deserializeMessage(message);
            log.debug("Received move message for car {}: {}", carComponent.getName(), msg.getContent());

            if (msg.getType() == MessageType.MOVE_REQUEST) {
                // 执行移动
                carComponent.move(msg.getContent());
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse move message for car {}: {}", carComponent.getName(), e.getMessage());
        }
    }

    /**
     * 处理导航消息
     */
    private void handleNavMessage(String message) {
        try {
            Message msg = messageProcessor.deserializeMessage(message);
            log.debug("Received nav message for car {}: {}", carComponent.getName(), msg.getContent());

            if (msg.getType() == MessageType.NAV_REQUEST) {
                // 更新状态为导航中
                carComponent.updateStatus(CarStatusEnum.NAVIGATING);
            } else if (msg.getType() == MessageType.NAV_RESPONSE) {
                // 收到导航结果
                String path = msg.getContent();
                if (path != null && !path.isEmpty()) {
                    // 执行路径
                    carComponent.executePath(path);
                } else {
                    // 导航失败
                    carComponent.updateStatus(CarStatusEnum.FREE);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse nav message for car {}: {}", carComponent.getName(), e.getMessage());
        }
    }

    /**
     * 处理目标消息
     */
    private void handleTargetMessage(String message) {
        try {
            Message msg = messageProcessor.deserializeMessage(message);
            log.debug("Received target message for car {}: {}", carComponent.getName(), msg.getContent());

            if (msg.getType() == MessageType.STATUS_QUERY) {
                // 更新状态为搜索中
                carComponent.updateStatus(CarStatusEnum.SEARCHING);

                // 解析目标位置并设置
                String[] parts = msg.getContent().split(",");
                if (parts.length >= 2) {
                    try {
                        int targetX = Integer.parseInt(parts[0].trim());
                        int targetY = Integer.parseInt(parts[1].trim());

                        // 设置目标位置
                        carComponent.updateTarget(targetX, targetY);
                        log.info("Set target position for car {} to ({},{})",
                                carComponent.getName(), targetX, targetY);
                    } catch (NumberFormatException e) {
                        log.error("Invalid target position format for car {}: {}",
                                carComponent.getName(), msg.getContent());
                        carComponent.updateStatus(CarStatusEnum.FREE);
                    }
                } else {
                    log.error("Invalid target message format for car {}: {}",
                            carComponent.getName(), msg.getContent());
                    carComponent.updateStatus(CarStatusEnum.FREE);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse target message for car {}: {}", carComponent.getName(), e.getMessage());
        }
    }

    /**
     * 处理通用消息
     */
    private void handleGeneralMessage(String message) {
        try {
            Message msg = messageProcessor.deserializeMessage(message);
            log.debug("Received general message for car {}: type={}, content={}",
                    carComponent.getName(), msg.getType(), msg.getContent());

            switch (msg.getType()) {
                case HEARTBEAT:
                    // 心跳消息由CarComponent自动处理
                    break;

                case STATUS_QUERY:
                    // 状态查询由CarComponent自动更新到Redis
                    break;

                case ERROR:
                    // 记录错误消息
                    log.error("Received error message for car {}: {}", carComponent.getName(), msg.getContent());
                    break;

                default:
                    log.warn("Unhandled message type {} for car {}", msg.getType(), carComponent.getName());
                    break;
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse general message for car {}: {}", carComponent.getName(), e.getMessage());
        }
    }
}