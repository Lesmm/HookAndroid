package com.example.administrator.hookandroid.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class SQLServerUtil {

    private static String driverName = "net.sourceforge.jtds.jdbc.Driver";
    private static String dbURL = "jdbc:jtds:sqlserver://192.168.3.68:1433;DatabaseName=yun;charset=utf8";
    private static String userName = "sa";
    private static String userPwd = "123";

    private static Connection connection = null;

    private static Connection connection() {
        if (connection == null) {
            Connection con = null;
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                con = DriverManager.getConnection(dbURL, userName, userPwd);
                connection = con;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    public static JSONArray query() {
        JSONArray jsonArray = null;
        try {
            Connection conn = connection();
            String tableName = "[yun].[dbo].[tb_phonetype]";
            String sql = "select top 1000 [p_id] ,[phone_type] ,[phone_info] ,[use_radio] from " + tableName;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            jsonArray = resultSetToJsonArry(rs);

            rs.close();
            stmt.close();
//            conn.close();     // do not close the shared connection
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    public static ArrayList queryPhoneType(String phone_typ) {
        ArrayList array = new ArrayList();
        try {
            Connection conn = connection();
            String tableName = "[yun].[dbo].[tb_phonetype]";
            String sql = "select [p_id],[phone_type],[phone_info],[use_radio] from " + tableName + " where [phone_type] = \'" + phone_typ + "\';";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                ArrayList elements = new ArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    String value = rs.getString(columnName);
                    elements.add(value);
                }
                array.add(elements);
            }

            rs.close();
            stmt.close();
//            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return array;
    }

    public static void insertUpdatePhoneInfo(String phone_typ, String phone_info) {
        ArrayList arrayList = queryPhoneType(phone_typ);
        if (arrayList.size() >= 1) {
            updatePhoneInfo(phone_typ, phone_info);
        } else {
            insertPhoneInfo(phone_typ, phone_info);
        }
    }

    public static void insertPhoneInfo(String phone_typ, String phone_info) {
        try {
            Connection conn = connection();
            String tableName = "[yun].[dbo].[tb_phonetype]";
            String sql = "insert into " + tableName + " ([phone_type], [phone_info], [use_radio]) values(?, ?, ?);";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, phone_typ);
            pstmt.setString(2, phone_info);
            pstmt.setInt(3, 0);

            pstmt.executeUpdate();
//            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updatePhoneInfo(String phone_typ, String phone_info) {
        try {
            Connection conn = connection();
            String tableName = "[yun].[dbo].[tb_phonetype]";
            String sql = "update " + tableName + " set [phone_info] = \'" + phone_info + "\' where [phone_type] = \'" + phone_typ + "\';";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
//            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject resultSetToJsonObject(ResultSet rs) {
        JSONObject jsonObj = new JSONObject();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            if (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    String value = rs.getString(columnName);
                    jsonObj.put(columnName, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObj;
    }

    public static JSONArray resultSetToJsonArry(ResultSet rs) {
        JSONArray array = new JSONArray();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                JSONObject jsonObj = new JSONObject();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    String value = rs.getString(columnName);
                    jsonObj.put(columnName, value);
                }
                array.put(jsonObj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return array;
    }


    public static void main(String[] args) {
        queryPhoneType("MI 3W");
    }

}
