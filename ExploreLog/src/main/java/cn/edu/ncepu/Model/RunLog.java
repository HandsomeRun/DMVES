package cn.edu.ncepu.Model;

import cn.edu.ncepu.Car;
import com.google.gson.Gson;

import java.util.List;

public class RunLog {
    private Long timeStamp;
    private String mapExplore;
    private List<Car> cars;

    /**
     * 将json转为一个RunLog对象
     *
     * @param json json字符串
     * @return 一个RunLog对象
     */
    public static RunLog getRunLog(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, RunLog.class);
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

    public RunLog(Long timeStamp, String mapExplore, List<Car> cars) {
        this.timeStamp = timeStamp;
        this.mapExplore = mapExplore;
        this.cars = cars;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public String getMapExplore() {
        return mapExplore;
    }

    public List<Car> getCars() {
        return cars;
    }
}
