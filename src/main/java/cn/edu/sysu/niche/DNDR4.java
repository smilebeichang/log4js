package cn.edu.sysu.niche;

import cn.edu.sysu.adi.TYPE;
import cn.edu.sysu.pojo.Papers;
import cn.edu.sysu.utils.JDBCUtils4;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.sql.SQLException;
import java.util.*;

/**
 * @Author : song bei chang
 * @create 2021/8/20 22:01
 *
 *
 *  《A Diverse Niche radii Niching Technique for Multimodal Function Optimization》
 *
 * 复现自适应小生境的代码
 *
 *      初始化 -- 选择 -- 交叉 -- 变异 -- 修补
 *            |<----     niche      ---->|
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
 * FIXME 本周任务:
 *       1.代码初步实现
 *          优化合并峰
 *              方案一：(筛选-- 合并 -- 调整 -- 筛选 -- 合并)
 *                  判断哪些峰需要合并,选出一个最近点，进行执行合并
 *                  合并完成后，进行半径调整和个体剔除,再进行判断是否还有需要继续合并的峰
 *              方案二：若存在相同的峰,直接选择最小的一个进行,另外一个过滤掉暂时不做处理即可
 *          此处选择方案二：相对简单些,不用考虑迭代, 验证迭代过程中总个体数是否发生了变化  ok
 *
 *
 *       2. 使用现有的double算子,进行迭代操作,相似性：横坐标。交叉变异：不大好实现 not ok
 *
 *       2.代入GA 继续完成(难点:基因型转换)
 *          概率性选择10道题 二进制选择
 *          相似性：相似的题目数   交叉变异：正常执行
 *
 *
 *
 *       3.修补算子
 *
 */
public class DNDR4 {


    /**
     * 控制是否进行半径的修改
     */
    private static final int CONTINUE_TIMES = 5;
    private static final int NUMBER_OF_NICHES = 1;

    private int ct = 0;
    private int nn = 0;

    /**
     * 迭代次数
     */
    private int ITERATION_SIZE = 120;

    /**
     * 容器
     *    paper_genetic double[]格式
     *    保留了小数点后三位
     */
    private double[]  paper_genetic =new double[200];
    private int POPULATION_SIZE = 200;
    private Map<Double,Double> SIN_MAP = new HashMap<>(1000);

    /**
     * 排序后的list 1.0_0.3
     */
    private ArrayList<String> sortList = new ArrayList<>(200);

    /**
     * 全局半径(初始化为0.1，可设置为0.05,0.1,0.2进行验证)
     */
    private  double radius = 0.1 ;

    /**
     *  map(key是string存小生境leader, value是list存小生境member)
     *      leader + followers
     */
    private HashMap<String,ArrayList<String>> mapArrayList = new HashMap<>();

    /**
     * set存leader
     */
    private HashSet<String> leaderSet = new HashSet();

    private Logger log = Logger.getLogger(DNDR3.class);



    /* 100套试卷 10道题  */
    private static String[][] paperGenetic =new String[100][10];

    private JDBCUtils4 jdbcUtils = new JDBCUtils4();

    private static ArrayList<String> bankList = new ArrayList();




