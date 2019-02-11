package game_engine;

import mymodel.*;

import java.util.Random;

import static game_engine.Constants.*;

public class GameEngine {
    public static final Vector NORMAL_GROUND = new Vector(0, 1, 0);
    public static final Vector NORMAL_CEILING = new Vector(0, -1, 0);
    public static final Vector NORMAL_SIDE_X = new Vector(-1, 0, 0);
    public static final Vector NORMAL_SIDE_Z_GOAL = new Vector(0, 0, -1);
    public static final Vector NORMAL_SIDE_Z = new Vector(0, 0, -1);
    public static final Vector NORMAL_SIDE_X_CEILING = new Vector(-1, 0, 0);
    public static final Vector NORMAL_SIDE_X_CEILING_Y = new Vector(0, -1, 0);
    public static Arena arena = new Arena();

    public static DistanceAndNormal best = new DistanceAndNormal(0, null);
    public static DistanceAndNormal current = new DistanceAndNormal(0, null);


    public static void collision(Ball a, Ball b) {
        double dx = b.position.dx - a.position.dx;
        double dy = b.position.dy - a.position.dy;
        double dz = b.position.dz - a.position.dz;
        double squaredDistance = dx * dx + dy * dy + dz * dz;

        if (squaredDistance < (a.radius + b.radius) * (a.radius + b.radius)) {
            double distance = Math.sqrt(squaredDistance);
            double penetration = a.radius + b.radius - distance;

            double kA = (1.0 / a.mass) / ((1.0 / a.mass) + (1.0 / b.mass));
            double kB = (1.0 / b.mass) / ((1.0 / a.mass) + (1.0 / b.mass));

            dx /= distance;
            dy /= distance;
            dz /= distance;
            Vector deltaPosition = new Vector(dx, dy, dz);

            a.position.subtract(dx * (penetration * kA), dy * (penetration * kA), dz * (penetration * kA));
            b.position.add(dx * (penetration * kB), dy * (penetration * kB), dz * (penetration * kB));

            double deltaVelocity = deltaPosition.dx * (b.speed.dx - a.speed.dx)
                    + deltaPosition.dy * (b.speed.dy - a.speed.dy)
                    + deltaPosition.dz * (b.speed.dz - a.speed.dz) - b.radiusChangeSpeed - a.radiusChangeSpeed;
            if (deltaVelocity < 0) {
                Vector impulse = deltaPosition.multiply((1.45) * deltaVelocity);
                a.speed.add(impulse.dx * kA, impulse.dy * kA, impulse.dz * kA);
                b.speed.subtract(impulse.multiply(kB));
                if (!(b instanceof Robot)) {
                    a.collision = true;
                    b.collision = true;
                }
            }
        }
    }

    public static Vector collideWithArena(Ball e) {
        DistanceAndNormal dan = danToArena(e.position);
        double penetration = e.radius - dan.distance;
        if (penetration > 0) {
            e.position.add(dan.normal.dx * penetration, dan.normal.dy * penetration, dan.normal.dz * penetration);
            double velocity = e.speed.dotProduct(dan.normal) - e.radiusChangeSpeed;
            if (velocity < 0) {
                double multi = velocity * (1 + e.arenaE);
                e.speed.subtract(dan.normal.dx * multi, dan.normal.dy * multi, dan.normal.dz * multi);
                return dan.normal;
            }
        }
        return null;

    }

    public static void move(Ball ball, double deltaTime) {
        ball.speed = clamp(ball.speed, MAX_ENTITY_SPEED);
        ball.position.add(ball.speed.dx * deltaTime, ball.speed.dy * deltaTime, ball.speed.dz * deltaTime);
        ball.position.dy -= GRAVITY * deltaTime * deltaTime / 2;
        ball.speed.dy -= GRAVITY * deltaTime;
    }

