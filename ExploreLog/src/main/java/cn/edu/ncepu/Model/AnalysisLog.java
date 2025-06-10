package cn.edu.ncepu.Model;

import com.google.gson.Gson;

public class AnalysisLog {
    private int carId;
    private CarAlgorithmEnum carAlgorithmEnum;
    private long navTime;

    /**
     * 将json转为一个AnalysisLog对象
     *
     * @param json json字符串
     * @return 一个ExploreMessage对象
     */
    public static AnalysisLog getAnalysisLog(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, AnalysisLog.class);
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

    public AnalysisLog(int carId, CarAlgorithmEnum carAlgorithmEnum, long navTime) {
        this.carId = carId;
        this.carAlgorithmEnum = carAlgorithmEnum;
        this.navTime = navTime;
    }

    public int getCarId() {
        return carId;
    }

    public CarAlgorithmEnum getCarAlgorithmEnum() {
        return carAlgorithmEnum;
    }

    public long getNavTime() {
        return navTime;
    }
}
