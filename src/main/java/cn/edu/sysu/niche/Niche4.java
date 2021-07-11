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
 * FIXME 本周任务
 *     4.1 校验niche效果 + 相似度指标的确定
 *          交叉变异: r表达式 rx1+(1-r)x2       abs(x1 -x2)
 *
 *     4.2 niche 和 修补 的冲突
 *          niche 当做评价因子来使用,
 *          初始化 -- 选择 -- 交叉 -- 变异 -- 修补 -- niche
 *
 *     基本能跑，而且能够往适应度高的地方得出适应度值，很强
 *     接下来要处理的问题:
 *          1.保证多样性，延迟over time
 *          2.计算每个个体的数目,得出个体数目曲线图
 *
 *          人工计算终究不是回事，需要使用代码简化操作。如：abs小于0.02 则等于0.5
 *          第40代
 *              试题编号：0.1  次数：17
 *              试题编号：0.3  次数：30
 *              试题编号：0.5  次数：115
 *              试题编号：0.7  次数：2
 *              试题编号：0.9  次数：36
 *
 *          第60代
 *              试题编号：0.1  次数：35
 *              试题编号：0.3  次数：31
 *              试题编号：0.5  次数：116
 *              试题编号：0.7  次数：1
 *              试题编号：0.9  次数：17
 *
 */
public class Niche4 {

    /**
     * 容器
     */
    private double[]  paper_genetic =new double[200];
    private int POPULATION_SIZE = 200;
    private int ITERATION_SIZE = 200;

    /**
     *  全局变量
     */
    double tmp1;
    double tmp2;

    //private static Log log = LogFactory.getLog(Niche4.class);
    private  Logger log = Logger.getLogger(Niche4.class);

    /**
     *  1.稳态GA
     */
    @Test
    public void main(){

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
                mutate();

                // 精英策略
                //elitistStrategy();

                // 确定性拥挤小生境
                dc(j);

            }

