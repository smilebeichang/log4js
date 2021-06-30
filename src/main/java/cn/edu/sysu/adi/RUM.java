package cn.edu.sysu.adi;

import cn.edu.sysu.utils.KLUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sun.print.SunMinMaxPage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author : song bei chang
 * @create 2021/4/24 19:40
 *          理解RUM的 attribute 与 pattern 的响应之间的关系
 *          构成 K_L information 矩阵
 */
public class RUM {


    /*
     *  竖 vs 横
     *  Todo d(A)jk1  j表示Item K表示attribute  1表示掌握 D(A)ij 表示 i vs j
     *  K_L information矩阵如下:
     *      0.0  0.0  1.14  1.14
     *      0.0  0.0  1.14  1.14
     *      1.36  1.36  0.0  0.0
     *      1.36  1.36  0.0  0.0
     *  则d(A)j11 = ((1 0)(0 0) + (1 1)(0 1))/2 =  (D42 + D31)/2 = (1.36 + 1.36)/2 = 1.36
     *   d(A)j10 = ((0 1)(1 1) + (0 0)(1 0))/2 =  (D24 + D13)/2 = (1.14 + 1.14)/2 = 1.14
     *   d(A)j21 = ((1 1)(1 0) + (0 1)(0 0))/2 =  (D43 + D21)/2 = (0 + 0)/2 = 0
     *   d(A)j20 = ((0 0)(0 1) +(1 0)(1 1))/2 =   (D12 + D34)/2 = (0 + 0)/2 = 0
     *
     *   d(A)1 = (1.36 + 1.14)/2 = 1.25
     *   d(A)2 = (0 + 0)/2 = 0
     */

    @Test
    public void GetRumAdi(){

        System.out.println("D(A)ij 计算 ");

        //d(A)j11 = 辨别第一个属性,(1 X) to (0 X)
        //d(A)j10 = 辨别第一个属性,(0 X) to (1 X)

        //X 可以是0和1 ,故(1 0)to(0 0),(1 1)to(0 1) ==》 D31 ,D42
        //根据value 求下标 ==》 可以定义一个map("(00)",1),然后用key求value

        Map<String,Integer> map = new HashMap<>(4);
        map.put("(0 0)",1);
        map.put("(0 1)",2);
        map.put("(1 0)",3);
        map.put("(1 1)",4);

        Double[][] klArray = GetRumKLArray();

        new KLUtils().arrayPrint(klArray);

        //d(A)j11 辨别第一个属性，且固定为(1 X) to (0 X)
        System.out.println("获取下标");
        ArrayList<String> list1 = new ArrayList();
        ArrayList<String> list2 = new ArrayList();
        Double sum1 = 0.0;
        Double sum2 = 0.0;
        Double adi1 ;
        Double adi2 ;


        for (int X =0;X<2;X++){
            //adi1
            Integer index1 = map.get("(1 " + X + ")");
            Integer index2 = map.get("(0 " + X + ")");
            Integer index3 = map.get("(0 " + X + ")");
            Integer index4 = map.get("(1 " + X + ")");

            //adi2
            Integer index5 = map.get("(" + X + " 1)");
            Integer index6 = map.get("(" + X + " 0)");
            Integer index7 = map.get("(" + X + " 0)");
            Integer index8 = map.get("(" + X + " 1)");

            System.out.println("D"+index1+index2);
            System.out.println("D"+index3+index4);
            list1.add(""+index1+index2);
            list1.add(""+index3+index4);

            System.out.println("D"+index5+index6);
            System.out.println("D"+index7+index8);
            list2.add(""+index5+index6);
            list2.add(""+index7+index8);
        }


        System.out.println("list 遍历 ;拿list的值去Array中匹配寻找,然后输出其大小");
        for(String data  :    list1)    {
            System.out.print(data);
            Double v  = klArray[Integer.parseInt(data.substring(0,1))-1][Integer.parseInt(data.substring(1,2))-1];
            System.out.println("  "+v);
            sum1+=v;
        }
        adi1=sum1/4;

        for(String data  :    list2)    {
            System.out.print(data);
            Double v  = klArray[Integer.parseInt(data.substring(0,1))-1][Integer.parseInt(data.substring(1,2))-1];
            System.out.println("  "+v);
            sum2+=v;
        }
        adi2=sum2/4;

        System.out.println("ADI结果如下:");
        System.out.println("adi1: "+adi1);
        System.out.println("adi2: "+adi2);

    }



    /*
     * eg:0.8   0.125    (1,0)
     *      //(0,0)
     *      P(0,0)(0,0)=0.8 * (1) * 1 = 0.8
     *      P(0,1)(0,0)=0.8 * (1) * 1 = 0.8
     *      P(1,0)(0,0)=0.8 * (1) * 1 = 0.8
     *      P(1,0)(0,0)=0.8 * (1) * 1 = 0.8
     *
     *
     *      P(0,0)(1,0)=0.8 * (0.125^1) * 1 = 0.1
     *      P(0,1)(1,0)=0.8 * (0.125^1) * 1 = 0.1
     *      P(1,0)(1,0)=0.8 * (1) * 1 = 0.8
     *      P(1,1)(1,0)=0.8 * (1) * 1 = 0.8
     *
     */

    /*  K_L information 和 rum 的区别
     *   hql 大厂急缺人   (1,0)
     *         (0,0)  (0,1)  (1,0)  (1,1)
     *  (0,0)
     *  (0,1)
     *  (1,0)
     *  (1,1)
     *
     */
    /*
     * K_L information矩阵如下:
     * 0.0  0.0  1.14  1.14
     * 0.0  0.0  1.14  1.14
     * 1.36  1.36  0.0  0.0
     * 1.36  1.36  0.0  0.0
     */

    /**
     *  K_L 矩阵计算
     *  行列分别表示 （0,0）（0,1）（1,0）（1,1）
     *               0.1    0.1   0.8   0.8
     *  Dj 表示 K_L 矩阵
     *    1. 定义一维数组 和 二维数组  The probability of a correct response
     *    2. for 计算并存储
     *    3. 遍历输出
     */
    public Double[][] GetRumKLArray(){
        //Method 1
        ArrayList<Double> lists1 = new ArrayList<>();
        lists1.add(0.1);
        lists1.add(0.1);
        lists1.add(0.8);
        lists1.add(0.8);

        //Method 2 , (double brace initialization)
        ArrayList<Double> lists2 = new ArrayList<Double>(){{
            add(0.1);
            add(0.1);
            add(0.8);
            add(0.8);
        }};

        Double[][] klArray = new KLUtils().foreach(lists1);

        new KLUtils().arrayPrint(klArray);

        return klArray;
    }




}



