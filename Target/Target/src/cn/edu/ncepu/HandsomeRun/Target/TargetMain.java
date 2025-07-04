package cn.edu.ncepu.HandsomeRun.Target;

import cn.edu.ncepu.Car.CarStatusEnum;
import cn.edu.ncepu.Redis.RedisUtil;
import cn.edu.ncepu.Car.Car;
import com.google.gson.Gson;
import com.rabbitmq.impl.Receiver;

import java.awt.*;
import java.util.*;
import java.util.List;

public class TargetMain {
    private static final String REDIS_TARGET_KEY = "Target";
    private static final int INTERVAL_SECONDS = 5;
    private static final int SUBMAP_SIZE = 20;
    private static final String EXCHANGE_NAME = "1.target.exchange";
    private static final String QUEUE_NAME = "1.target.queue";
    private static final String ROUTING_KEY = "1.target.routing.key";
    private static final Gson gson = new Gson();
    private static final RedisUtil redisUtil = RedisUtil.getInstance();
    private static final UUID uuid = UUID.randomUUID();

    public static void main(String[] args) throws Exception {
        // 初始化 Redis 和 RabbitMQ
        init();
        // 启动定时线程
        startTimerThread();
        // 启动主线程（RabbitMQ 消息监听）
        startMainThread();
    }

