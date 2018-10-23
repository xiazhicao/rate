package com.rate.persistence.valueobject;

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.rate.persistence.helpers.Attribute;
import com.rate.persistence.helpers.ChildElement;
import com.rate.persistence.helpers.StoredProcedure;
import com.rate.persistence.helpers.XMLContent;
import com.rate.xml.XMLUtils;

public class ValueObjectConfigurationReader
{

  public ValueObjectConfigurationReader()
  {
  }

  public ValueObjectConfiguration readConfiguration(Object valueOb) throws Exception
  {
    InputSource source = XMLUtils.getConfigInputSource(valueOb);
    XMLReader reader = XMLUtils.getXMLReader();
    ValueObjectConfiguration config;

    if (valueOb instanceof Class)
    {
      config = new ValueObjectConfiguration((Class) valueOb);
    }
    else
    {
      config = new ValueObjectConfiguration(valueOb.getClass());
    }

    DefaultHandler handler = new RootHandler(reader, config);

    try
    {
      reader.parse(source);
    }
    catch (SAXParseException spe)
    {
      spe.printStackTrace();

      String err = "File " + XMLUtils.getConfigFileName(valueOb) + " Line " + spe.getLineNumber() + ", Column "
          + spe.getColumnNumber();


      throw new Exception(err);
    }
    catch (SAXException se)
    {

      throw new Exception(se.toString());
    }
    catch (IOException ioe)
    {

      throw new Exception(ioe.toString());
    }

    return config;
  }

  static class AbstractHandler extends DefaultHandler
  {

    /**
     * Previous handler for the document. When the next element is finished, control returns to this handler.
     */
    protected DefaultHandler parentHandler;

    protected XMLReader reader;

    protected ValueObjectConfiguration config;

    protected static Locator locator;

    /**
     * Creates a handler and sets the parser to use it for the current element.
     */
    public AbstractHandler(XMLReader reader, DefaultHandler parentHandler, ValueObjectConfiguration config)
    {
      this.parentHandler = parentHandler;
      this.reader = reader;
      this.config = config;

      // Start handling SAX events
      reader.setContentHandler(this);
      reader.setDTDHandler(this);
      reader.setEntityResolver(this);
      reader.setErrorHandler(this);
    }

    public void setDocumentLocator(Locator locator)
    {
      AbstractHandler.locator = locator;
    }

    /**
     * Handles the start of an element. This base implementation just throws an exception.
     */
    public void startElement(String namespace, String localName, String qName, Attributes attrs)
        throws SAXParseException
    {

      for (int i = 0; i < attrs.getLength(); i++)
      {
      }
    }

    /**
     * Handles text within an element. This base implementation just throws an exception.
     * 
     */
    public void characters(char[] buf, int start, int count) throws SAXParseException
    {
      if (count > 0)
      {
        String s = new String(buf, start, count).trim();

        if (s.length() > 0)
        {
          throw new SAXParseException("Unexpected characters '" + s + " in XML file", locator);
        }
      }
    }

    /**
     * Called when this element and all elements nested into it have been handled.
     */
    protected void finished()
    {
    }

    /**
     * Handles the end of an element. Any required clean-up is performed by the finished() method and then the original
     * handler is restored to the parser.
     * 
     */
    public void endElement(String namespace, String localName, String qName) throws SAXException
    {
      finished();

      // Let parent resume handling SAX events
      if (parentHandler != null)
      {
        reader.setContentHandler(parentHandler);
        reader.setDTDHandler(parentHandler);
        reader.setEntityResolver(parentHandler);
        reader.setErrorHandler(parentHandler);
      }
    }
  }

  static class RootHandler extends AbstractHandler
  {

    public RootHandler(XMLReader reader, ValueObjectConfiguration config)
    {
      super(reader, null, config);
    }

