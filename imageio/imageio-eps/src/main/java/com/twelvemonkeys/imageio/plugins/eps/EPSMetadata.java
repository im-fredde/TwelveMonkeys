package com.twelvemonkeys.imageio.plugins.eps;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class EPSMetadata extends IIOMetadata
{
    static final boolean standardMetadataFormatSupported = false;
    static final String nativeMetadataFormatName = "javax_imageio_eps_image_1.0";
    static final String nativeMetadataFormatClassName = "com.twelvemonkeys.imageio.plugins.eps.EPSMetaData";
    static final String[] extraMetadataFormatNames = null;
    static final String[] extraMetadataFormatClassNames = null;

    // Keyword/value pairs
    List keywords = new ArrayList();
    List values = new ArrayList();

    public EPSMetadata()
    {
        super(standardMetadataFormatSupported,
              nativeMetadataFormatName,
              nativeMetadataFormatClassName,
              extraMetadataFormatNames,
              extraMetadataFormatClassNames);
    }

    @Override
    public IIOMetadataFormat getMetadataFormat(String formatName)
    {
        if(!formatName.equals(nativeMetadataFormatName))
        {
            throw new IllegalArgumentException("Bad format name!");
        }
        return EPSMetaDataFormat.getDefaultInstance();
    }

    @Override
    public Node getAsTree(String formatName)
    {
        if(!formatName.equals(nativeMetadataFormatName))
        {
            throw new IllegalArgumentException("Bad format name!");
        }

        // Create a root node
        IIOMetadataNode root = new IIOMetadataNode(nativeMetadataFormatName);

        // Add a child to the root node for each keyword/value pair
        Iterator keywordIter = keywords.iterator();
        Iterator valueIter = values.iterator();
        while(keywordIter.hasNext())
        {
            IIOMetadataNode node = new IIOMetadataNode("KeywordValuePair");
            node.setAttribute("keyword", (String)keywordIter.next());
            node.setAttribute("value", (String)valueIter.next());
            root.appendChild(node);
        }

        return root;
    }

    @Override
    public boolean isReadOnly()
    {
        return false;
    }

    @Override
    public void reset()
    {
        this.keywords = new ArrayList();
        this.values = new ArrayList();
    }

    @Override
    public void mergeTree(String formatName, Node root) throws IIOInvalidTreeException
    {
        if(!formatName.equals(nativeMetadataFormatName))
        {
            throw new IllegalArgumentException("Bad format name!");
        }

        Node node = root;

        if(!node.getNodeName().equals(nativeMetadataFormatName))
        {
            fatal(node, "Root must be " + nativeMetadataFormatName);
        }
        node = node.getFirstChild();
        while(node != null)
        {
            if(!node.getNodeName().equals("KeywordValuePair"))
            {
                fatal(node, "Node name not KeywordValuePair!");
            }

            NamedNodeMap attributes = node.getAttributes();
            Node keywordNode = attributes.getNamedItem("keyword");
            Node valueNode = attributes.getNamedItem("value");
            if (keywordNode == null || valueNode == null)
            {
                fatal(node, "Keyword or value missing!");
            }

            // Store keyword and value
            keywords.add((String)keywordNode.getNodeValue());
            values.add((String)valueNode.getNodeValue());

            // Move to the next sibling
            node = node.getNextSibling();
        }
    }

    private void fatal(Node node, String reason) throws IIOInvalidTreeException
    {
        throw new IIOInvalidTreeException(reason, node);
    }

    public static class EPSMetaDataFormat extends IIOMetadataFormatImpl
    {
        // Create a single instance of this class (singleton pattern)
        private static EPSMetaDataFormat defaultInstance = new EPSMetaDataFormat();

        // Make constructor private to enforce the singleton pattern
        private EPSMetaDataFormat()
        {
            // Set the name of the root node
            // The root node has a single child node type that may repeat
            super("com.spectotechnologies.imageio.plugins.eps.EPSMetaData_1.0",CHILD_POLICY_REPEAT);

            // Set up the “KeywordValuePair” node, which has no children
            addElement("KeywordValuePair","com.spectotechnologies.imageio.plugins.eps.EPSMetaData_1.0",CHILD_POLICY_EMPTY);

            // Set up attribute “keyword” which is a String that is required
            // and has no default value
            addAttribute("KeywordValuePair","keyword",DATATYPE_STRING,true,null);

            // Set up attribute “value” which is a String that is required
            // and has no default value
            addAttribute("KeywordValuePair","value",DATATYPE_STRING,true,null);
        }

        // Check for legal element name
        @Override
        public boolean canNodeAppear(String elementName, ImageTypeSpecifier imageType)
        {
            return elementName.equals("KeywordValuePair");
        }

        // Return the singleton instance
        public static EPSMetaDataFormat getDefaultInstance()
        {
            return defaultInstance;
        }
    }
}