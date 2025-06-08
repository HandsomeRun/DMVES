package com.dmves.car.core.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息处理器
 * 负责消息的序列化和反序列化
 */
@Slf4j
public class MessageProcessor {
    private final ObjectMapper objectMapper;

    public MessageProcessor() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 将消息对象序列化为JSON字符串
     *
     * @param message 消息对象
     * @return JSON字符串
     * @throws JsonProcessingException 序列化异常
     */
    public String serializeMessage(Message message) throws JsonProcessingException {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 将JSON字符串反序列化为消息对象
     *
     * @param json JSON字符串
     * @return 消息对象
     * @throws JsonProcessingException 反序列化异常
     */
    public Message deserializeMessage(String json) throws JsonProcessingException {
        try {
            return objectMapper.readValue(json, Message.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize message: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 创建消息对象
     *
     * @param type     消息类型
     * @param content  消息内容
     * @param sender   发送者
     * @param receiver 接收者
     * @return 消息对象
     */
    public Message createMessage(MessageType type, String content, String sender, String receiver) {
        Message message = new Message();
        message.setType(type);
        message.setContent(content);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
}