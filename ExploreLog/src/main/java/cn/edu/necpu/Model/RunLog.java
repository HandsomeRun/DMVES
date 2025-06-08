package cn.edu.necpu.Model;

import cn.edu.necpu.Car;

import java.util.List;

public class RunLog {
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

    private Long timeStamp;
    private String mapExplore;
    private List<Car> cars;
}
