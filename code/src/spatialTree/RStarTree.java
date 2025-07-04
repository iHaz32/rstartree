package spatialTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

// Handles the R*-tree root, insertion, deletion, splits, and queries
public class RStarTree {

    TreeNode root;
    private final int maxEntries;
    public final int dimensions;

    static TreeNode globalRoot;

    public RStarTree(int maxEntries, int dimensions) {
        this.maxEntries = maxEntries;
        this.dimensions = dimensions;
        this.root = new TreeLeafNode(); // initial leaf
    }

    // Insert a point with its record ID
    public void insert(double[] point, TreeRecordID rid) {
        root.insert(point, rid, maxEntries);

        // Split root if needed
        while (needsSplit(getRoot())) {
            TreeNode sibling = splitRoot(getRoot());
            TreeInternalNode newRoot = new TreeInternalNode();
            newRoot.addChild(getRoot());
            newRoot.addChild(sibling);

            getRoot().setParent(newRoot);
            sibling.setParent(newRoot);
            root = newRoot;
            globalRoot = newRoot;
        }
    }

    // Checks if a node needs to split
    private boolean needsSplit(TreeNode node) {
        if (node instanceof TreeLeafNode) {
            return ((TreeLeafNode) node).getPointCount() > maxEntries;
        } else if (node instanceof TreeInternalNode) {
            return ((TreeInternalNode) node).getChildren().size() > maxEntries;
        }
        return false;
    }

    // Splits the root node and returns the new sibling
    private TreeNode splitRoot(TreeNode node) {
        if (node instanceof TreeLeafNode) {
            return ((TreeLeafNode) node).rstarSplit();
        } else if (node instanceof TreeInternalNode) {
            return ((TreeInternalNode) node).rstarSplit();
        }
        return null;
    }

    // Promotes two nodes to a new root
    public static void promoteRoot(TreeNode left, TreeNode right) {
        TreeInternalNode newRoot = new TreeInternalNode();
        newRoot.addChild(left);
        newRoot.addChild(right);
        left.setParent(newRoot);
        right.setParent(newRoot);
        globalRoot = newRoot;
    }

    public TreeNode getRoot() {
        return globalRoot != null ? globalRoot : root;
    }

    public int getDimensions() {
        return dimensions;
    }

    // ------------------ DELETION ------------------

    // Delete a point from the tree
    public boolean delete(double[] point) {
        boolean deleted = root.delete(point, maxEntries);

        // If root has only one child, promote it
        if (deleted && root instanceof TreeInternalNode) {
            TreeInternalNode internal = (TreeInternalNode) root;
            if (internal.getChildren().size() == 1) {
                root = internal.getChildren().get(0);
                root.setParent(null);
            }
        }
        return deleted;
    }

    // ------------------ RANGE QUERY ------------------

    // Returns all TreeRecordIDs inside the query MBR
    public List<TreeRecordID> rangeQuery(MBR query) {
        List<TreeRecordID> results = new ArrayList<>();
        rangeQueryRecursive(root, query, results);
        return results;
    }

    private void rangeQueryRecursive(TreeNode node, MBR query, List<TreeRecordID> results) {
        if (!node.getMbr().intersects(query)) {
            return;
        }
        if (node.isLeaf()) {
            TreeLeafNode leaf = (TreeLeafNode) node;
            for (int i = 0; i < leaf.getPointCount(); i++) {
                double[] point = leaf.getPoint(i);
                if (query.contains(point)) {
                    results.add(leaf.getRecordID(i));
                }
            }
        } else {
            TreeInternalNode internal = (TreeInternalNode) node;
            for (TreeNode child : internal.getChildren()) {
                rangeQueryRecursive(child, query, results);
            }
        }
    }

    // Euclidean distance between two points
    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    // k-Nearest Neighbors search
    public List<TreeRecordID> kNearestNeighbors(double[] queryPoint, int k) {
        PriorityQueue<NodeDIstanceInfo> queue = new PriorityQueue<>();
        PriorityQueue<TreeRecordIDWithDistance> bestK = new PriorityQueue<>(k);

        queue.add(new NodeDIstanceInfo(root, root.getMbr().minDistance(queryPoint)));

        while (!queue.isEmpty()) {
            NodeDIstanceInfo nd = queue.poll();
            TreeNode node = nd.node;

            if (node.isLeaf()) {
                TreeLeafNode leaf = (TreeLeafNode) node;
                for (int i = 0; i < leaf.getPointCount(); i++) {
                    double dist = euclideanDistance(queryPoint, leaf.getPoint(i));
                    bestK.add(new TreeRecordIDWithDistance(leaf.getRecordID(i), dist));
                    if (bestK.size() > k) {
                        bestK.poll();
                    }
                }
            } else {
                for (TreeNode child : node.getChildren()) {
                    double childDist = child.getMbr().minDistance(queryPoint);
                    queue.add(new NodeDIstanceInfo(child, childDist));
                }
            }
        }

        // Sort results by distance (closest first)
        List<TreeRecordIDWithDistance> tmpList = new ArrayList<>(bestK);
        tmpList.sort(Comparator.comparingDouble(r -> r.distance));
        List<TreeRecordID> result = new ArrayList<>();
        for (TreeRecordIDWithDistance r : tmpList) result.add(r.recordID);
        return result;
    }

    // ==================== SKYLINE QUERY ====================

    // Returns the skyline points' record IDs
    public List<TreeRecordID> skylineQuery() {
        List<double[]> points = getAllPoints();
        List<TreeRecordID> recordIDs = getAllRecordIDs();
        List<TreeRecordID> skyline = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            boolean dominated = false;
            for (int j = 0; j < points.size(); j++) {
                if (i != j && dominates(points.get(j), points.get(i))) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) {
                skyline.add(recordIDs.get(i));
            }
        }
        return skyline;
    }

    // Returns true if a dominates b
    private boolean dominates(double[] a, double[] b) {
        boolean strictlyBetter = false;
        for (int d = 0; d < a.length; d++) {
            if (a[d] > b[d]) return false;
            if (a[d] < b[d]) strictlyBetter = true;
        }
        return strictlyBetter;
    }

    // Gathers all points (coordinates) from the tree
    public List<double[]> getAllPoints() {
        List<double[]> result = new ArrayList<>();
        getAllPointsRecursive(root, result);
        return result;
    }

    private void getAllPointsRecursive(TreeNode node, List<double[]> result) {
        if (node.isLeaf()) {
            TreeLeafNode leaf = (TreeLeafNode) node;
            for (int i = 0; i < leaf.getPointCount(); i++) {
                result.add(leaf.getPoint(i));
            }
        } else {
            TreeInternalNode internal = (TreeInternalNode) node;
            for (TreeNode child : internal.getChildren()) {
                getAllPointsRecursive(child, result);
            }
        }
    }

    // Gathers all TreeRecordIDs from the tree (order matches getAllPoints)
    public List<TreeRecordID> getAllRecordIDs() {
        List<TreeRecordID> result = new ArrayList<>();
        getAllRecordIDsRecursive(root, result);
        return result;
    }

    private void getAllRecordIDsRecursive(TreeNode node, List<TreeRecordID> result) {
        if (node.isLeaf()) {
            TreeLeafNode leaf = (TreeLeafNode) node;
            for (int i = 0; i < leaf.getPointCount(); i++) {
                result.add(leaf.getRecordID(i));
            }
        } else {
            TreeInternalNode internal = (TreeInternalNode) node;
            for (TreeNode child : internal.getChildren()) {
                getAllRecordIDsRecursive(child, result);
            }
        }
    }

}