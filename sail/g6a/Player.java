package sail.g6a;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player extends sail.sim.Player {
    List<Point> targets;
    Map<Integer, Set<Integer>> visited_set;
    Random gen;
    int id;
    Point initial;

    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        Point bias = Point.rotateCounterClockwise(wind_direction, Math.PI / 2);
        double bias_x = bias.x * 4;
        double bias_y = bias.y * 4;
        initial = new Point(5 + bias_x, 5 + bias_y);
        double speed = Simulator.getSpeed(initial, wind_direction);
        return initial;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
    }

    @Override
    public Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms) {
        // testing timeouts... 
        // try {
        //     TimeUnit.MILLISECONDS.sleep(1);
        // } catch(Exception ex) {
        //     ;
        // }
        // just for first turn
        if(visited_set == null) {
            double min = 1e9;
            int mark = 0;
            for (int i = 0; i < targets.size(); i++) {
                double dist = Point.getDistance(group_locations.get(id), targets.get(i));
                if (dist < min) {
                    min = dist;
                    mark = i;
                }
            }
            return Point.getDirection(group_locations.get(id), targets.get(mark));
        }
        else if(visited_set.get(id).size() == targets.size()) {
            //this is if finished visiting all
            return Point.getDirection(group_locations.get(id), initial);
        }
        else { 
            double min = 1e9;
            int mark = 0;
            for (int i = 0; i < targets.size(); i++) {
                if (visited_set.get(id).contains(i)) continue;
                double dist = Point.getDistance(group_locations.get(id), targets.get(i));
                if (dist < min) {
                    min = dist;
                    mark = i;
                }
            }
            return Point.getDirection(group_locations.get(id), targets.get(mark));
        }
    }

    /**
    * visited_set.get(i) is a set of targets that the ith player has visited.
    */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set) {
        this.visited_set = visited_set;
    }
}
