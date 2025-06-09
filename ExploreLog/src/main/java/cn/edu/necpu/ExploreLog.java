package cn.edu.necpu;

import cn.edu.necpu.Model.ExploreMessage;
import cn.edu.necpu.Model.InformationLog;
import cn.edu.necpu.Model.LogNameEnum;
import cn.edu.necpu.Model.RunLog;
import com.rabbitmq.impl.Receiver;
import com.rabbitmq.interfaces.MessageHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

        Receiver receiver = new Receiver();
        final Logger[] loggerInformation = {null};
        final Logger[] loggerRunLog = {null};
        final Logger[] loggerAnalysisLog = {null};
        final String[] filePath = new String[1];
        final InformationLog[] informationLog = {null};

        String exchange = "exchange.ExploreLog";
        String queueName = "explore.queue";

        // 初始化
        receiver.initExchange(exchange, Receiver.MQ_DIRECT);
        receiver.initQueue(queueName);
        receiver.bindQueueToExchange(queueName, exchange, "exploreLog.fair.routing.key");
        receiver.receiveFairMessage(exchange, queueName, new MessageHandler() {
            @Override
            public void handleMessage(String message) {

                ExploreMessage exploreMessage = ExploreMessage.getExploreMessage(message);
                switch (exploreMessage.getMsgType()) {
                    case "Start" -> {
                        long startTime = Long.parseLong(exploreMessage.getMsgContent());
                        String formattedDate = new SimpleDateFormat("yyyy-MM-dd-HH_mm_ss")
                                .format(new Date(startTime));
                        filePath[0] = "logs/" + formattedDate + "/";

                        // 初始化append和logger
                        loggerInformation[0] = addLogFileAndBindLogger(filePath[0] + "information.log"
                                , LogNameEnum.INFORMATION);
                        loggerRunLog[0] = addLogFileAndBindLogger(filePath[0] + "runLog.log"
                                , LogNameEnum.RUN_LOG);
                        loggerAnalysisLog[0] = addLogFileAndBindLogger(filePath[0] + "analysisLog.log"
                                , LogNameEnum.ANALYSIS_LOG);

                        // 记录配置信息
                        int carNum = redisUtil.getIntByLock("carNum");
                        List<Car> cars = new ArrayList<>();
                        for (int i = 0; i < carNum; i++) {
                            cars.add(redisUtil.getCar(i + 1));
                        }
                        int mapHeight = redisUtil.getInt("mapHeight");
                        int mapWidth = redisUtil.getInt("mapWidth");
                        informationLog[0] = new InformationLog(0
                                , mapHeight
                                , mapWidth
                                , redisUtil.getMap("mapBarrier", mapHeight, mapWidth)
                                , cars);
                    }
                    case "Run" -> {
                        long durationTime = Long.parseLong(exploreMessage.getMsgContent());
                        if (null != loggerRunLog[0]) {
                            // 获取Redis中所有小车
                            int carNumber = redisUtil.getIntByLock("carNum");
                            List<Car> cars = new ArrayList<>();
                            for (int i = 0; i < carNumber; i++) {
                                cars.add(redisUtil.getCar(i + 1));
                            }

                            // 加读锁访问探索地图
                            redisUtil.waitRead(RedisUtil.getGroupId() + "_mapExplore_readLock");
                            String mapExplore = redisUtil.getString("mapExplore");
                            redisUtil.signalRead(RedisUtil.getGroupId() + "_mapExplore_readLock");

                            RunLog runLog = new RunLog(durationTime
                                    , mapExplore
                                    , cars);

                            // 记录序列化后的帧信息
                            loggerRunLog[0].info(runLog.toJson());
                        }


                    }
                    case "End" -> {
                        // 完善配置信息
                        long durationTime = Long.parseLong(exploreMessage.getMsgContent());
                        informationLog[0].setExpDuration(durationTime);

                        // 记录序列化后的配置信息
                        loggerInformation[0].info(informationLog[0].toJson());
                    }
                    case "Analysis" -> loggerAnalysisLog[0].info(exploreMessage.getMsgContent());
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

        return LoggerFactory.getLogger(loggerName.getDescription());
    }
}
