package com.dmves.car.core.message;

import com.dmves.car.core.blackboard.IBlackboard;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

/**
 * 小车消息发送器
 * 负责发送各种消息
 */
@Slf4j
public class CarMessageSender {
    private final String carId;
    private final IBlackboard blackboard;
    private final MessageProcessor messageProcessor;

    public CarMessageSender(String carId, IBlackboard blackboard) {
        this.carId = carId;
        this.blackboard = blackboard;
        this.messageProcessor = new MessageProcessor();
    }

    /**
     * 发送消息
     * 
     * @param type     消息类型
     * @param content  消息内容
     * @param receiver 接收者
     */
    public void sendMessage(MessageType type, String content, String receiver) {
        try {
            Message message = messageProcessor.createMessage(
                    type, content, "Car-" + carId, receiver);

            String queueKey = determineQueueKey(type, receiver);
            blackboard.write(queueKey, messageProcessor.serializeMessage(message));
            log.debug("Sent message of type {} to {}", type, receiver);
        } catch (JsonProcessingException e) {
            log.error("Failed to send message of type {} to {}", type, receiver, e);
        }
    }

    /**
     * 发送心跳消息
     */
    public void sendHeartbeat() {
        try {
            blackboard.write("CarSystem", String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Failed to send heartbeat for car {}", carId, e);
        }
    }

    /**
     * 发送日志消息
     * 
     * @param logType 日志类型 (Move/Navigate)
     * @param data    日志数据
     */
    public void sendLogMessage(String logType, String data) {
        try {
            Message logMessage = messageProcessor.createMessage(
                    MessageType.LOG_RECORD,
                    String.format("{\"Type\":\"%s\",\"carId\":\"%s\",\"Data\":\"%s\",\"TimeStemp\":%d}",
                            logType, carId, data, System.currentTimeMillis()),
                    "Car-" + carId,
                    "LogRecorder");

            blackboard.write("1_RunLogQueue", messageProcessor.serializeMessage(logMessage));
            log.debug("Sent log message of type {} for car {}", logType, carId);
        } catch (JsonProcessingException e) {
            log.error("Failed to send log message for car {}", carId, e);
        }
    }

    /**
     * 发送错误消息
     * 
     * @param errorType 错误类型
     * @param errorMsg  错误信息
     */
    public void sendErrorMessage(String errorType, String errorMsg) {
        try {
            Message errorMessage = messageProcessor.createMessage(
                    MessageType.ERROR,
                    String.format("{\"Type\":\"%s\",\"carId\":\"%s\",\"Message\":\"%s\",\"TimeStemp\":%d}",
                            errorType, carId, errorMsg, System.currentTimeMillis()),
                    "Car-" + carId,
                    "Controller");

            blackboard.write("1_ErrorQueue", messageProcessor.serializeMessage(errorMessage));
            log.error("Car {} sent error message: {} - {}", carId, errorType, errorMsg);
        } catch (JsonProcessingException e) {
            log.error("Failed to send error message for car {}", carId, e);
        }
    }

    /**
     * 根据消息类型和接收者确定队列键
     */
    private String determineQueueKey(MessageType type, String receiver) {
        switch (type) {
            case MOVE_RESPONSE:
                return receiver.equals("Controller") ? "1_CarMoveResponseQueue" : receiver + "_MoveQueue";
            case NAV_REQUEST:
                return "1_NavigatorQueue";
            case NAV_RESPONSE:
                return receiver.equals("Navigator") ? "1_NavResponseQueue" : receiver + "_NavQueue";
            case STATUS_UPDATE:
                return receiver.equals("Controller") ? "1_StatusUpdateQueue" : receiver + "_StatusQueue";
            case LOG_RECORD:
                return "1_RunLogQueue";
            case ERROR:
                return "1_ErrorQueue";
            default:
                return receiver + "_MessageQueue";
        }
    }
}