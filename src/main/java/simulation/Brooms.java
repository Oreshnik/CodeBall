package simulation;

import game_engine.Constants;
import game_engine.GameEngine;
import model.Action;
import mymodel.Game;
import mymodel.Vector;
import rendering.Render;
import strategy.Strategy;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.PI;

public class Brooms {
    private static Vector vBall = new Vector(0,0,0), vRobot = new Vector(0, 0, 0);
    public static List<Action> simulate(Game g, int timeToRan, BroomsOption option, List<Action> prevBest) {
        int timeLimit = 27;
        List<Branch> branches = generateActions(g, timeToRan);
        long start = System.nanoTime();
        int len = 0, s = 0;

        while ((System.nanoTime() - start)  * 0.000001 < timeLimit && len < 4 * timeToRan) {
            boolean hasUnfinishedBranches = false;

            for (int i = 0; i < branches.size(); i++) {

                Branch branch = branches.get(i);
                Game game = branch.game;
                if (option.isBranchFinished(branch)) {
                    continue;
                }
                prepareCollision(game);
                if (len != 0 && game.robots[0].action.jump_speed == 0
                        && (len % timeToRan == (timeToRan) / 2 || len % timeToRan == 0)) {
                    Branch jump = createJumpBrunch(branch);
                    branches.add(jump);
                }
                if (timeToRan >= 10 && (len == timeToRan / 2 || len == timeToRan)
                        && !branch.justBrunched && branch.game.robots[0].action.jump_speed == 0 && branch.game.robots[0].touch) {
                    branches.add(createRotateBranch(branch));
                }

                if (branch.game.robots[0].nitroAmount >= 5 && (timeToRan - len == 21)
                        && !branch.justBrunched) {
                    branches.add(createNitroBranch(branch, 15));
                }
                tick(option, branch);

                hasUnfinishedBranches = true;
                branch.len = len;
                branch.justBrunched = false;
                if (Strategy.isTest && len % 2 == 0) {
                    if (!branch.game.robots[0].action.use_nitro) {
                        Render.addSphere(branch.game.robots[0].position, 0.05, 0.01 * i, 0, 1, 0.7);
                    } else {
                        Render.addSphere(branch.game.robots[0].position, 0.05, 0.01 * i, 1, 0, 0.7);
                    }
                }
                s ++;
            }
            len ++;
            if (!hasUnfinishedBranches) {
                break;
            }
        }
        Branch best = branches.get(getBest(branches, option));
        double fullTime = (System.nanoTime() - start) * 0.000001;
        if (Strategy.isTest || fullTime > 30) {
            System.out.println(String.format("%s %d %d %d %s %.2f %.1f %d", option.addStr(), timeToRan, len, best.len,
                    best.game.ball.collision, option.calcBranchRate(best), fullTime, s));
        }

        if (prevBest != null && prevBest.size() > 0) {
            Branch prevBestBranch = proceedPrevBest(prevBest, g, option);
            double prevRate = option.calcBranchRate(prevBestBranch);
            if (prevRate > option.calcBranchRate(best)) {
                best = prevBestBranch;
                if (Strategy.isTest) {
                    System.out.println(String.format("choose old %.2f", prevRate));
                }
            }
        }
        if (Strategy.isTest) {
            if (best.game.ball.collision) {
                Render.addSphere(best.game.ball.position, 0.5, 1, 0, 0, 0.7);
            }
            if (!best.actions.isEmpty()) {
                Render.addText(option.addStr() + " a" + timeToRan + " L" + len + " b" + branches.size() + " bl" + best.len + " " + best.game.ball.collision
                        + String.format(" r%.2f t%.0f v%.2f j%.0f", option.calcBranchRate(best), (System.nanoTime() - start) * 0.000001,
                        Math.sqrt(best.actions.get(0).target_velocity_x * best.actions.get(0).target_velocity_x
                                + best.actions.get(0).target_velocity_z * best.actions.get(0).target_velocity_z),
                        best.actions.get(0).jump_speed));
            }
        }
        option.afterCalc(best);
        return best.actions;
    }

    private static void tick(BroomsOption option, Branch branch) {
        Game game = branch.game;
        branch.lastBallDz = branch.game.ball.speed.dz;
        GameEngine.tick(game, 10);
        option.checkDistance(branch);
        if (!game.robots[0].touch) {
            branch.inTheAir ++;
        }
        if (game.robots[0].action.use_nitro && game.robots[0].nitroAmount > 0) {
            branch.nitro ++;
        }
        branch.actions.add(game.robots[0].action);
    }

    public static void checkDistance(Branch branch) {
        if (branch.game.ball.position.dz < branch.game.robots[0].position.dz) {
            return;
        }
        double distance = branch.game.ball.position.getSquaredDistance(branch.game.robots[0].position);
        if (distance < branch.minSquaredDistance) {
            branch.minSquaredDistance = distance;
            branch.getMinSquaredDistanceY = branch.game.robots[0].position.dy;
        }
    }

