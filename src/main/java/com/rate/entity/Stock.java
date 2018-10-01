package com.rate.entity;

import java.sql.Timestamp;


public class Stock
{
	private String name;
	private Timestamp added;
	private Double price;
	private Integer volume;
	
  public String getName()
  {
  	return name;
  }
	
  public void setName(String name)
  {
  	this.name = name;
  }
	
  public Timestamp getAdded()
  {
  	return added;
  }
	
  public void setAdded(Timestamp added)
  {
  	this.added = added;
  }
	
  public Double getPrice()
  {
  	return price;
  }
	
  public void setPrice(Double price)
  {
  	this.price = price;
  }
	
  public Integer getVolume()
  {
  	return volume;
  }
	
  public void setVolume(Integer volume)
  {
  	this.volume = volume;
  }
}