    // 初始化 Redis 和 RabbitMQ
    private static void init() {
        try {
            // 初始化 Redis 连接
            redisUtil.getJedis(uuid);
            System.out.println("[Init] Redis 连接成功");
            // RabbitMQ 初始化（可选，实际连接在 Receiver 内部完成）
            // 这里只做简单连通性测试
            com.rabbitmq.impl.Receiver testReceiver = new com.rabbitmq.impl.Receiver();
            System.out.println("[Init] RabbitMQ 连接成功");
        } catch (Exception e) {
            System.err.println("[Init] 初始化失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void startTimerThread() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    redisUtil.setTimeStamp(REDIS_TARGET_KEY, now);
                    System.out.println("[Target] 写入 Target 时间戳: " + now);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, INTERVAL_SECONDS * 1000L);
    }

    private static void startMainThread() throws Exception {
        Receiver receiver = new Receiver();
        receiver.initExchange(EXCHANGE_NAME, "direct");
        receiver.initQueue(QUEUE_NAME);
        receiver.bindQueueToExchange(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
        receiver.receiveFairMessage(EXCHANGE_NAME, QUEUE_NAME, message -> handleTargetMessage(message));
        System.out.println("[Target] RabbitMQ 消息监听已启动...");
    }

    // 主线程任务链入口
    private static void handleTargetMessage(String message) {
        try {
            int carId = parseCarId(message);
            Car car = getCarFromRedis(carId);
            int[][] mapMerge = getMergedMap();
            int[] subMapInfo = locateCarSubMap(car, mapMerge);
            ExploreResult result = exploreSubMapAndCollectUnexplored(car, mapMerge, subMapInfo);
            if (result.explored) {
                int[] target = chooseNextSubMapTarget(car, mapMerge, subMapInfo);
                updateCarTarget(car, target);
            } else {
                int[] target = chooseSubMapTargetSmart(result.reachableUnexplored, mapMerge);
                updateCarTarget(car, target);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 以下为主线程任务的私有静态方法（待实现）
    // 1. 解析 carId
    private static int parseCarId(String message) {
        try {
            // 假设消息格式为 {1}
            System.out.println("原消息为 ： " + message);

            return Integer.parseInt(message);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
    // 2. 获取 Car 实体
    private static Car getCarFromRedis(int carId) {
        return redisUtil.getCar(carId);
    }
    // 3. 获取并合成地图
    private static int[][] getMergedMap() {
        System.out.println("开始获取并合并地图");

        int mapWidth = redisUtil.getInt("mapWidth");
        int mapHeight = redisUtil.getInt("mapHeight");
        int[][] mapBarrier = redisUtil.getMap("mapBarrier", mapHeight, mapWidth);
        int[][] mapExplore = redisUtil.getMap("mapExplore", mapHeight, mapWidth);
        int[][] mapMerge = new int[mapWidth][mapHeight];
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (mapExplore[x][y] == 1) {
                    mapMerge[x][y] = (mapBarrier[x][y] == 0) ? 0 : 1;
                } else {
                    mapMerge[x][y] = 2;
                }
            }
        }

        System.out.println("合并地图结束\n" + mapWidth + " " + mapHeight + " " + "\n");
        for(int y = 0 ; y < mapHeight ; y ++ ) {
            for(int x = 0 ; x < mapWidth ; x ++ ) {
                System.out.print(mapMerge[x][y] + " ");
            }
            System.out.println();
        }
        return mapMerge;
    }
    // 4. 划分子地图并定位
    private static int[] locateCarSubMap(Car car, int[][] mapMerge) {
        System.out.println("开始定位子地图");

        int mapWidth = mapMerge.length;
        int mapHeight = mapMerge[0].length;
        int x = car.getCarPosition().x;
        int y = car.getCarPosition().y;
        int i = x / SUBMAP_SIZE;
        int j = y / SUBMAP_SIZE;
        int leftTopX = i * SUBMAP_SIZE;
        int leftTopY = j * SUBMAP_SIZE;
        int rightBottomX = Math.min((i + 1) * SUBMAP_SIZE - 1, mapWidth - 1);
        int rightBottomY = Math.min((j + 1) * SUBMAP_SIZE - 1, mapHeight - 1);

        System.out.printf("定位子地图结束， %d %d %d %d %d %d\n" ,i , j , leftTopX,leftTopY ,rightBottomX , rightBottomY);

        return new int[]{i, j, leftTopX, leftTopY, rightBottomX, rightBottomY};
    }
    // 5. 判断子地图是否探索完 + 可达未探索点收集
    private static class ExploreResult {
        boolean explored;
        java.util.List<Point> reachableUnexplored;
        ExploreResult(boolean explored, java.util.List<Point> reachableUnexplored) {
            this.explored = explored;
            this.reachableUnexplored = reachableUnexplored;
        }
    }
    private static ExploreResult exploreSubMapAndCollectUnexplored(Car car, int[][] mapMerge, int[] subMapInfo) {
        int leftTopX = subMapInfo[2], leftTopY = subMapInfo[3];
        int rightBottomX = subMapInfo[4], rightBottomY = subMapInfo[5];
        int w = mapMerge.length, h = mapMerge[0].length;
        boolean[][] visited = new boolean[w][h];
        java.util.List<Point> unexplored = new java.util.ArrayList<>();
        java.util.Queue<Point> queue = new java.util.LinkedList<>();
        int startX = car.getCarPosition().x, startY = car.getCarPosition().y;
        if (startX < leftTopX || startX > rightBottomX || startY < leftTopY || startY > rightBottomY) {
            // 小车不在当前子地图，直接判定已探索完毕
            return new ExploreResult(true, unexplored);
        }
        queue.add(new Point(startX, startY));
        visited[startX][startY] = true;
        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int x = p.x, y = p.y;
            if (mapMerge[x][y] == 2) unexplored.add(new Point(x, y));
            for (int d = 0; d < 4; d++) {
                int nx = x + dx[d], ny = y + dy[d];
                if (nx >= leftTopX && nx <= rightBottomX && ny >= leftTopY && ny <= rightBottomY
                        && !visited[nx][ny] && (mapMerge[nx][ny] == 0 || mapMerge[nx][ny] == 2)) {
                    visited[nx][ny] = true;
                    queue.add(new Point(nx, ny));
                }
            }
        }
        boolean explored = unexplored.isEmpty();
        return new ExploreResult(explored, unexplored);
    }

    // 6.1 未探索完，选择目标点（可达未探索点中周围2最多的）
    private static int[] chooseSubMapTargetSmart(java.util.List<Point> candidates, int[][] mapMerge) {
        int maxCount = -1;
        Point target = null;
        for (Point p : candidates) {
            int count = countSurrounding2(mapMerge, p.x, p.y);
            if (count > maxCount) {
                maxCount = count;
                target = p;
            }
        }
        if (target == null) return new int[]{-1, -1};
        System.out.printf("目标点为： %d %d \n", target.x, target.y);
        return new int[]{target.x, target.y};
    }
    // 6.2 已探索完，蛇形遍历选择目标点
    private static int[] chooseNextSubMapTarget(Car car, int[][] mapMerge, int[] subMapInfo) {
        System.out.println("开始探索下一子地图");

        int i = subMapInfo[0], j = subMapInfo[1];
        int mapWidth = mapMerge.length;
        int mapHeight = mapMerge[0].length;
        int nextI = i, nextJ = j;
        // 蛇形遍历规则
        if (i % 2 == 1) {
            if (j == 0) {
                nextI = i + 1;
                nextJ = j;
            } else {
                nextI = i;
                nextJ = j - 1;
            }
        } else {
            if (j == (mapHeight - 1) / SUBMAP_SIZE) {
                nextI = i + 1;
                nextJ = j;
            } else {
                nextI = i;
                nextJ = j + 1;
            }
        }
        int leftTopX = nextI * SUBMAP_SIZE;
        int leftTopY = nextJ * SUBMAP_SIZE;
        int rightBottomX = Math.min((nextI + 1) * SUBMAP_SIZE - 1, mapWidth - 1);
        int rightBottomY = Math.min((nextJ + 1) * SUBMAP_SIZE - 1, mapHeight - 1);
        int maxCount = -1, targetX = -1, targetY = -1;
        for (int x = leftTopX; x <= rightBottomX; x++) {
            for (int y = leftTopY; y <= rightBottomY; y++) {
                if (mapMerge[x][y] == 2) {
                    int count = countSurrounding2(mapMerge, x, y);
                    if (count > maxCount) {
                        maxCount = count;
                        targetX = x;
                        targetY = y;
                    }
                }
            }
        }

        System.out.println("下一子地图目标点为 " + targetX + targetY + "\n");
        return new int[]{targetX, targetY};
    }
    // 统计周围8格为2的数量
    private static int countSurrounding2(int[][] map, int x, int y) {
        int count = 0;
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};
        int w = map.length, h = map[0].length;
        for (int k = 0; k < 8; k++) {
            int nx = x + dx[k], ny = y + dy[k];
            if (nx >= 0 && nx < w && ny >= 0 && ny < h && map[nx][ny] == 2) count++;
        }
        return count;
    }
    // 7. 更新 Car 状态与目标，写回 Redis
    private static void updateCarTarget(Car car, int[] target) {
        System.out.println("开始更新小车状态");
        if (target[0] == -1 || target[1] == -1) return;
        car.setCarStatus(CarStatusEnum.WAIT_NAV);
        car.setCarTarget(new java.awt.Point(target[0], target[1]));



        System.out.println("小车状态为 ： " + car.toString());
        redisUtil.setCar(car);
    }
} 