package com.sogou.bigdatakit.hbase.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.*;

/**
 * Created by Tao Li on 2016/3/18.
 */
public class HConnectionPool {
  private final Logger LOG = LoggerFactory.getLogger(HConnectionPool.class);

  private final Configuration conf;
  private final int initConnectionNum;
  private final int minConnectionNum;
  private final int maxConnectionNum;

  private final TreeSet<PooledConnection> availableConnections;
  private final TreeSet<PooledConnection> activeConnections;

  private final long idleTimeout;
  private final int idleQueueSize;
  private final BlockingQueue<HConnection> idleConnections;
  private final Runnable findIdleConnectionThread;
  private final int idleConnectionCloseThreadPoolSize;
  private final ExecutorService idleConnectionCloseThreadPool;

  private final Object lock = new Object();
  private volatile boolean isRunning = false;

  private final static int DEFAULT_INIT_CONNECTION_NUM = 1;
  private final static int DEFAULT_MIN_CONNECTION_NUM = 1;
  private final static int DEFAULT_MAX_CONNECTION_NUM = 20;
  private final static long DEFAULT_IDLE_TIMEOUT = 30 * 1000;
  private final static int DEFAULT_IDLE_QUEUE_SIZE = 20;
  private final static int DEFAULT_IDLE_CONNECTION_CLOSE_THREAD_POOL_SIZE = 10;

  private static class PooledConnection implements Comparable<PooledConnection> {
    private HConnection conn;
    private long time;

    public PooledConnection(HConnection conn) {
      this(conn, System.currentTimeMillis());
    }

    public PooledConnection(HConnection conn, long time) {
      this.conn = conn;
      this.time = time;
    }

    public HConnection getConn() {
      return conn;
    }

    public long getTime() {
      return time;
    }

    public void setTime(long time) {
      this.time = time;
    }

    public int compareTo(PooledConnection o) {
      return time >= o.time ? 1 : -1;
    }

    @Override
    public boolean equals(Object obj) {
      return super.equals(obj);
    }
  }

  private class FindIdleConnectionThread implements Runnable {
    private final long CHECK_INTERVAL = 5;

