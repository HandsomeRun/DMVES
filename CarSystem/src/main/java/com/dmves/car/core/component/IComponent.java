package com.dmves.car.core.component;

/**
 * C2架构的基础组件接口
 */
public interface IComponent {
    /**
     * 初始化组件
     */
    void initialize();

    /**
     * 启动组件
     */
    void start();

    /**
     * 停止组件
     */
    void stop();

    /**
     * 获取组件名称
     */
    String getName();
}