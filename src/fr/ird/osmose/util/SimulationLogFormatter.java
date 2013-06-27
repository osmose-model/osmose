/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 *
 * @author pverley
 */
public class SimulationLogFormatter extends Formatter {
    
    private final int iSimulation;
    
    public SimulationLogFormatter(int iSimulation) {
        this.iSimulation = iSimulation;
    }
 
    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000);
        builder.append("  simulation#");
        builder.append(iSimulation);
        builder.append("[").append(record.getLevel().toString().toLowerCase()).append("] - ");
        builder.append(formatMessage(record));
        if (null != record.getThrown()) {
            builder.append(" | ");
            builder.append(record.getThrown().getMessage());
        }
        builder.append("\n");
        return builder.toString();
    }
 
    @Override
    public String getHead(Handler h) {
        return super.getHead(h);
    }
 
    @Override
    public String getTail(Handler h) {
        return super.getTail(h);
    }
}
