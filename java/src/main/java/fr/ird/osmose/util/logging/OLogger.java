/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
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
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package fr.ird.osmose.util.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class OLogger {

    /**
     * The application logger
     */
    private final static Logger OLOGGER = Logger.getAnonymousLogger();

    final public void error(String msg, Exception ex) {
        OLOGGER.log(Level.SEVERE, msg, ex);
        System.exit(1);
    }

    final public void warning(String msg) {
        OLOGGER.warning(msg);
    }

    final public void warning(String msg, Object param) {
        OLOGGER.log(Level.WARNING, msg, param);
    }

    final public void warning(String msg, Object params[]) {
        OLOGGER.log(Level.WARNING, msg, params);
    }

    final public void info(String msg) {
        OLOGGER.info(msg);
    }

    final public void info(String msg, Object param) {
        OLOGGER.log(Level.INFO, msg, param);
    }

    final public void info(String msg, Object params[]) {
        OLOGGER.log(Level.INFO, msg, params);
    }

    final public void debug(String msg) {
        OLOGGER.log(Level.FINE, msg);
    }

    final public void debug(String msg, Object param) {
        OLOGGER.log(Level.FINE, msg, param);
    }

    final public void debug(String msg, Object params[]) {
        OLOGGER.log(Level.FINE, msg, params);
    }
    
    final public Logger getLogger() {
        return OLOGGER;
    }
}
