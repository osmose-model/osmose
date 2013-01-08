/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.nc2.NetcdfFile;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 *
 * @author pverley
 */
public class BFMReader extends JPanel {

    //private static String varname = "P1c";
    //private static String varname = "P4c";
    //private static String varname = "O2o";
    private static String varname = "Z4c";
    private NetcdfFile nc;
    private String ncpathname = "/home/pverley/osmose/dev/io/bfm_adriatic/result_bfm199001_2w.nc";
    private float[][] variable;
    private int[][][] indexOceanpoint;
    private int im, jm, km;
    private float minValue, maxValue;
    float lon[][];
    float lat[][];
    float[][][] mask;
    /**
     * Dimension of the component.
     */
    private static int hi, wi;
    /**
     * Minimum latitude of the domain to display
     */
    private static double latmin;
    /**
     * Maximum latitude of the domain to display
     */
    private static double latmax;
    /**
     * Minimum longitude of the domain to display
     */
    private static double lonmin;
    /**
     * Maximum longitude of the domain to display
     */
    private static double lonmax;
    /**
     * BufferedImage in which the background (cost + bathymetry) has been
     * drawn.
     */
    private static BufferedImage background;
    /**
     * Associated {@code RenderingHints} object
     */
    private static RenderingHints hints = null;
    private static final double ONE_DEG_LATITUDE_IN_METER = 111138.d;
    //private int height = 1600, width = 1200;
    //private int height = 1200, width = 1000;
    private int height = 800, width = 600;
    private boolean isGridVisible = false;
    private Color colormin = Color.BLUE;
    private Color colormed = Color.GREEN;
    private Color colormax = Color.RED;

