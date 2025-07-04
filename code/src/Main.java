import dataio.BlockIOHandler;
import dataio.DataStorageHandler;
import dataio.IndexStorageHandler;
import domain.DataBlock;
import domain.DataRecord;
import maps.MapParser;
import testing.SpatialTreeTester;
import spatialTree.*;

import java.io.IOException;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {

        Scanner inputScanner = new Scanner(System.in);
        System.out.println("Να δημιουργηθεί η δομή με μαζική εισαγωγή (bulk loading); (ναι / όχι)");
        String bulkChoice = inputScanner.nextLine().trim().toLowerCase();
        boolean enableBulkMode = bulkChoice.equals("ναι") || bulkChoice.equals("yes");

        List<DataRecord> importedData = MapParser.parseOSM("data/map.osm");
        System.out.println("➡ Αριθμός δεδομένων που διαβάστηκαν: " + importedData.size());

        System.out.print("\nΟρίστε το μέγιστο αριθμό στοιχείων ανά κόμβο: ");
        int maxNodeCapacity = Integer.parseInt(inputScanner.nextLine());

        System.out.print("Ορίστε το πλήθος των διαστάσεων: ");
        int numDimensions = Integer.parseInt(inputScanner.nextLine());

        // Δημιουργία λιστών με σημεία και IDs για το δέντρο
        List<double[]> pointsList = new ArrayList<>();
        List<TreeRecordID> recordIds = new ArrayList<>();
        int currentBlock = 1;
        int currentSlot = 0;

        for (DataRecord rec : importedData) {
            double[] coords = new double[numDimensions];
            if (numDimensions >= 1) coords[0] = rec.getLat();
            if (numDimensions >= 2) coords[1] = rec.getLon();
            if (numDimensions >= 3) coords[2] = rec.getId();
            if (numDimensions >= 4) coords[3] = rec.getUid();
            if (numDimensions >= 5) coords[4] = rec.getChangeset();

            pointsList.add(coords);
            recordIds.add(new TreeRecordID(currentBlock, currentSlot));

            currentSlot++;
            if (currentSlot == 10) {
                currentSlot = 0;
                currentBlock++;
            }
        }

        RStarTree indexTree;
        long timerStart = System.nanoTime();

        if (enableBulkMode) {
            indexTree = TreeBulkLoader.bulkLoad(pointsList, recordIds, maxNodeCapacity, numDimensions);
            System.out.println("\nΗ δομή κατασκευάστηκε με ΜΑΖΙΚΗ ΔΗΜΙΟΥΡΓΙΑ (bulk loading).");
        } else {
            indexTree = new RStarTree(maxNodeCapacity, numDimensions);
            for (int i = 0; i < pointsList.size(); i++) {
                indexTree.insert(pointsList.get(i), recordIds.get(i));
            }
            System.out.println("\nΗ δομή κατασκευάστηκε με ΚΛΑΣΙΚΗ ΕΙΣΑΓΩΓΗ (insertion).");
        }

        long timerEnd = System.nanoTime();
        System.out.printf("Χρόνος δημιουργίας δομής: %.2f ms%n\n", (timerEnd - timerStart) / 1_000_000.0);

        System.out.println("Στατιστικά αριθμού εγγραφών ανά block:");
        List<DataBlock> allBlocks = BlockIOHandler.assignEntriesToBlocks(importedData);

        int sumRecords = 0;
        for (DataBlock blk : allBlocks) {
            int recCount = blk.getRecords().size();
            System.out.println("DataBlock ID: " + blk.getBlockId() + " -> Entries: " + recCount);
            sumRecords += recCount;
        }

        System.out.println("-------------------------");
        System.out.println("Σύνολο χρησιμοποιημένων blocks: " + allBlocks.size());
        System.out.println("Συνολικές εγγραφές αποθηκεύτηκαν: " + sumRecords + "\n");

        try {
            DataStorageHandler.storeBlocks(allBlocks, sumRecords);
            IndexStorageHandler.exportIndex(allBlocks);
            System.out.println("Τα αρχεία δεδομένων και ευρετηρίου δημιουργήθηκαν επιτυχώς.\n");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SpatialTreeTester.runAllTests(indexTree, importedData);
    }
}