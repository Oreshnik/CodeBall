package strategy;

import game_engine.Constants;
import game_engine.GameEngine;
import messaging.Message;
import messaging.MessageType;
import messaging.Post;
import model.Action;
import mymodel.Game;
import mymodel.Nitro;
import mymodel.Robot;
import mymodel.Vector;
import rendering.Render;
import simulation.Brooms;
import steering_behaviors.Arrival;
import steering_behaviors.Seek;

import java.util.ArrayList;
import java.util.List;

import static game_engine.GameEngine.arena;

public class Forward {
    private static Vector home = new Vector(10, 1, -10);
    private Robot robot;
    public int robotId;
    public State state;
    private BallPrediction ballPrediction;
    private Game game;
    private List<Action> bestActions = null;
    private Post post;

    public Forward(int robotId, BallPrediction ballPrediction, Post post) {
        this.post = post;
        this.robotId = robotId;
        this.state = State.ATTACK;
        this.ballPrediction = ballPrediction;
    }

    public void update(Game game) {
        robot = game.robots[robotId - 1];
        this.game = game;
        changeState(game);

    }

    private void changeState(Game game) {
        if (state == State.WAIT) {
            if (mayAttack(game)) {
                state = State.ATTACK;
            }

        } else if (state == State.RETURN) {
            if (mayAttack(game)) {
                state = State.ATTACK;

            } else if (robot.position.getSquaredDistance(home) < 0.25) {
                state = State.WAIT;
            }

        } else if (state == State.ATTACK) {
            if (!mayAttack(game)) {
                state = State.RETURN;
                bestActions = null;
            }
            if (mayCollect(game)) {
                state = State.COLLECT;
            }
        } else if (state == State.COLLECT) {
            if (!mayCollect(game)) {
                state = State.ATTACK;
            }
        }

    }

    public Action doAction() {
        if (state == State.WAIT) {
            return new Action();
        }
        if (state == State.ATTACK) {
            return attack();
        }
        if (state == State.COLLECT) {
            return collect();
        }
        return Arrival.arrive(robot, home);
    }

    private Action createAction(Vector vector) {
        Action action = new Action();
        action.target_velocity_x = vector.dx;
        action.target_velocity_z = vector.dz;
        return action;
    }

    private boolean mayAttack(Game game) {
        return true;
        /*return *//*game.ball.position.getSquaredDistance(Goalkeeper.THREAT_CENTER) > Goalkeeper.THREAT_RADIUS * Goalkeeper.THREAT_RADIUS
                || game.ball.speed.dz > 10 && *//**//*post.getMessage(MessageType.GOALKEEPER_REPULSE_BALL) == null
                && *//*(post.getMessage(MessageType.FORWARD_REPULSE_BALL) == null || ((Robot) post.getMessage(MessageType.FORWARD_REPULSE_BALL).value).id == robotId);*/
    }

    private boolean mayCollect(Game game) {
        if (game.nitroPacks.length == 0 || robot.nitroAmount > 20) {
            return false;
        }
        double ballDistance = game.ball.position.getDistance(robot.position);
        if (ballDistance < arena.width / 2) {
            return false;
        }
        double nitroDistance = arena.depth * arena.depth;
        for (Nitro nitro : game.nitroPacks) {
            if (nitro.respawnTicks != null) {
                continue;
            }
            double sDistance = robot.position.getSquaredDistance(nitro.position);
            if (sDistance < nitroDistance) {
                nitroDistance = sDistance;
            }
        }
        return Math.sqrt(nitroDistance) < ballDistance;
    }

    private Action collect() {
        double nitroDistance = arena.depth * arena.depth;
        Nitro nearest = null;
        for (Nitro nitro : game.nitroPacks) {
            if (nitro.respawnTicks != null) {
                continue;
            }
            double sDistance = robot.position.getSquaredDistance(nitro.position);
            if (sDistance < nitroDistance) {
                nitroDistance = sDistance;
                nearest = nitro;
            }
        }
        return createAction(Seek.seek(robot, nearest.position));
    }

