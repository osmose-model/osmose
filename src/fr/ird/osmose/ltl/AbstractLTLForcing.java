/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

import fr.ird.osmose.Osmose;
import fr.ird.osmose.Plankton;
import fr.ird.osmose.Simulation;
import fr.ird.osmose.SimulationLinker;
import fr.ird.osmose.output.Indicators;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

/**
 *
 * @author pverley
 */
public abstract class AbstractLTLForcing extends SimulationLinker implements LTLForcing {

    private int nbPlankton, nbForcingDt;
    private String[] planktonNames;		// list of names of plankton groups
    private float[] trophicLevel;			// list of TL of plankton groups
    private float[] minSize, maxSize;		// list of min and max sizes of plankton groups
    private float[] conversionFactors;		// list of conversionFactors of plankton groups
    private float[] prodBiomFactors;		// list of prod/biom ratios of plankton groups
    private Plankton[] planktonList;     // list of plankton groups (here 4)
    private int planktonDimX;      // dimension of LTL model, here ROMS Plume (144 * 65 * 20)
    private int planktonDimY;
    private int planktonDimZ;	// vertical dimension (20)
    private float integrationDepth;   // latitude and longitude of each cell of the LTL grid, used for interpolation

    @Override
    /**
     * Read LTL basic file with name of plankton, sizes, format of files...
     */
    public void readLTLConfigFile1(String planktonStructureFileName) {

        int numSerie = getOsmose().numSerie;
        FileInputStream LTLFile;
        try {
            LTLFile = new FileInputStream(new File(getOsmose().inputPathName, planktonStructureFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("LTL file " + planktonStructureFileName + " doesn't exist");
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(LTLFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            nbPlankton = (new Integer(st.sval)).intValue();
            if (!(nbPlankton == getOsmose().nbPlanktonGroupsTab[numSerie])) {
                System.out.println("The number of plankton group in plankton structure file does not match the one from config file");
            }
            st.nextToken();
            nbForcingDt = (new Integer(st.sval)).intValue();
            if (!(nbForcingDt == getSimulation().getNumberTimeStepsPerYear())) {
                System.out.println("In the current version, the time step of plankton biomass should match the time step of osmose config");
            }

            // initializing tables
            planktonNames = new String[nbPlankton];
            trophicLevel = new float[nbPlankton];
            minSize = new float[nbPlankton];
            maxSize = new float[nbPlankton];
            conversionFactors = new float[nbPlankton];
            prodBiomFactors = new float[nbPlankton];


            for (int i = 0; i < nbPlankton; i++) {
                // filling tables
                st.nextToken();
                planktonNames[i] = st.sval;
                getOsmose().planktonNamesTab[numSerie][i] = st.sval;
                st.nextToken();
                minSize[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
                maxSize[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
                trophicLevel[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
                conversionFactors[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
                prodBiomFactors[i] = (new Float(st.sval)).floatValue();
            }

            st.nextToken();
            int nbDimensionsGrid = new Integer(st.sval).intValue();
            if ((nbDimensionsGrid > 3) || (nbDimensionsGrid < 2)) {
                System.out.println("The dimension " + nbDimensionsGrid + " cannot be consider - should be 2 or 3");
            }
            st.nextToken();
            planktonDimX = new Integer(st.sval).intValue();
            st.nextToken();
            planktonDimY = new Integer(st.sval).intValue();
            if (nbDimensionsGrid == 3) {
                st.nextToken();
                planktonDimZ = new Integer(st.sval).intValue();
                st.nextToken();
                integrationDepth = new Float(st.sval).floatValue();
            }
        } catch (IOException ex) {
            System.out.println("Reading error of LTL structure file");
        }
    }
    
    /**
     * Computes the biomass of the specified plankton over the domain.
     * 
     * @param iPlankton, the index of the plankton group
     * @return the cumulated biomass over the domain in ton/km2
     */
    @Override
    public double getBiomass(int iPlankton) {
        double biomass = 0.d;
        for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    if (!getGrid().getCell(i, j).isLand()) {
                        biomass += ((Plankton) planktonList[iPlankton]).biomass[i][j];
                    }
                }
            }
        return biomass;
    }

    @Override
    public int getNbPlanktonGroups() {
        return nbPlankton;
    }

    @Override
    public String getPlanktonName(int indexGroup) {
        return planktonNames[indexGroup];
    }

    @Override
    public Plankton getPlankton(int indexGroup) {
        return planktonList[indexGroup];
    }

    @Override
    public int getPlanktonDimX() {
        return planktonDimX;
    }

    @Override
    public int getPlanktonDimY() {
        return planktonDimY;
    }

    @Override
    public int getPlanktonDimZ() {
        return planktonDimZ;
    }

    @Override
    public void savePlanktonBiomass(String format) {
        
        if (format.matches("csv")) {
            savePlanktonBiomassCSV();
        } else if (format.matches("netcdf")) {
            savePlanktonBiomassNetCDF();
        }
    }

    public void savePlanktonBiomassCSV() {
        File targetPath;
        File targetFile;
        PrintWriter pr;

        int numSerie = getOsmose().numSerie;
        String mortalityFile = getOsmose().outputPrefix[numSerie] + "_planktonBiomassMatrix_Simu" + getOsmose().numSimu + ".csv";

        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "planktonBiomass");
        targetPath.mkdirs();

        FileOutputStream planktonTime;
        try {
            targetFile = new File(targetPath, mortalityFile);
            planktonTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(planktonTime, true);

        for (int j = 0; j < getGrid().getNbLines(); j++) {
            pr.print(getSimulation().getYear() + getSimulation().getIndexTimeYear() / (float) getSimulation().getNumberTimeStepsPerYear());
            pr.print(';');
            for (int p = 0; p < nbPlankton; p++) {
                for (int i = 0; i < getGrid().getNbColumns(); i++) {
                    pr.print(planktonList[p].biomass[j][i]);
                    pr.print(";");

                }
            }
            pr.println();
        }
        pr.close();
    }

    private void savePlanktonBiomassNetCDF() {

        int numSerie = getOsmose().numSerie;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[getOsmose().numSerie]);
        StringBuilder filename = new StringBuilder("planktonBiomass");
        filename.append(File.separatorChar);
        filename.append("Simu");
        filename.append(numSerie);
        filename.append(File.separatorChar);
        filename.append("osm_integrated_Y");
        filename.append((getSimulation().getYear() + 1));
        filename.append("S");
        filename.append((getSimulation().getIndexTimeYear() + 1));
        filename.append(".nc");
        File file = new File(path, filename.toString());
        file.getParentFile().mkdirs();

        // Create NetcdfFileWriteable
        NetcdfFileWriteable nc = null;
        try {
            nc = NetcdfFileWriteable.createNew("");
            nc.setLocation(file.toString());
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }

        /*
         * Create dimensions
         */
        Dimension columnsDim = nc.addDimension("xi_rho", getGrid().getNbColumns());
        Dimension linesDim = nc.addDimension("eta_rho", getGrid().getNbLines());
        /*
         * Add variables
         */
        nc.addVariable("lon_rho", DataType.FLOAT, new Dimension[]{linesDim, columnsDim});
        nc.addVariableAttribute("lon_rho", "units", "degree");
        nc.addVariable("lat_rho", DataType.FLOAT, new Dimension[]{linesDim, columnsDim});
        nc.addVariableAttribute("lat_rho", "units", "degree");
        for (int p = 0; p < nbPlankton; p++) {
            nc.addVariable(getPlanktonName(p), DataType.DOUBLE, new Dimension[]{linesDim, columnsDim});
            nc.addVariableAttribute(getPlanktonName(p), "units", "tons/km2");
        }

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
            for (int i = 0; i < getGrid().getNbColumns(); i++) {
                for (int j = 0; j < getGrid().getNbLines(); j++) {
                    arrLon.set(j, i, getGrid().getCell(j, i).getLon());
                    arrLat.set(j, i, getGrid().getCell(j, i).getLat());
                }
            }
            nc.write("lon_rho", arrLon);
            nc.write("lat_rho", arrLat);
            /*
             * Write plankton biomass
             */
            //double area = 111.138d * getGrid().getdLat() * 111.138d * getGrid().getdLong();
            for (int p = 0; p < nbPlankton; p++) {
                ArrayDouble.D2 arrPlankton = new ArrayDouble.D2(getGrid().getNbLines(), getGrid().getNbColumns());
                for (int j = 0; j < getGrid().getNbLines(); j++) {
                    for (int i = 0; i < getGrid().getNbColumns(); i++) {
                        if (getGrid().getCell(j, i).isLand()) {
                            arrPlankton.set(j, i, Double.NaN);
                        } else {
                            double area = 111f * getGrid().getdLat() * 111f * (float) Math.cos(getGrid().getCell(j, i).getLat() * Math.PI / (90f * 2f)) * getGrid().getdLong();
                            arrPlankton.set(j, i, planktonList[p].biomass[j][i] / area);
                        }
                    }
                }
                nc.write(getPlanktonName(p), arrPlankton);
            }
            /*
             * Close NetCDF
             */
            nc.close();
        } catch (InvalidRangeException ex) {
            Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getNbForcingDt() {
        return nbForcingDt;
    }

    void setDimX(int nx) {
        this.planktonDimX = nx;
    }

    void setDimY(int ny) {
        this.planktonDimY = ny;
    }

    void setDimZ(int nz) {
        this.planktonDimZ = nz;
    }

    void initPlanktonList() {
        planktonList = new Plankton[getNbPlanktonGroups()];
        for (int i = 0; i < getNbPlanktonGroups(); i++) {
            planktonList[i] = new Plankton(planktonNames[i], minSize[i], maxSize[i], trophicLevel[i], conversionFactors[i], prodBiomFactors[i], getOsmose().planktonAccessCoeffMatrix[i]);
        }
    }

    Plankton[] getPlanktonList() {
        return planktonList;
    }

    Plankton getPlanktonGroup(int index) {
        return planktonList[index];
    }

    float getIntegrationDepth() {
        return integrationDepth;
    }
}
