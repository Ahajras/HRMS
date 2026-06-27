package com.hrms.migration.service;

import com.linuxense.javadbf.DBFReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a legacy FoxPro/DBF table into a list of row maps keyed by UPPERCASE
 * field name. A sibling memo file (.fpt/.dbt) is attached when present so memo
 * columns are not lost. Used by {@link LegacyImportService} for the web import
 * of the old HR snapshot.
 */
final class DbfReader {

    private DbfReader() {
    }

    static List<Map<String, Object>> read(File dbfFile) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (InputStream in = new BufferedInputStream(new FileInputStream(dbfFile))) {
            DBFReader reader = new DBFReader(in);
            File memo = findMemo(dbfFile);
            if (memo != null) {
                reader.setMemoFile(memo);
            }
            int cols = reader.getFieldCount();
            String[] names = new String[cols];
            for (int i = 0; i < cols; i++) {
                names[i] = reader.getField(i).getName().toUpperCase();
            }
            Object[] record;
            while ((record = reader.nextRecord()) != null) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 0; i < cols; i++) {
                    row.put(names[i], record[i]);
                }
                rows.add(row);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read DBF: " + dbfFile.getName() + " - " + e.getMessage(), e);
        }
        return rows;
    }

    private static File findMemo(File dbfFile) {
        String base = dbfFile.getName();
        int dot = base.lastIndexOf('.');
        String stem = dot >= 0 ? base.substring(0, dot) : base;
        for (String ext : new String[] {".fpt", ".FPT", ".dbt", ".DBT"}) {
            File memo = new File(dbfFile.getParentFile(), stem + ext);
            if (memo.exists()) {
                return memo;
            }
        }
        return null;
    }
}
