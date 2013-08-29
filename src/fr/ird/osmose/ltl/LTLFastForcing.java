package fr.ird.osmose.ltl;

import fr.ird.osmose.Plankton;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class LTLFastForcing extends AbstractLTLForcing {

    private String ncFile;
    private float[][][][] data;

    @Override
    public void readLTLForcingFile() {

        ncFile = getConfiguration().getFile("ltl.netcdf.file");
        if (!new File(ncFile).exists()) {
            getLogger().log(Level.SEVERE, "LTL NetCDF file {0} doesn''t exist", ncFile);
            System.exit(1);
        }
    }

    @Override
    public void initLTLGrid() {

        /*
         * set dimensions
         */
        setDimY(getGrid().get_ny());
        setDimX(getGrid().get_nx());

        loadData();
    }

    @Override
    public float[][] computeBiomass(Plankton plankton, int iStepSimu) {

        float[][] biomass = new float[getGrid().get_ny()][getGrid().get_nx()];
        for (int j = 0; j < getGrid().get_ny(); j++) {
            for (int i = 0; i < getGrid().get_nx(); i++) {
                if (!getGrid().getCell(i, j).isLand()) {
                    biomass[j][i] = 100 * data[getIndexStepLTL(iStepSimu)][plankton.getIndex()][j][i];
                }
            }
        }
        return data[getIndexStepLTL(iStepSimu)][plankton.getIndex()];
    }

    private void loadData() {
        try {
            getLogger().info("Loading all plankton data...");
            getLogger().log(Level.FINE, "Forcing file {0}", ncFile);

            NetcdfFile nc = NetcdfFile.open(ncFile);
            data = (float[][][][]) nc.findVariable("ltl_biomass").read().copyToNDJavaArray();
            getLogger().info("All plankton data loaded");
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error while loading LTL biomass from file " + ncFile, ex);
        }
    }

    @Override
    float[][] getRawBiomass(int iPlankton, int iStepYear) {
        return null;
    }

    @Override
    public String[] getPlanktonFieldName() {
        return null;
    }

    @Override
    public String[] getNetcdfFile() {
        return new String[]{ncFile};
    }
}
