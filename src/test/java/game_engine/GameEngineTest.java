package game_engine;

import model.Action;
import mymodel.*;
import org.junit.Assert;
import org.junit.Test;

import static game_engine.Constants.BALL_RADIUS;
import static game_engine.Constants.EPS;
import static game_engine.Constants.ROBOT_RADIUS;
import static java.lang.Math.PI;

public class GameEngineTest {

    @Test
    public void testBallFalling() {
        Game game = createGameBall(0.0, 7.490254926094169, 0.0, 0.0, -1.9999999999999793, 0.0);
        GameEngine.tick(game);
        assertPositionVelocity(0.0, 7.452754926094185, 0.0, 0.0, -2.499999999999969, 0.0, game.ball);
    }

    @Test
    public void testBallBouncing() {
        Game game = createGameBall(0.0, 2.1569215927611887, 0.0, 0.0, -18.000000000000703, 0.0);
        GameEngine.tick(game);
        assertPositionVelocity(0.0, 2.1012960000000116, 0.0, 0.0, 12.542000000000417, 0.0, game.ball);
    }

    @Test
    public void testBallAfterRobotCollision() {
        Game game = createGameBall(-0.9989607187992452, 2.273214194044529, -1.4757853392010811,
                -4.038876112522701, 5.497726181454587, -5.966715248698531);
        GameEngine.tick(game);
        assertPositionVelocity(-1.0662753206746132, 2.3606762970687893, -1.575230593346062,
                -4.038876112522701, 4.997726181454597, -5.966715248698531, game.ball);
    }

    @Test
    public void testBallGoalOuterCorner() {
        Game game = createGameBall(14.670154465755424, 2.056114662575662, -37.11939787223021,
                20.703541544658364, -0.4786832095715779, -10.744351173350507);
        GameEngine.tick(game);
        assertPositionVelocity(15.0152134914998, 2.043969942416152, -37.2984703917857,
                20.703541544658364, -0.9786832095715783, -10.744351173350507, game.ball);
    }

    @Test
    public void testBallBottomCornersCorner() {
        Game game = createGameBall(16.69844502226156, 2.600148403475282, -37.91578014162374,
                20.156228094780065, 8.921589270389983, -2.7313470124610046);
        GameEngine.tick(game);
        assertPositionVelocity(17.034382157174626, 2.7446748913151304, -37.961302591831355,
                20.156228094780065, 8.421589270389905, -2.7313470124610046, game.ball);
    }

    @Test
    public void testDanToPlane() {
        Vector point = new Vector(10.153530395551542, 0.9999995833333334, 17.2309552987236);
        Vector planeNormal = new Vector(0, -1, 0);
        GameEngine.DistanceAndNormal dan = GameEngine.danToPlane(point, 0, 20, 0, planeNormal);
        Assert.assertEquals(19.000000416666666, dan.distance, EPS);
    }

    @Test
    public void testDanToSphereInner() {
        Vector point = new Vector(17.000097784451988, 4.6096278535857556, 35.58242025285458);
        double sphereRadius = 13.;
        GameEngine.DistanceAndNormal dan = GameEngine.danToSphereInner(point, 17.0, 4.6096278535857556, 27.0, sphereRadius);
        Assert.assertEquals(4.417579746588361, dan.distance, EPS);
        Assert.assertEquals(-1.139357536694582E-5, dan.normal.dx, EPS);
        Assert.assertEquals(0, dan.normal.dy, EPS);
        Assert.assertEquals(-0.9999999999350933, dan.normal.dz, EPS);
    }

    @Test
    public void testDanToSphereOuter() {
        Vector point = new Vector(10.153530395551542, 0.9999995833333334, 17.2309552987236);
        double sphereRadius = 1.;
        GameEngine.DistanceAndNormal dan = GameEngine.danToSphereOuter(point, 16.0, 0.9999995833333334, 41.0, sphereRadius);
        Assert.assertEquals(23.47751402506013, dan.distance, EPS);
        Assert.assertEquals(-0.23885062831392234, dan.normal.dx, EPS);
        Assert.assertEquals(0, dan.normal.dy, EPS);
        Assert.assertEquals(-0.9710563203821108, dan.normal.dz, EPS);
    }

    @Test
    public void testBallRobotCollision() {
        Game game = createGameBallAndRobot(-8.905093326497248, 2.891675754011973, -14.969689715352292,
                -10.843796308414708, 3.542263511380467, -27.888695203595482,
                -9.845679822392947, 1.0, -17.4086929100069, 0, 0, 0);
        GameEngine.tick(game);
        //верно только для коэффициента 0,45
        assertPositionVelocity(-9.052036784997782, 3.025502423393586, -15.34679024090944,
                -5.300170057565205, 15.997161368673302, -13.49721844788535, game.ball);
    }

