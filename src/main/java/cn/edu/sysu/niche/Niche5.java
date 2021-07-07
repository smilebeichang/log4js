package cn.edu.sysu.niche;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author : song bei chang
 * @create 2021/6/18 22:13
 *
 *
 *
 * Crossover interactions among niches
 * 确定性拥挤算法(W. Mahfoud,1994)
 * 1.有放回的随机选取两个父代个体p1 p2
 * 2.父代交叉、变异，产生新个体：c1 c2
 * 3.替换阶段(采用拥挤思想来决定下一代)：
 *        3.1 如果[d(p1,c1)+d(p2,c2)]<=[d(p1,c2)+d(p2,c1)]
 *                     如果f(c1)>f(p1),则用c1替换p1,否则保留p1;
 *                     如果f(c2)>f(p2),则用c2替换p2,否则保留p2;
 *         3.2 否则
 *                      如果f(c1)>f(p2),则用c1替换p2,否则保留p2;
 *                      如果f(c2)>f(p1),则用c2替换p1,否则保留p1;
 *
 *
 *
 * Finding multimodal solutions using restricted tournament selection
 * 限制锦标赛拥挤算法(Georges R. Harik,1995)
 *  1.有放回的随机选取两个父代个体p1 p2
 *  2.父代交叉、变异，产生新个体：c1 c2
 *  3.分别为个体c1/c2从当前种群中随机选取w个个体  w是窗口大小,即N元锦标赛
 *  4.设d1/d2分别为w个个体中与c1/c2距离最近的两个个体  难道我把这套逻辑做难了，多考虑了一层？核实文献
 *  5.如果f(c1)>f(d1),则用c1替换d1,否则保留d1;
 *    如果f(c2)>f(d2),则用c2替换d2,否则保留d2;
 *
 *
 * 这三天的任务：
 * 1.英文文献过一遍（认真看过程部分）
 * 2.限制锦标赛拥挤算法的实现
 *
 *              解决方法：直接在交叉。变异阶段各执行一次小生境
 *                      是否在交叉和变异阶段均需进行小生境？
 *                              交叉不需要,限制性锦标赛拥挤本身是没做要求的（文献：交叉变异是同一个个体？ 目前：全局交叉,全局变异）
 *                              单个执行和全局执行是否有偏差：
 *                                      交叉阶段：
 *                                          单个执行,保证了优胜个体的维持（即交叉后可能未被选取）
 *                                          全局执行,具有更高的多样性
 *                                      变异阶段：
 *                                          无差异
 *                              变异，可以嵌入小生境：
 *                              ①去除小生境的交叉部分，去除原有变异的变异部分，两者进行替换
 *                              ②意味着变异可能无效
 *                                  为什么小生境能维持多样性，因为其变异后的个体，会随机选择性的替换最相似的个体
 *                                  而正常的变异，则直接进行替换。直接替换的话，可能的问题是，无法定向维持样性
 *                                  是否还需要进行修补操作，需要看交叉是否影响了个体的类型属性要求。
 *                                     因为是全局交叉+全局变异，则表明类型属性要求遭到了破坏。故需要进行修补
 *                                     下一阶段任务：将全局交叉变异，变成单个个体的交叉变异。
 *
 *
 *
 *              c*w的影响
 *              类似于多小生境拥挤算法，采用的是若干个分体相互竞争的模式，竞争的内容包括适应值和个体之间的距离。
 *              ①随机选取p1,从种群中C个个体作为p1的交配候选集合，从中选出于p1最接近的个体p2。
 *              ①分别为c1/c2 从当前种群随机选择出C个群体,每个群体包含w个个体
 *              ②每个个体都选出一个与其对应子个体距离最近的个体，这样就为每个个体产生了C个替补候选个体
 *              ③不失一般性,设d1/d2是两个替换候选集中适应值最低的个体
 *              ④用c1替换的的d1,c2替换d2
 *              多小生境拥挤算法的搜索能力在拥挤算法中是最强的，这要归功于它的试探性限制交配策略和老个体竞争替换策略
 *              前者的效果与基于个体距离的限制交配策略类似
 *
 *
 * 3.确定性拥挤算法的实现
 * 4.汇总做比较  得出这周的学习汇报进展
 *
 *
 *
 * 本周任务
 *     1.执行顺序是否需要修改 选择 -- 交叉 -- 校验 -- 小生境
