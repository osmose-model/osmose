/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.io.File;
import java.util.List;

/**
 *
 * @author nbarrier
 */
public class RegionalOutputsAbundance extends RegionalOutputsBiomass {
    
    public RegionalOutputsAbundance(int rank, Species species) {
        super(rank, species);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("Regional");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_abundanceByDomain");
        filename.append("-");
        filename.append(this.getSpecies().getName());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        String output = "Mean abundance over pre-defined regional domains";
        return output;
    }

    @Override
    public void update() {
        // Loop over all the regions
        for (int idom = 0; idom < this.getNRegions(); idom++) {

            // index of the points that belong to the region
            int[] iind = this.getIDom(idom);
            int[] jind = this.getJDom(idom);
            int npoints = iind.length;

            // loop over all the points within the current region
            for (int ipoint = 0; ipoint < npoints; ipoint++) {
                int i = iind[ipoint];
                int j = jind[ipoint];
                Cell cell = this.getGrid().getCell(i, j);
                // extraction of the schools that belong to the current cell
                List<School> listSchool = getSchoolSet().getSchools(cell);
                if (null != listSchool) {
                    for (School school : listSchool) {
                        // integration of the biomass that belong to the proper index
                        if ((school.getSpeciesIndex() == this.getSpecies().getIndex()) && (this.include(school))) { 
                            values[idom] += school.getInstantaneousAbundance();
                        } // end if species test
                    }  // end of school loop
                }  // end if if null
            }  // end of domain point loop
        }  // end of domain loop
    }  // end of method
}