    /**
     * Handles the start of a valueobject element.
     */
    public void startElement(String namespace, String localName, String qName, Attributes attrs)
        throws SAXParseException
    {
      if (localName.equals("valueobject"))
      {
        new ValueObjectHandler(reader, this, config).init(namespace, localName, qName, attrs);
      }
      else
      {
        throw new SAXParseException("Config file is not of expected XML type", locator);
      }
    }
  }

  static class ValueObjectHandler extends AbstractHandler
  {

    public ValueObjectHandler(XMLReader reader, DefaultHandler parentHandler, ValueObjectConfiguration config)
    {
      super(reader, parentHandler, config);
    }

    // initial values for the element that caused this class to be invoked
    public void init(String namespace, String localName, String qName, Attributes attrs) throws SAXParseException
    {
      for (int i = 0; i < attrs.getLength(); i++)
      {
        // the class name
        if (attrs.getLocalName(i).equals("name"))
        {
          config.setTypeName(attrs.getValue(i));
        }
        else if (attrs.getLocalName(i).equals("use_cache"))
        {
          config.setUseCache(Boolean.valueOf(attrs.getValue(i)));
        }
        else if (attrs.getLocalName(i).equals("cache_time"))
        {
          config.setCacheTime(attrs.getValue(i));
        }
        else if (attrs.getLocalName(i).equals("cache_refresh_on_use"))
        {
          config.setCacheRefreshOnUse(Boolean.valueOf(attrs.getValue(i)));
        }
        else if (attrs.getLocalName(i).equals("hybridStrategy"))
        {
          config.setUseHybridStrategy(Boolean.valueOf(attrs.getValue(i)));
        }

      }
    }

    /**
     * Handles the start of a valueobject element.
     * 
     * Possible sub-elements are "attributes", "finderMethod", and "saveMethod".
     */
    public void startElement(String namespace, String localName, String qName, Attributes attrs)
        throws SAXParseException
    {
      if (localName.equals("attributes"))
      {
        new AttributesHandler(reader, this, config);
      }
      else if (localName.equals("finderMethod"))
      {
        new FinderMethodHandler(reader, this, config).init(namespace, localName, qName, attrs);
      }
      else if (localName.equals("saveMethod"))
      {
        new SaveMethodHandler(reader, this, config).init(namespace, localName, qName, attrs);
      }
      else if (localName.equals("distribution"))
      {
        new DistributionHandler(reader, this, config);
      }
      else if (localName.equals("validators"))
      {
        new ValidatorHandler(reader, this, config);
      }
    }
  }
  
  static class DistributionHandler extends AbstractHandler
  {
    public DistributionHandler(XMLReader reader, DefaultHandler parentHandler, ValueObjectConfiguration config)
    {
      super(reader, parentHandler, config);
    }
    
    public void startElement(String namespace, String localName, String qName, Attributes attrs)
        throws SAXParseException
    {
      if (localName.equals("fileStorageObject"))
      {
        new FileStorageObjectHandler(reader, this, config).init(namespace, localName, qName, attrs);
      }
    }

    public void endElement(String namespace, String localName, String qName) throws SAXException
    {
      // don't return back up to parent until we reach the closing "distribution" tag
      if (localName.equals("distribution"))
      {
        super.endElement(namespace, localName, qName);
      }
    }

  }

  
  static class FileStorageObjectHandler extends AbstractHandler
  {
    public FileStorageObjectHandler(XMLReader reader, DefaultHandler parentHandler, ValueObjectConfiguration config)
    {
      super(reader, parentHandler, config);
    }
    
