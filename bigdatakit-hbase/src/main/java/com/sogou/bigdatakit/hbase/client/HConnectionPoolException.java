package com.sogou.bigdatakit.hbase.client;

/**
 * Created by Tao Li on 2016/3/21.
 */
public class HConnectionPoolException extends Exception {
  public HConnectionPoolException(String message) {
    super(message);
  }

  public HConnectionPoolException(String message, Throwable cause) {
    super(message, cause);
  }

  public HConnectionPoolException(Throwable cause) {
    super(cause);
  }
}