    @Override
    public void run() {
      while (isRunning && !Thread.currentThread().isInterrupted()) {
        synchronized (lock) {
          long now = System.currentTimeMillis();
          Iterator<PooledConnection> iter = availableConnections.iterator();
          while (iter.hasNext()) {
            PooledConnection pooledConnection = iter.next();
            if (availableConnections.size() > minConnectionNum &&
                now - pooledConnection.getTime() > idleTimeout &&
                idleConnections.size() < idleQueueSize) {
              try {
                LOG.debug("find idle connection: " + getPoolInfo());
                idleConnections.put(pooledConnection.getConn());
                iter.remove();
              } catch (InterruptedException e) {
                LOG.warn("interrupted", e);
                Thread.currentThread().interrupt();
              }
            }
          }
        }

        try {
          TimeUnit.SECONDS.sleep(CHECK_INTERVAL);
        } catch (InterruptedException e) {
          LOG.warn("interrupted", e);
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private class IdleConnectionCloseThread implements Runnable {

    @Override
    public void run() {
      while (isRunning && !Thread.currentThread().isInterrupted()) {
        try {
          HConnection conn = idleConnections.take();
          try {
            LOG.debug("close connection: " + getPoolInfo());
            conn.close();
          } catch (Exception e) {
            LOG.error("fail to close connection", e);
          }
        } catch (InterruptedException e) {
          LOG.warn("interrupted", e);
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  public static class Builder {
    private Configuration conf;
    private int initConnectionNum;
    private int minConnectionNum;
    private int maxConnectionNum;
    private long idleTimeout;
    private int idleQueueSize;
    private int idleConnectionCloseThreadPoolSize;

    public Builder(Configuration conf) {
      this.conf = conf;
      this.initConnectionNum = DEFAULT_INIT_CONNECTION_NUM;
      this.minConnectionNum = DEFAULT_MIN_CONNECTION_NUM;
      this.maxConnectionNum = DEFAULT_MAX_CONNECTION_NUM;
      this.idleTimeout = DEFAULT_IDLE_TIMEOUT;
      this.idleQueueSize = DEFAULT_IDLE_QUEUE_SIZE;
      this.idleConnectionCloseThreadPoolSize = DEFAULT_IDLE_CONNECTION_CLOSE_THREAD_POOL_SIZE;
    }

    public Builder initConnectionNum(int initConnectionNum) {
      this.initConnectionNum = initConnectionNum;
      return this;
    }

    public Builder minConnectionNum(int minConnectionNum) {
      this.minConnectionNum = minConnectionNum;
      return this;
    }

    public Builder maxConnectionNum(int maxConnectionNum) {
      this.maxConnectionNum = maxConnectionNum;
      return this;
    }

    public Builder idleTimeout(long idleTimeout) {
      this.idleTimeout = idleTimeout;
      return this;
    }

    public Builder idleQueueSize(int idleQueueSize) {
      this.idleQueueSize = idleQueueSize;
      return this;
    }

    public Builder idleConnectionCloseThreadPoolSize(int idleConnectionCloseThreadPoolSize) {
      this.idleConnectionCloseThreadPoolSize = idleConnectionCloseThreadPoolSize;
      return this;
    }

    public HConnectionPool build() {
      return new HConnectionPool(this);
    }
  }

  public HConnectionPool(Configuration conf) {
    this(new Builder(conf));
  }

  public HConnectionPool(Builder builder) {
    conf = builder.conf;
    initConnectionNum = builder.initConnectionNum;
    minConnectionNum = builder.minConnectionNum;
    maxConnectionNum = builder.maxConnectionNum;
    idleTimeout = builder.idleTimeout;
    idleQueueSize = builder.idleQueueSize;
    idleConnectionCloseThreadPoolSize = builder.idleConnectionCloseThreadPoolSize;
    assert maxConnectionNum >= initConnectionNum && maxConnectionNum >= minConnectionNum;

    availableConnections = new TreeSet<PooledConnection>();
    activeConnections = new TreeSet<PooledConnection>();

    idleConnections = new ArrayBlockingQueue<HConnection>(idleQueueSize, true);
    idleConnectionCloseThreadPool = Executors.newFixedThreadPool(
        idleConnectionCloseThreadPoolSize,
        new ThreadFactoryBuilder().setNameFormat("IdleConnectionCloseThread-%d").build());
    findIdleConnectionThread = new FindIdleConnectionThread();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        close();
      }
    });
  }

  public synchronized void start() throws HConnectionPoolException {
    if (isRunning) {
      throw new HConnectionPoolException("pool is already running");
    }

    isRunning = true;

    for (int i = 0; i < idleConnectionCloseThreadPoolSize; i++) {
      idleConnectionCloseThreadPool.submit(new IdleConnectionCloseThread());
    }

    new Thread(findIdleConnectionThread, "FindIdleConnectionThread").start();

    for (int i = 0; i < initConnectionNum; i++) {
      availableConnections.add(createConnection());
    }
  }

  private PooledConnection createConnection() throws HConnectionPoolException {
    try {
      LOG.debug("create new connection: " + getPoolInfo());
      return new PooledConnection(HConnectionManager.createConnection(conf));
    } catch (IOException e) {
      throw new HConnectionPoolException("fail to create connection", e);
    }
  }

  public HConnection getConnection() throws HConnectionPoolException {
    if (!isRunning) {
      throw new HConnectionPoolException("pool is closed");
    }
    synchronized (lock) {
      LOG.debug("get connection: " + getPoolInfo());
      if (availableConnections.isEmpty()) {
        if (activeConnections.size() >= maxConnectionNum) {
          throw new HConnectionPoolException("pool is full");
        } else {
          availableConnections.add(createConnection());
        }
      }
      PooledConnection pooledConnection = availableConnections.pollFirst();
      pooledConnection.setTime(System.currentTimeMillis());
      activeConnections.add(pooledConnection);
      return pooledConnection.getConn();
    }
  }

  public void releaseConnection(HConnection conn) throws HConnectionPoolException {
    if (conn == null) {
      return;
    }
    synchronized (lock) {
      LOG.debug("release connection: " + getPoolInfo());
      Iterator<PooledConnection> iter = activeConnections.iterator();
      while (iter.hasNext()) {
        PooledConnection pooledConnection = iter.next();
        if (pooledConnection.getConn() == conn) {
          pooledConnection.setTime(System.currentTimeMillis());
          availableConnections.add(pooledConnection);
          iter.remove();
        }
      }
    }
  }

  private void closeAllConnections(TreeSet<PooledConnection> connections) {
    synchronized (lock) {
      Iterator<PooledConnection> iter = connections.iterator();
      while (iter.hasNext()) {
        PooledConnection pooledConnection = iter.next();
        try {
          pooledConnection.getConn().close();
        } catch (IOException e) {
          // ignore
        }
        iter.remove();
      }
    }
  }

  public synchronized void close() {
    if (!isRunning) {
      return;
    }

    closeAllConnections(availableConnections);
    closeAllConnections(activeConnections);
    isRunning = false;
  }

  public String getPoolInfo() {
    if (availableConnections == null || activeConnections == null || idleConnections == null) {
      return null;
    }
    return "(available: " + availableConnections.size() +
        ", active: " + activeConnections.size() +
        ", idle: " + idleConnections.size() + ")";
  }
}