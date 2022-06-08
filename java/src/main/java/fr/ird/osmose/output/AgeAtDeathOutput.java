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

package fr.ird.osmose.output;

import fr.ird.osmose.process.mortality.MortalityCause;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0 2013/09/01
 */
public class AgeAtDeathOutput extends AbstractOutput {

    private final Species species;

    // mortality rates por souces and per stages
    private double[] nDead, ageDeath;

    double nDeadTot;
    double ageDeathTot;

    public AgeAtDeathOutput(int rank, Species species) {
        super(rank, "Mortality", "ageAtDeath" + "-" + species.getName());
        this.species = species;
    }

    @Override
    String getDescription() {
        return "Total (Mtot), Predation (Mpred), Starvation (Mstarv), Additional mortality (Madd), Fishing (F) & Out-of-domain (Z) ages at death (years)";
    }

    @Override
    public void reset() {
        //mortalityRates = new double[MortalityCause.values().length + 1];
        int nCause = MortalityCause.values().length;
        nDead = new double[nCause];
        ageDeath = new double[nCause];
        nDeadTot = 0;
        ageDeathTot = 0;
    }

    @Override
    public void update() {

        // Loop on all the schools to be sure we don't discard dead schools
        for (School school : getSchoolSet().getSchools()) {
            if (school.getFileSpeciesIndex() != species.getFileSpeciesIndex()) {
                continue;
            }
            // Update number of deads
            for (MortalityCause cause : MortalityCause.values()) {
                double tempNdead = school.getNdead(cause);
                double tempAgeDeath = school.getNdead(cause);
                nDead[cause.index] += tempNdead;
                nDeadTot += tempNdead;
                ageDeath[cause.index] += tempAgeDeath;
                ageDeathTot += tempAgeDeath;
            }
        }
    }

    @Override
    public void write(float time) {

        int nCause = MortalityCause.values().length;
        double[] array = new double[nCause + 1];
        array[0] = ageDeathTot / nDeadTot;
        for(int i = 0; i < nCause; i++) {
            array[i + 1] = ageDeath[i] / nDead[i];
        }

        writeVariable(time, array);
    }

    @Override
    String[] getHeaders() {
        return new String[]{"Mtot", "Mpred", "Mstar", "Mnat", "F", "Z", "Mfor", "Dis", "Age"};
    }

    @Override
    public void initStep() {
    }

}
