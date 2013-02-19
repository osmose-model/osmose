/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

import fr.ird.osmose.Cell;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.Plankton;
import fr.ird.osmose.grid.IGrid;
import java.io.*;
import java.util.List;

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
    List<Integer>[][] icoordLTLGrid;
    List<Integer>[][] jcoordLTLGrid;
    
    /**
     * Get the biomass of the specified plankton group on the LTL grid and
     * vertically integrated, for a given time step.
     * The biomass is expressed in the same unit as it is in the forcing file
     * except that it is vertically integrated (unit/m2).
     * @param plankton, a plankton group
     * @param iStepSimu, the current step of the simulation
     * @return an array of dimension of the LTL grid with biomass vertically
     * integrated.
     */
    abstract float[][] getRawBiomass(Plankton plankton, int iStepSimu);
    /**
     * Converts the current time step of the simulation into the corresponding
     * time step of the LTL data.
     * @param iStepSimu, the current step of the simulation.
     * @return the corresponding time step of the LTL data.
     */
    abstract public int getIndexStepLTL(int iStepSimu);

    @Override
    /**
     * Read LTL basic file with name of plankton, sizes, format of files...
     */
    public void readLTLConfigFile1(String planktonStructureFileName) {

        FileInputStream LTLFile;
        try {
            LTLFile = new FileInputStream(new File(getOsmose().resolveFile(planktonStructureFileName)));
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
            if (!(nbPlankton == getOsmose().nbPlanktonGroupsTab)) {
                System.out.println("The number of plankton group in plankton structure file does not match the one from config file");
            }
            st.nextToken();
            nbForcingDt = (new Integer(st.sval)).intValue();
            if (!(nbForcingDt == getOsmose().nStepYear)) {
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
                getOsmose().planktonNamesTab[i] = st.sval;
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
            System.exit(1);
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

    public float[][] verticalIntegration(float[][][] data3d, float[][][] depthLayer, float maxDepth) {
        float[][] integratedData = new float[getPlanktonDimX()][getPlanktonDimY()];
        float integr;
        for (int i = 0; i < depthLayer.length; i++) {
            for (int j = 0; j < depthLayer[i].length; j++) {
                integr = 0f;
                for (int k = 0; k < depthLayer[i][j].length - 1; k++) {
                    if (depthLayer[i][j][k] > maxDepth) {
                        if (data3d[i][j][k] >= 0 && data3d[i][j][k + 1] >= 0) {
                            integr += (Math.abs(depthLayer[i][j][k] - depthLayer[i][j][k + 1])) * ((data3d[i][j][k] + data3d[i][j][k + 1]) / 2f);
                        }
                    }
                }
                integratedData[i][j] = integr;
            }
        }
        return integratedData;
    }
    
    @Override
    public void updatePlankton(int iStepSimu) {

        // clear & update biomass
        for (int p = 0; p < getNbPlanktonGroups(); p++) {
            getPlanktonGroup(p).clearPlankton();
            getPlanktonGroup(p).updateBiomass(getBiomass(getPlanktonGroup(p), iStepSimu));
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

    public int getPlanktonDimX() {
        return planktonDimX;
    }

    public int getPlanktonDimY() {
        return planktonDimY;
    }

    public int getPlanktonDimZ() {
        return planktonDimZ;
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

    @Override
    public void createPlanktonGroups() {
        planktonList = new Plankton[getNbPlanktonGroups()];
        for (int i = 0; i < getNbPlanktonGroups(); i++) {
            planktonList[i] = new Plankton(i, planktonNames[i], minSize[i], maxSize[i], trophicLevel[i], conversionFactors[i], prodBiomFactors[i], getOsmose().planktonAccessCoeffMatrix[i]);
        }
    }

    private float[][] getBiomass(Plankton plankton, int iStepSimu) {

        float[][] biomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
        
        float[][] rawBiomass = getRawBiomass(plankton, iStepSimu);
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                float area = 111.f * getGrid().getdLat() * 111.f * (float) Math.cos(cell.getLat() * Math.PI / (90f * 2f)) * getGrid().getdLong();
                int nCells = getNbCellsLTLGrid(cell);
                for (int k = 0; k < nCells; k++) {
                    biomass[cell.get_igrid()][cell.get_jgrid()] += area * plankton.unitConversion(rawBiomass[get_iLTL(cell).get(k)][get_jLTL(cell).get(k)]) / (float) nCells;
                }
            }
        }
        return biomass;
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

    int getNbCellsLTLGrid(int i, int j) {
        return (null == icoordLTLGrid[i][j])
                ? 0
                : icoordLTLGrid[i][j].size();
    }

    int getNbCellsLTLGrid(Cell cell) {
        return getNbCellsLTLGrid(cell.get_igrid(), cell.get_jgrid());
    }

    List<Integer> get_iLTL(Cell cell) {
        return icoordLTLGrid[cell.get_igrid()][cell.get_jgrid()];
    }

    List<Integer> get_jLTL(Cell cell) {
        return jcoordLTLGrid[cell.get_igrid()][cell.get_jgrid()];
    }

    static Osmose getOsmose() {
        return Osmose.getInstance();
    }

    static IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }
}
