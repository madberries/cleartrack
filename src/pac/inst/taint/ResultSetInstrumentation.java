package pac.inst.taint;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;

import pac.config.BaseConfig;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;
import pac.wrap.ByteArrayTaint;

@InstrumentationClass(value = "java/sql/ResultSet", isInterface = true)
public final class ResultSetInstrumentation {

    @InstrumentationMethod
    public static final URL getURL(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret) throws SQLException {
        URL url = rs.getURL(columnIndex, columnIndex_t, ret);
        if (url == null)
            return null;
        if (BaseConfig.getInstance().taintDbReads())
            URLInstrumentation.taintURL(url, TaintValues.DATABASE);
        else
            URLInstrumentation.trustURL(url, TaintValues.DATABASE);
        return url;
    }

    @InstrumentationMethod
    public static final URL getURL(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        URL url = rs.getURL(columnLabel, ret);
        if (url == null)
            return null;
        if (BaseConfig.getInstance().taintDbReads())
            URLInstrumentation.taintURL(url, TaintValues.DATABASE);
        else
            URLInstrumentation.trustURL(url, TaintValues.DATABASE);
        return url;
    }

    @InstrumentationMethod
    public static final String getString(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        String str = rs.getString(columnLabel, ret);
        if (str == null)
            return null;
        if (BaseConfig.getInstance().taintDbReads())
            TaintUtils.taint(str, TaintValues.DATABASE);
        else
            TaintUtils.trust(str, TaintValues.DATABASE);
        return str;
    }

    @InstrumentationMethod
    public static final String getString(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret)
            throws SQLException {
        String str = rs.getString(columnIndex, columnIndex_t, ret);
        if (str == null)
            return null;
        if (BaseConfig.getInstance().taintDbReads())
            TaintUtils.taint(str, TaintValues.DATABASE);
        else
            TaintUtils.trust(str, TaintValues.DATABASE);
        return str;
    }

    @InstrumentationMethod
    public static final InputStream getAsciiStream(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret)
            throws SQLException {
        InputStream in = rs.getAsciiStream(columnIndex, columnIndex_t, ret);
        if (in == null)
            return null;
        in.ss_hasUniformTaint = true;
        in.ss_taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return in;
    }

    @InstrumentationMethod
    public static final InputStream getAsciiStream(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        InputStream in = rs.getAsciiStream(columnLabel, ret);
        if (in == null)
            return null;
        in.ss_hasUniformTaint = true;
        in.ss_taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return in;
    }

    @Deprecated
    @InstrumentationMethod
    public static final InputStream getUnicodeStream(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        InputStream in = rs.getUnicodeStream(columnLabel, ret);
        if (in == null)
            return null;
        in.ss_hasUniformTaint = true;
        in.ss_taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return in;
    }

    @Deprecated
    @InstrumentationMethod
    public static final InputStream getUnicodeStream(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret)
            throws SQLException {
        InputStream in = rs.getUnicodeStream(columnIndex, columnIndex_t, ret);
        if (in == null)
            return null;
        in.ss_hasUniformTaint = true;
        in.ss_taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return in;
    }

    @InstrumentationMethod
    public static final InputStream getBinaryStream(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        InputStream in = rs.getBinaryStream(columnLabel, ret);
        if (in == null)
            return null;
        in.ss_hasUniformTaint = true;
        in.ss_taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return in;
    }

    @InstrumentationMethod
    public static final InputStream getBinaryStream(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret)
            throws SQLException {
        InputStream in = rs.getBinaryStream(columnIndex, columnIndex_t, ret);
        if (in == null)
            return null;
        in.ss_hasUniformTaint = true;
        in.ss_taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return in;
    }

    @InstrumentationMethod
    public static final Reader getCharacterStream(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret)
            throws SQLException {
        Reader reader = rs.getCharacterStream(columnIndex, columnIndex_t, ret);
        if (reader == null)
            return null;
        reader.ss_hasUniformTaint = true;
        reader.ss_taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return reader;
    }

    @InstrumentationMethod
    public static final Reader getCharacterStream(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        Reader reader = rs.getCharacterStream(columnLabel, ret);
        if (reader == null)
            return null;
        reader.ss_hasUniformTaint = true;
        reader.ss_taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return reader;
    }

