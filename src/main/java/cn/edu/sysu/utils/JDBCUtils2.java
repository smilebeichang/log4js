package cn.edu.sysu.utils;


import java.sql.*;
import java.util.ArrayList;


/**
 * @Author : song bei chang
 * @create 2021/5/2 7:24
 */
public class JDBCUtils2 {



    /**
     * 查询，并返回list
     */
    public  ArrayList<String> select() throws SQLException {
            ArrayList<String> list = new ArrayList<>();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs ;
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (Exception ex) {
                System.out.println("驱动加载失败");
                ex.printStackTrace();
            }

            try {
                conn =
                        DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
                ps = conn.prepareStatement("select * from sysu.adi20210523 order by id  ;");
                rs = ps.executeQuery();
                while(rs.next()) {
                    int id = rs.getInt("id");
                    String attributes = rs.getString("pattern");
                    list.add(id+":"+attributes);
                }

            } catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
            }finally {
                if(ps!= null) {
                    ps.close();
                }
                if(conn!= null) {
                    conn.close();
                }
            }
            System.out.println();
            return list;
    }

    /**
     * 新增到数据库
     */
    public  void insert(int id,String pattern,Double base,String penalty,Double adi1_r,Double adi2_r,Double adi3_r,Double adi4_r,Double adi5_r) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;


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
            String sql = "INSERT INTO sysu.adi20210523 \n" +
                    "(id, pattern,p1,p2,p3,p4,p5, base, penalty, adi1_r, adi2_r, adi3_r, adi4_r, adi5_r) \n" +
                    "VALUES("+id+",\""+pattern+"\","+p1+","+p2+","+p3+","+p4+","+p5+","+base+",\""+penalty+"\","+adi1_r+","+adi2_r+","+adi3_r+","+adi4_r+","+adi5_r+");";
                    //"VALUES("+id+",\""+pattern+"\","+base+",\""+penalty+"\","+adi1_r+","+adi2_r+","+adi3_r+","+adi4_r+","+adi5_r+");";
            System.out.println(sql);
            ps = conn.prepareStatement(sql);
            ps.execute();

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
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
            System.out.println(sql);
            ps = conn.prepareStatement(sql);
            ps.execute();

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
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
     */
    public  int selectItem(String sql) throws SQLException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        int id = 0;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            System.out.println("驱动加载失败");
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("SELECT t1.id \n" +
                    "FROM sysu.adi20210523 AS t1 \n" +
                    "JOIN (\n" +
                     sql  +
                    ") AS t2 \n" +
                    "WHERE t1.id = t2.id limit 1;");
            rs = ps.executeQuery();
            rs.next();
            id = rs.getInt("id");

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        System.out.println();
        return id;

    }




    /**
     * 查询，并返回list
     */
    public  ArrayList<String> selectBachItem(String ids) throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        Connection conn  = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            System.out.println("驱动加载失败");
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root");
            ps = conn.prepareStatement("select * from sysu.adi20210523 where id in (" +
                    ids +  ");");
            rs = ps.executeQuery();
            System.out.println("select * from sysu.adi20210523 where id in (" + ids +  ");");

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
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        System.out.println();
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
        System.out.println("sqlwhere :"+sqlWhere);

        ArrayList<String> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            System.out.println("驱动加载失败");
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root");
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
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        System.out.println();
        return list;
    }



    /**
     * 查询，并返回id
     */
    public   ArrayList<String> selectBySql(String sqlWhere) throws SQLException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        ArrayList<String> list = new ArrayList<>();

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            System.out.println("驱动加载失败");
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
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        System.out.println();
        return list;

    }


}



