package com.twelvemonkeys.imageio.plugins.eps;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;


/**
 * EPSImageReaderSpi
 *
 * @author <a href="mailto:fredrik@teamleader.se">Fredrik Gusatfsson</a>
 */
public class EPSImageReaderSpi extends ImageReaderSpi
{

    /**
     * Creates a {@code EPSImageReaderSpi}.
     */
    public EPSImageReaderSpi()
    {
        this(IIOUtil.getProviderInfo(EPSImageReaderSpi.class));
    }


    private EPSImageReaderSpi(final ProviderInfo providerInfo)
    {
        super(
                providerInfo.getVendorName(),
                providerInfo.getVersion(),
                new String[]{"eps", "EPS", "ps", "PS", "ai", "AI"},
                new String[]{"eps","ps","ai"},
                new String[]{
                        "application/eps", "application/postscript"
                },
                "com.twelvemkonkeys.imageio.plugins.eps.EPSImageReader",
                new Class[]{ImageInputStream.class},
//                new String[]{"com.twelvemkonkeys.imageio.plugins.eps.EPSImageWriterSpi"},
                null,
                true, // supports standard stream metadata
                null, null, // native stream format name and class
                null, null, // extra stream formats
                true, // supports standard image metadata
                null, null,
                null, null // extra image metadata formats
        );
    }



    public boolean canDecodeInput(final Object pSource) throws IOException
    {
        if (!(pSource instanceof ImageInputStream)) {
            return false;
        }

        return true;
    }


    public ImageReader createReaderInstance(final Object pExtension) throws IOException
    {
        return new EPSImageReader(this);
    }


    public String getDescription(final Locale pLocale)
    {
        return "Encapsulated PostScript (EPS) image reader";
    }
}
