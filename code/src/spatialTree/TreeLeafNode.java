package spatialTree;

import java.util.*;

// Leaf node for R*-Tree, stores points and their TreeRecordIDs
public class TreeLeafNode extends TreeNode {

    private List<double[]> points;    // d-dimensional points
    private List<TreeRecordID> pointers;  // Each point's record ID (block/slot)
    private boolean hasReinserted = false; // R*-tree: flag for one-time reinsertion

    public TreeLeafNode() {
        super();
        points = new ArrayList<>();
        pointers = new ArrayList<>();
    }

    // For bulk loading
    public TreeLeafNode(int dims) {
        super(dims);
        points = new ArrayList<>();
        pointers = new ArrayList<>();
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public List<TreeNode> getChildren() {
        return Collections.emptyList();
    }

    // Insert a point & update MBR
    @Override
    public void insert(double[] point, TreeRecordID rid, int maxEntries) {
        points.add(point);
        pointers.add(rid);

        if (mbr == null) {
            mbr = MBR.fromPoint(point);
        } else {
            mbr.merge(MBR.fromPoint(point));
        }

        if (points.size() > maxEntries) {
            if (!hasReinserted) {
                hasReinserted = true;
                performReinsertion(maxEntries);
            } else {
                TreeLeafNode sibling = this.rstarSplit();

                if (parent == null) {
                    RStarTree.promoteRoot(this, sibling);
                    return;
                }

                if (parent instanceof TreeInternalNode) {
                    ((TreeInternalNode) parent).addSiblingAfterSplit(this, sibling);
                } else {
                    throw new IllegalStateException("Parent of TreeLeafNode is not TreeInternalNode.");
                }
            }
        }
    }

    // Remove 30% of farthest points and reinsert them
    private void performReinsertion(int maxEntries) {
        int total = points.size();
        int reinsertionCount = (int) (0.3 * total);

        double[] center = mbr.getCenter();
        List<Integer> idxs = new ArrayList<>();
        for (int i = 0; i < total; i++) idxs.add(i);

        // Sort by descending distance from center
        idxs.sort((a, b) -> {
            double distA = distance(points.get(a), center);
            double distB = distance(points.get(b), center);
            return Double.compare(distB, distA);
        });

        List<double[]> reinPoints = new ArrayList<>();
        List<TreeRecordID> reinRids = new ArrayList<>();
        for (int i = 0; i < reinsertionCount; i++) {
            reinPoints.add(points.get(idxs.get(i)));
            reinRids.add(pointers.get(idxs.get(i)));
        }

        // Remove (in reverse order)
        idxs.subList(0, reinsertionCount).sort(Collections.reverseOrder());
        for (int idx : idxs.subList(0, reinsertionCount)) {
            points.remove(idx);
            pointers.remove(idx);
        }

        recalculateMBR();

        // Reinsert removed points
        for (int i = 0; i < reinPoints.size(); i++) {
            if (parent == null) {
                this.insert(reinPoints.get(i), reinRids.get(i), maxEntries);
            } else {
                parent.insert(reinPoints.get(i), reinRids.get(i), maxEntries);
            }
        }
    }

    // Euclidean distance helper
    private double distance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    // Checks if two points are equal (epsilon)
    private boolean pointsEqual(double[] a, double[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > 1e-7) return false;
        }
        return true;
    }

    @Override
    public boolean delete(double[] point, int maxEntries) {
        for (int i = 0; i < points.size(); i++) {
            if (pointsEqual(points.get(i), point)) {
                points.remove(i);
                pointers.remove(i);
                recalculateMBR();
                return true;
            }
        }
        return false;
    }

    // Split when leaf is full: returns new sibling node
    public TreeLeafNode rstarSplit() {
        int total = points.size();
        int d = points.get(0).length;
        int minSplit = (int) Math.ceil(0.4 * total);
        int maxSplit = total - minSplit;

        double minOverlap = Double.MAX_VALUE;
        double minArea = Double.MAX_VALUE;
        TreeLeafNode bestLeft = null, bestRight = null;

        for (int dim = 0; dim < d; dim++) {
            List<Integer> idxs = new ArrayList<>();
            for (int i = 0; i < total; i++) idxs.add(i);

            int finalDim = dim;
            idxs.sort(Comparator.comparingDouble(i -> points.get(i)[finalDim]));

            for (int k = minSplit; k <= maxSplit; k++) {
                TreeLeafNode left = new TreeLeafNode();
                TreeLeafNode right = new TreeLeafNode();

                for (int i = 0; i < k; i++) {
                    int idx = idxs.get(i);
                    left.points.add(points.get(idx));
                    left.pointers.add(pointers.get(idx));
                    if (left.mbr == null) left.mbr = MBR.fromPoint(points.get(idx));
                    else left.mbr.merge(MBR.fromPoint(points.get(idx)));
                }

                for (int i = k; i < total; i++) {
                    int idx = idxs.get(i);
                    right.points.add(points.get(idx));
                    right.pointers.add(pointers.get(idx));
                    if (right.mbr == null) right.mbr = MBR.fromPoint(points.get(idx));
                    else right.mbr.merge(MBR.fromPoint(points.get(idx)));
                }

                double overlap = computeOverlap(left.mbr, right.mbr);
                double area = left.mbr.area() + right.mbr.area();

                if (overlap < minOverlap || (overlap == minOverlap && area < minArea)) {
                    minOverlap = overlap;
                    minArea = area;
                    bestLeft = left;
                    bestRight = right;
                }
            }
        }

        this.points = bestLeft.points;
        this.pointers = bestLeft.pointers;
        this.mbr = bestLeft.mbr;

        TreeLeafNode sibling = new TreeLeafNode();
        sibling.points = bestRight.points;
        sibling.pointers = bestRight.pointers;
        sibling.mbr = bestRight.mbr;
        sibling.setParent(this.parent);

        return sibling;
    }

    // Calculates overlap area between two MBRs
    private double computeOverlap(MBR m1, MBR m2) {
        double[] min = new double[m1.getMin().length];
        double[] max = new double[m1.getMin().length];
        for (int i = 0; i < min.length; i++) {
            min[i] = Math.max(m1.getMin()[i], m2.getMin()[i]);
            max[i] = Math.min(m1.getMax()[i], m2.getMax()[i]);
            if (min[i] > max[i]) return 0.0;
        }
        double overlap = 1.0;
        for (int i = 0; i < min.length; i++) {
            overlap *= (max[i] - min[i]);
        }
        return overlap;
    }

    // Recompute MBR after deletion
    private void recalculateMBR() {
        if (points.isEmpty()) {
            mbr = null;
            return;
        }

        mbr = MBR.fromPoint(points.get(0));
        for (int i = 1; i < points.size(); i++) {
            mbr.merge(MBR.fromPoint(points.get(i)));
        }
    }

    public int getPointCount() {
        return points.size();
    }

    public double[] getPoint(int index) {
        return points.get(index);
    }

    public TreeRecordID getRecordID(int index) {
        return pointers.get(index);
    }
}