package pac.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import pac.config.NotifyMsg;
import pac.config.CleartrackException;
import pac.config.Tokenizer;
import pac.config.Tokenizer.Tokenized;
import pac.config.BaseConfig;
import pac.util.TaintUtils;

public class TokenizerTest {

    @Test
    public void escapeQuotesTest() throws IOException {
        Tokenizer tkz = BaseConfig.getInstance().getTokenizer("sqlTokenizer");

        // Ensure that we are tokenizing correctly for entirely trusted strings...
        String value = "1' OR '1'='1";
        String qry = "SELECT * FROM table WHERE x='" + value + "' AND test='1';";
        compareTokens(tkz, qry, qry, new String[] { "SELECT", "*", "FROM", "table", "WHERE", "x", "=", "'1'", "OR",
                "'1'", "=", "'1'", "AND", "test", "=", "'1'", ";" });

        // Ensure that we are tokenizing correctly for mixed trusted/tainted string inputs...
        value = "1' OR '1'='1'; --";
        TaintUtils.taint(value);
        qry = "SELECT * FROM table WHERE x='" + value + "' AND test='1';";
        compareTokens(tkz, qry, "SELECT * FROM table WHERE x='1'' OR ''1''=''1''; --' AND test='1';",
                      new String[] { "SELECT", "*", "FROM", "table", "WHERE", "x", "=", "'1' OR '1'='1'; --'", "AND",
                              "test", "=", "'1'", ";" });

        // Ensure that an entirely tainted string causes a cleartrack exception
        TaintUtils.taint(qry);
        boolean pass = false;
        try {
            compareTokens(tkz, qry, qry,
                          new String[] { "SELECT * FROM table WHERE x='1' OR '1'='1'; --' AND test='1';" });
        } catch (CleartrackException e) {
            pass = true;
        }
        Assert.assertTrue("entirely tainted query should have thrown a cleartrack exception", pass);

        // Ensure that we are tokenizing correctly for mixed trusted/tainted non-string inputs...
        String trailingStmt = "x OR 1=1";
        TaintUtils.taint(trailingStmt);
        qry = "SELECT * FROM table WHERE x=" + trailingStmt + ";";
        compareTokens(tkz, qry, "SELECT * FROM table WHERE x=x OR 1=1;",
                      new String[] { "SELECT", "*", "FROM", "table", "WHERE", "x", "=", "x OR 1=1", ";" });
    }

    @Test
    public void selfEscapedQuoteTest() throws IOException {
        Tokenizer tkz = BaseConfig.getInstance().getTokenizer("sqlTokenizer");

        // Ensure that we are tokenizing correctly for entirely trusted strings...
        String value = "1'' OR ''1''=''1";
        String qry = "SELECT * FROM table WHERE x='" + value + "' AND test='1';";
        compareTokens(tkz, qry, qry, new String[] { "SELECT", "*", "FROM", "table", "WHERE", "x", "=",
                "'1'' OR ''1''=''1'", "AND", "test", "=", "'1'", ";" });

        // Ensure that we are tokenizing correctly for mixed trusted/tainted string inputs...
        value = "1'' OR ''1''=''1'; --";
        TaintUtils.taint(value);
        qry = "SELECT * FROM table WHERE x='" + value + "' AND test='1';";
        compareTokens(tkz, qry, "SELECT * FROM table WHERE x='1'' OR ''1''=''1''; --' AND test='1';",
                      new String[] { "SELECT", "*", "FROM", "table", "WHERE", "x", "=", "'1'' OR ''1''=''1'; --'",
                              "AND", "test", "=", "'1'", ";" });

        // Ensure that an entirely tainted string causes a cleartrack exception
        TaintUtils.taint(qry);
        boolean pass = false;
        try {
            compareTokens(tkz, qry, qry,
                          new String[] { "SELECT * FROM table WHERE x='1'' OR ''1''=''1'; --' AND test='1';" });
        } catch (CleartrackException e) {
            pass = true;
        }
        Assert.assertTrue("entirely tainted query should have thrown a cleartrack exception", pass);
    }

    @Test
    public void backslashEscapeTest() throws IOException {
        Tokenizer tkz = BaseConfig.getInstance().getTokenizer("sqlTokenizer");

        String value = "1\\' OR \\'1\\'=\\'1'; --";
        TaintUtils.taint(value);
        String qry = "SELECT * FROM table WHERE x='" + value + "' AND test='1';";
        compareTokens(tkz, qry, "SELECT * FROM table WHERE x='1'' OR ''1''=''1''; --' AND test='1';",
                      new String[] { "SELECT", "*", "FROM", "table", "WHERE", "x", "=", "'1\\' OR \\'1\\'=\\'1'; --'",
                              "AND", "test", "=", "'1'", ";" });

        String usr = "fakeuser";
        String pass = "Demo12345\\' UNION ALL SELECT NULL\\-\\- AND \\'AOEC\\' LIKE \\'AOEC";
        TaintUtils.taint(usr);
        TaintUtils.taint(pass);
        qry = "SELECT id FROM employees WHERE name='" + usr + "' AND password='" + pass + "';";
        compareTokens(tkz, qry,
                      "SELECT id FROM employees WHERE name='fakeuser' AND "
                              + "password='Demo12345'' UNION ALL SELECT NULL\\-\\- AND ''AOEC'' LIKE ''AOEC';",
                      new String[] { "SELECT", "id", "FROM", "employees", "WHERE", "name", "=", "'fakeuser'", "AND",
                              "password", "=", "'Demo12345\\' UNION ALL SELECT NULL\\-\\- AND \\'AOEC\\' LIKE \\'AOEC'",
                              ";" });

        usr = "fakeuser";
        pass = "Demo12345\\\"\\\"\' or 1 \\= 1 \\-\\- \\'\\'\\(\\(\\\"\\)";
        TaintUtils.taint(usr);
        TaintUtils.taint(pass);
        qry = "SELECT id FROM employees WHERE name='" + usr + "' " + "AND password='" + pass + "';";
        compareTokens(tkz, qry,
                      "SELECT id FROM employees WHERE name='fakeuser' AND password='Demo12345\"\"'' or 1 \\= 1 \\-\\- ''''\\(\\(\"\\)';",
                      new String[] { "SELECT", "id", "FROM", "employees", "WHERE", "name", "=", "'fakeuser'", "AND",
                              "password", "=", "'Demo12345\\\"\\\"\' or 1 \\= 1 \\-\\- \\'\\'\\(\\(\\\"\\)'", ";" });
    }

    private void compareTokens(Tokenizer tkz, String qry, String expectedResult, String[] expectedTokens)
            throws IOException {
        Tokenized tokenized = tkz.tokenize(new String(qry),
                                           new NotifyMsg("Tokenize(String)", "Tokenize(" + qry + ")", 89));
        String result = tokenized.escapeQuotes();
        Assert.assertEquals("The tokenizer failed to escape tainted quotes correctly", expectedResult, result);
        int i;
        for (i = 0; i < expectedTokens.length; i++) {
            String tkn = tokenized.getTokenAt(i);
            Assert.assertNotNull("Epecting more tokens than received", tkn);
            String expected = expectedTokens[i];
            Assert.assertEquals("Expected token '" + expected + "' at index " + i + ", but received '" + tkn, expected,
                                tkn);
        }
        Assert.assertNull("Expecting less tokens than received", tokenized.getTokenAt(i));
    }
    
}
