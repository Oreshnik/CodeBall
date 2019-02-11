package strategy;

import game_engine.Constants;
import game_engine.GameEngine;
import mymodel.Ball;
import mymodel.Game;
import mymodel.Vector;
import rendering.Render;

import java.util.ArrayList;
import java.util.List;

public class BallPrediction {
    private final static int ticks = 300;
    private static final double eps = 0.1;
    private List<Vector> nextPositions;
    private Ball ball;

    public BallPrediction() {
        nextPositions = new ArrayList<>();
        ball = new Ball(0, 0, 0, Constants.BALL_RADIUS);
    }

    public void update(Game game) {
        if (!nextPositions.isEmpty()) {
            Vector vector = game.ball.position.copy().subtract(nextPositions.get(0));
            if (vector.getSquaredLength() > eps * eps) {
                nextPositions.clear();
            }
        }

        if (!nextPositions.isEmpty()) {
            nextPositions.remove(0);
        }
        this.ball.speed = game.ball.speed.copy();
        this.ball.position = game.ball.position.copy();
    }

    public List<Vector> getNextPositions() {
        if (ball == null || ball.speed == null) {
            return nextPositions;
        }
        if (nextPositions.size() < ticks / 2) {
            nextPositions.clear();
            Game game = new Game();
            game.ball = ball;
            for (int i = 0; i < ticks; i++) {
                GameEngine.tick(game, 10);
                nextPositions.add(game.ball.position.copy());
            }
        }
        return nextPositions;
    }

    public void renderPositions() {
        getNextPositions();
        for (Vector position : nextPositions) {
            Render.addSphere(position, 0.5, 0, 0, 1, 0.1);
        }
    }

}
