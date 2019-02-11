package strategy;

import messaging.Post;
import model.Action;
import mymodel.Arena;
import mymodel.Game;
import mymodel.Robot;
import rendering.Render;

import java.util.HashMap;

public class Strategy {
    public static boolean isTest = "true".equals(System.getenv("TEST_ENV"));
    private BallPrediction ballPrediction;
    private Goalkeeper goalkeeper;
    private Forward forward;
    private Forward secondForward;
    private Arena arena;
    private State state;
    private Post post;

    public Strategy(Game game) {
        arena = new Arena();
        post = new Post();
        ballPrediction = new BallPrediction();
        goalkeeper = new Goalkeeper(game.myTeam.get(0).id, ballPrediction, post);
        forward = new Forward(game.myTeam.get(1).id, ballPrediction, post);
        if (game.myTeam.size() > 2) {
            secondForward = new Forward(game.myTeam.get(2).id, ballPrediction, post);
        }
        state = State.STARTED;
    }

    public void update(Game game) {
        changeState(game);
        post.update(game.currentTick);
        if (state == State.STARTED) {
            ballPrediction.update(game);
            goalkeeper.update(game);
            forward.update(game);
            if (secondForward != null) {
                secondForward.update(game);
            }
            Render.clear();
        } else {
            goalkeeper.state = Goalkeeper.State.WAIT;
        }
    }

    private void changeState(Game game) {
        if (state == State.STARTED) {
            if (Math.abs(game.ball.position.dz) > arena.depth / 2 + game.ball.radius) {
                state = State.GOAL;
            }

        } else if (state == State.GOAL) {
            if (game.ball.position.dx == 0 && game.ball.position.dz == 0 && game.ball.speed.dy == 0) {
                state = State.STARTED;
                goalkeeper.state = Goalkeeper.State.RETURN;
            }
        }
    }

    public String getCustomRendering() {
        ballPrediction.renderPositions();
        goalkeeper.render();
        String customRendering = Render.getCustomRendering();
        Render.clear();
        return customRendering;
    }

    public HashMap<Integer, Action> getActions(Game game) {
        HashMap<Integer, Action> actions = new HashMap<>();
        for (Robot robot : game.robots) {
            if (state == State.STARTED) {
                if (robot.isTeammate) {
                    if (robot.id == goalkeeper.robotId) {
                        actions.put(robot.id, goalkeeper.doAction());
                    } else if (robot.id == forward.robotId) {
                        actions.put(robot.id, forward.doAction());

                    } else if (secondForward != null && robot.id == secondForward.robotId) {
                        actions.put(robot.id, secondForward.doAction());

                    } else {
                        actions.put(robot.id, new Action());
                    }
                    robot.action = actions.get(robot.id);
                }
            } else {
                actions.put(robot.id, new Action());
            }
        }
        return actions;
    }

    public enum State {
        STARTED, GOAL
    }
}