    /**
     *  主程序
     */
    @Test
    public void main() throws SQLException {

        //选择100套的原因，基数够大，能为交叉变异提供相对较多的原始材料
        //抽取试卷  100套、每套试卷10题
        Papers papers = new Papers();
        papers.setPc(0.6);
        papers.setPm(0.8);

        //初始化试卷(长度，题型，属性比例)
        //轮盘赌
        initItemBank4();

        // 百万级别的循环耗时1s
        initSin();


        mapArrayList.clear();
        leaderSet.clear();

            // 初始化
            init();

            // 迭代次数  目前只迭代一次
            for (int i = 0; i < ITERATION_SIZE; i++) {



                // 适应度值排序 0.9992_0.101
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

                // FIXME  选择交叉变异如何嵌入其中 应该是先动态生成小生境,然后进行选择交叉变异 mapArrayList



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
     * 初始化计算,赋值给 SIN_MAP
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

        Comparator comp = new MyComparator3();
        // empty String
        Collections.sort(sortList,comp);

    }



    /**
     * 将群体中的个体分配到不同小生境中 1.0_0.3
     *
     *  步骤：
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
                // 需要使用一个计数器,进行判断是否符合全部要求
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
                // 判空操作是因为后续会做标记,将value值重置为空
                if (!StringUtils.isBlank(s)){
                    double a  = Double.valueOf(s.split("_")[1]);
                    if(Math.abs(a-b)<radius){
                            memberList.add(s);
                            // java.lang.NumberFormatException: empty String
                            // 是否可以直接移除
                            sortList.set(i, "");
                    }
                }
            }
            mapArrayList.put(leader, memberList);
            // 验证个体总数是否丢失
            sum = memberList.size() + sum;
        }
        System.out.println("此次迭代个体总数目："+sum);
    }

    /**
     * 判断哪些峰需要合并(矩阵+凹点问题) mapArrayList leaderSet
     *    用距离公式来表示任意两个小生境之间的关系,得出一个距离关系w矩阵（注：因为是01矩阵,所以后续需进一步去重）
     *
     *    使用什么来保存矩阵关系呢？ 一个动态大小的二维数组
     *
     *    优化:
     *      1.将距离关系矩阵,进一步简化,只将要合并的集合返回即可
     *      2.若存在共用相同的峰,直接选择最小的一个进行,另外一个过滤掉即可(集合的再次过滤)
     *
     */
    private HashSet<String> judgeMerge() {

        // 距离关系w矩阵
        int[][] distanceMatrix =new int[leaderSet.size()][leaderSet.size()];

        // set 转 arrayList
        List<String> leaderList = new ArrayList<>(leaderSet);

        // 遍历计算距离关系,并生成01矩阵
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


            // 随机选取5个点进行凹点验证
            // 多个随机值当中,只要存在凹点,证明这两个相邻的小生境是独立的,不需要合并
            // 为了消除噪音干扰,定义了一个忍受因子 sf=0.9
            Double bDouble = Double.valueOf(leaderList.get(b).split("_")[1]);
            double sf = 0.9;

            // 本轮循环中未出现凹点,则需要合并
            boolean flag = true ;
            final int GER_RND = 5;
            for (int j = 0; j < GER_RND; j++) {
                double lemuda = Math.random();
                // 计算公式一致  ra+(1-r)b  和  a-r(a-b) = (1-r)a + rb
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

            // 未出现凹点需要合并的最终因子,通过ab索引,赋值为1,其余赋值为0
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


        // 获取将要合并的两个峰的下标  hs去重
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


        // 在hs中统计各个峰值出现的次数
        // hs.toString(): [0.964308398_0.507:6.9465303E-7_0.406]
        // 将横坐标放置到list集合中
        ArrayList<Double> indexList = new ArrayList<>();
        for (String h : hs) {
            // leader
            String s1 = h.split(":")[0];
            String s2 = h.split(":")[1];
            double l1 = Double.valueOf(s1.split("_")[1]);
            double l2 = Double.valueOf(s2.split("_")[1]);
            // 存峰值的位置
            indexList.add(l1);
            indexList.add(l2);
        }
        // 统计次数大于1的峰
        List<Double> gtOneList = frequencyGtOne(indexList);
        System.out.println(gtOneList);

        // 新增一个方法，将次数等于1的找出来
        List<String> eqOneList = findEqOne(hs,gtOneList);

        // 最终需要合并的集合容器
        HashSet<String> allHs = new HashSet<>();
        allHs.addAll(eqOneList);

        // 2.分组计算出哪个最近,本质是进行一次过滤操作
        // 将hs和doubleList遍历,重复的进行进一步选择,每一个doubleList只返回一个距离最近的  set.get() != null
        // 对gtOneList做非空判断
        if(gtOneList!=null){
            for (Double aDouble : gtOneList) {
                ArrayList<String> tmpList = new ArrayList<>();
                // 将含有相同元素的集合分组
                for (String h : hs) {
                    if(h.contains(aDouble+"")){
                        tmpList.add(h);
                    }
                }
                // 无需对tmpList做判空处理  tmpList至少为2
                String t = null;
                if(tmpList.size() != 0){
                    // 遍历tmpList,进行比较,只留下最近的一个
                    double minDis = 9999;
                    for(String tmp : tmpList){
                        String s1 = tmp.split(":")[0];
                        String s2 = tmp.split(":")[1];
                        double l1 = Double.valueOf(s1.split("_")[1]);
                        double l2 = Double.valueOf(s2.split("_")[1]);
                        if (minDis > Math.abs(l1 - l2)){
                            minDis = Math.abs(l1 - l2);
                            t = tmp;
                        }
                    }
                }
                // 最近的一组t
                System.out.println("最近的一组以及找到:"+t);
                allHs.add(t);
            }
        }

        System.out.println(allHs);

        return allHs;
    }

    /**
     * 查找 只出现一次的峰
     *      1.排除掉不等于1的元素即可
     *      2.hs和doubleList的遍历判断过程
     */
    private List<String> findEqOne(HashSet<String> hs, List<Double> gtOneList) {

        // 未出现在gtOneList中
        boolean oneOnlyAppear = true;
        ArrayList<String> eqOneList = new ArrayList<>();
        for (String h : hs) {
            for (Double aDouble : gtOneList) {
                // 如果包含
                if(h.contains(aDouble+"")){
                    oneOnlyAppear = false;
                }
            }
            if(oneOnlyAppear){
                eqOneList.add(h);
            }
        }
        return eqOneList;
    }


    /**
     * 合并
     *    集合成员调整: 取大leader做总leader,两个List集合进行合并
     *    半径调整1:   选择大leader和小集群的最远值作为半径
     *    个体剔除操作+半径调整2: 理论基础:小生境中适应度最低的个体 = 离领导个体最远的个体
     *          目的：调整的是各个小生境的半径,剔除不需要的个体以及获取峰半径,
     *               便于随后的交叉变异,在某个具体的小生境范围内进行GA
     *
     */
    private void merge(HashSet<String> hs) {

        // hs集合不需要进一步去重 hs.toString(): [0.964308398_0.507:6.9465303E-7_0.406]
        for (String h : hs) {
            // 根据适应度值确定leader   合并判断值错误,待优化
            String s1 = h.split(":")[0];
            String s2 = h.split(":")[1];
            double l1 = Double.valueOf(s1.split("_")[0]);
            double l2 = Double.valueOf(s2.split("_")[0]);
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

        // 集合调整
        Iterator iter = mapArrayList.entrySet().iterator();
        // 打印操作
        System.out.println("过滤前:");
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object key = entry.getKey();
            Object val = entry.getValue();
            System.out.println("key: "+key +"  val："+ val);
        }

        // 将hs进行过滤,保留最新的mapArrayList集合
        for (String h : hs) {
            mapArrayList.remove(h.split(":")[0]);
            mapArrayList.remove(h.split(":")[1]);
        }

        // HashMap遍历，此种方式效率高
        System.out.println("过滤后:");
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
        int sum = 0;

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
                    // 如果距离大于半径,则需要将其剔除
                    if (finalRadius < distance){
                        // 侧输出流
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

            //


        }
        // 验证当前总个数
        Iterator iter4 = mapArrayList.entrySet().iterator();
        while (iter4.hasNext()) {
            Map.Entry entry = (Map.Entry) iter4.next();
            String key = (String) entry.getKey();
            ArrayList<String> val = (ArrayList<String>) entry.getValue();
            sum = sum + val.size();
        }

        sum = sum +  sideList.size() ;
        System.out.println("个体总数："+sum);

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
     *      峰的移除和保留：
     *          移除目的：为了下一步的小生境剔除操作提供了便利
     *          保留目的：解决合并的过程中存在一个集合多次合并,若提前移除,导致空值现象、
     *                  judge过程已经做了去重,所以应该可以不再考虑此逻辑
     *
     * 将新半径保存到map中,改变其数据结构 value_key_radius
     * map是无法直接修改key值的，所以要采用其他的方案，新增一个键值对，再删除之前那个要修改的
     * 采用迭代器的方式遍历，在迭代中it.remove(),map.put()操作
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

    @Test
    public void test(){
        ArrayList<Double> items = new ArrayList<>();
        items.add(111.0);
        items.add(111.0);
        items.add(222.0);
        Map<Double, Integer> stringIntegerMap = frequencyOfListElements(items);
        System.out.println(stringIntegerMap);

    }

    /**
     * java统计List集合中每个元素出现的次数
     * 例如frequencyOfListElements("111","111","222")
     *
     * 则返回Map {"111"=2,"222"=1}
     *
     */
    public  Map<Double,Integer> frequencyOfListElements( List<Double> items ) {
        //
        if (items == null || items.size() == 0) {
            return null;
        }
        Map<Double, Integer> map = new HashMap<Double, Integer>();
        for (Double temp : items) {
            Integer count = map.get(temp);
            map.put(temp, (count == null) ? 1 : count + 1);
        }
        return map;
    }


    /**
     * java统计List集合中每个元素出现的次数
     * 例如frequencyOfListElements("111","111","222")
     *
     * 则返回次数大于1的key {"111"=2,"222"=1} --> 111
     *
     */
    public  List<Double> frequencyGtOne(List<Double> items ) {
        // 过滤空值
        if (items == null || items.size() == 0) {
            return null;
        }
        Map<Double, Integer> map = new HashMap<>();

        for (Double temp : items) {
            Integer count = map.get(temp);
            map.put(temp, (count == null) ? 1 : count + 1);
        }

        // 遍历map,将value>1的key存储下来
        List<Double> list = new ArrayList<>();
        for(Map.Entry<Double, Integer> entry : map.entrySet()){
            System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
            if(entry.getValue() > 1){
                list.add(entry.getKey());
            }
        }
        return list;
    }



    /**
     *
     * 使用构造法选取题目  (轮盘赌）
     *      1.题型构造解决 （完全随机模式,不考虑下限比例）
     *      2.属性构造解决 （完全随机模式,不考虑下限比例）
     *      3.设置属性比例  可以通过惩罚系数来设定  超出,则急剧减少
     *      总结：在初始化的时候，不需要保证题型和属性符合要求，使用GA迭代和轮盘赌解决
     *
     *
     */
    private void initItemBank4() throws SQLException {

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







}

/**
 * 比较器类
 */
class MyComparator4 implements Comparator{
    @Override
    public int compare(Object str1, Object str2) {
        return  Double.valueOf(str2.toString().split("_")[0]).compareTo(Double.valueOf(str1.toString().split("_")[0]));
    }
}



