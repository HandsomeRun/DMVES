package com.dmves.car.core.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 坐标点类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Point {
    private int x;
    private int y;
}