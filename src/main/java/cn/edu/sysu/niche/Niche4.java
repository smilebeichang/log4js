package cn.edu.sysu.niche;


import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.*;

/**
 * @Author : song bei chang
 * @create 2021/7/3 10:22
 *
 *  复现 GA 和 Niche的代码，并实现多峰函数的曲线图
 *      1.稳态GA
 *      2.嵌入Niche
 *      3.替换适应度函数
 *      4.打印适应度值
 *
 * 《Messy genetic algorithms Motivation analysis and first results》Goldberg,Korb, Deb  1989
 *
 * 1. 稳态GA sizer =200，pc=0.4  迭代=100次 w=4c
 * 2. 已经将GA和小生境进行了初步融合
 * 3. 适应度函数的替换  使用文献中的多峰函数公式
 *        影响范围： 初始化部分+计算公式部分+选择部分
 *        初始化部分  f(x) = sin6(5πx)    f(x) = e-2*ln(2)*((x-0.1)/0.8)2 * sin6(5π(x3/4-0.05))
 *        关键:如何保证五个峰值  ==>   保证0.1、0.3、0.5 这些极值点 ==>  即x是个体，且能满足交叉变异
 *             基因 如何变成0.1呢？
 *             1. 给每个基因片段赋予一个初始值，范围均为0~1 ，然后取平均值，仍然能得到0~1的值。
 *                  影响可能是每个数字出现的频率呈现正态,进一步探讨 window 大小的影响
 *             2. 迭代过程中，使用f(x)来进行选择
 *             3. 交叉变异影响：判断相似时无论基因型还是表现型，需对double类型的数据进行转换  或者使用abs来进行判断
 *             4. 目前的困难在哪里：
 *                  找文献，下载不了，不找了，大不了自己实现
 *                  通过适应度开始验证融合的效果
 *                  交叉变异有一部耗时严重，需要进一步确认
 *
 * 4. 打印适应度值，绘制折线图
 *      1.全部打印  行不通，效果无法展示           not ok
 *      2.每隔10代全量打印一次，显示各个指标的位置   ok
 *
 *
 *
 */
public class Niche4 {

    /**
     * 容器
     */
    private double[][]  paper_genetic =new double[200][10];
    private int POPULATION_SIZE = 200;
    private int GENE_SIZE = 10;
    private int ITERATION_SIZE = 100;

    //private static Log log = LogFactory.getLog(Niche4.class);
    /**
     * log4j:WARN No appenders could be found for logger (cn.edu.sysu.niche.Niche4).
     * log4j:WARN Please initialize the log4j system properly.
     * log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
     */
    private static Logger log = Logger.getLogger(Niche4.class);

    /**
     *  1.稳态GA
     */
    @Test
    public void main()  {

        // 初始化
        init();

        // 迭代次数
        for (int i = 0; i < ITERATION_SIZE; i++) {

            // 选择
            selection();

            for (int j = 0; j < POPULATION_SIZE - 1; j++) {
                // 交叉
                crossCover(j);

                // 变异
                mutate(j);

                // 精英策略
                //elitistStrategy();
            }

            // 统计相似个体的数目  全量结果使用曲线表示
            if(i%5==0){
                //countCalculations();
                fitnessCalculations();
            }
        }
        System.out.println();

    }


    /**
     * 选择
     */
    public  void selection( ){

            //System.out.println("====================== select ======================");

            //200套试卷
            //int paperSize = POPULATION_SIZE;

            //轮盘赌 累加百分比
            double[] fitPie = new double[POPULATION_SIZE];

            //每套试卷的适应度占比  基因片段平均值的适应度值
            double[] fitPro = getFitness();

            //累加初始值
            double accumulate = 0;

            //试卷占总试卷的适应度累加百分比
            for (int i = 0; i < POPULATION_SIZE; i++) {
                fitPie[i] = accumulate + fitPro[i];
                accumulate += fitPro[i];
            }

            //累加的概率为1 数组下标从0开始
            fitPie[POPULATION_SIZE-1] = 1;

            //初始化容器 随机生成的random概率值
            double[] randomId = new double[POPULATION_SIZE];

            //生成随机id
            for (int i = 0; i < POPULATION_SIZE; i++) {
                randomId[i] = Math.random();
            }

            // 排序
            Arrays.sort(randomId);

            //轮盘赌 越大的适应度，其叠加时增长越快，即有更大的概率被选中
            double[][] new_paper_genetic =new double[POPULATION_SIZE][];
            int newSelectId = 0;
            for (int i = 0; i < POPULATION_SIZE; i++) {
                while (newSelectId < POPULATION_SIZE && randomId[newSelectId] < fitPie[i]){
                    //需要确保fitPie[i] 和 paperGenetic[i] 对应的i 是同一套试卷
                    new_paper_genetic[newSelectId]   = paper_genetic[i];
                    newSelectId += 1;
                }
            }

            //重新赋值种群的编码
            paper_genetic=new_paper_genetic;

    }





