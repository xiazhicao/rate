package com.rate.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;

public class XMLUtils
{

  private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

  private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

  public final static int NONVALIDATING_PARSER = 0; // the

  // default

  public final static int VALIDATING_PARSER_SCHEMA = 1;

  public final static int VALIDATING_PARSER_DTD = 2;

  XMLUtils()
  {
  }

  /**
   * Returns a new parser factory instance.
   * 
   */
  public static SAXParserFactory newParserFactory() throws Exception
  {
    return newParserFactory(NONVALIDATING_PARSER);
  }

  /**
   * Returns a new parser factory instance.
   * 
   */
  public static SAXParserFactory newParserFactory(int validationType) throws Exception
  {
    try
    {
      SAXParserFactory factory = SAXParserFactory.newInstance();

      factory.setNamespaceAware(true);

      if (validationType != NONVALIDATING_PARSER)
      {
        factory.setValidating(true);
      }

      return factory;
    }
    catch (FactoryConfigurationError e)
    {
      throw new Exception("XML parser factory has not been " + "configured correctly: "
          + e.getMessage());
    }
  }

  /**
   * Returns a newly created SAX 2 XMLReader, using the default parser factory.
   * 
   * @return a SAX 2 XMLReader.
   */
  public static XMLReader getXMLReader() throws Exception
  {
    return getXMLReader(NONVALIDATING_PARSER);
  }

  /**
   * Returns a newly created SAX 2 XMLReader, using the default parser factory.
   * 
   * @return a SAX 2 XMLReader.
   */
  public static XMLReader getXMLReader(int validationType) throws Exception
  {
    try
    {
      return newSAXParser(validationType).getXMLReader();
    }
    catch (SAXException e)
    {
      throw new Exception(e.toString());
    }
  }

  /**
   * @return a new SAXParser instance as helper for getXMLReader.
   */
  private static SAXParser newSAXParser(int validationType) throws Exception
  {
    try
    {
      SAXParser parser = newParserFactory(validationType).newSAXParser();

      if (validationType == VALIDATING_PARSER_SCHEMA)
      {
        parser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
      }

      return parser;
    }
    catch (ParserConfigurationException e)
    {
      throw new Exception("Cannot create parser for the given " + "configuration: " + e.getMessage());
    }
    catch (SAXNotRecognizedException x)
    {
      // Happens if the parser does not support JAXP 1.2
      throw new Exception(x.toString());
    }
    catch (SAXException e)
    {
      throw new Exception(e.toString());
    }
  }

  /**
   * Finds the XML configuration file that corresponds to the given class. Returns it as an org.xml.sax.InputSource to
   * be readily used by the XMLReader.
   */
  public static InputSource getConfigInputSource(Object forWhom) throws Exception
  {
    // this is based on the class of the passed-in object
    if (forWhom == null)
    {
      return null; // we can't handle a null object
    }

    Class inClass = null;

    if (forWhom instanceof java.lang.Class)
    {
      inClass = (Class) forWhom;
    }
    else
    {
      inClass = forWhom.getClass();
    }

    if (inClass == null)
    {
      return null;
    }

    // we need to calculate inClass's package name
    String className = inClass.getName();
    String pkgName = inClass.getPackage().getName();

    String fileName = className.substring(pkgName.length() + 1) + ".xml";
    InputStream inStream = inClass.getClassLoader().getResourceAsStream(getPackagePath(pkgName) + "/" + fileName);

    if (inStream == null)
    {
      throw new Exception("Unable to find config file " + fileName + " for class " + className);
    }

    return new InputSource(inStream);
  }

  public static InputStream getXSLInputSource(String fileName)
  {
    // grab a new instance of this, as a hack to get the right classloader
    Class tmpClass = XMLUtils.class;

    String fullPath = "xsl/" + fileName;
    InputStream inStream = tmpClass.getClassLoader().getResourceAsStream(fullPath);

    return inStream;
  }

