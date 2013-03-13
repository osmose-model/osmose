/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

import fr.ird.osmose.Cell;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.Plankton;
import fr.ird.osmose.grid.IGrid;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public abstract class AbstractLTLForcing implements LTLForcing {

    private int nx;      // dimension of LTL model, here ROMS Plume (144 * 65 * 20)
    private int ny;
    private int nz;	// vertical dimension (20)
    List<Integer>[][] icoordLTLGrid;
    List<Integer>[][] jcoordLTLGrid;
    
    /**
     * Array of the NetCDF variable of the plankton fields.
     * @return 
     */
    abstract public String[] getPlanktonFieldName();
    /**
     * List of the NetCDF files.
     * @return 
     */
    abstract public String[] getNetcdfFile();

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
    abstract float[][] getRawBiomass(int iPlankton, int iStepSimu);

    /**
     * Loads parameters about how the LTL biomass are provided.
     *
     * @param ltlForcingFile, the pathname of the LTLForcing configuration file.
     */
    abstract void readLTLForcingFile();

    /**
     * This function loads the LTL grid. It loads longitude, latitude and depth
     * of vertical levels. It creates the index map between osmose cells and LTL
     * grid cells (fills up variables icoordLTLGrid & jcoordLTLGrid)
     */
    abstract void initLTLGrid();

    /**
     * Converts the current time step of the simulation into the corresponding
     * time step of the LTL data.
     *
     * @param iStepSimu, the current step of the simulation.
     * @return the corresponding time step of the LTL data.
     */
    public int getIndexStepLTL(int iStepSimu) {
        return iStepSimu % getConfiguration().getNumberLTLSteps();
    }
    
    @Override
    public void init() {
        readLTLForcingFile();
        initLTLGrid();
    }

    public float[][] verticalIntegration(float[][][] data3d, float[][][] depthLayer, float maxDepth) {
        float[][] integratedData = new float[get_ny()][get_nx()];
        float integr;
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                integr = 0f;
                for (int k = 0; k < nz - 1; k++) {
                    if (depthLayer[k][j][i] > maxDepth) {
                        if (data3d[k][j][i] >= 0 && data3d[k + 1][j][i] >= 0) {
                            integr += (Math.abs(depthLayer[k][j][i] - depthLayer[k + 1][j][i])) * ((data3d[k][j][i] + data3d[k + 1][j][i]) / 2f);
                        }
                    }
                }
                integratedData[j][i] = integr;
            }
        }
        return integratedData;
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

    void setDimX(int nx) {
        this.nx = nx;
    }

    void setDimY(int ny) {
        this.ny = ny;
    }

    void setDimZ(int nz) {
        this.nz = nz;
    }

    @Override
    public float[][] computeBiomass(Plankton plankton, int iStepSimu) {

        float[][] biomass = new float[getGrid().get_ny()][getGrid().get_nx()];

        float[][] rawBiomass = getRawBiomass(plankton.getIndex(), iStepSimu);
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                float area = 111.f * getGrid().getdLat() * 111.f * (float) Math.cos(cell.getLat() * Math.PI / (90f * 2f)) * getGrid().getdLong();
                int nCells = getNbCellsLTLGrid(cell);
                for (int k = 0; k < nCells; k++) {
                    biomass[cell.get_jgrid()][cell.get_igrid()] += area * plankton.convertToTonPerKm2(rawBiomass[get_jLTL(cell).get(k)][get_iLTL(cell).get(k)]) / (float) nCells;
                }
            }
        }
        return biomass;
    }

    int getNbCellsLTLGrid(int i, int j) {
        return (null == icoordLTLGrid[j][i])
                ? 0
                : icoordLTLGrid[j][i].size();
    }

    int getNbCellsLTLGrid(Cell cell) {
        return getNbCellsLTLGrid(cell.get_igrid(), cell.get_jgrid());
    }

    List<Integer> get_iLTL(Cell cell) {
        return icoordLTLGrid[cell.get_jgrid()][cell.get_igrid()];
    }

    List<Integer> get_jLTL(Cell cell) {
        return jcoordLTLGrid[cell.get_jgrid()][cell.get_igrid()];
    }

    static Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }

    static IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }
    
    static Logger getLogger() {
        return Osmose.getInstance().getLogger();
    }
}