    public static void update(double deltaTime, Game game) {
        // shuffle(robots) надо ли? todo
        for (int i = 0; i < game.robots.length; i++) {
            Robot robot = game.robots[i];
            changeSpeed(deltaTime, robot);
            move(robot, deltaTime);
            jump(robot);
        }
        move(game.ball, deltaTime);
        for (int i = 0; i < game.robots.length - 1; i++) {
            for (int j = i + 1; j < game.robots.length; j++) {
                collision(game.robots[i], game.robots[j]);
            }
        }
        for (int i = 0; i < game.robots.length; i++) {
            Robot robot = game.robots[i];
            collision(robot, game.ball);
            Vector collisionNormal = collideWithArena(robot);
            if (collisionNormal == null) {
                robot.touch = false;
            } else {
                robot.touch = true;
                robot.normal = collisionNormal;
            }
        }
        collideWithArena(game.ball);
        if (Math.abs(game.ball.position.dz) > arena.depth / 2 + game.ball.radius) {
            //goal_scored() //todo
            //game.ball.speed.dz = 0;
        }

        for (int i = 0; i < game.robots.length; i++) {
            Robot robot = game.robots[i];
            if (robot.nitroAmount == MAX_NITRO_AMOUNT) {
                continue;
            }
            for (int j = 0; j < game.nitroPacks.length; j++) {
                Nitro nitro = game.nitroPacks[j];
                //todo убрать копию
                if (nitro.respawnTicks == null && robot.position.copy().subtract(nitro.position).getSquaredLength()
                        < (robot.radius + nitro.radius) * (robot.radius + nitro.radius)) {
                    robot.nitroAmount = MAX_NITRO_AMOUNT;
                    nitro.respawnTicks = NITRO_PACK_RESPAWN_TICKS;
                }
            }
        }
    }

    public static void tick(Game game) {
        tick(game, Constants.MICROTICKS_PER_TICK);
    }

    public static void tick(Game game, int microticks) {
        double delta_time = 1.0 / TICKS_PER_SECOND;
        for (int i = 0; i < microticks; i ++) {
            update(delta_time / microticks, game);
        }

        for (int j = 0; j < game.nitroPacks.length; j++) {
            Nitro nitro = game.nitroPacks[j];
            if (nitro.respawnTicks != null) {
                nitro.respawnTicks --;
            }
        }
    }

    private static void jump(Robot robot) {
        if (robot.action == null) {
            return;
        }
        robot.radius = ROBOT_MIN_RADIUS + (ROBOT_MAX_RADIUS - ROBOT_MIN_RADIUS)
                * robot.action.jump_speed / ROBOT_MAX_JUMP_SPEED;
        robot.radiusChangeSpeed = robot.action.jump_speed;
    }

    private static void changeSpeed(double deltaTime, Robot robot) {
        if (robot.touch && robot.action != null) {
            Vector targetVelocity = clamp(new Vector(robot.action.target_velocity_x, robot.action.target_velocity_y,
                            robot.action.target_velocity_z), ROBOT_MAX_GROUND_SPEED);

            double normalDotTargetVelocity = robot.normal.dx * targetVelocity.dx
                    + robot.normal.dy * targetVelocity.dy + robot.normal.dz * targetVelocity.dz;
            targetVelocity.subtract(robot.normal.dx * normalDotTargetVelocity, robot.normal.dy * normalDotTargetVelocity,
                    robot.normal.dz * normalDotTargetVelocity);

            Vector targetVelocityChange = targetVelocity.subtract(robot.speed);
            double targetVelocityChangeSquaredLength = targetVelocityChange.getSquaredLength();

            if (targetVelocityChangeSquaredLength > 0) {
                double acceleration = ROBOT_ACCELERATION * Math.max(0, robot.normal.dy);
                double targetVelocityChangeLength = Math.sqrt(targetVelocityChangeSquaredLength);
                targetVelocityChange.divide(targetVelocityChangeLength); //normalize
                robot.speed.add(clamp((targetVelocityChange.multiply(acceleration * deltaTime)),
                        targetVelocityChangeLength));
            }
        }

        if (robot.action != null && robot.action.use_nitro) {
            Vector targetVelocityChange = clamp(new Vector(robot.action.target_velocity_x, robot.action.target_velocity_y,
                            robot.action.target_velocity_z).subtract(robot.speed),
                    robot.nitroAmount * NITRO_POINT_VELOCITY_CHANGE);
            if (targetVelocityChange.getSquaredLength() > 0) {
                Vector acceleration = targetVelocityChange.copy().normalize().multiply(ROBOT_NITRO_ACCELERATION);
                Vector velocityChange = clamp(acceleration.multiply(deltaTime), targetVelocityChange.getLength());
                robot.speed.add(velocityChange);
                robot.nitroAmount -= velocityChange.getLength() / NITRO_POINT_VELOCITY_CHANGE;
            }
        }
    }

    private static Vector clamp(Vector vector, double maxLength) {
        if (vector.getSquaredLength() > maxLength * maxLength) {
            return vector.setLength(maxLength);
        }
        return vector;
    }