    @InstrumentationMethod
    public static final String getNString(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        String str = rs.getNString(columnLabel, ret);
        if (str == null)
            return null;
        if (BaseConfig.getInstance().taintDbReads())
            TaintUtils.taint(str, TaintValues.DATABASE);
        else
            TaintUtils.trust(str, TaintValues.DATABASE);
        return str;
    }

    @InstrumentationMethod
    public static final String getNString(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret)
            throws SQLException {
        String str = rs.getNString(columnIndex, columnIndex_t, ret);
        if (str == null)
            return null;
        if (BaseConfig.getInstance().taintDbReads())
            TaintUtils.taint(str, TaintValues.DATABASE);
        else
            TaintUtils.trust(str, TaintValues.DATABASE);
        return str;
    }

    @InstrumentationMethod
    public static final Reader getNCharacterStream(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret)
            throws SQLException {
        Reader reader = rs.getNCharacterStream(columnIndex, columnIndex_t, ret);
        if (reader == null)
            return null;
        reader.ss_hasUniformTaint = true;
        reader.ss_taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return reader;
    }

    @InstrumentationMethod
    public static final Reader getNCharacterStream(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        Reader reader = rs.getNCharacterStream(columnLabel, ret);
        if (reader == null)
            return null;
        reader.ss_hasUniformTaint = true;
        reader.ss_taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return reader;
    }

    @InstrumentationMethod
    public static final ByteArrayTaint getBytes(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret)
            throws SQLException {
        ByteArrayTaint baTaint = rs.getBytes(columnIndex, columnIndex_t, ret);
        if (baTaint == null)
            return null;
        if (BaseConfig.getInstance().taintDbReads())
            ByteArrayTaint.taint(baTaint, TaintValues.DATABASE);
        else
            ByteArrayTaint.trust(baTaint, TaintValues.DATABASE);
        return baTaint;
    }

    @InstrumentationMethod
    public static final ByteArrayTaint getBytes(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        ByteArrayTaint baTaint = rs.getBytes(columnLabel, ret);
        if (baTaint == null)
            return null;
        if (BaseConfig.getInstance().taintDbReads())
            ByteArrayTaint.taint(baTaint, TaintValues.DATABASE);
        else
            ByteArrayTaint.trust(baTaint, TaintValues.DATABASE);
        return baTaint;
    }

    @InstrumentationMethod
    public static final int getInt(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret) throws SQLException {
        int res = rs.getInt(columnIndex, columnIndex_t, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final int getInt(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        int res = rs.getInt(columnLabel, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final long getLong(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret) throws SQLException {
        long res = rs.getLong(columnIndex, columnIndex_t, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final long getLong(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        long res = rs.getLong(columnLabel, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final float getFloat(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret) throws SQLException {
        float res = rs.getFloat(columnIndex, columnIndex_t, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final float getFloat(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        float res = rs.getFloat(columnLabel, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final byte getByte(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret) throws SQLException {
        byte res = rs.getByte(columnIndex, columnIndex_t, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final byte getByte(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        byte res = rs.getByte(columnLabel, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final boolean getBoolean(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret)
            throws SQLException {
        boolean res = rs.getBoolean(columnIndex, columnIndex_t, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final boolean getBoolean(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        boolean res = rs.getBoolean(columnLabel, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final double getDouble(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret)
            throws SQLException {
        double res = rs.getDouble(columnIndex, columnIndex_t, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final double getDouble(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        double res = rs.getDouble(columnLabel, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final short getShort(ResultSet rs, int columnIndex, int columnIndex_t, Ret ret) throws SQLException {
        short res = rs.getShort(columnIndex, columnIndex_t, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }

    @InstrumentationMethod
    public static final short getShort(ResultSet rs, String columnLabel, Ret ret) throws SQLException {
        short res = rs.getShort(columnLabel, ret);
        ret.taint = (BaseConfig.getInstance().taintDbReads() ? TaintValues.TAINTED : TaintValues.TRUSTED)
                | TaintValues.DATABASE;
        return res;
    }
    
}
