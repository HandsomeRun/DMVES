package com.dmves.car.core.blackboard;

/**
 * 黑板接口 - 基于Redis实现
 */
public interface IBlackboard {
    /**
     * 写入数据
     * 
     * @param key   键
     * @param value 值
     */
    void write(String key, String value);

    /**
     * 读取数据
     * 
     * @param key 键
     * @return 值
     */
    String read(String key);

    /**
     * 删除数据
     * 
     * @param key 键
     */
    void delete(String key);

    /**
     * 检查键是否存在
     * 
     * @param key 键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 设置过期时间
     * 
     * @param key     键
     * @param seconds 过期时间（秒）
     */
    void expire(String key, int seconds);
}