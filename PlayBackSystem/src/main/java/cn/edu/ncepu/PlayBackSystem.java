package cn.edu.ncepu;

import cn.edu.necpu.Car;
import cn.edu.necpu.Model.RunLog;
import cn.edu.necpu.RedisUtil;
import com.google.gson.Gson;
import com.rabbitmq.impl.Sender;
import com.rabbitmq.interfaces.ISender;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PlayBackSystem {
    private static final UUID uuid = UUID.randomUUID();

    public static void main(String[] args) {
        // 建立Redis连接
        RedisUtil redisUtil = RedisUtil.getInstance();
        try {
            redisUtil.getJedis(uuid);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        //初始化Sender
        ISender sender = new Sender();
        sender.initExchange("exchange.View", Sender.MQ_FANOUT);

        //得到文件路径
        String filePath = "E:/大学/软件体系结构/第二次作业/DMVES/logs/2025-06-08-11_31_05" + "/runLog.log";

        List<RunLog> logs = readRunLog(filePath);

        while (true) {
            long targetTime = redisUtil.getTimeStamp("playbackTime");
            RunLog targetLog = findNearest(logs, targetTime);

            //如果不为空就写Redis，并且给View发消息
            if (targetLog != null) {
                updateRedis(redisUtil, targetLog);
                sender.sendBroadcastMessage("exchange.View", "update");
            }
        }

    }

    /**
     * 读取指定路径下的runLog
     *
     * @param filePath 文件路径
     * @return 反序列化后的logs
     */
    private static List<RunLog> readRunLog(String filePath) {
        List<RunLog> logs = new ArrayList<>();
        Gson gson = new Gson();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(gson.fromJson(line, RunLog.class));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return logs;
    }

    /**
     * 使用二分查找与指定时间最近的log
     *
     * @param logs       日志中所有logs
     * @param targetTime 目标时间
     * @return 最近的log
     */
    private static RunLog findNearest(List<RunLog> logs, long targetTime) {
        if (logs == null || logs.isEmpty()) {
            System.out.println("Log为空！");
            return null;
        }

        int low = 0;
        int high = logs.size() - 1;

        // 二分法
        while (low < high) {
            int mid = (low + high) / 2;
            if (logs.get(mid).getTimeStamp() == targetTime) return logs.get(mid);
            else if (logs.get(mid).getTimeStamp() < targetTime) low = mid + 1;
            else high = mid;
        }

        // 寻找最近的那个时间戳
        if (low == 0) return logs.get(low);
        if (Math.abs(logs.get(low).getTimeStamp() - targetTime) <
                Math.abs(logs.get(low - 1).getTimeStamp() - targetTime)) {
            return logs.get(low);
        } else {
            return logs.get(low - 1);
        }
    }

    /**
     * 将runLog中的内容更新到Redis
     *
     * @param redisUtil Redis通信工具
     * @param runLog    指定RunLog
     */
    private static void updateRedis(RedisUtil redisUtil, RunLog runLog) {
        String mapExplore = runLog.getMapExplore();
        List<Car> cars = runLog.getCars();

        redisUtil.setString("mapExplore", mapExplore);
        for (Car carTemp : cars) {
            redisUtil.setCar(carTemp);
        }
    }
}
