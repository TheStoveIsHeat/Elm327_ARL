package com.obdelm327pro;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReceiveFromServer {

    public static void receive()
    {
        String jdbcUrl = "jdbc:mysql://108.17.113.150:7790/arl_1442?autoReconnect=true&useSSL=false";
        String username = "main";
        String password = "IW4nt2C0nnect";

        Log.i("FromSQL", "Grabbing data from database");

        int batchSize = 20;

        Connection connection = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(jdbcUrl, username, password);
            connection.setAutoCommit(false);
            Log.d("Database Connection", "SUCCESS");

            String sql = "SELECT * FROM vehicle";
            String [] data = new String [8];

            PreparedStatement statement = connection.prepareStatement(sql);

            ResultSet result = statement.executeQuery(sql);

            result.next();


            data[0] = result.getString(1);
            Log.d("FromSQL", "Vin: " + data[0]);
            data[1] = result.getString(2);
            Log.d("FromSQL", "AvgSpeed: " + data[1]);
            data[2] = result.getString(3);
            Log.d("FromSQL", "IdleTime: " + data[2]);
            data[3] = result.getString(4);
            Log.d("FromSQL", "FuelRate: " + data[3]);
            data[4] = result.getString(5);
            Log.d("FromSQL", "EngineOn: " + data[4]);
            data[5] = result.getString(6);
            Log.d("FromSQL", "MPG: " + data[5]);
            data[6] = result.getString(7);
            Log.d("FromSQL", "Date: " + data[6]);
            data[7] = result.getString(8);
            Log.d("FromSQL", "Time: " + data[7]);

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
