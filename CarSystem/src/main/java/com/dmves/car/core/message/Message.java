package com.dmves.car.core.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息基类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message implements IMessage {
    private MessageType type;
    private String content;
    private String sender;
    private String receiver;
    private long timestamp;

    @Override
    public MessageType getType() {
        return type;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public String getSender() {
        return sender;
    }

    @Override
    public String getReceiver() {
        return receiver;
    }
}