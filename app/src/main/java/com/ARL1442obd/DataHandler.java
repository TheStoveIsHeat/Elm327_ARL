package com.ARL1442obd;

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
import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;

import android.util.Log;

public class DataHandler {
    static String saveLocation = "/storage/emulated/0/Download";
    //static String fileName = "obd_data.csv";
    static String jdbcUrl = "jdbc:mysql://108.17.113.150:7790/arl_1442?autoReconnect=true&useSSL=false";
    static String username = "main";
    static String password = "IW4nt2C0nnect";

    public static boolean send(String filePath) {
        Log.i("ToSQL", "Grabbing CSV from " + filePath);

        int batchSize = 20;

        Connection connection = null;
        DriverManager.setLoginTimeout(10);
        boolean success = false;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(jdbcUrl, username, password);
            connection.setAutoCommit(false);

            String sql = "INSERT INTO trip(VIN, AvgSpeed, AvgRPM, EngineOnTime, BatteryVoltage, IntakeAirTemp, IntakeManPressure, ThrottlePosition, FuelRate, D, T) values(?,?,?,?,?,?,?,?,?,?,?)";
                    //"ON DUPLICATE KEY UPDATE " +
                    //"AvgSpeed = VALUES(AvgSpeed)," +
                    //"AvgRPM = VALUES(AvgRPM)," +
                    //"EngineOnTime = VALUES(EngineOnTime)," +
                    //"BatteryVoltage = VALUES(BatteryVoltage)" +
                    //"IntakeAirTemp = VALUES(IntakeAirTemp)" +
                    //"IntakeManPressure = VALUES(IntakeManPressure)" +
                    //"ThrottlePosition = VALUES(ThrottlePosition)" +
                    //"FuelRate = VALUES(FuelRate)" +
                    //"D = VALUES(D)" +
                    //"T = VALUES(T)";

            PreparedStatement statement = connection.prepareStatement(sql);

            BufferedReader lineReader = new BufferedReader(new FileReader(filePath));

            String lineText = null;
            int count = 0;

            lineReader.readLine();
            lineText = lineReader.readLine();
            //while (lineText != null) {
                String[] data = lineText.split(",");

                String VIN = data[0];
                Log.d("ToSQL", "Vin: " + VIN);
                String AvgSpeed = data[1];
                Log.d("ToSQL", "AvgSpeed: " + AvgSpeed);
                String AvgRPM = data[2];
                Log.d("ToSQL", "AvgRPM: " + AvgRPM);
                String EngineOnTime = data[3];
                Log.d("ToSQL", "EngineOnTime: " + EngineOnTime);
                String BatteryVoltage = data[4];
                Log.d("ToSQL", "BatteryVoltage: " + BatteryVoltage);
                String IntakeAirTemp = data[5];
                Log.d("ToSQL", "IntakeAirPressure: " + IntakeAirTemp);
                String IntakeManPressure = data[6];
                Log.d("ToSQL", "IntakeManPressure: " + IntakeManPressure);
                String ThrottlePosition = data[7];
                Log.d("ToSQL", "ThrottlePosition: " + ThrottlePosition);
                String FuelRate = data[8];
                Log.d("ToSQL", "FuelRate: " + FuelRate);
                String D = data[9];
                Log.d("ToSQL", "Date: " + D);
                String T = data[10];
                Log.d("ToSQL", "Time: " + T);

                if (AvgSpeed.contains(" ")){
                    int index = AvgSpeed.indexOf(" ");
                    AvgSpeed = AvgSpeed.substring(index+1);
                }
                if (AvgRPM.contains(" ")){
                    int index = AvgRPM.indexOf(" ");
                    AvgRPM = AvgRPM.substring(index+1);
                }
                if (EngineOnTime.contains(" ")){
                    int index = EngineOnTime.indexOf(" ");
                    EngineOnTime = EngineOnTime.substring(index+1);
                }
                if (BatteryVoltage.contains(" ")){
                    int index = BatteryVoltage.indexOf(" ");
                    BatteryVoltage = BatteryVoltage.substring(index+1);
                }
                if (IntakeAirTemp.contains(" ")){
                    int index = IntakeAirTemp.indexOf(" ");
                    IntakeAirTemp = IntakeAirTemp.substring(index+1);
                }
                if (IntakeManPressure.contains(" ")){
                    int index = IntakeManPressure.indexOf(" ");
                    IntakeManPressure = IntakeManPressure.substring(index+1);
                }
                if (ThrottlePosition.contains(" ")){
                    int index = ThrottlePosition.indexOf(" ");
                    ThrottlePosition = ThrottlePosition.substring(index+1);
                }
                if (FuelRate.contains(" ")){
                    int index = FuelRate.indexOf(" ");
                    FuelRate = FuelRate.substring(index+1);
                }
                if (D.contains(" ")){
                    int index = D.indexOf(" ");
                    D = D.substring(index+1);
                }
                if (T.contains(" ")){
                    int index = T.indexOf(" ");
                    T = T.substring(index+1);
                }

                statement.setString(1, VIN);
            Log.d("ToSQL", "YES");
                statement.setString(2, AvgSpeed);
            Log.d("ToSQL", "YES");
                statement.setString(3, AvgRPM);
            Log.d("ToSQL", "YES");
                statement.setString(4, EngineOnTime);
            Log.d("ToSQL", "YES");
                statement.setString(5, BatteryVoltage);
            Log.d("ToSQL", "YES");
                statement.setString(6, IntakeAirTemp);
            Log.d("ToSQL", "YES");
                statement.setString(7, IntakeManPressure);
            Log.d("ToSQL", "YES");
                statement.setString(8, ThrottlePosition);
            Log.d("ToSQL", "YES");
                statement.setString(9, FuelRate);
            Log.d("ToSQL", "YES");
                statement.setString(10, D);
            Log.d("ToSQL", "YES");
                statement.setString(11, T);
            Log.d("ToSQL", "YES");
                statement.addBatch();
            Log.d("ToSQL", "YES");
                //count += 1;
                //lineText = lineReader.readLine();
                if (count % batchSize == 0) {
                    statement.executeBatch();
                    Log.d("ToSQL", "Batch Executed");
                }
            //}
            lineReader.close();
            Log.d("ToSQL", "LineReader Closed");
            statement.executeBatch();
            Log.d("ToSQL", "Batch Executed 2");
            connection.commit();
            Log.d("ToSQL", "Committed ");
            connection.close();
            Log.i("ToSQL", "Data has been inserted successfully.");
            success = true;

        } catch (Exception exception) {
            exception.printStackTrace();
            Log.w("ToSQL", "Error!: " + exception.getMessage());
        }

