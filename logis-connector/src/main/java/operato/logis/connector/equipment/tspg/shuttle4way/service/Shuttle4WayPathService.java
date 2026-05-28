package operato.logis.connector.equipment.tspg.shuttle4way.service;


import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.Shuttle4WayWriteConsts;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Cell;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Node;
import lombok.Data;

import java.util.*;

@Data
public class Shuttle4WayPathService {

    private double moveCost;
    private double turnCost;
    private double droveBonusCost;

    public Cell[][] map;


    public Shuttle4WayPathService(int maxX, int maxY, int[] driveLine, double moveCost, double turnCost, double driveBonusCost){
        this.map = initMap(maxX, maxY, driveLine);
        this.moveCost = moveCost;
        this.turnCost = turnCost;
        this.droveBonusCost = driveBonusCost;
    }

    public Shuttle4WayPathService(int maxX, int maxY, int[] driveLine){
        this.map = initMap(maxX, maxY, driveLine);
        this.moveCost = 1;
        this.turnCost = 1;
        this.droveBonusCost = 1;
    }

    public Cell[][] initMap(int maxX, int maxY, int[] driveLine){
        Cell[][] map = new Cell[maxX][maxY];
        for (int x = 0; x < maxX; x++) {
            for (int y = 0; y < maxY; y++) {
                boolean isDriveLine = isDriveLine(y, driveLine);
                map[x][y] = new Cell(x, y, isDriveLine);
            }
        }
        return map;
    }

    private boolean isDriveLine(int y, int[] driveLine) {
        for (int v : driveLine) {
            if (v == y+1) return true;
        }
        return false;
    }

    private boolean isAlreadyTarget = false;
    public boolean isAlreadyAtTarget(int sx, int sy, int tx, int ty){
        isAlreadyTarget = (sx == tx && sy == ty);
        return isAlreadyTarget;
    }
    public void updateUseCell(int x, int y, boolean isUsed){
        map[x-1][y-1].setUsed(isUsed);
    }
    public void updateCargoCell(int x, int y, boolean hasCargo){
        map[x-1][y-1].setHasCargo(hasCargo);
    }

    private List<int[]> getAvailableDirections(Cell cell) {
        if (cell.isDriveLine()) {
            return List.of(
                    new int[]{1, 0},
                    new int[]{-1, 0},
                    new int[]{0, 1},
                    new int[]{0, -1}
            );
        } else {
            // Y축만 이동
            return List.of(
                    new int[]{0, 1},
                    new int[]{0, -1}
            );
        }
    }


