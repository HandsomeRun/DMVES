package cn.edu.necpu;

import cn.edu.necpu.Model.ExploreMessage;
import cn.edu.necpu.Model.InformationLog;
import cn.edu.necpu.Model.LoggerNameEnum;
import cn.edu.necpu.Model.RunLog;
import com.google.gson.Gson;
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
                                , LoggerNameEnum.INFORMATION);
                        loggerRunLog[0] = addLogFileAndBindLogger(filePath[0] + "runLog.log"
                                , LoggerNameEnum.RUN_LOG);
                        loggerAnalysisLog[0] = addLogFileAndBindLogger(filePath[0] + "analysisLog.log"
                                , LoggerNameEnum.ANALYSIS_LOG);

                        //记录配置信息
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
                            int carNumber = redisUtil.getIntByLock("carNum");
                            List<Car> cars = new ArrayList<>();
                            for (int i = 0; i < carNumber; i++) {
                                cars.add(redisUtil.getCar(i + 1));
                            }

                            redisUtil.waitRead(RedisUtil.getGroupId() + "_mapExplore_readLock");
                            String mapExplore = redisUtil.getString("mapExplore");
                            redisUtil.signalRead(RedisUtil.getGroupId() + "_mapExplore_readLock");

                            RunLog runLog = new RunLog(durationTime
                                    , mapExplore
                                    , cars);

                            Gson gson = new Gson();
                            loggerRunLog[0].info(gson.toJson(runLog));
                        }


                    }
                    case "End" -> {
                        long durationTime = Long.parseLong(exploreMessage.getMsgContent());
                        informationLog[0].setExpDuration(durationTime);

                        Gson gson = new Gson();
                        loggerInformation[0].info(gson.toJson(informationLog[0]));
                    }
                    case "Analysis" -> {
                        //记录导航器给的MQ
                    }
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
    private static Logger addLogFileAndBindLogger(String filePath, LoggerNameEnum loggerName) {
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
        config.getLoggerConfig(loggerName.getDescription()).removeAppender("DynamicRollingFile");

        // 创建 Logger 配置并将 Appender 与其关联
        config.getLoggerConfig(loggerName.getDescription()).addAppender(appender, null, null);

        // 更新 Loggers
        ctx.updateLoggers();

        return LoggerFactory.getLogger(loggerName.getDescription());
    }
}