  /**
   * Finds the XML configuration file that corresponds to the given class. By convention, will be in same JAR/WAR file
   * as the class it configures. Returns the full package path to the file. This method is mainly a convenience method
   * for use in debugging output.
   */
  public static String getConfigFileName(Object forWhom)
  {
    // this is based on the class of the passed-in object
    if (forWhom == null)
    {
      return null; // we can't handle a null object
    }

    Class inClass = null;

    if (forWhom instanceof java.lang.Class)
    {
      inClass = (Class) forWhom;
    }
    else
    {
      inClass = forWhom.getClass();
    }

    if (inClass == null)
    {
      return null;
    }

    String className = inClass.getName().replace('.', File.separatorChar);

    String fileName = className + ".xml";

    return fileName;
  }

  private static String getPackagePath(String packageName)
  {
    return packageName.replace('.', '/');
  }

  public static boolean applyXSLTransform(Source input, Result output, Source styleSheet)
  {
    boolean success = true;
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer;

    try
    {
      // if null then we want the "identity" x-form
      if (styleSheet == null)
      {
        transformer = factory.newTransformer();

        transformer.setOutputProperty(OutputKeys.METHOD, "xml"); // html
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); // ISO-8859-1
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "s1.dtd");
      }
      else
      {
        transformer = factory.newTransformer(styleSheet);
      }

      transformer.transform(input, output);
    }
    catch (Exception te)
    {
      success = false;
    }

    // catch (TransformerException te)
    // {
    // log.debug("Error = " + te.toString());
    // }
    return success;
  }

  /**
   * This class is needed to find DTDs and XML Schemas within our deployed app (WAR file).
   */
  public static class DTDSchemaResolver implements EntityResolver
  {

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
    {
      if (systemId.length() > 0)
      {
        // return a special input source
        // just get the plain file name
        int index = systemId.lastIndexOf('/');

        if (index >= 0)
        {
          systemId = systemId.substring(index + 1);
        }

        InputStream inStream = XMLUtils.getXSLInputSource(systemId);

        return new InputSource(inStream);
      }
      else
      {
        // use the default behaviour
        return null;
      }
    }
  }

  /**
   * Converts the input string to one that is valid to export as xml data. Converts &, ", <, and > to their HTML
   * equivalents.
   * 
   * @param in
   *          The input string to convert
   * @return The input string if it contains no delimiters, or a new converted string.
   * @throws NullPointerException
   *           if the input string is null
   */
  public static String convertXMLCharacters(String in)
  {
    final String delimiters = "&\"<>\n\r\t";
    StringTokenizer st = new StringTokenizer(in, delimiters, true);

    // If the string is empty, or if there is one token and it is not a delimiter, then no conversion is necessary.
    if (("".equals(in)) || ((st.countTokens() == 1) && (delimiters.indexOf(in) < 0)))
    {
      return in;
    }

    StringBuffer sb = new StringBuffer(in.length());

    while (st.hasMoreTokens())
    {
      String token = st.nextToken();

      if (token.length() == 1)
      {
        switch (token.charAt(0))
        {
          case '&':
            sb.append("&amp;");
            break;

          case '<':
            sb.append("&lt;");
            break;

          case '>':
            sb.append("&gt;");
            break;

          case '"':
            sb.append("&quot;");
            break;

          case '\n':
            sb.append("\\n");
            break;

          case '\r':
            sb.append("\\r");
            break;

          case '\t':
            sb.append("\\t");
            break;

          default:
            sb.append(token);
            break;
        }
      }
      else
      // Length != 1 implies it can't be a delimiter, so just append.
      {
        sb.append(token);
      }
    }

    return sb.toString();
  }

  /**
   * @param value
   * @return
   * 
   * Returns a nice safe output for XML
   * 
   */
  public static String removeNonSafeCharacters(String value)
  {
    if (value == null || value.length() == 0) return value;

    StringBuffer valueOut = new StringBuffer();

    for (int i = 0; i < value.length(); i++)
    {
      if (value.charAt(i) >= 32)
      {
        valueOut.append(value.charAt(i));
      }
    }

    return valueOut.toString();
  }

}