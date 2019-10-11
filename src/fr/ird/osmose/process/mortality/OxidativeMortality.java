/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class OxidativeMortality extends AbstractMortality {

    private double k_dam;

    public OxidativeMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        k_dam = 0.d;
        String key = "bioen.damage.k_dam";
        if (!getConfiguration().isNull(key)) {
            k_dam = getConfiguration().getDouble(key);
        }

    }

    @Override
    public double getRate(School school) {
        // calculation of PhiT
        return this.k_dam * school.getEGross();
    }

}
