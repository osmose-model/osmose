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
package fr.ird.osmose.util.version;

import fr.ird.osmose.util.OsmoseLinker;
import java.util.Arrays;

/**
 *
 * @author pverley
 */
public class VersionManager extends OsmoseLinker {

    // Declaration of the existing Osmose versions
    private final AbstractVersion v3 = new Version3();
    private final AbstractVersion v3u1 = new Version3Update1();
    private final AbstractVersion v3u2 = new Version3Update2();
    private final AbstractVersion v3u3 = new Version3Update3();
    private final AbstractVersion v3u3r3 = new Version3Update3Release3();
    private final AbstractVersion v4u0r0 = new Version4Update0Release0();
    private final AbstractVersion v4u1r0 = new Version4Update1Release0();
    private final AbstractVersion v4u2r0 = new Version4Update2Release0();
    private final AbstractVersion v4u2r1 = new Version4Update2Release1();
    
    // List of the existing Osmose versions
    private final AbstractVersion[] VERSIONS = {v3, v3u1, v3u2, v3u3, v3u3r3, v4u0r0, v4u1r0, v4u2r0, v4u2r1};

    // Current Osmose version
    public final AbstractVersion OSMOSE_VERSION = v4u2r1;

    // Version of the Osmose configuration
    private AbstractVersion cfgVersion;

    private static final VersionManager versionManager = new VersionManager();
        
    public static VersionManager getInstance() {
        return versionManager;
    }

    protected VersionManager() {
        Arrays.sort(VERSIONS);
    }

    public boolean checkConfiguration() {
        
        // Retrieve version of the Osmose configuration 
        cfgVersion = getConfigurationVersion();

        // Check version
        if (cfgVersion.compareTo(OSMOSE_VERSION) < 0) {
            warning("Configuration version {0} is older than software version {1}.", new Object[]{cfgVersion, OSMOSE_VERSION});
            return false;
        } else {
            info("Configuration version {0} matches software version {1}. Nothing to do.", new Object[]{cfgVersion, OSMOSE_VERSION});
        }
        return true;
    }

    /*
     * Upgrade the configuration file to the application version.
     */
    public void updateConfiguration() {

        // Update the configuration file
            for (AbstractVersion version : VERSIONS) {
                if ((version.compareTo(OSMOSE_VERSION) <= 0) && (cfgVersion.compareTo(version) < 0)) {
                    version.updateConfiguration();
                    getConfiguration().refresh();
                }
            }
    }

    /**
     * Returns the version of the configuration file. Parameter
     * <i>osmose.version</i>. If the parameter is not found or the value does
     * not match any listed Osmose version, {@code Configuration} assumes it is
     * version 3
     *
     * @see fr.ird.osmose.util.version.Version3
     * @return the version of the configuration file
     */
    AbstractVersion getConfigurationVersion() {

        if (!getConfiguration().isNull("osmose.version")) {
            try {
                String sversion = getConfiguration().getString("osmose.version");
                // Clean the version parameter
                
                // replace points by space, replace all not figure/space by empty, replace date (YYYYMMDD) into empty and remove additional whitespaces     
                sversion = sversion.replaceAll("\\.", " ").replaceAll("[^0-9\\s]", "").replaceAll(" +[0-9]{8}", "").replaceAll(" +", " ").trim();
                String[] split = sversion.split(" ");

                // Version number is the first figure
                int number = Integer.valueOf(split[0].trim());
                int update = 0;
                int release = 0;
                // Update number is the second figure
                
                if (split.length >= 2) {
                    update = Integer.valueOf(split[1].trim());
                }
                
                if (split.length == 3) {
                    release = Integer.valueOf(split[2].trim());
                }
                 
                for (AbstractVersion aversion : VERSIONS) {
                    if ((aversion.getNumber() == number) && (aversion.getUpdate() == update) && (aversion.getRelease() == release)) {
                        return aversion;
                    }
                }
            } catch (Exception ex) {
            }
            StringBuilder msg = new StringBuilder();
            msg.append("Could not identify version of the configuration. Check parameter ");
            msg.append(getConfiguration().printParameter("osmose.version"));
            error(msg.toString(), new IllegalArgumentException("Supported versions are " + Arrays.toString(VERSIONS)));
        }
        return v3;
    }
}