    @Test
    public void testCore2Duo1() {
        Game game = createGameBall(-3.094469, 14.393752, -37.612641,
                1.741709, 5.221515, -24.657713);
        GameEngine.tick(game);
        assertPositionVelocity(-3.065441, 14.362125, -37.640170,
                1.741709, -7.465922, 16.160170, game.ball, 0.000001);
    }

    @Test
    public void testCore2Duo2() {
        Game game = createGameBall(-23.019338622046326748, 17.999962430128810809, -19.14657426491466552,
                14.682921642666871165, 0.058461879134667917024, -1.4818202142470027205);
        GameEngine.tick(game);
        assertPositionVelocity(-22.774622648706465355, 17.996454426285627193, -19.171271268485416073,
                14.682959226403681896, -0.46121522321528829469, -1.4818202142470027205, game.ball);
    }

    @Test
    public void testCore2Duo3() {
        Game game = createGameBall(16.329517921537998859, 16.195591242457055614, -36.845542433926816273,
                -25.283203469330487678, 7.6680203103518476127, 6.3722070924858815744);
        GameEngine.tick(game);
        assertPositionVelocity(15.908131197049071304, 16.319208932268558954, -36.739319891201859036,
                -25.283203469330487678, 7.1669386313658574039, 6.3734987090127770415, game.ball);
    }

    @Test
    public void testCore2Duo4() {
        Game game = createGameBall(-27.995519339371629286, 2.9054418436248079516, 6.1702947673222912073,
                -0.57403220611490801684, 6.0725454135943017775, -12.125100730674212457);
        GameEngine.tick(game);
        assertPositionVelocity(-27.999998915097581431, 3.0028025199069756646, 5.9682097551443806793,
                0.0021698048401537694749, 5.600275987957815893, -12.125100730674212457, game.ball, 0.0001);
    }

    @Test
    public void testRobotCorner() {
        Game game = createGameBallAndRobot(0, 2, 0, 0, 0, 0,
                -14.445789273928646, 1.0, -36.9104110343027,
                -23.42606428329091, 0.0, -18.740851426632727);
        Action action = game.robots[0].action;
        action.target_velocity_x = - 50;
        action.target_velocity_z = - 40;
        GameEngine.tick(game);
        System.out.println(game.robots[0].position);
        System.out.println(game.robots[0].speed);
        assertPositionVelocity(-14.836221153193538, 1.0005649258741522, -37.22275042842413,
                -23.422223210758496, 0.5894491662031354, -18.728412753231797, game.robots[0], 0.0000001);
    }

    @Test
    public void testRobotJump() {
        Game game = createGameBallAndRobot(0, 2, 0, 10, 0, 10,
                10, 1, 10, 0, 0, 0);
        Action action = game.robots[0].action;
        action.jump_speed = 15;

        for (int t = 0; t < 60; t++) {
            GameEngine.tick(game, 10);
            System.out.println(t + ": " + game.robots[0].position);
        }
    }

    @Test
    public void testRobotRotate() {
        Game game = createGameBallAndRobot(0, 2, 0, 0, 0, 0,
                10, 1, 10, 0, 0, 30);
        Vector rotate = game.robots[0].speed.copy().rotate(PI/ 6);
        Action action = game.robots[0].action;
        action.target_velocity_x = rotate.dx;
        action.target_velocity_z = rotate.dz;
        for (int t = 0; t < 20; t++) {
            GameEngine.tick(game, 100);
            System.out.println(t + ": " + game.robots[0].speed);
        }
    }

    @Test
    public void testGoal() {
        Game game = createGameBallAndRobot(11.236381026213765,4.1738817075324235,-41.978146497949055,
                24.392954016831922,10.481324945411519,-19.906613526565735,
                12.08381361515204,4.091555545305139,-45.09500000000351,
                18.10942854354799,-6.502481793015321,15.0);
        /*Goalkeeper goalkeeper = new Goalkeeper(1, new BallPrediction());
        goalkeeper.update(game);
        Brooms.simulate(game, 1, goalkeeper.createRepulseOption());*/
        game.robots[0].action = new Action();
        game.robots[0].action.jump_speed = 0;
        GameEngine.tick(game, 100);
        System.out.println(game.ball.position);
        System.out.println(game.ball.speed);
    }

