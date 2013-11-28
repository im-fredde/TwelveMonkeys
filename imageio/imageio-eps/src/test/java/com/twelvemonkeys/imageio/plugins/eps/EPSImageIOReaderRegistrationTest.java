package com.twelvemonkeys.imageio.plugins.eps;

import org.junit.Test;

import javax.imageio.ImageIO;

import java.util.Arrays;

import static junit.framework.Assert.assertNotNull;

public class EPSImageIOReaderRegistrationTest {
    @Test
    public void epsIMIOReaderShouldBeRegistered() {
        final String[] readerFormatNames = ImageIO.getReaderFormatNames();
        System.out.println("readerFormatNames = " + Arrays.asList(readerFormatNames));
        assertNotNull(readerFormatNames);
    }
}
