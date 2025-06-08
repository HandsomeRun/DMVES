package cn.edu.necpu;

import cn.edu.necpu.Model.ExploreMessage;
import com.rabbitmq.impl.Sender;
import com.rabbitmq.interfaces.ISender;

import java.awt.*;
import java.util.UUID;

public class Test {
    public static void main(String[] args) {
        UUID uuid = UUID.randomUUID();
        ISender sender = new Sender();
        sender.initExchange("exchange.ExploreLog", Sender.MQ_DIRECT);

        // 建立Redis连接
        RedisUtil redisUtil = RedisUtil.getInstance();
        try {
            redisUtil.getJedis(uuid);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // 初始化Redis中的内容
        redisUtil.setInt("mapHeight", 5);
        redisUtil.setInt("mapWidth", 5);
        redisUtil.setMap("mapBarrier", new int[][]{
                {0, 1, 0, 0, 1}
                , {0, 0, 0, 1, 1}
                , {1, 1, 0, 0, 1}
                , {0, 0, 0, 0, 0}
                , {1, 0, 1, 0, 0}});
        redisUtil.setMap("mapExplore", new int[][]{
                {0, 1, 0, 0, 1}
                , {1, 1, 1, 1, 1}
                , {1, 1, 1, 0, 1}
                , {0, 1, 1, 1, 0}
                , {1, 0, 1, 1, 0}});
        redisUtil.setInt("carNum", 3);
        redisUtil.setCar(new Car(1, CarStatusEnum.RUNNING, new Point(1, 1), new Point(3, 3)
                , "1243", CarAlgorithmEnum.BFS, 3, "rgb(255,0,255)"
                , System.currentTimeMillis()));
        redisUtil.setCar(new Car(2, CarStatusEnum.RUNNING, new Point(2, 2), new Point(4, 1)
                , "1243", CarAlgorithmEnum.BFS, 3, "rgb(255,0,255)"
                , System.currentTimeMillis()));
        redisUtil.setCar(new Car(3, CarStatusEnum.RUNNING, new Point(5, 6), new Point(1, 3)
                , "1243", CarAlgorithmEnum.BFS, 3, "rgb(255,0,255)"
                , System.currentTimeMillis()));

        long nowTime = System.currentTimeMillis();
        sender.sendFairMessage("exchange.ExploreLog"
                , "exploreLog.fair.routing.key"
                , new ExploreMessage("Start", String.valueOf(nowTime)).toJson());

        sender.sendFairMessage("exchange.ExploreLog"
                , "exploreLog.fair.routing.key"
                , new ExploreMessage("Run", String.valueOf(nowTime+500)).toJson());

        sender.sendFairMessage("exchange.ExploreLog"
                , "exploreLog.fair.routing.key"
                , new ExploreMessage("Run", String.valueOf(nowTime+1000)).toJson());

        sender.sendFairMessage("exchange.ExploreLog"
                , "exploreLog.fair.routing.key"
                , new ExploreMessage("End", String.valueOf(500001)).toJson());

    }
}
