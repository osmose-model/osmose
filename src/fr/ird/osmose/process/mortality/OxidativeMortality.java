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
public class OxidativeMortality extends AbstractMortality {

    private double[] k_dam;

    public OxidativeMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        int nspec = this.getNSpecies();
        k_dam = new double[nspec];
        for (int i=0;i<nspec;i++){
            k_dam[i] = getConfiguration().getDouble("bioen.damage.k_dam.sp" + i);
        }
    }

    @Override
    public double getRate(School school) {

        // This mortality increase with individual ingestion --> division by abundance

        return k_dam[school.getSpeciesIndex()]*school.getIngestion()/ school.getInstantaneousAbundance();
        
//        // calcul de la mortalité en lien avec Imax
//        double output = 0 ;
//        if (this.getConfiguration().useGenetic()) {
//            String key = "imax";
//        
//            try {
//                output = this.k_dam * school.getTrait(key);
//                //* school.getAgeDt() / getSpecies(school.getSpeciesIndex()).getLifespanDt() 
//            } catch (Exception ex) {
//                Logger.getLogger(OxidativeMortality.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }else{
//            output =  0;
//        }
//        return output;
    }
   
}
