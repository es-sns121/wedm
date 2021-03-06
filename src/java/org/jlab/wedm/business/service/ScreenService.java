package org.jlab.wedm.business.service;

import static org.jlab.wedm.persistence.io.ColorListParser.COLOR_FILE_PATH;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jlab.wedm.persistence.io.ColorListParser;
import org.jlab.wedm.persistence.io.EDLParser;
import org.jlab.wedm.persistence.io.ScreenParser;
import org.jlab.wedm.persistence.model.ColorPalette;
import org.jlab.wedm.persistence.model.HtmlScreen;
import org.jlab.wedm.persistence.model.Macro;
import org.jlab.wedm.persistence.model.Screen;

/**
 *
 * @author slominskir
 */
public class ScreenService {

    private static final Logger LOGGER = Logger.getLogger(ScreenService.class.getName());

    private ColorPalette colorList;
    public static final ConcurrentHashMap<String, HtmlScreen> SCREEN_CACHE
            = new ConcurrentHashMap<>();

    private static final boolean CACHE_SCREENS_ENABLED = true;

    /*static {
        if (CACHE_SCREENS_ENABLED) {
            FileChangeWatcher watcher = new FileChangeWatcher();
            Thread t = new Thread(watcher);
            t.setDaemon(true);
            t.start();
        }
    }*/

    public ScreenService() throws FileNotFoundException {
        long start = System.currentTimeMillis();
        loadColorFile();
        long end = System.currentTimeMillis();

        LOGGER.log(Level.FINEST, "Color List Load time: (seconds) {0}", (end - start) / 1000.0);
    }

    public HtmlScreen load(String name, List<Macro> macros) throws FileNotFoundException,
            IOException {

        URL edl = EDLParser.getEdlURL(name);
        String url = edl.toString();

        HtmlScreen screen = SCREEN_CACHE.get(url);

        if (url.isEmpty())
            return null;

        if(screen != null) {
            if(edl.openConnection().getLastModified() > screen.getModifiedDate()) {
                LOGGER.log(Level.WARNING, "Resource changed so flushing cache: {0}", url);
                SCREEN_CACHE.remove(url);
                screen = null;
            }
        }

        if (screen == null) {
            ScreenParser parser = new ScreenParser();

            long start = System.currentTimeMillis();
            Screen parsedScreen = parser.parse(name, colorList, 0);

            screen = parsedScreen.toHtmlScreen();
            long end = System.currentTimeMillis();

            float generateSeconds = (end - start) / 1000.0f;

            screen.setGenerateSeconds(generateSeconds);

            if (CACHE_SCREENS_ENABLED) {
                SCREEN_CACHE.put(url, screen);
            }
        }

        screen.incrementUsageCount();

        screen = applyMacros(screen, macros);

        return screen;
    }

    private void loadColorFile() throws FileNotFoundException {
        ColorListParser parser = new ColorListParser();

        File file = new File(COLOR_FILE_PATH);

        colorList = parser.parse(file);
    }

    private HtmlScreen applyMacros(HtmlScreen screen, List<Macro> macros) {
        String html = screen.getHtml();

        for (Macro m : macros) {

            /*Avoid cross-site scripting and malformed HTML by escaping, but sacrifice ability to use XML reserved characters in Macros ("'&<>)*/
            String v = org.apache.taglibs.standard.functions.Functions.escapeXml(m.value);

            html = html.replace(m.key, v);
        }

        return new HtmlScreen(screen.getCanonicalPath(), screen.getModifiedDate(), html, screen.getCss(), screen.getJs(),
                screen.getTitle());
    }
}
