package cn.edu.sysu.niche;

import cn.edu.sysu.adi.TYPE;
import cn.edu.sysu.pojo.Papers;
import cn.edu.sysu.utils.JDBCUtils4;
import cn.edu.sysu.utils.KLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.sql.SQLException;
import java.util.*;



/**
 * @Author : song bei chang
 * @create : 2021/10/22 22:09
 *
 *
 * 《A Diverse Niche radii Niching Technique for Multimodal Function Optimization》
 *
 *
 * 反问：小生境 ---> 为了保证多样性
 * 自适应小生境 ---> 为了找到峰，然后保证多样性
 *
 * 目前进展：已经证明小生境确有这个功能，而且自适应小生境也的确能实现
 * 自适应小生境不完全基于标准函数，是通过惩罚系数实现适应度的计算，故其具有适配性
 *
 *
 * 复现自适应小生境的代码
 * 1.继续实现自己的代码(交叉变异不校验那种)
 *   不检验的时候，可以使用表现性作为相似性的判断，
 *   但校验之后呢,因为新个体都相似了，故可以在最后做一次总的变异。
 *   选取leader ok,选取follower 判断标准出了问题。（leader是适应度，follower是相似性）
 *
 * 2.使用文献的方式来实现
 *      初始化 -- 选择 -- 交叉 -- 变异 -- 修补
 *      |<----     niche    ---->|
 *
 *
 * 本周任务:
 * 1.代码初步实现
 *      优化合并峰(数据重复问题)
 *      方案一：(筛选-- 合并 -- 调整 -- 筛选 -- 合并)
 *          判断哪些峰需要合并,先选出一个最近点，进行合并。合并完成后，通过半径调整和个体剔除,再判断是否还有需要继续合并的峰
 *
 *      方案二：若存在相同的峰,直接选择最小的一个进行,另外一个过滤掉暂时不做处理即可
 *          此处选择方案二: 相对简单些,不用考虑迭代
 *
 * FIXME 本周任务:
 * 流程的校验和适应度值的计算：
 *      因为变异/多样性过大,是否能维持住全局最优？
 *      1.看代码逻辑,捋清思,准备在哪里做什么
 *      2.明天搞代码的具体实现
 *
 * 相似性：
 *      1.通过校验小生境个数来进行选择,选择是否进行小生境内的交叉变异
 *      2.侧输出流。可以两个大的转换成一个大的+小的
 *
 * 2.代入GA
 *      难点一: 基因型转换    解决方案：二进制选择 + 概率性
 *      难点二：相似性的判断   基因型(即题目的相似性) vs 表现型(即适应度,即满足各种约束条件的情况)
 *
 *      自适应小生境的目的是可以根据小生境半径确定小生境的个数，然后在小生境内进行GA操作
 *
 *                         原始               修订
 *      适应度：            标准函数计算       惩罚系数的计算
 *      leader:           适应度最大的个体    适应度最大的个体
 *      相似性：            离leader的相似性   题目的相似个数
 *      小生境的半径：       横坐标0~1          题目相似个数
 *
 *      适应度排序(适应度) --- 分配到不同小生境(相似性) --- 判断合并(距离矩阵、凹点问题、去重操作)
 *      集合调整 --- 半径调整1 --- 半径调整2 --- 个体剔除操作
 *
 *
 * 3.修补算子
 *
 */
public class DNDR6 {


    private Logger log = Logger.getLogger(DNDR6.class);

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
     * paper_genetic double[]格式
     * 保留了小数点后三位
     */
    private double[] paper_genetic = new double[200];
    private int POPULATION_SIZE = 200;
    private Map<Double, Double> SIN_MAP = new HashMap<>(1000);

    /**
     * 排序后的list 1.0_0.3
     */
    private ArrayList<String> sortList = new ArrayList<>(200);
    private ArrayList<String> sortListForGene = new ArrayList<>(200);

    /**
     * 全局半径(初始化为0.1，可设置为0.05,0.1,0.2进行验证)
     */
    private double radius = 0.1;

    /**
     * map(key是string存小生境leader, value是list存小生境member)
     * leader + followers
     */
    private HashMap<String, ArrayList<String>> mapArrayList = new HashMap<>();
    private HashMap<String, ArrayList<String>> mapArrayListForGene = new HashMap<>();

    /**
     * set存leader
     */
    private HashSet<String> leaderSet = new HashSet();
    private HashSet<String> leaderSetForGene = new HashSet();


    /**
     * 200套试卷 20道题
     */
    private static String[][] paperGenetic = new String[200][20];

    private JDBCUtils4 jdbcUtils = new JDBCUtils4();

    private static ArrayList<String> bankList = new ArrayList();

    private KLUtils klUtils = new KLUtils();

    /**
     * 小生境对象
     */
    private Niche3 niche3 = new Niche3();

    private static  double PC = 0.6;
    private static  double PM = 0.8;


