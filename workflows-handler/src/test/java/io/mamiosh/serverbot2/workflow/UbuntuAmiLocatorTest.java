package io.mamiosh.serverbot2.workflow;

import io.mamish.serverbot2.workflows.UbuntuAmiLocator;
import org.junit.jupiter.api.Test;

public class UbuntuAmiLocatorTest {

    @Test
    void testLocateAmi() {
        UbuntuAmiLocator locator = new UbuntuAmiLocator();
        locator.getIdealAmi();
    }

}