    private static void prepareCollision(Game game) {
        if (game.ball.position.getSquaredDistance(game.robots[0].position) < 16) {
            vBall.set(game.ball.speed).multiply(1.0 / Constants.TICKS_PER_SECOND).add(game.ball.position);
            vRobot.set(game.robots[0].speed).multiply(1.0 / Constants.TICKS_PER_SECOND).add(game.robots[0].position);
            double distance = vBall.getSquaredDistance(vRobot);

            if (distance <= (Constants.BALL_RADIUS + Constants.ROBOT_MAX_RADIUS) * (Constants.BALL_RADIUS + Constants.ROBOT_MAX_RADIUS)) {
                game.robots[0].action = new Action();
                game.robots[0].action.jump_speed = 15;
            }
        }
    }

    private static int getBest(List<Branch> branches, BroomsOption option) {
        double bestRate = -Double.MAX_VALUE;
        int bestInd = -1;
        for (int i = 0; i < branches.size(); i++) {
            double rate = option.calcBranchRate(branches.get(i));
            if (rate > bestRate) {
                bestRate = rate;
                bestInd = i;
            }
        }

        return bestInd;
    }

    private static List<Branch> generateActions(Game g, int len) {
        double squaredDistance = g.ball.position.getSquaredDistance(g.robots[0].position);
        List<Branch> branches = new ArrayList<>();

        branches.add(createBranch(g, squaredDistance, new Action()));
        Vector vector;
        if (g.robots[0].speed.dx > 3 || g.robots[0].speed.dz > 3) {
            vector = g.robots[0].speed.copy().setLength(Constants.ROBOT_MAX_GROUND_SPEED - 0.01);
        } else {
            vector = g.ball.position.copy().subtract(g.robots[0].position);
            vector.dy = 0;
            vector.setLength(Constants.ROBOT_MAX_GROUND_SPEED - 0.01);
        }

        if (g.robots[0].touch) {
            Vector vector2 = vector.copy().multiply(0.5);
            double r = PI / 12;
            for (double a = r; a < 2 * PI; a += r) {
                Vector v = vector.copy().rotate(a);
                branches.add(createBranch(g, squaredDistance, createAction(v)));
                if (a < PI / 2 || a > 3 * PI / 2) {
                    Vector v2 = vector2.copy().rotate(a);
                    branches.add(createBranch(g, squaredDistance, createAction(v2)));
                }
            }
            Vector v = vector.copy().rotate(r / 2);
            branches.add(createBranch(g, squaredDistance, createAction(v)));
            v = vector.copy().rotate(-r / 2);
            branches.add(createBranch(g, squaredDistance, createAction(v)));
            v = vector.copy().rotate(r / 4);
            branches.add(createBranch(g, squaredDistance, createAction(v)));
            v = vector.copy().rotate(-r / 4);
            branches.add(createBranch(g, squaredDistance, createAction(v)));

            for (int i = 15; i > 0; i -= 3) {
                //if (time < i * 4) {
                Action action = new Action();
                action.jump_speed = i;
                branches.add(createBranch(g, squaredDistance, action));
                //}
            }
            Action action = new Action();
            action.target_velocity_z = g.robots[0].speed.dz;
            action.target_velocity_x = g.robots[0].speed.dx;
            branches.add(createBranch(g, squaredDistance, action));
        }

        if (g.robots[0].nitroAmount >= 5) {
            if (len <= 20) {
                branches.add(createNitroBranch(createBranch(g, squaredDistance, new Action()), 0));
                if (g.robots[0].touch) {
                    branches.add(createNitroBranch(createBranch(g, squaredDistance, new Action()), 5));
                    branches.add(createNitroBranch(createBranch(g, squaredDistance, new Action()), 10));
                    branches.add(createNitroBranch(createBranch(g, squaredDistance, new Action()), 15));
                }
            }
            branches.add(createNitroBranch(createBranch(g, squaredDistance, new Action()), 15));
            Action action = new Action();
            action.use_nitro = true;
            action.jump_speed = 15;
            action.target_velocity_y = Constants.MAX_ENTITY_SPEED;
            Branch up = createBranch(g, squaredDistance, action);
            branches.add(up);

            Action action1 = new Action();
            action1.use_nitro = true;
            action1.jump_speed = 15;
            action1.target_velocity_y = 27;
            action1.target_velocity_x = 13;
            Branch upLeft = createBranch(g, squaredDistance, action1);
            branches.add(upLeft);

            Action action2 = new Action();
            action2.use_nitro = true;
            action2.jump_speed = 15;
            action2.target_velocity_y = 27;
            action2.target_velocity_x = -13;
            Branch upRight = createBranch(g, squaredDistance, action2);
            branches.add(upRight);

            Action action3 = new Action();
            action3.use_nitro = true;
            action3.jump_speed = 15;
            action3.target_velocity_y = 22;
            action3.target_velocity_x = 20;
            Branch upLeftLeft = createBranch(g, squaredDistance, action3);
            branches.add(upLeftLeft);

            Action action4 = new Action();
            action4.use_nitro = true;
            action4.jump_speed = 15;
            action4.target_velocity_y = 22;
            action4.target_velocity_x = -20;
            Branch upRightRight = createBranch(g, squaredDistance, action4);
            branches.add(upRightRight);
        }
        return branches;
    }