    /**
     *  반대 셀 찾기
     */
    public Cell findReachableTarget(int sx, int sy, int minX, int maxX, int minY, int maxY, int minMoveDistance) {
        // 최소 이동 거리 (출발점이 중앙 근처인 경우, 실제 이동거리가 작은것을 방지)
        int MIN_MOVE_DISTANCE = minMoveDistance;

        // 1. 중앙 기준 반전
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        int targetX = (int) Math.round(2 * centerX - sx);
        int targetY = (int) Math.round(2 * centerY - sy);


        int dist = Math.abs(targetX - sx) + Math.abs(targetY - sy);

        // 중앙 근처라 이동거리가 너무 짧은 경우
        if (dist < MIN_MOVE_DISTANCE) {

            // 반대 방향 벡터 계산
            int dx = targetX - sx;
            int dy = targetY - sy;

            // 방향이 0이면 강제로 방향 지정
            if (dx == 0 && dy == 0) {

                // 중앙 한복판이면 랜덤 혹은 고정 방향
                dx = (sx <= centerX) ? 1 : -1;
                dy = 0;
            }

            // 방향 normalize
            dx = Integer.compare(dx, 0);
            dy = Integer.compare(dy, 0);

            // 최소 거리만큼 더 밀기
            targetX += dx * MIN_MOVE_DISTANCE;
            targetY += dy * MIN_MOVE_DISTANCE;
        }


        // 2. clamp
        targetX = Math.max(minX, Math.min(maxX, targetX));
        targetY = Math.max(minY, Math.min(maxY, targetY));

        // index 보정
        targetX -= 1;
        targetY -= 1;
        Cell bestCell = new Cell(0, 0);
        int bestDist = Integer.MAX_VALUE;

        System.out.println(map.length);
        System.out.println(map[0].length);
        // 3. 전체 map 순회하면서 후보 찾기
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[0].length; y++) {
                Cell c = map[x][y];
                if (c == null) continue;
                if (!c.isUsed()) continue;
                if (x == targetX && y == targetY){
                    return new Cell(c.getLocX()+1,c.getLocY()+1);
                }
                // 가장 가까운 셀 찾기
                int d = Math.abs(x - targetX) + Math.abs(y - targetY);
                if (d < bestDist) {
                    bestDist = d;
                    bestCell = c;
                }
            }
        }
        return new Cell(bestCell.getLocX()+1,bestCell.getLocY()+1);
    }

    /**
     * 경로탐색
     * 경로탐색시 화물 로드한 상태인 경우 화물이 있는 셀 회피하여 경로 탐색
     * 화물 없는 상태인 경우 화물 유무 관계없이 경로 탐색
     */
    public List<Cell> findPath(
            boolean isAvoidance,
            int sx, int sy,
            int tx, int ty
    ) {
        sx -= 1;
        sy -= 1;
        tx -= 1;
        ty -= 1;

        PriorityQueue<Node> open =
                new PriorityQueue<>(Comparator.comparingDouble(Node::totalCost));

        boolean[][] visited = new boolean[map.length][map[0].length];

        Node start = new Node();
        start.setX(sx);
        start.setY(sy);
        start.setCostFromStart(0);
        start.setCostToGoal(heuristic(sx, sy, tx, ty));
        start.setDirection(null);

        open.add(start);

        while (!open.isEmpty()) {
            Node cur = open.poll();

            if (cur.getX() == tx && cur.getY() == ty) {
                return buildPath(cur, map);
            }

            if (visited[cur.getX()][cur.getY()]) continue;
            visited[cur.getX()][cur.getY()] = true;

            Cell curCell = map[cur.getX()][cur.getY()];

            for (int[] d : getAvailableDirections(curCell)) {
                int nx = cur.getX() + d[0];
                int ny = cur.getY() + d[1];

                if (nx < 0 || ny < 0 || nx >= map.length || ny >= map[0].length)
                    continue;

                Cell nextCell = map[nx][ny];
                if ((nextCell.isHasCargo() && isAvoidance) || !nextCell.isUsed())
                    continue;

                var nextDir =  Shuttle4WayWriteConsts.ShuttleDirection.getDirection(cur.getX(), cur.getY(), nx, ny);

                double cost = moveCost;

                if (cur.getDirection() != null && cur.getDirection() != nextDir) {
                    cost += turnCost;
                }

                if (nextCell.isDriveLine()) {
                    cost -= droveBonusCost;
                }

                Node next = new Node();
                next.setX(nx);
                next.setY(ny);
                next.setCostFromStart(cur.getCostFromStart() + cost);
                next.setCostToGoal(heuristic(nx, ny, tx, ty));
                next.setDirection(nextDir);
                next.setParentNode(cur);
                open.add(next);
            }
        }

        // TODO : 목적지 셀이 경로의 마지막 노드와 동일한지 체크 필요
        return List.of(); // 경로 없음
    }


    public double heuristic(int x, int y, int tx, int ty) {
        return Math.abs(x - tx) + Math.abs(y - ty);
    }

    public List<Cell> buildPath(Node end, Cell[][] map) {
        LinkedList<Cell> path = new LinkedList<>();
        Node cur = end;
        while (cur != null) {
            Cell cell = map[cur.getX()][cur.getY()];
            Cell outCell = new Cell(
                    cell.getLocX() + 1,
                    cell.getLocY() +1,
                    cell.isDriveLine()
            );
            path.addFirst(outCell);
            cur = cur.getParentNode();
        }
        return path;
    }

    public List<Shuttle4WayWriteMap> buildMoveCommands(List<Cell> path) {
        List<Shuttle4WayWriteMap> cmds = new ArrayList<>();
        if (path.size() < 2) return cmds;

        Shuttle4WayWriteConsts.ShuttleDirection curDir = null;
        List<Cell> curPath = new ArrayList<>();
        Cell fromCell = null;
        Cell toCell = null;
        int dist = 0;

        for (int i = 1; i < path.size(); i++) {
            Cell prev = path.get(i - 1);
            Cell cur  = path.get(i);

            Shuttle4WayWriteConsts.ShuttleDirection nextDir = Shuttle4WayWriteConsts.ShuttleDirection.getDirection(prev.getLocX(), prev.getLocY(), cur.getLocX(), cur.getLocY());

            // 최초 방향 설정
            if(curDir == null){
                curDir = nextDir;
                fromCell = prev;
                curPath.add(prev);
                curPath.add(cur);
                toCell = cur;
                dist = 1;
                continue;
            }
            if (curDir != nextDir) {
                cmds.add(new Shuttle4WayWriteMap(fromCell, toCell, curPath));
                curDir = nextDir;
                fromCell = prev;
                toCell = cur;
                curPath = new ArrayList<>();
                curPath.add(prev);
                curPath.add(cur);
                dist = 1;
            } else {
                curPath.add(cur);
                toCell = cur;
                dist++;
            }
        }

        cmds.add(new Shuttle4WayWriteMap(fromCell, path.get(path.size() - 1), curPath));
        return cmds;
    }




}
