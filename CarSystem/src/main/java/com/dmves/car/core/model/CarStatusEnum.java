package com.dmves.car.core.model;

/**
 * 小车状态枚举
 */
public enum CarStatusEnum {
    DISCONNECTING("断开连接中"),
    FREE("空闲"),
    RUNNING("运行"),
    SEARCHING("寻找目标中"),
    WAIT_NAV("待导航"),
    NAVIGATING("导航中"),
    WAITING("遇到小车等待中"),
    OBSTACLE_DETECTED("检测到障碍"),
    RETURNING("回退中");

    private final String description;

    CarStatusEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}