*      2.det()的计算,横纵坐标进一步了解
 *     3.流程的校验与核实,最好把适应度值计算出来,公用代码的抽取
 *
 *     考虑交叉变异小生境的执行顺序
 *         目前顺序： 选择 -- 交叉 -- 小生境 -- 校验
 *         执行小生境后，再执行修补算子是否会影响多样性？
 *             小生境：替换近似解,相似指的是adi的相似，
 *             校验：修补的是长度、类型、属性，
 *             两个指标方向不一样,是否会有很多无效行为。FIXME 后期查看相关文献,考虑其相关性
 *         拥挤小生境产生的个体,替换掉最相似的个体后,进入校验,校验过程是根据是否满足长度,属性
 *         如果分开执行,具有一定的随机性。
 *         小生境嵌套校验?
 *              比如：小生境选出了c*w个个体,然后从这c*w个个体中选择满足校验的解。
 *              小生境解集来自当代种群,校验解集来自题库。这该不会就是Pareto最优吧。
 *              ①在小生境的解里面加一个指标，将属性比例加入。w个窗口很难满足要求，故很可能找不到相关的解
 *              只能近似靠拢,
 *         校验嵌套小生境？
 *              ①解集来源不同（ 校验解 是否会和变异的id 进行冲突,查看代码确认其去重逻辑）
 *                  所以可能符合校验解的解，不符合小生境，但这有影响吗？
 *                  解决方案：
 *                      ①对校验解里面加一个指标，将适应度值加入。即满足属性要求，且适应度接近的解 才进行替换
 *                        那么问题来了,这样可能导致寻找解的时间变长，是否会影响效率。
 *                                   还需要考虑的一个点是：需要定义距离，什么样的解，才算适应度接近的解。
 *                        故不建议进行。
 *                        时间 换 空间 是否值得？
 *                        其实拥挤的本质是保证多样性,现在校验多样性也能很好的保证，故不用担心.
 *
 *              ②指标导向不同
 *
 *
 *
 *
 */
public class Niche5 {

    //private JDBCUtils4 jdbcUtils = new JDBCUtils4();

    /** 200套试卷 10道题  */
    private static double[][] paperGenetic =new double[200][10];
    private int POPULATION_SIZE = 200;
    private int GENE_SIZE = 10;



    /**
     * 限制性锦标赛选择算法 restricted tournament selection
     * 目前变异无效,需要查明原因
     *
     */
    public ArrayList<Object>  RTS(double[][] paperGenetictmp,int i)  {

        // 赋值给全局变量
        paperGenetic = paperGenetictmp;

        // 父代变异产生新个体c1
        ArrayList<double[]> cList = mutate(paperGenetic[i]);

        // 为c1从当前种群中随机选取c*w个体  5个小生境  4*5元锦标赛
        ArrayList<Map<Integer, double[]>[]> cwList = championship();

        // 替换 or 保留
        closestResemble(cList, cwList);

        ArrayList<Object> result = new ArrayList<>(2);
        result.add(9999);
        result.add(paperGenetic);
        return  result;

    }






    /**
     * 如果f(c1)>f(d1),则用c1替换d1,否则保留d1;
     * 如果f(c2)>f(d2),则用c2替换d2,否则保留d2;
     *
     * 表现型  适应度值，或者 minAdi
     * 基因型  解(2,3,56,24,4,6,89,98,200,23)
     *      换成基因型吗:为了多样性
     *      替换表现型相似的个体，其后期跳出循环的可能性不大，以及逻辑上存在问题，多峰且各个峰值相等
     *      替换基因型相似的个体，其是否能维持多样性？ 待确认
     *
     */
    private void closestResemble(ArrayList<double[]> cList, ArrayList<Map<Integer, double[]>[]> cwList) {

        double[] c1 = cList.get(0);

        Map<Integer, double[]>[] cw1 = cwList.get(0);

        // 选取基因型做相似性校验
        similarGene(c1, cw1);

    }



    /**
     * 在cw1中寻找c1的近似解  5个小生境  4*5元锦标赛  c1是一套试卷  cw1是c*w套试卷
     * 根据基因型来找出最相似的解
     *
     */
    private void similarGene(double[] c1, Map<Integer, double[]>[] cw1) {

        double max = 0;
        // 设置为0 可能会导致0号索引的数据一直在变化 解决方案：使得每次均能找到相似的个体  目前相似个体数能达到13 会不会太离谱了些
        int maxPhen = 0;

        // 外层C小生境数，内层W锦标赛
        // FIXME 考虑一下，窗口大小究竟是 4*5 还是 4
        for (Map<Integer, double[]> aCw11 : cw1) {

            double[] c2;
            // 遍历map
            for (int j = 0; j < aCw11.size(); j++) {
                for (Object o : aCw11.keySet()) {
                    int key = (int) o;
                    c2 = aCw11.get(key);

                    // 获取最相似的解  相似的判定标准：基因型
                    int sameNum = compareArrSameNum(c1, c2);
                    if (max < sameNum) {
                        max = sameNum;
                        maxPhen = key;
                    }
                }
            }
        }

        //System.out.println("相似的个数："+max +"  最相似的个体："+maxPhen );


        // 替换c1  替换判定标准：表现型|适应度
        // 计算试卷的适应度值
        double sumnum = 0 ;
        for (int i1 = 0; i1 < c1.length; i1++) {
            sumnum = sumnum + c1[i1];
        }

        double sumnum2 = 0 ;
        for (int i1 = 0; i1 < GENE_SIZE; i1++) {
            sumnum2 = sumnum2 + paperGenetic[maxPhen][i1];
        }

        //个体 | 最相似个体 适应度
        double fitc1 = sin1(sumnum/GENE_SIZE) ;
        double fitc2 = sin1(sumnum2/GENE_SIZE) ;

        if (fitc1 > fitc2){
            paperGenetic[maxPhen] = c1;
        }

    }

