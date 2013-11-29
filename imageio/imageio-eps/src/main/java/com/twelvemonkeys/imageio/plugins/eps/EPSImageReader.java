package com.twelvemonkeys.imageio.plugins.eps;

import com.twelvemonkeys.imageio.ImageReaderBase;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;


/**
 * ImageReader for Encapsulated PostScript (EPS) format.
 */
public class EPSImageReader extends ImageReaderBase
{
    final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.eps.debug"));

    int m_nWidth = -1, m_nHeight = -1;
    int m_nMode;
    // Constants enumerating the values of colorType
    boolean gotHeader = false;
    private EPSMetadata metadata;
    private int m_nPaddingBandsCount = -1;
    private int m_nDataType = -1;


    public static final int MODE_BITMAP = 1;
    public static final int MODE_LAB = 2;
    public static final int MODE_RGB = 3;
    public static final int MODE_CMYK = 4;

    public static final int DATATYPE_BINARY = 1;
    public static final int DATATYPE_ASCII85_JPEG_CMYK = 5;
    public static final int DATATYPE_ASCII85_JPEG_RGB = 6;

    public static final byte[] EPS_TIFF_HEADER = {(byte) 0xc5, (byte) 0xd0, (byte) 0xd3, (byte) 0xc6};


    private long m_lImageLength;
    private ImageTypeSpecifier ussSheetImageType;


    protected EPSImageReader(final ImageReaderSpi originatingProvider)
    {
        super(originatingProvider);
    }


    protected void resetMembers()
    {
    }


    public int getWidth(final int imageIndex) throws IOException
    {
        if (imageInput == null) {
            throw new IllegalStateException("No input stream");
        }

        BufferedImage first = read(0);

        return first.getWidth();
    }


    public int getHeight(final int imageIndex) throws IOException
    {
        if (imageInput == null) {
            throw new IllegalStateException("No input stream");
        }

        BufferedImage first = read(0);

        return first.getHeight();
    }


    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException
    {
        checkIndex(imageIndex);
        readHeader();
        ImageTypeSpecifier imageType;
        int datatype = DataBuffer.TYPE_BYTE;
        java.util.List<ImageTypeSpecifier> l = new ArrayList<ImageTypeSpecifier>();

        ColorSpace cs;
        int[] bandOffsets;

        // @TODO : Check if the file has an embedded color profile
        switch (m_nMode) {
            case MODE_BITMAP:
                cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                bandOffsets = new int[1];
                bandOffsets[0] = 0;
                imageType = ImageTypeSpecifier.createInterleaved(cs, bandOffsets, datatype, false, false);
                break;
            case MODE_RGB:
                cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                bandOffsets = new int[3];
                bandOffsets[0] = 0;
                bandOffsets[1] = 1;
                bandOffsets[2] = 2;
                imageType = ImageTypeSpecifier.createInterleaved(cs, bandOffsets, datatype, false, false);
                break;
            case MODE_CMYK:
                // Use the standard Adobe Photoshop ICC

                bandOffsets = new int[4];
                bandOffsets[0] = 0;
                bandOffsets[1] = 1;
                bandOffsets[2] = 2;
                bandOffsets[3] = 3;

                if (ussSheetImageType == null) {
                    InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("USSheetfedCoated.icc");

                    ICC_Profile ip = ICC_Profile.getInstance(resourceAsStream);
                    cs = new ICC_ColorSpace(ip);

                    ussSheetImageType = ImageTypeSpecifier.createInterleaved(cs, bandOffsets, datatype, false, false);
                }

                imageType = ussSheetImageType;

                break;
            default:
                throw new IIOException("Can't read mode " + m_nMode);
        }

        l.add(imageType);

        return l.iterator();
    }


