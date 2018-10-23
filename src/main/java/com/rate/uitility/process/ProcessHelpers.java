package com.rate.uitility.process;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;
import java.util.Vector;

import com.rate.persistence.ValueObject;
import com.rate.persistence.ValueObjectFactory;

public class ProcessHelpers
{
  /**
   * Save the contents of an MBean to an XML file
   *
   * @param vo
   *          An extension of ValueObject holding MBean data
   * @param fileName
   *          The name of the file to export to.
   */
//  public static void storeMBeanData(ValueObject vo, String fileName) throws IOException, ScholarOneException
//  {
//    // Output as UTF-8...
//    File outputFile = new File(fileName);
//    PrintWriter pw = new PrintWriter(new OutputStreamWriter(S3FileFactory.getOutputStream(outputFile), "UTF-8"));
//    pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
//    String strategyClassName = "com.scholarone.persistence.valueobject.XMLPersistence";
//    XMLPersistence strategy = (XMLPersistence) com.scholarone.persistence.valueobject.ValueObjectFactory.getInstance()
//        .createStrategy(strategyClassName);
//    strategy.setXMLPrintWriter(pw);
//    vo.setIsModified(true);
//    vo.save(Constants.INTEGER_0, "writeToXML", strategy);
//    pw.close();
//  }

  /**
   * Loads MBean data from an XML file that the MBean exported earlier
   * 
   * @param CLASS_NAME the name of the class, an extension of ValueObject
   * that will be populated with data from the file
   * @param fileName the name of the xml file to load
   * @return a ValueObject with data from the file
   */
  public static ValueObject loadMBeanData(String CLASS_NAME, String fileName)
  {
    Vector criteria = new Vector();
    criteria.add(fileName);
    criteria.add(Boolean.FALSE);
    ValueObject vo = null;
    try
    {
      String strategyClass = "com.scholarone.persistence.valueobject.XMLPersistence";
//      java.lang.reflect.Field f = vo.getClass().getField("CLASS_NAME");
//      String CLASS_NAME = (String)f.get(null);
      vo = ValueObjectFactory.getInstance().findObjectByCriteria(CLASS_NAME, criteria,
          "readFromXML", strategyClass);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    return vo;
  }

  /**
   * @param serverName
   * @param serversList text string list of servers to run LRU daemon on
   * @return
   */
  public static boolean isThisServerUsedByProcess(String serverName, String serversList)
  {
    // Machine run list can be either:
    // "NONE" - daemon will not run on any machine
    // <empty> - daemon will run on all machines
    // -xxx,yyy - daemon will run on all machines EXCEPT xxx or yyy
    //  xxx,yyy - daemon will run only on machines xxx,yyy

    // Should we run on this machine?
    boolean runThisTimerFl = false;
    if (serversList != null && "NONE".equalsIgnoreCase(serversList))
      return runThisTimerFl; // is false
    if (serversList == null || serversList.trim().length() == 0)
      runThisTimerFl = true; // runs on any and all servers
    else
    {
      if (serversList.startsWith("-"))
      {
        runThisTimerFl = true;

        String machineList = serversList.substring(1, serversList.length());
        StringTokenizer st = new StringTokenizer(machineList, ","); // split based on ,
        while (st.hasMoreTokens())
        {
          String value = st.nextToken().toUpperCase().trim();
          if (value.length() > 0)
          {
            if (value.equalsIgnoreCase(serverName))
            {
              runThisTimerFl = false;
              break;
            }
          }
        }
      }
      else
      {
        runThisTimerFl = false;

        String machineList = serversList.substring(0, serversList.length());
        StringTokenizer st = new StringTokenizer(machineList, ","); // split based on ,
        while (st.hasMoreTokens())
        {
          String value = st.nextToken().toUpperCase().trim();
          if (value.length() > 0)
          {
            if (value.equalsIgnoreCase(serverName))
            {
              runThisTimerFl = true;
              break;
            }
          }
        }
      }
    }
    return runThisTimerFl;
  }
  
  /**
   * This routine is Duplicated in MC DateUtiltiy. Given a long 
   * value this method will produce a String representation of the 
   * duration in the following form:
   * days 'D' hours 'H' minutes 'M'  
   *   123D12H34M
   * @param diff a long value representing the millisecond difference between two long time values
   * @return
   */
  public static String getDateDiffString(long diff)
  {
    String minus = (diff<0)?"-":"";
    diff = Math.abs(diff);
    String result = "";

    String miliStr = "";
    String secondsStr = "";
    String minutesStr = "";
    String hoursStr = "";
    String daysStr = "";
    
    long miliseconds = 0;
    long seconds = 0;
    long minutes = 0;
    long hours = 0;
    long days = 0;
    
    seconds = diff / 1000;
    miliseconds = diff-(seconds*1000);
    miliStr = (miliseconds >= 0)?"."+miliseconds+"S":"";
    if (seconds > 60)
    {
      minutes = seconds / 60;
      seconds = seconds-(minutes*60);
      secondsStr = ""+seconds;
      if (minutes > 60)
      {
        hours = minutes / 60;
        minutes = minutes-(hours*60);
        minutesStr = ""+minutes+"M";
        if (hours > 24)
        {
          days = hours / 24;
          hours = hours - (days*24);
          hoursStr = ""+hours+"H";
          daysStr = ""+days+"D";
        }
        else
        {
          hoursStr = ""+hours+"H";
        }
      }
      else
      {
        minutesStr = ""+minutes+"M";
      }
    }
    else
    {
      secondsStr = ""+seconds;
    }
    result = result + minus+daysStr+hoursStr+minutesStr+secondsStr+miliStr;
    return result;
  }

}