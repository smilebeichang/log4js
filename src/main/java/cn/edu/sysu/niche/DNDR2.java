package cn.edu.sysu.niche;

import com.sun.istack.internal.NotNull;
import org.junit.Test;

import java.sql.SQLException;
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
    public void main() throws SQLException {

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
            int[][] judgeMergeArray = judgeMerge();

            //selectionRts();



        }



    }




    /**
     * 随机生成初代种群
     *      200个体  单基因
     *      优化方案1：初始化一次，存入文件中，后续统一调用。但时间上估计不会相差太大。只是200个个体而已
     *      优化方案2：初始化时保证0.001.这样可以方便后续的计算。
     *
     *      个体的越多,越具有代表性。
     */
    private void  init() {
        System.out.println("=== init POPULATION_SIZE ===");
        for (int i = 0; i < POPULATION_SIZE; i++) {
            paper_genetic[i] = numbCohesion(Math.random());
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
     *  1.生成map(fitness,key)
     *  2.排序
     *
     *  问题:
     *      1.个体数存在!=200的情况吗？hashMap
     *      2.降序排序已实现
     *
     *  目的:
     *      1.排序的目的是为了找出峰值
     *      2.实现按照适应度降序,但希望能记住其原有的key,后续需使用key进行分组
     *      3.map ×  list ×
     *        3.1 list使用 value_key 进行排序,存在乱序现象,double进行比较即可
     *              1.4655297809617463E-5_0.39
     *              1.0_0.9
     *        3.2 list使用 value 排序,再在后面需要用的时候,进行一次反向取值(反向取值可能存在难度,其也是hashMap)
     *
     *  最终结果:
     *      1.0_0.3
     *      0.9992600231455838_0.101
     *      ......
     *
     *  如何存储呢?
     *      1.直接将上述list(value_key)存起来
     *      2.只存key
     *
     *
     */
    private void sortFitness() {

        for (double v : paper_genetic) {
            sortList.add(SIN_MAP.get(v)+"_"+v);
        }

        Comparator comp = new Mycomparator();
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
     * @param map
     * @return
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
     *      2.选取member
     */
    private void distributeNiche() {

        // 选取leader
        for (int i = 0; i < sortList.size(); i++) {
            if (leaderSet.size()==0){
                leaderSet.add(sortList.get(i));
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

                if(counter < 1){
                    leaderSet.add(sortList.get(i));
                    System.out.println("leader: "+sortList.get(i));
                }
            }
        }

        // 选取member ①每选完一个leader之后立即进行选取，可以避免无效的迭代 ②选完全部leader之后再进行，可以避免ConcurrentModificationException
        // 是否存在一个点分布在两个小生境中  no
        //         现象：存在一个值在两个小生境中   0.4 分别在0.301和0.0407中
        //         原因: leader选取出了点问题,最后有些个体其实不应该成为leader，暂时不深究，待后续完成
        // 选取member
        // 输出leader
        for (String leader : leaderSet) {
            ArrayList<String> memberList = new ArrayList<>();
            double b  = Double.valueOf(leader.split("_")[1]);
            for (String s : sortList) {
                double a  = Double.valueOf(s.split("_")[1]);
                if(Math.abs(a-b)<radius){
                    memberList.add(s);
                }
            }
            mapArrayList.put(leader, memberList);
        }


        // System.out.println(mapListArrayList);


    }

    /**
     * 判断哪些峰需要合并(矩阵+凹点问题) mapArrayList leaderSet
     *    用距离公式来表示任意两个小生境之间的关系,得出一个距离关系的w矩阵
     *
     *    使用什么来保存矩阵关系呢？
     *    直接使用一个二维数组吧,但二维数组的长度是确定的，所以最好的方式是此方法返回一个二维数组，而不是使用全局变量
     *
     *
     *
     */
    private int[][] judgeMerge() {

        // 将E-脏数据去除
        HashSet<String> nl = new HashSet<>();
        for (String s : leaderSet) {
            if (!s.contains("E-")){
                nl.add(s);
            }
        }
        System.out.println(leaderSet.size()+" vs "+nl.size());

        // 距离关系w矩阵
        int[][] distanceMatrix =new int[nl.size()][nl.size()];

        // set 转 arrayList
        List<String> leaderList = new ArrayList<>(nl);
        System.out.println(leaderList);

        // 遍历获取距离关系,并生成矩阵
        for (int i=0;i < leaderList.size(); i++) {
            double min = 9999; int a=0; int b=0;
            for (int j=0;j < leaderList.size(); j++) {

                if (!leaderList.get(i).equals(leaderList.get(j))){
                    // 将距离存在一个集合之中，然后选取最小值(0,1),可以参考之前的矩阵
                    double distance = Math.abs(Double.valueOf(leaderList.get(i).split("_")[1]) - Double.valueOf(leaderList.get(j).split("_")[1]));
                    // 取出最小值
                    if (min > distance){
                       a = i; b = j; min = distance;
                    }
                }
            }
            System.out.println(a+","+b);
            // 将ab有值时,赋值为1,其余赋值为0
            distanceMatrix[a][b]=1;

        }
        // 先尝试遍历二维数组试试
        for (int i1 = 0; i1 < distanceMatrix.length; i1++) {
            for (int i2 = 0; i2 < distanceMatrix[i1].length; i2++) {
                System.out.print(distanceMatrix[i1][i2]+" , ");
            }
            System.out.println();

        }
        return distanceMatrix;


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

class Mycomparator implements Comparator{
    @Override
    public int compare(Object str1, Object str2) {
        return  Double.valueOf(str2.toString().split("_")[0]).compareTo(Double.valueOf(str1.toString().split("_")[0]));
    }
}



