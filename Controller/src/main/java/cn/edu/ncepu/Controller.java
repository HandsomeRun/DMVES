package cn.edu.ncepu;

import cn.edu.ncepu.Model.Car;
import cn.edu.ncepu.Model.CarStatusEnum;
import cn.edu.ncepu.Model.ExploreMessage;
import cn.edu.ncepu.Util.RedisUtil;
import com.rabbitmq.impl.Sender;
import com.rabbitmq.interfaces.ISender;

import java.util.UUID;

public class Controller {

    private final static UUID uuid = UUID.randomUUID();

    public static void main(String[] args) {
        // 建立Redis连接
        RedisUtil redisUtil = RedisUtil.getInstance();
        try {
            redisUtil.getJedis(uuid);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // 初始化MQ
        ISender sender = new Sender();

        /*
        用于记录开始时间
         */
        long startTime = -1;
        long durationTime = 0;

        mainWhile:
        while (true) {
            long sleepTime = 2000;
            long nowTime = System.currentTimeMillis();
            redisUtil.setTimeStamp("Controller", nowTime);
            String isWork = redisUtil.getIsWork();
            if (!isWork.isEmpty()) {
                switch (isWork) {
                    case "运行中" -> {
                        // 检查View是否存活
                        long lastTime = redisUtil.getTimeStamp("View");
                        // 超出容忍时间
                        if (nowTime - lastTime > COMPONENT_TOLERANCE_TIME) {
                            redisUtil.setIsWork("故障");
                            // 如果不是新开始实验，给探索日志子系统发MQ，记录最后一帧，同时记录配置
                            if (startTime != -1) {
                                sender.DMVESSenderMessage(Sender.ControlName, Sender.TargetName
                                        , new ExploreMessage("End", String.valueOf(durationTime)).toJson());
                            }
                            System.out.println("View故障！");
                            startTime = -1;
                            continue mainWhile; // 跳到主循环
                        }

                        // 新实验的开始
                        if (-1 == startTime) {
                            startTime = nowTime;
                            durationTime = 0;

                            System.out.println("A new Exp, Time:" + startTime);
                            // 发MQ给日志系统
                            sender.DMVESSenderMessage(Sender.ControlName, Sender.ExploreLogName
                                    , new ExploreMessage("Start", String.valueOf(startTime)).toJson());
                        }

                        // 计算时间
                        durationTime += nowTime - startTime;
                        startTime = nowTime;

                        System.out.print("僵尸队列：");
                        for (int i : redisUtil.getDisConnectCars()) {
                            System.out.print(i + " ");
                        }
                        System.out.println();
                        // 遍历小车
                        int carNumber = redisUtil.getIntByLock("carNum");
                        System.out.println("carNum : " + carNumber);
                        int okCarCount = 0;// 用于计算遍历完连通块的小车数量
                        for (int i = 0; i < carNumber; i++) {
                            Car car = redisUtil.getCar(i + 1);
                            System.out.println(car);
                            CarStatusEnum carStatus = car.getCarStatus();
//                        int carStatusCnt = car.getCarStatusCnt();

                            // 断联状态
                            if (CarStatusEnum.DISCONNECTING == carStatus) continue;

                            // 超出小车容忍时间，认为小车进程断联
                            if (nowTime - car.getCarLastRunTime() > CAR_TOLERANCE_TIME) {
                                // 更改小车状态，并将小车放入僵尸队列
                                car.setCarStatus(CarStatusEnum.DISCONNECTING);
                                redisUtil.setCar(car);
                                redisUtil.addDisConnectCar(car.getCarId());

                                System.out.println("Find DisConnect carId : " + car.getCarId());

                                continue;
                            }


                            // 分发任务，设置小车状态
                            switch (carStatus) {
                                case FREE -> {
                                    car.setCarStatusCnt(3);
                                    car.setCarStatus(CarStatusEnum.SEARCHING);
                                    redisUtil.setCar(car);

                                    sender.DMVESSenderMessage(Sender.ControlName
                                            , Sender.TargetName
                                            , String.valueOf(car.getCarId()));

                                    System.out.println("Request Target carId : " + car.getCarId());
                                }
                                case WAIT_NAV -> {
                                    car.setCarStatusCnt(3);
                                    car.setCarStatus(CarStatusEnum.NAVIGATING);
                                    redisUtil.setCar(car);

                                    sender.DMVESSenderMessage(Sender.ControlName
                                            , Sender.NavigatorName
                                            , String.valueOf(car.getCarId()));

                                    System.out.println("Request Nav carId : " + car.getCarId());
                                }
                                case OK -> {
                                    okCarCount++;
                                    System.out.println("Car OK carId : " + car.getCarId());
                                }
//                            case SEARCHING, NAVIGATING, WAITING -> {
//                                car.setCarStatusCnt(carStatusCnt);
//                                redisUtil.setCar(car);
//                            }
                            }
                        }

                        // 给view发MQ，更新上一帧的画面
                        sender.DMVESSenderMessage(Sender.ControlName, Sender.ViewName, "update");

                        // 所有小车都已完成
                        if (okCarCount == carNumber) {
                            // 给探索日志子系统发MQ，记录最后一帧，同时记录配置
                            sender.DMVESSenderMessage(Sender.ControlName, Sender.ExploreLogName
                                    , new ExploreMessage("End", String.valueOf(durationTime)).toJson());

                            startTime = -1;

                            // 将redis中的isWork置为“未运行”，防止重复发消息
                            redisUtil.setIsWork("已完成");

                            System.out.println("Exp End!");

                        } else {  //未完成
                            // 给探索日志子系统发MQ，记录一帧
                            sender.DMVESSenderMessage(Sender.ControlName, Sender.ExploreLogName
                                    , new ExploreMessage("Run", String.valueOf(durationTime)).toJson());

                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }

                            // 给小车进程MQ，更新小车状态
                            sender.DMVESSenderMessage(Sender.ControlName, Sender.CarName, "run");
//                            try {
//                                Thread.sleep(500);
//                            } catch (InterruptedException e) {
//                                throw new RuntimeException(e);
//                            }
                        }

                        sleepTime = 400;
                    }
                    case "强制结束" -> {
                        // 给探索日志子系统发MQ，记录最后一帧，同时记录配置
                        if (startTime != -1) {
                            sender.DMVESSenderMessage(Sender.ControlName, Sender.ExploreLogName
                                    , new ExploreMessage("End", String.valueOf(durationTime)).toJson());
                            System.out.println("前端强制结束实验！");
                            startTime = -1;
                        }
                    }
                    case "未运行" -> {
                        //如果是暂停状态，需要更新时间戳
                        if (startTime != -1) {
                            startTime = nowTime;
                            System.out.println("暂停ing");
                        }
                    }
                }
            }

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }

    /**
     * 构件最长容忍时间
     */
    private final static long COMPONENT_TOLERANCE_TIME = 20000;
    /**
     * 小车进程最长容忍时间
     */
    private final static long CAR_TOLERANCE_TIME = 20000;
}