    private static DistanceAndNormal danToArena(Vector point) {
        boolean negateX = point.dx < 0;
        boolean negateZ = point.dz < 0;
        if (negateX) {
            point.dx = - point.dx;
        }
        if (negateZ) {
            point.dz = -point.dz;
        }
        DistanceAndNormal result = danToArenaQuarter(point);
        if (negateX) {
            result.normal.dx = -result.normal.dx;
            point.dx = - point.dx;
        }
        if (negateZ) {
            result.normal.dz = -result.normal.dz;
            point.dz = - point.dz;
        }
        return result;

    }

    private static DistanceAndNormal danToArenaQuarter(Vector point) {
        // Ground
        DistanceAndNormal dan = danToPlane(point,0, 0, 0, NORMAL_GROUND);
        best.distance = dan.distance;
        best.normal = dan.normal;
        dan = best;
        // Ceiling
        dan = minDistanceDan(dan, danToPlane(point,0, arena.height, 0, NORMAL_CEILING));

        // Side x
        dan = minDistanceDan(dan, danToPlane(point, arena.width / 2, 0, 0, NORMAL_SIDE_X));
        // Side z (goal)
        dan = minDistanceDan(dan, danToPlane(point,0, 0, (arena.depth / 2) + arena.goalDepth,
                NORMAL_SIDE_Z_GOAL));
        // Side z
        double vx = point.dx - (arena.goalWidth / 2) - arena.goalTopRadius;
        double vy = point.dy - (arena.goalHeight - arena.goalTopRadius);
        if (point.dx >= (arena.goalWidth / 2) + arena.goalSideRadius
                || point.dy >= arena.goalHeight + arena.goalSideRadius
                || (vx > 0 && vy > 0 && (vx * vx + vy * vy) > (arena.goalTopRadius + arena.goalSideRadius) * (arena.goalTopRadius + arena.goalSideRadius))) {
            dan = minDistanceDan(dan, danToPlane(point, 0, 0, arena.depth / 2,
                    NORMAL_SIDE_Z));
        }

        // Side x & ceiling (goal)
        if (point.dz >= (arena.depth / 2) + arena.goalSideRadius) {
            // x
            dan = minDistanceDan(dan, danToPlane(point, arena.goalWidth / 2, 0, 0,
                    NORMAL_SIDE_X_CEILING));
            // y
            dan = minDistanceDan(dan, danToPlane(point, 0, arena.goalHeight, 0, NORMAL_SIDE_X_CEILING_Y));
        }

        // Goal back corners
        if (point.dz > (arena.depth / 2) + arena.goalDepth - arena.bottomRadius) {
            dan = minDistanceDan(dan, danToSphereInner(point,
                clamp(point.dx, arena.bottomRadius - (arena.goalWidth / 2),
                                    (arena.goalWidth / 2) - arena.bottomRadius),
                    clamp(point.dy, arena.bottomRadius,
                            arena.goalHeight - arena.goalTopRadius),
                    (arena.depth / 2) + arena.goalDepth - arena.bottomRadius,
                arena.bottomRadius));
        }

        // Corner
        if (point.dx > (arena.width / 2) - arena.cornerRadius && point.dz > (arena.depth / 2) - arena.cornerRadius) {
            dan = minDistanceDan(dan, danToSphereInner(point,
                    (arena.width / 2) - arena.cornerRadius, point.dy,
                        (arena.depth / 2) - arena.cornerRadius,
                    arena.cornerRadius));
        }

        // Goal outer corner
        if (point.dz < (arena.depth / 2) + arena.goalSideRadius) {
            dan = checkGoalOuterCorner(point, dan);
        }

        // Goal inside top corners
        if (point.dz > (arena.depth / 2) + arena.goalSideRadius
            && point.dy > arena.goalHeight - arena.goalTopRadius) {
            // Side x
            if (point.dx > (arena.goalWidth / 2) - arena.goalTopRadius) {
                dan = minDistanceDan(dan, danToSphereInner(point,
                        (arena.goalWidth / 2) - arena.goalTopRadius,
                            arena.goalHeight - arena.goalTopRadius, point.dz, arena.goalTopRadius));
            }
            // Side z
            if (point.dz > (arena.depth / 2) + arena.goalDepth - arena.goalTopRadius) {
                dan = minDistanceDan(dan, danToSphereInner(point, point.dx,
                        arena.goalHeight - arena.goalTopRadius,
                        (arena.depth / 2) + arena.goalDepth - arena.goalTopRadius, arena.goalTopRadius));
            }
        }

        // Bottom corners
        if (point.dy < arena.bottomRadius) {
            dan = checkBottomCorners(point, dan);
        }

        // Ceiling corners
        if (point.dy > arena.height - arena.topRadius) {
            dan = checkCeilingCorners(point, dan);
        }

        dan.normal = dan.normal.copy();
        return dan;
    }

