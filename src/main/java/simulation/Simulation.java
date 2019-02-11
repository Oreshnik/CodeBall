package simulation;

import game_engine.Constants;
import game_engine.GameEngine;
import model.Action;
import mymodel.Ball;
import mymodel.Game;
import mymodel.Robot;
import mymodel.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static game_engine.Constants.ROBOT_MAX_GROUND_SPEED;
import static java.lang.Math.PI;

/*WARNING this class wasn't used in late versions*/
public class Simulation {
    //private static XORShiftRandom random = new XORShiftRandom(100500);
    private static Random random = new Random(100500);
    public static List<Vector> path = new ArrayList<>();
    public static List<Vector> tmpPath = new ArrayList<>();

    public static List<Action> simulateGoalKeeperKick(List<Action> actionList, Robot robot, Ball ball, Vector target) {
        if (actionList == null) {
            actionList = initListAction(robot, target);
        } else {
            actionList.remove(0);
            Action action = new Action();
            action.target_velocity_x = actionList.get(actionList.size() - 1).target_velocity_x;
            action.target_velocity_y = actionList.get(actionList.size() - 1).target_velocity_y;
            actionList.add(action);
        }
        simulateKick(actionList, robot, ball);
        return actionList;
    }

    /*public static List<Action> simulateGoalKeeperKick(Robot robot, Ball ball) {
        List<Action> actionList = initListAction(robot, ball);
        simulateKick(actionList, robot, ball);
        return actionList;
    }*/

    private static void simulateKick(List<Action> actionList, Robot robot, Ball ball) {

        double bestRate = calcRate(actionList, robot, ball);
        long start = System.currentTimeMillis();
        int maxTime = 20;
        int n = 0;
        int changes = 0;
        while (System.currentTimeMillis() - start < maxTime || n < 100) {
            int tick = random.nextInt(actionList.size());
            int type = (System.currentTimeMillis() - start) < maxTime / 2 ? random.nextInt(4) : random.nextInt(5);
            Action action = actionList.get(tick);
            double jump = action.jump_speed;
            double angle = 0;
            Vector tmp = new Vector(0, 0, 0);
            if (type == 4) {
                if (action.jump_speed > 0) {
                    action.jump_speed = 0;
                } else {
                    action.jump_speed = random.nextInt(15) + 1;
                }

            } else {
                angle = (random.nextDouble() * PI / 2 - PI / 4) * (maxTime - (System.currentTimeMillis() - start)) / maxTime;
                for (int t = tick; t < actionList.size(); t++) {
                    Action act = actionList.get(t);
                    tmp.set(act.target_velocity_x, 0, act.target_velocity_y);
                    tmp.rotate(angle);
                    act.target_velocity_x = tmp.dx;
                    act.target_velocity_z = tmp.dz;
                }

            }
            double rate = calcRate(actionList, robot, ball);
            if (rate > bestRate) {
                bestRate = rate;
                path.clear();
                path.addAll(tmpPath);
                changes ++;
            } else {
                action.jump_speed = jump;
                if (Math.abs(angle) > 0) {
                    for (int t = tick; t < actionList.size(); t++) {
                        Action act = actionList.get(t);
                        tmp.set(act.target_velocity_x, 0, act.target_velocity_y);
                        tmp.rotate(-angle);
                        act.target_velocity_x = tmp.dx;
                        act.target_velocity_z = tmp.dz;
                    }
                }
            }
            n++;
        }
        System.out.println("simulations " + n + " changes " + changes);
    }

    private static double calcRate(List<Action> actionList, Robot robot, Ball ball) {
        tmpPath.clear();
        Game game = new Game();
        game.robots = new Robot[] {robot.copy()};
        game.ball = ball.copy();
        double minSDistance = robot.position.getSquaredDistance(ball.position);
        for (int i = 0; i < actionList.size(); i++) {
            game.robots[0].action = actionList.get(i);
            GameEngine.tick(game, 10);
            double sDistance = game.robots[0].position.getSquaredDistance(game.ball.position);
            tmpPath.add(game.robots[0].position.copy());
            if (sDistance < minSDistance) {
                minSDistance = sDistance;
            }
            if (game.ball.collision) {
                break;
            }

        }
        return calcRate(minSDistance, game.ball, game.robots[0]);
    }

    private static List<Action> initListAction(Robot robot, Vector ball) {
        List<Action> actionList = new ArrayList<>();
        Vector vector = ball.copy().subtract(robot.position);
        vector.dy = 0;
        vector.setLength(ROBOT_MAX_GROUND_SPEED - Constants.EPS);
        Action action = new Action();
        action.target_velocity_x = vector.dx;
        action.target_velocity_z = vector.dz;
        for (int i = 0; i < 25; i ++) {
            actionList.add(action);
        }

        return actionList;
    }

    private static double calcRate(double minSDistance, Ball ball, Robot robot) {
        return 10 / minSDistance + ball.speed.dz * 10 - (robot.touch ? 0 : robot.position.dy);
    }
}