    private static Branch proceedPrevBest(List<Action> actions, Game g, BroomsOption option) {
        Branch branch = createBranch(g, g.ball.position.getSquaredDistance(g.robots[0].position), actions.get(0));
        for (int i = 0; i < actions.size(); i++) {
            branch.game.robots[0].action = actions.get(i);
            if (option.isBranchFinished(branch)) {
                break;
            }
            tick(option, branch);
            branch.len = i;
        }
        return branch;
    }

    private static Branch createBranch(Branch trunk, Action action) {
        Branch branch = createBranch(trunk.game, trunk.minSquaredDistance, action);
        branch.minSquaredDistance = trunk.minSquaredDistance;
        branch.len = trunk.len;
        branch.lastBallDz = trunk.lastBallDz;
        branch.inTheAir = trunk.inTheAir;
        branch.nitro = trunk.nitro;
        branch.getMinSquaredDistanceY = trunk.getMinSquaredDistanceY;
        branch.minSquaredDistance = trunk.minSquaredDistance;
        branch.actions.addAll(trunk.actions);
        return branch;
    }

    private static Branch createBranch(Game g, double squaredDistance, Action action) {
        Branch branch = new Branch();
        branch.game = new Game(g);
        branch.minSquaredDistance = squaredDistance;
        branch.getMinSquaredDistanceY = g.robots[0].position.dy;
        branch.game.robots[0].action = new Action();
        branch.game.robots[0].action.jump_speed = action.jump_speed;
        branch.game.robots[0].action.target_velocity_x = action.target_velocity_x;
        branch.game.robots[0].action.target_velocity_y = action.target_velocity_y;
        branch.game.robots[0].action.target_velocity_z = action.target_velocity_z;
        branch.game.robots[0].action.use_nitro = action.use_nitro;
        return branch;
    }

    private static Branch createJumpBrunch(Branch branch) {
        Action action = new Action();
        action.jump_speed = 15;
        Branch jump = createBranch(branch, action);
        jump.justBrunched = true;

        return jump;
    }

    private static Branch createRotateBranch(Branch branch) {
        Action action = new Action();
        Vector vector = branch.game.ball.position.copy().subtract(branch.game.robots[0].position);
        vector.dy = Constants.ROBOT_RADIUS;
        vector.setLength(Constants.ROBOT_MAX_GROUND_SPEED);
        action.target_velocity_x = vector.dx;
        action.target_velocity_z = vector.dz;
        Branch rotate = createBranch(branch, action);
        rotate.justBrunched = true;
        return rotate;
    }

    private static Branch createNitroBranch(Branch branch, int jump) {

        Vector vector = branch.game.ball.position.copy().subtract(branch.game.robots[0].position);
        if (jump == 0 && branch.game.robots[0].touch) {
            vector.setLength(Constants.ROBOT_MAX_GROUND_SPEED - 0.001);
        } else {
            vector.setLength(Constants.MAX_ENTITY_SPEED - 0.001);
        }
        Action action = new Action();
        action.target_velocity_x = vector.dx;
        action.target_velocity_z = vector.dz;
        action.target_velocity_y = vector.dy;
        action.jump_speed = jump;
        action.use_nitro = true;
        Branch nitro = createBranch(branch, action);
        nitro.justBrunched = true;
        return nitro;
    }

    private static Action createAction(Vector vector) {
        Action action = new Action();
        action.target_velocity_x = vector.dx;
        action.target_velocity_z = vector.dz;
        return action;
    }

    public static class Branch {
        public Game game;
        public double minSquaredDistance;
        public double getMinSquaredDistanceY;
        public int len;
        public double lastBallDz;
        public int inTheAir = 0;
        public int nitro = 0;
        boolean justBrunched;
        List<Action> actions = new ArrayList<>();
    }

    public interface BroomsOption {
        double calcBranchRate(Branch branch);
        boolean isBranchFinished(Branch branch);
        void checkDistance(Branch branch);
        String addStr();
        void afterCalc(Branch branch);
    }
}
