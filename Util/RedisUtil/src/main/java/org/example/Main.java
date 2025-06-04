package org.example;

import java.util.Arrays;

//RedisUtil使用示例
public class Main {
    public static void main(String[] args) {
        //example();  //使用示例代码见下
        testLock();
    }

    private static void testLock() {
        RedisUtil redisUtil = RedisUtil.getInstance();
        try {
            redisUtil.getJedis();
            redisUtil.setInt("carNumber_writeLock", 1);  //Redis中需要存在锁才能开始P V操作！
            redisUtil.setIntByLock("carNumber", 20);
            System.out.println(redisUtil.getIntByLock("carNumber"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void example() {
        //1.new对象（单例类）
        RedisUtil redisUtil = RedisUtil.getInstance();

        try {
            //2.连接Redis
            redisUtil.getJedis();

            //3.写Redis
            redisUtil.setInt("mapHeight", 15);
            redisUtil.setInt("mapWidth", 15);
            int[][] map = {
                    {0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 1},
                    {1, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0},
                    {1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 1},
                    {1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0},
                    {0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0},
                    {0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1},
                    {1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0},
                    {1, 1, 0, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1},
                    {1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0},
                    {0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0},
                    {0, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0},
                    {1, 1, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0},
                    {1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 1},
                    {1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1},
                    {0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0}
            };
            redisUtil.setMap("mapBarrier", map);

            //4.读Redis
            int[][] temp = redisUtil.getMap("mapBarrier", 15, 15);

            //5.关闭Redis连接
            redisUtil.close();

            for (int i = 0; i < 15; i++) {
                for (int j = 0; j < 15; j++) {
                    System.out.print(temp[i][j] + " ");
                }
                System.out.println();
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}