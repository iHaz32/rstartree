package spatialTree;

// Stores a node and its distance; used for distance-based sorting
public class NodeDIstanceInfo implements Comparable<NodeDIstanceInfo> {
    public TreeNode node;
    public double distance;

    public NodeDIstanceInfo(TreeNode node, double distance) {
        this.node = node;
        this.distance = distance;
    }

    // Sorts by distance (ascending)
    @Override
    public int compareTo(NodeDIstanceInfo other) {
        return Double.compare(this.distance, other.distance);
    }
}