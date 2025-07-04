package testing;

import dataio.DataStorageHandler;
import dataio.IndexStorageHandler;
import domain.DataRecord;
import spatialTree.*;
import spatialTree.TreeLeafNode;

import java.io.IOException;
import java.util.*;

// Provides diagnostic methods to verify and test the R*-Tree functionality
public class SpatialTreeTester {

    public static void runAllTests(RStarTree tree, List<DataRecord> recordList) throws IOException {
        displayTree(tree.getRoot(), 0);

        // --- DELETE & VERIFY ---
        System.out.println("\n--- Διαγραφή & Επαλήθευση ---");
        List<double[]> allCoords = tree.getAllPoints();
        if (!allCoords.isEmpty()) {
            Random selector = new Random();
            int idx = selector.nextInt(allCoords.size());
            double[] coordToRemove = allCoords.get(idx);

            // Find corresponding record in the list
            DataRecord linked = recordList.stream()
                    .filter(r -> coordToRemove[0] == r.getLat() && coordToRemove[1] == r.getLon())
                    .findFirst()
                    .orElse(null);

            System.out.println("- Επιλέχθηκε προς διαγραφή το σημείο: " + Arrays.toString(coordToRemove));
            if (linked != null)
                System.out.println("- Συνεχίζεται με εγγραφή με id = " + linked.getId());

            boolean removed = tree.delete(coordToRemove);
            System.out.println("Το σημείο αφαιρέθηκε; " + removed);

            if (removed && linked != null) {
                IndexStorageHandler.deleteFromIndex(linked.getId());
                System.out.println("- Το id " + linked.getId() + " αφαιρέθηκε από το αρχείο ευρετηρίου.");
                TreeRecordID pos = IndexStorageHandler.lookupRecordIDById(linked.getId());
                if (pos == null) {
                    System.out.println("- Το id " + linked.getId() + " ΔΕΝ υπάρχει πλέον στο αρχείο ευρετηρίου.");
                } else {
                    System.out.println(" - Το id " + linked.getId() + " εξακολουθεί να υπάρχει στο indexfile!");
                    System.out.println("➡ pointer: blockId=" + pos.getBlockId() + ", slotId=" + pos.getSlotId());
                }
            }

        } else {
            System.out.println("⚠ Δεν υπάρχουν διαθέσιμα σημεία στο δέντρο.");
        }

        // Points remaining after deletion
        int countAfter = countAllPoints(tree.getRoot());
        System.out.println("Σύνολο σημείων μετά τη διαγραφή: " + countAfter);

        // --- Range Query Test ---
        System.out.println("\n--- Δοκιμή Εύρους (Range Query) ---");
        int dimension = tree.getDimensions();
        double[] minimums = new double[dimension];
        double[] maximums = new double[dimension];
        if (dimension >= 1) { minimums[0] = 41.48; maximums[0] = 41.57; }
        if (dimension >= 2) { minimums[1] = 26.45; maximums[1] = 26.54; }
        if (dimension >= 3) { minimums[2] = Long.MIN_VALUE; maximums[2] = Long.MAX_VALUE; }
        if (dimension >= 4) { minimums[3] = Long.MIN_VALUE; maximums[3] = Long.MAX_VALUE; }
        if (dimension >= 5) { minimums[4] = Long.MIN_VALUE; maximums[4] = Long.MAX_VALUE; }

        MBR area = new MBR(minimums, maximums);
        long startRange = System.nanoTime();
        List<TreeRecordID> located = tree.rangeQuery(area);
        long stopRange = System.nanoTime();
        System.out.println("Βρέθηκαν: " + located.size() + " (χρόνος: " + ((stopRange - startRange) / 1_000_000.0) + " ms)");

        // --- k-NN Query Test ---
        System.out.println("\n--- Αναζήτηση k-Nearest Neighbor ---");
        int k = 5;
        double[] probe = new double[dimension];
        if (dimension >= 1) probe[0] = 41.5;
        if (dimension >= 2) probe[1] = 26.5;
        if (dimension >= 3) probe[2] = 0;
        if (dimension >= 4) probe[3] = 0;
        if (dimension >= 5) probe[4] = 0;
        long startKnn = System.nanoTime();
        List<TreeRecordID> knnResults = tree.kNearestNeighbors(probe, k);
        long stopKnn = System.nanoTime();
        System.out.println("Τα " + k + " κοντινότερα σημεία: " + knnResults.size() + " (χρόνος: " + ((stopKnn - startKnn) / 1_000_000.0) + " ms)");

        // --- Skyline Query Test ---
        System.out.println("\n--- Αναζήτηση Skyline ---");
        long startSky = System.nanoTime();
        List<TreeRecordID> skylineRes = tree.skylineQuery();
        long stopSky = System.nanoTime();
        System.out.println("Συνολικά σημεία skyline: " + skylineRes.size() + " (χρόνος: " + ((stopSky - startSky) / 1_000_000.0) + " ms)");

        // --- Serial Range Query ---
        System.out.println("\n--- Σειριακή Δοκιμή Εύρους ---");
        long startSerialRange = System.nanoTime();
        List<DataRecord> serialRange = TreeQueryExecutor.rangeQuery(recordList, minimums, maximums, tree.getDimensions());
        long stopSerialRange = System.nanoTime();
        System.out.println("Σειριακό Range Query: " + serialRange.size() + " σημεία (χρόνος: " + ((stopSerialRange - startSerialRange) / 1_000_000.0) + " ms)");

        // --- Serial k-NN Query ---
        System.out.println("\n--- Σειριακή k-NN Δοκιμή ---");
        long startSerialKnn = System.nanoTime();
        List<DataRecord> serialKnn = TreeQueryExecutor.kNearestNeighbors(recordList, probe, k, tree.getDimensions());
        long stopSerialKnn = System.nanoTime();
        System.out.println("Σειριακό kNN: " + serialKnn.size() + " σημεία (χρόνος: " + ((stopSerialKnn - startSerialKnn) / 1_000_000.0) + " ms)");

        // --- Serial Skyline ---
        System.out.println("\n--- Σειριακή Skyline Δοκιμή ---");
        long startSerialSky = System.nanoTime();
        List<DataRecord> serialSkyline = TreeQueryExecutor.skyline(recordList, tree.getDimensions());
        long stopSerialSky = System.nanoTime();
        System.out.println("Σειριακό Skyline: " + serialSkyline.size() + " σημεία (χρόνος: " + ((stopSerialSky - startSerialSky)/1_000_000.0) + " ms)");

        // --- IndexFile Lookup ---
        System.out.println("\n--- Αναζήτηση μέσω IndexFile ---");
        Random randomizer = new Random();
        DataRecord chosen = recordList.get(randomizer.nextInt(recordList.size()));
        long chosenId = chosen.getId();
        System.out.println("Επιλεγμένο id για δοκιμή: " + chosenId);

        TreeRecordID foundPointer = IndexStorageHandler.lookupRecordIDById(chosenId);
        if (foundPointer == null) {
            System.out.println("ΔΕΝ βρέθηκε το id στο indexfile!");
        } else {
            System.out.println("Βρέθηκε pointer: blockId=" + foundPointer.getBlockId() + ", slotId=" + foundPointer.getSlotId());
            try {
                DataRecord entry = DataStorageHandler.readRecord(foundPointer);
                if (entry == null) {
                    System.out.println("ΔΕΝ βρέθηκε το record στο datafile!");
                } else {
                    System.out.println("Εγγραφή που διαβάστηκε:\nid=" + entry.getId()
                            + ", name=" + entry.getLabel()
                            + ", lat=" + entry.getLat()
                            + ", lon=" + entry.getLon()
                            + ", uid=" + entry.getUid()
                            + ", changeset=" + entry.getChangeset());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void displayTree(TreeNode node, int depth) {
        String prefix = " ".repeat(depth * 2);
        System.out.println(prefix + (node.isLeaf() ? "Φύλλο" : "Εσωτερικός") +
                " | MBR: " + Arrays.toString(node.getMbr().getMin()) +
                " ως " + Arrays.toString(node.getMbr().getMax()));

        if (!node.isLeaf()) {
            for (TreeNode child : node.getChildren()) {
                displayTree(child, depth + 1);
            }
        }
    }

    public static int countAllPoints(TreeNode node) {
        if (node.isLeaf()) {
            TreeLeafNode leaf = (TreeLeafNode) node;
            return leaf.getPointCount();
        }
        int total = 0;
        for (TreeNode child : node.getChildren()) {
            total += countAllPoints(child);
        }
        return total;
    }
}