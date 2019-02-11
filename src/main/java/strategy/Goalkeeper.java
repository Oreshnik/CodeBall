package strategy;

import game_engine.Constants;
import game_engine.GameEngine;
import messaging.Message;
import messaging.MessageType;
import messaging.Post;
import model.Action;
import model.NitroPack;
import mymodel.Game;
import mymodel.Nitro;
import mymodel.Robot;
import mymodel.Vector;
import rendering.Render;
import simulation.Brooms;
import steering_behaviors.Arrival;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static game_engine.GameEngine.arena;

public class Goalkeeper {
    private static final Vector home = new Vector(0, 1, -40);
    private static final Vector checkCenter = new Vector(0, 2, -100);
    private static final double checkRadius = 80;
    public static final Vector THREAT_CENTER = new Vector(0, 1, -80);
    public static final int THREAT_RADIUS = 65;
    private static final int checkTick = 60;
    public static final int TIME_FOR_SIMULATION = 50;
    private Robot robot;
    public int robotId;
    public State state;
    private BallPrediction ballPrediction;
    private Game game;
    private Vector target;
    private int[] chain = null;
    private List<Action> bestActions = null;
    private Post post;

    public Goalkeeper(int robotId, BallPrediction ballPrediction, Post post) {
        this.post = post;
        this.robotId = robotId;
        this.state = State.RETURN;
        this.ballPrediction = ballPrediction;
    }

    public void update(Game game) {
        robot = game.robots[robotId - 1];
        this.game = game;
        changeState(game);
        target = null;
    }

    private void changeState(Game game) {
        if (state == State.WAIT) {
            if (defendIsNeeded(game)) {
                state = State.REPULSE;

            } else if (canCollectNitroPack()) {
                state = State.COLLECT;
            }

        } else if (state == State.RETURN) {
            if (defendIsNeeded(game)) {
                state = State.REPULSE;


            } else if (canCollectNitroPack()) {
                state = State.COLLECT;

            } else if (robot.position.getSquaredDistance(home) < 0.25) {
                state = State.WAIT;
            }

        } else if (state == State.REPULSE) {
            if (!defendIsNeeded(game)) {
                state = State.RETURN;
            }

        } else if (state == State.COLLECT) {
            if (defendIsNeeded(game)) {
                state = State.REPULSE;
            } else if (!canCollectNitroPack()) {
                state = State.RETURN;
            }
        }
    }

    private boolean canCollectNitroPack() {
        return game.nitroPacks.length > 0 && robot.nitroAmount < 15 && game.ball.position.dz > 0 && game.ball.speed.dz > 0
                && Arrays.stream(game.nitroPacks).anyMatch(n -> n.position.dz < 0 && n.respawnTicks == null);
    }

    public Action doAction() {
        if (state == State.WAIT) {
            return new Action();
        } if (state == State.REPULSE) {
            return doRepulse();
        } if (state == State.COLLECT) {
            return doCollect();
        }
        if (!robot.touch && robot.nitroAmount > 0 && robot.position.dy > 3) {
            Action action = new Action();
            action.target_velocity_y = - Constants.MAX_ENTITY_SPEED;
            action.use_nitro = true;
            return action;
        }
        return Arrival.arrive(robot, home);
    }

    private Action doCollect() {
        Nitro nearest = null;
        double minDistance = 100500;
        for (Nitro nitroPack : game.nitroPacks) {
            if (nitroPack.respawnTicks != null || nitroPack.position.dz > 0) {
                continue;
            }
            double distance = nitroPack.position.getDistance(robot.position);
            if (distance < minDistance) {
                nearest = nitroPack;
                minDistance = distance;
            }
        }
        if (nearest != null) {
            return Arrival.arrive(robot, nearest.position);
        }
        return new Action();
    }

    private Action doRepulse() {
        double distance = game.ball.getDistanceBetween(robot);
        int timeToRan = (int) Math.ceil(distance * Constants.TICKS_PER_SECOND / Constants.ROBOT_MAX_GROUND_SPEED);
        bestActions = Brooms.simulate(createGameForSimulation(), timeToRan, createRepulseOption(), bestActions);
        if (bestActions == null || bestActions.isEmpty()) {
            return new Action();
        } else {
            return bestActions.remove(0);
        }
    }

