package cn.edu.ncepu.Model;

import java.awt.*;

public class Car {
    private int carId;
    private CarStatusEnum carStatus;
    private Point carPosition;
    private Point carTarget;
    private String carPath;  //目标路径
    private CarAlgorithmEnum carAlgorithm;  //算法
    private int carStatusCnt;  //处于当前状态的时间
    private String carColor;  //用不到颜色，直接转为String即可
    private long carLastRunTime;  //小车心跳，小车进程每次运行时都更新

    /*
     * 仅仅测试用
     */

    public Car(int carId, CarStatusEnum carStatus, Point carPosition, Point carTarget, String carPath, CarAlgorithmEnum carAlgorithm, int carStatusCnt, String carColor, long carLastRunTime) {
        this.carId = carId;
        this.carStatus = carStatus;
        this.carPosition = carPosition;
        this.carTarget = carTarget;
        this.carPath = carPath;
        this.carAlgorithm = carAlgorithm;
        this.carStatusCnt = carStatusCnt;
        this.carColor = carColor;
        this.carLastRunTime = carLastRunTime;
    }

    public int getCarId() {
        return carId;
    }

    public long getCarLastRunTime() {
        return carLastRunTime;
    }

    public void setCarLastRunTime(long carLastRunTime) {
        this.carLastRunTime = carLastRunTime;
    }

    public CarAlgorithmEnum getCarAlgorithm() {
        return carAlgorithm;
    }

    public void setCarAlgorithm(CarAlgorithmEnum  carAlgorithm) {
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

    @Override
    public String toString() {
        return "Car{" +
                "carId=" + carId +
                ", carStatus=" + carStatus +
                ", carPosition=" + carPosition +
                ", carTarget=" + carTarget +
                ", carPath='" + carPath + '\'' +
                ", carAlgorithm=" + carAlgorithm +
                ", carStatusCnt=" + carStatusCnt +
                ", carColor='" + carColor + '\'' +
                ", carLastRunTime=" + carLastRunTime +
                '}';
    }
}
