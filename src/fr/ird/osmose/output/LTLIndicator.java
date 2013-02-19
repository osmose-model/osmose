package fr.ird.osmose.output;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.IOTools;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

/**
 *
 * @author pverley
 */
public class LTLIndicator extends AbstractIndicator {

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;
    /**
     * Object for creating/writing netCDF files.
     */
    private NetcdfFileWriteable nc;
    /**
     * LTL biomass array
     */
    private float[][][] ltlbiomass;

    @Override
    public void init() {
        // nothing to do
    }

    @Override
    public void reset() {
        int nx = getGrid().getNbColumns();
        int ny = getGrid().getNbLines();
        ltlbiomass = new float[getForcing().getNbPlanktonGroups()][ny][nx];

        /*
         * Create the NetCDF file at first time step
         */
        if (getSimulation().getIndexTimeSimu() == 0) {
            String ncfile;
            try {
                ncfile = makeFileLocation(getSimulation().getReplica());
                if (null == nc) {
                    createNCFile(ncfile);
                }
            } catch (IOException ex) {
                Logger.getLogger(LTLIndicator.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    @Override
    public void update() {
        // Loop over the cells
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                int i = cell.get_igrid();
                int j = cell.get_jgrid();
                for (int iltl = 0; iltl < getForcing().getNbPlanktonGroups(); iltl++) {
                    ltlbiomass[iltl][i][j] = getForcing().getPlankton(iltl).iniBiomass[i][j];
                }
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void write(float time) {
        // Pre-writing
        for (Cell cell : getGrid().getCells()) {
            int i = cell.get_igrid();
            int j = cell.get_jgrid();
            // Set _FillValue on land cells
            if (cell.isLand()) {
                for (int iltl = 0; iltl < getForcing().getNbPlanktonGroups(); iltl++) {
                    ltlbiomass[iltl][i][j] = FILLVALUE;
                }
            }
        }

        // Write into NetCDF file
        ArrayFloat.D4 arrLTL = new ArrayFloat.D4(1, getForcing().getNbPlanktonGroups(), getGrid().getNbLines(), getGrid().getNbColumns());
        int nl = getGrid().getNbLines() - 1;
        for (int kltl = 0; kltl < getForcing().getNbPlanktonGroups(); kltl++) {
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    arrLTL.set(0, kltl, nl - i, j, ltlbiomass[kltl][i][j]);
                }
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, time);

        int index = nc.getUnlimitedDimension().getLength();
        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write("ltl_biomass", new int[]{index, 0, 0, 0}, arrLTL);
        } catch (IOException ex) {
            Logger.getLogger(SpatialIndicator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (isLastStep()) {
            closeNCFile();
        }
    }

    private void createNCFile(String ncfile) {
        /*
         * Create NetCDF file
         */
        try {
            nc = NetcdfFileWriteable.createNew("");
            nc.setLocation(ncfile);
        } catch (IOException ex) {
            Logger.getLogger(LTLIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * Create dimensions
         */
        Dimension ltlDim = nc.addDimension("ltl", getForcing().getNbPlanktonGroups());
        Dimension columnsDim = nc.addDimension("columns", getGrid().getNbColumns());
        Dimension linesDim = nc.addDimension("lines", getGrid().getNbLines());
        Dimension timeDim = nc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        nc.addVariable("time", DataType.FLOAT, new Dimension[]{timeDim});
        nc.addVariableAttribute("time", "units", "year");
        nc.addVariableAttribute("time", "description", "time ellapsed, in years, since the begining of the simulation");
        nc.addVariable("ltl_biomass", DataType.FLOAT, new Dimension[]{timeDim, ltlDim, linesDim, columnsDim});
        nc.addVariableAttribute("ltl_biomass", "units", "tons per cell");
        nc.addVariableAttribute("ltl_biomass", "description", "plankton biomass in osmose cell, in tons integrated on water column, per group and per cell");
        nc.addVariableAttribute("ltl_biomass", "_FillValue", -99.f);
        nc.addVariable("latitude", DataType.FLOAT, new Dimension[]{linesDim, columnsDim});
        nc.addVariableAttribute("latitude", "units", "degree");
        nc.addVariableAttribute("latitude", "description", "latitude of the center of the cell");
        nc.addVariable("longitude", DataType.FLOAT, new Dimension[]{linesDim, columnsDim});
        nc.addVariableAttribute("longitude", "units", "degree");
        nc.addVariableAttribute("longitude", "description", "longitude of the center of the cell");
        /*
         * Add global attributes
         */
        StringBuilder str = new StringBuilder();
        for (int kltl = 0; kltl < getForcing().getNbPlanktonGroups(); kltl++) {
            str.append(kltl);
            str.append("=");
            str.append(getForcing().getPlanktonName(kltl));
            str.append(" ");
        }
        nc.addGlobalAttribute("dimension_ltl", str.toString());
        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc.create();
            /*
             * Writes variable longitude and latitude
             */
            ArrayFloat.D2 arrLon = new ArrayFloat.D2(getGrid().getNbLines(), getGrid().getNbColumns());
            ArrayFloat.D2 arrLat = new ArrayFloat.D2(getGrid().getNbLines(), getGrid().getNbColumns());
            for (Cell cell : getGrid().getCells()) {
                arrLon.set(getGrid().getNbLines() - cell.get_igrid() - 1, cell.get_jgrid(), cell.getLon());
                arrLat.set(getGrid().getNbLines() - cell.get_igrid() - 1, cell.get_jgrid(), cell.getLat());
            }
            nc.write("longitude", arrLon);
            nc.write("latitude", arrLat);
        } catch (IOException ex) {
            Logger.getLogger(LTLIndicator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(LTLIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Closes the NetCDF file.
     */
    private void closeNCFile() {
        try {
            nc.close();
            String strFilePart = nc.getLocation();
            String strFileBase = strFilePart.substring(0, strFilePart.indexOf(".part"));
            File filePart = new File(strFilePart);
            File fileBase = new File(strFileBase);
            filePart.renameTo(fileBase);
        } catch (Exception ex) {
            Logger.getLogger(LTLIndicator.class.getName()).log(Level.WARNING, "Problem closing the NetCDF output file ==> {0}", ex.toString());
        }

    }

    private String makeFileLocation(int nSerie) throws IOException {

        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab);
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append("planktonBiomass");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix);
        filename.append("_ltlbiomass_integrated_");
        filename.append("Simu");
        filename.append(nSerie);
        filename.append(".nc");
        File file = new File(filename.toString());
        try {
            IOTools.makeDirectories(file.getAbsolutePath());
            file.createNewFile();
            file.delete();
        } catch (Exception ex) {
            IOException ioex = new IOException("{Ouput} Failed to create NetCDF file " + filename + " ==> " + ex.getMessage());
            ioex.setStackTrace(ex.getStackTrace());
            throw ioex;
        }
        filename.append(".part");
        return filename.toString();
    }

    /**
     *
     * @return true if current step is the last step of the simulation
     */
    private boolean isLastStep() {
        int lastStep = getOsmose().getNumberYears() * getOsmose().getNumberTimeStepsPerYear() - 1;
        return getSimulation().getIndexTimeSimu() == lastStep;
    }
}
