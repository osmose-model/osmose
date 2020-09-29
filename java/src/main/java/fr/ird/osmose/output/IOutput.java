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

package fr.ird.osmose.output;

/**
 *
 * @author pverley
 */
public interface IOutput {
    
    /**
     * This function will be called at the beginning of every time step, before
     * any process occurred.
     * Indeed for some indicators it might be necessary to know the state of
     * the system just after the reproduction and before the following step.
     */
    public void initStep();

    /**
     * Reset the indicator after a saving step has been written in output file.
     * It will be automatically called after the write(time) function
     */
    public void reset();

    /**
     * The function is called every time step, at the end of the step,
     * usually before the reproduction process.
     */
    public void update();

    /**
     * Write the indicator in output file at specified time
     *
     * @param time, expressed in year
     */
    public void write(float time);
    
    /**
     * Whether the parameter should be written at specified time step.
     * @param iStepSimu, the current step of the simulation.
     * @return true if the parameter should be written in the file at the
     * specified time step, false otherwise.
     */
    public boolean isTimeToWrite(int iStepSimu);
    
    /**
     * Initializes the indicator. Load parameters and create the file.
     */
    public void init();
    
    /**
     * Closes the file
     */
    public void close();
    
}
