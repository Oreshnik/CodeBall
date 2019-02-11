package steering_behaviors;

import game_engine.Constants;
import model.Action;
import mymodel.Robot;
import mymodel.Vector;

public class Arrival {
    private static double slowingRadius = 10;

    public static Action arrive(Robot robot, Vector target) {
        Vector desiredVelocity = target.copy().subtract(robot.position);
        double squaredLength = desiredVelocity.getSquaredLength();
        if (squaredLength < slowingRadius * slowingRadius) {
            desiredVelocity.setLength(Constants.ROBOT_MAX_GROUND_SPEED - Constants.EPS).multiply(Math.sqrt(squaredLength) / slowingRadius);
        } else {
            desiredVelocity.setLength(Constants.ROBOT_MAX_GROUND_SPEED - Constants.EPS);
        }
        Action action = new Action();
        action.target_velocity_x = desiredVelocity.dx;
        action.target_velocity_z = desiredVelocity.dz;
        return action;
    }
}
