package dataio;

import domain.DataBlock;
import domain.DataRecord;
import spatialTree.TreeRecordID;

import java.io.*;
import java.util.List;

public class IndexStorageHandler {
    private static final String INDEX_BIN = "data/indexfile.bin";

    // Writes index info for all records to a binary file
    public static void exportIndex(List<DataBlock> blockList) throws IOException {
        File dataFolder = new File("data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(INDEX_BIN))) {
            for (DataBlock blk : blockList) {
                int slotCounter = 0;
                for (DataRecord rec : blk.getRecords()) {
                    output.writeLong(rec.getId());
                    output.writeUTF(rec.getLabel());
                    output.writeDouble(rec.getLat());
                    output.writeDouble(rec.getLon());
                    output.writeLong(rec.getUid());
                    output.writeLong(rec.getChangeset());
                    output.writeInt(blk.getBlockId());
                    output.writeInt(slotCounter);
                    slotCounter++;
                }
            }
        }
    }

    // Finds and returns the TreeRecordID for a record id (searches the index file)
    public static TreeRecordID findRecordLocationById(long searchId) throws IOException {
        File idxFile = new File(INDEX_BIN);
        if (!idxFile.exists()) {
            System.out.println("⚠ indexfile.bin has not been created yet.");
            return null;
        }
        try (DataInputStream inp = new DataInputStream(new FileInputStream(INDEX_BIN))) {
            while (inp.available() > 0) {
                long foundId = inp.readLong();
                String name = inp.readUTF();
                double lat = inp.readDouble();
                double lon = inp.readDouble();
                long uid = inp.readLong();
                long changeset = inp.readLong();
                int blockId = inp.readInt();
                int slotId = inp.readInt();
                if (foundId == searchId) {
                    return new TreeRecordID(blockId, slotId);
                }
            }
        }
        return null;
    }

    // Alias for findRecordLocationById
    public static TreeRecordID lookupRecordIDById(long searchId) throws IOException {
        return findRecordLocationById(searchId);
    }

    // Removes a record from the index file by id
    public static void removeFromIndex(long removeId) throws IOException {
        File srcFile = new File(INDEX_BIN);
        File tmpFile = new File("data/indexfile_temp.bin");

        try (DataInputStream inp = new DataInputStream(new FileInputStream(srcFile));
             DataOutputStream outp = new DataOutputStream(new FileOutputStream(tmpFile))) {

            while (inp.available() > 0) {
                long currId = inp.readLong();
                String name = inp.readUTF();
                double lat = inp.readDouble();
                double lon = inp.readDouble();
                long uid = inp.readLong();
                long changeset = inp.readLong();
                int blockId = inp.readInt();
                int slotId = inp.readInt();

                // Write record to temp file only if id is not the one to remove
                if (currId != removeId) {
                    outp.writeLong(currId);
                    outp.writeUTF(name);
                    outp.writeDouble(lat);
                    outp.writeDouble(lon);
                    outp.writeLong(uid);
                    outp.writeLong(changeset);
                    outp.writeInt(blockId);
                    outp.writeInt(slotId);
                }
            }
        }

        // Replace old index with updated temp file
        if (!srcFile.delete()) {
            throw new IOException("Could not delete old indexfile.");
        }
        if (!tmpFile.renameTo(srcFile)) {
            throw new IOException("Could not rename temp indexfile.");
        }
    }

    // Alias for removeFromIndex
    public static void deleteFromIndex(long removeId) throws IOException {
        removeFromIndex(removeId);
    }
}