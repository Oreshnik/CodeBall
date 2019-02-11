package mymodel;

import static java.lang.StrictMath.PI;

public class Vector {
    public double dx, dy, dz;

    public Vector(Vector vector) {
        this(vector.dx, vector.dy, vector.dz);
    }
    public Vector(double dx, double dy, double dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }
    public Vector(Point from, Point to) {
        this(to.x - from.x, to.y - from.y, to.z - from.z);
    }

    public Vector copy() {
        return new Vector(this);
    }

    public Vector reverse() {
        dx = -dx;
        dy = -dy;
        dz = -dz;
        return this;
    }

    public double getLength() {
        return Math.sqrt(getSquaredLength());
    }

    public double getSquaredLength() {
        return dx * dx + dy * dy + dz * dz;
    }

    public Vector setLength(double length) {
        double c = getLength() / length;
        dx = dx / c;
        dy = dy / c;
        dz = dz / c;
        return this;
    }

    public Vector normalize() {
        return setLength(1);
    }

    public Vector multiply(double multi) {
        dx = dx * multi;
        dy = dy * multi;
        dz = dz * multi;
        return this;
    }

    public Vector divide(double divider) {
        dx /= divider;
        dy /= divider;
        dz /= divider;
        return this;
    }

    public Vector add(double x, double y, double z) {
        dx += x;
        dy += y;
        dz += z;
        return this;
    }

    public Vector add(Vector other) {
        return add(other.dx, other.dy, other.dz);
    }

    public Vector subtract(double x, double y, double z) {
        dx -= x;
        dy -= y;
        dz -= z;
        return this;
    }

    public Vector subtract(Vector other) {
        return subtract(other.dx, other.dy, other.dz);
    }

    public double dotProduct(Vector vector) {
        return dx * vector.dx + dy * vector.dy + dz * vector.dz;
    }

    public double getSquaredDistance(double x, double y, double z) {
        double dx = this.dx - x;
        double dy = this.dy - y;
        double dz = this.dz - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double getSquaredDistance(Vector vector) {
        return getSquaredDistance(vector.dx, vector.dy, vector.dz);
    }

    public double getDistance(double x, double y, double z) {
        return Math.sqrt(getSquaredDistance(x, y, z));
    }

    public double getDistance(Vector vector) {
        return Math.sqrt(getSquaredDistance(vector));
    }

    public Vector set(double dx, double dy, double dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        return this;
    }

    public Vector set(Vector other) {
        this.dx = other.dx;
        this.dy = other.dy;
        this.dz = other.dz;
        return this;
    }


    /*Положительный угол при вращении по часовой стрелке при начале координат в левом верхнем углу
     * если надо в другую сторону поменять на double relativeAngle = angle - otherAngle; */
    public double getAngleTo(Vector other) {
        double angle = Math.atan2(dz, dx);
        double otherAngle = Math.atan2(other.dz, other.dx);
        double relativeAngle = otherAngle - angle;
        while (relativeAngle > PI) {
            relativeAngle -= 2.0D * PI;
        }
        while (relativeAngle < -PI) {
            relativeAngle += 2.0D * PI;
        }
        return relativeAngle;
    }


    //Положительный угол вращения дает поворот по часовой стрелке при начале координат в левом верхнем углу
    //Если надо в другую сторону поменя +/-
    public Vector rotate(double angle) {
        double nx = dx * Math.cos(angle) - dz * Math.sin(angle);
        dz = dx * Math.sin(angle) + dz * Math.cos(angle);
        dx = nx;
        return this;
    }

    public String toString() {
        return dx + ", " + dy + ", " + dz;
    }
}
