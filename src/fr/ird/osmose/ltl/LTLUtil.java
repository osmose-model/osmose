/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

/**
 *
 * @author pverley
 */
public class LTLUtil {

    /**
     *
     * @param data3d, the raw LTL data with a vertical dimension
     * {@code data3d[nz][ny][nx]}
     * @param depthLayer, an array of float that provides the depth of every
     * cell of the LTL grid. {@code depthLayer[nz][ny][nx]}
     * @param maxDepth, the maximum depth to be taken into account for the
     * vertical integration. Make sure the sign of the depth is consistent
     * between the depth of the LTL grid and the maximum depth.
     * @return the raw LTL data vertically integrated, in concentration of
     * plankton per surface unit.
     */
    public static double[][] verticalIntegration(float[][][] data3d, float[][][] depthLayer, float maxDepth) {
        int nx = data3d[0][0].length;
        int ny = data3d[0].length;
        int nz = data3d.length;
        double[][] integratedData = new double[ny][nx];
        double integr;
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                integr = 0.d;
                for (int k = 0; k < nz - 1; k++) {
                    if (depthLayer[k][j][i] > maxDepth) {
                        if (data3d[k][j][i] >= 0 && data3d[k + 1][j][i] >= 0) {
                            integr += (Math.abs(depthLayer[k][j][i] - depthLayer[k + 1][j][i])) * ((data3d[k][j][i] + data3d[k + 1][j][i]) / 2.d);
                        }
                    }
                }
                integratedData[j][i] = integr;
            }
        }
        return integratedData;
    }

}
