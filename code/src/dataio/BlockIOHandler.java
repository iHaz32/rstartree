package dataio;

import domain.DataBlock;
import domain.DataRecord;

import java.util.*;

public class BlockIOHandler {
    private static final int MAX_BLOCK_BYTES = 32 * 1024;

    // Splits records into blocks (max size: MAX_BLOCK_BYTES)
    public static List<DataBlock> assignEntriesToBlocks(List<DataRecord> entryList) {
        List<DataBlock> blockList = new ArrayList<>();
        DataBlock currentBlock = new DataBlock(1);
        int bytesUsed = 0;

        for (DataRecord entry : entryList) {
            // Add 4 bytes if starting a new block
            int extraBytes = bytesUsed == 0 ? 4 : 0;
            if (bytesUsed + extraBytes + entry.getSize() > MAX_BLOCK_BYTES) {
                blockList.add(currentBlock);
                currentBlock = new DataBlock(blockList.size() + 1);
                bytesUsed = 0;
            }
            if (bytesUsed == 0) bytesUsed += 4; // Overhead for numRecords
            currentBlock.addRecord(entry);
            bytesUsed += entry.getSize();
        }

        if (!currentBlock.getRecords().isEmpty()) {
            blockList.add(currentBlock);
        }

        // Debug print for blocks and record ids
        /*
        for (DataBlock block : blockList) {
            System.out.print("BlockId: " + block.getBlockId() + ", entries: " + block.getRecords().size() + ", ids: ");
            for (DataRecord r : block.getRecords()) {
                System.out.print(r.getId() + " ");
            }
            System.out.println();
        }
        */
        return blockList;
    }
}