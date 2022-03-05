package cn.edu.sysu.utils;


import cn.edu.sysu.adi.TYPE;
import cn.edu.sysu.clique.MaxcliqueV2;
import org.apache.log4j.Logger;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * @Author : song bei chang
 * @create 2021/5/6 19:09
 */
public class KLUtilsV2 {

    /** 打印对象 **/
    private Logger log = Logger.getLogger(KLUtilsV2.class);


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
     */
    public String randomInit(int num ) throws InterruptedException {

        /**
         * 输入判空操作
         */
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
                String c = ((char) (Math.random() * 8 + 'a'))+"";
                fill_set.add(c);
            }
        }
        //attributes = fill_set.toString();
        int p1 = fill_set.contains("a")?1:0;
        int p2 = fill_set.contains("b")?1:0;
        int p3 = fill_set.contains("c")?1:0;
        int p4 = fill_set.contains("d")?1:0;
        int p5 = fill_set.contains("e")?1:0;
        int p6 = fill_set.contains("f")?1:0;
        int p7 = fill_set.contains("g")?1:0;
        int p8 = fill_set.contains("h")?1:0;
        String ip = "("+p1+","+p2+","+p3+","+p4+","+p5+","+p6+","+p7+","+p8+")";
        System.out.println("属性："+ip);
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



    /**
     * 冒泡排序  打印top10
     *
     */
    /**  散点图索引  */
    private int scatterIndex = 0;
    public void bubbleSort(double[] a) {
        double temp ;
        for(int i=0 ;i < a.length ;i++) {
            for(int j=0 ; j< a.length-i -1;j++) {
                if(a[j]>a[j+1]) {
                    //互换位置
                    temp = a[j];
                    a[j] = a[j+1] ;
                    a[j+1] = temp ;
                }
            }
        }
        //遍历数组排序  arr[0]=2.3626148494872097
        for(int i=a.length -1 ;i >=90  ;i--) {
            //System.out.printf("arr[%d]=%s\n",i,a[i]);
            scatterIndex = scatterIndex +1 ;
            log.info(scatterIndex + ":" + numbCohesion(a[i]));
        }

    }


    /**
     * 格式转换工具
     */
    public Double numbCohesion(Double adi){


        return Double.valueOf(String.format("%.4f", adi));

    }



    /**
     * 15%,算出最大的圈 maximum clique
     *
     * 1.通过题目相似个数,形成距离关系w矩阵
     * 2.写入文件
     * 3.读取文件,计算最大圈
     *
     */
    public ArrayList<String>  similarClique(ArrayList<String> inBack, int algorithm,ArrayList<String> allItemList) {

        // 距离关系w矩阵
        int[][] distanceMatrix = new int[inBack.size() + 1][inBack.size() + 1];

        // 遍历计算距离关系,并生成01矩阵 初始化矩阵
        for (int i = 0; i < inBack.size() + 1; i++) {

            // 赋值
            for (int j = 0; j < inBack.size() + 1; j++) {

                // 第一行
                if (i < 1) {
                    distanceMatrix[i][j] = -1;
                }

                // 第一列
                if (j == 0) {
                    distanceMatrix[i][j] = -1;
                }
            }
        }

        // 遍历集合
        for (int i = 0; i < inBack.size(); i++) {

            // 矩阵 (根据题目的相似个数 判断相似个体,若题目相同数低于4,则赋值为1)
            String aids = inBack.get(i).split("_")[1];

            for (int j = 0; j < inBack.size(); j++) {

                if (!inBack.get(i).equals(inBack.get(j))) {

                    String bids = inBack.get(j).split("_")[1];

                    // 将基因型转为list,使用list来判断相似个数
                    List<String> ListA = stringToList(aids);
                    List<String> ListB = stringToList(bids);

                    // 计算相似题目个数
                    int counter = 0;
                    for (String c : ListB) {
                        for (String d : ListA) {
                            if (c.equals(d)) {
                                counter = counter + 1;
                            }
                        }
                    }

                    // 以15%为界限  第三行开始,第二列开始 最大相似设置的过大,将导致计算缓慢
                    // 而且只能延缓  无法最终解决
                    if (counter < 4) {

                        // 校验两个集合的相似程度
                        if (checkAttr(ListA, ListB, algorithm,allItemList)) {
                            distanceMatrix[i + 1][j + 1] = 1;
                        }
                    }
                }
            }
        }



        // 写入文件
        sinkToFileV1(distanceMatrix);

        // 读取文件
        ArrayList<String> mqList = readFromFileV1();

        System.out.println(" + ----------------------- + ");

        return mqList;

    }


    /**
     * string 转 list
     */
    private List<String> stringToList(String strs) {
        String str[] = strs.split(",");
        return Arrays.asList(str);
    }


    /**
     * 校验两个集合的相似程度
     *
     * @param listA
     * @param listB FIXME 此处应该加上其他验证逻辑 而不仅仅是题目不同
     *              功能相似： 题型、属性(拿单个题目计算还是一套试卷)
     *              思考：
     *              1.如果是单个题目的话,计算量会比较大
     *              每道题目 其余试卷的每道题挨个进行比较，意义不大
     *              2.如果是单套试卷的话,整套试卷经过了correct,其应该是相似的
     *              3.此处以单套试卷为单位，先进行计算
     *              3.1 通过ids,获取属性和类型
     *              3.2 分别计算属性和类型的数目 sum(abs())
     *              3.3 如果小于了某一个临界值,则表明其是真的相似
     */
    private Boolean checkAttr(List<String> listA, List<String> listB, int diffDegree,ArrayList<String> allItemList) {

        Boolean flag = false;

        System.out.println(listA);


        // 根据id从数据库中查询相对应的题目
        String idsA = listA.toString().substring(1, listA.toString().length() - 1);

        List<String> sListA = Arrays.asList(idsA.split(","));

        // 题型
        int typeChoseA = 0;
        int typeFillA = 0;
        int typeShortA = 0;
        int typeCompreA = 0;

        // 属性
        int attributeNum1A = 0;
        int attributeNum2A = 0;
        int attributeNum3A = 0;
        int attributeNum4A = 0;
        int attributeNum5A = 0;
        int attributeNum6A = 0;
        int attributeNum7A = 0;


        for (int k = 0; k < sListA.size(); k++) {

            String s = allItemList.get(Integer.parseInt(sListA.get(k).trim()) - 1 > -1 ? Integer.parseInt(sListA.get(k).trim()) - 1 : 1);

            //计算每种题型个数
            if (TYPE.CHOSE.toString().equals(s.split(":")[1])) {
                typeChoseA += 1;
            }
            if (TYPE.FILL.toString().equals(s.split(":")[1])) {
                typeFillA += 1;
            }
            if (TYPE.SHORT.toString().equals(s.split(":")[1])) {
                typeShortA += 1;
            }
            if (TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])) {
                typeCompreA += 1;
            }


            //计算每种属性的个数
            if (!"0.0".equals(s.split(":")[2])) {
                attributeNum1A += 1;
            }
            if (!"0.0".equals(s.split(":")[3])) {
                attributeNum2A += 1;
            }
            if (!"0.0".equals(s.split(":")[4])) {
                attributeNum3A += 1;
            }
            if (!"0.0".equals(s.split(":")[5])) {
                attributeNum4A += 1;
            }
            if (!"0.0".equals(s.split(":")[6])) {
                attributeNum5A += 1;
            }
            if (!"0.0".equals(s.split(":")[7])) {
                attributeNum6A += 1;
            }
            if (!"0.0".equals(s.split(":")[8])) {
                attributeNum7A += 1;
            }

        }


        // 根据id从数据库中查询相对应的题目
        String idsB = listB.toString().substring(1, listB.toString().length() - 1);

        List<String> sListB = Arrays.asList(idsB.split(","));

        // 题型
        int typeChoseB = 0;
        int typeFillB = 0;
        int typeShortB = 0;
        int typeCompreB = 0;

        // 属性
        int attributeNum1B = 0;
        int attributeNum2B = 0;
        int attributeNum3B = 0;
        int attributeNum4B = 0;
        int attributeNum5B = 0;
        int attributeNum6B = 0;
        int attributeNum7B = 0;


        for (int k = 0; k < sListB.size(); k++) {

            String s = allItemList.get(Integer.parseInt(sListB.get(k).trim()) - 1 > -1 ? Integer.parseInt(sListB.get(k).trim()) - 1 : 1);

            //计算每种题型个数
            if (TYPE.CHOSE.toString().equals(s.split(":")[1])) {
                typeChoseB += 1;
            }
            if (TYPE.FILL.toString().equals(s.split(":")[1])) {
                typeFillB += 1;
            }
            if (TYPE.SHORT.toString().equals(s.split(":")[1])) {
                typeShortB += 1;
            }
            if (TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])) {
                typeCompreB += 1;
            }


            //计算每种题型个数
            if (!"0.0".equals(s.split(":")[2])) {
                attributeNum1B += 1;
            }
            if (!"0.0".equals(s.split(":")[3])) {
                attributeNum2B += 1;
            }
            if (!"0.0".equals(s.split(":")[4])) {
                attributeNum3B += 1;
            }
            if (!"0.0".equals(s.split(":")[5])) {
                attributeNum4B += 1;
            }
            if (!"0.0".equals(s.split(":")[6])) {
                attributeNum5B += 1;
            }
            if (!"0.0".equals(s.split(":")[7])) {
                attributeNum6B += 1;
            }
            if (!"0.0".equals(s.split(":")[8])) {
                attributeNum7B += 1;
            }
        }


        // 属性
        int type = Math.abs(typeChoseA - typeChoseB) + Math.abs(typeFillA - typeFillB) + Math.abs(typeShortA - typeShortB) + Math.abs(typeCompreA - typeCompreB);

        int attr = Math.abs(attributeNum1A - attributeNum1B) + Math.abs(attributeNum2A - attributeNum2B) + Math.abs(attributeNum3A - attributeNum3B) + Math.abs(attributeNum4A - attributeNum4B) + Math.abs(attributeNum5A - attributeNum5B)+ Math.abs(attributeNum6A - attributeNum6B)+ Math.abs(attributeNum7A - attributeNum7B);

        System.out.println(type + ":" + attr);

        // 可在此处做相关推断，并赋予不同的值,或者 switch case
        switch (diffDegree) {
            case 1:
                if (type < 6 && attr < 6) {
                    flag = true;
                }
                break;
            case 2:
                if (type < 6 && attr < 12) {
                    flag = true;
                }
                break;
            default:
                if (type < 6 && attr < 18) {
                    flag = true;
                }
        }


        return flag;

    }



    /**
     * 写入文件
     *
     * @param distanceMatrix
     */
    private void sinkToFileV1(int[][] distanceMatrix) {
        OutputStream os = null;
        try {
            os = new FileOutputStream("F:\\song\\SYSU\\Log4j\\input\\output.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        PrintWriter pw = new PrintWriter(os);


        // 打印 遍历二维数组
        for (int i1 = 0; i1 < distanceMatrix.length; i1++) {
            if (i1 == 0) {
                for (int i2 = 0; i2 < distanceMatrix[i1].length; i2++) {
                    pw.print(distanceMatrix[i1][i2] + " , ");
                }
                pw.print("333");
                pw.println();
            }

            for (int i2 = 0; i2 < distanceMatrix[i1].length; i2++) {
                pw.print(distanceMatrix[i1][i2] + " , ");
            }
            pw.print("333");
            pw.println();
        }

        pw.close();
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * 计算最大圈
     */
    MaxcliqueV2 mq = new MaxcliqueV2();


    /**
     * 读取文件,输出最大圈顶点数和最大圈的个数
     */
    private ArrayList<String> readFromFileV1() {

        return  mq.readFromFileV1();

    }




    /**
     * 验证BSF的整套平行试卷的适应度平均值和方差, 校验效果和波动性
     * 如果校验以后发现效果不是很理想，可以换成top的平均值
     *
     * FIXME: 平行试卷的avg
     * 1. avg 和 sd 的计算(只取top适应度值最大的一份,然后比较适应度值的方差,可以均返回,然后循坏做比较即可)
     * 2. avg 的检验是否需要过滤或者做某种操作,使其可以正常比较.目前差异过大
     */
    public void calAvgFitness(ArrayList<String> inBack,ArrayList<String> mqList) {

        ArrayList<String> avgList = new ArrayList<>();
        List <String> ml = Arrays.asList(mqList.get(mqList.size() - 1).split("_"));

        // 遍历ml,获得下标
        for (int i = 0;i< ml.size();i++) {
            if ("1".equals(ml.get(i))){
                System.out.println(inBack.get(i));
                avgList.add(inBack.get(i));
            }
        }


        // 1.遍历list,计算每个个体的fitness值,并使用变量进行汇总统计
        // 计算平均值
        Double avgsum = 0.0;
        Double avg = 0.0;

        if (avgList.size() > 0) {

            for (String s : avgList) {
                avgsum = avgsum + Double.valueOf(s.split("_")[0]);
            }
            avg = avgsum / avgList.size();

        }

        // 2.sum / count
        System.out.println(avg);
        log.info("top 50%的平均适应度值：" + avg);

        // 3.计算波动  方差=1/n(s^2+....)  方差越小越稳定
        Double sdsum = 0.0;
        Double sd = 0.0;

        if (avgList.size() > 0) {

            for (String s : avgList) {
                sdsum = sdsum + Math.pow((Double.valueOf(s.split("_")[0]) - avg), 2);
            }
            sd = sdsum / avgList.size();

        }
        System.out.println(sd);
        log.info("top 50%的波动情况：" + sd);


    }






}



