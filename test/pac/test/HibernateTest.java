package pac.test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.hql.ast.QuerySyntaxException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import pac.config.CleartrackException;

public class HibernateTest // extends StatementInjectionTest
{

  private SessionFactory sessionFactory = null;
  Session s = null;

  public HibernateTest() {
    super();
    sessionFactory = getH2Session();
  }

  @Test
  public void testInjections() {
    HashMap<String, String> injectiontypes = StatementInjectionTest.getInjectionData();
    for (Map.Entry<String, String> e : injectiontypes.entrySet()) {
      String injectionType = e.getKey();
      String fileName = e.getValue();
      // System.out.println("Reading File: " + fileName);
      System.out.println("Injection Test: " + injectionType);
      List<List<String>> rows = StatementInjectionTest.readCSV(fileName, "\t");
      // s = sessionFactory.openSession();
      testHibernateInjections(rows);
      // s.close();
    }
  }

  private void testHibernateInjections(List<List<String>> rows) {
    int qcount = 0;
    for (List<String> adata : rows) {
      String func = adata.get(StatementInjectionTest.FUNCTION_INDEX);
      HashMap<String, String> params = StatementInjectionTest
          .parseFuncParams(adata.get(StatementInjectionTest.FUNCTION_PARAM_INDEX));

      if (func.equalsIgnoreCase("verifyPassword")) {
        String name = params.get("name");
        String pass = params.get("password");

        ++qcount;
        if (qcount % 400 == 0) {
          System.out.println("\n Q Count: " + qcount);
        }

        String message = "Password query returned a result for a bogus password.";
        String resultString = null;

        try {
          resultString = verifyPasswordHql(name, pass);
        } catch (CleartrackException e) {

        } catch (SQLException e) {
          // TODO Auto-generated catch block
          // e.printStackTrace();
        }

        if (resultString != null) {
          // System.out.println("name=" + name);
          // System.out.println("password=" + pass);
          // System.out.println("resultString=" + resultString);
          message += "\nname=" + name + "\n";
          message += "password=" + pass + "\n";
          message += "resultString=" + resultString;
        }
        Assert.assertNull(message, resultString);

        // message = "Password query returned a result for a bogus password.";

        /*
         * try {
         *   resultString = new VerifyPasswordNoQuotesHibernate().verifyPasswords(name, pass);
         * } catch (SQLException e) {
         *   // TODO Auto-generated catch block
         *   // e.printStackTrace();
         * }
         * 
         * if(resultString != null) {
         *   // System.out.println("name=" + name);
         *   // System.out.println("password=" + pass);
         *   // System.out.println("resultString=" + resultString);
         *   message += "\nname=" + name + "\n";
         *   message += "password=" + pass + "\n";
         *   message += "resultString=" + resultString;
         * }
         * Assert.assertNull(message,resultString);
         */
      }
    }
  }

  public String verifyPasswordHql(String name, String password) throws SQLException {
    /*
     * System.out.println("Hibernate verifyPassword:\n name = " + name + "\npassword = " +
     *                    password);
     * Session s = sessionFactory.openSession();
     * Query q = s.createQuery("from Employee where name = '" + name + "' and password='" +
     *                         password + "'");
     * List<Employee> elist = (List<Employee>) q.list();
     * s.close();
     * if(TaintValues.isTainted(name)) {
     *   System.out.println("TAINTED name = " + name);
     * }
     * 
     * if(TaintValues.isTainted(password)) {
     *   System.out.println("TAINTED password = " + password);
     * }
     */

    List<Employee> elist =
        query("from Employee where name = '" + name + "' and password='" + password + "'");
    String retValue = "";
    for (Employee e : elist) {
      // System.out.println(e);
      retValue += e.getId();
    }
    if (elist.size() == 0)
      return null;

    return retValue;
  }

  @Before
  public void initSession() {
    s = sessionFactory.openSession();
  }

  @After
  public void closeSession() {
    try {
      s.close();
    } catch (HibernateException e) {
      System.out.println("could not close hibernate connection.");
    }
  }

  @Test
  public void dumpDB() throws SQLException {
    // Server server = Server.createWebServer().start();
    // //Server.createWebServer(new String[] { "-trace" }).start();
    // sessionFactory = getH2Session();
    List<Employee> el = query("from Employee");
    for (Employee e : el) {
      System.out.println(e);
    }
    // server.stop();
  }

  @SuppressWarnings("unchecked")
  private List<Employee> query(String query) {

    List<Employee> elist = new ArrayList<Employee>();
    Query q = null;
    try {
      q = s.createQuery(query);
    } catch (SQLGrammarException e) {
      System.out.println("Exception: " + e.getMessage());
    } catch (QuerySyntaxException e) {
      System.out.println("Exception: " + e.getMessage());
    } catch (QueryException e) {
      System.out.println("Exception: " + e.getMessage());
      // System.out.println("qe class: " + e.getClass().getCanonicalName());
    } catch (GenericJDBCException e) { // A problem occurred translating a Hibernate query to SQL
                                       // due to invalid query syntax, etc...
      System.out.println("Exception: " + e.getMessage());
      // System.out.println("class " + e.getClass().getCanonicalName());
      // System.out.println("Exception caught when executing hql: " + query);
      // System.out.println(e.getMessage());
      // System.out.println();
    } catch (CleartrackException sse) {
      // If diversity is enabled, we may see this exception.
      throw sse;
    } catch (RuntimeException rte) {
      System.out.println("Exception: " + rte.getMessage());
    }
    if (q != null) {
      try {
        elist = (List<Employee>) q.list();
      } catch (HibernateException e) {
        System.out.println("hibernate q.list exception: " + e.getMessage());
      }

    }
    return elist;
  }

  static public SessionFactory getH2Session() {
    Configuration config = new Configuration();
    config.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
    config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
    config.setProperty("hibernate.connection.url", "jdbc:h2:~/test"); // + ";MVCC=TRUE"
    config.setProperty("hibernate.connection.username", "sa");
    config.setProperty("hibernate.connection.password", "");
    config.setProperty("hibernate.connection.pool_size", "1");
    config.setProperty("hibernate.connection.autocommit", "true");
    // We need to figure out a way to turn off hibernate logging completely.

    // Add your mapped classes here:
    config.addAnnotatedClass(Employee.class);

    return config.buildSessionFactory();
  }
}
