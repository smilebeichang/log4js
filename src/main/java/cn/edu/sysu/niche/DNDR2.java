package cn.edu.sysu.niche;

import com.sun.istack.internal.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.*;

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
 *              判断哪些峰需要合并(矩阵+凹点问题)  --待确认:故基因型需要为2进制的多基因序列
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
public class DNDR2 {


    /**
     * 容器
     */
    private double[]  paper_genetic =new double[200];
    private int POPULATION_SIZE = 200;
    private int ITERATION_SIZE = 120;
    private Map<Double,Double> SIN_MAP = new HashMap<>(1000);

    /**
     * 排序后的list 1.0_0.3
     */
    ArrayList<String> sortList = new ArrayList<>(200);

    /**
     * 全局半径(初始化为0.1，后续可设置为0.05,0.1,0.2进行验证)
     */
    private  double radius = 0.1 ;


    /**
     *  map存小生境leader, list存小生境member
     */
    HashMap<String,ArrayList<String>> mapArrayList = new HashMap<>();
    HashSet<String> leaderSet = new HashSet();


    /**
     *  1.稳态GA
     */
    @Test
    public void main() {

        // 初始化
        init();
        initSin();

        // 迭代次数
        for (int i = 0; i < 1; i++) {

            // 1.1 哪些峰需要合并
            // 适应度值排序
            sortFitness();
            // 将群体中的个体分配到不同小生境中
            distributeNiche();
            // 判断哪些峰需要合并(矩阵+凹点问题)
            HashSet<String> hs = judgeMerge();
            // 合并
            merge(hs);

            //selectionRts();





        }



    }

