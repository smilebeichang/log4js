package cn.edu.sysu.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author : song bei chang
 * @create 2021/5/19 10:34
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ADI {

    private int id ;
    private String pattern ;
    private double base ;
    private String penalty ;
    private double adi1_r ;
    private double adi2_r ;
    private double adi3_r ;
    private double adi4_r ;
    private double adi5_r ;

    private double ps;
    private double pg;
    private double adi1_d ;
    private double adi2_d ;
    private double adi3_d ;
    private double adi4_d ;
    private double adi5_d ;

}



