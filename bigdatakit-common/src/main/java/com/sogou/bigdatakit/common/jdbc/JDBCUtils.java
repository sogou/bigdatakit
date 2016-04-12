package com.sogou.bigdatakit.common.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Tao Li on 4/12/16.
 */
public class JDBCUtils {

  private static class ExecuteQueryTask implements Callable<ResultSet> {
    private PreparedStatement stmt;

    public ExecuteQueryTask(PreparedStatement stmt) {
      this.stmt = stmt;
    }

    @Override
    public ResultSet call() throws Exception {
      return stmt.executeQuery();
    }
  }

  public static Future<ResultSet> asyncExecuteQuery(PreparedStatement stmt) {
    ExecutorService service = Executors.newFixedThreadPool(1);
    try {
      return asyncExecuteQuery(service, stmt);
    } finally {
      service.shutdown();
    }
  }

  public static List<Future<ResultSet>> asyncExecuteQuerys(PreparedStatement... stmts) {
    ExecutorService service = Executors.newFixedThreadPool(stmts.length);
    try {
      return asyncExecuteQuerys(service, stmts);
    } finally {
      service.shutdown();
    }
  }

  public static Future<ResultSet> asyncExecuteQuery(ExecutorService service, PreparedStatement stmt) {
    return service.submit(new ExecuteQueryTask(stmt));
  }

  public static List<Future<ResultSet>> asyncExecuteQuerys(ExecutorService service, PreparedStatement... stmts) {
    List<Future<ResultSet>> futures = new ArrayList<>();
    for (PreparedStatement stmt : stmts) {
      futures.add(service.submit(new ExecuteQueryTask(stmt)));
    }
    return futures;
  }
}
