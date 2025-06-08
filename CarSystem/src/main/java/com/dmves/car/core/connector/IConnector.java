package com.dmves.car.core.connector;

import com.dmves.car.core.message.IMessage;

/**
 * C2架构的基础连接器接口
 */
public interface IConnector {
    /**
     * 处理消息
     * 
     * @param message 消息对象
     */
    void handleMessage(IMessage message);

    /**
     * 发送消息
     * 
     * @param message 消息对象
     */
    void sendMessage(IMessage message);

    /**
     * 获取连接器名称
     */
    String getName();
}