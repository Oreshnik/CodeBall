package simulation;

import game_engine.Constants;
import game_engine.GameEngine;
import model.Action;
import mymodel.Ball;
import mymodel.Game;
import mymodel.Robot;
import mymodel.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static game_engine.GameEngine.arena;
import static java.lang.Math.PI;

/*WARNING this class wasn't used in late versions*/
public class MonteCarloTreeSearch {
    public static String text = "";

    private static Random random = new Random(100500);

    public static Node findNextMove(Robot robot, Ball ball, Node node, int approximateSteps, Action action, int tick) {
        Node root;
        if (node == null) {
            root = new Node();
            root.game = new Game(robot, ball);
            root.action = action;
            /*double rootRate = calcRate(root);
            root.lowest = rootRate;
            root.highest = rootRate;*/
        } else {
            root = node;
        }

        int time = 30;
        long start = System.currentTimeMillis();
        int nodes = 0;

        if (root.childArray == null) {
            expandNode(root, approximateSteps);
        }
        //мы уже находимся в точке с финальным состоянием
        if (root.childArray == null) {
            return root;
        }
        //для корня с одним потомком симулировать нечего
        if (root.childArray != null && root.childArray.length == 1) {
            root.childArray[0].parent = null;
            return root.childArray[0];
        }

        while (System.currentTimeMillis() - start < time || root.visits < 100) {
            Node promisingNode = selectPromisingNode(root);
            Node finalNode = play(promisingNode, approximateSteps);

            double playoutResult = calcRate(finalNode);
            backPropogation(finalNode, playoutResult);
            if (finalNode.visits >= 100) {
                break;
            }
        }
        Node winnerNode = root.getChildWithCombo();
        text = "t " + tick + ", s " + root.visits + ", max " + root.highest + ",  visits " + winnerNode.visits + ", avg " + winnerNode.sumRate / winnerNode.visits;
        System.out.println(text);
        winnerNode.parent = null;

        return winnerNode;
    }

    private static double calcRate(Node node) {
        double rate = 0;
        if (node.game.ball.collision) {
            rate = node.game.ball.speed.dz;
            rate += (19 - node.game.robots[0].position.dy) / 10; //бонус за удар без прыжка
        } else {
            rate = -node.game.ball.position.getDistance(node.game.robots[0].position);
        }
        if (Math.abs(node.game.ball.position.dz) > arena.depth / 2 + node.game.ball.radius) {
            rate -= 100;
        }
        return rate;
    }

    private static Node play(Node nodeToExplore, int approximateSteps) {
        Node node = nodeToExplore;
        expandNode(node, approximateSteps);
        while (node.childArray != null) {
            int n = random.nextInt(node.childArray.length);
            Node tmpNode = node.childArray[node.childArray.length - 1];
            node.childArray[node.childArray.length - 1] = node.childArray[n];
            node.childArray[n] = tmpNode;
            node = node.childArray[node.childArray.length - 1];
            expandNode(node, approximateSteps);
        }
        return node;
    }

    private static void backPropogation(Node nodeToExplore, double playoutResult) {
        Node tempNode = nodeToExplore;
        while (tempNode != null) {
            tempNode.visits ++;
            tempNode.sumRate += playoutResult;
            if (playoutResult < tempNode.lowest) {
                tempNode.lowest = playoutResult;
            }
            if (playoutResult > tempNode.highest) {
                tempNode.highest = playoutResult;
            }
            tempNode = tempNode.parent;
        }
    }


    private static void expandNode(Node promisingNode, int approximateSteps) {
        if (promisingNode.game == null) {
            promisingNode.game = new Game(promisingNode.parent.game.robots[0].copy(), promisingNode.parent.game.ball.copy());
            promisingNode.game.robots[0].action = promisingNode.action;

            for (int i = 0; i < 3; i++) {
                GameEngine.tick(promisingNode.game, 10);
                if (promisingNode.game.ball.collision) {
                    break;
                }
            }
        }
        if (promisingNode.game.ball.collision || promisingNode.game.ball.position.dz < promisingNode.game.robots[0].position.dz) {
            return;
        }
        int n = 0;
        Node node = promisingNode;
        while (node.parent != null) {
            n ++;
            node = node.parent;
        }
        if (n >= approximateSteps * 2 / 3) {
            return;
        }

        List<Action> actionList = new ArrayList<>();
        Vector vector = new Vector(promisingNode.action.target_velocity_x, 0, promisingNode.action.target_velocity_z);
        if (vector.getSquaredLength() < 29 * 29) {
            vector.dx *= Constants.ROBOT_MAX_GROUND_SPEED - Constants.EPS;
            vector.dz *= Constants.ROBOT_MAX_GROUND_SPEED - Constants.EPS;
        }


        Action forward = new Action();
        forward.target_velocity_x = vector.dx;
        forward.target_velocity_z = vector.dz;
        actionList.add(forward);


        if (promisingNode.game.robots[0].touch) {
            /*Action jumpSmall = new Action();
            jumpSmall.jump_speed = 7;
            jumpSmall.target_velocity_x = vector.dx;
            jumpSmall.target_velocity_z = vector.dz;
            actionList.add(jumpSmall);

            Action jumpBig = new Action();
            jumpBig.jump_speed = 15;
            jumpBig.target_velocity_x = vector.dx;
            jumpBig.target_velocity_z = vector.dz;
            actionList.add(jumpBig);*/

            Action keepGoing = new Action();
            keepGoing.target_velocity_x = promisingNode.game.robots[0].speed.dx;
            keepGoing.target_velocity_z = promisingNode.game.robots[0].speed.dz;

            Action back = new Action();
            back.target_velocity_x = -vector.dx;
            back.target_velocity_z = -vector.dz;
            actionList.add(back);

            Action forwardSlow = new Action();
            forwardSlow.target_velocity_x = vector.dx / (Constants.ROBOT_MAX_GROUND_SPEED - Constants.EPS);
            forwardSlow.target_velocity_z = vector.dz / (Constants.ROBOT_MAX_GROUND_SPEED - Constants.EPS);
            actionList.add(forwardSlow);

            vector.rotate(-PI / 6);
            Action left = new Action();
            left.target_velocity_x = vector.dx;
            left.target_velocity_z = vector.dz;
            actionList.add(left);

            vector.rotate(PI / 3);
            Action right = new Action();
            right.target_velocity_x = vector.dx;
            right.target_velocity_z = vector.dz;
            actionList.add(right);

            vector.rotate(PI / 3);
            Action rightR = new Action();
            right.target_velocity_x = vector.dx;
            right.target_velocity_z = vector.dz;
            actionList.add(rightR);

            Action leftL = new Action();
            right.target_velocity_x = -vector.dx;
            right.target_velocity_z = -vector.dz;
            actionList.add(leftL);
        }

        promisingNode.childArray = new Node[actionList.size()];
        for (int i = 0; i < actionList.size(); i++) {
            Action action = actionList.get(i);
            Node newNode = new Node();
            newNode.parent = promisingNode;
            newNode.action = action;
            promisingNode.childArray[i] = newNode;
        }
    }