    private static DistanceAndNormal checkCeilingCorners(Vector point, DistanceAndNormal dan) {
        // Side x
        if (point.dx > (arena.width / 2) - arena.topRadius) {
            dan = minDistanceDan(dan, danToSphereInner(point, (arena.width / 2) - arena.topRadius,
                    arena.height - arena.topRadius, point.dz, arena.topRadius));
        }
        // Side z
        if (point.dz > (arena.depth / 2) - arena.topRadius) {
            dan = minDistanceDan(dan, danToSphereInner(point, point.dx,
                    arena.height - arena.topRadius, (arena.depth / 2) - arena.topRadius, arena.topRadius));
        }

        // Corner
        if (point.dx > (arena.width / 2) - arena.cornerRadius && point.dz > (arena.depth / 2) - arena.cornerRadius) {
            double ox = (arena.width / 2) - arena.cornerRadius;
            double oy = (arena.depth / 2) - arena.cornerRadius;

            double dvx = point.dx - ox;
            double dvy = point.dz - oy;
            double dvSquaredLength = dvx * dvx + dvy * dvy;

            if (dvSquaredLength > (arena.cornerRadius - arena.topRadius) * (arena.cornerRadius - arena.topRadius)) {
                double dvLength = Math.sqrt(dvSquaredLength);
                dvx = dvx / dvLength * (arena.cornerRadius - arena.topRadius);
                dvy = dvy / dvLength * (arena.cornerRadius - arena.topRadius);

                ox += dvx;
                oy += dvy;
                dan = minDistanceDan(dan, danToSphereInner(point,
                        ox, arena.height - arena.topRadius, oy,
                        arena.topRadius));
            }
        }
        return dan;
    }

    private static DistanceAndNormal checkBottomCorners(Vector point, DistanceAndNormal dan) {
        // Side x
        if (point.dx > (arena.width / 2) - arena.bottomRadius) {
            dan = minDistanceDan(dan, danToSphereInner(point, (arena.width / 2) - arena.bottomRadius,
                    arena.bottomRadius, point.dz, arena.bottomRadius));
        }
        // Side z
        if (point.dz > (arena.depth / 2) - arena.bottomRadius && point.dx >= (arena.goalWidth / 2) + arena.goalSideRadius) {
            dan = minDistanceDan(dan, danToSphereInner(point, point.dx, arena.bottomRadius,
                    (arena.depth / 2) - arena.bottomRadius, arena.bottomRadius));
        }
        // Side z (goal)
        if (point.dz > (arena.depth / 2) + arena.goalDepth - arena.bottomRadius) {
            dan = minDistanceDan(dan, danToSphereInner(point, point.dx, arena.bottomRadius,
                    (arena.depth / 2) + arena.goalDepth - arena.bottomRadius, arena.bottomRadius));
        }
        // Goal outer corner
        double ox = (arena.goalWidth / 2) + arena.goalSideRadius;
        double oy = (arena.depth / 2) + arena.goalSideRadius;

        double dvx = point.dx - ox;
        double dvy = point.dz - oy;

        if (dvx < 0 && dvy < 0) {
            double dvSquaredLength = dvx * dvx + dvy * dvy;
            if (dvSquaredLength < (arena.goalSideRadius + arena.bottomRadius) * (arena.goalSideRadius + arena.bottomRadius)) {
                double dvLength = Math.sqrt(dvSquaredLength);
                dvx = dvx / dvLength * (arena.goalSideRadius + arena.bottomRadius);
                dvy = dvy / dvLength * (arena.goalSideRadius + arena.bottomRadius);
                ox += dvx;
                oy += dvy;
                dan = minDistanceDan(dan, danToSphereInner(point, ox, arena.bottomRadius, oy,
                        arena.bottomRadius));
            }
        }
        // Side x (goal)
        if (point.dz >= (arena.depth / 2) + arena.goalSideRadius && point.dx > (arena.goalWidth / 2) - arena.bottomRadius) {
            dan = minDistanceDan(dan, danToSphereInner(point, (arena.goalWidth / 2) - arena.bottomRadius,
                    arena.bottomRadius, point.dz, arena.bottomRadius));
        }
        // Corner
        if (point.dx > (arena.width / 2) - arena.cornerRadius && point.dz > (arena.depth / 2) - arena.cornerRadius) {
            double ux = (arena.width / 2) - arena.cornerRadius;
            double uy = (arena.depth / 2) - arena.cornerRadius;

            double vdx = point.dx - ux;
            double vdy = point.dz - uy;
            double vdSquaredLength = vdx * vdx + vdy * vdy;
            if (vdSquaredLength > (arena.cornerRadius - arena.bottomRadius) * (arena.cornerRadius - arena.bottomRadius)) {
                double vdLength = Math.sqrt(vdSquaredLength);
                vdx = vdx / vdLength * (arena.cornerRadius - arena.bottomRadius);
                vdy = vdy / vdLength * (arena.cornerRadius - arena.bottomRadius);

                ux += vdx;
                uy += vdy;
                dan = minDistanceDan(dan, danToSphereInner(point, ux, arena.bottomRadius, uy,
                        arena.bottomRadius));
            }
        }
        return dan;
    }

