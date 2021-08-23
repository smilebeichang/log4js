package cn.edu.sysu.niche;


import cn.edu.sysu.adi.TYPE;
import cn.edu.sysu.utils.JDBCUtils4;
import com.sun.istack.internal.NotNull;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.sql.SQLException;
import java.util.*;

import static java.util.Arrays.sort;


/**
 * @Author : song bei chang
 * @create 2021/8/20 22:01
 *
 *  《A Diverse Niche radii Niching Technique for Multimodal Function Optimization》
 *
 * FIXME 本周任务 复现自适应小生境的代码
 *
 *      初始化 -- 选择 -- 交叉 -- 变异 -- 修补
 *            |<----     niche      ---->|
 *
 *      1.自适应小生境
 *          能够找到峰，评价标准是平均数和标准差，日志打印画图
 *          1.1 哪些峰需要合并
 *              将群体中的个体分配到不同小生境中
 *              距离度量定义
 *              判断哪些峰需要合并(矩阵+凹点问题)  故基因型需要为2进制的多基因序列
 *          1.2 如何合并
 *              集合调整
 *              半径调整1
 *              个体剔除操作
 *              半径调整2
 *          1.3 什么时候合并
 *              调整初始半径
 *                  1)若候选小生境数连续p次等同于实际小生境数，则 R = R * r ;
 *                  2)若候选小生境数小于2，则 R = R  / r.
 *
 *      2.代入GA
 *          1和2的区别仅仅在于修补算子,故只要1出来了，2的话，应该很好解决
 *
 *
 */
public class DNDR {

    /**
     * 容器
     */
    private double[]  paper_genetic =new double[100];
    private int POPULATION_SIZE = 100;
    private int ITERATION_SIZE = 120;
    private Map<Double,Double> SIN_MAP = new HashMap<>(1000);
    private static ArrayList<String> bankList = new ArrayList();

    /**
     *  100套试卷 10道题
     */
    private  String[][] paperGenetic =new String[100][10];

    private JDBCUtils4 jdbcUtils = new JDBCUtils4();


    /**
     * 全局半径(初始化为0.1，后续可设置为0.05,0.1,0.2进行验证)
     */
    private  double radius = 0.1 ;

    /**
     *  全局变量
     */
    private double tmp1;
    private double tmp2;

    private  Logger log = Logger.getLogger(DNDR.class);

    /**
     *  1.稳态GA
     */
    @Test
    public void main() throws SQLException {

        // 初始化  这个初始化这么简单吗？单double类型，都已经不是数组了？
        // 为了适配一篇文献，直接采用[0,1],需要研究一下，以及改动回来
        init();
        //initSin();

        // 按照适应度排序
        sortByFitness(paperGenetic);


        // 迭代次数
        for (int i = 0; i < ITERATION_SIZE; i++) {
            // 计算各个极值点出现的次数
            //countFunc1();
            //countFunc3();

            // 选择
            selectionRts();

            for (int j = 0; j < POPULATION_SIZE - 1; j++) {

                // 交叉
                crossCover(j);

                // 变异
                mutate();


                // 限制性锦标赛选择 暂时不删除,因为需要使用其替换掉文献中的适应度共享算子
                // rts();

            }

            // 统计相似个体的数目
            if(i%20==0){
                //countCalculations(paper_genetic);
                //fitnessCalculations();
            }
        }
        System.out.println();

    }






