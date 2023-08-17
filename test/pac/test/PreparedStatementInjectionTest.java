package pac.test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PreparedStatementInjectionTest extends StatementInjectionTest {

  @Override
  public int queryNumberOfEmployees() throws SQLException {
    int result;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
      stmt = connection.prepareStatement("SELECT COUNT(*) FROM " + TEST_TABLE_NAME);
      rs = stmt.executeQuery();
      if (!rs.next()) {
        throw new RuntimeException("No result set for query");
      }
      result = rs.getInt(1);
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (stmt != null) {
        stmt.close();
      }
    }
    return result;
  }

  @Override
  public String lookupEmail(String TEST_NAME) throws SQLException {
    String resultString = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.prepareStatement("SELECT email FROM " + TEST_TABLE_NAME + " WHERE name=?");
      stmt.setString(1, TEST_NAME);
      boolean result = stmt.execute();
      if (result) {
        rs = stmt.getResultSet();
        if (rs.next()) {
          resultString = rs.getString(1);
        }
      }
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (stmt != null) {
        stmt.close();
      }
    }
    return resultString;
  }

  @Override
  public boolean queryHasEmployee(String TEST_NAME) throws SQLException {
    boolean result;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.prepareStatement("SELECT * FROM " + TEST_TABLE_NAME + " WHERE name=?;");
      stmt.setString(1, TEST_NAME);
      rs = stmt.executeQuery();
      result = rs.next();
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (stmt != null) {
        stmt.close();
      }
    }
    return result;
  }

  @Override
  public String verifyPassword(String TEST_NAME, String TEST_PASSWORD) throws SQLException {
    String resultString = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
      stmt = connection.prepareStatement("SELECT id FROM " + TEST_TABLE_NAME + " WHERE name='"
          + TEST_NAME + "' AND password='" + TEST_PASSWORD + "';");
      rs = stmt.executeQuery();
      if (rs.next()) {
        resultString = rs.getString(1);
      }
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (stmt != null) {
        stmt.close();
      }
    }
    return resultString;
  }

}
