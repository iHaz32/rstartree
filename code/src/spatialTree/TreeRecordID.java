package spatialTree;

// Connects the R*-Tree index to the data file (block/slot)
public class TreeRecordID {

    private int blockId; // which block (e.g., 2nd block)
    private int slotId;  // position inside the block

    public TreeRecordID(int blockId, int slotId) {
        this.blockId = blockId;
        this.slotId = slotId;
    }

    public int getBlockId() {
        return blockId;
    }

    public int getSlotId() {
        return slotId;
    }

    @Override
    public String toString() {
        return "TreeRecordID(" + blockId + ", " + slotId + ")";
    }
}