package cn.edu.ncepu.Model;

import cn.edu.ncepu.Car;
import com.google.gson.Gson;

import java.util.List;

public class InformationLog {
    private long expDuration;
    private int mapHeight;
    private int mapWidth;
    private int[][] mapBarrier;
    private List<Car> cars;

    /**
     * 将json转为一个InformationLog对象
     *
     * @param json json字符串
     * @return 一个RunLog对象
     */
    public static InformationLog getInformationLog(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, InformationLog.class);
    }

    /**
     * 将自身转为json字符串
     *
     * @return 对应的json
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

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
