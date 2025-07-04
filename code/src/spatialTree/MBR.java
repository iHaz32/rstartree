package spatialTree;

public class MBR {

    private double[] min;
    private double[] max;

    public MBR(double[] min, double[] max) {
        this.min = min;
        this.max = max;
    }

    // Returns the area (or volume) of the MBR
    public double area() {
        double area = 1.0;
        for (int i = 0; i < min.length; i++) {
            area *= (max[i] - min[i]);
        }
        return area;
    }

    // Checks if this MBR intersects with another MBR
    public boolean intersects(MBR other) {
        for (int i = 0; i < min.length; i++) {
            if (this.max[i] < other.min[i] || this.min[i] > other.max[i]) {
                return false;
            }
        }
        return true;
    }

    // Calculates how much the area increases if merging with another MBR
    public double enlargement(MBR other) {
        double[] newMin = new double[min.length];
        double[] newMax = new double[max.length];
        for (int i = 0; i < min.length; i++) {
            newMin[i] = Math.min(min[i], other.min[i]);
            newMax[i] = Math.max(max[i], other.max[i]);
        }
        double originalArea = this.area();
        double newArea = new MBR(newMin, newMax).area();
        return newArea - originalArea;
    }

    // Expands this MBR to include the other MBR
    public void merge(MBR other) {
        for (int i = 0; i < min.length; i++) {
            min[i] = Math.min(min[i], other.min[i]);
            max[i] = Math.max(max[i], other.max[i]);
        }
    }

    // Creates an MBR from a single point
    public static MBR fromPoint(double[] point) {
        return new MBR(point.clone(), point.clone());
    }

    public double[] getMin() {
        return min;
    }

    public double[] getMax() {
        return max;
    }

    // Checks if a point is inside this MBR
    public boolean contains(double[] point) {
        for (int i = 0; i < min.length; i++) {
            if (point[i] < min[i] || point[i] > max[i]) {
                return false;
            }
        }
        return true;
    }

    // Returns the minimum Euclidean distance from a point to this MBR
    public double minDistance(double[] point) {
        double sum = 0.0;
        for (int i = 0; i < min.length; i++) {
            double v = 0.0;
            if (point[i] < min[i]) v = min[i] - point[i];
            else if (point[i] > max[i]) v = point[i] - max[i];
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    // Returns the center of the MBR
    public double[] getCenter() {
        int dimensions = min.length;
        double[] center = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            center[i] = (min[i] + max[i]) / 2.0;
        }
        return center;
    }
}