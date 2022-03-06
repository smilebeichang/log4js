package cn.edu.sysu.utils;


import cn.edu.sysu.adi.TYPE;

import java.sql.*;
import java.util.ArrayList;


/**
 * @Author : song bei chang
 * @create 2021/5/2 7:24
 */
public class JDBCUtils4 {



    /**
     * 查询，并返回list
     *
     * adi20210528  adi_5_500  adi_8_1000
     */
    public  ArrayList<String> selectV2() throws SQLException {

        ArrayList<String> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("select * from sysu.adi_8_1000 order by id  ;");
            rs = ps.executeQuery();
            while(rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String penalty = rs.getString("penalty");
                list.add(id+":"+type+":"+penalty);
            }

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        return list;
    }


    /**
     * 查询，并返回list
     *
     * adi20210528  adi_5_500  adi_8_1000
     */
    public  ArrayList<String> select() throws SQLException {

            ArrayList<String> list = new ArrayList<>();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs ;
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            try {
                conn =
                        DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
                ps = conn.prepareStatement("select * from sysu.adi_5_500 order by id  ;");
                rs = ps.executeQuery();
                while(rs.next()) {
                    int id = rs.getInt("id");
                    String type = rs.getString("type");
                    String attributes = rs.getString("pattern");
                    list.add(id+":"+type+":"+attributes);
                }

            } catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
            }finally {
                if(ps!= null) {
                    ps.close();
                }
                if(conn!= null) {
                    conn.close();
                }
            }
            return list;
    }

    /**
     * 新增到数据库
     * adi20210528
     *
     */
    public  void insert(int id, TYPE type1, String pattern, Double base, String penalty, Double adi1_r, Double adi2_r, Double adi3_r, Double adi4_r, Double adi5_r) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        String type = type1+"";

        pattern.split(",");
        String p1  = pattern.split(",")[0].substring(1,2);
        String p2  = pattern.split(",")[1];
        String p3  = pattern.split(",")[2];
        String p4  = pattern.split(",")[3];
        String p5  = pattern.split(",")[4].substring(0,1);

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            String sql = "INSERT INTO sysu.adi20220119 \n" +
                    "(id,type, pattern,p1,p2,p3,p4,p5, base, penalty, adi1_r, adi2_r, adi3_r, adi4_r, adi5_r) \n" +
                    "VALUES("+id+",\""+type+"\","+"\""+pattern+"\","+p1+","+p2+","+p3+","+p4+","+p5+","+base+",\""+penalty+"\","+adi1_r+","+adi2_r+","+adi3_r+","+adi4_r+","+adi5_r+");";
            System.out.println(sql);
            ps = conn.prepareStatement(sql);
            ps.execute();

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
    }



    /**
     * 新增到数据库
     * adi20210528
     *
     */
    public  void insertV2(int id, TYPE type1, String pattern, Double base, String penalty, Double adi1_r, Double adi2_r, Double adi3_r, Double adi4_r, Double adi5_r, Double adi6_r, Double adi7_r, Double adi8_r) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        String type = type1+"";

        pattern.split(",");
        String p1  = pattern.split(",")[0].substring(1,2);
        String p2  = pattern.split(",")[1];
        String p3  = pattern.split(",")[2];
        String p4  = pattern.split(",")[3];
        String p5  = pattern.split(",")[4];
        String p6  = pattern.split(",")[5];
        String p7  = pattern.split(",")[6];
        String p8  = pattern.split(",")[7].substring(0,1);

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            String sql = "INSERT INTO sysu.adi_8_1000 \n" +
                    "(id,type,p1,p2,p3,p4,p5,p6,p7,p8, base, penalty, adi1_r, adi2_r, adi3_r, adi4_r, adi5_r, adi6_r, adi7_r, adi8_r) \n" +
                    "VALUES("+id+",\""+type+"\","+p1+","+p2+","+p3+","+p4+","+p5+","+p6+","+p7+","+p8+","+base+",\""+penalty+"\","+adi1_r+","+adi2_r+","+adi3_r+","+adi4_r+","+adi5_r+","+adi6_r+","+adi7_r+","+adi8_r+");";
            //"VALUES("+id+",\""+pattern+"\","+base+",\""+penalty+"\","+adi1_r+","+adi2_r+","+adi3_r+","+adi4_r+","+adi5_r+");";
            System.out.println(sql);
            ps = conn.prepareStatement(sql);
            ps.execute();

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
    }



    /**
     * 更新到数据库
     */
    public  void updateDina(int id,double ps1,double pg,double adi1_d,double adi2_d,double adi3_d,double adi4_d,double  adi5_d) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null ;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            String sql = "UPDATE sysu.adi20210523 \n" +
                    "SET adi1_d="+adi1_d +", adi2_d=" +adi2_d+", " +
                    "adi3_d="+ adi3_d+", adi4_d="+adi4_d+", adi5_d="+adi5_d+", " +
                    "ps="+ps1+", pg="+pg+" where id="+id+" ;";
            ps = conn.prepareStatement(sql);
            ps.execute();

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
    }


    /**
     * 查询，并返回id
     * adi20210528  adi_5_500
     */
    public  int selectItem(String sql) throws SQLException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        int id = 0;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("SELECT t1.id \n" +
                    "FROM sysu.adi_8_1000 AS t1 \n" +
                    "JOIN (\n" +
                     sql  +
                    ") AS t2 \n" +
                    "WHERE t1.id = t2.id limit 1;");
            rs = ps.executeQuery();
            rs.next();
            id = rs.getInt("id");

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        return id;

    }



    /**
     * 查询，并返回list
     * adi_8_1000
     */
    public  ArrayList<String> selectBachItemV2(String ids) throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        Connection conn  = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("select * from sysu.adi_8_1000 where id in (" +
                    ids +  ");");
            rs = ps.executeQuery();

            while(rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                //String pattern = rs.getString("pattern");
                double adi1_r = rs.getDouble("adi1_r");
                double adi2_r = rs.getDouble("adi2_r");
                double adi3_r = rs.getDouble("adi3_r");
                double adi4_r = rs.getDouble("adi4_r");
                double adi5_r = rs.getDouble("adi5_r");
                double adi6_r = rs.getDouble("adi6_r");
                double adi7_r = rs.getDouble("adi7_r");
                double adi8_r = rs.getDouble("adi8_r");

                //list.add(id+":"+type+":"+pattern+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r+":"+adi6_r+":"+adi7_r+":"+adi8_r);
                list.add(id+":"+type+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r+":"+adi6_r+":"+adi7_r+":"+adi8_r);
            }

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        return list;
    }



    /**
     * 查询，并返回list
     * adi20210528  adi_5_500
     */
    public  ArrayList<String> selectBachItem(String ids) throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        Connection conn  = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("select * from sysu.adi_5_500 where id in (" +
                    ids +  ");");
            rs = ps.executeQuery();

            while(rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String pattern = rs.getString("pattern");
                double adi1_r = rs.getDouble("adi1_r");
                double adi2_r = rs.getDouble("adi2_r");
                double adi3_r = rs.getDouble("adi3_r");
                double adi4_r = rs.getDouble("adi4_r");
                double adi5_r = rs.getDouble("adi5_r");

                list.add(id+":"+type+":"+pattern+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r);
            }

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        return list;
    }


    /**
     * 查询，并返回list
     */
    public  ArrayList<String> selectBachItemBak(String ids) throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        Connection conn  = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("select * from sysu.adi20210528 order by RAND() limit 20  ;");
            rs = ps.executeQuery();

            while(rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String pattern = rs.getString("pattern");
                double adi1_r = rs.getDouble("adi1_r");
                double adi2_r = rs.getDouble("adi2_r");
                double adi3_r = rs.getDouble("adi3_r");
                double adi4_r = rs.getDouble("adi4_r");
                double adi5_r = rs.getDouble("adi5_r");

                list.add(id+":"+type+":"+pattern+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r);
            }

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        return list;
    }


    /**
     * 查询，并返回list
     *
     * adi_8_1000
     *
     */
    public  ArrayList<String> selectAllItemsV2() throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        Connection conn  = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("select * from sysu.adi_8_1000 ;");
            rs = ps.executeQuery();

            while(rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                //String pattern = rs.getString("pattern");
                double adi1_r = rs.getDouble("adi1_r");
                double adi2_r = rs.getDouble("adi2_r");
                double adi3_r = rs.getDouble("adi3_r");
                double adi4_r = rs.getDouble("adi4_r");
                double adi5_r = rs.getDouble("adi5_r");
                double adi6_r = rs.getDouble("adi6_r");
                double adi7_r = rs.getDouble("adi7_r");
                double adi8_r = rs.getDouble("adi8_r");

                list.add(id+":"+type+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r+":"+adi6_r+":"+adi7_r+":"+adi8_r);
            }

        } catch (SQLException ex) {
            System.out.println("VendorError: " + ex.getErrorCode());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        return list;
    }


    /**
     * 查询，并返回list
     *
     * adi20210528  adi_5_500
     *
     */
    public  ArrayList<String> selectAllItems() throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        Connection conn  = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("select * from sysu.adi_5_500 ;");
            rs = ps.executeQuery();

            while(rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String pattern = rs.getString("pattern");
                double adi1_r = rs.getDouble("adi1_r");
                double adi2_r = rs.getDouble("adi2_r");
                double adi3_r = rs.getDouble("adi3_r");
                double adi4_r = rs.getDouble("adi4_r");
                double adi5_r = rs.getDouble("adi5_r");

                list.add(id+":"+type+":"+pattern+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r);
            }

        } catch (SQLException ex) {
            System.out.println("VendorError: " + ex.getErrorCode());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        return list;
    }



    /**
     *  初始化修补查询，并返回list  查询优质解
     */
    public  ArrayList<String> selectInitFixItem( ArrayList<Integer> overIndex,ArrayList<Integer> bigIndex) throws SQLException {

        //将overIndex 和 bigIndex 拼接成sql
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < overIndex.size(); i++) {
            int index = overIndex.get(i);
            sb.append("p"+index+"=0 and ");
        }
        sb.append(" ( ");
        for (int i = 0; i < bigIndex.size(); i++) {
            int index = bigIndex.get(i);
            sb.append("p"+index+"=1 or ");
        }
        String sqlWhere = sb.toString().substring(0, sb.toString().length() - 3);
        sqlWhere = sqlWhere +");";

        ArrayList<String> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            //System.out.println("驱动加载失败");
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("select * from sysu.adi20210523 where " + sqlWhere );
            rs = ps.executeQuery();

            while(rs.next()) {
                int id = rs.getInt("id");
                String attributes = rs.getString("pattern");
                double adi1_r = rs.getDouble("adi1_r");
                double adi2_r = rs.getDouble("adi2_r");
                double adi3_r = rs.getDouble("adi3_r");
                double adi4_r = rs.getDouble("adi4_r");
                double adi5_r = rs.getDouble("adi5_r");

                double adi1_d = rs.getDouble("adi1_d");
                double adi2_d = rs.getDouble("adi2_d");
                double adi3_d = rs.getDouble("adi3_d");
                double adi4_d = rs.getDouble("adi4_d");
                double adi5_d = rs.getDouble("adi5_d");

                list.add(id+":"+attributes+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r+":"+adi1_d+":"+adi2_d+":"+adi3_d+":"+adi4_d+":"+adi5_d);
            }

        } catch (SQLException ex) {
            System.out.println("SQLState: " + ex.getSQLState());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        return list;
    }



    /**
     * 查询，并返回id
     * adi20210528  adi_5_500   adi_8_1000
     */
    public   ArrayList<String> selectBySqlV2(String sqlWhere) throws SQLException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        ArrayList<String> list = new ArrayList<>();

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("select * from sysu.adi_8_1000 where " + sqlWhere );
            rs = ps.executeQuery();

            while(rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                double adi1_r = rs.getDouble("adi1_r");
                double adi2_r = rs.getDouble("adi2_r");
                double adi3_r = rs.getDouble("adi3_r");
                double adi4_r = rs.getDouble("adi4_r");
                double adi5_r = rs.getDouble("adi5_r");
                double adi6_r = rs.getDouble("adi6_r");
                double adi7_r = rs.getDouble("adi7_r");
                double adi8_r = rs.getDouble("adi8_r");


                list.add(id+":"+type+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r+":"+adi6_r+":"+adi7_r+":"+adi8_r);
            }

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        return list;

    }


    /**
     * 查询，并返回id
     * adi20210528  adi_5_500   adi_8_1000
     */
    public   ArrayList<String> selectBySql(String sqlWhere) throws SQLException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        ArrayList<String> list = new ArrayList<>();

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("select * from sysu.adi20210528 where " + sqlWhere );
            rs = ps.executeQuery();

            while(rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String attributes = rs.getString("pattern");
                double adi1_r = rs.getDouble("adi1_r");
                double adi2_r = rs.getDouble("adi2_r");
                double adi3_r = rs.getDouble("adi3_r");
                double adi4_r = rs.getDouble("adi4_r");
                double adi5_r = rs.getDouble("adi5_r");


                list.add(id+":"+type+":"+attributes+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r);
            }

        } catch (SQLException ex) {
            //System.out.println("SQLException: " + ex.getMessage());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        return list;

    }



    /**
     * 查询，并返回id,type,pattern
     * adi20210528  adi_5_500  adi_8_1000
     */
    public  String selectOneItemV2(int sqlId)  {


        ResultSet rs ;
        String item = "";
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/sysu?" + "user=root&password=root&useSSL=false");
             PreparedStatement ps = conn.prepareStatement("SELECT t1.id,t1.type,t1.penalty \n" +
                     "FROM sysu.adi_8_1000 AS t1 \n" +
                     "WHERE t1.id = " + sqlId)) {
            rs = ps.executeQuery();
            while(rs.next()){
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String penalty = rs.getString("penalty");
                item = id + ":" + type + ":" + penalty;

            }
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }
        return item;

    }



    /**
     * 查询，并返回id,type,pattern
     * adi20210528  adi_5_500  adi_8_1000
     */
    public  String selectOneItem(int sqlId) throws SQLException {


        ResultSet rs ;
        String item = "";
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/sysu?" + "user=root&password=root&useSSL=false");
             PreparedStatement ps = conn.prepareStatement("SELECT t1.id,t1.type,t1.pattern \n" +
                "FROM sysu.adi_5_500 AS t1 \n" +
                "WHERE t1.id = " + sqlId)) {
            rs = ps.executeQuery();
            while(rs.next()){
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String attributes = rs.getString("pattern");
                item = id + ":" + type + ":" + attributes;

            }
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }
        return item;

    }



    /**
     * 锦标赛查询，并返回list
     */
    public  ArrayList<String> selectChampionship(int num) throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("SELECT * FROM adi20210528 ORDER BY RAND() LIMIT "+ num+" ;");
            rs = ps.executeQuery();
            while(rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String attributes = rs.getString("pattern");
                list.add(id+":"+type+":"+attributes);
            }

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        return list;
    }



    /**
     * 锦标赛查询，并返回list
     */
    public  ArrayList<String> championshipSet(int num) throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("SELECT * FROM adi20210528 ORDER BY RAND() LIMIT "+ num+" ;");
            rs = ps.executeQuery();
            while(rs.next()) {

                int id = rs.getInt("id");
                String type = rs.getString("type");
                String attributes = rs.getString("pattern");
                double adi1_r = rs.getDouble("adi1_r");
                double adi2_r = rs.getDouble("adi2_r");
                double adi3_r = rs.getDouble("adi3_r");
                double adi4_r = rs.getDouble("adi4_r");
                double adi5_r = rs.getDouble("adi5_r");

                list.add(id+":"+type+":"+attributes+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r);

            }

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        return list;
    }

}