    private static Node selectPromisingNode(Node root) {
        Node node = root;
        while (node.childArray != null/* && node.childArray.length != 0*/) {
            node = UCT.findBestNodeWithUCT(node);
        }
        return node;
    }

    private static class UCT {
        static double calcUCTValueAvg(double logParentVisits, Node node) {
            //считаем по среднему
            return (node.sumRate - node.parent.lowest * node.visits) / (Math.max(0, node.parent.highest - node.parent.lowest))
                    / node.visits + Math.sqrt(2 * logParentVisits / node.visits);
        }

        static double calcUCTValueMax(double logParentVisits, Node node) {

            //подсчет по максимуму
            return (node.highest - node.parent.lowest) / (Math.max(0, node.parent.highest - node.parent.lowest))
                    + Math.sqrt(2 * logParentVisits / node.visits);
        }

        static double calcUCTValueCombo(double logParentVisits, Node node) {

            //подсчет по среднему + максимум
            return ((node.sumRate - node.parent.lowest * node.visits) / node.visits + (node.highest - node.parent.lowest))
                    / (2 * Math.max(0, node.parent.highest - node.parent.lowest))
                    + Math.sqrt(2 * logParentVisits / node.visits);

        }

        static Node findBestNodeWithUCT(Node node) {
            if (node.childArray.length == 1) {
                return node.childArray[0];
            }
            if (node.childArray.length > node.visits) {
                int n = random.nextInt(node.childArray.length - node.visits);
                Node tmp = node.childArray[node.childArray.length - node.visits - 1];
                node.childArray[node.childArray.length - node.visits - 1] = node.childArray[n];
                node.childArray[n] = tmp;
                return node.childArray[node.childArray.length - node.visits - 1];
            }

            double logParentVisits = Math.log(node.visits);
            Node best = null;
            double max = -Double.MAX_VALUE;
            for (int i = 0; i < node.childArray.length; i++) {
                Node child = node.childArray[i];
                if (child.visits == 0) {
                    return child;
                }
                //double uct = calcUCTValueAvg(logParentVisits, child);
                //double uct = calcUCTValueMax(logParentVisits, child);
                double uct = calcUCTValueCombo(logParentVisits, child);
                if (uct > max) {
                    max = uct;
                    best = child;
                }
            }
            if (best == null) {
                return node.childArray[0];
            }
            return best;
        }
    }


    public static class Node {
        public Game game;
        public Action action;
        int visits;
        Node parent;
        public Node childArray[];
        double sumRate;
        double lowest = Double.MAX_VALUE;
        double highest = -Double.MAX_VALUE;

        public Node getChildWithMaxValue() {
            double max = -Double.MAX_VALUE;
            Node best = null;
            for (int i = 0; i < childArray.length; i++) {
                Node child = childArray[i];
                if (child.highest > max) {
                    max = child.highest;
                    best = child;
                }
            }
            return best;
        }

        public Node getChildWithMaxRatio() {
            double max = -Double.MAX_VALUE;
            Node best = null;
            for (int i = 0; i < childArray.length; i++) {
                Node child = childArray[i];
                double avgRate = child.sumRate / Math.max(child.visits, 1);

                if (avgRate > max) {
                    max = avgRate;
                    best = child;
                }
            }
            return best;
        }

        public Node getChildWithCombo() {
            double max = -Double.MAX_VALUE;
            Node best = null;
            for (int i = 0; i < childArray.length; i++) {
                Node child = childArray[i];
                double avgHighRate = (child.sumRate / Math.max(child.visits, 1) + child.highest) / 2;

                if (avgHighRate > max) {
                    max = avgHighRate;
                    best = child;
                }
            }
            return best;
        }
    }
}
