package io.mamish.serverbot2.workflows;

import org.junit.jupiter.api.Test;

public class UbuntuAmiLocatorTest {

    @Test
    void testLocateAmi() {
        UbuntuAmiLocator locator = new UbuntuAmiLocator();
        locator.getIdealAmi();
    }

}
