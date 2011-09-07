/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StreamTokenizer;

/**
 *
 * @author pverley
 */
public abstract class AbstractLTLForcing implements LTLForcing {

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
            if (!(nbForcingDt == getSimulation().getNbTimeStepsPerYear())) {
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
            return;
        }
    }

    @Override
    public void saveForDiet() {

        for (int p = 0; p < nbPlankton; p++) {
            getSimulation().biomPerStage[getSimulation().getNbSpecies() + p][0] = 0; //biomPerStage[][0] because just 1 stage per plankton group
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    if (!getGrid().getCell(i, j).isLand()) {
                        getSimulation().biomPerStage[getSimulation().getNbSpecies() + p][0] += ((Plankton) planktonList[p]).biomass[i][j];
                    }
                }
            }
        }
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

    public IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    Osmose getOsmose() {
        return Osmose.getInstance();
    }

    Simulation getSimulation() {
        return getOsmose().getSimulation();
    }

    @Override
    public void savePlanktonBiomass() {
        File targetPath;
        File targetFile;
        PrintWriter pr;

        int numSerie = getOsmose().numSerie;
        String mortalityFile = getOsmose().outputFileNameTab[numSerie] + "_planktonBiomassMatrix_Simu" + getOsmose().numSimu + ".csv";

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
            pr.print(getSimulation().getYear() + getSimulation().getIndexTime() / (float) getSimulation().getNbTimeStepsPerYear());
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
