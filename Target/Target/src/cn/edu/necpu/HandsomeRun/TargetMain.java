package cn.edu.necpu.HandsomeRun;

import cn.edu.necpu.RedisUtil;
import cn.edu.necpu.Car;
import com.google.gson.Gson;
import com.rabbitmq.impl.Receiver;
import com.rabbitmq.interfaces.MessageHandler;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;

public class TargetMain {
    private static final String REDIS_TARGET_KEY = "Target";
    private static final int INTERVAL_SECONDS = 60;
    private static final int SUBMAP_SIZE = 20;
    private static final String EXCHANGE_NAME = "1.target.exchange";
    private static final String QUEUE_NAME = "1.target.queue";
    private static final String ROUTING_KEY = "1.target.routing.key";
    private static final Gson gson = new Gson();
    private static final RedisUtil redisUtil = RedisUtil.getInstance();

    public static void main(String[] args) throws Exception {
        // 启动定时线程
        startTimerThread();
        // 启动主线程（RabbitMQ 消息监听）
        startMainThread();
    }

    private static void startTimerThread() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    redisUtil.setString(REDIS_TARGET_KEY, String.valueOf(now));
                    System.out.println("[Timer] 写入 Target 时间戳: " + now);
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
        receiver.receiveFairMessage(EXCHANGE_NAME, QUEUE_NAME, new MessageHandler() {
            @Override
            public void handleMessage(String message) {
                handleTargetMessage(message);
            }
        });
        System.out.println("[Target] RabbitMQ 消息监听已启动...");
    }

    // 主线程任务链入口
    private static void handleTargetMessage(String message) {
        try {
            int carId = parseCarId(message);
            Car car = getCarFromRedis(carId);
            int[][] mapMerge = getMergedMap();
            int[] subMapInfo = locateCarSubMap(car, mapMerge);
            if (isSubMapExplored(car, mapMerge, subMapInfo)) {
                int[] target = chooseNextSubMapTarget(car, mapMerge, subMapInfo);
                updateCarTarget(car, target);
            } else {
                int[] target = chooseSubMapTarget(car, mapMerge, subMapInfo);
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
            // 假设消息格式为 {"carId":"123"}
            Map<?,?> map = gson.fromJson(message, Map.class);
            String carIdStr = map.get("carId").toString();
            return Integer.parseInt(carIdStr);
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
        int mapHeight = redisUtil.getInt("mapHeight");
        int mapWidth = redisUtil.getInt("mapWidth");
        int[][] mapBarrier = redisUtil.getMap("mapBarrier", mapHeight, mapWidth);
        int[][] mapExplore = redisUtil.getMap("mapExplore", mapHeight, mapWidth);
        int[][] mapMerge = new int[mapHeight][mapWidth];
        for (int i = 0; i < mapHeight; i++) {
            for (int j = 0; j < mapWidth; j++) {
                if (mapExplore[i][j] == 1) {
                    mapMerge[i][j] = (mapBarrier[i][j] == 0) ? 0 : 1;
                } else {
                    mapMerge[i][j] = 2;
                }
            }
        }
        return mapMerge;
    }
    // 4. 划分子地图并定位
    private static int[] locateCarSubMap(Car car, int[][] mapMerge) {
        int mapHeight = mapMerge.length;
        int mapWidth = mapMerge[0].length;
        int x = car.getCarPosition().x;
        int y = car.getCarPosition().y;
        int i = x / SUBMAP_SIZE;
        int j = y / SUBMAP_SIZE;
        int leftTopX = i * SUBMAP_SIZE;
        int leftTopY = j * SUBMAP_SIZE;
        int rightBottomX = Math.min((i + 1) * SUBMAP_SIZE - 1, mapHeight - 1);
        int rightBottomY = Math.min((j + 1) * SUBMAP_SIZE - 1, mapWidth - 1);
        return new int[]{i, j, leftTopX, leftTopY, rightBottomX, rightBottomY};
    }
    // 5. 判断子地图是否探索完
    private static boolean isSubMapExplored(Car car, int[][] mapMerge, int[] subMapInfo) {
        int leftTopX = subMapInfo[2], leftTopY = subMapInfo[3];
        int rightBottomX = subMapInfo[4], rightBottomY = subMapInfo[5];
        // 检查边缘是否全为1或超界
        // 上下边
        for (int y = leftTopY; y <= rightBottomY; y++) {
            if (mapMerge[leftTopX][y] != 1) return false;
            if (mapMerge[rightBottomX][y] != 1) return false;
        }
        // 左右边
        for (int x = leftTopX; x <= rightBottomX; x++) {
            if (mapMerge[x][leftTopY] != 1) return false;
            if (mapMerge[x][rightBottomY] != 1) return false;
        }
        return true;
    }
    // 6.1 未探索完，选择目标点
    private static int[] chooseSubMapTarget(Car car, int[][] mapMerge, int[] subMapInfo) {
        int leftTopX = subMapInfo[2], leftTopY = subMapInfo[3];
        int rightBottomX = subMapInfo[4], rightBottomY = subMapInfo[5];
        int maxCount = -1;
        int targetX = -1, targetY = -1;
        for (int x = leftTopX; x <= rightBottomX; x++) {
            for (int y = leftTopY; y <= rightBottomY; y++) {
                if (mapMerge[x][y] == 0) {
                    int count = countSurrounding2(mapMerge, x, y);
                    if (count > maxCount) {
                        maxCount = count;
                        targetX = x;
                        targetY = y;
                    }
                }
            }
        }
        return new int[]{targetX, targetY};
    }
    // 6.2 已探索完，蛇形遍历选择目标点
    private static int[] chooseNextSubMapTarget(Car car, int[][] mapMerge, int[] subMapInfo) {
        int i = subMapInfo[0], j = subMapInfo[1];
        int mapHeight = mapMerge.length;
        int mapWidth = mapMerge[0].length;
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
            if (j == (mapWidth - 1) / SUBMAP_SIZE) {
                nextI = i + 1;
                nextJ = j;
            } else {
                nextI = i;
                nextJ = j + 1;
            }
        }
        int leftTopX = nextI * SUBMAP_SIZE;
        int leftTopY = nextJ * SUBMAP_SIZE;
        int rightBottomX = Math.min((nextI + 1) * SUBMAP_SIZE - 1, mapHeight - 1);
        int rightBottomY = Math.min((nextJ + 1) * SUBMAP_SIZE - 1, mapWidth - 1);
        int maxCount = -1, targetX = -1, targetY = -1;
        for (int x = leftTopX; x <= rightBottomX; x++) {
            for (int y = leftTopY; y <= rightBottomY; y++) {
                if (mapMerge[x][y] == 0) {
                    int count = countSurrounding2(mapMerge, x, y);
                    if (count > maxCount) {
                        maxCount = count;
                        targetX = x;
                        targetY = y;
                    }
                }
            }
        }
        return new int[]{targetX, targetY};
    }
    // 统计周围8格为2的数量
    private static int countSurrounding2(int[][] map, int x, int y) {
        int count = 0;
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};
        int h = map.length, w = map[0].length;
        for (int k = 0; k < 8; k++) {
            int nx = x + dx[k], ny = y + dy[k];
            if (nx >= 0 && nx < h && ny >= 0 && ny < w && map[nx][ny] == 2) count++;
        }
        return count;
    }
    // 7. 更新 Car 状态与目标，写回 Redis
    private static void updateCarTarget(Car car, int[] target) {
        if (target[0] == -1 || target[1] == -1) return;
        car.setCarStatus(cn.edu.necpu.CarStatusEnum.WAIT_NAV);
        car.setCarTarget(new java.awt.Point(target[0], target[1]));
        redisUtil.setCar(car);
    }
} 