    // initial values for the element that caused this class to be invoked
    public void init(String namespace, String localName, String qName, Attributes attrs) throws SAXParseException
    {
      config.setCurrentFsMap(true);
      for (int i = 0; i < attrs.getLength(); i++)
      {
        // the class name
        if (attrs.getLocalName(i).equals("object_type"))
        {
          config.setObjectType(attrs.getValue(i));
        }
        else if (attrs.getLocalName(i).equals("file_location"))
        {
          config.setFileLocation(attrs.getValue(i));
        }
        else if (attrs.getLocalName(i).equals("file_extension"))
        {
          config.setFileExtension(attrs.getValue(i));
        }
        else if (attrs.getLocalName(i).equals("primary_key_type"))
        {
          config.setPrimaryKeyType(attrs.getValue(i));
        }
        else if (attrs.getLocalName(i).equals("get_primary_key_method_name"))
        {
          config.setGetPrimaryKeyMethodName(attrs.getValue(i));
        }
        else if (attrs.getLocalName(i).equals("set_primary_key_method_name"))
        {
          config.setSetPrimaryKeyMethodName(attrs.getValue(i));
        }
        else if (attrs.getLocalName(i).equals("datetime_added_type"))
        {
          config.setDatetimeAddedType(attrs.getValue(i));
        }
        else if (attrs.getLocalName(i).equals("get_datetime_added_method_name"))
        {
          config.setGetDatetimeAddedMethodName(attrs.getValue(i));
        }
        else if (attrs.getLocalName(i).equals("set_datetime_added_method_name"))
        {
          config.setSetDatetimeAddedMethodName(attrs.getValue(i));
        }
      }
    }
    
    
    public void startElement(String namespace, String localName, String qName, Attributes attrs)
        throws SAXParseException
    {
      if (localName.equals("fieldMapping"))
      {
        new FieldMappingHandler(reader, this, config).init(namespace, localName, qName, attrs);
      }
    }

    public void endElement(String namespace, String localName, String qName) throws SAXException
    {
      // don't return back up to parent until we reach the closing "fileStorageObject" tag
      if (localName.equals("fileStorageObject"))
      {
        config.setCurrentFsMap(false);
        super.endElement(namespace, localName, qName);
      }
    }
  
  }

  
  static class FieldMappingHandler extends AbstractHandler
  {
    public FieldMappingHandler(XMLReader reader, DefaultHandler parentHandler, ValueObjectConfiguration config)
    {
      super(reader, parentHandler, config);
    }
    
    // initial values for the element that caused this class to be invoked
    public void init(String namespace, String localName, String qName, Attributes attrs) throws SAXParseException
    {
      FieldMappingMap mappingMap = new FieldMappingMap();
      for (int i = 0; i < attrs.getLength(); i++)
      {
        // the class name
        if (attrs.getLocalName(i).equals("element_name"))
        {
          mappingMap.elementName = attrs.getValue(i);
        }
        else if (attrs.getLocalName(i).equals("type"))
        {
          mappingMap.type = attrs.getValue(i);
        }
        else if (attrs.getLocalName(i).equals("get_method_name"))
        {
          mappingMap.getMethodName = attrs.getValue(i);
        }
        else if (attrs.getLocalName(i).equals("set_method_name"))
        {
          mappingMap.setMethodName = attrs.getValue(i);
        }
        else if (attrs.getLocalName(i).equals("store_in_db"))
        {
          mappingMap.store_in_db = attrs.getValue(i);
        }
        else if (attrs.getLocalName(i).equals("store_in_xml"))
        {
          mappingMap.store_in_xml = attrs.getValue(i);
        }
        else if (attrs.getLocalName(i).equals("read_from"))
        {
          mappingMap.read_from = attrs.getValue(i);
        }
        
        
      }
      config.setFieldMapping(mappingMap);
    }
        
  }

  
  static class AttributesHandler extends AbstractHandler
  {

    public AttributesHandler(XMLReader reader, DefaultHandler parentHandler, ValueObjectConfiguration config)
    {
      super(reader, parentHandler, config);
    }