    private static DistanceAndNormal checkGoalOuterCorner(Vector point, DistanceAndNormal dan) {
        // Side x
        if (point.dx < (arena.goalWidth / 2) + arena.goalSideRadius) {
            dan = minDistanceDan(dan, danToSphereOuter(point,
                    (arena.goalWidth / 2) + arena.goalSideRadius, point.dy,
                        (arena.depth / 2) + arena.goalSideRadius, arena.goalSideRadius));
        }
        // Ceiling
        if (point.dy < arena.goalHeight + arena.goalSideRadius) {
            dan = minDistanceDan(dan, danToSphereOuter(point, point.dx,
                    arena.goalHeight + arena.goalSideRadius,
                    (arena.depth / 2) + arena.goalSideRadius, arena.goalSideRadius));
        }
        // Top corner
        double ox = (arena.goalWidth / 2) - arena.goalTopRadius;
        double oy = arena.goalHeight - arena.goalTopRadius;

        double dx = point.dx - ox;
        double dy = point.dy - oy;

        if (dx > 0 && dy > 0) {
            double dvLength = Math.sqrt(dx * dx + dy * dy);
            dx = dx / dvLength * (arena.goalTopRadius + arena.goalSideRadius);
            dy = dy / dvLength * (arena.goalTopRadius + arena.goalSideRadius);
            ox += dx;
            oy += dy;
            dan = minDistanceDan(dan, danToSphereOuter(point,
                    ox, oy, (arena.depth / 2) + arena.goalSideRadius,
                    arena.goalSideRadius));
        }
        return dan;
    }

    private static DistanceAndNormal minDistanceDan(DistanceAndNormal a, DistanceAndNormal b) {
        if (a.distance < b.distance) {
            best.distance = a.distance;
            best.normal = a.normal;
        } else {
            best.distance = b.distance;
            best.normal = b.normal;
        }
        return best;
    }

    protected static DistanceAndNormal danToPlane(Vector point, double dx, double dy, double dz, Vector planeNormal) {
        double distance = (point.dx - dx) * planeNormal.dx
                + (point.dy - dy) * planeNormal.dy
                + (point.dz - dz) * planeNormal.dz;
        current.distance = distance;
        current.normal = planeNormal;
        return current;
    }

    protected static DistanceAndNormal danToSphereInner(Vector point, double x, double y, double z, double sphereRadius) {
        double dx = point.dx - x;
        double dy = point.dy - y;
        double dz = point.dz - z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double distance = sphereRadius - length;

        dx /= length;
        dy /= length;
        dz /= length;

        current.distance = distance;
        current.normal = new Vector(-dx, -dy, -dz);
        return current;
    }


    protected static DistanceAndNormal danToSphereOuter(Vector point, double x, double y, double z, double sphereRadius) {
        double dx = point.dx - x;
        double dy = point.dy - y;
        double dz = point.dz - z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double distance = length - sphereRadius;

        dx /= length;
        dy /= length;
        dz /= length;
        current.distance = distance;
        current.normal = new Vector(dx, dy, dz);
        return current;
    }


    private static double clamp(double value, double min, double max) {
        return Math.min(Math.max(min, value), max);
    }

    protected static class DistanceAndNormal {
        double distance;
        Vector normal;

        DistanceAndNormal(double distance, Vector vector) {
            this.distance = distance;
            this.normal = vector;
        }

        @Override
        public String toString() {
            return "distance=" + distance +
                    ", normal=" + normal;
        }
    }

}
