package org.example;

import java.awt.*;

public class Car {
    private int carId;
    private CarStatusEnum carStatus;
    private Point carPosition;
    private Point carTarget;
    private String carPath;  //目标路径
    private CarStatusEnum carAlgorithm;  //算法
    private int carStatusCnt;  //处于当前状态的时间
    private String carColor;  //用不到颜色，直接转为String即可
    private long carLastRunTime;  //小车心跳，小车进程每次运行时都更新

    public int getCarId() {
        return carId;
    }

    public long getCarLastRunTime() {
        return carLastRunTime;
    }

    public void setCarLastRunTime(long carLastRunTime) {
        this.carLastRunTime = carLastRunTime;
    }

    public CarStatusEnum getCarAlgorithm() {
        return carAlgorithm;
    }

    public void setCarAlgorithm(CarStatusEnum carAlgorithm) {
        this.carAlgorithm = carAlgorithm;
    }

    public int getCarStatusCnt() {
        return carStatusCnt;
    }

    public void setCarStatusCnt(int carStatusCnt) {
        this.carStatusCnt = carStatusCnt;
    }

    public String getCarPath() {
        return carPath;
    }

    public void setCarPath(String carPath) {
        this.carPath = carPath;
    }

    public Point getCarTarget() {
        return carTarget;
    }

    public void setCarTarget(Point carTarget) {
        this.carTarget = carTarget;
    }

    public Point getCarPosition() {
        return carPosition;
    }

    public void setCarPosition(Point carPosition) {
        this.carPosition = carPosition;
    }

    public CarStatusEnum getCarStatus() {
        return carStatus;
    }

    public void setCarStatus(CarStatusEnum carStatus) {
        this.carStatus = carStatus;
    }
}
