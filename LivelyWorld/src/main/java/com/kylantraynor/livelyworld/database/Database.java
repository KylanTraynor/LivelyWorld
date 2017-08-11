package com.kylantraynor.livelyworld.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Location;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.climate.ClimateCellData;
import com.kylantraynor.livelyworld.water.WaterChunk;
import com.kylantraynor.livelyworld.water.WaterData;

public abstract class Database {
    private LivelyWorld plugin;
    protected Connection connection;
    // The name of the table we created back in SQLite class.
    public String prefix = "lw_";
    public int tokens = 0;
    public Database(LivelyWorld instance){
        setPlugin(instance);
    }

    public abstract Connection getSQLConnection();

    public abstract void load();

    public void initialize(){
        connection = getSQLConnection();
        try{
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + prefix + "climate_cells" + " WHERE id = ?");
            ResultSet rs = ps.executeQuery();
            close(ps,rs);
    
        } catch (SQLException ex) {
            getPlugin().getLogger().log(Level.SEVERE, "Unable to retreive connection", ex);
        }
    }

    public ClimateCellData getClimateCellData(int id) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        ClimateCellData result = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + prefix + "climate_cells" + " WHERE id = "+id+";");
    
            rs = ps.executeQuery();
            while(rs.next()){
            	if(rs.getInt("id") == id){
            		result = new ClimateCellData(
            				rs.getDouble("temperature"),
            				rs.getDouble("high_temperature"),
            				rs.getDouble("low_pressure"),
            				rs.getDouble("high_pressure"),
            				rs.getDouble("humidity"));
            	}
            }
        } catch (SQLException ex) {
            getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return result;
    }
    
    public List<ClimateCellData> getAllClimateCellData() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<ClimateCellData> result = new ArrayList<ClimateCellData>();
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + prefix + "climate_cells ORDER BY id;");
    
            rs = ps.executeQuery();
            while(rs.next()){
            	result.add(new ClimateCellData(
            				rs.getDouble("temperature"),
            				rs.getDouble("high_temperature"),
            				rs.getDouble("low_pressure"),
            				rs.getDouble("high_pressure"),
            				rs.getDouble("humidity")));
            }
        } catch (SQLException ex) {
            getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return result;
    }

// Now we need methods to save things to the database
    public void setClimateCellData(int id, ClimateCellData data) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("REPLACE INTO " + prefix + "climate_cells (id,temperature,high_temperature,low_pressure,high_pressure,humidity) VALUES(?,?,?,?,?,?);"); // IMPORTANT. In SQLite class, We made 3 colums. player, Kills, Total.
            ps.setInt(1, id);                                                                 
            ps.setDouble(2, data.getTemperature());
            ps.setDouble(3, data.getHighTemperature());
            ps.setDouble(4, data.getPressure());
            ps.setDouble(5, data.getHighPressure());
            ps.setDouble(6, data.getHumidity());
            ps.executeUpdate();
            return;
        } catch (SQLException ex) {
            getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return;        
    }
    
    public void setWaterData(Location loc, WaterData data) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement(data.getSQLReplaceString(prefix + "water"));
            ps.executeUpdate();
            return;
        } catch (SQLException ex) {
            getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return;        
    }
    
    public WaterData getWaterDataAt(Location loc){
    	Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        WaterData result = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + prefix + "water WHERE location='"+Utils.getBlockLocationStringNoWorld(loc)+"';");
    
            rs = ps.executeQuery();
            while(rs.next()){
            	result = new WaterData(loc, rs.getInt("moisture"), rs.getDouble("currentDirection"), rs.getDouble("currentStrength"));
            }
        } catch (SQLException ex) {
            getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return result;
    }
    
    public void clearClimateCellsData(){
    	Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("DELETE FROM " + prefix + "climate_cells;");
            ps.executeUpdate();
            return;
        } catch (SQLException ex) {
            getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return;      
    }


    public void close(PreparedStatement ps,ResultSet rs){
        try {
            if (ps != null)
                ps.close();
            if (rs != null)
                rs.close();
        } catch (SQLException ex) {
            Error.close(getPlugin(), ex);
        }
    }

	public LivelyWorld getPlugin() {
		return plugin;
	}

	public void setPlugin(LivelyWorld plugin) {
		this.plugin = plugin;
	}

	public void loadWaterChunk(WaterChunk waterChunk) {
		Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        WaterData result = null;
        try {
            conn = getSQLConnection();
            //ps = conn.prepareStatement("SELECT * FROM " + prefix + "water WHERE x='"+waterChunk.getX()+"';");
    
            rs = ps.executeQuery();
            while(rs.next()){
            	//result = new WaterData(loc, rs.getInt("moisture"), rs.getDouble("currentDirection"), rs.getDouble("currentStrength"));
            }
        } catch (SQLException ex) {
            getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        //return result;
	}
}