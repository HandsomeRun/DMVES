package com.dmves.car.config;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis配置类
 */
@Slf4j
public class RedisConfig {
    private static final String REDIS_HOST = "118.230.230.151";
    private static final int REDIS_PORT = 6379;
    private static JedisPool jedisPool;

    static {
        initializeJedisPool();
    }

    /**
     * 初始化Jedis连接池
     */
    private static void initializeJedisPool() {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);

            jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, 2000);
            log.info("Redis连接池初始化成功，连接到 {}:{}", REDIS_HOST, REDIS_PORT);
        } catch (Exception e) {
            log.error("Redis连接池初始化失败", e);
        }
    }

    /**
     * 获取Jedis连接池
     */
    public static JedisPool getJedisPool() {
        return jedisPool;
    }

    /**
     * 关闭Jedis连接池
     */
    public static void closeJedisPool() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            log.info("Redis连接池已关闭");
        }
    }
}