    /**
     * 交叉
     *      交叉后可能导致题目重复，解决方案：在变异后进行补全size
     *
     */
    public  void crossCover( int k ){

        //System.out.println("=== crossCover begin ===");

        if (Math.random() < 0.4) {
            //单点交叉  只保留一个个体
            double[] temp1 = new double[GENE_SIZE];
            int a = new Random().nextInt(GENE_SIZE);

            for (int j = 0; j < a; j++) {
                temp1[j] = paper_genetic[k][j];
            }
            for (int j = a; j < GENE_SIZE; j++) {
                temp1[j] = paper_genetic[k+1][j];
            }
            paper_genetic[k] = temp1;

        }

        //System.out.println("=== crossCover end ===");
    }


    /**
     * 变异
     */
    public  void mutate(int j)  {

        //System.out.println("=== mutate begin ===");

        //限制性锦标赛拥挤小生境
        ArrayList<Object> rts = new  Niche5().RTS(paper_genetic, j);
        paper_genetic = (double[][]) rts.get(1);

        //System.out.println("=== mutate end ===");
    }


    /**
     * 找出一个数组中一个数字出现次数最多的数字
     * 用HashMap的key来存放数组中存在的数字，value存放该数字在数组中出现的次数
     *
     * 将结果写到指定文件，便于后续统计
     *
     */
    private void countCalculations() throws FileNotFoundException {


        //log.info("测试 log4j");

        String[] array = new String[POPULATION_SIZE];

        for (int i = 0; i < POPULATION_SIZE; i++) {
            //排序操作，为了保证检测出相似性
            String[] strings = null;
            //String[] strings = sortPatch(paperGenetic[i]);
            StringBuilder idTmp = new StringBuilder();
            idTmp.append("[");
            for (String s : strings) {
                //将id抽取,拼接成新数组
                idTmp.append(s.split(":")[0]).append(",");
            }
            idTmp.append("]");
            array[i] = idTmp.toString();
        }


        //map的key数字，value出现的次数
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for(int i = 0; i < array.length; i++) {
            if(map.containsKey(array[i])) {
                int temp = map.get(array[i]);
                map.put(array[i], temp + 1);
            } else {
                map.put(array[i], 1);
            }
        }

        //输出每个个体出现的次数
        for(Map.Entry<String, Integer> entry : map.entrySet()) {
            String key = entry.getKey();
            Integer count = entry.getValue();
            //log.info("试题编号："+ key+"  次数："+count);
        }

        //找出map的value中最大的数字，即数组中数字出现最多的次数
        //Collection<Integer> count = map.values();
        //int max = Collections.max(count);
        //System.out.println(max);

        String maxKey = "";
        int maxCount = 0;
        for(Map.Entry<String, Integer> entry : map.entrySet()) {
            //得到value为maxCount的key，也就是数组中出现次数最多的数字
            if(maxCount < entry.getValue()) {
                maxCount = entry.getValue();
                maxKey = entry.getKey();
            }
        }
        //log.info("出现次数最多的数对象为：" + maxKey);
        //log.info("该数字一共出现" + maxCount + "次");

    }


    /**
     * 打印个体的适应度值
     */
    private void fitnessCalculations(){

        // 调用200个个体，计算每个个体的基因平均值
        // 计算试卷的适应度值
        for (int i = 0; i < POPULATION_SIZE; i++) {

            double sumnum = 0 ;
            for (int i1 = 0; i1 < GENE_SIZE; i1++) {
                sumnum = sumnum + paper_genetic[i][i1];
            }

            log.info(sin1(sumnum/GENE_SIZE));

        }
    }





