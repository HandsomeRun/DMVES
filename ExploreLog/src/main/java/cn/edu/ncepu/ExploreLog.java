package cn.edu.ncepu;

import cn.edu.ncepu.Model.*;
import cn.edu.ncepu.Util.CosUtil;
import cn.edu.ncepu.Util.RedisUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbitmq.impl.Receiver;
import rabbitmq.interfaces.IReceiver;
import rabbitmq.interfaces.MessageHandler;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExploreLog {

    private static final UUID uuid = UUID.randomUUID();

    public static void main(String[] args) {

        RedisUtil redisUtil = RedisUtil.getInstance();
        try {
            redisUtil.getJedis(uuid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        IReceiver receiver = new Receiver();

        // 初始化
        receiver.initExchange(EXPLORE_EXCHANGE, Receiver.MQ_FANOUT);
        receiver.receiveBroadcastMessage(EXPLORE_EXCHANGE, new MessageHandler() {
            @Override
            public void handleMessage(String message) {
                System.out.println("Get message: " + message);
                ExploreMessage exploreMessage = ExploreMessage.getExploreMessage(message);
                switch (exploreMessage.getMsgType()) {
                    case "Start" -> {
                        filePath = redisUtil.getString("filePath");

                        System.out.print(exploreMessage.getMsgType());
                        System.out.println(filePath);

                        // 初始化append和logger
                        loggerInformation = addLogFileAndBindLogger(filePath + "information.log"
                                , LogNameEnum.INFORMATION);
                        loggerRunLog = addLogFileAndBindLogger(filePath + "runLog.log"
                                , LogNameEnum.RUN_LOG);
                        loggerAnalysisLog = addLogFileAndBindLogger(filePath + "analysisLog.log"
                                , LogNameEnum.ANALYSIS_LOG);

                        // 记录配置信息
                        int carNum = redisUtil.getIntByLock("carNum");
                        List<Car> cars = new ArrayList<>();
                        for (int i = 0; i < carNum; i++) {
                            cars.add(redisUtil.getCar(i + 1));
                        }
                        int mapHeight = redisUtil.getInt("mapHeight");
                        int mapWidth = redisUtil.getInt("mapWidth");
                        informationLog = new InformationLog(0
                                , mapHeight
                                , mapWidth
                                , redisUtil.getMap("mapBarrier", mapHeight, mapWidth)
                                , cars);
                        System.out.println("新实验开始！");
                    }
                    case "Run" -> {
                        long durationTime = Long.parseLong(exploreMessage.getMsgContent());
                        if (null != loggerRunLog) {
                            // 获取Redis中所有小车
                            int carNumber = redisUtil.getIntByLock("carNum");
                            List<Car> cars = new ArrayList<>();
                            for (int i = 0; i < carNumber; i++) {
                                cars.add(redisUtil.getCar(i + 1));
                            }

                            // 加读锁访问探索地图
                            redisUtil.waitRead("mapExplore");
                            String mapExplore = redisUtil.getString("mapExplore");
                            redisUtil.signalRead("mapExplore");

                            RunLog runLog = new RunLog(durationTime
                                    , mapExplore
                                    , cars);

                            // 记录序列化后的帧信息
                            loggerRunLog.info(runLog.toJson());
                            System.out.println("已记录一帧！");
                        }
                    }
                    case "End" -> {
                        // 完善配置信息
                        long durationTime = Long.parseLong(exploreMessage.getMsgContent());
                        informationLog.setExpDuration(durationTime);

                        // 记录序列化后的配置信息
                        loggerInformation.info(informationLog.toJson());
                        System.out.println("本次实验结束！");

                        // 上传所有日志
                        CosUtil.upload(filePath);
                        System.out.println("上传日志成功！");
                    }
                    case "Analysis" -> loggerAnalysisLog.info(exploreMessage.getMsgContent());
                }
            }
        });
    }

    /**
     * 添加append并与配置中的logger进行绑定
     *
     * @param filePath   log位置
     * @param loggerName logger名称
     * @return 绑定后的logger
     */
    private static Logger addLogFileAndBindLogger(String filePath, LogNameEnum loggerName) {
        // 获取当前上下文
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();

        // 创建 PatternLayout（输出格式）
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%msg%n")
                .build();

        // 创建 SizeBasedTriggeringPolicy
        SizeBasedTriggeringPolicy policy = SizeBasedTriggeringPolicy.createPolicy("10MB");

        // 创建 RollingFileAppender
        RollingFileAppender appender = RollingFileAppender.newBuilder()
                .setName("DynamicRollingFile")
                .setLayout(layout)
                .withFileName(filePath)
                .withFilePattern(filePath + ".%i.gz")
                .withPolicy(policy)
                .build();
        appender.start();

        // 将新创建的 Appender 添加到当前配置中
        config.addAppender(appender);

        // 删去 Logger 中原有的 Appender
        config.getLoggerConfig(loggerName.getDescription() + ".logger").removeAppender("DynamicRollingFile");

        // 创建 Logger 配置并将 Appender 与其关联
        config.getLoggerConfig(loggerName.getDescription() + ".logger").addAppender(appender, null, null);

        // 更新 Loggers
        ctx.updateLoggers();

        return LoggerFactory.getLogger(loggerName.getDescription() + ".logger");
    }

    /**
     * 读取指定路径下的AnalysisLog
     *
     * @param filePath 文件路径
     * @return 反序列化后的logs
     */
    private static List<AnalysisLog> readAnalysisLog(String filePath) {
        List<AnalysisLog> logs = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(AnalysisLog.getAnalysisLog(line));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return logs;
    }

    private final static String EXPLORE_EXCHANGE = "1.exploreLog.exchange";

    private static Logger loggerInformation;
    private static Logger loggerRunLog;
    private static Logger loggerAnalysisLog;
    private static String filePath;
    private static InformationLog informationLog;
}