        if (success) {
            File fDelete = new File(filePath);
            if (fDelete.exists()) {
                if (fDelete.delete()) {
                    Log.i("ToSQL","File Deleted :" + filePath);
                } else {
                    Log.i("ToSQL","File not Deleted :" + filePath);
                }
            }
        }
        return success;
    }

    public static List<String[]> request()
    {
        Log.i("FromSQL", "Grabbing data from database");

        DriverManager.setLoginTimeout(10);
        Connection connection = null;
        List<String[]> dataList = new ArrayList<>();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(jdbcUrl, username, password);
            connection.setAutoCommit(false);
            Log.d("Database Connection", "SUCCESS");

            String sql = "SELECT * FROM trip";

            PreparedStatement statement = connection.prepareStatement(sql);

            ResultSet result = statement.executeQuery(sql);

            while(result.next()){
                String[] data = new String[11];
                data[0] = result.getString(2);
                Log.d("FromSQL", "Vin: " + data[0]);
                data[1] = result.getString(3);
                Log.d("FromSQL", "AvgSpeed: " + data[1]);
                data[2] = result.getString(4);
                Log.d("FromSQL", "AvgRPM: " + data[2]);
                data[3] = result.getString(5);
                Log.d("FromSQL", "EngineOnTime: " + data[3]);
                data[4] = result.getString(6);
                Log.d("FromSQL", "BatteryVoltage: " + data[4]);
                data[5] = result.getString(7);
                Log.d("FromSQL", "IntakeAirTemp: " + data[5]);
                data[6] = result.getString(8);
                Log.d("FromSQL", "IntakeManPressure: " + data[6]);
                data[7] = result.getString(9);
                Log.d("FromSQL", "ThrottlePosition: " + data[7]);
                data[8] = result.getString(10);
                Log.d("FromSQL", "FuelRate: " + data[8]);
                data[9] = result.getString(11);
                Log.d("FromSQL", "D: " + data[9]);
                data[10] = result.getString(12);
                Log.d("FromSQL", "T: " + data[10]);

                dataList.add(data);
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return dataList;
    }

    // Function to save data to CSV file (new code)
    public static boolean saveToCSV(String fileName, String data) {
        boolean success = false;
        try {
            /// Create a new File object with the desired file name and location
            File directory = new File(saveLocation);
            File file = new File(directory, fileName);
            // Create a FileOutputStream to write to the file and OutputStreamWriter to write chars
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            // Write the headers to the file
            osw.write("Vin, AvgSpeed, AvgRPM, EngineOnTime, BatteryVoltage, IntakeAirTemp, IntakeManPressure, ThrottlePos, FuelRate, Date, Time");
            //New line
            osw.write(10);
            //Data goes here, inputted data should be a full string with all the values
            osw.write(data);
            // Close the streams
            osw.close();
            fos.close();
            // Optional: Display a message or log success
            success = true;
            // Toast.makeText(this, "Data saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            // Handle IO exception
            e.printStackTrace();
            // Optional: Display an error message or log the error
            Log.w("CSVConsume", "ERROR SAVING DATA: " + e.getMessage());
        }
        return success;
    }
}