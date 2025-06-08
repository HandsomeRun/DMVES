package com.dmves.car.core.connector;

import com.dmves.car.config.RedisConfig;
import com.dmves.car.core.model.Point;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis连接器
 * 用于与Redis服务器交互，存储和检索小车位置信息
 */
@Slf4j
public class RedisConnector {
    private static final String CAR_POSITION_PREFIX = "car:position:";
    private static final String CAR_IDS_KEY = "car:ids";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 测试模式标志
    private static boolean testMode = false;

    /**
     * 设置测试模式
     * 
     * @param isTestMode 是否启用测试模式
     */
    public static void setTestMode(boolean isTestMode) {
        testMode = isTestMode;
        log.info("Redis连接器测试模式: {}", testMode ? "已启用" : "已禁用");
    }

    /**
     * 是否处于测试模式
     */
    public static boolean isTestMode() {
        return testMode;
    }

    /**
     * 更新小车位置
     *
     * @param carId    小车ID
     * @param position 小车位置
     * @return 是否更新成功
     */
    public static boolean updateCarPosition(String carId, Point position) {
        // 测试模式下，使用测试桩
        if (testMode) {
            return MockRedisConnector.updateCarPosition(carId, position);
        }

        JedisPool jedisPool = RedisConfig.getJedisPool();
        if (jedisPool == null) {
            log.error("Redis连接池未初始化");
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            // 创建位置信息Map
            Map<String, Integer> positionMap = new HashMap<>();
            positionMap.put("x", position.getX());
            positionMap.put("y", position.getY());

            // 将位置信息转换为JSON
            String positionJson = objectMapper.writeValueAsString(positionMap);

            // 存储小车位置
            jedis.set(CAR_POSITION_PREFIX + carId, positionJson);

            // 将小车ID添加到集合中
            jedis.sadd(CAR_IDS_KEY, carId);

            log.debug("已更新小车 {} 的位置到Redis: ({}, {})", carId, position.getX(), position.getY());
            return true;
        } catch (Exception e) {
            log.error("更新小车位置到Redis失败", e);
            return false;
        }
    }

    /**
     * 检查指定位置是否有其他小车
     *
     * @param currentCarId 当前小车ID
     * @param x            目标X坐标
     * @param y            目标Y坐标
     * @return 是否有其他小车
     */
    public static boolean hasCarAtPosition(String currentCarId, int x, int y) {
        // 测试模式下，使用测试桩
        if (testMode) {
            return MockRedisConnector.hasCarAtPosition(currentCarId, x, y);
        }

        JedisPool jedisPool = RedisConfig.getJedisPool();
        if (jedisPool == null) {
            log.error("Redis连接池未初始化");
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            // 获取所有小车ID
            for (String carId : jedis.smembers(CAR_IDS_KEY)) {
                // 跳过当前小车
                if (carId.equals(currentCarId)) {
                    continue;
                }

                // 获取小车位置
                String positionJson = jedis.get(CAR_POSITION_PREFIX + carId);
                if (positionJson != null) {
                    Map<String, Integer> position = objectMapper.readValue(positionJson, Map.class);

                    // 检查位置是否匹配
                    if (position.get("x") == x && position.get("y") == y) {
                        log.debug("在位置 ({}, {}) 发现小车 {}", x, y, carId);
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            log.error("从Redis检查小车位置失败", e);
            return false;
        }
    }

    /**
     * 移除小车位置信息
     *
     * @param carId 小车ID
     * @return 是否移除成功
     */
    public static boolean removeCarPosition(String carId) {
        // 测试模式下，使用测试桩
        if (testMode) {
            return MockRedisConnector.removeCarPosition(carId);
        }

        JedisPool jedisPool = RedisConfig.getJedisPool();
        if (jedisPool == null) {
            log.error("Redis连接池未初始化");
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            // 删除小车位置
            jedis.del(CAR_POSITION_PREFIX + carId);

            // 从集合中移除小车ID
            jedis.srem(CAR_IDS_KEY, carId);

            log.debug("已从Redis移除小车 {} 的位置信息", carId);
            return true;
        } catch (Exception e) {
            log.error("从Redis移除小车位置失败", e);
            return false;
        }
    }

    /**
     * 测试用的模拟Redis连接器
     */
    public static class MockRedisConnector {
        private static final Map<String, Point> carPositions = new HashMap<>();

        public static void reset() {
            carPositions.clear();
        }

        public static boolean updateCarPosition(String carId, Point position) {
            carPositions.put(carId, position);
            log.debug("[测试模式] 已更新小车 {} 的位置: ({}, {})", carId, position.getX(), position.getY());
            return true;
        }

        public static boolean hasCarAtPosition(String currentCarId, int x, int y) {
            for (Map.Entry<String, Point> entry : carPositions.entrySet()) {
                if (!entry.getKey().equals(currentCarId)) {
                    Point pos = entry.getValue();
                    if (pos.getX() == x && pos.getY() == y) {
                        log.debug("[测试模式] 在位置 ({}, {}) 发现小车 {}", x, y, entry.getKey());
                        return true;
                    }
                }
            }
            return false;
        }

        public static boolean removeCarPosition(String carId) {
            carPositions.remove(carId);
            log.debug("[测试模式] 已移除小车 {} 的位置信息", carId);
            return true;
        }
    }
}