            // 统计相似个体的数目
            if(i%10==0){
                countCalculations(paper_genetic);
                //fitnessCalculations();
            }
        }
        System.out.println();

    }


    /**
     * 选择
     */
    public  void selection( ){

            System.out.println("====================== select ======================");


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
            double[] new_paper_genetic =new double[POPULATION_SIZE];
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
     *      完全按照老师的文档来，转换为代码，看看效果，争取实现满足效果
     *      交叉变异: r表达式 rx1+(1-r)x2
     *
     *      不修改父代,且保留两个新个体
     *
     */
    public  void crossCover( int k ){

        System.out.println("=== crossCover begin ===");

        double pm = 0.4;
        if (Math.random() < pm) {
            // 保留交叉后的两新个体，并提升为全局变量
            double lemuda = Math.random();
             tmp1 = (lemuda * paper_genetic[k]) + ((1 - lemuda) * paper_genetic[k + 1]);
             tmp2 = (lemuda * paper_genetic[k]) + ((1 - lemuda) * paper_genetic[k + 1]);
        }

    }


    /**
     * 变异
     *      将变异和小生境剥离:
     *          变异使用 r表达式 rx1+(1-r)x2
     *          小生境作为修补后的评价因子
     */
    public  void mutate()  {

        System.out.println("=== mutate begin ===");

        if (Math.random() < 0.4) {
            // 保留变异后的两新个体，并提升为全局变量
            double lemuda = Math.random();
            tmp1 = (lemuda * tmp1) + ((1 - lemuda) * Math.random());
            tmp2 = (lemuda * tmp2) + ((1 - lemuda) * Math.random());

        }

    }


    /**
     * 找出一个数组中一个数字出现次数最多的数字
     * 用HashMap的key来存放数组中存在的数字，value存放该数字在数组中出现的次数
     *
     * 将结果写到指定文件，便于后续统计
     *      需核实是否需要将key进行格式化，如保留小数点后四位
     *
     */
    private void countCalculations(double[] paperGenetic) {


        log.info("测试 log4j");

        double[] array = new double[paperGenetic.length];


        //map的key数字，value出现的次数
        HashMap<Double, Integer> map = new HashMap<>();
        for(int i = 0; i < array.length; i++) {
            double tmpKey = formatDouble(paperGenetic[i]);
            if(map.containsKey(tmpKey)) {
                int tempCount = map.get(tmpKey);
                map.put(tmpKey, tempCount + 1);
            } else {
                map.put(tmpKey, 1);
            }
        }

        //输出每个个体出现的次数
        for(Map.Entry<Double, Integer> entry : map.entrySet()) {
            double key = entry.getKey();
            Integer count = entry.getValue();
            log.info("试题编号："+ key+"  次数："+count);
        }


    }





    /**
     * 随机生成初代种群
     *      200个体  单基因
     */
    public  void  init( ) {
        System.out.println("=== init begin ===");
        for (int i = 0; i < POPULATION_SIZE; i++) {
            paper_genetic[i] = Math.random();
        }
    }



    /**
     * 每套试卷的适应度占比
     *          使用多峰函数计算计算基因的适应度值
     *
     */
    private double[] getFitness(){

        //log.info("适应值 log4j");

        // 所有试卷的适应度总和
        double fitSum = 0.0;

        // 每套试卷的适应度值
        double[] fitTmp = new double[POPULATION_SIZE];

        // 每套试卷的适应度占比
        double[] fitPro = new double[POPULATION_SIZE];


        // 计算试卷的适应度值
        for (int i = 0; i < POPULATION_SIZE; i++) {

            // 个体、总和
            fitTmp[i] = sin1(paper_genetic[i]) ;
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

            System.out.format("%f 为 %.10f%n", i,  d * Math.pow(Math.sin(radians), 6));

        }


    }



    /**
     * 确定性拥挤小生境
     *
     */
    public void  dc(int i)  {

        // 父代 c1 c2
        ArrayList<Double> cList = new ArrayList<>(2);
        cList.add(paper_genetic[i]);
        cList.add(paper_genetic[i+1]);


        // 为c1从当前种群中随机选取c*w个体  5个小生境  4*5元锦标赛
        // ArrayList<Map<Integer, Double>[]> cwList = championship();
        // 确定性拥挤算法
        ArrayList<Double> cwList = deterministicCrowding();

        // 替换 or 保留
        // cList: 原始父代，  cwList:新个体 原始父代 和 交叉变异后的个体进行比较操作
        // closestResemble(cList, cwList);
        closestResembledc(cList, cwList, i);

    }


    /**
     * 如果f(c1)>f(d1),则用c1替换d1,否则保留d1;
     * 如果f(c2)>f(d2),则用c2替换d2,否则保留d2;
     *
     *      换成基因型吗:为了多样性
     *      替换表现型相似的个体，其后期跳出循环的可能性不大，以及逻辑上存在问题，多峰且各个峰值相等
     *      替换基因型相似的个体，其是否能维持多样性？ 待确认
     *
     */
    private void closestResemble(ArrayList<Double> cList, ArrayList<Map<Integer, Double>[]> cwList) {
        //  表现型  适应度值，或者 minAdi
        //  基因型  解(2,3,56,24,4,6,89,98,200,23)
        double c1 = cList.get(0);

        Map<Integer, Double>[] cw1 = cwList.get(0);

        // 选取基因型做相似性校验
        similarGene(c1, cw1);

    }

    /**
     * 使用确定性拥挤计算出相似的个体,并执行替换操作
     *
     *      1 如果[d(p1,c1)+d(p2,c2)]<=[d(p1,c2)+d(p2,c1)]
     *              如果f(c1)>f(p1),则用c1替换p1,否则保留p1;
     *              如果f(c2)>f(p2),则用c2替换p2,否则保留p2;
     *      2 否则
     *               如果f(c1)>f(p2),则用c1替换p2,否则保留p2;
     *               如果f(c2)>f(p1),则用c2替换p1,否则保留p1;
     *
     *
     */
    private void closestResembledc(ArrayList<Double> cList, ArrayList<Double> cwList,int i) {

        //  表现型  适应度值，或者 minAdi
        //  基因型  解(2,3,56,24,4,6,89,98,200,23)
        double c1 = cList.get(0);
        double c2 = cList.get(1);

        double cw1 = cwList.get(0);
        double cw2 = cwList.get(1);

        double dc1 = sin1(c1);
        double dc2 = sin1(c2);
        double dcw1 = sin1(cw1);
        double dcw2 = sin1(cw2);

        double d1 = Math.abs(dc1 - dcw1) + Math.abs(dc2 - dcw2);
        double d2 = Math.abs(dc1 - dcw2) + Math.abs(dc2 - dcw1);

        if(d1 <= d2){
            if(dc1 < dcw1){
                paper_genetic[i]=cw1;
            }
            if(dc2 < dcw2){
                paper_genetic[i+1]=cw2;
            }
        }else {
            if(dc1 < dcw2){
                paper_genetic[i]=cw2;
            }
            if(dc2 < dcw1){
                paper_genetic[i+1]=cw1;
            }
        }

    }





    /**
     * 在cw1中寻找c1的近似解  5个小生境  4*5元锦标赛  c1是一套试卷  cw1是c*w套试卷
     * 根据基因型来找出最相似的值
     *
     */
    private void similarGene(double c1, Map<Integer, Double>[] cw1) {

        double max = 9999;
        // 设置为0  可能会导致0号索引的数据一直在变化 解决方案：使得每次均能找到相似的个体
        int maxPhen = 0;

        // 外层C小生境数，内层W元锦标赛
        // FIXME 考虑一下，窗口大小究竟是 4*5 还是 4
        for (Map<Integer, Double> aCw11 : cw1) {

            double c2;
            // 遍历map
            for (int j = 0; j < aCw11.size(); j++) {
                for (Object o : aCw11.keySet()) {
                    int key = (int) o;
                    c2 = aCw11.get(key);

                    // 获取最相似的解  相似的判定标准：基因型
                    double sameNum = compareArrSameNum(c1, c2);
                    if (max > sameNum) {
                        max = sameNum;
                        maxPhen = key;
                    }
                }
            }
        }

        System.out.println("相似的差值："+max +"  最相似的个体："+maxPhen );


        // 替换c1  替换判定标准：表现型|适应度
        //个体 | 最相似个体 适应度
        double fitc1 = sin1(paper_genetic[1]) ;
        double fitc2 = sin1(paper_genetic[maxPhen]) ;

        if (fitc1 > fitc2){
            paper_genetic[maxPhen] = c1;
        }

    }



    /**
     *  比较2个数组中相同的个数
     *      难点在于 可能基因型都只有一个元素不一样  故基因型的判断可能需要将改变了近似相等
     *      会是这里导致 速度变慢的吗？
     */
    private double compareArrSameNum(double arr, double arr2) {

        return  Math.abs(arr - arr2);
    }





    /**
     *  分别为c1从当前种群中随机选取c*w个体
     *  当前种群和题库的关系
     *  题库: 310 道题
     *  种群: 4*5<=20（存在重复+交叉变异）
     *
     *  是否是20元锦标赛过大，待后续优化
     *  Map<Integer, double[]>  key是paperGenetic的索引，value是基因型
     *
     */
    private ArrayList<Map<Integer, Double>[]> championship()  {

        // 5个小生境  4*5元锦标赛  需进一步验证  窗口大小的具体含义
        int num = 5 ;
        int window = 4 * 5;
        Map<Integer, Double>[] cwList1 = new HashMap[num];

        // 基本单位:试卷。故随机生成一个下标即可 (需保存下标,方便后续替换 map(k,v))
        // 数组裹map
        for (int i = 0; i < num; i++) {
            Set<String> set1 = new HashSet<>();
            // 将个体保存为map结构
            Map<Integer, Double> mapc1w = new HashMap<>(window);
            while (set1.size() != window) {
                int i1 = new Random().nextInt(POPULATION_SIZE);
                if (!set1.contains(":"+i1)) {
                    set1.add(":"+i1 );
                    mapc1w.put(i1,paper_genetic[i1]);
                }
                cwList1[i] = mapc1w;
            }
        }

        ArrayList<Map<Integer, Double>[]> cwList = new ArrayList<>(1);
        cwList.add(cwList1);
        // 获取个体的方法:   cwList.get(0)[1]
        return cwList;

    }

    /**
     * 确定性拥挤算子
     *
     */
    private ArrayList<Double>  deterministicCrowding(){

        ArrayList<Double> cList = new ArrayList<>(2);
        cList.add(tmp1);
        cList.add(tmp2);
        return cList;

    }





    /**
     * 格式转换工具, 保留小数点后三位
     */
    public Double numbCohesion(Double adi){

        return Double.valueOf(String.format("%.3f", adi));

    }

    /**
     * double 格式转换, 保留小数点后四位
     */
    public Double formatDouble(double x1){

        return Double.valueOf(String.format("%.4f", x1));
    }


    /**
     * 实现多峰函数 f(x) = sin6(5πx)
     *
     */
    private double sin1(double avgnum ){

        double degrees = 5 * 180 * avgnum;
        //将角度转换为弧度
        double radians = Math.toRadians(degrees);

        System.out.format("%f 为 %.10f%n", avgnum,  Math.pow(Math.sin(radians), 6));
        return Math.pow(Math.sin(radians), 6);

    }


    /**
     * 打印个体的适应度值
     *      1. 散点图（key|value）
     *      2. 200 和 1000 之间的关系如何确定
     *          2.1 设置为1000，显然不合适，而且这个1000 只是为了配合横坐标？
     *          2.2 目前遇到的难题在于 wps内置的散点图，只有一个横坐标，无法叠加在一起进行效果的显示
     *
     *          如果只是计算打印的话，感觉可以在select 部分进行打印即可，避免额外多一个方法，性能的消耗
     *
     */
    private void fitnessCalculations(){

        // 调用200个个体，计算每个个体的适应度值
        // 计算试卷的适应度值
        for (int i = 0; i < POPULATION_SIZE; i++) {

            log.info(i + "<=="+  paper_genetic[i] + "==>"+ sin1(paper_genetic[i]));

        }
    }


}



