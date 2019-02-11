package steering_behaviors;

import game_engine.Constants;
import model.Action;
import mymodel.Robot;
import mymodel.Vector;

public class Seek {
    public static Vector seek(Robot robot, Vector target) {
        Vector groundTarget = target.copy();
        groundTarget.dy = 1.;
        Vector desiredVelocity = groundTarget.subtract(robot.position);
        desiredVelocity.setLength(Constants.ROBOT_MAX_GROUND_SPEED - Constants.EPS);
        return desiredVelocity;
    }
}
