package mymodel;

import game_engine.Constants;

public class Ball  {
    public Vector position;
    public Vector speed;
    public double radius;
    public int mass;
    public double radiusChangeSpeed;
    public double arenaE;
    public boolean collision;

    public Ball(double x, double y, double z, double radius) {
        position = new Vector(x, y, z);
        this.radius = radius;
        mass = Constants.BALL_MASS;
        arenaE = Constants.BALL_ARENA_E;
    }

    public Ball(model.Ball ball) {
        this(ball.x, ball.y, ball.z, ball.velocity_x, ball.velocity_y, ball.velocity_z, ball.radius);
    }

    public Ball(double x, double y, double z, double vx, double vy, double vz, double radius) {
        this(x, y, z, radius);
        this.speed = new Vector(vx, vy, vz);
    }

    public double getDistanceBetween(Ball other) {
        return position.getDistance(other.position) - radius - other.radius;
    }

    public Ball copy() {
        Ball copy =  new Ball(position.dx, position.dy, position.dz, speed.dx, speed.dy, speed.dz, radius);
        copy.speed = new Vector(speed.dx, speed.dy, speed.dz);
        return copy;
    }
}
