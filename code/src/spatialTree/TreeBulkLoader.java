package spatialTree;

import java.util.*;

// Implements bottom-up bulk loading for R*-Tree
public class TreeBulkLoader {
    public static RStarTree bulkLoad(List<double[]> dataPoints, List<TreeRecordID> recordPointers, int maxPerNode, int dims) {
        // Sort points by first dimension
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < dataPoints.size(); i++) order.add(i);
        order.sort(Comparator.comparingDouble(idx -> dataPoints.get(idx)[0]));

        // Create leaf nodes
        List<TreeNode> thisLevel = new ArrayList<>();
        for (int i = 0; i < order.size(); i += maxPerNode) {
            TreeLeafNode leaf = new TreeLeafNode(dims);
            for (int j = i; j < Math.min(i + maxPerNode, order.size()); j++) {
                leaf.insert(dataPoints.get(order.get(j)), recordPointers.get(order.get(j)), maxPerNode);
            }
            thisLevel.add(leaf);
        }

        // Build upper levels (internal nodes)
        while (thisLevel.size() > 1) {
            List<TreeNode> upperLevel = new ArrayList<>();
            for (int i = 0; i < thisLevel.size(); i += maxPerNode) {
                TreeInternalNode internal = new TreeInternalNode(dims);
                for (int j = i; j < Math.min(i + maxPerNode, thisLevel.size()); j++) {
                    internal.addChild(thisLevel.get(j));
                }
                upperLevel.add(internal);
            }
            thisLevel = upperLevel;
        }

        // Set tree root and return
        RStarTree tree = new RStarTree(maxPerNode, dims);
        if (!thisLevel.isEmpty()) {
            tree.root = thisLevel.get(0);
            tree.globalRoot = tree.root;
        }
        return tree;
    }
}