    /**
     * 合并
     *    集合成员调整：取大leader做leader,List集合进行合并
     *    半径调整1：选择大leader和小集群的最远值作为半径
     *    个体剔除操作
     *    半径调整2
     */
    public void merge(HashSet<String> hs) {
        // 初步合并
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
                // 需要将适应度小的种群剔除出leaderSet和mapArrayList
                // mapArrayList.remove(s2)
                // leaderSet.remove(s2)
                // 半径调整1
                radiusMerge(s1,s2,l1);
            }else {
                // 集合成员调整
                mapArrayList.get(s2).addAll(mapArrayList.get(s1));
                // 半径调整1
                radiusMerge(s2,s1,l2);
            }
        }


        // 调整合并
        // 通过hs来进行一次过滤,保留最新的mapArrayList集合
        System.out.println("过滤前");
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

        System.out.println("过滤后");
        // HashMap遍历，此种方式效率高
        Iterator iter2 = mapArrayList.entrySet().iterator();
        while (iter2.hasNext()) {
            Map.Entry entry = (Map.Entry) iter2.next();
            Object key = entry.getKey();
            Object val = entry.getValue();
            System.out.println("key: "+key +"  val："+ val);
        }

        // 个体剔除操作 + 半径调整2
        // 调整的是各个小生境的半径,目的是为了可以剔除不需要的个体,以及获取峰半径,便于找到新个体后,在某个具体的小生境范围内进行GA
        // 过滤出样式为value_key_radius的个体,只有这一部分个体半径需要做修改
        // 用于存储临时集合
        ArrayList<String> sideList = new ArrayList<>();

        Iterator iter3 = mapArrayList.entrySet().iterator();
        while (iter3.hasNext()) {
            Map.Entry entry = (Map.Entry) iter3.next();
            String key = (String) entry.getKey();
            Object val = entry.getValue();
            if(key.split("_").length == 3){
                // 用于存储集合
                ArrayList<String> arrayList = new ArrayList<>();
                // 进行剔除操作:(理论基础:小生境中适应度最低的个体 = 离领导个体最远的个体)
                // 半径修改 ==> 如果yes,则保持原状即可。如果no,则将半径修改为最低的位置即可。
                // 其会导致某些解不见,此时如何保留这些解呢？用一个新的集合others
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



            }else{
                // 暂时不进行格式的清洗value_key_radius,因为这将导致异常
            }

            // 更新key和value

        }

        // 通过mapArrayList 反过来修改 leaderSet,待验证是否有必要更新leaderSet





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


    @Test
    public void ttt(){
        /*System.out.println(sin1(0.602));
        System.out.println(sin1(0.703));*/
        System.out.println(sin1(0.997)*0.9);
        System.out.println(6.463107308182912E-4 > 5.5157804095745435E-8);
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
     *
     * 选择嵌套局部最优
     *      搜素空间限定为局部峰值附近寻找，解的话，也使用附近的解去轮盘赌。在每个峰半径单独执行轮盘赌即可
     *      轮盘赌的大小=该小生境个体数
     *      根据极值点的x轴进行计算，将附近的个体求出个体数，适应度值，适应度占比，轮盘赌
     *
     */
    private void selectionRts() {

        System.out.println("====================== selectionRts ======================");
        System.out.println("====================== 拆分数组 ======================");


        // 计算试卷的适应度值
        ArrayList<Double> list1 = new ArrayList<>();
        ArrayList<Double> list2 = new ArrayList<>();
        ArrayList<Double> list3 = new ArrayList<>();
        ArrayList<Double> list4 = new ArrayList<>();
        ArrayList<Double> list5 = new ArrayList<>();

        for (int i = 0; i < POPULATION_SIZE; i++) {

            //abs 获取key 判断key是否是属于某个极值点，若是，map.put(tmpKey, tempCount + 1)
            double tmpKey = numbCohesion(paper_genetic[i]);
            double k1 = Math.abs(0.1 - tmpKey);
            double k2 = Math.abs(0.3 - tmpKey);
            double k3 = Math.abs(0.5 - tmpKey);
            double k4 = Math.abs(0.7 - tmpKey);
            double k5 = Math.abs(0.9 - tmpKey);


            // 等距
            double limitRange = 0.1 ;
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
            Arrays.sort(randomId);

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
     * 分别计算每个小生境中每套试卷的适应度占比
     *      涉及适应度的获取，适应度占比的转换
     *
     */
    private double[] getFitnessRts(@NotNull ArrayList<Double> array){


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
     * 适应度排序
     *      1.排序的目的是为了找出峰值
     *      2.按照适应度降序,但希望能记住其原有的key,后续需使用key进行分组
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

        // 数组转list value_key
        for (double v : paper_genetic) {
            sortList.add(SIN_MAP.get(v)+"_"+v);
        }

        Comparator comp = new MyComparator();
        Collections.sort(sortList,comp);

        /*for(int i = 0;i<sortList.size();i++){
            System.out.println(sortList.get(i));
        }*/


        //按Key进行排序
//        Map<Double, Double> resultMap = sortMapByKey(alist);
//        System.out.println(resultMap.size());

        /*for (Map.Entry<Double, Double> entry : resultMap.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }*/
    }

    /**
     * 使用 Map按key进行排序
     */
    public static Map<Double, Double> sortMapByKey(Map<Double, Double> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        Map<Double, Double> sortMap = new TreeMap<>(
                new MapKeyComparator());

        sortMap.putAll(map);

        return sortMap;
    }


    /**
     * 将群体中的个体分配到不同小生境中 1.0_0.3
     *
     *  后续步骤：
     *      1.选取leader
     *           最后有些个体其实不应该成为leader，待后续进行合并和剔除操作
     *      2.选取member
     *           保证数据总数不变
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
                // 如果隶属于一个小生境
                if(counter < 1){
                    leaderSet.add(sortList.get(i));
                    System.out.println("leader: "+sortList.get(i));
                }
            }
        }

        // 选取member
        //      ①每选完一个leader之后立即进行选取，可以避免无效的迭代
        //      ②选完全部leader之后再进行，可以避免ConcurrentModificationException
        //
        //     现象：存一个点分布在两个小生境中   0.4 分别在0.301和0.0407中
        //     解决方案：每次判断完后，对这个个体标记，表明已经清除
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
     *    使用什么来保存矩阵关系呢？
     *    此方法一个动态大小的二维数组，而不是使用全局变量
     *
     *    优化：对返回距离关系矩阵，只要将要合并的集合返回即可，可能是一个或多个
     *
     *
     */
    private HashSet<String> judgeMerge() {

        // 将E-脏数据去除,目的:其不具有成为leader的资格,但去除的话,将导致数据量变少,丢失正确性
        /*HashSet<String> nl = new HashSet<>();
        for (String s : leaderSet) {
            if (!s.contains("E-")){
                nl.add(s);
            }
        }*/

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
                    // 将距离存在一个集合之中，然后选取最小值(0,1),可以参考之前的矩阵
                    double distance = Math.abs(aDouble - Double.valueOf(leaderList.get(j).split("_")[1]));
                    // 取出最小值
                    if (min > distance){
                       a = i; b = j; min = distance;
                    }
                }
            }


            // 随机选取5个点进行凹点验证：为了解决独立因子
            // 多个随机值当中,只要存在凹点,证明这两个相邻的小生境是独立的,不需要合并
            // 为了消除噪音干扰，定义了一个忍受因子 sf=0.9
            Double bDouble = Double.valueOf(leaderList.get(b).split("_")[1]);
            double sf = 0.9;

            // 一次循环均为正常,则需要合并
            boolean flag = true ;
            for (int j = 0; j < 5; j++) {
                double lemuda = Math.random();
                double cDouble = (lemuda * aDouble) + ((1 - lemuda) * bDouble);
                // 计算适应度值,并进行比较
                Double aFitness = SIN_MAP.get(numbCohesion(aDouble));
                Double bFitness = SIN_MAP.get(numbCohesion(bDouble));
                Double cFitness = SIN_MAP.get(numbCohesion(cDouble));
                //System.out.println(cFitness +","+sf*aFitness+","+sf*bFitness);


                if (cFitness<sf*aFitness && cFitness<sf*bFitness){
                    //System.out.println("存在凹点");
                    flag = false;
                }else{
                    //System.out.println("正常");
                }
            }

            // 需要合并的最终因子
            if(flag){
                // 将ab有值时,赋值为1,其余赋值为0
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


        // 待优化：去重的逻辑很简单，不取整个矩阵，只要根据对角线划分即可  not ok
        /**
         * 0 , 0 , 0 , 0 , 0 , 0 ,
         * 0 , 0 , 0 , 0 , 0 , 0 ,
         * 0 , 0 , 0 , 0 , 0 , 0 ,
         * 0 , 0 , 0 , 0 , 0 , 1 ,
         * 0 , 0 , 0 , 0 , 0 , 1 ,
         * 0 , 0 , 0 , 0 , 0 , 0 ,
         * 0 ,
         * 0 , 0 ,
         * 0 , 0 , 0 ,
         * 0 , 0 , 0 , 0 ,
         * 0 , 0 , 0 , 0 , 0 ,
         * 0 , 0 , 0 , 0 , 0 , 0 ,
         */

        // 获取将要合并的两个峰的下标
        HashSet<String> hs = new HashSet<>();
        for (int i1 = 0; i1 < distanceMatrix.length; i1++) {
            for (int i2 = 0; i2 < distanceMatrix.length; i2++) {
                // 通过对ab排序,将小数字放在前面,然后使用set,解决重复的问题
                // 合并去重的情况也需要进一步考虑 [0.798_0.9, 0.696_0.798] 即 0,2 和 2,5 待进一步考量
                if(distanceMatrix[i1][i2] == 1){
                    if(i1<i2){
                        hs.add(leaderList.get(i1)+":"+leaderList.get(i2));
                    }else {
                        hs.add(leaderList.get(i2)+":"+leaderList.get(i1));
                    }
                }
            }
        }
        System.out.println(hs.size());
        System.out.println(hs.toString());
        return hs;
    }


    /**
     * 半径合并
     *      s1是高峰,s2是低峰
     *      集合可以改变,但不能改变leaderSet,因为会影响后续的合并操作
     */
    private void radiusMerge(String s1,String s2,double l1) {

        // 进行迭代,选择最远的个体即可,寻找最大距离，即为新半径,
        double maxDiff = 0;
        for (String s : mapArrayList.get(s2)) {
            //System.out.println(s);
            double tmpDistance = Double.valueOf(s.split("_")[1]);
            double tmpDiff = Math.abs(tmpDistance - l1);
            if(maxDiff < tmpDiff){
                maxDiff = tmpDiff;
            }
        }
        // 需要使用某种方式将新半径保存下来,改变其数据结构 value_key_radius
        // map可以修改key吗？还是说使用一个新的map进行存储
        /**
         * map是无法直接修改key值的，所以要采用其他的方案，新增一个键值对，再删除之前那个要修改的
         * map要使用ConcurrentHashMap，不然会抛.ConcurrentModificationException异常，然后采用迭代器的方式遍历，不能用for循环，不然也会抛ConcurrentModificationException异常，然后在迭代中it.remove(),map.put();就可以了
         *
         */
        String s = s1 + "_" + maxDiff;
        mapArrayList.put(s, mapArrayList.get(s1));
        // 峰的移除，是否会有其他影响。
        //    移除目的：为了下一步的小生境剔除操作提供了便利
        //    保留目的：解决合并的过程中存在一个集合多次合并,若提前移除,导致空值现象
        // java.lang.NullPointerException
        // [0.8983199482630352_0.512:4.328701998830273E-5_0.412, 4.328701998830273E-5_0.412:0.9992600231455838_0.301]
        //mapArrayList.remove(s1);
        //mapArrayList.remove(s2);
        // 现象：个体数小于200，待核实
        //System.out.println(mapArrayList);

    }






}

/**
 * 比较器类
 */
class MapKeyComparator implements Comparator<Double>{

    @Override
    public int compare(Double str1, Double str2) {

        return str2.compareTo(str1);
    }
}

class MyComparator implements Comparator{
    @Override
    public int compare(Object str1, Object str2) {
        return  Double.valueOf(str2.toString().split("_")[0]).compareTo(Double.valueOf(str1.toString().split("_")[0]));
    }
}



