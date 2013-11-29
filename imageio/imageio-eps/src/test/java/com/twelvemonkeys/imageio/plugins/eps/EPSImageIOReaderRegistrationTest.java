package com.twelvemonkeys.imageio.plugins.eps;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;


public class EPSImageIOReaderRegistrationTest {

    @Test
    public void epsIMIOReaderShouldBeRegistered() {

        final String[] readerFormatNames = ImageIO.getReaderFormatNames();

        List<String> readers = Arrays.asList(readerFormatNames);

        System.out.println("readerFormatNames = " + readers);

        assertNotNull(readerFormatNames);

        assertTrue(readers.contains("EPS"));
    }
}
