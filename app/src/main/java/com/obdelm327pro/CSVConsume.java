package com.obdelm327pro;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import static java.lang.Integer.parseInt;

import android.util.Log;
import android.widget.Toast;

public class CSVConsume {


    static String saveLocation = "/storage/emulated/0/Download";
    static String fileName = "pid_data.csv";

    public static void send(String filePath) {
        String jdbcUrl = "jdbc:mysql://108.17.113.150:7790/arl_1442";
        String username = "main";
        String password = "IW4nt2C0nnect";

        //String filePath="C:\\Users\\laser\\Desktop\\data.csv";

        Log.i("ToSQL", "Grabbing CSV from " + filePath);

        int batchSize = 20;

        Connection connection = null;


        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(jdbcUrl, username, password);
            connection.setAutoCommit(false);

            String sql = "insert into vehicle(VIN,IdleTime,FuelRate,EngineOn) values(?,?,?,?)";

            PreparedStatement statement = connection.prepareStatement(sql);

            BufferedReader lineReader = new BufferedReader(new FileReader(filePath));

            String lineText = null;
            int count = 0;

            lineReader.readLine();
            while ((lineText = lineReader.readLine()) != null) {
                String[] data = lineText.split(",");

                String VIN = data[0];
                Log.d("ToSQL", "Vin: " + VIN);
                String IdleTime = data[1];
                Log.d("ToSQL", "IdleTime: " + IdleTime);
                String FuelRate = data[2];
                Log.d("ToSQL", "FuelRate: " + FuelRate);
                String EngineOn = data[3];
                Log.d("ToSQL", "EngineOn: " + EngineOn);

                statement.setString(1, VIN);
                statement.setInt(2, parseInt(IdleTime));
                statement.setInt(3, parseInt(FuelRate));
                statement.setInt(4, parseInt(EngineOn));
                statement.addBatch();
                if (count % batchSize == 0) {
                    statement.executeBatch();
                }
            }
            lineReader.close();
            statement.executeBatch();
            connection.commit();
            connection.close();
            Log.i("ToSQL", "Data has been inserted successfully.");

        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    // Function to save data to CSV file (new code)
    public static void saveToCSV(String fileName, String data) {
        try {
            // Get the app's internal storage directory
            //File directory = getFilesDir();
            File directory = new File(saveLocation);
            //mConversationArrayAdapter.add(String.valueOf(directory));
            // Create a new File object with the desired file name
            File file = new File(directory, fileName);
            // Create a FileOutputStream to write to the file
            FileOutputStream fos = new FileOutputStream(file);
            // Create an OutputStreamWriter to write characters to the FileOutputStream
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            // Write the header to the file
            osw.write("Vin, AvgSpeed, FuelRate, IdleTime, EngineOnTime, MPG, Date, Time");
            //New line
            osw.write(10);
            //Data goes here, inputted data should be a full string with all the values
            osw.write(data);
            // Close the streams
            osw.close();
            fos.close();
            // Optional: Display a message or log success
            // Toast.makeText(this, "Data saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            // Handle IO exception
            e.printStackTrace();
            // Optional: Display an error message or log the error
            //Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.w("CSVConsume", "ERROR SAVING DATA: " + e.getMessage());
        }
    }
}