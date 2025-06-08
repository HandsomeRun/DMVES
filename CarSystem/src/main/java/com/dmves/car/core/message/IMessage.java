package com.dmves.car.core.message;

/**
 * 消息接口
 */
public interface IMessage {
    /**
     * 获取消息类型
     */
    MessageType getType();

    /**
     * 获取消息内容
     */
    String getContent();

    /**
     * 获取消息发送者
     */
    String getSender();

    /**
     * 获取消息接收者
     */
    String getReceiver();
}