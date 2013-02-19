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
    private Plankton[] planktonList;     // list of plankton groups (here 4)
    private int nx;      // dimension of LTL model, here ROMS Plume (144 * 65 * 20)
    private int ny;
    private int nz;	// vertical dimension (20)
    private float integrationDepth;   // latitude and longitude of each cell of the LTL grid, used for interpolation
    List<Integer>[][] icoordLTLGrid;
    List<Integer>[][] jcoordLTLGrid;

    /**
     * Get the biomass of the specified plankton group on the LTL grid and
     * vertically integrated, for a given time step. The biomass is expressed in
     * the same unit as it is in the forcing file except that it is vertically
     * integrated (unit/m2).
     *
     * @param plankton, a plankton group
     * @param iStepSimu, the current step of the simulation
     * @return an array of dimension of the LTL grid with biomass vertically
     * integrated.
     */
    abstract float[][] getRawBiomass(Plankton plankton, int iStepSimu);

    /**
     * Converts the current time step of the simulation into the corresponding
     * time step of the LTL data.
     *
     * @param iStepSimu, the current step of the simulation.
     * @return the corresponding time step of the LTL data.
     */
    abstract int getIndexStepLTL(int iStepSimu);
    
    /**
     * Loads parameters about how the LTL biomass are provided.
     * 
     * @param ltlForcingFile, the pathname of the LTLForcing configuration
     * file.
     */
    abstract void readLTLForcingFile(String ltlForcingFile);
    
    /**
     * This function loads the LTL grid.
     * It loads longitude, latitude and depth of vertical levels.
     * It creates the index map between osmose cells and LTL grid cells
     * (fills up variables icoordLTLGrid & jcoordLTLGrid)
     */
    abstract void initLTLGrid();
    
    @Override
    public void init() {
        readLTLBasisFile(getOsmose().planktonStructureFileNameTab);
        readLTLForcingFile(getOsmose().planktonFileNameTab);
        initLTLGrid();
    }

    /**
     * Read LTL basic file with name of plankton, sizes, format of files...
     */
    private void readLTLBasisFile(String planktonStructureFileName) {

        String[] planktonNames = null;		// list of names of plankton groups
        float[] trophicLevel = null;			// list of TL of plankton groups
        float[] minSize = null, maxSize = null;		// list of min and max sizes of plankton groups
        float[] conversionFactors = null;		// list of conversionFactors of plankton groups
        float[] prodBiomFactors = null;

        FileInputStream LTLFile = null;
        try {
            LTLFile = new FileInputStream(new File(getOsmose().resolveFile(planktonStructureFileName)));
        } catch (FileNotFoundException ex) {
            System.out.println("LTL file " + planktonStructureFileName + " doesn't exist");
            System.exit(1);
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
            nx = new Integer(st.sval).intValue();
            st.nextToken();
            ny = new Integer(st.sval).intValue();
            if (nbDimensionsGrid == 3) {
                st.nextToken();
                nz = new Integer(st.sval).intValue();
                st.nextToken();
                integrationDepth = new Float(st.sval).floatValue();
            }
        } catch (IOException ex) {
            System.out.println("Reading error of LTL structure file");
            System.exit(1);
        }
        planktonList = new Plankton[getNumberPlanktonGroups()];
        for (int i = 0; i < getNumberPlanktonGroups(); i++) {
            planktonList[i] = new Plankton(i, planktonNames[i], minSize[i], maxSize[i], trophicLevel[i], conversionFactors[i], prodBiomFactors[i], getOsmose().planktonAccessCoeffMatrix[i]);
        }
    }

    public float[][] verticalIntegration(float[][][] data3d, float[][][] depthLayer, float maxDepth) {
        float[][] integratedData = new float[get_nx()][get_ny()];
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
    public void update(int iStepSimu) {

        // clear & update biomass
        for (Plankton plankton : planktonList) {
            plankton.updateBiomass(getBiomass(plankton, iStepSimu));
        }
    }

    @Override
    public int getNumberPlanktonGroups() {
        return nbPlankton;
    }

    @Override
    public Plankton getPlankton(int indexGroup) {
        return planktonList[indexGroup];
    }

    public int get_nx() {
        return nx;
    }

    public int get_ny() {
        return ny;
    }

    public int get_nz() {
        return nz;
    }

    public int getNbForcingDt() {
        return nbForcingDt;
    }

    void setDimX(int nx) {
        this.nx = nx;
    }

    void setDimY(int ny) {
        this.ny = ny;
    }

    void setDimZ(int nz) {
        this.nz = nz;
    }

    private float[][] getBiomass(Plankton plankton, int iStepSimu) {

        float[][] biomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];

        float[][] rawBiomass = getRawBiomass(plankton, iStepSimu);
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                float area = 111.f * getGrid().getdLat() * 111.f * (float) Math.cos(cell.getLat() * Math.PI / (90f * 2f)) * getGrid().getdLong();
                int nCells = getNbCellsLTLGrid(cell);
                for (int k = 0; k < nCells; k++) {
                    biomass[cell.get_igrid()][cell.get_jgrid()] += area * plankton.convertToTonPerKm2(rawBiomass[get_iLTL(cell).get(k)][get_jLTL(cell).get(k)]) / (float) nCells;
                }
            }
        }
        return biomass;
    }

    Plankton[] getPlanktonList() {
        return planktonList;
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