    /**
     *  比较2个数组中相同的个数
     *      难点在于 可能基因型都只有一个元素不一样  故基因型的判断可能需要将改变了近似相等
     *      会是这里导致 速度变慢的吗？
     */
    private int compareArrSameNum(double[] arr, double[] arr2) {

        // 用于计数
        int c = 0;
        // 遍历arr数组的所有元素
        for (int x = 0; x < arr.length; x++) {
            // 看看这样是否能加快循环
            String s1 = formatDouble(arr[x]);
            // 遍历arr2数组中的所有元素
            for (int y = 0; y < arr2.length; y++) {
                // 计数+1，即相同元素的个数+1
                if (s1.equals(formatDouble(arr2[y]))) {
                    c++;
                }
            }
        }
        return  c;
    }

    /**
     * double 格式转换，保留小数点后两位
     */
    public String formatDouble(double x1){

        return String.format("%.2f", x1);
    }

    /**
     * 按照value倒序排序
     */
    private  <K extends Comparable, V extends Comparable> Map<K, V> sortMapByValues(Map<K, V> aMap) {
        HashMap<K, V> finalOut = new LinkedHashMap<>();
        aMap.entrySet()
                .stream()
                .sorted((p1, p2) -> p1.getValue().compareTo(p2.getValue()))
                .collect(Collectors.toList()).forEach(ele -> finalOut.put(ele.getKey(), ele.getValue()));
        return finalOut;
    }



    /**
     *  分别为c1从当前种群中随机选取c*w个体   怎么会是从题库中搜索题呢，明显是当前种群
     *  种群: 4*5<=20（存在重复+交叉变异）
     *
     *  是否是20元锦标赛过大，待后续优化
     *  Map<Integer, String[]>  key是paperGenetic的索引，value是基因型
     *
     */
    private ArrayList<Map<Integer, double[]>[]> championship()  {

        // 5个小生境  4*5元锦标赛  需进一步验证  窗口大小的具体含义
        int num = 5 ;
        int window = 4 * 5;
        Map<Integer, double[]>[] cwList1 = new HashMap[num];

        // 基本单位:试卷,随机生成一个下标即可 (需保存下标,方便后续替换 map(k,v))
        // 数组裹map
        for (int i = 0; i < num; i++) {
            Set<String> set1 = new HashSet<>();
            // 将个体保存为map结构
            Map<Integer, double[]> mapc1w = new HashMap<>(window);
            while (set1.size() != window) {
                int i1 = new Random().nextInt(POPULATION_SIZE);
                if (!set1.contains(":"+i1)) {
                    set1.add(":"+i1 );
                    mapc1w.put(i1,paperGenetic[i1]);
                }
                cwList1[i] = mapc1w;
            }
        }

        ArrayList<Map<Integer, double[]>[]> cwList = new ArrayList<>(1);
        cwList.add(cwList1);
        // 获取个体的方法:   cwList.get(0)[1]
        return cwList;

    }











    /**
     *  通过变异获得c1个体
     *  随机变异某个基因片段 + 完善交叉导致的基因长度缺失问题
     *
     */
    private ArrayList<double[]> mutate(double[] c1)  {

        //c1变异
        Random random = new Random();
        int mutatePoint = random.nextInt(GENE_SIZE-1);

        //将Array 转 hashSet  去除交叉导致的重复的元素
        ArrayList<Double> arrayList = new ArrayList<>(c1.length);
            for (double anArr : c1) {
            arrayList.add(anArr);
        }
        Set<Double> set = new HashSet<>(arrayList);

        //去除变异的元素
        double s = c1[mutatePoint];
        set.remove(s);

        //临时存储容器
        double[] c11 = new double[GENE_SIZE];

        //生成新的元素  保证题型长度符合要求
        while (set.size() != GENE_SIZE ){

            double key = numbCohesion(Math.random());
            set.add(key);

        }

        //增强for循环 进行赋值
        int index = 0;
        for(double gene : set){
            c11[index] = gene;
            index ++;
        }


        ArrayList<double[]> cList = new ArrayList<>(1);
        cList.add(c11);
        return  cList;

    }





    /**
     * 格式转换工具
     *      保留小数点后三位
     */
    public Double numbCohesion(Double adi){

        return Double.valueOf(String.format("%.3f", adi));

    }


    /**
     * 实现多峰函数 f(x) = sin6(5πx)
     *
     */
    public double sin1(double avgnum ){

        double degrees = 5 * 180 * avgnum;
        //将角度转换为弧度
        double radians = Math.toRadians(degrees);

        //System.out.format("%f 为 %.10f%n", avgnum,  Math.pow(Math.sin(radians), 6));
        return Math.pow(Math.sin(radians), 6);

    }


}



