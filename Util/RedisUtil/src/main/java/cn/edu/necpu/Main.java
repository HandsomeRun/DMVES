package cn.edu.necpu;

import java.util.UUID;

//RedisUtil使用示例
public class Main {
    public static void main(String[] args) {
        UUID uuid = UUID.randomUUID();
        //example(uuid);  //使用示例代码见下
        testLock(uuid);
    }

    private static void testLock(UUID uuid) {
        RedisUtil redisUtil = RedisUtil.getInstance();
        try {
            redisUtil.getJedis(uuid);
            redisUtil.setInt("carNumber_writeLock", 1);  //Redis中需要存在锁才能开始P V操作！
            redisUtil.setIntByLock("carNumber", 20);
            System.out.println(redisUtil.getIntByLock("carNumber"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void example(UUID uuid) {
        //1.new对象（单例类）
        RedisUtil redisUtil = RedisUtil.getInstance();

        try {
            //2.连接Redis
            redisUtil.getJedis(uuid);

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