    private void checkIndex(int imageIndex)
    {
        if (imageIndex != 0) {
            throw new IndexOutOfBoundsException("bad index");
        }
    }


    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException
    {
        checkBounds(imageIndex);

        readMetadata();

        imageInput.seek(0l);

        // Enure band settings from param are compatible with images
        int nInputBands = 0;

        switch (m_nMode) {
            case MODE_BITMAP:
                nInputBands = 1;
                break;
            case MODE_LAB:
                break;
            case MODE_CMYK:
                nInputBands = 4;
                break;
            case MODE_RGB:
                nInputBands = 3;
                break;
        }

        // Compute initial source region, clip against destination later
        Rectangle sourceRegion = getSourceRegion(param, m_nWidth, m_nHeight);

        // Set everything to default values
        int sourceXSubsampling = 1;
        int sourceYSubsampling = 1;
        int[] aSourceBands = null;
        int[] aResultBands = null;
        Point destinationOffset = new Point(0, 0);

        // Get values from the ImageReadParam, if any
        if (param != null) {
            sourceXSubsampling = param.getSourceXSubsampling();
            sourceYSubsampling = param.getSourceYSubsampling();
            aSourceBands = param.getSourceBands();
            aResultBands = param.getDestinationBands();
            destinationOffset = param.getDestinationOffset();
        }

        // Get the specified detination image or create a new one
        BufferedImage dst = getDestination(param, getImageTypes(0), m_nWidth, m_nHeight);

        checkReadParamBandSettings(param, nInputBands, dst.getSampleModel().getNumBands());

        int[] bankIndices = new int[nInputBands];
        int[] bandOffsets = new int[nInputBands];
        for (int i = 0; i < nInputBands; i++) {
            bandOffsets[i] = i * m_nWidth;
            bankIndices[i] = 0;
        }

        int bytesPerRow = m_nWidth * (nInputBands + m_nPaddingBandsCount);
        DataBufferByte rowDB = new DataBufferByte(bytesPerRow);
        WritableRaster oRasterRow = Raster.createBandedRaster(rowDB, m_nWidth, 1, m_nWidth, bankIndices, bandOffsets, new Point(0, 0));

        byte[] aRowBuffer = rowDB.getData();

        // Create an array that can handle a single pixel
        int[] pixel = oRasterRow.getPixel(0, 0, (int[]) null);

        WritableRaster oRasterImage = dst.getWritableTile(0, 0);
        int dstMinX = oRasterImage.getMinX();
        int dstMaxX = dstMinX + oRasterImage.getWidth();
        int dstMinY = oRasterImage.getMinY();
        int dstMaxY = dstMinY + oRasterImage.getHeight();

        // Create a child raster exposing only the desired source bands
        if (aSourceBands != null) {
            oRasterRow = oRasterRow.createWritableChild(0, 0, m_nWidth, 1, 0, 0, aSourceBands);
        }

        // Create a child raster exposing only the desired dest bands
        if (aResultBands != null) {
            oRasterImage = oRasterImage.createWritableChild(0, 0, oRasterImage.getWidth(), oRasterImage.getHeight(), 0, 0, aResultBands);
        }

        //
        processImageStarted(imageIndex);

        ByteBuffer oBuffer;
        byte[] aData;

        switch (m_nDataType) {
            case DATATYPE_BINARY:
                for (int nSourceY = 0; nSourceY < m_nHeight; nSourceY++) {
                    // Read the row
                    try {

                        imageInput.readFully(aRowBuffer);

                        // Skip paddingbands
                        //if(paddingBands > 0)
                        //{
                        //	m_oDataStream.skipBytes(m_nWidth * paddingBands);
                        //}
                    }
                    catch (IOException e) {
                        throw new IIOException("Error reading line " + nSourceY, e);
                    }

                    // Reject rows that lie outside the source region,
                    // or which aren’t part of the subsampling
                    if ((nSourceY < sourceRegion.y) || (nSourceY >= sourceRegion.y + sourceRegion.height) || (((nSourceY - sourceRegion.y) % sourceYSubsampling) != 0)) {
                        continue;
                    }

                    // Determine where the row will go in the destination
                    int dstY = destinationOffset.y + (nSourceY - sourceRegion.y) / sourceYSubsampling;

                    if (dstY < dstMinY) {
                        continue; // The row is above imRas
                    }

                    if (dstY > dstMaxY) {
                        break; // We’re done with the image
                    }

                    // Copy each (subsampled) source pixel into imRas
                    for (int srcX = sourceRegion.x; srcX < sourceRegion.x + sourceRegion.width; srcX++) {
                        if (((srcX - sourceRegion.x) % sourceXSubsampling) != 0) {
                            continue;
                        }

                        int dstX = destinationOffset.x + (srcX - sourceRegion.x) / sourceXSubsampling;
                        if (dstX < dstMinX) {
                            continue; // The pixel is to the left of imRas
                        }

                        if (dstX > dstMaxX) {
                            break; // We’re done with the row
                        }

                        // Copy the pixel, sub-banding is done automatically
                        oRasterRow.getPixel(srcX, 0, pixel);
                        oRasterImage.setPixel(dstX, dstY, pixel);
                    }
                }
                break;
            case DATATYPE_ASCII85_JPEG_CMYK:
            case DATATYPE_ASCII85_JPEG_RGB:
                aData = new byte[(int) m_lImageLength];
                imageInput.read(aData, 0, (int) m_lImageLength);
                oBuffer = ByteBuffer.wrap(aData);
                oBuffer = ASCII85Decoder.decode(oBuffer);

                dst = ImageIO.read(new ByteArrayInputStream(oBuffer.array()));
                break;
            default:
                throw new IIOException("Data type " + m_nDataType + " not supported");
        }

        processImageProgress(100f);

        if (abortRequested()) {
            processReadAborted();
        }
        else {
            processImageComplete();
        }

        return dst;
    }


