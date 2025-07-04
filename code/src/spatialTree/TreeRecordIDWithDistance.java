package spatialTree;

// Combines a TreeRecordID with a distance, used for kNN/maxHeap, etc.
public class TreeRecordIDWithDistance implements Comparable<TreeRecordIDWithDistance> {
    public TreeRecordID recordID;
    public double distance;

    public TreeRecordIDWithDistance(TreeRecordID recordID, double distance) {
        this.recordID = recordID;
        this.distance = distance;
    }

    @Override
    public int compareTo(TreeRecordIDWithDistance other) {
        // For MaxHeap — farther distances come first
        return Double.compare(other.distance, this.distance);
    }
}