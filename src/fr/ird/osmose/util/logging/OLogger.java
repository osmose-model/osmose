/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
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
package fr.ird.osmose.util.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
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
    private final Logger logger = Logger.getAnonymousLogger();

    public OLogger() {
        setup(-1);
    }

    public OLogger(int rank) {
        setup(rank);
    }

    private void setup(int rank) {
        Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            logger.removeHandler(handler);
        }
        logger.setUseParentHandlers(false);
        OsmoseLogFormatter formatter = new OsmoseLogFormatter(rank);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        logger.addHandler(handler);
    }

    public void error(String msg, Exception ex) {
        logger.log(Level.SEVERE, msg, ex);
        System.exit(1);
    }

    public void warning(String msg) {
        logger.warning(msg);
    }

    public void warning(String msg, Object param) {
        logger.log(Level.WARNING, msg, param);
    }

    public void warning(String msg, Object params[]) {
        logger.log(Level.WARNING, msg, params);
    }

    public void info(String msg) {
        logger.info(msg);
    }

    public void info(String msg, Object param) {
        logger.log(Level.INFO, msg, param);
    }

    public void info(String msg, Object params[]) {
        logger.log(Level.INFO, msg, params);
    }

    public void debug(String msg) {
        logger.log(Level.FINE, msg);
    }

    public void debug(String msg, Object param) {
        logger.log(Level.FINE, msg, param);
    }

    public void debug(String msg, Object params[]) {
        logger.log(Level.FINE, msg, params);
    }
}
