package com.flat.util;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Allows storing strings in a 3D object such as a database (rows, cols, tables)
 *
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class StringDb {
    private static final String TAG = StringDb.class.getSimpleName();

    /*
     * This must be different than the row and col delimiters of the DelimitedTable.
     */
    private final StringDelimiter tableDelim = new StringDelimiter("{}");
    private final List<DelimitedTable> db = Collections.synchronizedList(new ArrayList<DelimitedTable>(1));

    private String dbName;

    public StringDb() {

    }

    public String getName() {
        synchronized (dbName) {
            return dbName;
        }
    }
    public void setName(String dbName) {
        synchronized (dbName) {
            this.dbName = dbName;
        }
    }

    public boolean addTable(DelimitedTable dt) {
        if (getTable(dt.getName()) != null) {
            db.add(dt);
            return true;
        }
        return false;
    }

    public DelimitedTable removeTable(int tableIndex) {
        return db.remove(tableIndex);
    }

    public boolean removeTable(DelimitedTable dt) {
        return db.remove(dt);
    }

    public DelimitedTable getTable(int tableIndex) {
        return db.get(tableIndex);
    }

    public synchronized DelimitedTable getTable(String tableName) {
        for (DelimitedTable dt : db) {
            if (dt.getName().equals(tableName)) {
                return dt;
            }
        }
        return null;
    }

    public int getTableCount() {
        return db.size();
    }

    public synchronized String encode() {
        String[] tables = new String[db.size()];
        for (int i=0; i<db.size(); ++i) {
            tables[i] = db.get(i).encode();
        }
        return tableDelim.encode(tables);
    }





    public static class StringEncoder {
        private final String from, to;
        public StringEncoder(String from, String to) {
            this.from = from;
            this.to = to;
        }
        public String encode(String s) {
            return s.replace(from, to);
        }
        public String decode(String s) {
            return s.replace(to, from);
        }
        public String[] encodeAll(String[] strings) {
            String[] s = new String[strings.length];
            for (int i=0; i<strings.length; ++i) {
                s[i] = encode(strings[i]);
            }
            return s;
        }
        public String[] decodeAll(String[] strings) {
            String[] s = new String[strings.length];
            for (int i=0; i<strings.length; ++i) {
                s[i] = decode(strings[i]);
            }
            return s;
        }
    }





    public static class StringDelimiter {
        private final String delim;
        private final StringEncoder encoder;

        public StringDelimiter(String delim) {
            if (delim.length() != 2) {
                throw new IllegalArgumentException("Delimiters must be 2 characters");
            }

            this.delim = delim;

            String from, to;
            from = delim.substring(0, 1);  // first char
            if (!delim.contains("/")) to = from + "/";
            else if (!delim.contains("_")) to = from + "_";
            else if (!delim.contains("-")) to = from + "-";
            else {
                throw new AssertionError("Two char string cannot have all 3 chars.");
            }
            encoder = new StringEncoder(from, to);
        }

        public String encode(String... args) {
            String[] encoded = encoder.encodeAll(args);
            return TextUtils.join(delim, encoded);
        }

        public String[] decode(String s) {
            return encoder.decodeAll(TextUtils.split(s, Pattern.quote(delim)));
        }
    }








    public static class DelimitedTable extends Table {
        private final StringDelimiter colDelim = new StringDelimiter("<>");
        private final StringDelimiter rowDelim = new StringDelimiter("[]");

        public DelimitedTable(int numFields) {
            super(numFields);
        }
        public DelimitedTable(String... fields) {
            super(fields);
        }
        public DelimitedTable(String from, boolean headerRow) {
            String[] rows = rowDelim.decode(from);
            String[] cols;
            boolean first = true;
            for (String row : rows) {
                cols = colDelim.decode(row);
                if (first) {
                    if (headerRow) {
                        forceFields(cols);
                    } else {
                        forceFields(new String[cols.length]);
                    }
                    first = false;
                } else {
                    addRow(cols);
                }
            }
        }

        public synchronized String getEncodedFields() {
            return colDelim.encode(getFields());
        }

        public synchronized String getEncodedRow(int rowIndex) {
            return colDelim.encode(getRow(rowIndex));
        }

        public synchronized String encode() {
            String[] rows = new String[getRowCount()];
            for (int i=0; i<getRowCount(); ++i) {
                rows[i] = getEncodedRow(i);
            }
            return rowDelim.encode(rows);
        }
    }






    public static class Table {
        private String tableName;
        protected String[] fields;
        private final List<String[]> data = Collections.synchronizedList(new ArrayList<String[]>());

        private boolean includeFieldNames = false; // first row would be field names in output.
        private boolean cloneRows = false;


        protected  Table() {}
        public Table(int numFields) {
            fields = new String[numFields];
        }
        public Table(String... fields) {
            this.fields = fields;
        }


        public String getName() {
            synchronized (tableName) {
                return tableName;
            }
        }
        public void setName(String name) {
            synchronized (name) {
                this.tableName = name;
            }
        }

        public String[] getFields() {
            synchronized (fields) {
                return fields.clone();
            }
        }
        public void setField(int index, String field) {
            synchronized (fields) {
                fields[index] = field;
            }
        }

        protected void forceFields(String... fields) {
            this.fields = fields;
        }
        public void setFields(String... fields) {
            if (fields.length != this.fields.length) {
                throw new IllegalArgumentException("Cannot change the number of fields");
            }

            synchronized (this.fields) {
                for (int i=0; i<this.fields.length; ++i) {
                    this.fields[i] = fields[i];
                }
            }
        }

        public void addRow(String... row) {
            if (row.length != fields.length) {
                throw new IllegalArgumentException("Row length does not match the number of fields.");
            }

            if (cloneRows) {
                data.add(row.clone());
            } else {
                data.add(row);
            }
        }

        public void setRow(int rowIndex, String... row) {
            if (rowIndex < 0 || rowIndex > data.size() - 1) {
                throw new IllegalArgumentException("Invalid rowIndex " + rowIndex + " for table of " + data.size() + " rows.");
            }
            if (row.length != fields.length) {
                throw new IllegalArgumentException("Row length does not match the number of fields.");
            }

            if (cloneRows) {
                data.set(rowIndex, row.clone());
            } else {
                data.set(rowIndex, row);
            }
        }

        public String[] removeRow(int rowIndex) {
            return data.remove(rowIndex);
        }

        public String[] getRow(int rowIndex) {
            if (cloneRows) {
                return data.get(rowIndex).clone();
            } else {
                return data.get(rowIndex);
            }
        }

        public int getRowCount() {
            return data.size();
        }

        public int getColCount() {
            return fields.length;
        }

        public String[][] toArray() {
            return data.toArray(new String[data.size()][fields.length]);
        }
    }
}
