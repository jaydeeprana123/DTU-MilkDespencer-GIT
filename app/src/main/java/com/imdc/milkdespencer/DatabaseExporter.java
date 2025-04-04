package com.imdc.milkdespencer;

import android.content.Context;
import android.os.Environment;
import java.io.*;

public class DatabaseExporter {
    public static void copyDatabase(Context context) {
        try {
            File dbFile = new File(context.getDatabasePath("IDMC-MilkVending").getPath());
            File outFile = new File(Environment.getExternalStorageDirectory(), "IDMC-MilkVending.db");

            InputStream in = new FileInputStream(dbFile);
            OutputStream out = new FileOutputStream(outFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            out.flush();
            out.close();
            in.close();
            System.out.println("Database copied successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
