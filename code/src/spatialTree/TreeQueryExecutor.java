package spatialTree;

import domain.DataRecord;
import java.util.*;

// Brute-force executor for range, k-NN, and skyline queries on DataRecord lists
public class TreeQueryExecutor {

    // Brute-force skyline (non-dominated records)
    public static List<DataRecord> skyline(List<DataRecord> records, int dimensions) {
        List<DataRecord> skyline = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            DataRecord a = records.get(i);
            boolean dominated = false;
            for (int j = 0; j < records.size(); j++) {
                if (i == j) continue;
                DataRecord b = records.get(j);
                if (dominates(b, a, dimensions)) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) {
                skyline.add(a);
            }
        }
        return skyline;
    }

    // Returns true if a dominates b in the given dimensions
    private static boolean dominates(DataRecord a, DataRecord b, int dimensions) {
        boolean strictlyBetter = false;
        if (dimensions >= 1 && a.getLat() > b.getLat()) return false;
        if (dimensions >= 1 && a.getLat() < b.getLat()) strictlyBetter = true;
        if (dimensions >= 2 && a.getLon() > b.getLon()) return false;
        if (dimensions >= 2 && a.getLon() < b.getLon()) strictlyBetter = true;
        if (dimensions >= 3 && a.getId() > b.getId()) return false;
        if (dimensions >= 3 && a.getId() < b.getId()) strictlyBetter = true;
        if (dimensions >= 4 && a.getUid() > b.getUid()) return false;
        if (dimensions >= 4 && a.getUid() < b.getUid()) strictlyBetter = true;
        if (dimensions >= 5 && a.getChangeset() > b.getChangeset()) return false;
        if (dimensions >= 5 && a.getChangeset() < b.getChangeset()) strictlyBetter = true;
        return strictlyBetter;
    }

    // Brute-force range query in d dimensions (min/max arrays)
    public static List<DataRecord> rangeQuery(List<DataRecord> records, double[] min, double[] max, int dimensions) {
        List<DataRecord> result = new ArrayList<>();
        for (DataRecord r : records) {
            boolean inside = true;
            if (dimensions >= 1 && (r.getLat() < min[0] || r.getLat() > max[0])) inside = false;
            if (dimensions >= 2 && (r.getLon() < min[1] || r.getLon() > max[1])) inside = false;
            if (dimensions >= 3 && (r.getId() < min[2] || r.getId() > max[2])) inside = false;
            if (dimensions >= 4 && (r.getUid() < min[3] || r.getUid() > max[3])) inside = false;
            if (dimensions >= 5 && (r.getChangeset() < min[4] || r.getChangeset() > max[4])) inside = false;
            if (inside) result.add(r);
        }
        return result;
    }

    // Brute-force k nearest neighbors (Euclidean distance)
    public static List<DataRecord> kNearestNeighbors(List<DataRecord> records, double[] queryPoint, int k, int dimensions) {
        List<RecordDist> dists = new ArrayList<>();
        for (DataRecord r : records) {
            double dist = euclideanDistance(r, queryPoint, dimensions);
            dists.add(new RecordDist(r, dist));
        }
        dists.sort(Comparator.comparingDouble(a -> a.dist));
        List<DataRecord> result = new ArrayList<>();
        for (int i = 0; i < Math.min(k, dists.size()); i++) {
            result.add(dists.get(i).record);
        }
        return result;
    }

    // Euclidean distance helper
    private static double euclideanDistance(DataRecord r, double[] q, int dimensions) {
        double sum = 0.0;
        if (dimensions >= 1) sum += Math.pow(r.getLat() - q[0], 2);
        if (dimensions >= 2) sum += Math.pow(r.getLon() - q[1], 2);
        if (dimensions >= 3) sum += Math.pow(r.getId() - q[2], 2);
        if (dimensions >= 4) sum += Math.pow(r.getUid() - q[3], 2);
        if (dimensions >= 5) sum += Math.pow(r.getChangeset() - q[4], 2);
        return Math.sqrt(sum);
    }

    // Helper class for kNN
    private static class RecordDist {
        DataRecord record;
        double dist;
        RecordDist(DataRecord record, double dist) {
            this.record = record;
            this.dist = dist;
        }
    }
}