package sail.g6;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player extends sail.sim.Player {
    List<Point> targets;
    List<Point> path;
    List<Point> prevLoc;
    Map<Integer, Set<Integer>> visited_set;
    Random gen;
    int id;
    Point initial;
    Point wind;

    int nextTarget = -1;
    int[] plan;

    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        wind = wind_direction;
        prevLoc = new ArrayList<>();
        path = new ArrayList<>();
        Point bias = Point.rotateCounterClockwise(wind_direction, Math.PI / 2);
        double bias_x = bias.x * 4;
        double bias_y = bias.y * 4;
        initial = new Point(5 + bias_x, 5 + bias_y);
        double speed = Simulator.getSpeed(initial, wind_direction);
        prevLoc.add(0, initial);
        plan = new int[6];
        for (int i = 0; i < 6; i++)
            plan[i] = -1;
        return initial;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
    }

    @Override
    public Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms) {

        // Check if we've reached the target.
        if (this.path.size() != 0 && visited_set.get(id).contains(nextTarget)) {
            prevLoc.add(0, path.get(0));
            path.remove(0);
        }
        else if (this.path.size() != 0) {
            // System.out.println("Moving to set target.");
            // Check if we crossed the intermediate point.
            // This won't be a target point because that is already checked for in the
            // first "if".
            if (checkCrossedInterpolation(path.get(0), prevLoc.get(0), group_locations.get(id))) {
                prevLoc.add(0, path.get(0));
                path.remove(0);
                // We are guaranteed that there is another point in "path".
            }
            if (path.size() == 0) {
                // We should never come here.
                // If we do, then it means that the first "if" that checks if
                // we crossed a target is buggy.
                System.out.println("Whaaaaaa");
            }
            // Continue moving to the intermediate point or move to the target point.
            return moveInPath(group_locations.get(id), path.get(0));
        }

        if(visited_set != null && visited_set.get(id).size() == targets.size()) {
            // This is if we have finished visiting all targets.
            nextTarget = targets.size();
            return findPathAndMove(group_locations.get(id), initial);
        }
        else {
            // System.out.println("Selecting next target.");
            // Here is the logic to decide which target to head to next.
            // If we do not know which target to go next, we make a plan.
            if (plan[0] == -1) makePlan(group_locations, id);
            nextTarget = plan[0];
            for (int i = 0; i < 5; i++) {
                plan[i] = plan[i + 1];
            }
            return findPathAndMove(group_locations.get(id), targets.get(nextTarget));
        }
    }
    
    public void makePlan(List<Point> group_locations, int id) {
        // Find five closest targets which we have not visited
        int[] closest;
        int[] selected;
        closest = new int[5];
        selected = new int[targets.size()];
        for (int k = 0; k < 5; k++) {
            closest[k] = -1;
            double min = 1e9;
            int mark = -1;
            for (int i = 0; i < targets.size(); i++) {
                if (visited_set != null && visited_set.get(id).contains(i)) continue;
                if (selected[i] == 1) continue;
                int count = group_locations.size();
                for (int j = 0; j < group_locations.size(); j++) {
                    if (j == id) continue;
                    if (visited_set != null && visited_set.get(j).contains(i)) {
                        count--;
                    }
                }
                double dist = getTrueWeight(group_locations.get(id), targets.get(i)) / count;
                if (dist < min) {
                    min = dist;
                    mark = i;
                }
            }
            if (mark > -1) {
                closest[k] = mark;
                selected[mark] = 1;
            }
        }
        
        // We should consider that the end point should close to other points.
        double minDistance = 1e9;
        int endPoint = -1;
        for (int k = 0; k < 5; k++) {
            if (closest[k] == -1) continue;
            for (int i = 0; i < targets.size(); i++) {
                if (visited_set != null && visited_set.get(id).contains(i)) continue;
                if (selected[i] == 1) continue;
                double dist = getTrueWeight(targets.get(closest[k]), targets.get(i));
                if (dist < minDistance) {
                    minDistance = dist;
                    endPoint = k;
                }
            }
        }
        // If no end point exists, which means the game will terminate soon, we calculate distance to initial point.
        if (endPoint == -1) {
            for (int k = 0; k < 5; k++) {
                if (closest[k] == -1) continue;
                double dist = getTrueWeight(targets.get(closest[k]), initial);
                if (dist < minDistance) {
                    minDistance = dist;
                    endPoint = k;
                }
            }
        }
        
        //Compute permutation of five closest targets and make an optimal plan.
        double minTotal = 1e9;
        for (int k1 = 0; k1 < 5; k1++) {
            for (int k2 = 0; k2 < 5; k2++) {
                if (k1 == k2) continue;
                for (int k3 = 0; k3 < 5; k3++) {
                    if (k1 == k3 || k2 == k3) continue;
                    for (int k4 = 0; k4 < 5; k4++) {
                        if (k1 == k4 || k2 == k4 || k3 == k4) continue;
                        int k5 = 10 - k1 - k2 - k3 - k4;
                        if (k5 != endPoint) continue;
                        int[] kk;
                        kk = new int[5];
                        kk[0] = k1;
                        kk[1] = k2;
                        kk[2] = k3;
                        kk[3] = k4;
                        kk[4] = k5;
                        Point current = group_locations.get(id);
                        double total = 0;
                        for (int i = 0; i < 5; i++) {
                            if (closest[kk[i]] > -1) {
                                total = total + getTrueWeight(current, targets.get(closest[kk[i]]));
                                current = targets.get(closest[kk[i]]);
                            }
                        }
                        if (total < minTotal) {
                            minTotal = total;
                            int j = 0;
                            for (int i = 0; i < 5; i++) {
                                if (closest[kk[i]] > -1) {
                                    plan[j] = closest[kk[i]];
                                    j++;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public Point findPathAndMove(Point currentLoc, Point nextLoc) {
        ArrayList<Point> grid = getGrid(currentLoc, nextLoc);

        double minDist = getTrueWeight(currentLoc, nextLoc);
        int minIndex = -1;

        for (int i=0; i < grid.size(); ++i) {
            if (getTrueWeight(currentLoc, grid.get(i))
                    + getTrueWeight(grid.get(i), nextLoc) < minDist) {
                minIndex = i;
                minDist = getTrueWeight(currentLoc, grid.get(i))
                        + getTrueWeight(grid.get(i), nextLoc);
            }
        }

        // Direct path is the best path.
        if (minIndex == -1) {
            //System.out.println("Direct path is the best.");
            path.add(nextLoc);
            return moveInPath(currentLoc, nextLoc);
        }

        //System.out.println("Current point: " + currentLoc.x + " " + currentLoc.y);
        //System.out.println("Destination point: " + nextLoc.x + " " + nextLoc.y);
        //System.out.println("Taking a deviation through: " + grid.get(minIndex).x + " " + grid.get(minIndex).y);
        path.add(grid.get(minIndex));
        path.add(nextLoc);

        return moveInPath(currentLoc, grid.get(minIndex));
    }

    // Get the grid of intermediate points.
    private ArrayList<Point> getGrid(Point currentLoc, Point nextLoc) {
        double maxLength = Point.getDistance(currentLoc, nextLoc);
        double step = 0.04;    // TODO: Determine the best step size.
        int gridLength = 40;    // TODO: Determine the optimum number of points.

        ArrayList<Point> grid = new ArrayList<Point>();

        Point p = new Point(nextLoc.x - (gridLength/2) * step, nextLoc.y - (gridLength/2) * step);

        for (int i = 0; i < gridLength; ++i) {
            for (int j = 0; j < gridLength; ++j) {
                double x = p.x + i*step;
                double y = p.y + j*step;

                // Check if it's an invalid point.
                if (x < 0 || y < 0 || x > 10.0 || y > 10.0) {
                    continue;
                }

                Point p1 = new Point(x, y);
                if (Point.getDistance(currentLoc, p1) >= maxLength) {
                    continue;
                }

                grid.add(p1);
            }
        }

        // System.out.println("Grid size: " + grid.size());

        return grid;
    }

    // Below 2 methods are borrowed from the simulator.
    // checks if (a --- > b) line segment intersects circle radius 0.01 around t
    private static boolean checkCrossedInterpolation(Point t, Point a, Point b) {
        Point p = getInterpolatedIntersectionPoint(t, a, b);
        if(p.x == -1 && p.y == -1) return false;
        else return true;
    }

    private static Point getInterpolatedIntersectionPoint(Point centre, Point e, Point l) {
        double r = 0.01;
        Point d = Point.getDirection(e, l);
        Point f = Point.getDirection(centre, e);
        double a = Point.dot(d,d);
        double b = 2 * Point.dot(f,d);
        double c = Point.dot(f,f) - r*r;
        double discr = b*b - 4*a*c;
        if(discr < 0) {
            return new Point(-1, -1);
        }
        discr = Math.sqrt(discr);
        double t1 = (-b - discr)/(2*a);
        double t2 = (-b + discr)/(2*a);


        if(t1 >=0 && t1 <= 1) {
            return Point.sum(e, Point.multiply(d,t1));
        } else if(t2 >= 0 && t2 <= 1) {
            return new Point(-1, -1);
        } else {
            return new Point(-1, -1);
        }
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

        return dist/speed;
    }

    // Gets the speed factor in the direction of the angle.
    private double calcSpeed(double angle) {
        Point p = new Point(2.5*Math.cos(angle) - 0.5, 5 * Math.sin(angle));
        return Point.getDistance(p, new Point(0, 0));
    }

    /**
    * visited_set.get(i) is a set of targets that the ith player has visited.
    */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set) {
        this.visited_set = visited_set;
    }
}
