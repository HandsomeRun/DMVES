package cn.edu.ncepu.HandsomeRun.CarAlgorithm;

import java.awt.Point;
import java.util.*;

public class CarAlgorithmAStart extends CarAlgorithm {
    @Override
    public String calculatePath(Point start, Point end, int[][] map, Point leftTop, Point rightBottom) {
        int rows = rightBottom.y - leftTop.y + 1;
        int cols = rightBottom.x - leftTop.x + 1;
        boolean[][] visited = new boolean[rows][cols];
        Map<Point, Point> cameFrom = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        Point s = new Point(start.x - leftTop.x, start.y - leftTop.y);
        Point t = new Point(end.x - leftTop.x, end.y - leftTop.y);
        open.add(new Node(s, 0, heuristic(s, t)));
        while (!open.isEmpty()) {
            Node curr = open.poll();
            if (curr.p.equals(t)) {
                List<Point> path = reconstructPath(cameFrom, curr.p, leftTop);
                if (path == null || path.size() < 2) return "";
                Collections.reverse(path);
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < path.size(); i++) {
                    Point prev = path.get(i - 1);
                    Point currPt = path.get(i);
                    int dx = currPt.x - prev.x;
                    int dy = currPt.y - prev.y;
                    if (dx == 1 && dy == 0) sb.append('R');
                    else if (dx == -1 && dy == 0) sb.append('L');
                    else if (dx == 0 && dy == 1) sb.append('D');
                    else if (dx == 0 && dy == -1) sb.append('U');
                    else sb.append('?');
                }
                return sb.toString();
            }
            if (visited[curr.p.y][curr.p.x]) continue;
            visited[curr.p.y][curr.p.x] = true;
            for (int[] d : DIRS) {
                int nx = curr.p.x + d[0], ny = curr.p.y + d[1];
                if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) continue;
                if (map[ny + leftTop.y][nx + leftTop.x] == 1) continue;
                Point np = new Point(nx, ny);
                if (visited[ny][nx]) continue;
                int g = curr.g + 1;
                int h = heuristic(np, t);
                open.add(new Node(np, g, g + h));
                if (!cameFrom.containsKey(np)) cameFrom.put(np, curr.p);
            }
        }
        return "";
    }
    private static final int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}};
    private int heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }
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
    private static class Node {
        Point p;
        int g, f;
        Node(Point p, int g, int f) { this.p = p; this.g = g; this.f = f; }
    }
} 