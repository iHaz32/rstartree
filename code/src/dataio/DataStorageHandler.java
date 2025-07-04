package dataio;

import domain.DataBlock;
import domain.DataRecord;
import spatialTree.TreeRecordID;

import java.io.*;
import java.util.*;

public class DataStorageHandler {
    private static final int BLOCK_BYTES = 32 * 1024;
    private static final String STORAGE_FILE = "data/datafile.bin";

    // Stores a list of DataBlock objects and some metadata in a binary file
    public static void storeBlocks(List<DataBlock> blockList, int entriesCount) throws IOException {
        try (DataOutputStream dout = new DataOutputStream(new FileOutputStream(STORAGE_FILE))) {
            // Write block 0 (metadata)
            ByteArrayOutputStream metaBuf = new ByteArrayOutputStream();
            DataOutputStream metaOut = new DataOutputStream(metaBuf);
            metaOut.writeInt(entriesCount);
            metaOut.writeInt(blockList.size());
            byte[] metaBytes = metaBuf.toByteArray();
            dout.write(metaBytes);
            dout.write(new byte[BLOCK_BYTES - metaBytes.length]); // fill to block size

            // Write the rest of the blocks
            for (DataBlock b : blockList) {
                ByteArrayOutputStream blockBuf = new ByteArrayOutputStream();
                DataOutputStream blockOut = new DataOutputStream(blockBuf);

                blockOut.writeInt(b.getRecords().size());
                for (DataRecord rec : b.getRecords()) {
                    blockOut.writeLong(rec.getId());
                    blockOut.writeUTF(rec.getLabel());
                    blockOut.writeDouble(rec.getLat());
                    blockOut.writeDouble(rec.getLon());
                    blockOut.writeLong(rec.getUid());
                    blockOut.writeLong(rec.getChangeset());
                }
                byte[] blockBytes = blockBuf.toByteArray();
                dout.write(blockBytes);
                if (blockBytes.length < BLOCK_BYTES) {
                    dout.write(new byte[BLOCK_BYTES - blockBytes.length]);
                }
            }
        }
    }

    // Reads a record from the file using a TreeRecordID (block and slot)
    public static DataRecord fetchRecord(TreeRecordID rid) throws IOException {
        int blockIdx = rid.getBlockId();
        int position = rid.getSlotId();

        try (RandomAccessFile raf = new RandomAccessFile(STORAGE_FILE, "r")) {
            // Block 0 is metadata, data starts at block 1
            long offset = (long) blockIdx * BLOCK_BYTES;
            raf.seek(offset);

            int recordCount = raf.readInt();
            if (position >= recordCount) {
                throw new IOException("Slot id is out of block bounds!");
            }

            // Find the record at the requested position
            for (int idx = 0; idx <= position; idx++) {
                long id = raf.readLong();
                String name = raf.readUTF();
                double lat = raf.readDouble();
                double lon = raf.readDouble();
                long uid = raf.readLong();
                long changeset = raf.readLong();
                if (idx == position) {
                    return new DataRecord(id, name, lat, lon, uid, changeset);
                }
            }
        }
        return null;
    }

    // Just calls fetchRecord, same result
    public static DataRecord readRecord(TreeRecordID rid) throws IOException {
        return fetchRecord(rid);
    }
}