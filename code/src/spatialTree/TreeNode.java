package spatialTree;

import java.util.List;

// Abstract base class for R*-Tree nodes (internal & leaf nodes)
public abstract class TreeNode {

    protected MBR mbr;
    protected TreeNode parent;
    protected int dimensions;

    public TreeNode() {}

    public TreeNode(int dimensions) {
        this.dimensions = dimensions;
    }

    // Returns this node's MBR
    public MBR getMbr() {
        return mbr;
    }

    // Sets this node's parent
    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    public TreeNode getParent() {
        return parent;
    }

    // True if this is a leaf node
    public abstract boolean isLeaf();

    // Returns node's children (empty for leaf)
    public abstract List<TreeNode> getChildren();

    // Insert a point and its record ID
    public abstract void insert(double[] point, TreeRecordID rid, int maxEntries);

    // Delete a point from the node
    public abstract boolean delete(double[] point, int maxEntries);
}