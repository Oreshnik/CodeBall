package mymodel;

import model.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Game {
    public int currentTick;
    public Player[] players;
    public Robot[] robots;
    public Nitro[] nitroPacks;
    public List<Robot> myTeam;
    public Ball ball;

    public Game() {
        players = new Player[0];
        robots = new Robot[0];
        nitroPacks = new Nitro[0];

    }

    public Game(Robot robot, Ball ball) {
        this.ball = ball;
        robots = new Robot[] {robot};
        nitroPacks = new Nitro[0];
    }

    public Game(List<Robot> robots, Ball ball) {
        this.ball = ball;
        this.robots = robots.toArray(new Robot[0]);
        nitroPacks = new Nitro[0];
    }

    public Game(Game game) {
        this.ball = game.ball.copy();
        this.robots = new Robot[game.robots.length];
        for (int i = 0; i < game.robots.length; i++) {
            this.robots[i] = game.robots[i].copy();
        }
        nitroPacks = new Nitro[0];
    }

    public Game(model.Game game) {
        currentTick = game.current_tick;
        players = game.players;
        robots = new Robot[game.robots.length];
        myTeam = new ArrayList<>();
        for (int i = 0; i < game.robots.length; i++) {
            Robot robot = new Robot(game.robots[i]);
            robots[game.robots[i].id - 1] = robot;
            if (robot.isTeammate) {
                myTeam.add(robot);
            }
        }
        myTeam.sort(Comparator.comparingInt(r -> r.id));
        nitroPacks = new Nitro[game.nitro_packs.length];
        for (int i = 0; i < game.nitro_packs.length; i++) {
            nitroPacks[i] = new Nitro(game.nitro_packs[i]);
        }
        ball = new Ball(game.ball);
    }
}