    public void readMetadata() throws IIOException
    {
        if (metadata != null) {
            return;
        }
        readHeader();

        this.metadata = new EPSMetadata();


    }


    public void readHeader() throws IIOException
    {
        byte[] aStart = new byte[4];
        String sLine;
        String[] aParameters;
        int nParameter;
        String sDataStart = "";

        if (gotHeader) {
            return;
        }

        m_nWidth = 0;
        m_nHeight = 0;
        m_nMode = 0;

        gotHeader = true;

        if (imageInput == null) {
            throw new IllegalStateException("No input stream");
        }

        String sSignature;
        try {
            imageInput.read(aStart);
            while (aStart[0] != '%' || aStart[1] != '!' || aStart[2] != 'P' || aStart[3] != 'S') {
                aStart[0] = aStart[1];
                aStart[1] = aStart[2];
                aStart[2] = aStart[3];
                aStart[3] = imageInput.readByte();
            }
            sSignature = "%!PS" + imageInput.readLine();
        }
        catch (IOException e) {
            throw new IIOException("Error reading signature", e);
        }

        // Test the signature

        //%!PS-Adobe-3.0 EPSF-3.0 %!PS-Adobe-3.1 EPSF-3.0
        if (sSignature.matches("%!PS-Adobe-3\\.[0-1] EPSF-3\\.[0-1]\\n")) {
            throw new IIOException("Bad file signature: " + sSignature);
        }

		/*
        %!PS-Adobe-3.0 EPSF-3.0
		%%Creator: Adobe Photoshop Version 10.0x20070321 [20070321.m.1480 2007/03/21:16:39:00 cutoff; m branch]
		%%Title: 235-821.eps
		%%CreationDate: 9/16/08 1:43 PM
		%%BoundingBox: 0 0 360 360
		%%HiResBoundingBox: 0 0 360 360
		%%SuppressDotGainCompensation
		%%DocumentProcessColors: Cyan Magenta Yellow Black
		 */
        try {
            while ((sLine = imageInput.readLine()) != null) {
                // Find the image data
                //%ImageData: <columns> <rows> <depth> <mode> <pad channels> <block size> <binary/hex> "<data start>"
                if (sLine.startsWith("%ImageData:")) {
                    aParameters = sLine.split(" ");
                    nParameter = 1;

                    m_nWidth = Integer.valueOf(aParameters[nParameter++]);
                    m_nHeight = Integer.valueOf(aParameters[nParameter++]);
                    int nDepth = Integer.valueOf(aParameters[nParameter++]);
                    m_nMode = Integer.valueOf(aParameters[nParameter++]);
                    m_nPaddingBandsCount = Integer.valueOf(aParameters[nParameter++]);
                    int nBlockSize = Integer.valueOf(aParameters[nParameter++]);
                    m_nDataType = Integer.valueOf(aParameters[nParameter++]);
                    sDataStart = aParameters[nParameter];
                    sDataStart = sDataStart.substring(1, sDataStart.length() - 1);
                }
                else if (sLine.startsWith("%%BeginBinary:")) {
                    String p_sSource = sLine.substring(sLine.indexOf(':') + 1).replaceAll("^\\s+", "");
                    m_lImageLength = Long.parseLong(p_sSource);
                }
                else if (sLine.equalsIgnoreCase(sDataStart)) {
                    // We got the right position for image data
                    break;
                }
            }
        }
        catch (Exception e) {
            throw new IIOException("Error reading header", e);
        }

        switch (m_nDataType) {
            case DATATYPE_BINARY:
                break;
            case DATATYPE_ASCII85_JPEG_CMYK:
                break;
            case DATATYPE_ASCII85_JPEG_RGB:
                break;
            default:
                throw new IIOException("Data type " + m_nDataType + " not supported");
        }

        // Read width, height, color type, newline
        if (m_nWidth == 0 || m_nHeight == 0 || m_nMode == 0) {
            throw new IIOException("Error reading header");
        }
    }


    @Override
    public int getNumImages(boolean allowSearch) throws IOException
    {
        if (getInput() == null) {
            throw new IllegalStateException("Got no input");
        }

        return 1;
    }


    @Override
    public IIOMetadata getStreamMetadata() throws IOException
    {
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
        return checkTiffHeader() ? 1 : 0;
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
        byte[] tiffData = getTiffData();
        if (tiffData == null) {
            return null;
        }


        return ImageIO.read(ImageIO.createImageInputStream(new ByteArrayInputStream(tiffData)));

    }


