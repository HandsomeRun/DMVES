package cn.edu.ncepu;

import cn.edu.ncepu.Model.Car;
import cn.edu.ncepu.Model.RunLog;
import cn.edu.ncepu.Util.CosUtil;
import cn.edu.ncepu.Util.RedisUtil;
import com.rabbitmq.impl.Sender;
import com.rabbitmq.interfaces.ISender;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
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

        String filePath = ""; /*= "E:/大学/软件体系结构/第二次作业/DMVES/logs/2025-06-08-11_31_05" + "/runLog.log";*/
        long startTime = -1;
        List<RunLog> runLogs = new ArrayList<>();

        int index = 0;  //用来记录回放到第几帧
        while (true) {
            long sleepTime = 300;
            if ("回放中".equals(redisUtil.getIsWork())) {
                long nowTime = System.currentTimeMillis();
                String carRunLogName = redisUtil.getString("carRunLogName");
                String playbackTime = redisUtil.getString("playbackTime");
                redisUtil.setString("playbackTime", "");

                // runLogs为空 或者 更换了路径
                if (runLogs.isEmpty() || !filePath.equals(carRunLogName)) {
                    startTime = System.currentTimeMillis();
                    filePath = carRunLogName;
                    runLogs = readRunLog(filePath);
                }

                if (!playbackTime.isEmpty()) {
                    index = findNearestAfter(runLogs, Long.parseLong(playbackTime));
                }

                // 如果不为空就写Redis，并且给View发消息
                if (index != -1 && nowTime - startTime > runLogs.get(index).getTimeStamp()) {
                    updateRedis(redisUtil, runLogs.get(index++));
                    sender.DMVESSenderMessage(Sender.ControlName, Sender.ViewName, "update");
                }
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

        try (BufferedReader reader = CosUtil.readRunLog(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(RunLog.getRunLog(line));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return logs;
    }

    /**
     * 找到时间 >= targetTime 的第一条日志的索引
     *
     * @param logs       日志列表，按时间戳升序排列
     * @param targetTime 目标时间
     * @return 第一个时间大于等于 targetTime 的日志索引；如果全部小于 targetTime，返回 -1
     */
    private static int findNearestAfter(List<RunLog> logs, long targetTime) {
        if (logs == null || logs.isEmpty()) {
            System.out.println("Log为空！");
            return -1;
        }

        int low = 0;
        int high = logs.size() - 1;

        // 如果所有日志的时间都小于 targetTime，则没有符合条件的
        if (logs.get(high).getTimeStamp() < targetTime) {
            return -1;
        }

        int ans = -1;

        while (low <= high) {
            int mid = (low + high) / 2;

            if (logs.get(mid).getTimeStamp() >= targetTime) {
                ans = mid;     // 先记录候选答案
                high = mid - 1; // 继续往左找更小的满足条件的 index
            } else {
                low = mid + 1;
            }
        }

        return ans;
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

        redisUtil.waitWrite("mapExplore");
        redisUtil.setString("mapExplore", mapExplore);
        redisUtil.signalWrite("mapExplore");
        for (Car carTemp : cars) {
            redisUtil.setCar(carTemp);
        }
    }
}