    private Action attack() {
        double time = (new Vector(game.ball.position.dx, robot.position.dy, game.ball.position.dz).getDistance(robot.position)
                - Constants.BALL_RADIUS - Constants.ROBOT_RADIUS) * Constants.TICKS_PER_SECOND / Constants.ROBOT_MAX_GROUND_SPEED;

        boolean cantCatch = game.ball.position.getDistance(robot.position) > 8
                && robot.position.copy().subtract(game.ball.position).normalize().dotProduct(game.ball.speed.copy().normalize()) < -0.7071 //3PI/4
                && game.ball.speed.getLength() > 25;
        boolean iAmClosest = true;
        Message forwardMessage = post.getMessage(MessageType.FORWARD_REPULSE_BALL, robotId);
        for (Robot robot : game.robots) {
            if (!robot.isTeammate || robot.id % game.myTeam.size() == 1 || robot == this.robot) {
                continue;
            }
            if ((robot.position.dy < 4 || forwardMessage != null)
                    && robot.getDistanceBetween(game.ball) < this.robot.getDistanceBetween(game.ball)) {
                iAmClosest = false;
                break;
            }
        }
        Message goalkeeperMessage = post.getMessage(MessageType.GOALKEEPER_REPULSE_BALL);
        if (time > 45 || !iAmClosest || goalkeeperMessage != null || cantCatch) {
            if (!robot.touch && robot.nitroAmount > 0 && robot.position.dy > 3) {
                Action action = new Action();
                action.target_velocity_y = - Constants.MAX_ENTITY_SPEED;
                action.use_nitro = true;
                return action;
            }

            List<Vector> ballPositions = ballPrediction.getNextPositions();
            int i = 0;
            if (goalkeeperMessage != null) {
                ballPositions = (List<Vector>) goalkeeperMessage.value;
                i = goalkeeperMessage.intValue + 50;
                if (!iAmClosest) {
                    i += 70;
                }
            } else if (!iAmClosest && forwardMessage != null) {
                ballPositions = (List<Vector>) forwardMessage.value;
                i = forwardMessage.intValue + 50;
            }

            for (; i < ballPositions.size(); i++) {
                Vector ballPosition = ballPositions.get(i);
                if (ballPosition.dy > 6.7 && Math.abs(ballPosition.dx) < arena.width / 2 + 5) {
                    continue; //если мяч высоко и далеко от стен на которые можно залезть, сюда не идем
                }
                Vector fromGoal = new Vector(0, 1, 40).subtract(ballPosition.dx, 1, ballPosition.dz).reverse();
                fromGoal.setLength(Constants.ROBOT_RADIUS + Constants.BALL_RADIUS * 2);
                Vector targetPos = new Vector(ballPosition.dx, 1, ballPosition.dz).add(fromGoal);

                double distance = targetPos.getDistance(robot.position) - Constants.ROBOT_RADIUS - Constants.BALL_RADIUS;
                if (distance * Constants.TICKS_PER_SECOND < (Constants.ROBOT_MAX_GROUND_SPEED  * 2 / 3) * i) {
                    if (Strategy.isTest) {
                        Render.addSphere(targetPos, 0.5, 0, 1, 0, 1);
                    }
                    return Arrival.arrive(robot, targetPos);
                }

            }
            if (game.ball.speed.dz > 0 && robot.position.dz < 25) {
                return createAction(Seek.seek(robot, new Vector(game.ball.position.dx, Constants.ROBOT_RADIUS, game.ball.position.dz)));
            }
            return new Action();
        }

            bestActions = Brooms.simulate(createGameForSimulation(), Math.max(5, (int) Math.ceil(time)), createAttackOption(), bestActions);
            Action action;
            if (bestActions == null || bestActions.isEmpty()) {
            action = new Action();
        } else {
            action = bestActions.remove(0);
        }
        return action;
        //}
    }

