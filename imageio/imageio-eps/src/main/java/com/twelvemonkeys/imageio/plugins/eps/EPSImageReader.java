package com.twelvemonkeys.imageio.plugins.eps;

import com.twelvemonkeys.imageio.ImageReaderBase;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;


/**
 * ImageReader for Encapsulated PostScript (EPS) format.
 *
 * @author <a href="mailto:fredrik@teamleader.se">Fredrik Gustafsson</a>
 */
public class EPSImageReader extends ImageReaderBase
{
    final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.eps.debug"));

    protected EPSImageReader(final ImageReaderSpi originatingProvider)
    {
        super(originatingProvider);
    }


    protected void resetMembers()
    {
    }


    public int getWidth(final int imageIndex) throws IOException
    {
        return 0;
    }


    public int getHeight(final int imageIndex) throws IOException
    {
        return 0;
    }


    @Override
    public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException
    {
        return null;
    }


    public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException
    {
        return null;
    }


    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException
    {
        return null;
    }


    @Override
    public int getNumImages(boolean allowSearch) throws IOException
    {
        return 0;
    }

    // TODO: For now, leave as Metadata

    /*

    // ?
    Point getOffset(int pImageIndex) throws IOException;
    // Return 0, 0 for index 0, otherwise use layer offset

     */

    /// Metadata support
    // TODO


    @Override
    public IIOMetadata getStreamMetadata() throws IOException
    {
        // null might be appropriate here
        // "For image formats that contain a single image, only image metadata is used."
        return super.getStreamMetadata();
    }


    @Override
    public IIOMetadata getImageMetadata(final int imageIndex) throws IOException
    {
        return null;
    }


    @Override
    public IIOMetadata getImageMetadata(final int imageIndex, final String formatName, final Set<String> nodeNames) throws IOException
    {
        // TODO: It might make sense to overload this, as there's loads of meta data in the file
        return super.getImageMetadata(imageIndex, formatName, nodeNames);
    }


    /// Thumbnail support
    @Override
    public boolean readerSupportsThumbnails()
    {
        return true;
    }


    @Override
    public int getNumThumbnails(final int imageIndex) throws IOException
    {
        return 0;
    }


    @Override
    public int getThumbnailWidth(final int imageIndex, final int thumbnailIndex) throws IOException
    {
        return 0;
    }


    @Override
    public int getThumbnailHeight(final int imageIndex, final int thumbnailIndex) throws IOException
    {
        return 0;
    }


    @Override
    public BufferedImage readThumbnail(final int imageIndex, final int thumbnailIndex) throws IOException
    {
        return null;
    }


    /// Functional testing
    public static void main(final String[] pArgs) throws IOException
    {
    }
}