    /**
     * Handles the start of a valueobject element.
     * 
     * Possible sub-elements are "attributes", "finderMethod", and "saveMethod".
     */
    public void startElement(String namespace, String localName, String qName, Attributes attrs)
        throws SAXParseException
    {
      if (localName.equals("attribute"))
      {
        String name = null;
        String type = null;
        String column = null;
        boolean collection = false;
        String persistence = null;
        String parentKey = null;
        boolean resource = false;
        int size = 0;

        for (int i = 0; i < attrs.getLength(); i++)
        {
          if (attrs.getLocalName(i).equals("name"))
          {
            name = attrs.getValue(i);
          }
          else if (attrs.getLocalName(i).equals("type"))
          {
            type = attrs.getValue(i);
          }
          else if (attrs.getLocalName(i).equals("column"))
          {
            column = attrs.getValue(i).toUpperCase();
          }
          else if (attrs.getLocalName(i).equals("collection"))
          {
            collection = Boolean.valueOf(attrs.getValue(i)).booleanValue();
          }
          else if (attrs.getLocalName(i).equals("resource"))
          {
            resource = Boolean.valueOf(attrs.getValue(i)).booleanValue();
          }
          else if (attrs.getLocalName(i).equals("size"))
          {
            size = Integer.valueOf(attrs.getValue(i)).intValue();
          }
          else if (attrs.getLocalName(i).equals("persistence"))
          {
            persistence = attrs.getValue(i);
          }
          else if (attrs.getLocalName(i).equals("parentkey"))
          {
            parentKey = attrs.getValue(i);
          }
        }

        config.setAttribute(name, type, column, collection, persistence, parentKey, resource, size);
      }
    }

    public void endElement(String namespace, String localName, String qName) throws SAXException
    {
      // don't return back up to parent until we reach the closing "attributes"
      // tag
      if (localName.equals("attributes"))
      {
        super.endElement(namespace, localName, qName);
      }
    }
  }

  static class FinderMethodHandler extends AbstractHandler
  {

    public FinderMethodHandler(XMLReader reader, DefaultHandler parentHandler, ValueObjectConfiguration config)
    {
      super(reader, parentHandler, config);
    }

    // initial values for the element that caused this class to be invoked
    public void init(String namespace, String localName, String qName, Attributes attrs) throws SAXParseException
    {
      for (int i = 0; i < attrs.getLength(); i++)
      {
        if (attrs.getLocalName(i).equals("name"))
        {
          config.setCurrentFinderMethod(attrs.getValue(i));
        }
      }
    }

    /**
     * Handles the start of a valueobject element.
     * 
     * Possible sub-elements are "attributes", "finderMethod", and "saveMethod".
     */
    public void startElement(String namespace, String localName, String qName, Attributes attrs)
        throws SAXParseException
    {
      if (localName.equals("storedProcedure"))
      {
        new StoredProcedureHandler(reader, this, config).init(namespace, localName, qName, attrs);
        config.setFinderProcedure();
      }
      else if (localName.equals("xmlContent"))
      {
        XMLContent xmlContent = new XMLContent();

        config.setFinderXMLContent(xmlContent);

        XMLContentHandler xch = new XMLContentHandler(reader, this, config);

        xch.setXMLContent(xmlContent);
      }
    }

    public void endElement(String namespace, String localName, String qName) throws SAXException
    {
      config.setCurrentFinderMethod(null);
      super.endElement(namespace, localName, qName);
    }
  }

  static class SaveMethodHandler extends AbstractHandler
  {

    public SaveMethodHandler(XMLReader reader, DefaultHandler parentHandler, ValueObjectConfiguration config)
    {
      super(reader, parentHandler, config);
    }

    // initial values for the element that caused this class to be invoked
    public void init(String namespace, String localName, String qName, Attributes attrs) throws SAXParseException
    {
      String singleLine = null;
      String doResourcing = null;

      for (int i = 0; i < attrs.getLength(); i++)
      {
        if (attrs.getLocalName(i).equals("name"))
        {
          config.setCurrentSaveMethod(attrs.getValue(i));
        }
        else if (attrs.getLocalName(i).equals("singleLine"))
        {
          singleLine = attrs.getValue(i);
        }
        else if (attrs.getLocalName(i).equals("do_resourcing"))
        {
          doResourcing = attrs.getValue(i);
        }
      }

      if (singleLine != null)
      {
        config.setSaveSingleLine(singleLine);
      }
      if (doResourcing != null)
      {
        config.setSaveDoResourcing(doResourcing);
      }
    }

