package spatialTree;

import java.util.*;

// Internal node for R*-Tree, manages children nodes and advanced split/reinsertion logic
public class TreeInternalNode extends TreeNode {

    private List<TreeNode> children;
    private boolean hasReinserted = false;

    public TreeInternalNode() {
        super();
        this.children = new ArrayList<>();
    }

    // Constructor for bulk loading
    public TreeInternalNode(int dimensions) {
        super(dimensions);
        this.children = new ArrayList<>();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public List<TreeNode> getChildren() {
        return children;
    }

    // Insert point to the best child, handle split/reinsertion if needed
    @Override
    public void insert(double[] point, TreeRecordID rid, int maxEntries) {
        TreeNode chosen = chooseSubtree(point);
        chosen.insert(point, rid, maxEntries);

        // Update MBR after insert
        if (mbr == null) {
            mbr = chosen.getMbr();
        } else {
            mbr.merge(chosen.getMbr());
        }

        // Handle overfull
        if (children.size() > maxEntries) {
            if (!hasReinserted) {
                hasReinserted = true;
                triggerReinsertion(maxEntries);
            } else {
                TreeInternalNode sibling = this.rstarSplit();

                if (parent == null) {
                    RStarTree.promoteRoot(this, sibling);
                } else if (parent instanceof TreeInternalNode) {
                    ((TreeInternalNode) parent).addSiblingAfterSplit(this, sibling);
                } else {
                    throw new IllegalStateException("Parent of TreeInternalNode is not TreeInternalNode!");
                }
            }
        }
    }

    // Reinserts 30% of farthest children from center
    private void triggerReinsertion(int maxEntries) {
        int total = children.size();
        int numToReinsert = (int) (0.3 * total);

        double[] ctr = mbr.getCenter();
        List<Integer> idxList = new ArrayList<>();
        for (int i = 0; i < total; i++) idxList.add(i);

        idxList.sort((a, b) -> {
            double distA = calcDistance(children.get(a).getMbr().getCenter(), ctr);
            double distB = calcDistance(children.get(b).getMbr().getCenter(), ctr);
            return Double.compare(distB, distA);
        });

        List<TreeNode> forReinsert = new ArrayList<>();
        for (int i = 0; i < numToReinsert; i++) {
            forReinsert.add(children.get(idxList.get(i)));
        }

        // Remove from children (descending order)
        idxList.subList(0, numToReinsert).sort(Collections.reverseOrder());
        for (int idx : idxList.subList(0, numToReinsert)) {
            children.remove(idx);
        }

        updateMBR();

        for (TreeNode n : forReinsert) {
            n.setParent(null);
            recursivelyReinsertAllPoints(n, maxEntries);
        }
    }

    // Recursively reinsert all points of a subtree
    private void recursivelyReinsertAllPoints(TreeNode n, int maxEntries) {
        if (n.isLeaf()) {
            TreeLeafNode leaf = (TreeLeafNode) n;
            for (int i = 0; i < leaf.getPointCount(); i++) {
                RStarTree.globalRoot.insert(leaf.getPoint(i), leaf.getRecordID(i), maxEntries);
            }
        } else {
            TreeInternalNode branch = (TreeInternalNode) n;
            for (TreeNode child : branch.getChildren()) {
                recursivelyReinsertAllPoints(child, maxEntries);
            }
        }
    }

    private double calcDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    @Override
    public boolean delete(double[] point, int maxEntries) {
        MBR ptMBR = MBR.fromPoint(point);
        boolean deletedSomewhere = false;

        for (Iterator<TreeNode> it = children.iterator(); it.hasNext(); ) {
            TreeNode child = it.next();
            if (child.getMbr().intersects(ptMBR)) {
                boolean deleted = child.delete(point, maxEntries);

                // Remove empty children
                if (deleted) {
                    if (child.isLeaf() && ((TreeLeafNode) child).getPointCount() == 0) {
                        it.remove();
                    } else if (!child.isLeaf() && child.getChildren().isEmpty()) {
                        it.remove();
                    }
                    deletedSomewhere = true;
                }
            }
        }
        updateMBR();
        return deletedSomewhere;
    }

    // Add sibling node after split
    public void addSiblingAfterSplit(TreeNode original, TreeNode sibling) {
        int index = children.indexOf(original);
        if (index != -1) {
            children.add(index + 1, sibling);
        } else {
            children.add(sibling);
        }
        sibling.setParent(this);
        mbr.merge(sibling.getMbr());
    }

    // Add a child node, update parent and mbr
    public void addChild(TreeNode child) {
        children.add(child);
        child.setParent(this);

        if (mbr == null) {
            mbr = child.getMbr();
        } else {
            mbr.merge(child.getMbr());
        }
    }

    // Selects the child that causes the least enlargement
    private TreeNode chooseSubtree(double[] point) {
        TreeNode best = null;
        double minEnlargement = Double.MAX_VALUE;
        MBR ptMBR = MBR.fromPoint(point);

        for (TreeNode child : children) {
            double enlargement = child.getMbr().enlargement(ptMBR);
            if (enlargement < minEnlargement) {
                minEnlargement = enlargement;
                best = child;
            }
        }
        return best;
    }

    // Splits children into two groups, returns the new sibling
    public TreeInternalNode rstarSplit() {
        int total = children.size();
        int dims = children.get(0).getMbr().getMin().length;
        int minSplit = (int) Math.ceil(0.4 * total);
        int maxSplit = total - minSplit;

        double minOverlap = Double.MAX_VALUE;
        double minArea = Double.MAX_VALUE;
        TreeInternalNode bestLeft = null, bestRight = null;

        for (int dim = 0; dim < dims; dim++) {
            List<Integer> idxList = new ArrayList<>();
            for (int i = 0; i < total; i++) idxList.add(i);

            int currentDim = dim;
            idxList.sort(Comparator.comparingDouble(i -> children.get(i).getMbr().getMin()[currentDim]));

            for (int k = minSplit; k <= maxSplit; k++) {
                TreeInternalNode left = new TreeInternalNode();
                TreeInternalNode right = new TreeInternalNode();

                for (int i = 0; i < k; i++) left.addChild(children.get(idxList.get(i)));
                for (int i = k; i < total; i++) right.addChild(children.get(idxList.get(i)));

                double overlap = calcOverlap(left.getMbr(), right.getMbr());
                double area = left.getMbr().area() + right.getMbr().area();

                if (overlap < minOverlap || (overlap == minOverlap && area < minArea)) {
                    minOverlap = overlap;
                    minArea = area;
                    bestLeft = left;
                    bestRight = right;
                }
            }
        }

        this.children = bestLeft.children;
        this.mbr = bestLeft.getMbr();
        for (TreeNode ch : children) ch.setParent(this);

        TreeInternalNode sibling = new TreeInternalNode();
        sibling.children = bestRight.children;
        sibling.mbr = bestRight.getMbr();
        sibling.setParent(this.parent);
        for (TreeNode ch : sibling.children) ch.setParent(sibling);

        return sibling;
    }

    // Calculates overlap area between two MBRs
    private double calcOverlap(MBR m1, MBR m2) {
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

    // Recalculates the MBR for this node
    private void updateMBR() {
        if (children.isEmpty()) {
            mbr = null;
            return;
        }
        mbr = children.get(0).getMbr();
        for (int i = 1; i < children.size(); i++) {
            mbr.merge(children.get(i).getMbr());
        }
    }
}