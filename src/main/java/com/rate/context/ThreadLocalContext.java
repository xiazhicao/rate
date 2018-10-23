package com.rate.context;

public class ThreadLocalContext
{
  public static final ThreadLocal<ICallContext> userThreadLocal = new ThreadLocal<ICallContext>();

  public static void set(ICallContext callContext)
  {
    userThreadLocal.set(callContext);
  }

  public static void unset()
  {
    userThreadLocal.remove();
  }

  public static ICallContext get()
  {
    return userThreadLocal.get();
  }
}