    private boolean checkTiffHeader() throws IOException
    {
        boolean check = true;

        byte[] headerId = new byte[4];

        imageInput.seek(0L);
        imageInput.read(headerId);

        for (int i = 0; i < headerId.length; i++) {
            if (headerId[i] != EPS_TIFF_HEADER[i]) {
                check = false;
            }
        }

        return check;
    }


    private byte[] getTiffData() throws IOException
    {
        byte[] data = null;

        if (checkTiffHeader()) {

            int offset = getTiffOffset();
            int length = getTiffLength();

            imageInput.seek(offset);

            data = new byte[length];

            imageInput.read(data);
        }

        return data;
    }


    private int getTiffOffset() throws IOException
    {
        imageInput.seek(20);

        int offset = imageInput.readInt();

        return msbShift(offset);

    }


    private int getTiffLength() throws IOException
    {

        imageInput.seek(24L);

        int length = imageInput.readInt();

        return msbShift(length);
    }


    private int msbShift(int value)
    {
        // MSB TO LSB
        int a, b, c, d;

        a = (value & 0xff000000) >> 24;
        b = (value & 0x00ff0000) >> 16;
        c = (value & 0x0000ff00) >> 8;
        d = (value & 0x000000ff);


        return (d & 0xff) << 24 | (c & 0xff) << 16 | (b & 0xff) << 8 | a & 0xff;

    }


    private static class ASCII85Decoder
    {
        private ByteBuffer m_oBuffer;


        /**
         * initialize the decoder with byte buffer in ASCII85 format
         */
        private ASCII85Decoder(ByteBuffer buf)
        {
            m_oBuffer = buf;
        }


        /**
         * get the next character from the input.
         * @return the next character, or -1 if at end of stream
         */
        private int nextChar()
        {
            // skip whitespace
            // returns next character, or -1 if end of stream
            while (m_oBuffer.remaining() > 0) {
                char c = (char) m_oBuffer.get();

                if (!Character.isWhitespace(c)) {
                    return c;
                }
            }

            // EOF reached
            return -1;
        }


        /**
         * decode the next five ASCII85 characters into up to four decoded
         * bytes.  Return false when finished, or true otherwise.
         *
         * @param baos the ByteArrayOutputStream to write output to, set to the
         *        correct position
         * @return false when finished, or true otherwise.
         */
        private boolean decode5(ByteArrayOutputStream baos) throws IIOException
        {
            // stream ends in ~>
            int[] five = new int[5];
            int i;

            for (i = 0; i < 5; i++) {
                five[i] = nextChar();

                if (five[i] == '~') {
                    if (nextChar() == '>') {
                        break;
                    }
                    else {
                        throw new IIOException("Bad character in ASCII85Decode: not ~>");
                    }
                }
                else if (five[i] >= '!' && five[i] <= 'u') {
                    five[i] -= '!';
                }
                else if (five[i] == 'z') {
                    if (i == 0) {
                        five[i] = 0;
                        i = 4;
                    }
                    else {
                        throw new IIOException("Inappropriate 'z' in ASCII85Decode");
                    }
                }
                else {
                    throw new IIOException("Bad character in ASCII85Decode: " + five[i] + " (" + (char) five[i] + ")");
                }
            }

            if (i > 0) {
                i -= 1;
            }

            int value = five[0] * 85 * 85 * 85 * 85 + five[1] * 85 * 85 * 85 + five[2] * 85 * 85 + five[3] * 85 + five[4];

            for (int j = 0; j < i; j++) {
                int shift = 8 * (3 - j);
                baos.write((byte) ((value >> shift) & 0xff));
            }

            return (i == 4);
        }


        /**
         * decode the bytes
         * @return the decoded bytes
         */
        private ByteBuffer decode() throws IIOException
        {
            // start from the beginning of the data
            m_oBuffer.rewind();
            // allocate the output buffer
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // decode the bytes

            while (decode5(baos)) {

            }

            return ByteBuffer.wrap(baos.toByteArray());
        }


        /**
         * decode an array of bytes in ASCII85 format.
         * <p>
         * In ASCII85 format, every 5 characters represents 4 decoded
         * bytes in base 85.  The entire stream can contain whitespace,
         * and ends in the characters '~&gt;'.
         *
         * @param buf the encoded ASCII85 characters in a byte buffer
         * @return the decoded bytes
         */
        public static ByteBuffer decode(ByteBuffer buf) throws IIOException
        {
            ASCII85Decoder me = new ASCII85Decoder(buf);

            return me.decode();
        }
    }
}
