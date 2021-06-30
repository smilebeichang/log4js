package cn.edu.sysu.utils;




import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;



/**
 * @Author : song bei chang
 * @create 2021/5/2 7:24
 */
public class JDBCUtils {



    /**
     * 查询，并返回list
     */
    public  ArrayList select() {
            ArrayList list = new ArrayList();
            Connection conn ;
            PreparedStatement ps ;
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
                ps = conn.prepareStatement("select * from sysu.adi order by id  ;");
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

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            String sql = "INSERT INTO sysu.adi \n" +
                    "(id, pattern, base, penalty, adi1_r, adi2_r, adi3_r, adi4_r, adi5_r) \n" +
                    "VALUES("+id+",\""+pattern+"\","+base+",\""+penalty+"\","+adi1_r+","+adi2_r+","+adi3_r+","+adi4_r+","+adi5_r+");";
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
            String sql = "UPDATE sysu.adi \n" +
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
     * 查询，并返回list
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
                    "FROM sysu.adi AS t1 \n" +
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
    public  ArrayList selectBachItem(String ids) {
        ArrayList list = new ArrayList();
        Connection conn ;
        PreparedStatement ps ;
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
            ps = conn.prepareStatement("select * from sysu.adi where id in (" +
                    ids +  ");");
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
        }
        System.out.println();
        return list;
    }

}



