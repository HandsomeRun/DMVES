package cn.edu.ncepu.HandsomeRun.Navigator;

import cn.edu.ncepu.Car.Car;
import cn.edu.ncepu.Car.CarStatusEnum;
import cn.edu.ncepu.Car.CarAlgorithmEnum;
import cn.edu.ncepu.Redis.RedisUtil;
import cn.edu.necpu.Model.ExploreMessage;
import com.google.gson.Gson;
import com.rabbitmq.impl.Receiver;
import com.rabbitmq.interfaces.MessageHandler;
import com.rabbitmq.impl.Sender;
import cn.edu.necpu.Model.AnalysisLog;

import java.awt.Point;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NavigatorMain {
    private static final String REDIS_NAVIGATOR_KEY = "Navigator";
    private static final int SCHEDULE_PERIOD = 5; // 秒
    private static final int SUB_MAP_SIZE = 20;
    private static final String EXCHANGE_NAME = "1.navigator.exchange";
    private static final String QUEUE_NAME = "1.navigator.queue";
    private static final String ROUTING_KEY = "1.navigator.routing.key";
    private static final String NAVI_ANALYSIS_LOG_EXCHANGE = "1.exploreLog.exchange";
    private static RedisUtil redisUtil;
    private static Sender analysisLogSender;

    public static void main(String[] args) {
        // 初始化 Redis 单例
        redisUtil = RedisUtil.getInstance();
        try {
            UUID uuid = UUID.randomUUID();
            redisUtil.getJedis(uuid);
            System.out.println("[Navigator] Redis 连接初始化成功");
        } catch (Exception e) {
            System.err.println("[Navigator] Redis 初始化失败: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        // 初始化分析日志Sender
        analysisLogSender = new Sender();
        analysisLogSender.initExchange(NAVI_ANALYSIS_LOG_EXCHANGE, Sender.MQ_FANOUT);
        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (analysisLogSender != null) {
                    analysisLogSender.close();
                    System.out.println("[Navigator] 分析日志Sender已关闭");
                }
            } catch (Exception e) {
                System.err.println("[Navigator] 关闭Sender时出错: " + e.getMessage());
            }
        }));
        // 启动定时线程
        startScheduleThread();
        // 启动 MQ 监听
        startMQListener();
    }

    // 定时线程任务
    private static void startScheduleThread() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> writeNavigatorTimestamp(), 0, SCHEDULE_PERIOD, TimeUnit.SECONDS);
        System.out.println("[Navigator] 定时线程已启动，每" + SCHEDULE_PERIOD + "秒写入心跳");
    }

    // MQ 监听任务
    private static void startMQListener() {
        Receiver receiver = new Receiver();
        receiver.initExchange(EXCHANGE_NAME, Receiver.MQ_DIRECT);
        receiver.initQueue(QUEUE_NAME);
        receiver.bindQueueToExchange(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
        receiver.receiveFairMessage(EXCHANGE_NAME, QUEUE_NAME, new MessageHandler() {
            @Override
            public void handleMessage(String message) {
                System.out.println("[Navigator] 收到MQ消息: " + message);
                handleCarMessage(message);
            }
        });
        System.out.println("[Navigator] MQ监听已启动");
    }

    // 3.1 定时写入Redis
    private static void writeNavigatorTimestamp() {
        long now = System.currentTimeMillis();
        redisUtil.setTimeStamp(REDIS_NAVIGATOR_KEY, now);
        System.out.println("[Navigator] 心跳写入Redis: " + now);
    }

    // 3.2 主线程处理消息
    private static void handleCarMessage(String message) {
        // 3.2.1 解析消息，获取Car
        Car car = getCarFromMessage(message);
        if (car == null) {
            System.err.println("[Navigator] 解析Car失败，消息: " + message);
            return;
        }
        System.out.println("[Navigator] 处理Car: " + car.getCarId());
        System.out.println(car.toString());
        // 3.2.2 获取并合成地图
        MapData mapData = getMergedMap();
        if (mapData == null) {
            System.err.println("[Navigator] 获取地图失败");
            return;
        }
        // 3.2.3 地图划分与定位
        SubMapInfo subMapInfo = locateCarAndTarget(car, mapData);
        if (subMapInfo == null) {
            System.err.println("[Navigator] 地图划分失败");
            return;
        }
        // 3.2.4 判断目标位置，确定mapSearch
        MapSearchInfo mapSearchInfo = getMapSearch(car, subMapInfo, mapData);
        if (mapSearchInfo == null) {
            System.err.println("[Navigator] 搜索区域确定失败");
            return;
        }
        // 3.2.5 选择算法并寻路
        String path = calculatePath(car, mapSearchInfo, mapData);
        if (path == null || path.isEmpty()) {
            System.err.println("[Navigator] 未找到路径");
            return;
        }
        // 3.2.6 写回Redis
        updateCarStatusAndPath(car, path);
    }

    // 3.2.1 解析消息，获取Car
    private static Car getCarFromMessage(String message) {
        try {
            Gson gson = new Gson();
            Map<String, Object> map = gson.fromJson(message, Map.class);
            int carId = Integer.parseInt(map.get("carId").toString());
            return redisUtil.getCar(carId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 3.2.2 获取并合成地图
    private static MapData getMergedMap() {
        try {
            int mapHeight = redisUtil.getInt("mapHeight");
            int mapWidth = redisUtil.getInt("mapWidth");
            int[][] mapBarrier = redisUtil.getMap("mapBarrier", mapHeight, mapWidth);
            int[][] mapExplore = redisUtil.getMap("mapExplore", mapHeight, mapWidth);
            int[][] mapMerge = new int[mapHeight][mapWidth];
            for (int i = 0; i < mapHeight; i++) {
                for (int j = 0; j < mapWidth; j++) {
                    if (mapExplore[i][j] == 1) {
                        mapMerge[i][j] = mapBarrier[i][j];
                    } else {
                        mapMerge[i][j] = 2;
                    }
                }
            }
            System.out.println("[Navigator] 地图合成完成，尺寸：" + mapHeight + "x" + mapWidth);
            return new MapData(mapHeight, mapWidth, mapBarrier, mapExplore, mapMerge);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 3.2.3 地图划分与定位
    private static SubMapInfo locateCarAndTarget(Car car, MapData mapData) {
        int rows = (int) Math.ceil((double) mapData.mapHeight / SUB_MAP_SIZE);
        int cols = (int) Math.ceil((double) mapData.mapWidth / SUB_MAP_SIZE);
        Point[][] leftTops = new Point[rows][cols];
        Point[][] rightBottoms = new Point[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int leftX = i * SUB_MAP_SIZE;
                int leftY = j * SUB_MAP_SIZE;
                int rightX = Math.min((i + 1) * SUB_MAP_SIZE - 1, mapData.mapHeight - 1);
                int rightY = Math.min((j + 1) * SUB_MAP_SIZE - 1, mapData.mapWidth - 1);
                leftTops[i][j] = new Point(leftX, leftY);
                rightBottoms[i][j] = new Point(rightX, rightY);
            }
        }
        int[] carBlock = getBlockIndex(car.getCarPosition(), leftTops, rightBottoms);
        int[] targetBlock = getBlockIndex(car.getCarTarget(), leftTops, rightBottoms);
        System.out.println("[Navigator] Car所在块: [" + carBlock[0] + "," + carBlock[1] + "] 目标所在块: [" + targetBlock[0] + "," + targetBlock[1] + "]");
        return new SubMapInfo(rows, cols, leftTops, rightBottoms, carBlock, targetBlock);
    }

    // 3.2.4 判断目标位置，确定mapSearch
    private static MapSearchInfo getMapSearch(Car car, SubMapInfo subMapInfo, MapData mapData) {
        int[] carBlock = subMapInfo.carBlock;
        int[] targetBlock = subMapInfo.targetBlock;
        Point leftTop, rightBottom;
        if (Arrays.equals(carBlock, targetBlock)) {
            leftTop = subMapInfo.leftTops[carBlock[0]][carBlock[1]];
            rightBottom = subMapInfo.rightBottoms[carBlock[0]][carBlock[1]];
        } else {
            // 合并两个子地图的最小外接矩形
            int minX = Math.min(subMapInfo.leftTops[carBlock[0]][carBlock[1]].x, subMapInfo.leftTops[targetBlock[0]][targetBlock[1]].x);
            int minY = Math.min(subMapInfo.leftTops[carBlock[0]][carBlock[1]].y, subMapInfo.leftTops[targetBlock[0]][targetBlock[1]].y);
            int maxX = Math.max(subMapInfo.rightBottoms[carBlock[0]][carBlock[1]].x, subMapInfo.rightBottoms[targetBlock[0]][targetBlock[1]].x);
            int maxY = Math.max(subMapInfo.rightBottoms[carBlock[0]][carBlock[1]].y, subMapInfo.rightBottoms[targetBlock[0]][targetBlock[1]].y);
            leftTop = new Point(minX, minY);
            rightBottom = new Point(maxX, maxY);
        }
        System.out.println("[Navigator] 搜索区域: 左上(" + leftTop.x + "," + leftTop.y + ") 右下(" + rightBottom.x + "," + rightBottom.y + ")");
        return new MapSearchInfo(leftTop, rightBottom);
    }

    // 3.2.5 选择算法并寻路，返回UDRL字符串
    private static String calculatePath(Car car, MapSearchInfo mapSearchInfo, MapData mapData) {
        cn.edu.ncepu.HandsomeRun.CarAlgorithm.ICarAlgorithm algorithm;
        if (car.getCarAlgorithm() == CarAlgorithmEnum.ASTART) {
            algorithm = new cn.edu.ncepu.HandsomeRun.CarAlgorithm.CarAlgorithmAStart();
        } else if (car.getCarAlgorithm() == CarAlgorithmEnum.BFS) {
            algorithm = new cn.edu.ncepu.HandsomeRun.CarAlgorithm.CarAlgorithmBFS();
        } else {
            algorithm = new cn.edu.ncepu.HandsomeRun.CarAlgorithm.CarAlgorithm();
        }
        // 记录寻路开始时间（纳秒）
        carNavStartTime = System.nanoTime();
        String path = algorithm.calculatePath(car.getCarPosition(), car.getCarTarget(), mapData.mapMerge, mapSearchInfo.leftTop, mapSearchInfo.rightBottom);
        carNavEndTime = System.nanoTime();
        System.out.println("[Navigator] 规划路径(UDRL): " + path);
        return path;
    }

    // 3.2.6 写回Redis
    private static long carNavStartTime = 0;
    private static long carNavEndTime = 0;
    private static void updateCarStatusAndPath(Car car, String path) {
        if (path == null || path.isEmpty()) return;
        // 路径逆序
        String reversedPath = new StringBuilder(path).reverse().toString();
        System.out.println("[Navigator] 路径(原): " + path);
        System.out.println("[Navigator] 路径(逆序): " + reversedPath);
        car.setCarStatus(CarStatusEnum.RUNNING);
        car.setCarPath(reversedPath);
        redisUtil.setCar(car);
        System.out.println("[Navigator] 路径已写回Redis: " + reversedPath);
        // 记录导航耗时并发送分析日志（微秒）
        long navTime = (carNavEndTime - carNavStartTime) / 1000; // 微秒
        System.out.println("[Navigator] 本次导航耗时(μs): " + navTime);
        sendAnalysisLog(car, navTime);
    }

    // 发送分析日志
    private static void sendAnalysisLog(Car car, long navTime) {
        try {
            AnalysisLog log = new AnalysisLog(car.getCarId(), car.getCarAlgorithm(), navTime);
            String logJson = log.toJson();
            ExploreMessage exploreMessage = new ExploreMessage("Analysis" , logJson) ;
            analysisLogSender.sendBroadcastMessage(NAVI_ANALYSIS_LOG_EXCHANGE, exploreMessage.toJson());
            System.out.println("[Navigator] 分析日志已发送: " + exploreMessage.toJson());
        } catch (Exception e) {
            System.err.println("[Navigator] 分析日志发送失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 工具方法：获取点所在子地图块索引
    private static int[] getBlockIndex(Point p, Point[][] leftTops, Point[][] rightBottoms) {
        for (int i = 0; i < leftTops.length; i++) {
            for (int j = 0; j < leftTops[0].length; j++) {
                if (p.x >= leftTops[i][j].x && p.x <= rightBottoms[i][j].x &&
                    p.y >= leftTops[i][j].y && p.y <= rightBottoms[i][j].y) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[]{0, 0}; // 默认返回
    }

    // 地图数据结构体
    private static class MapData {
        int mapHeight, mapWidth;
        int[][] mapBarrier, mapExplore, mapMerge;
        MapData(int h, int w, int[][] b, int[][] e, int[][] m) {
            mapHeight = h; mapWidth = w; mapBarrier = b; mapExplore = e; mapMerge = m;
        }
    }
    // 子地图信息结构体
    private static class SubMapInfo {
        int rows, cols;
        Point[][] leftTops, rightBottoms;
        int[] carBlock, targetBlock;
        SubMapInfo(int r, int c, Point[][] lt, Point[][] rb, int[] cb, int[] tb) {
            rows = r; cols = c; leftTops = lt; rightBottoms = rb; carBlock = cb; targetBlock = tb;
        }
    }
    // 搜索区域信息结构体
    private static class MapSearchInfo {
        Point leftTop, rightBottom;
        MapSearchInfo(Point lt, Point rb) { leftTop = lt; rightBottom = rb; }
    }
} 