package com.socrata.geotoolbox;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.vividsolutions.jts.geom.Coordinate;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Prompts the user for a shapefile and displays the contents on the screen in a map frame.
 * <p>
 * This is the GeoTools Quickstart application used in documentationa and tutorials. *
 */
public class Reprojector {
    static String DEFAULT_PROJ = "EPSG:4326";
    /**
     * GeoTools Quickstart demo application. Prompts the user for a shapefile and displays its
     * contents on the screen in a map frame
     */
    public static void main(String[] args) throws Exception {
        OptionParser parser = new OptionParser();
        parser.accepts("verbose");
        parser.accepts("from").withRequiredArg().ofType(String.class);
        parser.accepts("to").withRequiredArg().ofType(String.class).required();
        parser.accepts("file").withRequiredArg().ofType(String.class);
        parser.accepts("x-index").withRequiredArg().ofType(Integer.class);
        parser.accepts("y-index").withRequiredArg().ofType(Integer.class);
        parser.accepts("skip").withRequiredArg().ofType(Integer.class);

        OptionSet options = parser.parse(args);

        // Deal with projections
        CoordinateReferenceSystem toCRS = CRS.decode(DEFAULT_PROJ);
        if(options.hasArgument("to")) {
            toCRS = CRS.decode((String)options.valueOf("to"));
        }

        CoordinateReferenceSystem fromCRS = CRS.decode((String) options.valueOf("from"));
        MathTransform transform = CRS.findMathTransform(fromCRS, toCRS);

        // Figure out where we'll be indexing into the CSV
        int xCol = 0;
        int yCol = 1;
        if(options.hasArgument("x-index"))
            xCol = (Integer)options.valueOf("x-index");
        if(options.hasArgument("y-index"))
            yCol = (Integer)options.valueOf("y-index");

        // Open our CSV
        CSVReader reader = null;
        if(options.hasArgument("file")) {
            reader = new CSVReader(new FileReader((String) options.valueOf("file")));
        } else {
            reader = new CSVReader(new InputStreamReader(System.in));
        }

        // For output!
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(System.out));

        // Skip!
        if(options.hasArgument("skip")) {
            for(int i = 0; i < (Integer)options.valueOf("skip"); i++) {
                writer.writeNext(reader.readNext());
            }
        }

        // Iterate over the remainder of our CSV
        String line[] = null;
        while((line = reader.readNext()) != null) {
            List<String> newLine = new ArrayList<String>();
            Collections.addAll(newLine, line);

            try {
                double xVal = Double.parseDouble(line[xCol]);
                double yVal = Double.parseDouble(line[yCol]);

                // Transform it
                Coordinate from = new Coordinate(xVal, yVal);
                Coordinate to = new Coordinate();
                JTS.transform(from, to, transform);

                // Add it to our line
                newLine.add(Double.toString(to.x));
                newLine.add(Double.toString(to.y));
            } catch(RuntimeException e) {
                // Nom Nom!
                if(options.has("verbose"))
                    System.err.println("Exception: " + e) ;
            }

            writer.writeNext((String[]) newLine.toArray(new String[newLine.size()]));
        }

        writer.flush();
        writer.close();
    }
}
