package mymodel;

import game_engine.Constants;
import model.Action;

public class Robot extends Ball {
    public int id;
    public int playerId;
    public boolean isTeammate;
    public double nitroAmount;
    public boolean touch;
    public Vector normal;
    public Action action;

    public Robot(model.Robot robot) {
        super(robot.x, robot.y, robot.z, robot.velocity_x, robot.velocity_y, robot.velocity_z, robot.radius);
        touch = robot.touch;
        if (touch) {
            normal = new Vector(robot.touch_normal_x, robot.touch_normal_y, robot.touch_normal_z);
        }
        id = robot.id;
        playerId = robot.player_id;
        isTeammate = robot.is_teammate;
        nitroAmount = robot.nitro_amount;
        mass = Constants.ROBOT_MASS;
        arenaE = Constants.ROBOT_ARENA_E;
    }

    public Robot(double x, double y, double z, double radius) {
        super(x, y, z, radius);
    }

    public Robot copy() {
        Robot copy = new Robot(position.dx, position.dy, position.dz, radius);
        copy.touch = touch;
        if (touch) {
            copy.normal = new Vector(normal.dx, normal.dy, normal.dz);
        }
        copy.id = id;
        copy.mass = Constants.ROBOT_MASS;
        copy.arenaE = Constants.ROBOT_ARENA_E;
        copy.nitroAmount = nitroAmount;
        copy.speed = new Vector(speed.dx, speed.dy, speed.dz);
        copy.isTeammate = isTeammate;
        copy.nitroAmount = nitroAmount;
        return copy;
    }
}
