package com.kylantraynor.livelyworld.database;

import java.util.logging.Level;

import com.kylantraynor.livelyworld.LivelyWorld;

public class Error {
    public static void execute(LivelyWorld plugin, Exception ex){
        plugin.getLogger().log(Level.SEVERE, "Couldn't execute MySQL statement: ", ex);
    }
    public static void close(LivelyWorld plugin, Exception ex){
        plugin.getLogger().log(Level.SEVERE, "Failed to close MySQL connection: ", ex);
    }
}