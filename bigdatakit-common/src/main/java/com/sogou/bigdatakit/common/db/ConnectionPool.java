package com.sogou.bigdatakit.common.db;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.*;

/**
 * Created by Tao Li on 2016/3/18.
 */
public abstract class ConnectionPool<T> {
  private final Logger LOG = LoggerFactory.getLogger(ConnectionPool.class);

  private int initConnectionNum = DEFAULT_INIT_CONNECTION_NUM;
  private int minConnectionNum = DEFAULT_MIN_CONNECTION_NUM;
  private int maxConnectionNum = DEFAULT_MAX_CONNECTION_NUM;

  private TreeSet<PooledConnection> availableConnections;
  private TreeSet<PooledConnection> activeConnections;

  private long idleTimeout = DEFAULT_IDLE_TIMEOUT;
  private int idleQueueSize = DEFAULT_IDLE_QUEUE_SIZE;
  private BlockingQueue<T> idleConnections;
  private Runnable findIdleConnectionThread;
  private int idleConnectionCloseThreadPoolSize = DEFAULT_IDLE_CONNECTION_CLOSE_THREAD_POOL_SIZE;
  private ExecutorService idleConnectionCloseThreadPool;

  private Object lock = new Object();
  private volatile boolean isRunning = false;

  private final static int DEFAULT_INIT_CONNECTION_NUM = 1;
  private final static int DEFAULT_MIN_CONNECTION_NUM = 1;
  private final static int DEFAULT_MAX_CONNECTION_NUM = 20;
  private final static long DEFAULT_IDLE_TIMEOUT = 30 * 1000;
  private final static int DEFAULT_IDLE_QUEUE_SIZE = 20;
  private final static int DEFAULT_IDLE_CONNECTION_CLOSE_THREAD_POOL_SIZE = 10;

  private class PooledConnection implements Comparable<PooledConnection> {
    private T conn;
    private long time;

    public PooledConnection(T conn) {
      this(conn, System.currentTimeMillis());
    }

    public PooledConnection(T conn, long time) {
      this.conn = conn;
      this.time = time;
    }

    public T getConn() {
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
          T conn = idleConnections.take();
          try {
            LOG.debug("close connection: " + getPoolInfo());
            closeConnection(conn);
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

  public void setInitConnectionNum(int initConnectionNum) {
    this.initConnectionNum = initConnectionNum;
  }

  public void setMinConnectionNum(int minConnectionNum) {
    this.minConnectionNum = minConnectionNum;
  }

  public void setMaxConnectionNum(int maxConnectionNum) {
    this.maxConnectionNum = maxConnectionNum;
  }

  public void setIdleTimeout(long idleTimeout) {
    this.idleTimeout = idleTimeout;
  }

  public void setIdleQueueSize(int idleQueueSize) {
    this.idleQueueSize = idleQueueSize;
  }

  public void setIdleConnectionCloseThreadPoolSize(int idleConnectionCloseThreadPoolSize) {
    this.idleConnectionCloseThreadPoolSize = idleConnectionCloseThreadPoolSize;
  }

  public synchronized void start() throws ConnectionPoolException {
    if (isRunning) {
      throw new ConnectionPoolException("pool is already running");
    }

    isRunning = true;

    availableConnections = new TreeSet<PooledConnection>();
    activeConnections = new TreeSet<PooledConnection>();

    idleConnections = new ArrayBlockingQueue<T>(idleQueueSize, true);
    idleConnectionCloseThreadPool = Executors.newFixedThreadPool(
        idleConnectionCloseThreadPoolSize,
        new ThreadFactoryBuilder().setNameFormat("IdleConnectionCloseThread-%d").build());
    findIdleConnectionThread = new FindIdleConnectionThread();

    for (int i = 0; i < idleConnectionCloseThreadPoolSize; i++) {
      idleConnectionCloseThreadPool.submit(new IdleConnectionCloseThread());
    }

    new Thread(findIdleConnectionThread, "FindIdleConnectionThread").start();

    for (int i = 0; i < initConnectionNum; i++) {
      availableConnections.add(createPooledConnection());
    }

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        close();
      }
    });
  }

  private PooledConnection createPooledConnection() throws ConnectionPoolException {
    try {
      LOG.debug("create new connection: " + getPoolInfo());
      return new PooledConnection(createConnection());
    } catch (IOException e) {
      throw new ConnectionPoolException("fail to create connection", e);
    }
  }

  public T getConnection() throws ConnectionPoolException {
    if (!isRunning) {
      throw new ConnectionPoolException("pool is closed");
    }
    synchronized (lock) {
      LOG.debug("get connection: " + getPoolInfo());
      if (availableConnections.isEmpty()) {
        if (activeConnections.size() >= maxConnectionNum) {
          throw new ConnectionPoolException("pool is full");
        } else {
          availableConnections.add(createPooledConnection());
        }
      }
      PooledConnection pooledConnection = availableConnections.pollFirst();
      pooledConnection.setTime(System.currentTimeMillis());
      activeConnections.add(pooledConnection);
      return pooledConnection.getConn();
    }
  }

  public void releaseConnection(T conn) throws ConnectionPoolException {
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

  private void closeAllPooledConnections(TreeSet<PooledConnection> connections) {
    synchronized (lock) {
      Iterator<PooledConnection> iter = connections.iterator();
      while (iter.hasNext()) {
        PooledConnection pooledConnection = iter.next();
        try {
          closeConnection(pooledConnection.getConn());
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

    closeAllPooledConnections(availableConnections);
    closeAllPooledConnections(activeConnections);
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

  protected abstract void closeConnection(T conn) throws IOException;

  protected abstract T createConnection() throws IOException;
}