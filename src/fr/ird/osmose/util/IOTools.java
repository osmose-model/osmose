/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class IOTools {

    public static String resolvePath(String path) {
        String pathname = resolveFile(path);
        if (!pathname.endsWith(File.separator)) {
            pathname += File.separator;
        }
        return pathname;
    }

    public static String resolveFile(String filename) {
        try {
            File file = new File(System.getProperty("user.dir"));
            String pathname = new File(file.toURI().resolve(new File(filename).toURI())).getAbsolutePath();
            return pathname;
        } catch (Exception e) {
            return filename;
        }
    }

    public static boolean makeDirectories(String file) throws SecurityException {
        String path = file.substring(0, file.lastIndexOf(File.separator));
        return new File(path).mkdirs();
    }

    public static void copyFile(File src, File dest) throws IOException {

        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dest);

        java.nio.channels.FileChannel channelSrc = fis.getChannel();
        java.nio.channels.FileChannel channelDest = fos.getChannel();

        channelSrc.transferTo(0, channelSrc.size(), channelDest);

        fis.close();
        fos.close();
    }

    public static String cleanFilePath(String filePath) {
        String fpath = filePath.replace('/', File.separatorChar);
        return fpath;
    }

    public static void backup(File src, String destDirectory) throws IOException {

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");
        formatter.setCalendar(calendar);
        StringBuffer bakfilePath = new StringBuffer(destDirectory);
        if (!destDirectory.endsWith(File.separator)) {
            bakfilePath.append(File.separator);
        }
        bakfilePath.append(src.getName());
        bakfilePath.append(".");
        bakfilePath.append(formatter.format(calendar.getTime()));
        File bakfile = new File(bakfilePath.toString());

        makeDirectories(bakfilePath.toString());
        copyFile(src, bakfile);
    }

    public static void backup(File src) throws IOException {
        String destDirectory = System.getProperty("user.dir");
        if (!destDirectory.endsWith(File.separator)) {
            destDirectory += File.separator;
        }
        destDirectory += "bak";
        backup(src, destDirectory);
    }

    public static int getFileSize(URL url) {
        try {
            URLConnection connection;
            connection = url.openConnection();
            return connection.getContentLength();
        } catch (IOException ex) {
            Logger.getLogger(IOTools.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }

    public static String getHumanFileSize(URL url) {
        return fileSizeToHuman(getFileSize(url));
    }

    public static String fileSizeToHuman(int filesize) {
        String[] sbytes = new String[]{"Kb", "Mb", "Gb"};
        float length = (float) filesize;
        if (length < 1024) {
            return "unknown size";
        }
        String unit = sbytes[0];
        for (int i = 0; i < sbytes.length; i++) {
            if ((length / 1024.f) < 1) {
                break;
            }
            length = length / 1024.f;
            unit = sbytes[i];
        }
        NumberFormat dft = NumberFormat.getInstance(Locale.US);
        dft.setMaximumFractionDigits(1);
        dft.setMinimumFractionDigits(1);
        return dft.format(length) + " " + unit;
    }

    public static List<File> listFiles(File folder) {
        List<File> listf = new ArrayList();
        File[] list = folder.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.isDirectory()) {
                    listf.addAll(listFiles(f));
                } else {
                    listf.add(f);
                }
            }
        }
        return listf;
    }

    public static List<File> relativize(List<File> list, URI againstURI) {
        List<File> listrel = new ArrayList(list.size());
        for (File f : list) {
            listrel.add(new File(againstURI.relativize(f.toURI()).toString()));
        }
        return listrel;
    }

    public static void deleteDirectoryOnExit(File directory) {

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                deleteDirectoryOnExit(file);
            }
            file.deleteOnExit();
        }
        directory.deleteOnExit();
    }

    public static void deleteDirectory(File directory) {

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            }
            file.delete();
        }
        directory.delete();
    }

    public static void deleteRecursively(String pathname, String pattern) {

        File path = new File(pathname);
        //System.out.println("Delete recursively " + path);
        if (path.isDirectory()) {
            for (File file : path.listFiles(new MetaFilenameFilter(pattern))) {
                //System.out.println("  Deleted file " + file);
                file.delete();
            }
            File[] list = path.listFiles();
            if (null != list) {
                for (File folder : list) {
                    if (folder.isDirectory()) {
                        deleteRecursively(folder.getAbsolutePath(), pattern);
                    }
                }
                if (list.length == 0) {
                    path.delete();
                    //System.out.println("Deleted folder " + path);
                }
            }
        }
    }

    public static void browse(URI uri) throws IOException {

        // On tente d'abord avec la classe Desktop
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    // Si on arrive ici cela signifie que l'on a réussi
                    // => On sort de la méthode
                    desktop.browse(uri);
                    return;
                }
            }
        } catch (IOException e) {
            // ignored
            //System.err.println("Desktop.browse() : " + e.getMessage());
        }

        // Sinon on tente d'appeler directement le navigateur
        String[] commands = {
            // En utilisant la variable d'environnement $BROWSER (si elle existe)
            System.getenv("BROWSER"),
            // En utilisant les outils "génériques" des environnements de bureau :
            "xdg-open", // multi
            "kfmclient exec", // KDE
            "exo-open", // XFCE
            "gnome-open", // Gnome
            // En appellant directement les navigateurs
            "firefox",
            "konqueror",
            "netscape"
        };

        for (String cmd : commands) {
            if (cmd != null) { // $BROWSER peut être null
                try {
                    browse(cmd, uri);
                    return; // OK
                } catch (IOException e) {
                    // ignored
                    System.err.println(cmd + " : " + e.getMessage());
                }
            }
        }
        // Si on arrive ici cela signifie que l'on n'est arrivé à rien :
        throw new IOException("No browser found.");
    }

    /*
     * Utilise la commande pour ouvrir une URI
     */
    private static void browse(String command, URI uri) throws IOException {
        // On découpe d'abord la commande
        // (c'est préférable pour Runtime.exec()).
        String[] args = command.split(" +");
        args = Arrays.copyOf(args, args.length + 1);
        args[args.length - 1] = uri.toString();

        // Et on tente de lancer le process 
        final Runtime runtime = Runtime.getRuntime();
        final Process process = runtime.exec(args);
        // On ferme les flux pour éviter les deadlocks ;)
        process.getOutputStream().close();
        process.getInputStream().close();
        process.getErrorStream().close();
    }
}
