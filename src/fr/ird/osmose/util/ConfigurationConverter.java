package fr.ird.osmose.util;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class ConfigurationConverter {

    private Configuration cfg;
    private Properties prop;
    
    ConfigurationConverter(String[] args) {
        
        // Get old configuration
        Osmose osmose = Osmose.getInstance();
        osmose.init(args);
        cfg = osmose.getConfiguration();
        
        // Create new Properties
        prop = new Properties();
        
        // convert
        convert();
        write();
    }
    
    private void convert() {
        prop.setProperty("", "");
    }
    
    private void write() {
        try {
            prop.store(new FileWriter(cfg.resolveFile(getFilename())), null);
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    String getFilename() {
        StringBuilder filename = new StringBuilder(cfg.getOutputPrefix());
        filename.append("_all-parameters.cfg");
        return filename.toString();
    }

    public static void main(String[] args) {
        new ConfigurationConverter(args).convert();
    }
}
