package com.rate.persistence.valueobject;

import java.sql.CallableStatement;
import java.sql.ResultSet;

public class DataBaseConnectionInfo
{
ResultSet resultSet;;
CallableStatement callableStatement;
public ResultSet getResultSet()
{
  return resultSet;
}
public void setResultSet(ResultSet resultSet)
{
  this.resultSet = resultSet;
}
public CallableStatement getCallableStatement()
{
  return callableStatement;
}
public void setCallableStatement(CallableStatement callableStatement)
{
  this.callableStatement = callableStatement;
}




}
