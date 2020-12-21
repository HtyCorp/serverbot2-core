package com.admiralbot.workflows;

import org.junit.jupiter.api.Test;

public class UbuntuAmiLocatorTest {

    @Test
    void testLocateAmi() {
        UbuntuAmiLocator locator = new UbuntuAmiLocator();
        locator.getIdealAmi();
    }

}
