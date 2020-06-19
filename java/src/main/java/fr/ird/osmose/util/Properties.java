/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
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
