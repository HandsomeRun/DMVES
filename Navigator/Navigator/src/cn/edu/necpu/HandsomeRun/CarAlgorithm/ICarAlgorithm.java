package cn.edu.necpu.HandsomeRun.CarAlgorithm;

import java.awt.Point;
import java.util.List;

public interface ICarAlgorithm {
    List<Point> calculatePath(Point start, Point end, int[][] map, Point leftTop, Point rightBottom);
    String calculatePathUDRL(Point start, Point end, int[][] map, Point leftTop, Point rightBottom);
} 