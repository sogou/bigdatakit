package com.sogou.bigdatakit.common.jdbc;

import com.sogou.bigdatakit.common.db.ConnectionPool;
import com.sogou.bigdatakit.common.db.ConnectionPoolException;

import java.io.IOException;
import java.sql.*;

/**
 * Created by Tao Li on 4/11/16.
 */
public class JDBCConnectionPool extends ConnectionPool<Connection> {
  private String url;

  public JDBCConnectionPool(String driver, String url) throws ConnectionPoolException {
    try {
      Class.forName(driver);
    } catch (ClassNotFoundException e) {
      throw new ConnectionPoolException(e);
    }
    this.url = url;
  }

  @Override
  protected Connection createConnection() throws IOException {
    try {
      return DriverManager.getConnection(url);
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  protected void closeConnection(Connection conn) throws IOException {
    try {
      conn.close();
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }
}