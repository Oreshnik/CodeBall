package rendering;

import mymodel.Vector;

public class Render {
    private static StringBuilder stringBuilder;

    public static void clear() {
        stringBuilder = new StringBuilder("[");
    }

    public static String getCustomRendering() {
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public static void addSphere(Vector position, double radius, double r, double g, double b, double a) {
        addSphere(stringBuilder, position, radius, r, g, b, a);
    }

    public static void addSphere(StringBuilder s, Vector position, double radius, double r, double g, double b, double a) {
        if (s.length() > 1) {
            s.append(", ");
        }
        s.append("{\"Sphere\": {\"x\":").append(position.dx).append(", \"y\": ")
                .append(position.dy).append(", \"z\": ").append(position.dz)
                .append(",\"radius\": ").append(radius).append(", \"r\": ")
                .append(r).append(", \"g\":").append(g).append(", \"b\":").append(b).append(", \"a\": ").append(a).append("}} ");
    }

    public static void addString(String string) {
        if (stringBuilder.length() > 1) {
            stringBuilder.append(", ");
        }
        stringBuilder.append(string);
    }

    public static void addText(String text) {
        if (stringBuilder.length() > 1) {
            stringBuilder.append(", ");
        }
        stringBuilder.append("{\"Text\": \"").append(text).append("\"}");
    }
}