    /**
     * 主程序
     */
    @Test
    public void main() throws SQLException {

        //抽取试卷  200套、每套试卷20题  为交叉变异提供原始材料

        // 百万级别的循环耗时1s
        //initSin();

        // 初始化
        //init();

        // 初始化试卷(长度，题型，属性比例) 轮盘赌生成二维数组 String[][] paperGenetic = new String[200][20]
        initItemBank4();

        // 迭代次数
        for (int i = 0; i < ITERATION_SIZE; i++) {

            //mapArrayList.clear();
            mapArrayListForGene.clear();
            //leaderSet.clear();
            leaderSetForGene.clear();

            // 适应度值排序 0.9992_0.101  ArrayList<String> sortListForGene = new ArrayList<>(200)
            //sortFitness();
            sortFitnessForGene();

            // 将群体中的个体分配到不同小生境中 leader + members    mapArrayListForGene(key,value)
            // distributeNiche();
            distributeNicheForGene();


            // 暂时不做合并操作（合并+判断） 为了简化难度 后期优化

            // 判断哪些峰需要合并(矩阵+凹点问题)
            //   HashSet<String> hs = judgeMerge();

            // 合并
            //   merge(hs);

            // 调整初始半径
            //  adjustRadius();
            log.info("小生境个数: " + mapArrayListForGene.size());

            /**
             * FIXME:
             * 相似性：
             *   1.通过判断小生境个数来进行选择,选择是否进行小生境内的交叉变异
             *   2.侧输出流。可以两个大的生成一个大的+小的
             *
             *
             *  交叉变异：若小生境个数大于2的那部分在小生境内执行
             *          若小生境个数=1的进行全局的交叉变异(自从代码优化后，发现没有=1的个体了)
             *
             *  需在各个小生境中进行选择|交叉|变异  mapArrayListForGene
             *
             *
             *  GA 操作的范围: 小生境内 or 全局
             *      方案: 挨个对个体进行判断,
             *              选择： size 决定了 选择的范围
             *              交叉|变异： 因为只变化一个个体，故，确定好了选择范围后，直接进行就好，无需考虑第二个个体
             *
             *  必须把代码完成，不然不好交差
             *
             */


            // ①遍历，根据该个体所在的小生境个数，然后塞到不同集合中
            HashMap<String, ArrayList<String>> inListHashMap = new HashMap<>();
            ArrayList<String> outList = new ArrayList<>();
            Iterator<Map.Entry<String, ArrayList<String>>> iterator = mapArrayListForGene.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, ArrayList<String>> entry = iterator.next();

                // 遍历value,看是否小生境个数 >= 2
                if (entry.getValue().size() >= 2) {
                    inListHashMap.put(entry.getKey(), entry.getValue());
                } else {
                    // 是需要addValue,但绝对不是连key不管   有leader 但没value的,leader 不包含在member中吗？
                    //outList.add(entry.getKey());
                    outList.addAll(entry.getValue());
                }
            }


            // ②对不同的集合进行不同的选择 (小生境内 | 小生境外)
            // 此处选择和之前不大一样了，之前是一次性选择出所有，这里是挨个进行选择
            // 方案：将小生境个数>=2的个体挑出来，这部分个体进行小生境内选择；其余部分进行小生境外选择

            // 进行小生境内的选择   小生境内有多少个体就执行执行多少次选举，选出适应个体
            // 最好是进去啥,返回啥。保持原样不做修改,这样有利于后续的交叉变异
            HashMap<String, ArrayList<String>>  inBack = selectionIn(inListHashMap);

            ArrayList<String> outBack = new ArrayList<>();
            // 进行小生境外的选择
            if(outList.size()>0){
                 outBack  = selectionOut(outList);
            }

            // 问题一: 为什么会存在相同的value      已OK
            // 问题二: 总数对不上，2(29+439)+197   已OK


            // 交叉变异:
            // 方案: 小生境范围内进行交叉变异， 修改交叉变异的方案,返回新个体后，然后复原为 paperGenetic   因为适应度值排序，下一轮用得上 sortFitnessForGene
            // 进行小生境内交叉
            HashMap<String, ArrayList<String>> stringArrayListHashMap = crossCoverIn(inBack);
            ArrayList<String> arrayList = crossCoverOut(outBack);

            for (int j = 0; j < paperGenetic.length-1 ; j++) {
                //交叉  未校验
                //crossCover(j);
                //变异  新增了变异部分后，变得这么慢了吗 嵌套了一层 paperGenetic.length 限制性锦标赛
                //mutate(j);
            }


            // 暂时将 交叉变异设置为全局模式，待后续优化
            // 交叉
            // 通过索引j,来确保交叉变异的均为同一个个体
/*                for (int j = 0; j < paperGenetic.length-1 ; j++) {
                    //交叉
                    crossCover(papers,j);
                    //变异  新增了变异部分后，变得这么慢了吗 嵌套了一层 paperGenetic.length
                    mutate(papers,j);
                }*/

        }

    }


    /**
     * 调整初始半径
     * 1)若候选小生境数连续p次等同于实际小生境数，则 R = R * r ;
     * 2)若候选小生境数小于2，则 R = R  / r.
     */
    private void adjustRadius() {

        // 缩小初始半径
        if (CONTINUE_TIMES == ct) {
            radius = radius * Math.random();
            ct = 0;
        }

        // 扩大初始半径
        if (NUMBER_OF_NICHES == mapArrayList.size()) {
            radius = radius / Math.random();
        }

    }


    /**
     * 随机生成初代种群
     * 200个体  单基因
     * 优化方案：初始化一次，存入redis中,精度保证0.001.这样可以方便后续的计算
     * <p>
     * 个体的越多,越具有代表性
     */
    private void init() {

        System.out.println("=== init POPULATION_SIZE ===");
        for (int i = 0; i < POPULATION_SIZE; i++) {
            paper_genetic[i] = numbCohesion(Math.random());
        }

    }


    /**
     * 初始化计算,赋值给 SIN_MAP
     * 1000/0.001= 100W的数据
     */
    private void initSin() {

        System.out.println("=== init SIN_MAP ===");
        double step = 0.001;
        double end1 = 1000;
        for (double i = 0.000; i < end1; i = i + step) {
            SIN_MAP.put(numbCohesion(i), sin1(i));
        }
        /*double end3 = 1;
        for (double i = 0.000; i < end3; i=i+step) {
            SIN_MAP.put(numbCohesion(i),sin2(i));
        }*/

    }


    /**
     * 格式转换工具, 保留小数点后三位
     */
    public Double numbCohesion(Double adi) {

        return Double.valueOf(String.format("%.3f", adi));

    }


    /**
     * 实现多峰函数 f(x) = sin6(5πx)
     */
    private double sin1(double avgnum) {

        double degrees = 5 * 180 * avgnum;
        //将角度转换为弧度
        double radians = Math.toRadians(degrees);

        //System.out.format("%f 为 %.10f%n", avgnum,  Math.pow(Math.sin(radians), 6));
        return Math.pow(Math.sin(radians), 6);

    }


    /**
     * 适应度排序
     * 1.排序的目的是为了找出峰值
     * 2.按照适应度降序,希望能记住其原有的key,后续需使用key进行分组
     * 3.map ×  list √
     * list使用 (value_key) ,使用double进行比较解决乱序问题
     * <p>
     * 最终结果:
     * 1.0_0.3
     * 0.9992600231455838_0.101
     * ......
     */
    private void sortFitness() {

        // 数据清空
        sortList.clear();

        // 数组转list value_key
        for (double v : paper_genetic) {
            sortList.add(SIN_MAP.get(v) + "_" + v);
        }

        Comparator comp = new MyComparator();
        // empty String
        Collections.sort(sortList, comp);

    }

    /**
     * 适应度值排序
     */
    private void sortFitnessForGene() {

        // 数据清空
        //sortList.clear();
        sortListForGene.clear();

        // 数组转list value_key
        /*for (double v : paper_genetic) {
            sortList.add(SIN_MAP.get(v)+"_"+v)
        }*/

        // 遍历二维数组，获取其适应度，然后拼接上本身  sortListForGene.add(minrum + "_" + ids)
        int paperSize = paperGenetic.length;
        getFitnessForGene(paperSize);

        Comparator comp = new MyComparator();
        // empty String
        Collections.sort(sortListForGene, comp);

    }


    /**
     * 将群体中的个体分配到不同小生境中 1.0_0.3
     * <p>
     * 步骤：
     * 1.选取leader
     * 最后有些个体其实不应该成为leader,待后续进行合并和剔除操作
     * 2.选取member
     * 保证数据总数不变
     * ①每选完一个leader之后立即进行选取，可以避免无效的迭代                    未采用
     * ②选完全部leader之后统一进行，可以避免ConcurrentModificationException  √
     * <p>
     * 现象：存一个点分布在两个小生境中   0.4 分别在0.301和0.0407中
     * 解决方案：每次判断完后，对这个个体标记已经清除
     */
    private void distributeNiche() {

        // 选取leader
        for (int i = 0; i < sortList.size(); i++) {
            if (leaderSet.size() == 0) {
                leaderSet.add(sortList.get(i));
                System.out.println("leader: " + sortList.get(i));
            } else {
                double a = Double.valueOf(sortList.get(i).split("_")[1]);
                // 需要使用一个计数器,进行判断是否符合全部要求
                int counter = 0;
                for (String leader : leaderSet) {
                    // b 的判断应该满足全部的leader
                    double b = Double.valueOf(leader.split("_")[1]);
                    if (Math.abs(a - b) < radius) {
                        counter = counter + 1;
                    }
                }
                // 如果不隶属于任何一个小生境
                if (counter < 1) {
                    leaderSet.add(sortList.get(i));
                    System.out.println("leader: " + sortList.get(i));
                }
            }
        }

        // 选取member
        int sum = 0;
        for (String leader : leaderSet) {
            ArrayList<String> memberList = new ArrayList<>();
            double b = Double.valueOf(leader.split("_")[1]);
            for (int i = 0; i < sortList.size(); i++) {
                String s = sortList.get(i);
                // 判空操作是因为后续会做标记,将value值重置为空
                if (!StringUtils.isBlank(s)) {
                    double a = Double.valueOf(s.split("_")[1]);
                    if (Math.abs(a - b) < radius) {
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
        System.out.println("此次迭代个体总数目：" + sum);
    }


    /**
     * 将群体中的个体分配到不同小生境中 1.0_0.3
     * <p>
     * 步骤：
     * 1.选取leader
     * --注释: 最后有些个体其实不应该成为leader,待后续进行合并和剔除操作
     * <p>
     * 2.选取member
     * 保证数据总数不变
     * 选完全部leader之后统一进行，可以避免ConcurrentModificationException  √
     * <p>
     * 问题现象：存在一个点分布在两个小生境中   0.4 分别在0.301和0.0407中
     * 解决方案：每次判断完后，对这个个体标记已经清除
     */
    private void distributeNicheForGene() {

        // 选取leader
        for (int i = 0; i < sortListForGene.size(); i++) {
            // 选择第一个小生境leader
            if (leaderSetForGene.size() == 0) {
                leaderSetForGene.add(sortListForGene.get(i));
                System.out.println("leader: " + sortListForGene.get(i));

            } else {

                String aids = sortListForGene.get(i).split("_")[1];

                // 使用一个计数器,判断是否符合全部要求
                int max = 0;
                // 获取目前leader的信息
                for (String leader : leaderSetForGene) {

                    // b 的判断应该满足全部的leader
                    String bids = leader.split("_")[1];
                    // 判断两套试卷的相似程度,如果相似个体数达到一定数目，则判定为是同一个小生境 如1
                    // 将基因型转为list,使用list来判断相似个数
                    List<String> ListA = stringToList(aids);
                    List<String> ListB = stringToList(bids);

                    // 假设上面ListA 和 ListB都存在数据
                    // counter 大部分为0,优化方案：将试题数扩大 10*2=20
                    // aids：3,4,9,28,34,36,43,52,59,102,116,126,129,138,145,213,230,233,247,267
                    // bids：8,10,17,18,24,25,26,39,71,72,81,84,92,102,107,141,143,150,273,303


                    // 思考：使用什么来进行判断相似性？题目个数还是比例信息
                    // 题目个数？是没意义的,几乎都为0.
                    // 比例信息？估计也够呛,因为每轮都是经过校验的.
                    // count 在此置为0，是否会有偏差，需上移动
                    int counter = 0;
                    for (String a : ListB) {
                        for (String b : ListA) {
                            if (a.equals(b)) {
                                counter = counter + 1;
                            }
                        }
                    }
                    max = Math.max(max, counter);
                }
                // 一边修改一边循环可能会出异常
                // 重复的题目小于1,则不隶属于任何一个小生境,  FIXME 使用1可能过小 count 和 mark 需要相互对应
                if (max < 3) {
                    leaderSetForGene.add(sortListForGene.get(i));
                    System.out.println("leader: " + sortListForGene.get(i));
                }
            }
        }

        // 选取member
        int sum = 0;
        // leaderSetForGene 表示小生境的峰值
        // 待验证：为什么小生境个数到了第二代开始就不再变化
        log.info("小生境数目: " + leaderSetForGene.size());
        for (String leader : leaderSetForGene) {
            ArrayList<String> memberList = new ArrayList<>();
            String aids = leader.split("_")[1];
            for (int i = 0; i < sortListForGene.size(); i++) {
                String s = sortListForGene.get(i);
                // 判空操作 是因为后续会做标记,将value值重置为空  FIXME s 是ArrayList的一个值,循环遍历 200 * 小生境数
                if (!StringUtils.isBlank(s)) {
                    String bids = s.split("_")[1];

                    // 判断两套试卷的相似程度,如果相似个体数达到一定数目，则判定为是同一个小生境 如1
                    List<String> ListA = stringToList(aids);
                    List<String> ListB = stringToList(bids);

                    // 假设上面ListA和ListB都存在数据  计算A与B之间的相似个数
                    // mark 在此处置为0，是否会有偏差，需上移动
                    int mark = 0;
                    for (String a : ListA) {
                        for (String b : ListB) {
                            if (a.equals(b)) {
                                mark = mark + 1;
                            }
                        }
                    }
                    // 先在A中去匹配B里面全部数据，若匹配，则存放在集合里，并做标记  此处标记为null
                    // FIXME 原本是 >0
                    if (mark >= 3) {
                        memberList.add(s);
                        // java.lang.NumberFormatException: empty String
                        // 是否可以直接移除
                        sortListForGene.set(i, "");
                    }
                }
            }
            mapArrayListForGene.put(leader, memberList);
            // 验证个体总数是否丢失  FIXME sum存在偏差，待验证
            sum = memberList.size() + sum;
        }
        // 现象 sum = 200 始终成立，但leader成为了别人的member,以及自己不一定是自己的member
        // 原因: leader 成为member时,未进行置空  或者 先确定了leader，然后却成了member
        System.out.println("此次迭代个体总数目：" + sum);
    }


    /**
     * string 转 list
     */
    private List<String> stringToList(String strs) {
        String str[] = strs.split(",");
        return Arrays.asList(str);
    }


    /**
     * 判断哪些峰需要合并(矩阵+凹点问题) mapArrayList leaderSet
     * 用距离公式来表示任意两个小生境之间的关系,得出一个距离关系w矩阵（注：因为是01矩阵,所以后续需进一步去重）
     * <p>
     * 使用什么来保存矩阵关系呢？ 一个动态大小的二维数组
     * <p>
     * 优化:
     * 1.将距离关系矩阵,进一步简化,只将要合并的集合返回即可
     * 2.若存在共用相同的峰,直接选择最小的一个进行,另外一个过滤掉即可(集合的再次过滤)
     */
    private HashSet<String> judgeMerge() {

        // 距离关系w矩阵
        int[][] distanceMatrix = new int[leaderSet.size()][leaderSet.size()];

        // set 转 arrayList
        List<String> leaderList = new ArrayList<>(leaderSet);

        // 遍历计算距离关系,并生成01矩阵
        for (int i = 0; i < leaderList.size(); i++) {

            double min = 9999;
            int a = 0;
            int b = 0;

            Double aDouble = Double.valueOf(leaderList.get(i).split("_")[1]);

            for (int j = 0; j < leaderList.size(); j++) {

                if (!leaderList.get(i).equals(leaderList.get(j))) {
                    // 将距离存在一个集合之中，然后选取最小值(0,1)
                    double distance = Math.abs(aDouble - Double.valueOf(leaderList.get(j).split("_")[1]));
                    // 取出最小值
                    if (min > distance) {
                        a = i;
                        b = j;
                        min = distance;
                    }
                }
            }


            // 随机选取5个点进行凹点验证
            // 多个随机值当中,只要存在凹点,证明这两个相邻的小生境是独立的,不需要合并
            // 为了消除噪音干扰,定义了一个忍受因子 sf=0.9
            Double bDouble = Double.valueOf(leaderList.get(b).split("_")[1]);
            double sf = 0.9;

            // 本轮循环中未出现凹点,则需要合并
            boolean flag = true;
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
                if (cFitness < sf * aFitness && cFitness < sf * bFitness) {
                    flag = false;
                }
            }

            // 未出现凹点需要合并的最终因子,通过ab索引,赋值为1,其余赋值为0
            if (flag) {
                distanceMatrix[a][b] = 1;
            }
        }

        // 打印 遍历二维数组
        for (int i1 = 0; i1 < distanceMatrix.length; i1++) {
            for (int i2 = 0; i2 < distanceMatrix[i1].length; i2++) {
                System.out.print(distanceMatrix[i1][i2] + " , ");
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
                if (distanceMatrix[i1][i2] == 1) {
                    if (i1 < i2) {
                        hs.add(leaderList.get(i1) + ":" + leaderList.get(i2));
                    } else {
                        hs.add(leaderList.get(i2) + ":" + leaderList.get(i1));
                    }
                }
            }
        }
        System.out.println("验证去重效果 hs.size(): " + hs.size() + " hs.toString(): " + hs.toString());


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
        List<String> eqOneList = findEqOne(hs, gtOneList);

        // 最终需要合并的集合容器
        HashSet<String> allHs = new HashSet<>();
        allHs.addAll(eqOneList);

        // 2.分组计算出哪个最近,本质是进行一次过滤操作
        // 将hs和doubleList遍历,重复的进行进一步选择,每一个doubleList只返回一个距离最近的  set.get() != null
        // 对gtOneList做非空判断
        if (gtOneList != null) {
            for (Double aDouble : gtOneList) {
                ArrayList<String> tmpList = new ArrayList<>();
                // 将含有相同元素的集合分组
                for (String h : hs) {
                    if (h.contains(aDouble + "")) {
                        tmpList.add(h);
                    }
                }
                // 无需对tmpList做判空处理  tmpList至少为2
                String t = null;
                if (tmpList.size() != 0) {
                    // 遍历tmpList,进行比较,只留下最近的一个
                    double minDis = 9999;
                    for (String tmp : tmpList) {
                        String s1 = tmp.split(":")[0];
                        String s2 = tmp.split(":")[1];
                        double l1 = Double.valueOf(s1.split("_")[1]);
                        double l2 = Double.valueOf(s2.split("_")[1]);
                        if (minDis > Math.abs(l1 - l2)) {
                            minDis = Math.abs(l1 - l2);
                            t = tmp;
                        }
                    }
                }
                // 最近的一组t
                System.out.println("最近的一组以及找到:" + t);
                allHs.add(t);
            }
        }

        System.out.println(allHs);

        return allHs;
    }

    /**
     * 查找 只出现一次的峰
     * 1.排除掉不等于1的元素即可
     * 2.hs和doubleList的遍历判断过程
     */
    private List<String> findEqOne(HashSet<String> hs, List<Double> gtOneList) {

        // 未出现在gtOneList中
        boolean oneOnlyAppear = true;
        ArrayList<String> eqOneList = new ArrayList<>();
        for (String h : hs) {
            for (Double aDouble : gtOneList) {
                // 如果包含
                if (h.contains(aDouble + "")) {
                    oneOnlyAppear = false;
                }
            }
            if (oneOnlyAppear) {
                eqOneList.add(h);
            }
        }
        return eqOneList;
    }


    /**
     * 合并
     * 集合成员调整: 取大leader做总leader,两个List集合进行合并
     * 半径调整1:   选择大leader和小集群的最远值作为半径
     * 个体剔除操作+半径调整2: 理论基础:小生境中适应度最低的个体 = 离领导个体最远的个体
     * 目的：调整的是各个小生境的半径,剔除不需要的个体以及获取峰半径,
     * 便于随后的交叉变异,在某个具体的小生境范围内进行GA
     */
    private void merge(HashSet<String> hs) {

        // hs集合不需要进一步去重 hs.toString(): [0.964308398_0.507:6.9465303E-7_0.406]
        for (String h : hs) {
            // 根据适应度值确定leader   合并判断值错误,待优化
            String s1 = h.split(":")[0];
            String s2 = h.split(":")[1];
            double l1 = Double.valueOf(s1.split("_")[0]);
            double l2 = Double.valueOf(s2.split("_")[0]);
            if (l1 > l2) {
                // 集合成员调整
                mapArrayList.get(s1).addAll(mapArrayList.get(s2));
                // 半径调整1
                radiusMerge(s1, s2, l1);
            } else {
                // 集合成员调整
                mapArrayList.get(s2).addAll(mapArrayList.get(s1));
                // 半径调整1
                radiusMerge(s2, s1, l2);
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
            System.out.println("key: " + key + "  val：" + val);
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
            System.out.println("key: " + key + "  val：" + val);
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
            if (key.split("_").length == 3) {
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
                    if (tmpValue < lowerFitness) {
                        lowerFitness = tmpValue;
                        lowerIndividual = s;
                    }
                    if (tmpValue > highFitness) {
                        highFitness = tmpValue;
                        higherIndividual = s;
                    }
                }

                // 最终半径
                double finalRadius = Double.valueOf(higherIndividual.split("_")[1]) - Double.valueOf(lowerIndividual.split("_")[1]);

                // 根据距离判断是否需要剔除
                for (String s : splitArr) {
                    Double tmpValue = Double.valueOf(s.split("_")[1]);
                    double distance = tmpValue - Double.valueOf(lowerIndividual.split("_")[1]);
                    // 如果距离大于半径,则需要将其剔除
                    if (finalRadius < distance) {
                        // 侧输出流
                        sideList.add(s);
                    } else {
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

        sum = sum + sideList.size();
        System.out.println("个体总数：" + sum);

        // 更新计数器
        if (mapArrayList.size() == nn) {
            ct = ct + 1;
        } else {
            nn = mapArrayList.size();
            ct = 0;
        }

    }


    /**
     * 半径合并
     * s1是高峰,s2是低峰,原有峰不会变化,新增了一个合并后的键值对
     * 峰的移除和保留：
     * 移除目的：为了下一步的小生境剔除操作提供了便利
     * 保留目的：解决合并的过程中存在一个集合多次合并,若提前移除,导致空值现象、
     * judge过程已经做了去重,所以应该可以不再考虑此逻辑
     * <p>
     * 将新半径保存到map中,改变其数据结构 value_key_radius
     * map是无法直接修改key值的，所以要采用其他的方案，新增一个键值对，再删除之前那个要修改的
     * 采用迭代器的方式遍历，在迭代中it.remove(),map.put()操作
     */
    private void radiusMerge(String s1, String s2, double l1) {

        // 进行迭代,选择最远的个体即可,最大距离 = 新半径
        double maxDiff = 0;
        for (String s : mapArrayList.get(s2)) {
            double tmpSite = Double.valueOf(s.split("_")[1]);
            double tmpDiff = Math.abs(tmpSite - l1);
            if (maxDiff < tmpDiff) {
                maxDiff = tmpDiff;
            }
        }

        // 新增一个合并后的键值对
        String s = s1 + "_" + maxDiff;
        mapArrayList.put(s, mapArrayList.get(s1));

        // 现象：个体数小于200，待核实
        //System.out.println(mapArrayList);

    }


    /**
     * java统计List集合中每个元素出现的次数
     * 例如frequencyOfListElements("111","111","222")
     * <p>
     * 则返回Map {"111"=2,"222"=1}
     */
    public Map<Double, Integer> frequencyOfListElements(List<Double> items) {
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
     * <p>
     * 则返回次数大于1的key {"111"=2,"222"=1} --> 111
     */
    public List<Double> frequencyGtOne(List<Double> items) {
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
        for (Map.Entry<Double, Integer> entry : map.entrySet()) {
            System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
            if (entry.getValue() > 1) {
                list.add(entry.getKey());
            }
        }
        return list;
    }


    /**
     * 使用构造法选取题目  (轮盘赌） 生成 paperGenetic
     * 1.题型构造解决 （不考虑下限比例）
     * 2.属性构造解决 （不考虑下限比例）
     * 设置比例  可以通过惩罚系数来设定  超出,则急剧减少
     * 总结：在初始化的时候，不需要完全保证题型和属性符合要求，后续使用GA迭代和轮盘赌解决即可
     */
    private void initItemBank4() throws SQLException {

        System.out.println("====== 开始选题,构成试卷  轮盘赌构造 ======");

        // 试卷|个体大小  提供遗传变异的基本单位
        int paperNum = paperGenetic.length;

        // 试题|基因大小
        int questionsNum = paperGenetic[1].length;

        // 单套试卷的集合
        HashSet<String> itemSet = new HashSet<>();

        // 题库310道题  50:100:100:50:10   硬性约束：长度  软性约束：题型、属性比例
        // 获取题库所有试题  [8:CHOSE:(1,0,0,0,0),....] 旁路缓存的概念
        bankList = getBank();

        // 生成了二维数组 String[][] paperGenetic = new String[100][20]
        for (int j = 0; j < paperNum; j++) {

            // 清空上一次迭代的数据
            itemSet.clear();

            for (int i = 0; i < questionsNum; i++) {
                // 减少频繁的gc
                String item;
                // 去重操作
                while (itemSet.size() == i) {
                    // 获取试题id   轮盘赌构造
                    int sqlId = roulette(itemSet);
                    // 两个id相差1,保证选题无偏差
                    item = jdbcUtils.selectOneItem(sqlId + 1);
                    itemSet.add(item);
                }
            }

            // 将hashSet转ArrayList 并排序
            ArrayList<String> list = new ArrayList<>(itemSet);

            // idList容器
            ArrayList<Integer> idList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                idList.add(Integer.valueOf(list.get(i).split(":")[0]));
            }

            //list  排序  目前这套试卷抽取到的试题id
            Collections.sort(idList);


            // 根据id从数据库中查询相对应的试题
            String ids = idList.toString().substring(1, idList.toString().length() - 1);
            ArrayList<String> bachItemList = jdbcUtils.selectBachItem(ids);


            // 交叉变异的针对的是试题   即试卷=个体  试题=基因
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
     */
    private ArrayList<String> getBank() throws SQLException {

        return jdbcUtils.select();

    }


    /**
     * 逻辑判断 ：轮盘赌构造选题
     * 1.id不能重复
     * 2.题型|属性比例 影响适应度
     */
    private int roulette(HashSet<String> itemSet) {

        //轮盘赌 累加百分比
        double[] fitPie = new double[bankList.size()];

        //计算每道试题的适应度占比   1*0.5*0.8
        double[] fitnessArray = getRouletteFitness(itemSet);

        //id去重操作
        HashSet<Integer> idSet = new HashSet<>();

        //迭代器遍历HashSet  确保取出目前题目中不存在的试题
        Iterator<String> it = itemSet.iterator();
        while (it.hasNext()) {
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
        fitPie[310 - 1] = 1;

        //随机生成的random概率值  [0,1)
        double randomProbability = Math.random();

        //轮盘赌 越大的适应度，其叠加时增长越快，即有更大的概率被选中
        int answerSelectId = 0;
        // i 从0开始,故后续真正取数的时候需要+1
        int i = 0;

        while (i < bankList.size() && randomProbability > fitPie[i]) {
            //id 去重
            if (idSet.contains(i)) {
                i += 1;
            } else {
                i++;
                answerSelectId = i;
            }
        }

        return answerSelectId;

    }


    /**
     * 1.根据已选试题，计算题库中每道试题的适应度值比例
     * 2.每道题的概率为 1*penalty^n,总概率为310道题叠加
     * 2.1  初始化的时候将全局的310题查询出来（id:type:pattern）
     * 2.2  求出每道试题的概率 1 * 惩罚系数
     * 2.3  求出每道试题的适应度占比
     * 题型比例 选择[0.2,0.4]   填空[0.2,0.4]  简答[0.1,0.3] 应用[0.1,0.3]
     * 属性比例 第1属性[0.2,0.4]第2属性[0.2,0.4] 第3属性[0.1,0.3] 第4属性[0.1,0.3] 第5属性[0.1,0.3]
     */
    private double[] getRouletteFitness(HashSet<String> itemSet) {

        // 所有试题的适应度总和
        double fitSum = 0.0;

        // 每道试题的适应度值
        double[] fitTmp = new double[bankList.size()];

        // 每道试题的适应度占比   疑问:1/310 会很小,random() 这样产生的值是否符合要求
        double[] fitPro = new double[bankList.size()];

        // 数据库属性排列的规则,因为基数大,导致随机选取的题目不具有代表性
        // 解决方案:题目顺序打乱（修改数据库的属性排序）

        //题型个数
        int typeChose = 0;
        int typeFill = 0;
        int typeShort = 0;
        int typeCompre = 0;


        //此次迭代各个题型的数目
        for (String s : itemSet) {

            //计算每种题型个数
            if (TYPE.CHOSE.toString().equals(s.split(":")[1])) {
                typeChose += 1;
            }
            if (TYPE.FILL.toString().equals(s.split(":")[1])) {
                typeFill += 1;
            }
            if (TYPE.SHORT.toString().equals(s.split(":")[1])) {
                typeShort += 1;
            }
            if (TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])) {
                typeCompre += 1;
            }
        }

        //题型比例
        double typeChoseRation = typeChose / 10.0;
        double typeFileRation = typeFill / 10.0;
        double typeShortRation = typeShort / 10.0;
        double typeCompreRation = typeCompre / 10.0;


        //属性个数
        int attributeNum1 = 0;
        int attributeNum2 = 0;
        int attributeNum3 = 0;
        int attributeNum4 = 0;
        int attributeNum5 = 0;

        //此次迭代各个属性的数目
        for (String s : itemSet) {

            //计算每种题型个数
            if ("1".equals(s.split(":")[2].substring(1, 2))) {
                attributeNum1 += 1;
            }
            if ("1".equals(s.split(":")[2].substring(3, 4))) {
                attributeNum2 += 1;
            }
            if ("1".equals(s.split(":")[2].substring(5, 6))) {
                attributeNum3 += 1;
            }
            if ("1".equals(s.split(":")[2].substring(7, 8))) {
                attributeNum4 += 1;
            }
            if ("1".equals(s.split(":")[2].substring(9, 10))) {
                attributeNum5 += 1;
            }
        }
        //System.out.println("ar1: "+attributeNum1+"\tar2: "+attributeNum2+"\tar3: "+attributeNum3+"\tar4: "+attributeNum4+"\tar5: "+attributeNum5);

        //属性比例
        double attributeRatio1 = attributeNum1 / 23.0;
        double attributeRatio2 = attributeNum2 / 23.0;
        double attributeRatio3 = attributeNum3 / 23.0;
        double attributeRatio4 = attributeNum4 / 23.0;
        double attributeRatio5 = attributeNum5 / 23.0;


        // 题型和属性比例 和轮盘赌搭建关系：
        //      已抽取的属性个数越多，则惩罚系数越大 且各个属性是累乘关系
        //      比例和一个固定值做比较即可  eg: typeChose/10    AttributeRatio1/23
        //      如果未超出比例，则按照正常流程走，一旦超过，则适应度值急剧下降
        for (int j = 0; j < bankList.size(); j++) {
            double penalty = 1;
            String[] splits = bankList.get(j).split(":");

            // 题型比例
            // 为什么小于也要做惩罚，因为选取了一次，需要实时统计比例信息，并获取惩罚系数
            if (splits[1].contains(TYPE.CHOSE + "")) {
                if (typeChoseRation < 0.4) {
                    penalty = penalty * Math.pow(0.5, typeChose);
                } else {
                    penalty = penalty * Math.pow(0.5, typeChose * typeChose);
                }
            }
            if (splits[1].contains(TYPE.FILL + "")) {
                if (typeFileRation < 0.4) {
                    penalty = penalty * Math.pow(0.5, typeFill);
                } else {
                    penalty = penalty * Math.pow(0.5, typeFill * typeFill);
                }
            }
            if (splits[1].contains(TYPE.SHORT + "")) {
                if (typeShortRation < 0.3) {
                    penalty = penalty * Math.pow(0.5, typeShort);
                } else {
                    penalty = penalty * Math.pow(0.5, typeShort * typeShort);
                }
            }
            if (splits[1].contains(TYPE.COMPREHENSIVE + "")) {
                if (typeCompreRation < 0.3) {
                    penalty = penalty * Math.pow(0.5, typeCompre);
                } else {
                    penalty = penalty * Math.pow(0.5, typeCompre * typeCompre);
                }
            }

            //属性比例
            if ("1".equals(splits[2].substring(1, 2))) {
                if (attributeRatio1 < 0.4) {
                    penalty = penalty * Math.pow(0.8, attributeNum1);
                } else {
                    penalty = penalty * Math.pow(0.8, attributeNum1 * attributeNum1);
                }
            }
            if ("1".equals(splits[2].substring(3, 4))) {
                if (attributeRatio2 < 0.4) {
                    penalty = penalty * Math.pow(0.8, attributeNum2);
                } else {
                    penalty = penalty * Math.pow(0.8, attributeNum2 * attributeNum2);
                }
            }
            if ("1".equals(splits[2].substring(5, 6))) {
                if (attributeRatio3 < 0.3) {
                    penalty = penalty * Math.pow(0.8, attributeNum3);
                } else {
                    penalty = penalty * Math.pow(0.8, attributeNum3 * attributeNum3);
                }
            }
            if ("1".equals(splits[2].substring(7, 8))) {
                if (attributeRatio4 < 0.3) {
                    penalty = penalty * Math.pow(0.8, attributeNum4);
                } else {
                    penalty = penalty * Math.pow(0.8, attributeNum4 * attributeNum4);
                }
            }
            if ("1".equals(splits[2].substring(9, 10))) {
                if (attributeRatio5 < 0.3) {
                    penalty = penalty * Math.pow(0.8, attributeNum5);
                } else {
                    penalty = penalty * Math.pow(0.8, attributeNum5 * attributeNum5);
                }
            }


            //个体值 和 总和
            fitTmp[j] = penalty;
            fitSum = fitSum + penalty;

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


        return fitPro;
    }


    /**
     * 返回：每套试卷的适应度_ids
     * <p>
     * 方案  进行乘以一个exp 来进行适应度值的降低，高等数学里以自然常数e为底的指数函数
     * 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3]  应用[0.1,0.3]
     * 属性比例 第1属性[0.2,0.4]  第2属性[0.2,0.4]  第3属性[0.1,0.3] 第4属性[0.1,0.3] 第5属性[0.1,0.3]
     */
    private void getFitnessForGene(int paperSize) {

        //log.info("适应值 log4j")

        // 所有试卷的适应度总和
        double fitSum = 0.0;
        // 拼接字符串 每套试卷的适应度值_本身
        String[] fitTmp = new String[paperSize];
        // 每套试卷的适应度占比
        double[] fitPro = new double[paperSize];

        // 计算试卷的适应度值，即衡量试卷优劣的指标之一 Fs
        for (int i = 0; i < paperSize; i++) {

            double adi1r = 0;
            double adi2r = 0;
            double adi3r = 0;
            double adi4r = 0;
            double adi5r = 0;

            StringBuilder idsb = new StringBuilder();

            // 获取原始adi  数组里面裹数组
            String[] itemList = paperGenetic[i];
            for (int j = 0; j < itemList.length; j++) {

                String[] splits = itemList[j].split(":");
                adi1r = adi1r + Double.parseDouble(splits[3]);
                adi2r = adi2r + Double.parseDouble(splits[4]);
                adi3r = adi3r + Double.parseDouble(splits[5]);
                adi4r = adi4r + Double.parseDouble(splits[6]);
                adi5r = adi5r + Double.parseDouble(splits[7]);

                // 拼接ids
                idsb.append(",").append(splits[0]);

            }

            String ids = idsb.toString().substring(1);
            System.out.println(ids);

            // 题型个数
            String[] expList = paperGenetic[i];
            int typeChose = 0;
            int typeFill = 0;
            int typeShort = 0;
            int typeCompre = 0;


            //此次迭代各个题型的数目
            for (String s : expList) {

                //计算每种题型个数
                if (TYPE.CHOSE.toString().equals(s.split(":")[1])) {
                    typeChose += 1;
                }
                if (TYPE.FILL.toString().equals(s.split(":")[1])) {
                    typeFill += 1;
                }
                if (TYPE.SHORT.toString().equals(s.split(":")[1])) {
                    typeShort += 1;
                }
                if (TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])) {
                    typeCompre += 1;
                }
            }

            // 题型比例/10  属性比例/23 是固定值,到了后期需要修正
            // 题型比例
            double typeChoseRation = typeChose / 10.0;
            double typeFileRation = typeFill / 10.0;
            double typeShortRation = typeShort / 10.0;
            double typeCompreRation = typeCompre / 10.0;

            // 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3]  应用[0.1,0.3]
            // 先判断是否在范围内，在的话，为0，不在的话，然后进一步和上下限取差值，绝对值
            double td1;
            if (typeChoseRation >= 0.2 && typeChoseRation < 0.4) {
                td1 = 0;
            } else if (typeChoseRation < 0.2) {
                td1 = Math.abs(0.2 - typeChoseRation);
            } else {
                td1 = Math.abs(typeChoseRation - 0.4);
            }

            double td2;
            if (typeFileRation >= 0.2 && typeFileRation < 0.4) {
                td2 = 0;
            } else if (typeFileRation < 0.2) {
                td2 = Math.abs(0.2 - typeFileRation);
            } else {
                td2 = Math.abs(typeFileRation - 0.4);
            }

            double td3;
            if (typeShortRation >= 0.1 && typeShortRation < 0.3) {
                td3 = 0;
            } else if (typeShortRation < 0.1) {
                td3 = Math.abs(0.1 - typeShortRation);
            } else {
                td3 = Math.abs(typeShortRation - 0.3);
            }

            double td4;
            if (typeCompreRation >= 0.1 && typeCompreRation < 0.3) {
                td4 = 0;
            } else if (typeCompreRation < 0.1) {
                td4 = Math.abs(0.1 - typeCompreRation);
            } else {
                td4 = Math.abs(typeCompreRation - 0.3);
            }


            // 属性个数
            int exp1 = 0;
            int exp2 = 0;
            int exp3 = 0;
            int exp4 = 0;
            int exp5 = 0;

            for (int j = 0; j < expList.length; j++) {
                String[] splits = expList[j].split(":");
                exp1 = exp1 + Integer.parseInt(splits[2].split(",")[0].substring(1, 2));
                exp2 = exp2 + Integer.parseInt(splits[2].split(",")[1]);
                exp3 = exp3 + Integer.parseInt(splits[2].split(",")[2]);
                exp4 = exp4 + Integer.parseInt(splits[2].split(",")[3]);
                exp5 = exp5 + Integer.parseInt(splits[2].split(",")[4].substring(0, 1));
            }

            // 属性比例 第1属性[0.2,0.4]   第2属性[0.2,0.4]   第3属性[0.1,0.3]  第4属性[0.1,0.3]  第5属性[0.1,0.3]
            //先判断是否在范围内，在的话，为0，不在的话，然后进一步和上下限取差值，绝对值
            double ed1;
            double edx1 = exp1 / 23.0;
            if (edx1 >= 0.2 && edx1 < 0.4) {
                ed1 = 0;
            } else if (edx1 < 0.2) {
                ed1 = Math.abs(0.2 - edx1);
            } else {
                ed1 = Math.abs(edx1 - 0.4);
            }

            double ed2;
            double edx2 = exp2 / 23.0;
            if (edx2 >= 0.2 && edx2 < 0.4) {
                ed2 = 0;
            } else if (edx2 < 0.2) {
                ed2 = Math.abs(0.2 - edx2);
            } else {
                ed2 = Math.abs(edx2 - 0.4);
            }

            double ed3;
            double edx3 = exp3 / 23.0;
            if (edx3 >= 0.1 && edx3 < 0.3) {
                ed3 = 0;
            } else if (edx3 < 0.1) {
                ed3 = Math.abs(0.1 - edx3);
            } else {
                ed3 = Math.abs(edx3 - 0.3);
            }

            double ed4;
            double edx4 = exp4 / 23.0;
            if (edx4 >= 0.1 && edx4 < 0.3) {
                ed4 = 0;
            } else if (edx4 < 0.1) {
                ed4 = Math.abs(0.1 - edx4);
            } else {
                ed4 = Math.abs(edx4 - 0.3);
            }

            double ed5;
            double edx5 = exp5 / 23.0;
            if (edx5 >= 0.1 && edx5 < 0.3) {
                ed5 = 0;
            } else if (edx5 < 0.1) {
                ed5 = Math.abs(0.1 - edx5);
            } else {
                ed5 = Math.abs(edx5 - 0.3);
            }

            //System.out.println("题型和属性超额情况： td1:"+td1+" td2:"+td2+" td3:"+td3+" td4:"+td4 + "ed1:"+ed1+" ed2:"+ed2+" ed3:"+ed3+" ed4:"+ed4+" ed5:"+ed5)

            // 惩罚个数  只有比例不符合要求时才惩罚，故不会有太大的影响
            double expNum = -(td1 + td2 + td3 + td4 + ed1 + ed2 + ed3 + ed4 + ed5);

            //System.out.printf("exp(%.3f) 为 %.3f%n", expNum, Math.exp(expNum))


            //均值 和 最小值
            double avgrum = (adi1r + adi2r + adi3r + adi4r + adi5r) / 5;
            double minrum = Math.min(Math.min(Math.min(Math.min(adi1r, adi2r), adi3r), adi4r), adi5r) * 100;

            //System.out.println("minrum: "+minrum)

            //适应度值 (min * 惩罚系数)
            minrum = minrum * Math.exp(expNum);

            // 个体
            // 本身的基因型选用id拼接,其具有代表性
            fitTmp[i] = minrum + "_" + ids;
            sortListForGene.add(minrum + "_" + ids);

        }

        //return  fitTmp;
    }


    /**
     * 选择: 以适应度为导向,轮盘赌为策略, 适者生存和多样性的权衡
     * <p>
     * ①计算适应度：以试卷为单位，min*exp^1
     * ②轮盘赌进行筛选 paperGenetic = newPaperGenetic;
     */
    public void selection() {

        System.out.println("====================== select ======================");

        // 200套试卷
        int paperSize = paperGenetic.length;

        // 累加百分比,为轮盘赌做准备
        double[] fitPie = new double[paperSize];

        // 每套试卷的适应度占比  min*exp^1
        double[] fitPro = getFitness(paperSize);

        // 累加初始值
        double accumulate = 0;

        // 试卷占总试卷的适应度累加百分比
        for (int i = 0; i < paperSize; i++) {
            fitPie[i] = accumulate + fitPro[i];
            accumulate += fitPro[i];
        }

        // 累加的概率为1 数组下标从0开始
        fitPie[paperSize - 1] = 1;

        // 初始化容器 随机生成的random概率值
        double[] randomId = new double[paperSize];

        // 不需要去重
        for (int i = 0; i < paperSize; i++) {
            randomId[i] = Math.random();
        }

        // 排序
        Arrays.sort(randomId);


        // 轮盘赌 越大的适应度,其叠加时增长越快,即有更大的概率被选中
        // 同一套试卷可能会被选取多次（轮盘赌的意义）
        // GA 的通病：多样性的维持
        //      择优录取,个体越来越相似,所以才需要变异,但变异后的个体，因为经过轮盘赌,也不一定能够保存下来
        String[][] newPaperGenetic = new String[paperSize][];
        int newSelectId = 0;

        for (int i = 0; i < paperSize; i++) {
            while (newSelectId < paperSize && randomId[newSelectId] < fitPie[i]) {
                // 需要确保fitPie[i] 和 paperGenetic[i] 对应的i 是同一套试卷
                newPaperGenetic[newSelectId] = paperGenetic[i];
                newSelectId += 1;
            }
        }

        // 重新赋值种群的编码
        paperGenetic = newPaperGenetic;

    }


    /**
     * 小生境内选择
     * 选择: 以适应度为导向,轮盘赌为策略, 适者生存和多样性的权衡
     * <p>
     * ①计算适应度：以试卷为单位，min*exp^1
     * ②轮盘赌进行筛选 inBack
     */
    public HashMap<String, ArrayList<String>> selectionIn(HashMap<String, ArrayList<String>> inListHashMap) {

        System.out.println("====================== select In ======================");

        HashMap<String, ArrayList<String>> inBack = new HashMap<>();

        // 外围嵌上一层循环，然后转化为多个list，挨个执行 selectionOut 即可
        // 因为适应度是降序排列的,执行精英策略时是否会有影响  后续考虑

        for(Map.Entry<String, ArrayList<String>> entry : inListHashMap.entrySet()){
            //System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
            ArrayList<String> outBack = selectionOut(entry.getValue());
            inBack.put(entry.getKey(), outBack);
        }

        return inBack;
    }





    /**
     * 小生境外选择
     * 选择: 以适应度为导向,轮盘赌为策略, 适者生存和多样性的权衡
     *
     * ①计算适应度：以试卷为单位，min*exp^1
     * ②轮盘赌进行筛选 返回 outBack
     */
    public ArrayList<String> selectionOut(ArrayList<String> outList) {

        System.out.println("====================== select Out ======================");

        ArrayList<String> outBack = new ArrayList<>();

        // 试卷套数
        int paperSize = outList.size();

        // 累加百分比,为轮盘赌做准备
        double[] fitPie = new double[paperSize];

        // 每套试卷的适应度占比  min*exp^1
        double[] fitPro = getFitnessNiche(outList);

        // 累加初始值
        double accumulate = 0;

        // 试卷占总试卷的适应度累加百分比
        for (int i = 0; i < paperSize; i++) {
            fitPie[i] = accumulate + fitPro[i];
            accumulate += fitPro[i];
        }

        // 累加的概率为1 数组下标从0开始
        fitPie[paperSize - 1] = 1;

        // 初始化容器 随机生成的random概率值
        double[] randomId = new double[paperSize];

        // 不需要去重
        for (int i = 0; i < paperSize; i++) {
            randomId[i] = Math.random();
        }

        // 排序
        Arrays.sort(randomId);


        // 轮盘赌 越大的适应度,其叠加时增长越快,即有更大的概率被选中
        // 同一套试卷可能会被选取多次（轮盘赌的意义）
        // GA 的通病：多样性的维持
        //      择优录取,个体越来越相似,所以才需要变异,但变异后的个体，因为经过轮盘赌,也不一定能够保存下来
        //String[][] newPaperGenetic = new String[paperSize][];
        int newSelectId = 0;

        for (int i = 0; i < paperSize; i++) {
            while (newSelectId < paperSize && randomId[newSelectId] < fitPie[i]) {
                // 需要确保fitPie[i] 和 paperGenetic[i] 对应的i 是同一套试卷  此处不能使用paperGenetic[i]进行取值，会错位
                //newPaperGenetic[newSelectId] = paperGenetic[i];
                newSelectId += 1;
                // 还需要保证顺序吗？保证顺序的目的是什么   为了交叉变异是同一个个体
                outBack.add(outList.get(i));
            }
        }

        // 重新赋值种群的编码  因错位的原因，paperGenetic赋值也意义不大
        //paperGenetic = newPaperGenetic;

        return outBack;

    }


    /**
     * 每套试卷的适应度占比
     * <p>
     * selection 计算适应度值
     * 方案  进行乘以一个exp 来进行适应度值的降低，高等数学里以自然常数e为底的指数函数
     * 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3]  应用[0.1,0.3]
     * 属性比例 第1属性[0.2,0.4]  第2属性[0.2,0.4]  第3属性[0.1,0.3] 第4属性[0.1,0.3] 第5属性[0.1,0.3]
     *
     *
     */
    private double[] getFitness(int paperSize) {

        //log.info("适应值 log4j")

        // 所有试卷的适应度总和
        double fitSum = 0.0;
        // 每套试卷的适应度值
        double[] fitTmp = new double[paperSize];
        // 每套试卷的适应度占比  返回结果部分
        double[] fitPro = new double[paperSize];

        // 计算试卷的适应度值，即衡量试卷优劣的指标之一 Fs
        for (int i = 0; i < paperSize; i++) {

            double adi1r = 0;
            double adi2r = 0;
            double adi3r = 0;
            double adi4r = 0;
            double adi5r = 0;

            // 获取原始adi
            String[] itemList = paperGenetic[i];
            for (int j = 0; j < itemList.length; j++) {

                String[] splits = itemList[j].split(":");
                adi1r = adi1r + Double.parseDouble(splits[3]);
                adi2r = adi2r + Double.parseDouble(splits[4]);
                adi3r = adi3r + Double.parseDouble(splits[5]);
                adi4r = adi4r + Double.parseDouble(splits[6]);
                adi5r = adi5r + Double.parseDouble(splits[7]);

            }


            // 题型个数
            String[] expList = paperGenetic[i];
            int typeChose = 0;
            int typeFill = 0;
            int typeShort = 0;
            int typeCompre = 0;


            //此次迭代各个题型的数目
            for (String s : expList) {

                //计算每种题型个数
                if (TYPE.CHOSE.toString().equals(s.split(":")[1])) {
                    typeChose += 1;
                }
                if (TYPE.FILL.toString().equals(s.split(":")[1])) {
                    typeFill += 1;
                }
                if (TYPE.SHORT.toString().equals(s.split(":")[1])) {
                    typeShort += 1;
                }
                if (TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])) {
                    typeCompre += 1;
                }
            }

            // 题型比例
            double typeChoseRation = typeChose / 10.0;
            double typeFileRation = typeFill / 10.0;
            double typeShortRation = typeShort / 10.0;
            double typeCompreRation = typeCompre / 10.0;

            // 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3]  应用[0.1,0.3]
            // 先判断是否在范围内，在的话，为0，不在的话，然后进一步和上下限取差值，绝对值
            double td1;
            if (typeChoseRation >= 0.2 && typeChoseRation < 0.4) {
                td1 = 0;
            } else if (typeChoseRation < 0.2) {
                td1 = Math.abs(0.2 - typeChoseRation);
            } else {
                td1 = Math.abs(typeChoseRation - 0.4);
            }

            double td2;
            if (typeFileRation >= 0.2 && typeFileRation < 0.4) {
                td2 = 0;
            } else if (typeFileRation < 0.2) {
                td2 = Math.abs(0.2 - typeFileRation);
            } else {
                td2 = Math.abs(typeFileRation - 0.4);
            }

            double td3;
            if (typeShortRation >= 0.1 && typeShortRation < 0.3) {
                td3 = 0;
            } else if (typeShortRation < 0.1) {
                td3 = Math.abs(0.1 - typeShortRation);
            } else {
                td3 = Math.abs(typeShortRation - 0.3);
            }

            double td4;
            if (typeCompreRation >= 0.1 && typeCompreRation < 0.3) {
                td4 = 0;
            } else if (typeCompreRation < 0.1) {
                td4 = Math.abs(0.1 - typeCompreRation);
            } else {
                td4 = Math.abs(typeCompreRation - 0.3);
            }


            // 属性个数
            int exp1 = 0;
            int exp2 = 0;
            int exp3 = 0;
            int exp4 = 0;
            int exp5 = 0;

            for (int j = 0; j < expList.length; j++) {
                String[] splits = expList[j].split(":");
                exp1 = exp1 + Integer.parseInt(splits[2].split(",")[0].substring(1, 2));
                exp2 = exp2 + Integer.parseInt(splits[2].split(",")[1]);
                exp3 = exp3 + Integer.parseInt(splits[2].split(",")[2]);
                exp4 = exp4 + Integer.parseInt(splits[2].split(",")[3]);
                exp5 = exp5 + Integer.parseInt(splits[2].split(",")[4].substring(0, 1));
            }

            // 属性比例 第1属性[0.2,0.4]   第2属性[0.2,0.4]   第3属性[0.1,0.3]  第4属性[0.1,0.3]  第5属性[0.1,0.3]
            //先判断是否在范围内，在的话，为0，不在的话，然后进一步和上下限取差值，绝对值
            double ed1;
            double edx1 = exp1 / 23.0;
            if (edx1 >= 0.2 && edx1 < 0.4) {
                ed1 = 0;
            } else if (edx1 < 0.2) {
                ed1 = Math.abs(0.2 - edx1);
            } else {
                ed1 = Math.abs(edx1 - 0.4);
            }

            double ed2;
            double edx2 = exp2 / 23.0;
            if (edx2 >= 0.2 && edx2 < 0.4) {
                ed2 = 0;
            } else if (edx2 < 0.2) {
                ed2 = Math.abs(0.2 - edx2);
            } else {
                ed2 = Math.abs(edx2 - 0.4);
            }

            double ed3;
            double edx3 = exp3 / 23.0;
            if (edx3 >= 0.1 && edx3 < 0.3) {
                ed3 = 0;
            } else if (edx3 < 0.1) {
                ed3 = Math.abs(0.1 - edx3);
            } else {
                ed3 = Math.abs(edx3 - 0.3);
            }

            double ed4;
            double edx4 = exp4 / 23.0;
            if (edx4 >= 0.1 && edx4 < 0.3) {
                ed4 = 0;
            } else if (edx4 < 0.1) {
                ed4 = Math.abs(0.1 - edx4);
            } else {
                ed4 = Math.abs(edx4 - 0.3);
            }

            double ed5;
            double edx5 = exp5 / 23.0;
            if (edx5 >= 0.1 && edx5 < 0.3) {
                ed5 = 0;
            } else if (edx5 < 0.1) {
                ed5 = Math.abs(0.1 - edx5);
            } else {
                ed5 = Math.abs(edx5 - 0.3);
            }

            //System.out.println("题型和属性超额情况： td1:"+td1+" td2:"+td2+" td3:"+td3+" td4:"+td4 + "ed1:"+ed1+" ed2:"+ed2+" ed3:"+ed3+" ed4:"+ed4+" ed5:"+ed5);

            // 惩罚个数  只有比例不符合要求时才惩罚，故不会有太大的影响
            double expNum = -(td1 + td2 + td3 + td4 + ed1 + ed2 + ed3 + ed4 + ed5);

            //System.out.printf("exp(%.3f) 为 %.3f%n", expNum, Math.exp(expNum));


            //均值 和 最小值
            double avgrum = (adi1r + adi2r + adi3r + adi4r + adi5r) / 5;
            double minrum = Math.min(Math.min(Math.min(Math.min(adi1r, adi2r), adi3r), adi4r), adi5r) * 100;

            //System.out.println("minrum: "+minrum);

            //适应度值 (min * 惩罚系数)
            minrum = minrum * Math.exp(expNum);
            //个体、总和
            fitTmp[i] = minrum;
            fitSum = fitSum + minrum;


        }

        //System.out.println("全局最优："+GlobalOptimal);

        for (int i = 0; i < paperSize; i++) {
            //  各自的比例
            fitPro[i] = fitTmp[i] / fitSum;
        }

        //冒泡排序 打印top10
        klUtils.bubbleSort(fitTmp);

        return fitPro;
    }




    /**
     * 每套试卷的适应度占比
     *
     * selection 计算适应度值
     * 方案  进行乘以一个exp 来进行适应度值的降低，高等数学里以自然常数e为底的指数函数
     * 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3]  应用[0.1,0.3]
     * 属性比例 第1属性[0.2,0.4]  第2属性[0.2,0.4]  第3属性[0.1,0.3] 第4属性[0.1,0.3] 第5属性[0.1,0.3]
     *
     *
     * 方案一:
     * 将 list 转化为 gene 数组( 其中借助select * from table,初次是没问题的，但其会随着交叉变异而个体的变化，故需定义一个全局变量 时刻维护 )
     *  paperGenetic[i] = 1:FILL:(0,0,0,0,1):0.0:0.0:0.0:0.0:0.055000000000000035 * 20
     *  value[i] = 19.941320442946314_8,16,19,21,29,30,35,50,62,69,76,107,108,133,136,173,207,222,242,299
     *
     * 方案二:
     * 直接使用 list 计算,因为首位已经是计算过的适应度值  此处采用
     *
     */
    private double[] getFitnessNiche( ArrayList<String> outList ) {

        //log.info("适应值 log4j")
        int paperSize = outList.size();

        // 所有试卷的适应度总和
        double fitSum = 0.0;

        // 每套试卷的适应度值
        double[] fitTmp = new double[paperSize];

        // 每套试卷的适应度占比  返回结果部分
        double[] fitPro = new double[paperSize];

        // 计算试卷的适应度值，即衡量试卷优劣的指标之一 Fs
        for (int i = 0; i < paperSize; i++) {
            // 对outList进行拆分,获取其前半部分的适应度值，并赋值给fitTmp[]
            fitTmp[i] = Double.parseDouble(outList.get(i).split("_")[0]);
            fitSum =  fitSum +  fitTmp[i];
        }


        for (int i = 0; i < paperSize; i++) {
            // 各自的比例
            fitPro[i] = fitTmp[i] / fitSum;
        }

        //冒泡排序 打印top10
        klUtils.bubbleSort(fitTmp);

        return fitPro;
    }




    /**
     * 交叉+修补  此处不涉及适应度
     * ①交叉的单位:  试题
     * ②修补的方面:  长度,题型,属性比例
     * <p>
     * random.nextInt(100)  指生成一个介于[0,n)的int值
     * 选择（轮盘赌）：择优录取+多样式减低
     * 交叉+变异：增加多样性(外部作用)
     */
    private void crossCover(Papers papers, int k) throws SQLException {

        //  单点交叉(只保留交叉一个个体)
        int point = paperGenetic[1].length;
        // 根据概率判断是否进行交叉
        if (Math.random() < papers.getPc()) {
            String[] temp = new String[point];
            int a = new Random().nextInt(point);

            for (int j = 0; j < a; j++) {
                temp[j] = paperGenetic[k][j];
            }

            for (int j = a; j < point; j++) {
                temp[j] = paperGenetic[k + 1][j];
            }
            // 放在内存执行,每执行一次pc 则校验一次
            // 对tmp进行排序
            paperGenetic[k] = sortPatch(temp);
            // 此处需要校验属性和类型 交叉和变异各执行一次全方面修补，可能就是这个原因导致的多样性如此之高
            // 变异具有随机性,适应度无法得到充分保证
            // correct(k);
        }

    }


    /**
     * 交叉 底层调用 crossCoverOut
     *
     */
    private HashMap<String, ArrayList<String>> crossCoverIn(HashMap<String, ArrayList<String>>  inListHashMap) throws SQLException {


        HashMap<String, ArrayList<String>> inBack = new HashMap<>();

        // 外围嵌上一层循环，然后转化为多个list，挨个执行 selectionOut 即可
        // 因为适应度是降序排列的,执行精英策略时是否会有影响  后续考虑

        for(Map.Entry<String, ArrayList<String>> entry : inListHashMap.entrySet()){
            //System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
            ArrayList<String> outBack = crossCoverOut(entry.getValue());
            inBack.put(entry.getKey(), outBack);
        }

        return inBack;

    }





    /**
     * 交叉  此处不涉及适应度
     * ①交叉的单位:  试题
     *
     * random.nextInt(100)  指生成一个介于[0,n)的int值
     * 选择（轮盘赌）：择优录取+多样式减低
     * 交叉+变异：增加多样性(外部作用)
     *
     * List 是否需要转化为 paperGenetic[1]
     * 交叉部分可以不转化，因为最小单位为试题，而不是试题里面的题型和属性
     * 变异部分可以不转化，因为最小单位为试题，而不是试题里面的题型和属性
     *
     */
    private ArrayList<String> crossCoverOut(ArrayList<String> outList) {

        //  获取长度基本信息
        //  单点交叉(只保留交叉一个个体)
        int size  = outList.size();
        // 数组转list
        ArrayList<String>  re = new ArrayList<>();

        if (size > 0){
            int point = outList.get(0).split("_")[1].split(",").length;


            //  将outList转为数组List<arr>
            ArrayList<String[]>  arr = new ArrayList<>();
            for (int i = 0; i < size  ; i++){
                String str = outList.get(i).split("_")[1];
                arr.add(str.split(","));
            }


            for (int i = 0; i < size -2 ; i++){

                // 根据概率判断是否进行交叉
                if (Math.random() < PC) {
                    String[] temp = new String[point];
                    int a = new Random().nextInt(point);

                    for (int j = 0; j < a; j++) {
                        temp[j] = arr.get(i)[j];
                    }

                    for (int j = a; j < point; j++) {
                        temp[j] = arr.get(i+1)[j];
                    }

                    // FIXME 对tmp的题目大小顺序进行排序,防止数量变小
                    // arr.set(i,sortPatchOut(temp));
                    arr.set(i,temp);

                }
            }


            // 遍历数组
            for(int i = 0;i< arr.size();i++){
                String[] stringArray = arr.get(i);
                String  str1= StringUtils.join(stringArray,",");
                re.add(str1);
            }
        }


        // 最后一个个体不变,但需保证不丢失
        //arr.set(size,outList.get(size -1).split("_")[1].split(","));
        return re;

    }



    /**
     * 排序
     * 1.获取id,重新据库查询一遍  返回的Array[]
     */
    public String[] sortPatch(String[] temp1) {


        //题型数量
        int typeNum = paperGenetic[0].length;

        //抽取id,封装成int[]
        int[] sortArray = new int[typeNum];
        for (int i = 0; i < temp1.length; i++) {
            sortArray[i] = Integer.parseInt(temp1[i].split(":")[0]);
        }
        Arrays.sort(sortArray);
        //System.out.println("排序后的数组: "+Arrays.toString(sortArray));

        //根据id的位置，映射，重新排序 tmp2
        String[] temp2 = new String[typeNum];
        for (int i = 0; i < sortArray.length; i++) {
            int index = sortArray[i];
            for (String ts : temp1) {
                if (Integer.parseInt(ts.split(":")[0]) == index) {
                    temp2[i] = ts;
                }
            }
        }

        return temp2;
    }



    /**
     * 排序
     * 1.获取id,重新据库查询一遍  返回的Array[]
     */
    public String[] sortPatchOut(String[] temp1) {


        //题型数量
        int typeNum = paperGenetic[0].length;

        //抽取id,封装成int[]
        int[] sortArray = new int[typeNum];
        for (int i = 0; i < temp1.length; i++) {
            sortArray[i] = Integer.parseInt(temp1[i].split(":")[0]);
        }
        Arrays.sort(sortArray);
        //System.out.println("排序后的数组: "+Arrays.toString(sortArray));

        //根据id的位置，映射，重新排序 tmp2
        String[] temp2 = new String[typeNum];
        for (int i = 0; i < sortArray.length; i++) {
            int index = sortArray[i];
            for (String ts : temp1) {
                if (Integer.parseInt(ts.split(":")[0]) == index) {
                    temp2[i] = ts;
                }
            }
        }

        return temp2;
    }



    /**
     * 变异  (长度，属性类型，属性比例)
     * 目的：为GA提供多样性
     * 方法：以试卷为单位、交换试卷的部分试题
     * <p>
     * 原有小生境是随机生成父代,并一定进行变异操作。和自带的逻辑存在偏差
     * 解决方案：
     * ①迭代个体+if交叉概率+只变异一个个体
     */
    private void mutate(Papers papers, int j) throws SQLException {

        //mutePlus(papers,j);
        //System.out.println("================== mutate ==================")

        if (Math.random() < papers.getPm()) {

            //限制性锦标赛拥挤小生境
            ArrayList<Object> rts = niche3.RTS(paperGenetic, j);
            int similarPhenIndex = (int) rts.get(0);
            paperGenetic = (String[][]) rts.get(1);

            //执行变异后的修补操作 如果替换，则校验子类。如果未替换。si可以不进行获得，且无需校验。但校验也无所谓
            //难道这个个体大概率相似 不是,index 很随机
            //System.out.println(similarPhenIndex)
            //correct(similarPhenIndex);

            //确定性拥挤小生境
            //niche2.DET(paperGenetic);

        }

    }


}



