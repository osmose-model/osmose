package fr.ird.osmose.output;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.process.MovementProcess;
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
public class SpatialIndicator extends AbstractIndicator {

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;
    /**
     * Object for creating/writing netCDF files.
     */
    private NetcdfFileWriteable nc;
    // spatial indicators
    private float[][][] biomass;
    private float[][][] mean_size;
    private float[][][] tl;
    private float[][][] ltlbiomass;
    private float[][][] abundance;
    private float[][][] yield;

    @Override
    public void init() {
    }

    @Override
    public void reset() {

        int nSpecies = getNSpecies();
        int nx = getGrid().getNbColumns();
        int ny = getGrid().getNbLines();
        biomass = new float[nSpecies][ny][nx];
        mean_size = new float[nSpecies][ny][nx];
        tl = new float[nSpecies][ny][nx];
        ltlbiomass = new float[getForcing().getNbPlanktonGroups()][ny][nx];
        abundance = new float[nSpecies][ny][nx];
        yield = new float[nSpecies][ny][nx];

        /*
         * Create the NetCDF file at first time step
         */
        if (getSimulation().getIndexTimeSimu() == 0) {
            createNCFile(getSimulation().getReplica());
        }
    }

    @Override
    public void update() {
        // Loop over the cells
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                int i = cell.get_igrid();
                int j = cell.get_jgrid();
                for (School school : getPopulation().getSchools(cell)) {
                    if (school.getAgeDt() > school.getSpecies().indexAgeClass0 && !MovementProcess.isOut(school)) {
                        int iSpec = school.getSpeciesIndex();
                        biomass[iSpec][i][j] += school.getBiomass();
                        abundance[iSpec][i][j] += school.getAbundance();
                        mean_size[iSpec][i][j] += school.getLength() * school.getAbundance();
                        tl[iSpec][i][j] += school.trophicLevel * school.getBiomass();
                        yield[iSpec][i][j] += school.adb2biom(school.nDeadFishing);
                    }
                }
                for (int iltl = 0; iltl < getForcing().getNbPlanktonGroups(); iltl++) {
                    ltlbiomass[iltl][cell.get_igrid()][cell.get_jgrid()] = getForcing().getPlankton(iltl).getBiomass(cell);
                }
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return getOsmose().spatializedOutputs;
    }

