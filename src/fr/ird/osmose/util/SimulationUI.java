/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util;

import fr.ird.osmose.Osmose;
import fr.ird.osmose.School;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author pverley
 */
public class SimulationUI extends SimulationLinker {

    /**
     * Time in milliseconds for holding animation in between two steps
     */
    private final static int SLEEP = 1000;
    /**
     * Height of the Simulation panel
     */
    private final static int height = 1600;
    
///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Width of the Simulation panel
     */
    private int width;
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
     * BufferedImage in which the background (cost + bathymetry) has been drawn.
     */
    private static BufferedImage background;
    /**
     * Associated {@code RenderingHints} object
     */
    private static RenderingHints hints = null;
    private static final double ONE_DEG_LATITUDE_IN_METER = 111138.d;
    private boolean isGridVisible = false;
    // JPanel
    private JPanel panel = new JPanel() {
        @Override
        public void paintComponent(Graphics g) {

            int h = getHeight();
            int w = getWidth();

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHints(hints);

            /**
             * Clear the graphics
             */
            g2.clearRect(0, 0, w, h);

            /* Redraw the background when size changed */
            if (hi != h || wi != w) {
                drawBackground(g2, w, h);
                hi = h;
                wi = w;
            }
            /* Draw the background into the graphics */
            g2.drawImage(background, 0, 0, this);

            /* Draw the particles */
            if (getSchoolSet() != null) {
                SchoolUI schoolUI = new SchoolUI();
                for (School school : SimulationUI.this.getSchoolSet().getPresentSchools()) {
                    schoolUI.draw(school, w, h);
                    g2.setColor(schoolUI.getColor(school.getSpeciesIndex()));
                    g2.fill(schoolUI);
                }
            }
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
    };
    private static JFrame frameUI;
    private static SimulationUI simulationUI;

///////////////
// Constructors
///////////////
    /**
     * Constructs an empty
     * <code>SimulationUI</code>, intializes the range of the domain and the
     * {@code RenderingHints}.
     */
    public SimulationUI() {
        super(0);

        hi = -1;
        wi = -1;

        hints = new RenderingHints(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        
        init();
    }

    private static void createUI() {
        try {

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {

                    //1. Create the frame.
                    frameUI = new JFrame("Osmose grid");
                    simulationUI = new SimulationUI();

                    //2. Optional: What happens when the frame closes?
                    frameUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                    //3. Create components and put them in the frame.
                    JScrollPane scrollPane = new JScrollPane();
                    scrollPane.setViewportView(simulationUI.getPanel());
                    frameUI.getContentPane().add(scrollPane, BorderLayout.CENTER);

                    //4. Size the frame.
                    frameUI.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    scrollPane.setPreferredSize(simulationUI.getPanel().getSize());
                    scrollPane.revalidate();
                    frameUI.pack();
                    frameUI.setLocationRelativeTo(null);

                    //5. Show it.
                    simulationUI.setGridVisible(true);
                    frameUI.setVisible(true);
                }
            });
        } catch (InterruptedException ex) {
            Logger.getLogger(SimulationUI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(SimulationUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void step(final int year, final int iStepYear) {

        if (null == frameUI) {
            createUI();
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    frameUI.setTitle("Year " + year + " Step " + iStepYear);
                    simulationUI.getPanel().repaint();
                }
            });
        } catch (InterruptedException ex) {
            Logger.getLogger(SimulationUI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(SimulationUI.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(SLEEP);
        } catch (InterruptedException ex) {
            Logger.getLogger(SimulationUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public JPanel getPanel() {
        return panel;
    }

    private void drawBackground(Graphics2D g2, int w, int h) {

        background = g2.getDeviceConfiguration().createCompatibleImage(w, h);
        Graphics2D graphic = background.createGraphics();
        graphic.setColor(new Color(223, 212, 200));
        graphic.fillRect(0, 0, w, h);

        CellUI cell = new CellUI();
        for (int j = getGrid().get_ny() - 1; j-- > 0;) {
            for (int i = getGrid().get_nx() - 1; i-- > 0;) {
                cell.draw(i, j, w, h);
                graphic.setColor(cell.getColor(i, j));
                graphic.fillPolygon(cell);
                if (isGridVisible) {
                    graphic.setColor(Color.WHITE);
                    graphic.drawPolygon(cell);
                }
            }
        }
    }

    public void setGridVisible(boolean visible) {
        isGridVisible = visible;
        repaintBackground();
    }

    /**
     * Forces the background to repaint.
     */
    private void repaintBackground() {

        hi = -1;
        wi = -1;
        panel.repaint();
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

        point[0] = w * ((getGrid().getCell(igrid, jgrid).getLon() - lonmin)
                / Math.abs(lonmax - lonmin));
        point[1] = h * (1.d - ((getGrid().getCell(igrid, jgrid).getLat() - latmin)
                / Math.abs(latmax - latmin)));

        return (point);
    }

    private void init() {

        latmin = getGrid().getLatMin();
        latmax = getGrid().getLatMax();
        lonmin = getGrid().getLongMin();
        lonmax = getGrid().getLongMax();
        //System.out.println("ny: " + getGrid().get_ny());
        //System.out.println("nx: " + getGrid().get_nx());

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

    /**
     * Computes the geodesic distance between the two points (lat1, lon1) and
     * (lat2, lon2)
     *
     * @param lat1 a double, the latitude of the first point
     * @param lon1 a double, the longitude of the first point
     * @param lat2 double, the latitude of the second point
     * @param lon2 double, the longitude of the second point
     * @return a double, the curvilinear absciss s(A[lat1, lon1]B[lat2, lon2])
     */
    public static double geodesicDistance(double lat1, double lon1, double lat2, double lon2) {

        double lat1_rad = Math.PI * lat1 / 180.d;
        double lat2_rad = Math.PI * lat2 / 180.d;
        double lon1_rad = Math.PI * lon1 / 180.d;
        double lon2_rad = Math.PI * lon2 / 180.d;

        double d = 2 * 6367000.d
                * Math.asin(Math.sqrt(Math.pow(Math.sin((lat2_rad - lat1_rad) / 2), 2)
                + Math.cos(lat1_rad) * Math.cos(lat2_rad) * Math.pow(Math.sin((lon2_rad - lon1_rad) / 2), 2)));

        return d;
    }

    public static Osmose getOsmose() {
        return Osmose.getInstance();
    }

    private class CellUI extends Polygon {

        ///////////////////////////////
        // Declaration of the variables
        ///////////////////////////////
        /**
         * The (x-screen, y-screen) coordinates of the quadrilateral.
         * point[0:3][0:1] first dimension refers to the number of points (4 in
         * this case) and the second dimension, the (x, y) coordinates.
         */
        private int[][] points;

        ///////////////
        // Constructors
        ///////////////
        /**
         * Constructs an empty
         * <code>CellUI</code>
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

            if (getGrid().getCell(i, j).isLand()) {
                return Color.DARK_GRAY;
            } else {
                return new Color(150, 150, 255);
            }
        }
        //---------- End of class CellUI
    }

    //////////////////////////////////////////////////////////////////////////////
    /**
     * This class is the graphical representation of a {@code School} object.
     * The Particle is represented by an {@code Ellipse2D} with an associated
     * color.
     */
    private class SchoolUI extends Ellipse2D.Double {

        private Color[] colors = {Color.BLUE,
            Color.GRAY,
            Color.MAGENTA,
            Color.BLUE,
            Color.PINK,
            Color.ORANGE,
            Color.RED,
            Color.WHITE,
            Color.YELLOW,
            Color.GREEN,
            Color.CYAN,
            Color.BLACK
        };

        ////////////////////////////
        // Definition of the methods
        ////////////////////////////
        /**
         * Draws the particle at specified grid point
         *
         * @param data a float[] (xgrid, ygrid) particle's coordinate.
         * @param w the width of the component
         * @param h the height of the component
         */
        private void draw(School school, int w, int h) {

            double xs = school.getCell().get_igrid() + Math.random() - 0.5d;
            xs = Math.min(xs, getGrid().get_nx() - 2);
            double ys = school.getCell().get_jgrid() + Math.random() - 0.5d;
            ys = Math.min(ys, getGrid().get_ny() - 2);
            int[] corner = grid2Screen(xs, ys, w, h);
            double length = school.getLength() / 4;
            length = Math.min(Math.max(length, 1), 50);
            setFrame(corner[0] - 0.5 * length, corner[1] - 1.5, length, Math.min(length, 3));
            //setFrame(corner[0], corner[1], 1, 1);
        }

        /**
         * Gets the color of the particle.
         *
         * @return the Color of the particle
         */
        private Color getColor(int iSpec) {

            return colors[iSpec];
        }
        //---------- End of class SchoolUI
    }
}
