package fr.ird.osmose.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class Properties extends java.util.Properties {

    @Override
    public String getProperty(String key) {
        String value = super.getProperty(key);
        /* If we've found a String expression then replace
         * any ${key} variables, and then reset the
         * the original resourceMapNode entry.
         */
        if ((null != value) && ((String) value).contains("${")) {
            try {
                value = evaluateStringExpression((String) value);
            } catch (IOException ex) {
                Logger.getLogger(Properties.class.getName()).log(Level.SEVERE, null, ex);
            }
            setProperty(key, value);
        }
        return value;
    }

    public String[] getProperties(String key) {
        String value = getProperty(key);
        if ((null != value)) {
            return value.split(",");
        } else {
            return null;
        }
    }

    /**
     * This key filter only uses shell meta-characters.
     * It accepts the following meta-character: "?" for any single character
     * and "*" for any String.
     */
    public List<String> getKeys(String filter) {
        /*
         * Add \Q \E around substrings of fileMask that are not meta-characters
         */
        String regexpPattern = filter.replaceAll("[^\\*\\?]+", "\\\\Q$0\\\\E");
        /*
         * Replace all "*" by the corresponding java regex meta-characters
         */
        regexpPattern = regexpPattern.replaceAll("\\*", ".*");
        /*
         * Replace all "?" by the corresponding java regex meta-characters
         */
        regexpPattern = regexpPattern.replaceAll("\\?", ".");
        /*
         * List the keys and select the ones that match the filter
         */
        List<String> filteredKeys = new ArrayList();
        Enumeration keys = this.keys();
        while(keys.hasMoreElements()) {
            String key = String.valueOf(keys.nextElement());
            if (key.matches(regexpPattern)) {
                filteredKeys.add(key);
            }
        }
        return filteredKeys;
    }

    /* Given the following resources:
     * 
     * hello = Hello
     * world = World
     * place = ${world}
     * 
     * The value of evaluateStringExpression("${hello} ${place}")
     * would be "Hello World".  The value of ${null} is null.
     */
    private String evaluateStringExpression(String expr) throws IOException {
        if (expr.trim().equals("${null}")) {
            return null;
        }
        StringBuilder value = new StringBuilder();
        int i0 = 0, i1;
        while ((i1 = expr.indexOf("${", i0)) != -1) {
            if ((i1 == 0) || ((i1 > 0) && (expr.charAt(i1 - 1) != '\\'))) {
                int i2 = expr.indexOf("}", i1);
                if ((i2 != -1) && (i2 > i1 + 2)) {
                    String k = expr.substring(i1 + 2, i2);
                    String v = evaluateStringExpression(k);
                    value.append(expr.substring(i0, i1));
                    if (v != null) {
                        value.append(v);
                    } else {
                        String msg = String.format("no value for \"%s\" in \"%s\"", k, expr);
                        throw new IOException(msg);
                    }
                    i0 = i2 + 1;  // skip trailing "}"
                } else {
                    String msg = String.format("no closing brace in \"%s\"", expr);
                    throw new IOException(msg);
                }
            } else {  // we've found an escaped variable - "\${"
                value.append(expr.substring(i0, i1 - 1));
                value.append("${");
                i0 = i1 + 2; // skip past "${"
            }
        }
        value.append(expr.substring(i0));
        return value.toString();
    }
}
