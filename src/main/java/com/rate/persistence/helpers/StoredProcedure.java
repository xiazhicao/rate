package com.rate.persistence.helpers;

import java.io.Serializable;
import java.util.Vector;

public class StoredProcedure implements Serializable
{

  private String procedureName;

  Vector arguments;

  Vector inoutArguments;

  Vector outputs;

  Vector results;

  Vector resultConfigs;

  public StoredProcedure()
  {
    arguments = new Vector();
    inoutArguments = new Vector();
    outputs = new Vector();
    results = new Vector();
    resultConfigs = new Vector();

    // the 0th result set is this object itself
    // so make the array start at 1
    results.add(new Object());
    resultConfigs.add(new Object());
  }

  public StoredProcedure(String name)
  {
    this();

    procedureName = name;
  }

  public String getName()
  {
    return procedureName;
  }

  public void addArgument(Attribute arg)
  {
    arguments.add(arg);
  }

  public Vector getArguments()
  {
    return arguments;
  }

  public void addInOutArgument(Attribute arg)
  {
    inoutArguments.add(arg);
  }

  public Vector getInOutArguments()
  {
    return inoutArguments;
  }

  public void addOutput(Attribute arg)
  {
    outputs.add(arg);
  }

  public Vector getOutputs()
  {
    return outputs;
  }

  public void addResult(Attribute arg)
  {
    results.add(arg);
  }

  public Vector getResults()
  {
    return results;
  }

  public void addResultConfig(Object config)
  {
    resultConfigs.add(config);
  }

  public Vector getResultConfigs()
  {
    return resultConfigs;
  }

}
