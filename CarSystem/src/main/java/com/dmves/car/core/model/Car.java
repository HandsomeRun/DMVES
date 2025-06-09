package com.dmves.car.core.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 小车实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Car {
    private String carId;
    private CarStatusEnum carStatus;
    private Point carPosition;
    private Point carTarget; // 小车目标
    private String carPath; // U D L R
    private String carAlgorithm;
    private int carStatusCnt; //
    private String carColor;
    private Long carLastRunTime; // 小车心跳,控制器需要每个周期都检测
}