    /**
     * Handles the start of a valueobject element.
     * 
     * Possible sub-elements are "attributes", "finderMethod", and "saveMethod".
     */
    public void startElement(String namespace, String localName, String qName, Attributes attrs)
        throws SAXParseException
    {
      if (localName.equals("storedProcedure"))
      {
        new StoredProcedureHandler(reader, this, config).init(namespace, localName, qName, attrs);
        config.setSaveProcedure();
      }
      else if (localName.equals("xmlContent"))
      {
        XMLContent xmlContent = new XMLContent();

        config.setSaveXMLContent(xmlContent);

        XMLContentHandler xch = new XMLContentHandler(reader, this, config);

        xch.setXMLContent(xmlContent);
      }
    }

    public void endElement(String namespace, String localName, String qName) throws SAXException
    {
      config.setCurrentSaveMethod(null);
      super.endElement(namespace, localName, qName);
    }
  }

  static class StoredProcedureHandler extends AbstractHandler
  {

    private int unNamedAttrCount = 0;

    public StoredProcedureHandler(XMLReader reader, DefaultHandler parentHandler, ValueObjectConfiguration config)
    {
      super(reader, parentHandler, config);
    }

    // initial values for the element that caused this class to be invoked
    public void init(String namespace, String localName, String qName, Attributes attrs) throws SAXParseException
    {
      for (int i = 0; i < attrs.getLength(); i++)
      {
        if (attrs.getLocalName(i).equals("name"))
        {
          config.setCurrentStoredProcedure(attrs.getValue(i));
        }
      }
    }

    /**
     * Handles the start of a StoredProcedure element.
     * 
     * Possible sub-elements are "input_parameter" and "resultSets"
     */
    public void startElement(String namespace, String localName, String qName, Attributes attrs)
        throws SAXParseException
    {
      if (localName.equals("input_parameter"))
      {
        Attribute newArg = null;
        boolean inOut = false;
        String format = null;
        String attributeName = null;

        for (int i = 0; i < attrs.getLength(); i++)
        {
          if (attrs.getLocalName(i).equals("attribute_name"))
          {
            attributeName = attrs.getValue(i);
          }
          else if (attrs.getLocalName(i).equals("attribute_type"))
          {
            String type = attrs.getValue(i);
            String name = "dummy_attr_" + unNamedAttrCount;
            String col = "dummy_col_" + unNamedAttrCount;

            unNamedAttrCount++;

            config.setAttribute(name, type, col, false, "no");

            attributeName =name;
          }
          else if (attrs.getLocalName(i).equals("inout"))
          {
            inOut = Boolean.valueOf(attrs.getValue(i)).booleanValue();
          }
          else if (attrs.getLocalName(i).equals("format"))
          {
            format = attrs.getValue(i);
          }
        }
        if (format !=null)
        {
          newArg = config.getAttribute(attributeName);
          if (newArg == null)
          {
            throw new SAXParseException("Attribute " + attributeName + " not defined.  Cannot parse", locator);
          }
          if (newArg != null)
          {
            newArg = config.setAttribute(newArg, format);
            newArg = config.addStoredProcedureArg(newArg.getActualName());
          }
        } 
        else
        {
          newArg = config.addStoredProcedureArg(attributeName);
          if (newArg == null)
          {
            throw new SAXParseException("Attribute " + attributeName + " not defined.  Cannot parse", locator);
          }
        }

        if ( inOut ) 
        {
          StoredProcedure currentSP = config.getCurrentStoredProcedure();
          currentSP.addInOutArgument(newArg);
          //newArg.setInOut(inOut);
        }
      }
      else if (localName.equals("resultSets"))
      {
        new ResultSetsHandler(reader, this, config);
      }
    }

