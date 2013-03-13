package fr.ird.osmose.output;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.util.IOTools;
import fr.ird.osmose.util.SimulationLinker;
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
public class LTLIndicator extends SimulationLinker implements Indicator {

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;
    /**
     * Object for creating/writing netCDF files.
     */
    private NetcdfFileWriteable nc;
    /**
     * LTL biomass array at the beginning of the time step.
     */
    private float[][][] ltlbiomass0;
    /**
     * LTL biomass array after predation process.
     */
    private float[][][] ltlbiomass1;
    /**
     * Whether the indicator should be enabled or not.
     */
    private boolean enabled;

    public LTLIndicator(int replica, String keyEnabled) {
        super(replica);
        enabled = getConfiguration().getBoolean(keyEnabled);
    }

    @Override
    public void init() {
        String filename = getFilename();
        IOTools.makeDirectories(filename);
        createNCFile(filename);
    }

    @Override
    public void close() {
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

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void reset() {
        int nx = getGrid().get_nx();
        int ny = getGrid().get_ny();
        ltlbiomass0 = new float[getConfiguration().getNPlankton()][ny][nx];
        ltlbiomass1 = new float[getConfiguration().getNPlankton()][ny][nx];
    }

    @Override
    public void update() {
        // Loop over the cells
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                for (int iltl = 0; iltl < getConfiguration().getNPlankton(); iltl++) {
                    ltlbiomass0[iltl][cell.get_jgrid()][cell.get_igrid()] = getSimulation().getPlankton(iltl).getBiomass(cell);
                    ltlbiomass1[iltl][cell.get_jgrid()][cell.get_igrid()] = ltlbiomass0[iltl][cell.get_jgrid()][cell.get_igrid()];
                }
            }
        }

        //
        int nspec = getNSpecies();
        for (School school : getPopulation().getPresentSchools()) {
            int i = (int) school.getX();
            int j = (int) school.getY();
            for (int iltl = 0; iltl < getConfiguration().getNPlankton(); iltl++) {
                ltlbiomass1[iltl][j][i] -= school.diet[nspec + iltl][0];
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void write(float time) {
        // Pre-writing
        for (Cell cell : getGrid().getCells()) {
            int i = cell.get_igrid();
            int j = cell.get_jgrid();
            // Set _FillValue on land cells
            if (cell.isLand()) {
                for (int iltl = 0; iltl < getConfiguration().getNPlankton(); iltl++) {
                    ltlbiomass0[iltl][j][i] = FILLVALUE;
                    ltlbiomass1[iltl][j][i] = FILLVALUE;
                }
            }
        }

        // Write into NetCDF file
        ArrayFloat.D4 arrLTL0 = new ArrayFloat.D4(1, getConfiguration().getNPlankton(), getGrid().get_ny(), getGrid().get_nx());
        ArrayFloat.D4 arrLTL1 = new ArrayFloat.D4(1, getConfiguration().getNPlankton(), getGrid().get_ny(), getGrid().get_nx());
        int nl = getGrid().get_ny() - 1;
        for (int kltl = 0; kltl < getConfiguration().getNPlankton(); kltl++) {
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    arrLTL0.set(0, kltl, j, i, ltlbiomass0[kltl][j][i]);
                    arrLTL1.set(0, kltl, j, i, ltlbiomass1[kltl][j][i]);
                }
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, time * 360);

        int index = nc.getUnlimitedDimension().getLength();
        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write("time", new int[] {index}, arrTime);
            nc.write("ltl_biomass", new int[]{index, 0, 0, 0}, arrLTL0);
            nc.write("ltl_biomass_pred", new int[]{index, 0, 0, 0}, arrLTL1);
        } catch (IOException ex) {
            Logger.getLogger(SpatialIndicator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialIndicator.class.getName()).log(Level.SEVERE, null, ex);
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
        Dimension ltlDim = nc.addDimension("ltl", getConfiguration().getNPlankton());
        Dimension nxDim = nc.addDimension("nx", getGrid().get_nx());
        Dimension nyDim = nc.addDimension("ny", getGrid().get_ny());
        Dimension timeDim = nc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        nc.addVariable("time", DataType.FLOAT, new Dimension[]{timeDim});
        nc.addVariableAttribute("time", "units", "days since 1-1-1 0:0:0");
        nc.addVariableAttribute("time", "calendar", "360_day");
        nc.addVariableAttribute("time", "description", "time ellapsed, in days, since the beginning of the simulation");
        nc.addVariable("ltl_biomass", DataType.FLOAT, new Dimension[]{timeDim, ltlDim, nyDim, nxDim});
        nc.addVariableAttribute("ltl_biomass", "units", "tons per cell");
        nc.addVariableAttribute("ltl_biomass", "description", "plankton biomass in osmose cell, in tons integrated on water column, per group and per cell");
        nc.addVariableAttribute("ltl_biomass", "_FillValue", -99.f);
        nc.addVariable("ltl_biomass_pred", DataType.FLOAT, new Dimension[]{timeDim, ltlDim, nyDim, nxDim});
        nc.addVariableAttribute("ltl_biomass_pred", "units", "tons per cell");
        nc.addVariableAttribute("ltl_biomass_pred", "description", "plankton biomass after predation process in osmose cell, in tons integrated on water column, per group and per cell");
        nc.addVariableAttribute("ltl_biomass_pred", "_FillValue", -99.f);
        nc.addVariable("latitude", DataType.FLOAT, new Dimension[]{nyDim, nxDim});
        nc.addVariableAttribute("latitude", "units", "degree");
        nc.addVariableAttribute("latitude", "description", "latitude of the center of the cell");
        nc.addVariable("longitude", DataType.FLOAT, new Dimension[]{nyDim, nxDim});
        nc.addVariableAttribute("longitude", "units", "degree");
        nc.addVariableAttribute("longitude", "description", "longitude of the center of the cell");
        /*
         * Add global attributes
         */
        StringBuilder str = new StringBuilder();
        for (int kltl = 0; kltl < getConfiguration().getNPlankton(); kltl++) {
            str.append(kltl);
            str.append("=");
            str.append(getSimulation().getPlankton(kltl));
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
            ArrayFloat.D2 arrLon = new ArrayFloat.D2(getGrid().get_ny(), getGrid().get_nx());
            ArrayFloat.D2 arrLat = new ArrayFloat.D2(getGrid().get_ny(), getGrid().get_nx());
            for (Cell cell : getGrid().getCells()) {
                arrLon.set(cell.get_jgrid(), cell.get_igrid(), cell.getLon());
                arrLat.set(cell.get_jgrid(), cell.get_igrid(), cell.getLat());
            }
            nc.write("longitude", arrLon);
            nc.write("latitude", arrLat);
        } catch (IOException ex) {
            Logger.getLogger(LTLIndicator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(LTLIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append("planktonBiomass");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_ltlbiomass_integrated_");
        filename.append("Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".nc.part");
        return filename.toString();
    }
}
