package com.twelvemonkeys.imageio.plugins.eps;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;
import org.junit.Test;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import static org.junit.Assert.assertEquals;


/**
 * EPSImageReaderTest
 */
public class EPSImageReaderTest extends ImageReaderAbstractTestCase<EPSImageReader>
{

    static ImageReaderSpi provider = new EPSImageReaderSpi();


    protected List<TestData> getTestData()
    {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/eps/NoTiffPreview.eps"), new Dimension(354, 280)),
                new TestData(getClassLoaderResource("/eps/TiffPreview.eps"), new Dimension(354, 280))
        );
    }


    protected ImageReaderSpi createProvider()
    {
        return provider;
    }


    @Override
    protected EPSImageReader createReader()
    {
        return new EPSImageReader(provider);
    }


    protected Class<EPSImageReader> getReaderClass()
    {
        return EPSImageReader.class;
    }


    protected List<String> getFormatNames()
    {
        return Arrays.asList("eps");
    }


    protected List<String> getSuffixes()
    {
        return Arrays.asList("eps");
    }


    protected List<String> getMIMETypes()
    {
        return Arrays.asList("application/eps");
    }


    //ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(data));


    @Test
    public void testNotBadCachingThumbnails() throws IOException {

    }

    @Test
    public void testSetDestination() throws IOException
    {

    }


    @Test
    public void testSetDestinationIllegal() throws IOException
    {

    }
}