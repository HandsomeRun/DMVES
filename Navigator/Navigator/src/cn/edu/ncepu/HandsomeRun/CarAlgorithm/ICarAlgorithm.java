package cn.edu.ncepu.HandsomeRun.CarAlgorithm;

import java.awt.Point;

public interface ICarAlgorithm {
    String calculatePath(Point start, Point end, int[][] map, Point leftTop, Point rightBottom);
} 