    /**
     * 随机生成初代种群
     *      200个体  10基因  无序不重复
     */
    public  void  init( ) {
        System.out.println("=== init begin ===");
        for (int i = 0; i < POPULATION_SIZE; i++) {

            // 基因容器
            double[] testGene= new double[GENE_SIZE];
            Set<Double> set = new HashSet<>();

            //保证题目不重复,且满足长度约束 随机生成 基因
            for(int j = 0; j < GENE_SIZE; j++){

                while (set.size() == j ){
                    double key = numbCohesion(Math.random());
                    set.add(key);
                }

            }

            //增强for循环 进行赋值
            int index = 0;
            for(double gene : set){
                testGene[index] = gene;
                index ++;
            }

            paper_genetic[i] = testGene;
        }

        System.out.println("=== init end ===");

    }



    /**
     * 每套试卷的适应度占比
     *          计算基因片段的平均值，然后使用多峰函数计算
     *          平均值是将基因片段平均 还是将适应度值平均，其是存在差异的
     *          为了保证x的唯一，此处选择将基因片段平均
     *
     */
    private double[] getFitness(){

        log.info("适应值 log4j");

        // 所有试卷的适应度总和
        double fitSum = 0.0;

        // 每套试卷的适应度值
        double[] fitTmp = new double[POPULATION_SIZE];

        // 每套试卷的适应度占比
        double[] fitPro = new double[POPULATION_SIZE];

        // int length = paper_genetic[0].length;

        // 计算试卷的适应度值
        for (int i = 0; i < POPULATION_SIZE; i++) {

            double sumnum = 0 ;
            for (int i1 = 0; i1 < GENE_SIZE; i1++) {
                sumnum = sumnum + paper_genetic[i][i1];
            }

            // 个体、总和
            fitTmp[i] = sin1(sumnum/GENE_SIZE) ;
            fitSum = fitSum + fitTmp[i] ;

        }

        // 各自的比例
        for (int i = 0; i < POPULATION_SIZE; i++) {
            fitPro[i] = fitTmp[i] / fitSum;
        }

        //冒泡排序 打印top10
        //bubbleSort(fitTmp);

        return  fitPro;
    }


    /**
     * 实现多峰函数 f(x) = sin6(5πx)
     *
     */
    public double sin1(double avgnum ){

//        for (double i = 0; i < 1; i=i+0.001) {
            double degrees = 5 * 180 * avgnum;
            //将角度转换为弧度
            double radians = Math.toRadians(degrees);
            //正弦
            //System.out.format("%.1f 度的正弦值为 %.4f%n", degrees, Math.sin(radians));
            //次方
            //System.out.format("pow(%.3f, 6) 为 %.10f%n", Math.sin(radians),  Math.pow(Math.sin(radians), 6))
            //System.out.format("%f 为 %.10f%n", avgnum,  Math.pow(Math.sin(radians), 6));
            return Math.pow(Math.sin(radians), 6);
//        }


    }


    /**
     * 实现多峰函数 f(x) = e-2*ln(2)*((x-0.1)/0.8)2 * sin6(5π(x3/4-0.05))
     *
     */
    @Test
    public void sin2(){

        for (double i = 0; i < 1; i=i+0.001) {


            //自然常数e的近似值
            double e = Math.E;
            //System.out.println("e="+e);

            //e次方数
            double y = -2 * Math.log(2) * (Math.pow((i-0.1)/0.8,2));

            //输出结果
            double d = Math.pow(e, y);
            //System.out.println("e^"+y+"="+d);


            double degrees = 5 * 180 * (Math.pow(i,0.75)-0.05);
            //将角度转换为弧度
            double radians = Math.toRadians(degrees);

            //System.out.format("%f 为 %.10f%n", i,  d * Math.pow(Math.sin(radians), 6));

        }


    }


    /**
     * 格式转换工具
     *      保留小数点后三位
     */
    public Double numbCohesion(Double adi){

        return Double.valueOf(String.format("%.3f", adi));

    }



}



