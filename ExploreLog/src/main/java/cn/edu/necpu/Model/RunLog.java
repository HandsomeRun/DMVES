package cn.edu.necpu.Model;

import cn.edu.necpu.Car;

import java.util.List;

public class RunLog {
    public RunLog(Long timeStamp, String map, List<Car> cars) {
        this.timeStamp = timeStamp;
        this.map = map;
        this.cars = cars;
    }

    private Long timeStamp;
    private String map;
    private List<Car> cars;
}
