package pac.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.junit.Test;

public class ClassTest {

    @Test
    public void testForName() throws ClassNotFoundException, SecurityException, NoSuchFieldException {
        Class<?> tlmClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
        // java source has field named 'table'. Diversity is randomizing 'table' to 'tableXXXX' and hence the error.
        Field tableField = tlmClass.getDeclaredField("table");
        tableField.setAccessible(true);
    }

    /**
     * Test case to reproduce this error. Recent update, clean, build seem to have fix this issue in 
     * the junit test. However, tomcat still produces this error. So this error is caused by tomcat's
     * unique class hierarchy and sequence of method calls.
     * 
     * java.lang.VerifyError: (class: org/apache/catalina/loader/WebappClassLoader, 
     *     method: setWorkDir signature: (Ljava/io/File;)V) Incompatible type for getting or setting field
     * at java.lang.Class.forName0(Native Method)
     * at java.lang.Class.forName(Class.java:169)
     * at org.apache.catalina.loader.WebappLoader.createClassLoader(WebappLoader.java:718)
     * at org.apache.catalina.loader.WebappLoader.startInternal(WebappLoader.java:581)
     */
    @Test
    public void testClassLoad() throws Exception {
        Apploader app = new Apploader();
        app.createClassLoader();
    }

    private class Apploader {
        private String loaderClass = "pac.test.ClassTest$AppClassLoader";

        public AppClassLoader createClassLoader() throws Exception {
            @SuppressWarnings("unused")
            Class<?> clazz = Class.forName(loaderClass);
            AppClassLoader classLoader = null;
            // Class<?>[] argTypes = { ClassLoader.class };

            return classLoader;
        }
    }

    private class AppClassLoader {
        protected File loaderDir = null;
        protected String canonicalLoaderDir = null;

        @SuppressWarnings("unused")
        public void setWorkDir(File workDir) {
            this.loaderDir = new File(workDir, "loader");
            if (loaderDir == null) {
                canonicalLoaderDir = null;
            } else {
                try {
                    canonicalLoaderDir = loaderDir.getCanonicalPath();
                    if (!canonicalLoaderDir.endsWith(File.separator)) {
                        canonicalLoaderDir += File.separator;
                    }
                } catch (IOException ioe) {
                    canonicalLoaderDir = null;
                }
            }
        }
    }

}
