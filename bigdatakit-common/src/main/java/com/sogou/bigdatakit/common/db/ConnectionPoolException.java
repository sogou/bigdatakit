package com.sogou.bigdatakit.common.db;

/**
 * Created by Tao Li on 2016/3/21.
 */
public class ConnectionPoolException extends Exception {
  public ConnectionPoolException(String message) {
    super(message);
  }

  public ConnectionPoolException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConnectionPoolException(Throwable cause) {
    super(cause);
  }
}