    private Game createGameForSimulation() {
        List<Robot> robots = new ArrayList<>();
        robots.add(robot);
        for (Robot r : game.robots) {
            if (r == robot) {
                continue;
            }
            if (r.position.getSquaredDistance(game.ball.position) <= 30 * 30
                    || r.position.getSquaredDistance(robot.position) <= 30 * 30) {
                robots.add(r);
            }
        }
        Game newGame = new Game(robots, game.ball);
        newGame.currentTick = game.currentTick;
        return newGame;
    }

    public  Brooms.BroomsOption createRepulseOption() {
        return new Brooms.BroomsOption() {
            @Override
            public double calcBranchRate(Brooms.Branch branch) {
                return calcRate(branch);
            }

            @Override
            public boolean isBranchFinished(Brooms.Branch branch) {
                return isFinished(branch);
            }

            @Override
            public void checkDistance(Brooms.Branch branch) {
                checkHorizontalDistance(branch);
            }

            @Override
            public String addStr() {
                return game.currentTick + " gkr";
            }

            @Override
            public void afterCalc(Brooms.Branch branch) {
                if (branch.game.ball.collision && branch.game.robots[0].collision) {
                    afterCollision(branch);
                }
            }
        };
    }

    private void afterCollision(Brooms.Branch branch) {
        if (branch.len >= ballPrediction.getNextPositions().size()) {
            return;
        }
        List<Vector> ballPositions = new ArrayList<>(ballPrediction.getNextPositions().subList(0, branch.len));
        Game ballGame = new Game(branch.game);
        ballGame.robots = new Robot[0];
        for (int i = ballPositions.size(); i < 200; i ++) {
            GameEngine.tick(ballGame, 4);
            ballPositions.add(ballGame.ball.position.copy());
            if (Strategy.isTest) {
                Render.addSphere(ballGame.ball.position, 0.1, 0, 0, 1, 1);
            }
        }
        post.putMessage(new Message(MessageType.GOALKEEPER_REPULSE_BALL, game.currentTick, ballPositions, branch.len));
    }

    private boolean defendIsNeeded(Game game) {

        double distance = game.ball.getDistanceBetween(robot);
        Message forwardRepulse = post.getMessage(MessageType.FORWARD_REPULSE_BALL);
        if (forwardRepulse != null && game.ball.getDistanceBetween(forwardRepulse.robot) < distance) {
            return false;
        }

        double time = distance * Constants.TICKS_PER_SECOND / Constants.ROBOT_MAX_GROUND_SPEED;
        if (time < TIME_FOR_SIMULATION + 15 && THREAT_CENTER.getSquaredDistance(game.ball.position) <= THREAT_RADIUS * THREAT_RADIUS
                && game.ball.speed.dz < 10) {
            return true;
        }
        for (int t = 0; t < checkTick; t++) {
            Vector ballPosition = ballPrediction.getNextPositions().get(t);
            if (ballPosition.dz < -arena.depth / 2) {
                return true;
            }
        }
        return false;
    }

    private static double calcRate(Brooms.Branch branch) {
        double rate = 0;
        if (branch.game.ball.collision && branch.game.robots[0].collision) {
            rate = branch.game.ball.speed.dz * 10; //хочу ударить посильнее в сторону ворот
            if (branch.game.ball.speed.dz < 0 && branch.lastBallDz > branch.game.ball.speed.dz) {
                rate -= 500; //если в результате удара мяч летит ближе к воротам, это плохо!
            }
            rate -= branch.game.robots[0].speed.dy * 3; //если робот отлетает вверх, это плохо
            rate -= new Vector(home.dx, branch.game.robots[0].position.dy, home.dz).getDistance(branch.game.robots[0].position) * 3; //немного накажем за выбегание
            if (branch.game.ball.speed.dz >= 0) {
                rate += calculateOpponentDistanceRate(branch);
            }

        } else {
            rate = -Math.sqrt(branch.minSquaredDistance); // чем меньше дистанция до мяча, тем ближе нас подведет ветка
            rate -= new Vector(home.dx, branch.game.robots[0].position.dy, home.dz).getDistance(branch.game.robots[0].position) / 2;
            if (branch.game.robots[0].position.dz > -46) {
                rate += (1 - branch.getMinSquaredDistanceY) * 2; // нет смысла зависать в воздухе, чтобы просто приблизиться к мячу, а в воротах пусть скочет
            }
        }
        rate -= branch.inTheAir;
        rate -= branch.nitro * 4;
        if (Math.abs(branch.game.ball.position.dz) > arena.depth / 2 + branch.game.ball.radius - 0.3) {
            rate -= 1000; //гол, все плохо
        }
        if (branch.game.ball.position.dz < branch.game.robots[0].position.dz) {
            rate += branch.game.ball.position.dz - branch.game.robots[0].position.dz; //если оказались за мячом, то стараемся обойти мяч
        }
        rate -= branch.len;
        return rate;
    }

