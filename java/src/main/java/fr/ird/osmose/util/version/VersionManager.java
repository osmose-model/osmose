/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
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

package fr.ird.osmose.util.version;

import fr.ird.osmose.util.OsmoseLinker;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 *
 * @author pverley
 */
public class VersionManager extends OsmoseLinker {

    // Current Osmose version
    private final VersionNumber jarVersion;

    // Version of the Osmose configuration
    private VersionNumber cfgVersion;

    private static final VersionManager VERSION_MANAGER = new VersionManager();

    public static VersionManager getInstance() {
        return VERSION_MANAGER;
    }

    protected VersionManager() {
        Arrays.sort(Releases.ALL, (Release r1, Release r2) -> r1.getVersionNumber().compareTo(r2.getVersionNumber()));
        jarVersion = retrieveJarVersion();
    }

    private VersionNumber retrieveJarVersion() {

        try {
            String clz = getClass().getSimpleName() + ".class";
            String pth = getClass().getResource(clz).toString();
            String mnf = pth.substring(0, pth.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            URL url = new URL(mnf);
            Manifest manifest = new Manifest(url.openStream());
            Attributes attributes = manifest.getMainAttributes();
            return VersionNumber.valueOf(attributes.getValue("Implementation-Version"));
        } catch (IOException ex) {
            return VersionNumber.valueOf("4.3.3");
        }
    }

    public VersionNumber getJarVersion() {
        return jarVersion;
    }

    public boolean isConfigurationUpToDate() {

        // Retrieve version of the Osmose configuration 
        cfgVersion = getConfigurationVersion();

        // compare current configuration to jar version
        return cfgVersion.compareTo(jarVersion) >= 0;
    }

    /*
     * Upgrade the configuration file to the application version.
     */
    public void updateConfiguration() {

        // Update the configuration file
        for (Release release : Releases.ALL) {
            VersionNumber version = release.getVersionNumber();
            if ((version.compareTo(jarVersion) <= 0) && (cfgVersion.compareTo(version) < 0)) {
                release.updateConfiguration();
                getConfiguration().refresh();
            }
        }
        // update cfg number to jar version in case last release is anterior to jar version
        if (jarVersion.compareTo(Releases.ALL[Releases.ALL.length - 1].getVersionNumber()) > 0) {
            latest().updateConfiguration();
            getConfiguration().refresh();
        }
        info("Configuration file updated successfully to version " + jarVersion.toString());
    }

    private Release latest() {
        return new Release(jarVersion.toString()) {
            @Override
            void updateParameters() {
                // Update version
                updateValue("osmose.version", getVersionNumber().toString());
            }
        };
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
    public VersionNumber getConfigurationVersion() {

        if (!getConfiguration().isNull("osmose.version")) {
            try {
                String sversion = getConfiguration().getString("osmose.version");
                // Clean the version parameter
                // replace points by space, replace all not figure/space by empty, replace date (YYYYMMDD) into empty and remove additional whitespaces
                // (conversion of old format Osmose 3 update 1 release 2 (2019/11/13)) into 3.1.2
                // should remain something like {major} {minor} {patch}
                sversion = sversion.replaceAll("\\.", " ").replaceAll("[^0-9\\s]", "").replaceAll(" +[0-9]{8}", "").replaceAll(" +", " ").trim();
                // put the dots back between figures
                sversion = sversion.replaceAll(" ", "\\.");
                // should be {major}.{minor}.{patch}
                return VersionNumber.valueOf(sversion);
            } catch (Exception ex) {
                StringBuilder msg = new StringBuilder();
                msg.append("Could not identify version of the configuration. Check parameter ");
                msg.append(getConfiguration().printParameter("osmose.version"));
                error(msg.toString(), ex);

            }
        }
        return VersionNumber.valueOf("3.0");
    }
}