    @Override
    public void write(float time) {

        // Pre-writing
        for (Cell cell : getGrid().getCells()) {
            int i = cell.get_igrid();
            int j = cell.get_jgrid();
            // Set _FillValue on land cells
            if (cell.isLand()) {
                for (int ispec = 0; ispec < getNSpecies(); ispec++) {
                    biomass[ispec][i][j] = FILLVALUE;
                    abundance[ispec][i][j] = FILLVALUE;
                    mean_size[ispec][i][j] = FILLVALUE;
                    tl[ispec][i][j] = FILLVALUE;
                    yield[ispec][i][j] = FILLVALUE;
                }
                for (int iltl = 0; iltl < getForcing().getNbPlanktonGroups(); iltl++) {
                    ltlbiomass[iltl][i][j] = FILLVALUE;
                }
            } else {
                // Weight mean size with abundance
                // Weight mean size with biomass
                for (int ispec = 0; ispec < getNSpecies(); ispec++) {
                    if (abundance[ispec][i][j] > 0) {
                        mean_size[ispec][i][j] /= abundance[ispec][i][j];
                        tl[ispec][i][j] /= biomass[ispec][i][j];
                    }
                }
            }
        }

        // Write into NetCDF file
        int nSpecies = getNSpecies();
        ArrayFloat.D4 arrBiomass = new ArrayFloat.D4(1, nSpecies, getGrid().getNbLines(), getGrid().getNbColumns());
        ArrayFloat.D4 arrAbundance = new ArrayFloat.D4(1, nSpecies, getGrid().getNbLines(), getGrid().getNbColumns());
        ArrayFloat.D4 arrYield = new ArrayFloat.D4(1, nSpecies, getGrid().getNbLines(), getGrid().getNbColumns());
        ArrayFloat.D4 arrSize = new ArrayFloat.D4(1, nSpecies, getGrid().getNbLines(), getGrid().getNbColumns());
        ArrayFloat.D4 arrTL = new ArrayFloat.D4(1, nSpecies, getGrid().getNbLines(), getGrid().getNbColumns());
        ArrayFloat.D4 arrLTL = new ArrayFloat.D4(1, getForcing().getNbPlanktonGroups(), getGrid().getNbLines(), getGrid().getNbColumns());
        int nl = getGrid().getNbLines() - 1;
        for (int kspec = 0; kspec < nSpecies; kspec++) {
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    arrBiomass.set(0, kspec, nl - i, j, biomass[kspec][i][j]);
                    arrAbundance.set(0, kspec, nl - i, j, abundance[kspec][i][j]);
                    arrSize.set(0, kspec, nl - i, j, mean_size[kspec][i][j]);
                    arrTL.set(0, kspec, nl - i, j, tl[kspec][i][j]);
                    arrYield.set(0, kspec, nl - i, j, yield[kspec][i][j]);
                }
            }
        }
        for (int kltl = 0; kltl < getForcing().getNbPlanktonGroups(); kltl++) {
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    arrLTL.set(0, kltl,  nl - i, j, ltlbiomass[kltl][i][j]);
                }
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, time);

        int index = nc.getUnlimitedDimension().getLength();
        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write("time", new int[]{index}, arrTime);
            nc.write("biomass", new int[]{index, 0, 0, 0}, arrBiomass);
            nc.write("abundance", new int[]{index, 0, 0, 0}, arrAbundance);
            nc.write("yield", new int[]{index, 0, 0, 0}, arrYield);
            nc.write("mean_size", new int[]{index, 0, 0, 0}, arrSize);
            nc.write("trophic_level", new int[]{index, 0, 0, 0}, arrTL);
            nc.write("ltl_biomass", new int[]{index, 0, 0, 0, 0}, arrLTL);
        } catch (IOException ex) {
            Logger.getLogger(SpatialIndicator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (isLastStep()) {
            closeNCFile();
        }
    }

    private void createNCFile(int nSimu) {
        /*
         * Create NetCDF file
         */
        try {
            nc = NetcdfFileWriteable.createNew("");
            nc.setLocation(makeFileLocation(nSimu));
        } catch (IOException ex) {
            Logger.getLogger(SpatialIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * Create dimensions
         */
        Dimension speciesDim = nc.addDimension("species", getNSpecies());
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
        nc.addVariable("biomass", DataType.FLOAT, new Dimension[]{timeDim, speciesDim, linesDim, columnsDim});
        nc.addVariableAttribute("biomass", "units", "ton");
        nc.addVariableAttribute("biomass", "description", "biomass, in tons, per species and per cell");
        nc.addVariableAttribute("biomass", "_FillValue", -99.f);
        nc.addVariable("abundance", DataType.FLOAT, new Dimension[]{timeDim, speciesDim, linesDim, columnsDim});
        nc.addVariableAttribute("abundance", "units", "number of fish");
        nc.addVariableAttribute("abundance", "description", "Number of fish per species and per cell");
        nc.addVariableAttribute("abundance", "_FillValue", -99.f);
        nc.addVariable("yield", DataType.FLOAT, new Dimension[]{timeDim, speciesDim, linesDim, columnsDim});
        nc.addVariableAttribute("yield", "units", "ton");
        nc.addVariableAttribute("yield", "description", "Catches, in tons, per species and per cell");
        nc.addVariableAttribute("yield", "_FillValue", -99.f);
        nc.addVariable("mean_size", DataType.FLOAT, new Dimension[]{timeDim, speciesDim, linesDim, columnsDim});
        nc.addVariableAttribute("mean_size", "units", "centimeter");
        nc.addVariableAttribute("mean_size", "description", "mean size, in centimeter, per species and per cell");
        nc.addVariableAttribute("mean_size", "_FillValue", -99.f);
        nc.addVariable("trophic_level", DataType.FLOAT, new Dimension[]{timeDim, speciesDim, linesDim, columnsDim});
        nc.addVariableAttribute("trophic_level", "units", "scalar");
        nc.addVariableAttribute("trophic_level", "description", "trophic level per species and per cell");
        nc.addVariableAttribute("trophic_level", "_FillValue", -99.f);
        nc.addVariable("ltl_biomass", DataType.FLOAT, new Dimension[]{timeDim, ltlDim, linesDim, columnsDim});
        nc.addVariableAttribute("ltl_biomass", "units", "ton/km2");
        nc.addVariableAttribute("ltl_biomass", "description", "plankton biomass, in tons per km2 integrated on water column, per group and per cell");
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
        nc.addGlobalAttribute("dimension_step", "step=0 before predation, step=1 after predation");
        StringBuilder str = new StringBuilder();
        for (int kltl = 0; kltl < getForcing().getNbPlanktonGroups(); kltl++) {
            str.append(kltl);
            str.append("=");
            str.append(getForcing().getPlanktonName(kltl));
            str.append(" ");
        }
        nc.addGlobalAttribute("dimension_ltl", str.toString());
        str = new StringBuilder();
        for (int ispec = 0; ispec < getOsmose().getNumberSpecies(); ispec++) {
            str.append(ispec);
            str.append("=");
            str.append(getSpecies(ispec).getName());
            str.append(" ");
        }
        nc.addGlobalAttribute("dimension_species", str.toString());
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
            Logger.getLogger(SpatialIndicator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialIndicator.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(SpatialIndicator.class.getName()).log(Level.WARNING, "Problem closing the NetCDF output file ==> {0}", ex.toString());
        }

    }

    private String makeFileLocation(int iSimu) throws IOException {

        StringBuilder filename = new StringBuilder();
        filename.append(getOsmose().outputPathName);
        filename.append(getOsmose().outputFileNameTab);
        filename.append(getOsmose().fileSeparator);
        filename.append(getOsmose().outputPrefix);
        filename.append("_spatialized_Simu");
        filename.append(iSimu);
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
        return filename + ".part";
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
