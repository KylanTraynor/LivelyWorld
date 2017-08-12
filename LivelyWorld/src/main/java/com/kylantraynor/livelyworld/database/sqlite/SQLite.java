package com.kylantraynor.livelyworld.database.sqlite;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.database.Database;

public class SQLite extends Database{
	String dbname = "database";
    public SQLite(LivelyWorld instance){
        super(instance);
    }

    public String SQLiteCreateClimateCellsTable = "CREATE TABLE IF NOT EXISTS "+prefix+ "climate_cells (" + // make sure to put your table name in here too.
            "`id` INTEGER NOT NULL," + // This creates the different colums you will save data too.
            "`temperature` FLOAT NOT NULL," +
            "`low_pressure` FLOAT NOT NULL," +
            "`high_temperature` FLOAT NOT NULL," +
            "`high_pressure` FLOAT NOT NULL," +
            "`humidity` FLOAT NOT NULL," +
            "PRIMARY KEY (`id`)" +
            ");";
    
    public String SQLiteCreateWaterTable = "CREATE TABLE IF NOT EXISTS " + prefix + "water (" +
    		"`id` VARCHAR(32) NOT NULL," +
    		"`data` INTEGER NOT NULL," +
    		"`x` INTEGER NOT NULL," +
    		"`y` INTEGER NOT NULL," +
    		"`z` INTEGER NOT NULL," +
    		"PRIMARY KEY (`id`)"+
    		");";

    // SQL creation stuff, You can leave the blow stuff untouched.
    public Connection getSQLConnection() {
        File dataFolder = new File(getPlugin().getDataFolder(), dbname+".db");
        if (!dataFolder.exists()){
            try {
                dataFolder.createNewFile();
            } catch (IOException e) {
                getPlugin().getLogger().log(Level.SEVERE, "File write error: "+dbname+".db");
            }
        }
        try {
            if(connection!=null&&!connection.isClosed()){
                return connection;
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            return connection;
        } catch (SQLException ex) {
            getPlugin().getLogger().log(Level.SEVERE,"SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            getPlugin().getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
        }
        return null;
    }

    public void load() {
        connection = getSQLConnection();
        try {
            Statement s = connection.createStatement();
            s.executeUpdate(SQLiteCreateClimateCellsTable);
            s.executeUpdate(SQLiteCreateWaterTable);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        initialize();
    }
}