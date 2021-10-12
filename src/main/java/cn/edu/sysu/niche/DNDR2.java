package cn.edu.sysu.niche;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.*;

/**
 * @Author : song bei chang
 * @create 2021/8/20 22:01
 *
 *
 *  《A Diverse Niche radii Niching Technique for Multimodal Function Optimization》
 *
 * FIXME 本周任务 复现自适应小生境的代码
 *
 *      初始化 -- 选择 -- 交叉 -- 变异 -- 修补
 *           |<----     niche      ---->|
 *
 *      1.自适应小生境
 *          能够找到峰，评价标准是平均数和标准差，日志打印画图
 *          1.1 哪些峰需要合并
 *              将群体中的个体分配到不同小生境中
 *              距离度量定义
 *              判断哪些峰需要合并(矩阵+凹点问题)
 *
 *          1.2 如何合并
 *              集合调整
 *              半径调整1
 *              半径调整2
 *              个体剔除操作
 *
 *          1.3 什么时候合并
 *              调整初始半径
 *                  1)若候选小生境数连续p次等同于实际小生境数，则 R = R * r ;
 *                  2)若候选小生境数小于2，则 R = R  / r.
 *
 *
 * FIXME 下周任务:
 *       1.代入GA 继续完成(难点:基因型转换)
 *       2.修补算子
 *
 */
public class DNDR2 {


    /**
     * 控制是否进行半径的修改
     */
    private static final int CONTINUE_TIMES = 5;
    private static final int NUMBER_OF_NICHES = 1;

    private int ct = 0;
    private int nn = 0;

    /**
     * 容器
     */
    private double[]  paper_genetic =new double[200];
    private int POPULATION_SIZE = 200;
    private Map<Double,Double> SIN_MAP = new HashMap<>(1000);

    /**
     * 排序后的list 1.0_0.3
     */
    private ArrayList<String> sortList = new ArrayList<>(200);

    /**
     * 全局半径(初始化为0.1，后续可设置为0.05,0.1,0.2进行验证)
     */
    private  double radius = 0.2 ;


    /**
     *  key是map存小生境leader, value是list存小生境member
     */
    private HashMap<String,ArrayList<String>> mapArrayList = new HashMap<>();

    /**
     * set存leader
     */
    private HashSet<String> leaderSet = new HashSet();

    private Logger log = Logger.getLogger(DNDR2.class);


    /**
     *  主程序
     */
    @Test
    public void main() {

        // 百万级别的循环耗时1s
        initSin();

        for (int j = 0; j < 120; j++) {
            mapArrayList.clear();
            leaderSet.clear();

            // 初始化
            init();


            // 迭代次数
            for (int i = 0; i < 1; i++) {

                // 适应度值排序
                sortFitness();
                // 将群体中的个体分配到不同小生境中
                distributeNiche();
                // 判断哪些峰需要合并(矩阵+凹点问题)
                HashSet<String> hs = judgeMerge();
                // 合并
                merge(hs);
                // 调整初始半径
                //adjustRadius();

                log.info("小生境个数: "+ mapArrayList.size());

            }

        }
    }


    /**
     * 调整初始半径
     *      1)若候选小生境数连续p次等同于实际小生境数，则 R = R * r ;
     *      2)若候选小生境数小于2，则 R = R  / r.
     */
    private void adjustRadius() {

        // 缩小初始半径
        if(CONTINUE_TIMES == ct ){
            radius = radius * Math.random();
            ct = 0;
        }

        // 扩大初始半径
        if (NUMBER_OF_NICHES == mapArrayList.size()){
            radius = radius / Math.random();
        }

    }


    /**
     * 随机生成初代种群
     *      200个体  单基因
     *      优化方案：初始化一次，存入redis中,精度保证0.001.这样可以方便后续的计算
     *
     *      个体的越多,越具有代表性
     */
    private void  init() {

        System.out.println("=== init POPULATION_SIZE ===");
        for (int i = 0; i < POPULATION_SIZE; i++) {
            paper_genetic[i] = numbCohesion(Math.random());
        }

    }


