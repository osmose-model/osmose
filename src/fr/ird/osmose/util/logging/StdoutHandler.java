/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 *
 * @author pverley
 */
public class StdoutHandler extends StreamHandler {

    public StdoutHandler() {
        super(System.out, new OsmoseLogFormatter());
        setLevel(Level.ALL);
    }

    /**
     * Publish a <tt>LogRecord</tt>.
     * <p>
     * The logging request was made initially to a <tt>Logger</tt> object, which
     * initialized the <tt>LogRecord</tt> and forwarded it here.
     * <p>
     * @param record description of the log event. A null record is silently
     * ignored and is not published
     */
    @Override
    public void publish(LogRecord record) {
        super.publish(record);
        flush();
    }

}
