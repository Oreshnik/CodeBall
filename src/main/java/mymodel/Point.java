package mymodel;

public class Point {
    public double x, y, z;
    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public double getSquareDistanceTo(double x, double y, double z) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        return dx * dx + dy * dy + dz * dz;
    }
    public double getDistanceTo(double x, double y, double z) {
        return Math.sqrt(getSquareDistanceTo(x, y, z));
    }
    public double getDistanceTo(Point p) {
        if (p == null) {
            return 0;
        }
        return getDistanceTo(p.x, p.y, p.y);
    }
    double getSquaredDistanceTo(Point p) {
        if (p == null) {
            return 0;
        }
        return getSquareDistanceTo(p.x, p.y, p.z);
    }

    public double getShortestDistanceToLane(Point from, Point to) {
        if (from.x == to.x) {
            return Math.abs(x - from.x);
        }
        double m = (to.y - from.y) * 1.0 / (to.x - from.x);
        double a = - m;
        double b = 1;
        double c = - from.y + m * from.x;

        return Math.abs(a * x + b * y + c) / (Math.sqrt(a * a + b * b));
    }

    @Override
    public String toString() {
        return String.format("%.2f %.2f %.2f", x, y, z);
    }
}
