package com.admiralbot.networksecurity.firewall;

import java.security.Key;
import java.util.List;

public class DecryptedPrefixList {

    private String id;
    private long version;
    private Key dataKey;
    private List<DecryptedPrefixListEntry> entries;

    public DecryptedPrefixList(String id, long version, Key dataKey, List<DecryptedPrefixListEntry> entries) {
        this.id = id;
        this.version = version;
        this.dataKey = dataKey;
        this.entries = entries;
    }

    public String getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public Key getDataKey() {
        return dataKey;
    }

    public List<DecryptedPrefixListEntry> getEntries() {
        return entries;
    }
}
