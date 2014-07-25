package de.completionary.proxy.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

public abstract class Options {

    /*
     * Paste following code into your Class inheriting from this one to
     * initialize all !!!static!!! Fields in the class with the values in the
     * file $ClassName.properties
     */
    //    static {
    //        try {
    //            Options.initialize("/path/to/config/file", MethodHandles.lookup()
    //                    .lookupClass());
    //        } catch (IllegalArgumentException | IllegalAccessException e) {
    //            e.printStackTrace();
    //            System.exit(1);
    //        }
    //    }

    /**
     * This method is used to load all Strings from a resource properties file
     * into the associated class variables
     * 
     * @param c
     *            The class to be initialized
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    protected static void initialize(String configFile, Class c)
            throws IllegalArgumentException, IllegalAccessException {

        File propertyFile = new File(configFile);
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propertyFile)) {
            props.load(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * Fields defined in c
         */
        Field[] fields = c.getDeclaredFields();
        if (fields.length != props.size()) {
            System.err
                    .println("Unable to initialize "
                            + c.getName()
                            + ": The number of class fields and entries in the resource bundle file must be equal.");
            System.exit(1);
        }

        for (Field f : fields) {
            if (props.getProperty(f.getName()) == null) {
                System.err.print("Property '" + f.getName()
                        + "' not defined in config file");
            }
            if (f.getType().equals(String.class)) {
                f.set(null, props.getProperty(f.getName()));
            } else if (f.getType().equals(long.class)) {
                f.setLong(null, Long.valueOf(props.getProperty(f.getName())));
            } else if (f.getType().equals(int.class)) {
                f.setInt(null, Integer.valueOf(props.getProperty(f.getName())));
            } else if (f.getType().equals(boolean.class)) {
                f.setBoolean(null,
                        Boolean.valueOf(props.getProperty(f.getName())));
            } else if (f.getType().equals(String[].class)) {
                f.set(null, props.getProperty(f.getName()).split(";"));
            } else if (f.getType().equals(int[].class)) {
                String[] tmp = props.getProperty(f.getName()).split(";");
                int[] ints = new int[tmp.length];
                for (int i = 0; i < tmp.length; i++) {
                    ints[i] = Integer.parseInt(tmp[i]);
                }
                f.set(null, ints);
            } else if (f.getType().equals(long[].class)) {
                String[] tmp = props.getProperty(f.getName()).split(";");
                long[] longs = new long[tmp.length];
                for (int i = 0; i < tmp.length; i++) {
                    longs[i] = Long.parseLong(tmp[i]);
                }
                f.set(null, longs);
            }
        }
    }
}