package sail.g6;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.io.*;
// import java.io.PrintWriter;
// import java.io.File;

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

    // Oliver added these three attributes below. isVisited might be deprecated now.
    int isVisited[][];
    PrintWriter writer;// this lets us write to the file playerLocationData.txt
    int roundNumber = 0;
    int pointThreshold = 100;// if there are more points then this then we'll start in a corner.


    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        wind = wind_direction;


        double eps = 0.5;
        if (t >= pointThreshold){
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
            // System.out.println(wind);
            // System.out.println(fourCorners_test);
            Point cornerStart = new Point(0,0);
            double angleBetween = 0.0;
            double minPerpendicularAngleDifference = Double.MAX_VALUE;
            for (int i = 0; i < fourCorners_test.size(); i++){
                System.out.println("min perp difference " + minPerpendicularAngleDifference);
                angleBetween = Point.angleBetweenVectors(wind, fourCorners_test.get(i));
                System.out.println("angle between " + angleBetween + "\n");
                if(Math.abs(90.0 - angleBetween ) <= minPerpendicularAngleDifference){
                   cornerStart =  fourCorners.get(i);
                   minPerpendicularAngleDifference = Math.abs(90.0 - angleBetween );
                }
            }
            // System.out.println("wind direction");
            // System.out.println(wind.x);
            // System.out.println(wind.y);
            // System.out.println("Starting at x, y");
            // System.out.println(cornerStart.x);
            // System.out.println(cornerStart.y);
            initial = cornerStart;
        }else{
            // prevLoc = new ArrayList<>();
            // path = new ArrayList<>();
            Point bias = Point.rotateCounterClockwise(wind_direction, Math.PI / 2);
            double bias_x = bias.x * 4;
            double bias_y = bias.y * 4;
            initial = new Point(5 + bias_x, 5 + bias_y);
        }
        prevLoc = new ArrayList<>();
        path = new ArrayList<>();
        double speed = Simulator.getSpeed(initial, wind_direction);
        prevLoc.add(0, initial);
        return initial;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
        isVisited = new int[targets.size()][group_locations.size()];
        // File myFile = new File("playerLocationData.txt");
        // myFile.getParentFile().mkdirs();
        // File file = new File ("playerLocationData_2.txt");
        // file.getParentFile().mkdirs();
        try{
            this.writer = new PrintWriter("playerLocationData.txt");
            // PrintWriter printWriter = new PrintWriter(file);
            // this.writer.close();
            // System.out.println(System.getProperty("user.dir"));
            // printWriter.close();       
        }
        catch (FileNotFoundException ex)  {
            System.out.println("ok");
            // insert code to run when exception occurs
        }
        this.writer.println("Number of players: " + group_locations.size());
        this.writer.println("Number of targets: " + targets.size());
        this.writer.println("TargetLocations");
        for (int i = 0; i < targets.size(); i++) {
            this.writer.println("Target number: " + i );
            this.writer.println("X: " + targets.get(i).x);
            this.writer.println("Y: " + targets.get(i).y);

        }
        this.writer.println("******************************\n\n");
        this.writer.close();

        // this.writer = new PrintWriter("playerLocationData.txt", "UTF-8");
        // PrintWriter temp = new PrintWriter("playerLocationData.txt", "UTF-8");
    }

    @Override
    public Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms) {

        if (visited_set != null && visited_set.get(id).size() == targets.size()) {
            // This is if we have finished visiting all targets.
            nextTarget = targets.size();
            return findAngle(group_locations.get(id), initial, dt);
        }
        else {
            // System.out.println("Selecting next target.");
            // Here is the logic to decide which target to head to next.
            // Now we just consider the nearest target.
            double min = 1e9;
            int mark = 0;
            for (int i = 0; i < targets.size(); i++) {
                if (visited_set != null && visited_set.get(id).contains(i)) continue;
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
                if (x < 0.1 || y < 0.1 || x > 9.9 || y > 9.9) {
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
        // movementPredictionFileWrite(group_locations, visited_set);
        roundNumber += 1;
        this.visited_set = visited_set;
    }



    private void movementPredictionFileWrite(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set){
        /*
            writes information to the outfile which can be used by outside scripts for some good ol' machine learning

            Right now we've already wrote the location of all the targets in init, and this function will simply write
                the current locations of all players and which sets they've visited.
            In the future we might try writing stuff about how far it is to each target, the point distribution at each target,
            ....

            


        */


        try{
            this.writer = new PrintWriter(new FileOutputStream(
        new File("playerLocationData.txt"), true /* append = true */));     
        }
        catch (FileNotFoundException ex)  {
            System.out.println("ok");
            // insert code to run when exception occurs
        }
        this.writer.println("round number " + roundNumber);
        this.writer.println("Printing locations of players");
        for (int i = 0; i < group_locations.size(); i++) {
            this.writer.println("location for player " + i);
            this.writer.println("X: "+ group_locations.get(i).x);
            this.writer.println("Y: "+ group_locations.get(i).y);
        }
        this.writer.println("Printing visited locations");
        this.writer.println(visited_set);
        for(int i = 0; i < visited_set.size(); i++){
            for(Integer targetID : visited_set.get(i)){
                this.writer.println("Visited Location for player " + i + " " + targetID);
            }
            // for(int j = 0; j < visited_set.get(i).size(); j ++){
            //     this.writer.println("Visited Location for player " + i + " " + visited_set.get(i).get(j));
            // }

        }
        // this.writer.println("Points at Each Location");
        // this.writer.println(targets);
        // this.writer.println(isVisited);


        // this.writer.println("Target Scores");
        // for (int i = 0; i < targets.size(); i++) {
        //     for (int j = 0; j < group_locations.size(); j++) {
        //         this.writer.println("target: " + i + " player: " + j + " score: " + isVisited[i][j]);
        //         // if (isVisited[i][j] == 1) continue;
        //         // if (Point.getDistance(targets.get(i), group_locations.get(j)) < 0.1) {
        //         //     isVisited[i][j] = 1;
        //         // }
        //     }
        // }

        this.writer.println("************\n\n\n");
        this.writer.close();

    }
}