    private Brooms.BroomsOption createAttackOption() {
        return new Brooms.BroomsOption() {
            @Override
            public double calcBranchRate(Brooms.Branch branch) {
                return calcAttackRate(branch);
            }

            @Override
            public boolean isBranchFinished(Brooms.Branch branch) {
                return isAttackFinished(branch);
            }

            @Override
            public void checkDistance(Brooms.Branch branch) {
                checkHorizontalDistance(branch);
            }

            @Override
            public String addStr() {
                return game.currentTick + " att";
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
        post.putMessage(new Message(MessageType.FORWARD_REPULSE_BALL, game.currentTick, ballPositions, branch.len, robot));
    }

    private double calcAttackRate(Brooms.Branch branch) {
        double rate = 0;
        if (branch.game.ball.collision && branch.game.robots[0].collision) {
            rate = getCollisionRate(branch);
        } else {
            rate = -Math.sqrt(branch.minSquaredDistance); // чем меньше дистанция до мяча, тем ближе нас подведет ветка
        }
        rate -= branch.inTheAir;
        rate -= branch.nitro * 3;
        if (branch.game.ball.position.dz < -arena.depth / 2 - branch.game.ball.radius) {
            rate -= 1000; //гол в наши ворота, все плохо
        }
        return rate;
    }

    private double getCollisionRate(Brooms.Branch branch) {
        double rate = 0;
        if (branch.game.ball.speed.dz < 0 && branch.lastBallDz > branch.game.ball.speed.dz) {
            rate -= 100 - branch.game.ball.speed.dz * 10; //если в результате удара мяч летит ближе к воротам, это плохо!
        } else {
            boolean goal = false;
            int i = 0;
            Game game = new Game(branch.game);
            if (branch.game.robots[0].position.dz > 0) {
                game.robots[0].action = null;
                for (; i < 70; i++) {
                    GameEngine.tick(game, 4);
                    if (game.ball.position.dz > arena.depth / 2 + branch.game.ball.radius) {
                        goal = true;
                        break;
                    } else if (game.ball.speed.dz < 0) {
                        break;
                    }
                    if (Strategy.isTest) {
                        Render.addSphere(game.ball.position, 0.05, 1, 1, 1, 1);
                    }
                }
            }
            if (goal) {
                rate += 1500;
                double distance = game.ball.position.getDistance(0, 7, 40);
                rate -= distance;
                rate -= i * 10;
            } else {
                rate = branch.game.ball.speed.dz * 10;
            }
        }
        return rate;
    }

    private boolean isAttackFinished(Brooms.Branch branch) {
        return branch.game.ball.collision && branch.game.robots[0].collision//удар по мячу
                || Math.abs(branch.game.ball.position.dz) > arena.depth / 2 + branch.game.ball.radius; // гол
    }

    private static void checkHorizontalDistance(Brooms.Branch branch) {
        if (branch.game.ball.position.dz < branch.game.robots[0].position.dz) {
            return;
        }
        Vector target = branch.game.ball.position.copy();
        target.dy = branch.game.robots[0].position.dy;
        target.dz = target.dz - 2 *  Constants.BALL_RADIUS;
        target.dx += target.dx / 10;
        double distance = branch.game.robots[0].position.getSquaredDistance(branch.game.ball.position.dx,
                branch.game.robots[0].position.dy, branch.game.ball.position.dz - Constants.BALL_RADIUS - Constants.ROBOT_RADIUS);
        if (distance < branch.minSquaredDistance) {
            branch.minSquaredDistance = distance;
            branch.getMinSquaredDistanceY = branch.game.robots[0].position.dy;
        }
    }

    private Game createGameForSimulation() {
        List<Robot> robots = new ArrayList<>();
        robots.add(robot);
        for (Robot r : game.robots) {
            if (r == robot) {
                continue;
            }
            if (r.position.getSquaredDistance(game.ball.position) <= 100
                    || r.position.getSquaredDistance(robot.position) <= 100) {
                robots.add(r);
            }
        }
        return new Game(robots, game.ball);
    }

    public enum State {
        WAIT, RETURN, ATTACK, COLLECT
    }
}
