package com.dmves.car.core.message;

/**
 * 消息类型枚举
 */
public enum MessageType {
    // 心跳消息
    HEARTBEAT,

    // 移动相关消息
    MOVE_REQUEST,
    MOVE_RESPONSE,

    // 导航相关消息
    NAV_REQUEST,
    NAV_RESPONSE,

    // 状态相关消息
    STATUS_UPDATE,
    STATUS_QUERY,

    // 日志相关消息
    LOG_RECORD,

    // 错误消息
    ERROR
}