    /**
     *
     * FIXME 需修改，因此算子中将小生境个数固定设置为了5
     * 选择嵌套局部最优
     *      搜素空间限定为局部峰值附近寻找，解的话，也使用附近的解去轮盘赌。
     *
     *
     *   小生境和局部最优的联系
     *      遗传算法种群中的小生境可以定义为搜索空间中最接近遗传算法当前最优集合中的任何一个元素的部分。
     *      遗传算法种群中各个小生境相互协作，形成对整个搜索空间的覆盖。
     *      rts是如何抵抗变化的：与最相似的解竞争，判断是否进入新种群。如果窗口足够大，那么总体的潜在概率分布在很长一段时间内不会发生变化。
     *
     *
     */
    private void selectionRts() {

        System.out.println("====================== selectionRts ======================");
        System.out.println("====================== 拆分数组 ======================");


        // 根据极值点的x轴进行计算，将附近的个体求出个体数，适应度值，适应度占比，轮盘赌
        // FIXME 疑问，轮盘赌的大小是多少？ 200/5=40？ 初始化多少就多少？可以的，具有一定的随机性
        // 将个体进行分组，后续应该可以提出去，调用一次即可
        // 计算试卷的适应度值

        // 个体、总和 加一层判断，防止map获取的值为null。 不能用hash 去重操作将导致数量不一致
        // 容器  后续考虑默认容量的初始值大小

        ArrayList<Double> list1 = new ArrayList<>();
        ArrayList<Double> list2 = new ArrayList<>();
        ArrayList<Double> list3 = new ArrayList<>();
        ArrayList<Double> list4 = new ArrayList<>();
        ArrayList<Double> list5 = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {

            //abs 获取key 判断key是否是属于某个极值点，若是，map.put(tmpKey, tempCount + 1)
            double tmpKey = numbCohesion(paper_genetic[i]);
            /*double k1 = Math.abs(0.1 - tmpKey);
            double k2 = Math.abs(0.3 - tmpKey);
            double k3 = Math.abs(0.5 - tmpKey);
            double k4 = Math.abs(0.7 - tmpKey);
            double k5 = Math.abs(0.9 - tmpKey);*/
            // 等距问题需要再次考虑
            double k1 = Math.abs(0.1 - tmpKey);
            double k2 = Math.abs(0.246 - tmpKey);
            double k3 = Math.abs(0.450 - tmpKey);
            double k4 = Math.abs(0.681 - tmpKey);
            double k5 = Math.abs(0.934 - tmpKey);

            // 等距
            /*double limitRange = 0.1 ;
            if(k1 < limitRange){
                list1.add(paper_genetic[i]);
            }
            if(k2 < limitRange){
                list2.add(paper_genetic[i]);
            }
            if(k3 < limitRange){
                list3.add(paper_genetic[i]);
            }
            if(k4 < limitRange){
                list4.add(paper_genetic[i]);
            }
            if(k5 < limitRange){
                list5.add(paper_genetic[i]);
            }*/

            // 不等距
            double limitRange1 = 0.1 ;  //0.2
            if(k1 < limitRange1){
                list1.add(paper_genetic[i]);
            }
            double limitRange2 = 0.046 ;  //0.092
            if(k2 < limitRange2){
                list2.add(paper_genetic[i]);
            }
            double limitRange3 = 0.158 ;  //0.316
            if(k3 < limitRange3){
                list3.add(paper_genetic[i]);
            }
            double limitRange4 = 0.08 ;  //0.16
            if(k4 < limitRange4){
                list4.add(paper_genetic[i]);
            }
            double limitRange5 = 0.066 ;  //0.13
            if(k5 < limitRange5){
                list5.add(paper_genetic[i]);
            }

        }


        System.out.println("============ 计算适应度过程  ===================");
        ArrayList<ArrayList<Double>> arrayList = new ArrayList<>(5);
        arrayList.add(list1);
        arrayList.add(list2);
        arrayList.add(list3);
        arrayList.add(list4);
        arrayList.add(list5);

        // 新个体容器
        double[] new_paper_genetic =new double[POPULATION_SIZE];
        int aggNum = 0;
        for (int j = 0; j < arrayList.size(); j++) {

            //轮盘赌 累加百分比  长度有待考量
            int arrLen = arrayList.get(j).size();
            System.out.println("长度:"+arrLen);
            double[] fitPie = new double[arrLen];

            //每套试卷的适应度占比
            double[] fitPro = getFitnessRts(arrayList.get(j));

            //累加初始值
            double accumulate = 0;

            //试卷占总试卷的适应度累加百分比
            for (int i = 0; i < arrLen; i++) {
                fitPie[i] = accumulate + fitPro[i];
                accumulate += fitPro[i];
            }

            //累加的概率为1 数组下标从0开始
            fitPie[arrLen-1] = 1;

            //初始化容器 随机生成的random概率值
            double[] randomId = new double[arrLen];

            //生成随机id
            for (int i = 0; i < arrLen; i++) {
                randomId[i] = Math.random();
            }

            // 排序
            sort(randomId);

            //轮盘赌 越大的适应度，其叠加时增长越快，即有更大的概率被选中
            int newSelectId = 0;
            for (int i = 0; i < arrLen ; i++) {
                while (newSelectId < arrLen && randomId[newSelectId] < fitPie[i]){
                    new_paper_genetic[aggNum]   = arrayList.get(j).get(i);
                    aggNum  += 1;
                    newSelectId += 1;
                }
            }
        }

        //重新赋值种群的编码  需考虑 顺序及个数问题
        paper_genetic=new_paper_genetic;


    }





    /**
     * 交叉
     *      完全按照老师的文档来，转换为代码，看看效果，争取实现满足效果 FIXME 什么时候开始变成了单点基因了？
     *      交叉变异: r表达式 rx1+(1-r)x2
     *
     *      不修改父代,且保留两个新个体
     *
     */
    public  void crossCover( int k ){

        System.out.println("=== crossCover begin ===");

        // 将变异系数变大,以及x1 x2 使用随机值
        double pc = 0.4;
        if (Math.random() < pc) {
            // 保留交叉后的两新个体，并提升为全局变量
            double lemuda = Math.random();
            tmp1 = (lemuda * paper_genetic[k]) + ((1 - lemuda) * paper_genetic[k + 1]);
            tmp2 = ((1 - lemuda) * paper_genetic[k]) + (lemuda * paper_genetic[k + 1]);
        }
    }


