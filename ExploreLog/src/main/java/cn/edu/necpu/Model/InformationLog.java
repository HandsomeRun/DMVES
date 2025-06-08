package cn.edu.necpu.Model;

import cn.edu.necpu.Car;

import java.util.List;

public class InformationLog {
    private long expDuration;
    private int mapHeight;
    private int mapWidth;
    private int[][] mapBarrier;
    private List<Car> cars;

    public InformationLog(long expDuration, int mapHeight, int mapWidth, int[][] mapBarrier, List<Car> cars) {
        this.expDuration = expDuration;
        this.mapHeight = mapHeight;
        this.mapWidth = mapWidth;
        this.mapBarrier = mapBarrier;
        this.cars = cars;
    }

    public void setExpDuration(long expDuration) {
        this.expDuration = expDuration;
    }
}
