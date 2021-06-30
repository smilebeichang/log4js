package cn.edu.sysu.utils;


import org.junit.Test;


import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @Author : song bei chang
 * @create 2021/5/6 19:09
 */
public class KLUtils {


    /**
     * 计算K_L矩阵
     * @param rumList 答对此题的概率集合  横向 vs 纵向
     * @return  Double[][]
     */
    public   Double[][] foreach( ArrayList<Double> rumList) {

        //计算 K_L
        Double[][] klArray =new Double[rumList.size()][rumList.size()];
        for (int i = 0;i<rumList.size();i++){
            for (int j = 0; j < rumList.size(); j++) {
                //(0,0) vs (0,0)
                double v = rumList.get(i) * Math.log(rumList.get(i) / rumList.get(j)) +
                        (1 - rumList.get(i)) * Math.log((1 - rumList.get(i)) / (1 - rumList.get(j)));

                v = Double.valueOf((v+"0000").substring(0,4));
                klArray[i][j] = v;
            }
        }

        return klArray;
    }


    /**
     * 使用foreach方法对二维数组进行遍历
     * @param klArray  K_L 矩阵
     */
    public  void arrayPrint(Double[][] klArray) {
        //遍历输出 K_L 矩阵
        //System.out.println("K_L information矩阵如下: ");
        for (Double[] fs:klArray) {
            for (Double fss:fs) {
                //相当于arr[i][j]
                System.out.print(fss+"  ");
            }
            //System.out.println();
        }
    }




    /**
     * 生成指定范围，指定小数位数的随机数
     * @param max 最大值
     * @param min 最小值
     * @param scale 小数位数
     * @return
     */
    public Double makeRandom(float max, float min, int scale){
        BigDecimal cha = new BigDecimal(Math.random() * (max-min) + min);
        //保留 scale 位小数，并四舍五入
        return cha.setScale(scale,BigDecimal.ROUND_HALF_UP).doubleValue();
    }



    /**
     * 随机生成pattern
     * @param num  该pattern包含的属性个数
     * @return
     * @throws InterruptedException
     */
    public String randomInit(int num ) throws InterruptedException {

        if(num == 0 ){
            System.err.println("提示：属性不能全为空！！");
            Thread.sleep(2000);
            //直接 return 是否有效，待验证
            return null;
        }

        //随机生成
        //String attributes ;
        Set<String> fill_set = new HashSet<>();
        for (int j = 0; j < num; j++) {
            //a的ASCII码 数字--字符--pattern
            while (fill_set.size() == j ){
                String c = ((char) (Math.random() * 5 + 'a'))+"";
                fill_set.add(c);
            }
        }
        //attributes = fill_set.toString();
        int p1 = fill_set.contains("a")?1:0;
        int p2 = fill_set.contains("b")?1:0;
        int p3 = fill_set.contains("c")?1:0;
        int p4 = fill_set.contains("d")?1:0;
        int p5 = fill_set.contains("e")?1:0;
        //System.out.println("=======================");
        String ip = "("+p1+","+p2+","+p3+","+p4+","+p5+")";
        ////System.out.println("属性："+attributes);
        //System.out.println("属性："+ip);
        return ip;

    }


    /**
     * 返回所有子集合的组成
     *
     */
    public  Set<Set<String>> getSubCollection(String[] arr) {

        Set<Set<String>> f = f(arr.length, arr);
        //System.out.println(f);
        return f;

    }

    public  Set<Set<String>> f(int k, String[] arr) {
        if (k == 0) {
            Set<Set<String>> set = new HashSet<>();
            //添加一个空集合
            set.add(new HashSet<>());
            return set;
        }

        Set<Set<String>> set = f(k - 1, arr);
        Set<Set<String>> resultSet = new HashSet<>();

        //扫描上一层的集合
        for (Set<String> integerSet : set) {

            //上一层的每个集合都包含两种情况，一种是加入新来的元素，另一种是不加入新的元素
            HashSet<String> subSet = new HashSet<>();

            subSet.addAll(integerSet);
            subSet.add(arr[k - 1]);

            resultSet.add(subSet);
            resultSet.add(integerSet);
        }
        return resultSet;
    }



}



