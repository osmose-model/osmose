package fr.ird.osmose.output;

import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
abstract public class AbstractIndicator extends SimulationLinker implements Indicator {

    private FileOutputStream fos;
    private PrintWriter prw;

    abstract String getFilename();

    abstract String getDescription();

    abstract String[] getHeaders();
    
    AbstractIndicator(int replica) {
        super(replica);
    }

    @Override
    public void init() {

        // Create parent directory
        File path = new File(getConfiguration().getOutputPathname() + getConfiguration().getOutputFolder());
        File file = new File(path, getFilename());
        file.getParentFile().mkdirs();
        try {
            // Init stream
            fos = new FileOutputStream(file, true);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AbstractIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
        prw = new PrintWriter(fos, true);

        prw.print("\"");
        prw.print(getDescription());
        prw.println("\"");
        prw.print("Time");
        String[] headers = getHeaders();
        for (int i = 0; i < headers.length; i++) {
            prw.print(";");
            prw.print(headers[i]);
        }
        prw.println();
    }

    @Override
    public void close() {
        if (null != prw) {
            prw.close();
        }
        if (null != fos) {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(AbstractIndicator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    void writeVariable(float time, double[] variable) {

        prw.print(time);
        for (int i = 0; i < variable.length; i++) {
            prw.print(";");
            prw.print((float) variable[i]);
            //pr.print((long) variable[i]);
            //System.out.println(filename + " " + time + " spec" + i + " " + variable[i]);
        }
        prw.println();
    }

    void writeVariable(float time, double[][] variable) {

        for (int i = 0; i < variable.length; i++) {
            prw.print(time);
            for (int j = 0; j < variable[i].length; j++) {
                prw.print(";");
                prw.print((float) variable[i][j]);
            }
            prw.println();
        }
        prw.println();
    }
}