    /**
     * 变异
     *      将tmp1|tmp2变异和小生境剥离:
     *          变异使用 r表达式 rx1+(1-r)x2
     *          小生境作为修补后的评价因子
     */
    public  void mutate()  {

        System.out.println("=== mutate begin ===");

        double pm = 0.6;
        if (Math.random() < pm) {
            // 保留变异后的两新个体，并提升为全局变量 其实参数没必要过多纠结,其目的只是为了产生新个体
            double lemuda = Math.random();
            tmp1 = (lemuda * tmp1) + ((1 - lemuda) * Math.random());
            tmp2 = ((1 - lemuda) * tmp2) + (lemuda * Math.random());

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

        //double[] array = new double[paperGenetic.length];


        //map的key数字，value出现的次数
        HashMap<Double, Integer> map = new HashMap<>();
        for(int i = 0; i < paperGenetic.length; i++) {
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
     * 边界探索
     */
    @Test
    public  void rangeTest(){
        double v = sin1(1 - 0.02);
        System.out.println(v);

    }


    /**
     *  多峰函数1
     *  计算每一个极值点出现的次数    exceeds 99%
     *  人工计算终究不是回事，需要使用代码简化操作。如：abs小于0.02 则count+1
     *  判断是否同一对象,五条曲线:可以设置一个取值范围上下波动0.03  这样就可以获得每个极值点的个数
     *
     *  初代的个体为什么初始值不为0 (已解决，打印顺序导致的)
     */
    private void countFunc1(){

        log.info("测试 log4j");

        //map的key数字，value出现的次数  key 分为5类[0.1,0.3,0.5,0.7,0.9]
        HashMap<Double, Integer> map = new HashMap<>(5);
        int v1 = 0;
        int v2 = 0;
        int v3 = 0;
        int v4 = 0;
        int v5 = 0;

        for(int i = 0; i < POPULATION_SIZE; i++) {
            //获取key 判断key是否是属于某个极值点，若是，map.put(tmpKey, tempCount + 1)
            double tmpKey = numbCohesion(paper_genetic[i]);
            double k1 = Math.abs(0.1 - tmpKey);
            double k2 = Math.abs(0.3 - tmpKey);
            double k3 = Math.abs(0.5 - tmpKey);
            double k4 = Math.abs(0.7 - tmpKey);
            double k5 = Math.abs(0.9 - tmpKey);


            double limitRange = 0.02 ;
            if(k1 < limitRange){
                v1 = v1 +1 ;
            }
            if(k2 < limitRange){
                v2 = v2 +1 ;
            }
            if(k3 < limitRange){
                v3 = v3 +1 ;
            }
            if(k4 < limitRange){
                v4 = v4 +1 ;
            }
            if(k5 < limitRange){
                v5 = v5 +1 ;

            }

        }

        map.put(0.1, v1);
        map.put(0.3, v2);
        map.put(0.5, v3);
        map.put(0.7, v4);
        map.put(0.9, v5);

        //输出每个个体出现的次数
        for(Map.Entry<Double, Integer> entry : map.entrySet()) {
            double key = entry.getKey();
            Integer count = entry.getValue();
            log.info("极值点："+ key+"  次数："+count);
        }

    }

    /**
     * 选择的校验算子
     *      将迭代次数考虑进行，如50代不能超过50,100代不能超过100
     */
    private HashMap<Double, Integer>  countSelect1(double[] new_paper_genetic){


        //map的key数字，value出现的次数  key 分为5类[0.1,0.3,0.5,0.7,0.9]
        HashMap<Double, Integer> map = new HashMap<>(5);
        int v1 = 0;
        int v2 = 0;
        int v3 = 0;
        int v4 = 0;
        int v5 = 0;

        for(int i = 0; i < POPULATION_SIZE; i++) {
            //获取key 判断key是否是属于某个极值点，若是，map.put(tmpKey, tempCount + 1)
            double tmpKey = numbCohesion(new_paper_genetic[i]);
            double k1 = Math.abs(0.1 - tmpKey);
            double k2 = Math.abs(0.3 - tmpKey);
            double k3 = Math.abs(0.5 - tmpKey);
            double k4 = Math.abs(0.7 - tmpKey);
            double k5 = Math.abs(0.9 - tmpKey);


            double limitRange = 0.02 ;
            if(k1 < limitRange){
                v1 = v1 +1 ;
            }
            if(k2 < limitRange){
                v2 = v2 +1 ;
            }
            if(k3 < limitRange){
                v3 = v3 +1 ;
            }
            if(k4 < limitRange){
                v4 = v4 +1 ;
            }
            if(k5 < limitRange){
                v5 = v5 +1 ;

            }

        }

        map.put(0.1, v1);
        map.put(0.3, v2);
        map.put(0.5, v3);
        map.put(0.7, v4);
        map.put(0.9, v5);


        //找出map的value中最大的数字，即数组中数字出现最多的次数
        double maxKey = 0;
        int maxCount = 0;
        for(Map.Entry<Double, Integer> entry : map.entrySet()) {

            if(maxCount < entry.getValue()) {
                maxCount = entry.getValue();
                maxKey = entry.getKey();
            }
        }

        HashMap<Double, Integer> countResult = new HashMap<>(1);
        countResult.put(maxKey,maxCount);

        return countResult;
    }

    /**
     * 多峰函数3
     *      多样性方面和多峰函数1一样，也无法保证多样性的维持。更为重要的是，其收敛速度更快，甚至10带开始就已经50%
     */
    private void countFunc3(){

        log.info("测试 log4j");

        //map的key数字，value出现的次数  key 分为5类[0.1,0.246,0.450,0.681,0.934]
        HashMap<Double, Integer> map = new HashMap<>(5);
        int v1 = 0;
        int v2 = 0;
        int v3 = 0;
        int v4 = 0;
        int v5 = 0;

        for(int i = 0; i < POPULATION_SIZE; i++) {
            //获取key 判断key是否是属于某个极值点，若是，map.put(tmpKey, tempCount + 1)
            double tmpKey = numbCohesion(paper_genetic[i]);
            double k1 = Math.abs(0.1 - tmpKey);
            double k2 = Math.abs(0.246 - tmpKey);
            double k3 = Math.abs(0.450 - tmpKey);
            double k4 = Math.abs(0.681 - tmpKey);
            double k5 = Math.abs(0.934 - tmpKey);

            // 等距
            double limitRange = 0.02 ;
            if(k1 < limitRange){
                v1 = v1 +1 ;
            }
            if(k2 < limitRange){
                v2 = v2 +1 ;
            }
            if(k3 < limitRange){
                v3 = v3 +1 ;
            }
            if(k4 < limitRange){
                v4 = v4 +1 ;
            }
            if(k5 < limitRange){
                v5 = v5 +1 ;
            }



        }

        map.put(0.1, v1);
        map.put(0.246, v2);
        map.put(0.450, v3);
        map.put(0.681, v4);
        map.put(0.934, v5);

        //输出每个个体出现的次数
        for(Map.Entry<Double, Integer> entry : map.entrySet()) {
            double key = entry.getKey();
            Integer count = entry.getValue();
            log.info("极值点："+ key+"  次数："+count);
        }

    }




    /**
     * 随机生成初代种群
     *      100个体，每个个体10个基因  多基因
     *
     */
    private void  init() throws SQLException {

        System.out.println("====== 开始选题,构成试卷  轮盘赌构造  ======");

        // 试卷|个体大小  提供遗传变异的基本单位
        int  paperNum = paperGenetic.length;

        // 试题|基因大小
        int questionsNum = 10 ;

        // 单套试卷的集合
        HashSet<String> itemSet = new HashSet<>();

        // 题库310道题  50:100:100:50:10   硬性约束：长度  软性约束：题型，属性比例
        // 获取题库所有试题  [8:CHOSE:(1,0,0,0,0),....]
        bankList = getBank();

        for (int j = 0; j < paperNum; j++) {

            //清空上一次迭代的数据
            itemSet.clear();

            for (int i = 0; i < questionsNum; i++) {
                //减少频繁的gc
                String item ;
                //去重操作
                while (itemSet.size() == i) {
                    //获取试题id   轮盘赌构造
                    int sqlId = roulette(itemSet);

                    item = jdbcUtils.selectOneItem(sqlId+1);
                    itemSet.add(item);
                }
            }

            // 将hashSet转ArrayList 并排序
            ArrayList<String> list = new ArrayList<>(itemSet);

            // idList容器
            ArrayList<Integer> idList = new ArrayList<>();
            for (int i = 0; i <list.size() ; i++) {
                idList.add(Integer.valueOf(list.get(i).split(":")[0]));
            }

            //list  排序  目前这套试卷抽取到的试题id
            Collections.sort(idList);


            // 根据id从数据库中查询相对应的试题
            String ids = idList.toString().substring(1,idList.toString().length()-1);
            ArrayList<String> bachItemList = jdbcUtils.selectBachItem(ids);


            // 交叉变异的对象是 试题   即试卷=个体  试题=基因
            String[] itemArray = new String[bachItemList.size()];
            for (int i = 0; i < bachItemList.size(); i++) {
                itemArray[i] = bachItemList.get(i);
            }
            // 赋值  把题库提升为全局变量，方便整体调用 容器：二维数组
            paperGenetic[j] = itemArray;

        }


    }


    /**
     * 初始化计算
     */
    private void  initSin() {

        System.out.println("=== init  SIN_MAP ===");
        double step = 0.001;
        double end1 = 1000 ;
        for (double i = 0.000; i < end1; i=i+step) {
            SIN_MAP.put(numbCohesion(i),sin1(i));
        }
        /*double end3 = 1;
        for (double i = 0.000; i < end3; i=i+step) {
            SIN_MAP.put(numbCohesion(i),sin2(i));
        }*/
    }





    /**
     * 每套试卷的适应度占比
     *          使用多峰函数计算计算基因的适应度值
     *
     */
    private double[] getFitness(){

        //log.info("适应值 log4j")

        // 所有试卷的适应度总和
        double fitSum = 0.0;

        // 每套试卷的适应度值
        double[] fitTmp = new double[POPULATION_SIZE];

        // 每套试卷的适应度占比
        double[] fitPro = new double[POPULATION_SIZE];


        // 计算试卷的适应度值
        for (int i = 0; i < POPULATION_SIZE; i++) {

            // 个体、总和 加一层判断，防止map获取的值为null。
            fitTmp[i] = SIN_MAP.get(paper_genetic[i]) ;
            fitSum = fitSum + fitTmp[i] ;

        }

        // 各自的比例
        for (int i = 0; i < POPULATION_SIZE; i++) {
            fitPro[i] = fitTmp[i] / fitSum;
        }

        return  fitPro;
    }


    private double[] getFitnessRts(@NotNull  ArrayList<Double> array){


        int arrLen = array.size();

        // 所有试卷的适应度总和
        double fitSum = 0.0;

        // 每套试卷的适应度值
        double[] fitTmp = new double[arrLen];

        // 每套试卷的适应度占比
        double[] fitPro = new double[arrLen];


        // 计算试卷的适应度值
        for (int i = 0; i < arrLen; i++) {

            // 个体、总和 加一层判断，防止map获取的值为null。
            fitTmp[i] = SIN_MAP.get(array.get(i)) ;
            fitSum = fitSum + fitTmp[i] ;

        }

        // 各自的比例
        for (int i = 0; i < arrLen; i++) {
            fitPro[i] = fitTmp[i] / fitSum;
        }

        return  fitPro;
    }


    /**
     * 实现多峰函数 f(x) = e-2*ln(2)*((x-0.1)/0.8)2 * sin6(5π(x3/4-0.05))
     *
     */
    private double sin2(double i){

        //for (double i = 0; i < 1; i=i+0.001) {


            //自然常数e的近似值
            double e = Math.E;

            //e次方数
            double y = -2 * Math.log(2) * (Math.pow((i-0.1)/0.8,2));

            //输出结果
            double d = Math.pow(e, y);

            double degrees = 5 * 180 * (Math.pow(i,0.75)-0.05);
            //将角度转换为弧度
            double radians = Math.toRadians(degrees);

            System.out.format("%f 为 %.10f%n", i,  d * Math.pow(Math.sin(radians), 6));

            return d * Math.pow(Math.sin(radians), 6);

        //}


    }





    /**
     * 限制性选择小生境
     *
     */
    public void  rts()  {

        // 父代 c1 c2  限制拥挤小生境算法  为c1从当前种群中随机选取c*w个体  5个小生境  4*5元锦标赛
        ArrayList<Map<Integer, Double>[]> cList = championship();


        // 替补解
        ArrayList<Double> cwList = deterministicCrowding();


        // 替换 or 保留
        // cList: 原始父代，  cwList:新个体   原始父代 和 交叉变异后的个体进行比较操作
        closestResembleRts(cList, cwList);

    }


    /**
     * 如果f(c1)>f(d1),则用c1替换d1,否则保留d1;
     * 如果f(c2)>f(d2),则用c2替换d2,否则保留d2;
     *
     *
     *      替换基因型相似的个体，能够维持多样性 ，多峰且各个峰值相等
     *
     *      表现型  适应度值，或者 minAdi
     *      基因型  解(2,3,56,24,4,6,89,98,200,23)
     *
     */
    private void closestResembleRts(ArrayList<Map<Integer, Double>[]> cList, ArrayList<Double> cwList) {

        Map<Integer, Double>[] c11 = cList.get(0);
        Map<Integer, Double>[] c12 = cList.get(1);

        double cw21 = cwList.get(0);
        double cw22 = cwList.get(1);

        // 选取基因型做相似性校验
        similarGene(c11, cw21);
        similarGene(c12, cw22);

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

        // 使用基因型判断相似性
        double c1 = numbCohesion(cList.get(0));
        double c2 = numbCohesion(cList.get(1));

        double cw1 = numbCohesion(cwList.get(0));
        double cw2 = numbCohesion(cwList.get(1));

        double dc1 = SIN_MAP.get(c1);
        double dc2 = SIN_MAP.get(c2);
        double dcw1 = SIN_MAP.get(cw1);
        double dcw2 = SIN_MAP.get(cw2);

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
     * 在c1中寻找cw1的近似解  5个小生境  4*5元锦标赛  c1是c*w套试卷  cw1是1套试卷
     * 根据基因型来找出最相似的值
     *      这边无法使用redis 进行缓存，需实时的计算abs()
     *
     */
    private void similarGene(Map<Integer, Double>[] c1, double cw1) {

        double min = 9999;
        int minPhen = 0;

        cw1 = numbCohesion(cw1);

        // 外层C小生境数，内层W元锦标赛
        for (Map<Integer, Double> aC11 : c1) {

            double c2;
            // 遍历map
            for (int j = 0; j < aC11.size(); j++) {
                for (Object o : aC11.keySet()) {
                    int key = (int) o;
                    c2 = aC11.get(key);

                    // 获取最相似的解  相似的判定标准：基因型
                    double sameNum = compareArrSameRts(c2,cw1);
                    if (min > sameNum) {
                        min = sameNum;
                        minPhen = key;
                    }
                }
            }
        }

        System.out.println("相似的差值："+min +"  最相似的个体索引："+minPhen );
        // 其出现的原因是因为,父代到了后期，集中于波峰，但新解可能出现在波谷
        if (min > 0.4){
            System.out.println("相似的差值："+min +"  最相似的个体索引："+minPhen);
        }

        // 替换c1   个体 vs 最相似个体 适应度
        double fitc1 = SIN_MAP.get(cw1) ;
        double fitc2 = SIN_MAP.get(paper_genetic[minPhen]) ;

        if (fitc1 > fitc2){
            paper_genetic[minPhen] = cw1;
        }

    }



    /**
     *  相似性使用 基因型进行比较
     *
     *      会是这里导致 速度变慢的吗？
     */
    private double compareArrSameRts(double arr, double arr2) {

        //return  Math.abs(sin1(arr) - sin1(arr2));
        return  Math.abs(arr - arr2);
    }





    /**
     *  分别为c1从当前种群中随机选取c*w个体
     *  当前种群和题库的关系
     *
     *  是否是20元锦标赛过大，待后续优化
     *  Map<Integer, double[]>  key是paperGenetic的索引，value是基因型
     *         可以使用redis进行缓存，优化计算时间。或者使用一个超级大的map。存在内存中但不知道ieda是否会oom
     *
     */
    private ArrayList<Map<Integer, Double>[]> championship()  {

        // 5个小生境  4*5元锦标赛  需进一步验证  窗口大小的具体含义
        int num = 5 ;
        int window = 1 * 5 ;
        int size = 2 ;


        // 基本单位:试卷。故随机生成一个下标即可 (需保存下标,方便后续替换 map(k,v))
        // 数组裹map
        ArrayList<Map<Integer, Double>[]> cwList = new ArrayList<>(2);
        for (int w = 0; w < size ; w++) {
            Map<Integer, Double>[] cwList1 = new HashMap[num];
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
            cwList.add(cwList1);
        }


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
    private Double formatDouble(double x1){

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

        //System.out.format("%f 为 %.10f%n", avgnum,  Math.pow(Math.sin(radians), 6));
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

        // 调用100个个体，计算每个个体的适应度值
        // 计算试卷的适应度值
        for (int i = 0; i < POPULATION_SIZE; i++) {

            log.info(i + "<=="+  paper_genetic[i] + "==>"+ sin1(numbCohesion(paper_genetic[i])));

        }
    }


    /**
     * 返回题库所有试题 id:type:pattern
     *
     */
    private ArrayList<String> getBank() throws SQLException {

        return jdbcUtils.select();

    }


    /**
     *   逻辑判断 ：轮盘赌构造选题
     *        1.id不能重复
     *        2.题型|属性比例 影响适应度
     *
     */
    private int roulette(HashSet<String> itemSet)  {


        //轮盘赌 累加百分比
        double[] fitPie = new double[bankList.size()];

        //计算每道试题的适应度占比   1*0.5*0.8
        double[] fitnessArray = getRouletteFitness(itemSet);

        //id去重操作
        HashSet<Integer> idSet = new HashSet<>();

        //迭代器遍历HashSet
        Iterator<String> it = itemSet.iterator();
        while(it.hasNext()) {
            idSet.add(Integer.valueOf(it.next().split(":")[0]));
        }

        //累加初始值
        double accumulate = 0;

        //试题占总试题的适应度累加百分比
        for (int i = 0; i < bankList.size(); i++) {
            fitPie[i] = accumulate + fitnessArray[i];
            accumulate += fitnessArray[i];
        }

        //累加的概率为1   数组下标从0开始
        fitPie[310-1] = 1;

        //随机生成的random概率值  [0,1)
        double randomProbability = Math.random();

        //轮盘赌 越大的适应度，其叠加时增长越快，即有更大的概率被选中
        int answerSelectId = 0;
        int i = 0;

        while (i < bankList.size() && randomProbability > fitPie[i]){
            //id 去重
            if(idSet.contains(i)){
                i += 1;
            }else{
                i ++;
                answerSelectId   = i;
            }
        }

        return answerSelectId;

    }


    /**
     * 1.根据已选试题，计算题库中每道试题的适应度值比例
     * 2.每道题的概率为 1*penalty^n,总概率为310道题叠加
     *      2.1  初始化的时候将全局的310题查询出来（id:type:pattern）
     *      2.2  求出每道试题的概率 1 * 惩罚系数
     *      2.3  求出每道试题的适应度占比
     *  题型比例 选择[0.2,0.4]   填空[0.2,0.4]  简答[0.1,0.3] 应用[0.1,0.3]
     *  属性比例 第1属性[0.2,0.4]第2属性[0.2,0.4] 第3属性[0.1,0.3] 第4属性[0.1,0.3] 第5属性[0.1,0.3]
     *
     */
    private double[] getRouletteFitness(HashSet<String> itemSet)  {

        // 所有试题的适应度总和
        double fitSum = 0.0;

        // 每道试题的适应度值
        double[] fitTmp = new double[bankList.size()];

        // 每道试题的适应度占比   疑问:1/310 会很小,random() 这样产生的值是否符合要求
        double[] fitPro = new double[bankList.size()];

        // 数据库属性排列的规则,因为基数大,导致随机选取的题目不具有代表性
        // 解决方案:题目顺序打乱（修改数据库的属性排序）

        //题型个数
        int typeChose  = 0;
        int typeFill   = 0;
        int typeShort  = 0;
        int typeCompre = 0;


        //此次迭代各个题型的数目
        for (String s:itemSet) {
            //System.out.println(s);

            //计算每种题型个数
            if(TYPE.CHOSE.toString().equals(s.split(":")[1])){
                typeChose += 1;
            }
            if(TYPE.FILL.toString().equals(s.split(":")[1])){
                typeFill += 1;
            }
            if(TYPE.SHORT.toString().equals(s.split(":")[1])){
                typeShort += 1;
            }
            if(TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])){
                typeCompre += 1;
            }
        }

        //题型比例
        double typeChoseRation  =  typeChose/10.0;
        double typeFileRation   =  typeFill/10.0;
        double typeShortRation  =  typeShort/10.0;
        double typeCompreRation =  typeCompre/10.0;


        //属性个数
        int attributeNum1  = 0;
        int attributeNum2  = 0;
        int attributeNum3  = 0;
        int attributeNum4  = 0;
        int attributeNum5  = 0;

        //此次迭代各个属性的数目
        for (String s:itemSet) {
            //System.out.println(s);

            //计算每种题型个数
            if("1".equals(s.split(":")[2].substring(1,2))){
                attributeNum1 += 1;
            }
            if("1".equals(s.split(":")[2].substring(3,4))){
                attributeNum2 += 1;
            }
            if("1".equals(s.split(":")[2].substring(5,6))){
                attributeNum3 += 1;
            }
            if("1".equals(s.split(":")[2].substring(7,8))){
                attributeNum4 += 1;
            }
            if("1".equals(s.split(":")[2].substring(9,10))){
                attributeNum5 += 1;
            }
        }
        //System.out.println("ar1: "+attributeNum1+"\tar2: "+attributeNum2+"\tar3: "+attributeNum3+"\tar4: "+attributeNum4+"\tar5: "+attributeNum5);

        //属性比例
        double attributeRatio1 = attributeNum1/23.0;
        double attributeRatio2 = attributeNum2/23.0;
        double attributeRatio3 = attributeNum3/23.0;
        double attributeRatio4 = attributeNum4/23.0;
        double attributeRatio5 = attributeNum5/23.0;



        // 题型和属性比例 和轮盘赌搭建关系：
        //      已抽取的属性个数越多，则惩罚系数越大 且各个属性是累乘关系
        //      比例和一个固定值做比较即可  eg: typeChose/10    AttributeRatio1/23
        //      如果未超出比例，则按照正常流程走，一旦超过，则适应度值急剧下降
        for (int j = 0; j < bankList.size(); j++) {
            double penalty = 1;
            String[] splits = bankList.get(j).split(":");

            // 题型比例
            // 为什么小于也要做惩罚，因为选取了一次，需要实时统计比例信息，并获取惩罚系数
            if(splits[1].contains(TYPE.CHOSE+"")){
                if(typeChoseRation<0.4){
                    penalty = penalty * Math.pow(0.5,typeChose);
                }else {
                    penalty = penalty * Math.pow(0.5,typeChose*typeChose);
                }
            }
            if(splits[1].contains(TYPE.FILL+"")){
                if(typeFileRation<0.4){
                    penalty = penalty * Math.pow(0.5,typeFill);
                }else {
                    penalty = penalty * Math.pow(0.5,typeFill*typeFill);
                }
            }
            if(splits[1].contains(TYPE.SHORT+"")){
                if(typeShortRation<0.3){
                    penalty = penalty * Math.pow(0.5,typeShort);
                }else {
                    penalty = penalty * Math.pow(0.5,typeShort*typeShort);
                }
            }
            if(splits[1].contains(TYPE.COMPREHENSIVE+"")){
                if(typeCompreRation<0.3){
                    penalty = penalty * Math.pow(0.5,typeCompre);
                }else {
                    penalty = penalty * Math.pow(0.5,typeCompre*typeCompre);
                }
            }

            //属性比例
            if("1".equals(splits[2].substring(1,2))){
                if(attributeRatio1<0.4){
                    penalty = penalty * Math.pow(0.8,attributeNum1);
                }else {
                    penalty = penalty * Math.pow(0.8,attributeNum1*attributeNum1);
                }
            }
            if("1".equals(splits[2].substring(3,4))){
                if(attributeRatio2<0.4){
                    penalty = penalty * Math.pow(0.8,attributeNum2);
                }else {
                    penalty = penalty * Math.pow(0.8,attributeNum2*attributeNum2);
                }
            }
            if("1".equals(splits[2].substring(5,6))){
                if(attributeRatio3<0.3){
                    penalty = penalty * Math.pow(0.8,attributeNum3);
                }else {
                    penalty = penalty * Math.pow(0.8,attributeNum3*attributeNum3);
                }
            }
            if("1".equals(splits[2].substring(7,8))){
                if(attributeRatio4<0.3){
                    penalty = penalty * Math.pow(0.8,attributeNum4);
                }else {
                    penalty = penalty * Math.pow(0.8,attributeNum4*attributeNum4);
                }
            }
            if("1".equals(splits[2].substring(9,10))){
                if(attributeRatio5<0.3){
                    penalty = penalty * Math.pow(0.8,attributeNum5);
                }else {
                    penalty = penalty * Math.pow(0.8,attributeNum5*attributeNum5);
                }
            }


            //个体值 和 总和
            fitTmp[j] = penalty ;
            fitSum = fitSum + penalty ;

        }

        // 计算出每道试题的各自比例
        for (int i = 0; i < bankList.size(); i++) {
            fitPro[i] = fitTmp[i] / fitSum;
        }

        //返回类型为 double[] 转 ArrayList<Double>  可以省略
        //ArrayList<Double> arrayList = new ArrayList<>(fitPro.length);
        //for (double anArr : fitPro) {
        //    arrayList.add(anArr);
        //}
        //System.out.println(arrayList.toString());


        return  fitPro;
    }


    /**
     * 根据适应度值将原始个体进行排序
     *
     */
    public void sortByFitness(String[][] paperGenetic){

        String[][] tmpPaperGenetic = new String[100][10];



        for (int i = 0; i < paperGenetic.length; i++) {
            //需要将个体的基因String转换为double
            sin1(numbCohesion(geneToDouble(paperGenetic[i])));
        }


    }

    /**
     * 个体基因长什么样,(24,CHOSE,(0,0,0,1,0))....*10
     * 需要转换成什么样，需要计算出个体的适应度值
     * 如何计算,使用惩罚因子计算适应度值  1*penalty^n
     * 每道题的默认值适应度均为1
     *
     *
     * 原始根本就没使用0~1的适应度值,直接使用惩罚系数*1，然后轮盘赌选择下一代个体
     * 目前需要使用0~1的适应度值吗？
     *
     * 现象：
     * 适应度值是纵坐标,0~1是横坐标,需要0~1的横坐标来获取适配多峰函数
     * ①最原始的版本,的确不需要横坐标
     * ②上一个版本,使用的是横坐标单基因型
     * ③这个版本,需要横坐标多基因型才能满足凹点的概念
     *
     * 解决：
     * ①初始化重新设计，生成0,1的多基因组
     *    之前是不是遇到过这个问题啊，
     *    单基因类型，所以使用了一个ram函数，解决交叉问题。
     *    单基因求适应度是使用多峰函数1，map映射
     *
     * ②将原始版本的基因型转化为0~1的横坐标，这个可能不是很好转，需要多个基因片段，如果是1*Pen，得到的将呈现正态分布,不符合要求
     *   等下周见老师询问一下建议
     *
     *  多个基因片段为0,1,最终取平均值，的确会导致正态分布，无法均匀分布，先看看是否有解决方案，如果没有则先忽略，后期再说。
     *
     *  先搞两个单基因，试试。(0.1,0.8) = 0.45
     *  初始化的时候，的确可以直接使用if,使其均匀分布，
     *
     * 今晚任务:
     *  1. 直接套用单点基因，然后做一个交叉解决凹点问题
     *
     *
     *
     *
     */
    private Double geneToDouble(String[] v) {


        System.out.println(v);
        //getRouletteFitness();
        return 0.1;
    }

}