    private static double calculateOpponentDistanceRate(Brooms.Branch branch) {
        List<Robot> opponents = new ArrayList<>();
        double minSquaredDistance = arena.width * arena.width;
        Game game = new Game(branch.game);
        for (Robot robot : game.robots) {
            if (!robot.isTeammate) {
                opponents.add(robot);
            }
        }
        if (opponents.isEmpty()) {
            return 0;
        }

        game.robots[0].action = null;
        boolean intersect = false;
        boolean collision = false;
        for (int i = 0; i < 70; i++) {
            GameEngine.tick(game, 4);
            for (int r = 0; r < opponents.size(); r++) {
                if (Math.abs(opponents.get(r).position.dz - game.ball.position.dz) < Constants.BALL_RADIUS + Constants.ROBOT_RADIUS + 0.5) {
                    double sDistance = opponents.get(r).position.getSquaredDistance(game.ball.position);
                    if (sDistance < minSquaredDistance) {
                        minSquaredDistance = sDistance;
                    }
                    intersect = true;
                }
                if (opponents.get(r).collision) {
                    collision = true;
                }
            }
            if (Math.abs(game.ball.position.dz) > arena.depth / 2 + Constants.BALL_RADIUS - 0.3) {
                return -500;
            }
            if (Strategy.isTest) {
                Render.addSphere(game.ball.position, 0.05, 1, 1, 1, 1);
            }
        }
        if (!intersect) {
            return 0;
        }
        //return 8 * (arena.width / 4 - Math.sqrt(minSquaredDistance));
        return Math.min(10, Math.sqrt(minSquaredDistance)) * 10 + (collision ? -100 : 0);
    }

    private boolean isFinished(Brooms.Branch branch) {
        return branch.game.ball.collision && branch.game.robots[0].collision //удар по мячу
                || Math.abs(branch.game.ball.position.dz) > arena.depth / 2 + branch.game.ball.radius // гол
                || branch.game.ball.speed.dz > 10 // мяч уже летит от ворот
                || (branch.game.robots[0].position.getSquaredDistance(Goalkeeper.THREAT_CENTER) > Goalkeeper.THREAT_RADIUS * Goalkeeper.THREAT_RADIUS // выход за пределы защитного круга
                && branch.game.ball.position.getSquaredDistance(Goalkeeper.THREAT_CENTER) > Goalkeeper.THREAT_RADIUS * Goalkeeper.THREAT_RADIUS);

    }

    private static void checkHorizontalDistance(Brooms.Branch branch) {
        if (branch.game.ball.position.dz < branch.game.robots[0].position.dz
                || branch.game.ball.speed.dz > 10
                || (branch.game.robots[0].position.getSquaredDistance(Goalkeeper.THREAT_CENTER) > Goalkeeper.THREAT_RADIUS * Goalkeeper.THREAT_RADIUS // выход за пределы защитного круга
                && branch.game.ball.position.getSquaredDistance(Goalkeeper.THREAT_CENTER) > Goalkeeper.THREAT_RADIUS * Goalkeeper.THREAT_RADIUS)) {
            return;
        }
        double distance = branch.game.robots[0].position.getSquaredDistance(branch.game.ball.position.dx,
                branch.game.robots[0].position.dy, branch.game.ball.position.dz - Constants.BALL_RADIUS - Constants.ROBOT_RADIUS);
        if (distance < branch.minSquaredDistance) {
            branch.minSquaredDistance = distance;
            branch.getMinSquaredDistanceY = branch.game.robots[0].position.dy;
        }
    }

    public void render() {
        Render.addSphere(home, 0.5, 1, 0, 0, 0.5);
        if (target != null) {
            Render.addSphere(target, 0.5, 0, 0, 1, 0.7);
        }
    }

    public enum State {
        WAIT, RETURN, REPULSE, COLLECT
    }
}