    @Test
    public void testRobotNitroUp() {
        Game game = createGameBallAndRobot(20, 2, 20, 0, 0, 0,
                0, 1, 0, 0, 0, 0);
        Action action = game.robots[0].action;
        game.robots[0].nitroAmount = Constants.MAX_NITRO_AMOUNT;
        action.use_nitro = true;
        action.target_velocity_y = Constants.MAX_ENTITY_SPEED;

        for (int t = 0; t < 60; t++) {
            GameEngine.tick(game, 100);
            System.out.println(t + ": " + game.robots[0].position);
        }
    }

    @Test
    public void testRobotNitroJump() {
        Game game = createGameBallAndRobot(20, 2, 20, 0, 0, 0,
                0, 1, 0, 0, 0, 0);
        Action action = game.robots[0].action;
        game.robots[0].nitroAmount = Constants.MAX_NITRO_AMOUNT;
        action.use_nitro = true;
        action.jump_speed = 15;
        action.target_velocity_y = Constants.MAX_ENTITY_SPEED;

        for (int t = 0; t < 100; t++) {
            GameEngine.tick(game, 100);
            System.out.println(t + ": " + game.robots[0].position);
        }
    }

    @Test
    public void testRobotNitroHorizontal() {
        Game game = createGameBallAndRobot(20, 2, 20, 0, 0, 0,
                0, 1, 0, 0, 0, 0);
        Action action = game.robots[0].action;
        game.robots[0].nitroAmount = Constants.MAX_NITRO_AMOUNT;
        action.use_nitro = true;
        action.target_velocity_z = Constants.MAX_ENTITY_SPEED;

        for (int t = 0; t < 40; t++) {
            GameEngine.tick(game, 100);
            System.out.println(t + ": " + game.robots[0].speed);
        }
    }

/*    @Test
    public void testRobotMove() {
        Game game = createGameBallAndRobot(0, 2, 0, 0, 0, 0,
                9.968560354039342, 1.0, -17.37893133192481,
                -4.996185110955339, 0.0, -3.9969480887642828);
        Action action = game.robots[0].action;
        action.target_velocity_x = - 40;
        action.target_velocity_z = - 50;
        GameEngine.tick(game);
        System.out.println(game.robots[0].position.toString());
        System.out.println(game.robots[0].speed.toString());
        assertPositionVelocity(9.877349645638331, 1.0, -17.456772270777236,
                -5.949099897166098, 0.0, -5.343964573526406, game.robots[0], 0.0000001);
    }*/

    private Game createGameBallAndRobot(double dx, double dy, double dz, double vx, double vy, double vz,
                                        double rdx, double rdy, double rdz, double rvx, double rvy, double rvz) {
        Game game = createGameBall(dx, dy, dz, vx, vy, vz);
        model.Robot robot = new model.Robot();
        robot.x = rdx;
        robot.y = rdy;
        robot.z = rdz;
        robot.velocity_x = rvx;
        robot.velocity_y = rvy;
        robot.velocity_z = rvz;
        robot.radius = ROBOT_RADIUS;
        Robot r = new Robot(robot);
        r.action = new Action();
        game.robots = new Robot[] {r};
        return game;
    }

    private void assertPositionVelocity(double dx, double dy, double dz, double vx, double vy, double vz, Ball ball) {
        assertPositionVelocity(dx, dy, dz, vx, vy, vz, ball, EPS);
    }

    private void assertPositionVelocity(double dx, double dy, double dz, double vx, double vy, double vz, Ball ball, double eps) {
        Assert.assertEquals("dx", dx, ball.position.dx, eps);
        Assert.assertEquals("dy", dy, ball.position.dy, eps);
        Assert.assertEquals("dz", dz, ball.position.dz, eps);
        Assert.assertEquals("vx", vx, ball.speed.dx, eps);
        Assert.assertEquals("vy", vy, ball.speed.dy, eps);
        Assert.assertEquals("vz", vz, ball.speed.dz, eps);
    }


    private Game createGameBall(double dx, double dy, double dz, double vx, double vy, double vz) {
        model.Game gameIn = new model.Game();
        gameIn.robots = new model.Robot[0];
        gameIn.nitro_packs = new model.NitroPack[0];
        model.Ball ball = new model.Ball();
        ball.x = dx;
        ball.y = dy;
        ball.z = dz;
        ball.velocity_x = vx;
        ball.velocity_y = vy;
        ball.velocity_z = vz;
        ball.radius = BALL_RADIUS;
        gameIn.ball = ball;
        return new Game(gameIn);
    }

}