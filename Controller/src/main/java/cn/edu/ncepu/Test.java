package cn.edu.ncepu;

import cn.edu.ncepu.Model.Car;
import cn.edu.ncepu.Model.CarAlgorithmEnum;
import cn.edu.ncepu.Model.CarStatusEnum;

import java.awt.*;
import java.util.UUID;

public class Test {
    private static UUID uuid = UUID.randomUUID();

    public static void main(String[] args) {
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

            map = new int[15][15];

            for (int i = 0; i < 15; i++) {
                for (int j = 0; j < 15; j++) {
                    map[i][j] = 0;
                }
            }

            redisUtil.setMap("mapExplore", map);

            redisUtil.setInt("carNum", 1);
            redisUtil.setCar(new Car(1, CarStatusEnum.FREE, new Point(0, 0), null
                    , null, CarAlgorithmEnum.BFS, 3, "rgb(255,0,255)"
                    , System.currentTimeMillis()));

//            Sender sender = new Sender();
//            sender.initExchange("1.view.exchange" , Sender.MQ_FANOUT);
//            for(int i = 0 ; i < 10 ; i ++ ) {
//                sender.sendBroadcastMessage("1.view.exchange" , "update");
//                Thread.sleep(1000);
//            }

            redisUtil.setIsWork("运行中");

            Thread.sleep(100000);

            redisUtil.setIsWork("已完成");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
