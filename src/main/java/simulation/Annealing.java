package simulation;

import game_engine.Constants;
import game_engine.GameEngine;
import model.Action;
import mymodel.Game;
import mymodel.Vector;
import strategy.Goalkeeper;

import java.util.ArrayList;
import java.util.List;

import static game_engine.GameEngine.arena;
import static java.lang.Math.PI;

/*WARNING this class wasn't used in late versions*/
public class Annealing {
    public static List<Action> actions;
    private static final int LEN = 25;
    private static XORShiftRandom random;

    static {
        actions = new ArrayList<>();
        actions.add(new Action());
        Vector vector = new Vector(0., 0., 1.);
        vector.setLength(Constants.ROBOT_MAX_GROUND_SPEED - 0.01);

        double r = PI / 10;
        for (double a = r; a < 2 * PI; a += r) {
            Vector v = vector.copy().rotate(a);
            actions.add(createAction(v));
        }
        for (int i = 15; i > 0; i -= 5) {
            Action action = new Action();
            action.jump_speed = i;
            actions.add(action);
        }
        random = new XORShiftRandom(100500);
    }

    public static int[] findAction(Game game, int[] prev) {
        if (prev == null) {
            prev = generateRandomChain();
        } else {
            if (game.currentTick % 2 == 0) {
                System.arraycopy(prev, 1, prev, 0, LEN - 1);
                prev[LEN - 1] = random.nextInt(actions.size());
            }
        }

        int[] best = prev;
        double bestRate = calculateRate(best, game);

        long start = System.nanoTime();
        int timeLimit = 25;
        int n = 0, m = 0;
        while ((System.nanoTime() - start)  * 0.000001 < timeLimit && n < 1000) {
            int r = (int) Math.ceil((System.nanoTime() - start) * 0.000001 / 25 * 100);
            int[] current;
            if (r > 0 && n % r == 0) {
                current = generateRandomChain();
            } else {
                current = new int[LEN];
                System.arraycopy(best, 0, current, 0, LEN);
                mutateA(current);
            }

            double rate = calculateRate(current, game);
            if (rate > bestRate) {
                bestRate = rate;
                best = current;
                m ++;
            }
            n ++;
        }
        System.out.println("sims " + n + " changes " + m + " rate " + bestRate);
        return best;
    }

    private static double calculateRate(int[] best, Game g) {
        Game game = new Game(g);
        int len = 0;
        double minSquaredDistance = checkHorizontalDistance(game);
        double minSquaredDistanceY = game.robots[0].position.dy;
        double lastBallDz = 0;

        exitlabel: for (int i = 0; i < LEN; i ++) {
            Action action = actions.get(best[i]);
            for (int j = 0; j < 2; j++) {
                len ++;
                game.robots[0].action = action;
                lastBallDz = game.ball.speed.dz;
                GameEngine.tick(game, 10);
                double squaredDistance = checkHorizontalDistance(game);
                if (squaredDistance < minSquaredDistance) {
                    minSquaredDistance = squaredDistance;
                    minSquaredDistanceY = game.robots[0].position.dy;
                }
                if (i == 0 && game.currentTick % 2 == 1) {
                    break;
                }
                if (game.ball.collision && game.robots[0].collision //удар по мячу
                        || Math.abs(game.ball.position.dz) > arena.depth / 2 + game.ball.radius // гол
                        || game.ball.speed.dz > 10 // мяч уже летит от ворот
                        || (game.robots[0].position.getSquaredDistance(Goalkeeper.THREAT_CENTER) > Goalkeeper.THREAT_RADIUS * Goalkeeper.THREAT_RADIUS // выход за пределы защитного круга
                        && game.ball.position.getSquaredDistance(Goalkeeper.THREAT_CENTER) > Goalkeeper.THREAT_RADIUS * Goalkeeper.THREAT_RADIUS)) {
                    break exitlabel;
                }
            }
        }

        double rate = 0;
        if (game.ball.collision && game.robots[0].collision) {
            rate = game.ball.speed.dz * 10; //хочу ударить посильнее в сторону ворот
            if (game.ball.speed.dz < 0 && lastBallDz > game.ball.speed.dz) {
                rate -= 500; //если в результате удара мяч летит ближе к воротам, это плохо!
            }
            rate -= game.robots[0].speed.dy * 3; //если робот отлетает вверх, это плохо

        } else {
            rate = -Math.sqrt(minSquaredDistance); // чем меньше дистанция до мяча, тем ближе нас подведет ветка
            if (game.robots[0].position.dz > - 40) {
                rate += (1 - minSquaredDistanceY) * 2; // нет смысла зависать в воздухе, чтобы просто приблизиться к мячу, а в воротах пусть скочет
            }

        }
        /*if (branch.action.jump_speed > 0) {
            rate -= branch.action.jump_speed;
        }*/
        if (Math.abs(game.ball.position.dz) > arena.depth / 2 + game.ball.radius - 0.3) {
            rate -= 1000; //гол, все плохо, 0.1 на погрешность симуляции
        }
        //удаление от ворот вбок
        if (Math.abs(game.robots[0].position.dx) > arena.goalWidth / 2 - 2) {
            rate -= (Math.abs(game.robots[0].position.dx) - (arena.goalWidth / 2 - 2)) * 4;
        }

        if (game.ball.position.dz < game.robots[0].position.dz) {
            rate += game.ball.position.dz - game.robots[0].position.dz; //если оказались за мячом, то стараемся обойти мяч
        }
        rate -= len;
        return rate;
    }

    public static double checkHorizontalDistance(Game game) {
        if (game.ball.position.dz < game.robots[0].position.dz
                || game.ball.speed.dz > 10
                || (game.robots[0].position.getSquaredDistance(Goalkeeper.THREAT_CENTER) > Goalkeeper.THREAT_RADIUS * Goalkeeper.THREAT_RADIUS // выход за пределы защитного круга
                && game.ball.position.getSquaredDistance(Goalkeeper.THREAT_CENTER) > Goalkeeper.THREAT_RADIUS * Goalkeeper.THREAT_RADIUS)) {
            return arena.depth;
        }
        double distance = game.robots[0].position.getSquaredDistance(game.ball.position.dx,
                game.robots[0].position.dy, game.ball.position.dz);
        return distance;
        /*if (distance < branch.minSquaredDistance) {
            branch.minSquaredDistance = distance;
            branch.getMinSquaredDistanceY = branch.game.robots[0].position.dy;
        }*/
    }

    private static Action createAction(Vector vector) {
        Action action = new Action();
        action.target_velocity_x = vector.dx;
        action.target_velocity_z = vector.dz;
        return action;
    }

    private static int[] generateRandomChain() {
        int[] chain = new int[LEN];
        for (int i = 0; i < LEN; i++) {
            chain[i] = random.nextInt(actions.size());
        }
        return chain;
    }

    private static void mutateA(int[] chain) {
        chain[random.nextInt(LEN)] = random.nextInt(actions.size());
    }

    private static void mutateB (int[] chain) {
        int a = random.nextInt(actions.size());
        for (int i = random.nextInt(LEN); i < LEN; i++) {
            chain[i] = a;
        }
    }
}
