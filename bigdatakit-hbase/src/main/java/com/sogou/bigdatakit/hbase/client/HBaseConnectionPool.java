package com.sogou.bigdatakit.hbase.client;

import com.sogou.bigdatakit.common.db.ConnectionPool;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;

import java.io.IOException;

/**
 * Created by Tao Li on 4/11/16.
 */
public class HBaseConnectionPool extends ConnectionPool<HConnection> {
  private Configuration conf;

  public HBaseConnectionPool(Configuration conf) {
    this.conf = conf;
  }

  @Override
  protected HConnection createConnection() throws IOException {
    return HConnectionManager.createConnection(conf);
  }

  @Override
  protected void closeConnection(HConnection conn) throws IOException {
    conn.close();
  }
}
