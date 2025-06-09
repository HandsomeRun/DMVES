package cn.edu.necpu.HandsomeRun.CarAlgorithm;

import java.awt.Point;
import java.util.*;

public class CarAlgorithmBFS extends CarAlgorithm {
    @Override
    public List<Point> calculatePath(Point start, Point end, int[][] map, Point leftTop, Point rightBottom) {
        int rows = rightBottom.x - leftTop.x + 1;
        int cols = rightBottom.y - leftTop.y + 1;
        boolean[][] visited = new boolean[rows][cols];
        Map<Point, Point> cameFrom = new HashMap<>();
        Queue<Point> queue = new LinkedList<>();
        Point s = new Point(start.x - leftTop.x, start.y - leftTop.y);
        Point t = new Point(end.x - leftTop.x, end.y - leftTop.y);
        queue.add(s);
        visited[s.x][s.y] = true;
        while (!queue.isEmpty()) {
            Point curr = queue.poll();
            if (curr.equals(t)) {
                return reconstructPath(cameFrom, curr, leftTop);
            }
            for (int[] d : DIRS) {
                int nx = curr.x + d[0], ny = curr.y + d[1];
                if (nx < 0 || ny < 0 || nx >= rows || ny >= cols) continue;
                if (map[nx + leftTop.x][ny + leftTop.y] != 0) continue;
                if (visited[nx][ny]) continue;
                Point np = new Point(nx, ny);
                queue.add(np);
                visited[nx][ny] = true;
                if (!cameFrom.containsKey(np)) cameFrom.put(np, curr);
            }
        }
        return Collections.emptyList();
    }
    @Override
    public String calculatePathUDRL(Point start, Point end, int[][] map, Point leftTop, Point rightBottom) {
        List<Point> path = calculatePath(start, end, map, leftTop, rightBottom);
        if (path == null || path.size() < 2) return "";
        Collections.reverse(path);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < path.size(); i++) {
            Point prev = path.get(i - 1);
            Point curr = path.get(i);
            int dx = curr.x - prev.x, dy = curr.y - prev.y;
            if (dx == 1 && dy == 0) sb.append('D');
            else if (dx == -1 && dy == 0) sb.append('U');
            else if (dx == 0 && dy == 1) sb.append('R');
            else if (dx == 0 && dy == -1) sb.append('L');
            else sb.append('?');
        }
        return sb.toString();
    }
    private static final int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}};
    private List<Point> reconstructPath(Map<Point, Point> cameFrom, Point end, Point leftTop) {
        List<Point> path = new ArrayList<>();
        Point curr = end;
        while (cameFrom.containsKey(curr)) {
            path.add(new Point(curr.x + leftTop.x, curr.y + leftTop.y));
            curr = cameFrom.get(curr);
        }
        path.add(new Point(curr.x + leftTop.x, curr.y + leftTop.y));
        return path;
    }
} 