package domain;

import java.nio.charset.StandardCharsets;

public class DataRecord {
    private long id;
    private String label;
    private double lat;
    private double lon;
    private long uid;
    private long changeset;

    public DataRecord(long id, String label, double lat, double lon, long uid, long changeset) {
        this.id = id;
        this.label = label;
        this.lat = lat;
        this.lon = lon;
        this.uid = uid;
        this.changeset = changeset;
    }

    // Getters for the record fields
    public long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public long getUid() {
        return uid;
    }

    public long getChangeset() {
        return changeset;
    }

    // Returns the estimated size of this record in bytes
    public int getSize() {
        int sum = 8 + 8 + 8 + 8 + 8; // id, lat, lon, uid, changeset
        byte[] labelBytes = label == null ? new byte[0] : label.getBytes(StandardCharsets.UTF_8);
        sum += 2 + labelBytes.length; // UTF-8 prefix + bytes
        return sum;
    }
}