    public BFMReader() {

        hi = -1;
        wi = -1;

        hints = new RenderingHints(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

    }

    public void initReader() {
        loadNetcdf(ncpathname);
        variable = readVariable(varname);
        lon = readVariable("lon");
        lat = readVariable("lat");
        indexMapping();
        minValue = findMinValue();
        maxValue = findMaxValue();
    }

    private void loadNetcdf(String pathname) {
        try {
            nc = NetcdfFile.open(pathname);
        } catch (IOException ex) {
            Logger.getLogger(BFMReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private float[][] readVariable(String varname) {
        try {
            return (float[][]) nc.findVariable(varname).read().copyToNDJavaArray();
        } catch (IOException ex) {
            Logger.getLogger(BFMReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private float findMaxValue() {
        float max = -1.f;
        for (int j = 0; j < jm; j++) {
            for (int i = 0; i < im; i++) {
                float tmp = getMeanValueSurface(i, j);
                if (tmp > max) {
                    max = tmp;
                }
            }
        }
        return max;
    }

    private float findMinValue() {
        float min = Float.MAX_VALUE;
        for (int j = 0; j < jm; j++) {
            for (int i = 0; i < im; i++) {
                float tmp = getMeanValueSurface(i, j);
                if (tmp < min) {
                    min = tmp;
                }
            }
        }
        return min;
    }

    private int ijk2oceanpoint(int i, int j, int k) {
        return indexOceanpoint[k][j][i];
    }

    private void indexMapping() {
        try {
            mask = (float[][][]) nc.findVariable("mask").read().copyToNDJavaArray();
            int[] oceanpoint = (int[]) nc.findVariable("oceanpoint").read().copyToNDJavaArray();
            km = nc.findDimension("z").getLength();
            jm = nc.findDimension("y").getLength();
            im = nc.findDimension("x").getLength();
            int cont = 0;
            indexOceanpoint = new int[km][jm][im];
            for (int k = 0; k < km; k++) {
                for (int j = 0; j < jm; j++) {
                    for (int i = 0; i < im; i++) {
                        if (mask[k][j][i] > 0) {
                            //indexOceanpoint[k][j][i] = oceanpoint[cont];
                            indexOceanpoint[k][j][i] = cont;
                            cont++;
                        } else {
                            indexOceanpoint[k][j][i] = -1;
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(BFMReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean isWater(int i, int j) {
        return mask[0][j][i] > 0;
    }

    public float getValue(int i, int j, int k, int time) {
        int oceanpoint = ijk2oceanpoint(i, j, k);
        if (oceanpoint > 0) {
            return variable[time][oceanpoint];
        } else {
            return Float.NaN;
        }
    }

    public float getMeanValueSurface(int i, int j) {
        int nbTime = variable.length;
        float o2 = 0.f;
        for (int t = 0; t < nbTime; t++) {
            o2 += getValue(i, j, 0, t);
        }
        return o2 / nbTime;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMedValue() {
        return 0.5f * (maxValue + minValue);
    }

    private void paintColorbar(Graphics2D g, int w, int h) {
        g = (Graphics2D) g.create();

        int wbar = 300;
        int hbar = 20;
        //int xbar = (w - wbar) - hbar;
        int xbar = hbar;
        int ybar = h - 3 * hbar / 2;
        float x = Math.abs((getMedValue() - minValue) / (maxValue - minValue));
        float offset = 0.f;

        Rectangle2D bar = new Rectangle2D.Double(0.0, 0.0, x * wbar, hbar);
        g.translate(xbar, ybar);
        g.setColor(Color.BLACK);
        g.draw(bar);

        Ellipse2D corner = new Ellipse2D.Double(-0.5f * hbar, 0.f, hbar, hbar);
        g.draw(corner);
        g.setColor(colormin);
        g.fill(corner);

        Paint paint = g.getPaint();
        GradientPaint painter = new GradientPaint(0, 0, colormin, (x + offset) * wbar, hbar, colormed);
        g.setPaint(painter);
        g.fill(bar);

        bar = new Rectangle2D.Double(x * wbar, 0.0, (1 - x) * wbar, hbar);
        g.setColor(Color.BLACK);
        g.draw(bar);

        corner = new Ellipse2D.Double(wbar - 0.5 * hbar, 0.0, hbar, hbar);
        g.draw(corner);
        g.setColor(colormax);
        g.fill(corner);

        painter = new GradientPaint((x - offset) * wbar, 0, colormed, wbar, hbar, colormax);
        g.setPaint(painter);
        g.fill(bar);

        FontRenderContext context = g.getFontRenderContext();
        Font font = new Font("Dialog", Font.PLAIN, 11);
        TextLayout layout = new TextLayout(String.valueOf(minValue), font, context);
        Rectangle2D bounds = layout.getBounds();

        float text_x = 10;
        float text_y = (float) ((hbar - layout.getAscent() - layout.getDescent()) / 2.0) + layout.getAscent() - layout.getLeading();
        g.setColor(Color.BLACK);
        layout.draw(g, text_x, text_y);

        layout = new TextLayout(varname, font, context);
        bounds = layout.getBounds();
        text_x = (float) ((wbar - bounds.getWidth()) / 2.0);
        text_y = (float) ((hbar - layout.getAscent() - layout.getDescent()) / 2.0) + layout.getAscent() - layout.getLeading();
        g.setColor(Color.BLACK);
        layout.draw(g, text_x, text_y);

        layout = new TextLayout(String.valueOf(maxValue), font, context);
        bounds = layout.getBounds();
        text_x = (float) (wbar - bounds.getWidth() - 10);
        text_y = (float) ((hbar - layout.getAscent() - layout.getDescent()) / 2.0) + layout.getAscent() - layout.getLeading();
        g.setColor(Color.BLACK);
        layout.draw(g, text_x, text_y);

        g.setPaint(paint);
        g.translate(-xbar, -ybar);
        g.dispose();
    }

    /////////////////////////////////////////////////////////
    @Override
    public void paintComponent(Graphics g) {

        int h = getHeight();
        int w = getWidth();

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHints(hints);

        /** Clear the graphics */
        g2.clearRect(0, 0, w, h);

        /* Redraw the background when size changed */
        if (hi != h || wi != w) {
            drawBackground(g2, w, h);
            hi = h;
            wi = w;
        }
        /* Draw the background into the graphics */
        g2.drawImage(background, 0, 0, this);
    }

    private void drawBackground(Graphics2D g2, int w, int h) {

        background = g2.getDeviceConfiguration().createCompatibleImage(w, h);
        Graphics2D graphic = background.createGraphics();
        graphic.setColor(new Color(223, 212, 200));
        graphic.fillRect(0, 0, w, h);

        CellUI cell = new CellUI();
        for (int i = im - 1; i-- > 0;) {
            for (int j = jm - 1; j-- > 0;) {
                cell.draw(i, j, w, h);
                graphic.setColor(cell.getColor(i, j));
                graphic.fillPolygon(cell);
                if (isGridVisible) {
                    graphic.setColor(Color.LIGHT_GRAY);
                    graphic.drawPolygon(cell);
                }
            }
        }

        paintColorbar(graphic, w, h);
    }

    public void setGridVisible(boolean visible) {
        isGridVisible = visible;
        repaintBackground();
    }

    /**
     * Forces the background to repaint.
     */
    public void repaintBackground() {

        hi = -1;
        wi = -1;
        repaint();
    }

    /**
     * Transforms particle (x, y) coordinates into a screen point.
     *
     * @param xgrid a double, the particle x-coordinate
     * @param ygrid a double, the particle y-coordinate
     * @param w the width of the component
     * @param h the height of the component
     * @return an int[], the corresponding (x-sreen, y-screen) coordinates.
     */
    private int[] grid2Screen(double xgrid, double ygrid, int w, int h) {

        int[] point = new int[2];
        int igrid, jgrid;
        double dx, dy;
        double[] p1, p2, p3, p4;

        igrid = (int) xgrid;
        jgrid = (int) ygrid;
        dx = xgrid - igrid;
        dy = ygrid - jgrid;
        p1 = grid2Screen(igrid, jgrid, w, h);
        p2 = grid2Screen(igrid + 1, jgrid, w, h);
        p3 = grid2Screen(igrid, jgrid + 1, w, h);
        p4 = grid2Screen(igrid + 1, jgrid + 1, w, h);

        for (int n = 0; n < 2; n++) {
            double interp = (1.d - dx) * (1.d - dy) * p1[n] + dx * (1.d - dy) * p2[n] + (1.d - dx) * dy * p3[n] + dx * dy * p4[n];
            point[n] = Double.isNaN(interp)
                    ? -1
                    : (int) interp;
        }
        return point;
    }

    /**
     * Transforms a grid cell coordinate (i, j) into a screen point.
     *
     * @param igrid an int, the i-grid coordinate
     * @param jgrid an int, the j-grid coordiante
     * @param w the width of the component
     * @param h the height of the component
     * @return a double[], the corresponding (x-sreen, y-screen) coordinates.
     */
    private double[] grid2Screen(int igrid, int jgrid, int w, int h) {

        double[] point = new double[2];

        point[0] = w * ((lon[jgrid][igrid] - lonmin)
                / Math.abs(lonmax - lonmin));
        point[1] = h * (1.d - ((lat[jgrid][igrid] - latmin)
                / Math.abs(latmax - latmin)));

        return (point);
    }

    public void init() {

        initReader();

        getDimGeogArea();
        //System.out.println("lonmin: " + lonmin + " lonmax: " + lonmax + " latmin: " + latmin  + " latmax: " + latmax);
        double avgLat = 0.5d * (latmin + latmax);

        double dlon = Math.abs(lonmax - lonmin) * ONE_DEG_LATITUDE_IN_METER * Math.cos(Math.PI * avgLat / 180.d);
        double dlat = Math.abs(latmax - latmin) * ONE_DEG_LATITUDE_IN_METER;

        double ratio = dlon / dlat;
        width = (int) (height * ratio);
        /*if (ratio > 1) {
        width = (int) (height * ratio);
        } else if (ratio != 0.d) {
        height = (int) (width / ratio);
        }*/
        //setPreferredSize(new Dimension(width, height));
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    private class CellUI extends Polygon {

        ///////////////////////////////
        // Declaration of the variables
        ///////////////////////////////
        /**
         * The (x-screen, y-screen) coordinates of the quadrilateral.
         * point[0:3][0:1] first dimension refers to the number of points (4
         * in this case) and the second dimension, the (x, y) coordinates.
         */
        private int[][] points;

        ///////////////
        // Constructors
        ///////////////
        /**
         * Constructs an empty <code>CellUI</code>
         */
        public CellUI() {

            points = new int[4][2];
        }

        ////////////////////////////
        // Definition of the methods
        ////////////////////////////
        /**
         * Computes the coordinates of the quadrilateral, around the specified
         * grid point(i, j).
         *
         * @param i an int, the i-coordinate of the cell
         * @param j an int, the j-coordinate of the cell
         * @param w the width of the component
         * @param h the height of the component
         */
        public void draw(int i, int j, int w, int h) {

            this.reset();
            points[0] = grid2Screen(i - 0.5f, j - 0.5f, w, h);
            points[1] = grid2Screen(i + 0.5f, j - 0.5f, w, h);
            points[2] = grid2Screen(i + 0.5f, j + 0.5f, w, h);
            points[3] = grid2Screen(i - 0.5f, j + 0.5f, w, h);

            for (int n = 0; n < 4; n++) {
                if (points[n][0] < 0 || points[n][1] < 0) {
                    reset();
                    return;
                }
                addPoint(points[n][0], points[n][1]);
            }
        }

        private Color getColor(int i, int j) {

            if (!isWater(i, j)) {
                return Color.DARK_GRAY;
            } else {
                //return Color.CYAN;
                return getColor(getMeanValueSurface(i, j));
            }
        }

        private Color getColor(float value) {

            if (Float.isNaN(value)) {
                return Color.WHITE;
            }

            if (value <= getMedValue()) {
                float xval = Math.abs(bound((getMedValue() - value) / (getMedValue() - minValue)));
                return (new Color(((int) (xval * colormin.getRed()
                        + (1 - xval) * colormed.getRed())),
                        ((int) (xval * colormin.getGreen()
                        + (1 - xval) * colormed.getGreen())),
                        ((int) (xval * colormin.getBlue()
                        + (1 - xval) * colormed.getBlue()))));
            } else {
                float xval = Math.abs(bound((maxValue - value) / (maxValue - getMedValue())));
                return (new Color(((int) (xval * colormed.getRed()
                        + (1 - xval) * colormax.getRed())),
                        ((int) (xval * colormed.getGreen()
                        + (1 - xval) * colormax.getGreen())),
                        ((int) (xval * colormed.getBlue()
                        + (1 - xval) * colormax.getBlue()))));
            }
        }

        private float bound(float x) {

            return Math.max(Math.min(1.f, x), 0.f);
        }
        //---------- End of class CellUI
    }

    private void getDimGeogArea() {

        //--------------------------------------
        // Calculate the Physical Space extrema

        lonmin = Double.MAX_VALUE;
        lonmax = -lonmin;
        latmin = Double.MAX_VALUE;
        latmax = -latmin;
        int i = im;
        int j = 0;

        while (i-- > 0) {
            j = jm;
            while (j-- > 0) {
                if (lon[j][i] >= lonmax) {
                    lonmax = lon[j][i];
                }
                if (lon[j][i] <= lonmin) {
                    lonmin = lon[j][i];
                }
                if (lat[j][i] >= latmax) {
                    latmax = lat[j][i];
                }
                if (lat[j][i] <= latmin) {
                    latmin = lat[j][i];
                }
            }
        }

        //System.out.println("depth max " + depthMax);

        double double_tmp;
        if (lonmin > lonmax) {
            double_tmp = lonmin;
            lonmin = lonmax;
            lonmax = double_tmp;
        }

        if (latmin > latmax) {
            double_tmp = latmin;
            latmin = latmax;
            latmax = double_tmp;
        }

        System.out.println("lonmin " + lonmin + " lonmax " + lonmax + " latmin " + latmin + " latmax " + latmax);
    }

    public static void main(String args[]) {

        BFMReader bfm = new BFMReader();
        bfm.init();
        bfm.setGridVisible(false);

        //1. Create the frame.
        JFrame frame = new JFrame("BFM grid - " + varname + " variable at sea surface.");

        //2. Optional: What happens when the frame closes?
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //3. Create components and put them in the frame.
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(bfm);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        //4. Size the frame.
        frame.setPreferredSize(new Dimension(600, 800));
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        scrollPane.setPreferredSize(bfm.getSize());
        scrollPane.revalidate();
        frame.pack();
        frame.setLocationRelativeTo(null);

        //5. Show it.
        frame.setVisible(true);
    }
}
