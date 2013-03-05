/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util;

import java.util.logging.*;

public class OsmoseLogFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000);
        builder.append("osmose[").append(record.getLevel().toString().toLowerCase()).append("] - ");
        builder.append(formatMessage(record));
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