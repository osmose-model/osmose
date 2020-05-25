/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.School;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class ForagingMortality extends AbstractMortality {

    private double[] k_for;
    private double[] I_max;

    public ForagingMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        int nspec = this.getNSpecies();
        k_for = new double[nspec];
        I_max = new double[nspec];
        for (int i=0;i<nspec;i++){
            k_for[i] = getConfiguration().getDouble("bioen.forage.k_for.sp" + i);
            I_max[i] = getConfiguration().getDouble("predation.ingestion.rate.max.bioen.sp" + i);
        }
    }

    @Override
    public double getRate(School school) {

        // This mortality increase with individual ingestion --> division by abundance

//        return k_for[school.getSpeciesIndex()]*school.getIngestion()*1000000 / school.getInstantaneousAbundance()/(Math.pow(school.getWeight() * 1e6f, school.getAlphaBioen()));
        
        // calcul de la mortalité en lien avec Imax
        double output = 0 ;
        if (this.getConfiguration().useGenetic()) {
            String key = "imax";
        
            try {
                output = school.getTrait(key) * this.k_for[school.getSpeciesIndex()]/24;
            } catch (Exception ex) {
                Logger.getLogger(ForagingMortality.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            output =  I_max[school.getSpeciesIndex()] * this.k_for[school.getSpeciesIndex()]/24;;
        }
        if (output<0) {
            output = 0;
                    }
        return output;
    }
   
}