    public void endElement(String namespace, String localName, String qName) throws SAXException
    {
      // don't return back up to parent until we reach the closing
      // "storedProcedure" tag
      if (localName.equals("storedProcedure"))
      {
        config.setCurrentStoredProcedure(null);
        super.endElement(namespace, localName, qName);
      }
    }
  }

  static class ResultSetsHandler extends AbstractHandler
  {

    public ResultSetsHandler(XMLReader reader, DefaultHandler parentHandler, ValueObjectConfiguration config)
    {
      super(reader, parentHandler, config);
    }

    /**
     * Handles the start of a ResultSets element.
     * 
     * Possible sub-elements are "output_parameter"
     */
    public void startElement(String namespace, String localName, String qName, Attributes attrs)
        throws SAXParseException
    {
      if (localName.equals("output_parameter"))
      {
        for (int i = 0; i < attrs.getLength(); i++)
        {
          if (attrs.getLocalName(i).equals("attribute_name"))
          {
            config.addStoredProcedureResult(attrs.getValue(i));
          }
        }
      }
    }

    public void endElement(String namespace, String localName, String qName) throws SAXException
    {
      // don't return back up to parent until we reach the closing "resultSets"
      // tag
      if (localName.equals("resultSets"))
      {
        super.endElement(namespace, localName, qName);
      }
    }
  }

  static class XMLContentHandler extends AbstractHandler
  {

    private XMLContent xmlContent;

    public XMLContentHandler(XMLReader reader, DefaultHandler parentHandler, ValueObjectConfiguration config)
    {
      super(reader, parentHandler, config);
    }

    public void setXMLContent(XMLContent content)
    {
      xmlContent = content;
    }

    /**
     * Handles the start of a StoredProcedure element.
     * 
     * Possible sub-elements are "input_parameter" and "resultSets"
     */
    public void startElement(String namespace, String localName, String qName, Attributes attrs)
        throws SAXParseException
    {
      if (localName.equals("child_element"))
      {
        ChildElement ce = new ChildElement();

        for (int i = 0; i < attrs.getLength(); i++)
        {
          if (attrs.getLocalName(i).equals("element_name"))
          {
            ce.setElementName(attrs.getValue(i));
          }
          else if (attrs.getLocalName(i).equals("object_type"))
          {
            ce.setObjectType(attrs.getValue(i));
          }
          else if (attrs.getLocalName(i).equals("add_method_name"))
          {
            ce.setAddMethodName(attrs.getValue(i));
          }
          else if (attrs.getLocalName(i).equals("get_method_name"))
          {
            ce.setGetMethodName(attrs.getValue(i));
          }
        }

        xmlContent.addChildElement(ce);
      }
    }

    public void endElement(String namespace, String localName, String qName) throws SAXException
    {
      // don't return back up to parent until we reach the closing "xmlContent"
      // tag
      if (localName.equals("xmlContent"))
      {
        super.endElement(namespace, localName, qName);
      }
    }
  }

  static class ValidatorHandler extends AbstractHandler
  {

    public ValidatorHandler(XMLReader reader, DefaultHandler parentHandler, ValueObjectConfiguration config)
    {
      super(reader, parentHandler, config);
    }

    /**
     * Handles the start of a valueobject element.
     * 
     * Possible sub-elements are "attributes", "finderMethod", and "saveMethod".
     */
    public void startElement(String namespace, String localName, String qName, Attributes attrs)
        throws SAXParseException
    {
      if (localName.equals("validator"))
      {
        String name = null;
        for (int i = 0; i < attrs.getLength(); i++)
        {
          if (attrs.getLocalName(i).equals("name"))
          {
            name = attrs.getValue(i);
          }
        }

        config.getValidatorList().add(name);
      }
    }

    public void endElement(String namespace, String localName, String qName) throws SAXException
    {
      // don't return back up to parent until we reach the closing "attributes"
      // tag
      if (localName.equals("validators"))
      {
        super.endElement(namespace, localName, qName);
      }
    }
  }
}