package com.admiralbot.workflows;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UbuntuAmiInfo {

    private static final Pattern AMI_REGEX = Pattern.compile("ami-[a-z0-9]+");

    private final String region;
    private final String name;
    private final String version;
    private final String arch;
    private final String type;
    private final String release;
    private final String amiId;

    public UbuntuAmiInfo(String[] table) {
        region = table[0];
        name = table[1];
        version = table[2];
        arch = table[3];
        type = table[4];
        release = table[5];

        Matcher amiIdMatcher = AMI_REGEX.matcher(table[6]);
        amiIdMatcher.find();
        amiId = amiIdMatcher.group();
    }

    public String getRegion() {
        return region;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getArch() {
        return arch;
    }

    public String getType() {
        return type;
    }

    public String getRelease() {
        return release;
    }

    public String getAmiId() {
        return amiId;
    }
}
