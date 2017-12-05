package sail.g6f;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.*;

public class Player extends sail.sim.Player {
    List<Point> targets;
    Map<Integer, Set<Integer>> visited_set;
    Random gen;
    int id;
    Point initial;
    Point wind;
    int nextTarget = -1;
    int currentGrid = 0;
    int directionGrid = 1;
    int resolution = 3;

    // Oliver added these three attributes below. isVisited might be deprecated now.
    int isVisited[];
    int roundNumber = 0;
    int cornerThreshold = 100;// if there are more points then this then we'll start in a corner.
    int centerThreshold = 5; // If there are less than these many points, then we start closer to the center.
    int tThreshold = 500; // Number of targets.

    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        wind = wind_direction;

        double eps = 1.0;
        if (t >= cornerThreshold) {
            //System.out.println("Corner start");
            List<Point> fourCorners = new ArrayList<>();
            fourCorners.add(new Point(0 + eps, 0 + eps));
            fourCorners.add(new Point(0 + eps, 10 - eps));
            fourCorners.add(new Point(10 - eps, 0 + eps));
            fourCorners.add(new Point(10 - eps, 10 - eps));

            List<Point> fourCorners_test = new ArrayList<>();// test wind angle to these points with reference to the center
            fourCorners_test.add(new Point(-5, -5));
            fourCorners_test.add(new Point(-5, 5));
            fourCorners_test.add(new Point(5, -5));
            fourCorners_test.add(new Point(5, 5));

            Point cornerStart = new Point(0, 0);
            double angleBetween = 0.0;
            double minPerpendicularAngleDifference = Double.MAX_VALUE;
            for (int i = 0; i < fourCorners_test.size(); i++) {
                angleBetween = Point.angleBetweenVectors(wind, fourCorners_test.get(i));
                if (Math.abs(90.0 - angleBetween) <= minPerpendicularAngleDifference) {
                    cornerStart = fourCorners.get(i);
                    minPerpendicularAngleDifference = Math.abs(90.0 - angleBetween);
                }
            }

            initial = cornerStart;
        } else if (t <= centerThreshold) {
            Point bias = Point.rotateCounterClockwise(wind_direction, Math.PI / 2);
            initial = new Point(5 + bias.x, 5 + bias.y);
        } else {
            Point bias = Point.rotateCounterClockwise(wind_direction, Math.PI / 2);
            double bias_x = bias.x * 4;
            double bias_y = bias.y * 4;
            initial = new Point(5 + bias_x, 5 + bias_y);
        }
        double speed = Simulator.getSpeed(initial, wind_direction);
        currentGrid = getGrid(initial, resolution);
        return initial;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
        isVisited = new int[resolution * resolution];
    }

    @Override
    public Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms) {
        if (visited_set != null && visited_set.get(id).size() == targets.size()) {
            // This is if we have finished visiting all targets.
            nextTarget = targets.size();
            return findAngle(group_locations.get(id), initial, dt);
        }
        else if ((targets.size() >= tThreshold || dt < 0.01) && visited_set != null && !visited_set.get(id).contains(nextTarget)) {
            return findAngle(group_locations.get(id), targets.get(nextTarget), dt);
        }
        else {
            // Here is the logic to decide which target to head to next.
            // Check whether we have reached all targets in the current grid.
            boolean finishGrid = true;
            int x0 = currentGrid / resolution;
            int y0 = currentGrid % resolution;
            double x1 = (double)x0 * 10 / resolution;
            double x2 = (double)(x0 + 1) * 10 / resolution;
            double y1 = (double)y0 * 10 / resolution;
            double y2 = (double)(y0 + 1) * 10 / resolution;
            for (int i = 0; i < targets.size(); i++) {
                if (visited_set != null && visited_set.get(id).contains(i)) continue;
                Point temp = targets.get(i);
                if (temp.x > x1 && temp.x < x2 && temp.y > y1 && temp.y < y2) {
                    finishGrid = false;
                    break;
                }
            }
            if (finishGrid) {
                changeGrid();
            }
            x0 = currentGrid / resolution;
            y0 = currentGrid % resolution;
            x1 = (double)x0 * 10 / resolution;
            x2 = (double)(x0 + 1) * 10 / resolution;
            y1 = (double)y0 * 10 / resolution;
            y2 = (double)(y0 + 1) * 10 / resolution;
            
            // Find the closest target in the current grid.
            double min = 1e9;
            int mark = 0;
            for (int i = 0; i < targets.size(); i++) {
                if (visited_set != null && visited_set.get(id).contains(i)) continue;
                Point temp = targets.get(i);
                if (!(temp.x > x1 && temp.x < x2 && temp.y > y1 && temp.y < y2)) continue;
                int count = group_locations.size();
                for (int j = 0; j < group_locations.size(); j++) {
                    if (j == id) continue;
                    if (visited_set != null && visited_set.get(j).contains(i)) {
                        count--;
                    }
                }
                double dist = getTrueWeight(group_locations.get(id), temp) / count;
                if (dist < min) {
                    min = dist;
                    mark = i;
                }
            }

            nextTarget = mark;
            return findAngle(group_locations.get(id), targets.get(mark), dt);
        }
    }

    public Point findAngle(Point currentLoc, Point nextLoc, double dt) {
        double min = 1e9;
        Point result = nextLoc;
        for (int i = 20; i < 160; i++) {
            double angle = Math.PI * (i - 90) / 180;
            Point direction = Point.getDirection(currentLoc, nextLoc);
            Point rotation = Point.rotateCounterClockwise(direction, angle);
            Point step = Point.multiply(rotation, dt);
            Point median = Point.sum(currentLoc, step);
            if (median.x < 0.1 || median.x > 9.9 || median.y < 0.1 || median.y > 9.9) continue;
            double temp = getTrueWeight(currentLoc, median) + getTrueWeight(median, nextLoc);
            if (temp < min) {
                min = temp;
                result = median;
            }
        }
        return moveInPath(currentLoc, result);
    }
    
    public int getGrid(Point initial, int width) {
        // Find start grid.
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                double x1 = (double)i * 10 / width;
                double x2 = (double)(i + 1) * 10 / width;
                double y1 = (double)j * 10 / width;
                double y2 = (double)(j + 1) * 10 / width;
                if (initial.x > x1  && initial.x < x2 && initial.y > y1 && initial.y < y2) {
                    int temp = i * width + j;
                    return temp;
                }
            }
        }
        return 0;
    }
    
    public void changeGrid() {
        // If we have visited all targets in the current grid, we choose another grid.
        isVisited[currentGrid] = 1;
        int tot = resolution * resolution;
        int nextGrid = currentGrid + directionGrid;
        for (int i = 0; i < 5; i++) {
            // Choose proper direction.
            boolean tf = true;
            if (nextGrid < 0 || nextGrid > tot - 1) {
                tf = false;
            }
            else if (isVisited[nextGrid] == 1) {
                tf = false;
            }
            else if (currentGrid % resolution == 0 && directionGrid == -1) {
                tf = false;
            }
            else if ((currentGrid + 1) % resolution == 0 && directionGrid == 1) {
                tf = false;
            }
            if (tf == false) {
                if (directionGrid == 1) directionGrid = resolution;
                else if (directionGrid == resolution) directionGrid = -1;
                else if (directionGrid == -1) directionGrid = -resolution;
                else if (directionGrid == -resolution) directionGrid = 1;
                else directionGrid = 1;
            }
            nextGrid = currentGrid + directionGrid;
        }
        currentGrid = nextGrid;
    }

    // Move from currentLoc to nextLoc.
    private Point moveInPath(Point currentLoc, Point nextLoc) {
        return Point.getDirection(currentLoc, nextLoc);
    }

    // Gets the distance between 2 points scaled by the wind direction.
    private double getTrueWeight(Point currentLoc, Point nextLoc) {
        double dist = Point.getNorm(Point.getDirection(currentLoc, nextLoc));
        double angle = Point.angleBetweenVectors(Point.getDirection(currentLoc, nextLoc), wind);
        double speed = calcSpeed(angle);

        return dist / speed;
    }

    // Gets the speed factor in the direction of the angle.
    private double calcSpeed(double angle) {
        Point p = new Point(2.5 * Math.cos(angle) - 0.5, 5 * Math.sin(angle));
        return Point.getDistance(p, new Point(0, 0));
    }

    /**
     * visited_set.get(i) is a set of targets that the ith player has visited.
     */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set) {
        // movementPredictionFileWrite(group_locations, visited_set);
        roundNumber += 1;
        this.visited_set = visited_set;
    }
}
