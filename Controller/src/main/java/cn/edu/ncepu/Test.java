package cn.edu.ncepu;

import cn.edu.ncepu.Model.Car;
import cn.edu.ncepu.Model.CarAlgorithmEnum;
import cn.edu.ncepu.Model.CarStatusEnum;
import cn.edu.ncepu.Util.RedisUtil;
import com.google.gson.Gson;
import rabbitmq.impl.Sender;

import java.awt.*;
import java.util.*;

public class Test {
    private static UUID uuid = UUID.randomUUID();

    public static void main(String[] args) {

        Gson gson = new Gson();
//        System.out.println(gson.toJson(new Car(1, CarStatusEnum.FREE, new Point(2, 3), null
//                , "ULLU", CarAlgorithmEnum.BFS, 3, "#F000FF"
//                , System.currentTimeMillis()),Car.class));

//        Car car = gson.fromJson("{\"carId\":1,\"CarStatus\":\"DISCONNECTING\",\"CarPosition\":{\"X\":1,\"Y\":1},\"CarTarget\":{\"X\":0,\"Y\":0},\"CarPath\":\"\",\"CarAlgorithm\":\"BFS\",\"CarStatusCnt\":0,\"CarColor\":\"#87BDFF\",\"CarLastRunTime\":0}", Car.class);

//        if (car == null) System.out.println(1);
//        else System.out.println(2);
//
//        System.exit(0);

        RedisUtil redisUtil = RedisUtil.getInstance();
        try {
            //2.连接Redis
            redisUtil.getJedis(uuid);

//            //3.写Redis
//            redisUtil.setInt("mapHeight", 15);
//            redisUtil.setInt("mapWidth", 15);
//            int[][] map = {
//                    {0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 1},
//                    {0, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0},
//                    {1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 1},
//                    {1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0},
//                    {0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0},
//                    {0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1},
//                    {1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0},
//                    {1, 1, 0, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1},
//                    {1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0},
//                    {0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0},
//                    {0, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0},
//                    {1, 1, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0},
//                    {1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 1},
//                    {1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1},
//                    {0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0}
//            };
//            redisUtil.setMap("mapBarrier", map);
//
//            map = new int[15][15];
//
//            for (int i = 0; i < 15; i++) {
//                for (int j = 0; j < 15; j++) {
//                    map[i][j] = 0;
//                }
//            }
//
//            redisUtil.setMap("mapExplore", map);

//            redisUtil.setInt("carNum", 1);
//            redisUtil.setCar(new Car(1, CarStatusEnum.RUNNING, new Point(0, 0), null
//                    , "RRRRRRRRRRRRRRR", CarAlgorithmEnum.BFS, 3, "#FF00F0"
//                    , System.currentTimeMillis()));

            System.out.println(redisUtil.getInt("mapHeight"));

            //Set<Integer> set = new HashSet<>();
            //set.add(1);
            //redisUtil.setDisConnectCars(set);

//            Sender sender = new Sender();
//            for (int i = 0; i < 30; i++) {
//                sender.DMVESSenderMessage(Sender.ControlName,Sender.CarName,"run");
//                sender.DMVESSenderMessage(Sender.ControlName,Sender.ViewName,"update");
//                System.out.println("Send a Message!");
//                Thread.sleep(1000);
//            }

//            redisUtil.setCar(new Car(2, CarStatusEnum.FREE, new Point(3, 3), null
//                    , "UURR", CarAlgorithmEnum.BFS, 3, "#FF0000"
//                    , System.currentTimeMillis()));
//
////            Sender sender = new Sender();
////            sender.initExchange("1.view.exchange" , Sender.MQ_FANOUT);
////            for(int i = 0 ; i < 10 ; i ++ ) {
////                sender.sendBroadcastMessage("1.view.exchange" , "update");
////                Thread.sleep(1000);
////            }
//
//            redisUtil.setIsWork("运行中");
//
//            Thread.sleep(3000);
//
//            redisUtil.setCar(new Car(1, CarStatusEnum.FREE, new Point(2, 3), null
//                    , "DULU", CarAlgorithmEnum.BFS, 3, "#FF00FF"
//                    , System.currentTimeMillis()));
//
//            Thread.sleep(3000);
//
//            redisUtil.setCar(new Car(1, CarStatusEnum.FREE, new Point(2, 3), null
//                    , "ULLU", CarAlgorithmEnum.BFS, 3, "#F000FF"
//                    , System.currentTimeMillis()));
//
//            Thread.sleep(3000);
//
//            redisUtil.setCar(new Car(1, CarStatusEnum.OK, new Point(2, 3), null
//                    , "RRUU", CarAlgorithmEnum.BFS, 3, "#0000FF"
//                    , System.currentTimeMillis()));
//
//            redisUtil.setCar(new Car(2, CarStatusEnum.OK, new Point(3, 3), null
//                    , "UURR", CarAlgorithmEnum.BFS, 3, "#FF0000"
//                    , System.currentTimeMillis()));

            System.out.println("已完成Test");
            //redisUtil.setIsWork("已完成");


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
