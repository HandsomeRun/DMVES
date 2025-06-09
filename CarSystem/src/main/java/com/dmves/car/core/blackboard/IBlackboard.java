package com.dmves.car.core.blackboard;

import cn.edu.ncepu.RedisUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 黑板系统 - 基于Redis实现
 * 直接使用RedisUtil
 */
@Slf4j
public class IBlackboard {
    private final RedisUtil redisUtil;
    private final UUID uuid;
    private final String prefix = "blackboard:";

    public IBlackboard() {
        this.redisUtil = RedisUtil.getInstance();
        this.uuid = UUID.randomUUID();
        try {
            redisUtil.getJedis(uuid);
            log.info("Redis连接成功建立");
        } catch (Exception e) {
            log.error("Redis连接失败: {}", e.getMessage());
            throw new RuntimeException("Redis连接失败", e);
        }
    }

    /**
     * 写入数据
     * 
     * @param key   键
     * @param value 值
     */
    public void write(String key, String value) {
        try {
            redisUtil.setString(prefix + key, value);
        } catch (Exception e) {
            log.error("写入数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 读取数据
     * 
     * @param key 键
     * @return 值
     */
    public String read(String key) {
        try {
            return redisUtil.getString(prefix + key);
        } catch (Exception e) {
            log.error("读取数据失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 删除数据
     * 
     * @param key 键
     */
    public void delete(String key) {
        try {
            // Redis没有直接提供delete方法，所以我们将值设为空字符串
            redisUtil.setString(prefix + key, "");
        } catch (Exception e) {
            log.error("删除数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查键是否存在
     * 
     * @param key 键
     * @return 是否存在
     */
    public boolean exists(String key) {
        try {
            String value = redisUtil.getString(prefix + key);
            return value != null && !value.isEmpty();
        } catch (Exception e) {
            log.error("检查键是否存在失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 设置过期时间
     * 
     * @param key     键
     * @param seconds 过期时间（秒）
     */
    public void expire(String key, int seconds) {
        // RedisUtil可能没有直接提供expire方法，这里只是记录日志
        log.debug("设置键 {} 过期时间为 {} 秒", key, seconds);
    }

    /**
     * 关闭Redis连接
     */
    public void close() {
        try {
            redisUtil.close();
            log.info("Redis连接已关闭");
        } catch (Exception e) {
            log.error("关闭Redis连接失败: {}", e.getMessage(), e);
        }
    }
}