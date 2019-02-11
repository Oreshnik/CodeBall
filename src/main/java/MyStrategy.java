import game_engine.GameEngine;
import model.Action;
import model.Rules;
import mymodel.*;

import java.util.HashMap;

import static game_engine.Constants.EPS;
import static strategy.Strategy.isTest;

public final class MyStrategy implements Strategy {
    private static HashMap<Integer, Action> commands;
    private static Game predicted;
    private static String ballString;
    private static String robotString;
    private static int sims = 0;
    private static strategy.Strategy strategy;

    @Override
    public void act(model.Robot me, Rules rules, model.Game gameIn, Action action) {
        Game game = new mymodel.Game(gameIn);
        if (strategy == null) {
            strategy = new strategy.Strategy(game);
        }

        if (me.id % (game.robots.length / 2) == 1) {
            //simulate(gameIn);
            strategy.update(game);
            if (isTest) {
                printSimulation(game);
                ballString = gameBallString(game);
                robotString = gameRobotString(game.currentTick, me);
            }
            commands = doAllTheWork(game, me.id);
        }
        copyAction(action, commands.get(me.id));
    }

    private void simulate(model.Game gameIn) {
        Game game = new mymodel.Game(gameIn);
        game.robots[0].action = new Action();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 20) {
            GameEngine.tick(game);
            sims ++;
        }
        if (gameIn.current_tick == 999) {
            System.out.println(sims);
        }
    }

    private String gameRobotString(int currentTick, model.Robot me) {
        return "robot tick: " + currentTick + "\nposition: " + me.x + ", " + me.y + ", " + me.z
                + " speed: " + me.velocity_x + ", " + me.velocity_y + ", " + me.velocity_z;
    }

    private void printSimulation(Game game) {
        if (predicted == null) {
            return;
        }
        if (game.ball.position.dx == 0 && game.ball.position.dz == 0 && game.ball.speed.dy == 0) {
            return;
        }

        if (/*1 == 1 || */isDivergency(predicted.ball.position, game.ball.position)
                || isDivergency(predicted.ball.speed, game.ball.speed)) {
            //после гола можно не проверять
            if (Math.abs(game.ball.position.dz) <= new Arena().depth / 2 + game.ball.radius) {
                System.out.println(ballString);
                System.out.println("position");
                System.out.println(predicted.ball.position);
                System.out.println(game.ball.position);
                System.out.println("velocity");
                System.out.println(predicted.ball.speed);
                System.out.println(game.ball.speed);
                System.out.println();
            }
        }

        for (Robot robot : game.robots) {
            for (Robot robotP : predicted.robots) {
                if (robot.id == 1 && robot.id == robotP.id && (isDivergency(robotP.position, robot.position) || isDivergency(robotP.speed, robot.speed))) {
                    System.out.println(robotString);
                    System.out.println("position");
                    System.out.println(robotP.position);
                    System.out.println(robot.position);
                    System.out.println("velocity");
                    System.out.println(robotP.speed);
                    System.out.println(robot.speed);
                    System.out.println();
                }
            }
        }
    }

    private boolean isDivergency(Vector a, Vector b) {
        return  (Math.abs(a.dx - b.dx) > EPS || Math.abs(a.dy - b.dy) > EPS || Math.abs(a.dz - b.dz) > EPS);
    }

    private String gameBallString(Game game) {
        return "ball tick: " + game.currentTick + "\nposition: " + game.ball.position + " speed: " + game.ball.speed;
    }

    private HashMap<Integer, Action> doAllTheWork(Game game, int id) {
        HashMap<Integer, Action> actions = strategy.getActions(game);
        if (isTest) {
            GameEngine.tick(game);
            predicted = game;
        }
        return actions;
    }

    private void copyAction(Action action, Action actionFrom) {
        action.jump_speed = actionFrom.jump_speed;
        action.target_velocity_x = actionFrom.target_velocity_x;
        action.target_velocity_y = actionFrom.target_velocity_y;
        action.target_velocity_z = actionFrom.target_velocity_z;
        action.use_nitro = actionFrom.use_nitro;
    }

    @Override
    public String customRendering() {
        if (isTest) {
            return strategy.getCustomRendering();
        } else {
            return "";
        }
    }
}