    /**
     * 初始化计算
     *      1000/0.001= 100W的数据
     */
    private void  initSin() {

        System.out.println("=== init SIN_MAP ===");
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
     * 格式转换工具, 保留小数点后三位
     */
    public Double numbCohesion(Double adi){

        return Double.valueOf(String.format("%.3f", adi));

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
     * 适应度排序
     *      1.排序的目的是为了找出峰值
     *      2.按照适应度降序,希望能记住其原有的key,后续需使用key进行分组
     *      3.map ×  list √
     *        list使用 (value_key) ,使用double进行比较解决乱序问题
     *
     *  最终结果:
     *      1.0_0.3
     *      0.9992600231455838_0.101
     *      ......
     *
     */
    private void sortFitness() {

        // 数据清空
        sortList.clear();

        // 数组转list value_key
        for (double v : paper_genetic) {
            sortList.add(SIN_MAP.get(v)+"_"+v);
        }

        Comparator comp = new MyComparator();
        // empty String
        Collections.sort(sortList,comp);

    }



    /**
     * 将群体中的个体分配到不同小生境中 1.0_0.3
     *
     *  后续步骤：
     *      1.选取leader
     *           最后有些个体其实不应该成为leader,待后续进行合并和剔除操作
     *      2.选取member
     *           保证数据总数不变
     *            ①每选完一个leader之后立即进行选取，可以避免无效的迭代                    未采用
     *            ②选完全部leader之后统一进行，可以避免ConcurrentModificationException  √
     *
     *           现象：存一个点分布在两个小生境中   0.4 分别在0.301和0.0407中
     *           解决方案：每次判断完后，对这个个体标记已经清除
     */
    private void distributeNiche() {

        // 选取leader
        for (int i = 0; i < sortList.size(); i++) {
            if (leaderSet.size()==0){
                leaderSet.add(sortList.get(i));
                System.out.println("leader: "+sortList.get(i));
            }else{
                double a  = Double.valueOf(sortList.get(i).split("_")[1]);
                // 需要使用一个计数器，进行判断是否符合全部要求
                int counter = 0;
                for (String leader : leaderSet) {
                    // b 的判断应该满足全部的leader
                    double b  = Double.valueOf(leader.split("_")[1]);
                    if(Math.abs(a-b)<radius ){
                        counter = counter + 1;
                    }
                }
                // 如果不隶属于任何一个小生境
                if(counter < 1){
                    leaderSet.add(sortList.get(i));
                    System.out.println("leader: "+sortList.get(i));
                }
            }
        }

        // 选取member
        int sum = 0;
        for (String leader : leaderSet) {
            ArrayList<String> memberList = new ArrayList<>();
            double b  = Double.valueOf(leader.split("_")[1]);
            for (int i=0;i<sortList.size();i++) {
                String s = sortList.get(i);
                if (!StringUtils.isBlank(s)){
                    double a  = Double.valueOf(s.split("_")[1]);
                    if(Math.abs(a-b)<radius){
                            memberList.add(s);
                            // java.lang.NumberFormatException: empty String
                            sortList.set(i, "");
                    }
                }
            }
            mapArrayList.put(leader, memberList);
            sum = memberList.size() + sum;
        }

        System.out.println("此次迭代个体总数目："+sum);
    }

    /**
     * 判断哪些峰需要合并(矩阵+凹点问题) mapArrayList leaderSet
     *    用距离公式来表示任意两个小生境之间的关系,得出一个距离关系w矩阵,因为是矩阵,所以后续需进一步去重
     *
     *    使用什么来保存矩阵关系呢？ 一个动态大小的二维数组
     *
     *    优化: 将距离关系矩阵,进一步简化,只将要合并的集合返回即可,可能是一个或多个
     *
     */
    private HashSet<String> judgeMerge() {

        // 距离关系w矩阵
        int[][] distanceMatrix =new int[leaderSet.size()][leaderSet.size()];

        // set 转 arrayList
        List<String> leaderList = new ArrayList<>(leaderSet);

        // 遍历获取距离关系,并生成矩阵
        for (int i=0;i < leaderList.size(); i++) {

            double min = 9999; int a=0; int b=0;

            Double aDouble = Double.valueOf(leaderList.get(i).split("_")[1]);

            for (int j=0;j < leaderList.size(); j++) {

                if (!leaderList.get(i).equals(leaderList.get(j))){
                    // 将距离存在一个集合之中，然后选取最小值(0,1)
                    double distance = Math.abs(aDouble - Double.valueOf(leaderList.get(j).split("_")[1]));
                    // 取出最小值
                    if (min > distance){
                       a = i; b = j; min = distance;
                    }
                }
            }


            // 随机选取5个点进行凹点验证: 为了解决独立因子
            // 多个随机值当中,只要存在凹点,证明这两个相邻的小生境是独立的,不需要合并
            // 为了消除噪音干扰,定义了一个忍受因子 sf=0.9
            Double bDouble = Double.valueOf(leaderList.get(b).split("_")[1]);
            double sf = 0.9;

            // 本轮循环中未出现凹点,则需要合并
            boolean flag = true ;
            final int GER_RND = 5;
            for (int j = 0; j < GER_RND; j++) {
                double lemuda = Math.random();
                double cDouble = (lemuda * aDouble) + ((1 - lemuda) * bDouble);
                // 计算适应度值,并进行比较
                Double aFitness = SIN_MAP.get(numbCohesion(aDouble));
                Double bFitness = SIN_MAP.get(numbCohesion(bDouble));
                Double cFitness = SIN_MAP.get(numbCohesion(cDouble));
                //System.out.println(cFitness +","+sf*aFitness+","+sf*bFitness)

                // 存在凹点
                if (cFitness<sf*aFitness && cFitness<sf*bFitness){
                    flag = false;
                }
            }

            // 需要合并的最终因子,将ab有值时,赋值为1,其余赋值为0
            if(flag){
                distanceMatrix[a][b]=1;
            }
        }

        // 打印 遍历二维数组
        for (int i1 = 0; i1 < distanceMatrix.length; i1++) {
            for (int i2 = 0; i2 < distanceMatrix[i1].length; i2++) {
                System.out.print(distanceMatrix[i1][i2]+" , ");
            }
            System.out.println();
        }

        /**
         * 去重的逻辑很简单，不取整个矩阵，只要根据对角线划分即可  not ok
         * 0 , 0 , 0 , 0 , 1 , 0 ,
         * 0 , 0 , 0 , 0 , 0 , 0 ,
         * 0 , 0 , 0 , 0 , 0 , 0 ,
         * 0 , 0 , 0 , 0 , 0 , 1 ,
         * 1 , 0 , 0 , 0 , 0 , 0 ,
         * 0 , 0 , 0 , 0 , 0 , 1 ,
         * 0 ,
         * 0 , 0 ,
         * 0 , 0 , 0 ,
         * 0 , 0 , 0 , 0 ,
         * 1 , 0 , 0 , 0 , 0 ,
         * 0 , 0 , 0 , 0 , 0 , 1 ,
         */

        // 获取将要合并的两个峰的下标
        HashSet<String> hs = new HashSet<>();
        for (int i1 = 0; i1 < distanceMatrix.length; i1++) {
            for (int i2 = 0; i2 < distanceMatrix.length; i2++) {
                // 通过对ab排序,将小数字放在前面,然后使用set解决数据重复问题
                // 需进一步考虑 [0.798_0.9, 0.696_0.798] 即 0,2 和 2,5 待进一步考量
                if(distanceMatrix[i1][i2] == 1){
                    if(i1<i2){
                        hs.add(leaderList.get(i1)+":"+leaderList.get(i2));
                    }else {
                        hs.add(leaderList.get(i2)+":"+leaderList.get(i1));
                    }
                }
            }
        }
        System.out.println("验证去重效果 hs.size(): "+hs.size()+" hs.toString(): "+hs.toString());
        return hs;
    }





    /**
     * 合并
     *    集合成员调整: 取大leader做总leader,List集合进行合并
     *    半径调整1:   选择大leader和小集群的最远值作为半径
     *    个体剔除操作+半径调整2: 理论基础:小生境中适应度最低的个体 = 离领导个体最远的个体
     *          目的：调整的是各个小生境的半径,剔除不需要的个体以及获取峰半径,便于随后的交叉变异,在某个具体的小生境范围内进行GA
     *
     */
    private void merge(HashSet<String> hs) {

        // 集合是否还需进一步去重,待考量
        for (String h : hs) {
            // leader
            String s1 = h.split(":")[0];
            String s2 = h.split(":")[1];
            double l1 = Double.valueOf(s1.split("_")[1]);
            double l2 = Double.valueOf(s2.split("_")[1]);
            if(l1>l2){
                // 集合成员调整
                mapArrayList.get(s1).addAll(mapArrayList.get(s2));
                // 半径调整1
                radiusMerge(s1,s2,l1);
            }else {
                // 集合成员调整
                mapArrayList.get(s2).addAll(mapArrayList.get(s1));
                // 半径调整1
                radiusMerge(s2,s1,l2);
            }
        }


        // 个体剔除操作 + 径调整2
        // 通过hs进行过滤,保留最新的mapArrayList集合
        System.out.println("过滤前:");
        Iterator iter = mapArrayList.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object key = entry.getKey();
            Object val = entry.getValue();
            System.out.println("key: "+key +"  val："+ val);
        }

        for (String h : hs) {
            mapArrayList.remove(h.split(":")[0]);
            mapArrayList.remove(h.split(":")[1]);
        }

        System.out.println("过滤后:");
        // HashMap遍历，此种方式效率高
        Iterator iter2 = mapArrayList.entrySet().iterator();
        while (iter2.hasNext()) {
            Map.Entry entry = (Map.Entry) iter2.next();
            Object key = entry.getKey();
            Object val = entry.getValue();
            System.out.println("key: "+key +"  val："+ val);
        }

        // 个体剔除操作 + 半径调整2
        // 用于存储临时集合
        ArrayList<String> sideList = new ArrayList<>();

        Iterator iter3 = mapArrayList.entrySet().iterator();
        while (iter3.hasNext()) {
            Map.Entry entry = (Map.Entry) iter3.next();
            String key = (String) entry.getKey();
            Object val = entry.getValue();
            // 过滤出样式为value_key_radius的个体,只有这一部分个体半径需要做修改
            if(key.split("_").length == 3){
                // 用于存储集合
                ArrayList<String> arrayList = new ArrayList<>();
                // 进行剔除操作
                // val：[0.9992600231455838_0.099, 0.9970430120324887_0.098,....]
                String subStr = val.toString().substring(1, val.toString().length() - 1);
                String[] splitArr = subStr.split(",");
                // 寻找适应度值最低的个体 和 寻找适应度最高的个体
                double lowerFitness = 9999;
                double highFitness = 0;
                String lowerIndividual = null;
                String higherIndividual = null;
                for (String s : splitArr) {
                    Double tmpValue = Double.valueOf(s.split("_")[0]);
                    if (tmpValue < lowerFitness){
                        lowerFitness = tmpValue;
                        lowerIndividual = s;
                    }
                    if (tmpValue > highFitness){
                        highFitness = tmpValue;
                        higherIndividual = s;
                    }
                }

                // 最终半径
                double finalRadius = Double.valueOf(higherIndividual.split("_")[1]) - Double.valueOf(lowerIndividual.split("_")[1]);

                // 根据距离判断是否需要剔除
                for (String s : splitArr) {
                    Double tmpValue = Double.valueOf(s.split("_")[1]);
                    double distance =  tmpValue - Double.valueOf(lowerIndividual.split("_")[1]);
                    // 如果距离大于半径，则需要将其剔除
                    if (finalRadius < distance){
                        // 剔除操作待完成  或者else重新设置一个集合,然后生成即可  侧输出流
                        sideList.add(s);
                    }else {
                        arrayList.add(s);
                    }
                }
                System.out.println(arrayList.size());
                System.out.println(sideList.size());
                System.out.println("==========");

                // 更新key和value
                mapArrayList.put(key, arrayList);
            }

            // 遍历 sideList,找出leader,然后存入mapArrayList中
            /*double maxValue = 0;
            String maxIndividual = null;
            for (String s : sideList) {
                Double aDouble = Double.valueOf(s.split("_")[0]);
                if (aDouble > maxValue){
                    maxValue = aDouble;
                    maxIndividual = s;
                }
            }
            mapArrayList.put(maxIndividual, sideList);*/

        }

        // 通过mapArrayList 反过来修改 leaderSet,待验证是否有必要更新leaderSet

        // 更新计数器
        if(mapArrayList.size() == nn){
            ct = ct + 1;
        }else{
            nn = mapArrayList.size();
            ct = 0;
        }

    }


    /**
     * 半径合并
     *      s1是高峰,s2是低峰,原有峰不会变化,新增了一个合并后的键值对
     *      峰的移除，是否会有其他影响：
     *      移除目的：为了下一步的小生境剔除操作提供了便利
     *      保留目的：解决合并的过程中存在一个集合多次合并,若提前移除,导致空值现象
     *
     * 将新半径保存到map中,改变其数据结构 value_key_radius
     * map是无法直接修改key值的，所以要采用其他的方案，新增一个键值对，再删除之前那个要修改的
     * 采用迭代器的方式遍历，在迭代中it.remove(),map.put()
     *
     */
    private void radiusMerge(String s1,String s2,double l1) {

        // 进行迭代,选择最远的个体即可,最大距离 = 新半径
        double maxDiff = 0;
        for (String s : mapArrayList.get(s2)) {
            double tmpSite = Double.valueOf(s.split("_")[1]);
            double tmpDiff = Math.abs(tmpSite - l1);
            if(maxDiff < tmpDiff){
                maxDiff = tmpDiff;
            }
        }

        // 新增一个合并后的键值对
        String s = s1 + "_" + maxDiff;
        mapArrayList.put(s, mapArrayList.get(s1));

        // 现象：个体数小于200，待核实
        //System.out.println(mapArrayList);

    }



}



