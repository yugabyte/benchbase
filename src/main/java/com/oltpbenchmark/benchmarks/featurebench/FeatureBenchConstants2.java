package com.oltpbenchmark.benchmarks.featurebench;

public interface FeatureBenchConstants2 {



    public static final String DROP_QUERY_1="DROP TABLE IF EXISTS distributors";
    public static final String DROP_DB="DROP DATABASE test_db";

    // table and database names
    public static final String DB_NAME = "test_db";


    public static final String TABLE_NAME1 = "distributors";

    // Create database
    public static final String USE_DATABASE_QUERY = "CREATE DATABASE test_db;";

    public static final String drop_sequence="DROP SEQUENCE IF EXISTS serial_no";
    public static final String Sequence_generator= "CREATE SEQUENCE serial_no";

    public static final String CREATE_TABLE_1="CREATE TABLE distributors("
        + " did   DECIMAL(3)  DEFAULT NEXTVAL('serial_no'),"
        + " dname  VARCHAR(40) DEFAULT 'lusofilms' "
        +");";

    public static enum random_names {lusofilms , AssociatedComputing, Inc, XYZWidgets, GizmoTransglobal, RedlineGmbH,AcmeCorporation;};

    public static final String DROP_TABLE_1 = "DROP TABLE distributors";
    public static final String DROP_DATABASE = "DROP DATABASE TEST_DB";


}