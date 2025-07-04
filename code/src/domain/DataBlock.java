package domain;

import java.util.*;

public class DataBlock {
    private List<DataRecord> records = new ArrayList<>();
    private final int blockId;

    public DataBlock(int blockId) {
        this.blockId = blockId;
    }

    // Adds a record to this block
    public void addRecord(DataRecord entry) {
        records.add(entry);
    }

    // Returns all records in this block
    public List<DataRecord> getRecords() {
        return records;
    }

    // Returns the id of this block
    public int getBlockId() {
        return blockId;
    }
}