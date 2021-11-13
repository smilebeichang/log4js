package cn.edu.sysu.niche;

import cn.edu.sysu.adi.TYPE;
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
 * 目的：
 *      小生境      ---> 为了保证多样性
 *      自适应小生境 ---> 为了找到峰，然后保证多样性
 *      自适应小生境的目的是可以根据小生境半径确定小生境的个数，然后在小生境内进行GA操作
 *
 * 目前进展：已经证明小生境确有这个功能，而且自适应小生境也的确能实现
 * 自适应小生境不完全基于标准函数，其可通过惩罚系数实现适应度的计算，故其具有适配性
 *
 *
 * 复现自适应小生境的代码
 *      1.继续实现自己的代码(交叉变异不校验那种)
 *        基因型(即题目的相似性) vs 表现型(即适应度,即满足各种约束条件的情况)
 *        因校验之后,新个体都表现型都相似了，故可以①使用基因型作为相似性的判断 ②仅在最后做一次总的变异
 *
 *        选取标准：
 *          选取leader是适应度, 选取follower是相似性
 *
 *
 *
 *
 * 1.代码实现思路 优化合并峰(数据重复问题)
 *      方案一：(筛选--合并--调整)* N
 *          判断哪些峰需要合并,先选出一个最近点，进行合并。合并完成后，通过半径调整和个体剔除,再判断是否还有需要继续合并的峰
 *
 *      方案二：若存在相同的峰,直接选择最小的一个进行,剩余的过滤掉暂时不做处理
 *          此处选择方案二: 相对简单些,不用考虑迭代
 *
 *
 *   代码实现思路 交叉变异的位置、合并方式
 *      1.通过校验小生境个数来进行选择,选择是否进行小生境内的交叉变异
 *      2.侧输出流。可以两个大的转换成一个大的+小的
 *
 * 2.代入GA
 *      1.难点一: 基因型转换    解决方案：二进制选择 + 概率性
 *        难点二：相似性的判断   表现型(即适应度,即满足各种约束条件的情况)
 *
 *
 *                         原始               修订
 *      适应度：            标准函数计算        惩罚系数的计算
 *      leader:            适应度最大的个体    适应度最大的个体
 *      相似性：            离leader的相似性    题目的相似个数
 *      小生境的半径：       横坐标0~1          题目相似个数（阈值）
 *
 *
 *      2.此版本整体逻辑
 *           初始化 --- 适应度排序(适应度) --- 分配到不同小生境(相似性) --- 选择 -- 交叉 -- 变异 -- 修补
 *                                         |<------------       niche            ------------>|
 *
 */
public class DNDR6 {


    private Logger log = Logger.getLogger(DNDR6.class);
    private KLUtils klUtils = new KLUtils();


    /**
     * 迭代次数
     */
    private int ITERATION_SIZE = 120;


    /**
     * 排序后的 list 1.0_0.3
     */
    private ArrayList<String> sortListForGene = new ArrayList<>(200);


    /**
     * leader + followers  map(key是string存小生境leader, value是list存小生境member)
     */
    private HashMap<String, ArrayList<String>> mapArrayListForGene = new HashMap<>();

    /**
     * set存leader
     */
    private HashSet<String> leaderSetForGene = new HashSet();


    /**
     * 200套试卷 20道题  paperGenetic  vs  paper_genetic 的区别
     */
    private static String[][] paperGenetic = new String[200][20];

    private JDBCUtils4 jdbcUtils = new JDBCUtils4();

    private static ArrayList<String> bankList = new ArrayList();



    /**
     * 小生境对象(变异操作)
     */
    private Niche5 niche5 = new Niche5();

    /**
     * 交叉变异全局系数
     */
    private static  double PC = 0.6;
    private static  double PM = 0.8;


    /**
     * 主程序
     */
    @Test
    public void main() throws SQLException {

        // 抽取试卷  试卷200套,每套20题  为交叉变异提供原始材料
        // 初始化试卷(长度，题型，属性比例) 轮盘赌生成二维数组 String[][] paperGenetic = new String[200][20]
        initItemBank();

        // 迭代次数
        for (int i = 0; i < ITERATION_SIZE; i++) {

            // 置空leader容器
            iterationClear();

            // 适应度值排序 0.9992_0.101  ArrayList<String> sortListForGene = new ArrayList<>(200)
            sortFitnessForGene();

            // 将群体中的个体分配到不同小生境中 leader + members    mapArrayListForGene(key,value)
            distributeNicheForGene();

            log.info("小生境个数: " + mapArrayListForGene.size());

            /*
             *
             * 相似性：
             *   1.通过判断小生境的种群大小,选择是否进行小生境内外的交叉变异
             *   2.侧输出流。可以两个大的生成一个大的+小的(合并)
             *
             *
             * GA 操作的范围: 小生境内 or 全局
             *    1.1 若小生境个数大于2,在各自的小生境内执行  选择|交叉|变异  mapArrayListForGene
             *    1.2 若小生境个数=1,汇总到全局池,进行全局的交叉变异(自从代码优化后，发现没有=1的个体了)
             *
             *      方案: 挨个对个体进行判断,
             *           选择： size 决定了 选择的方式范围
             *           交叉|变异： 因为只变化一个个体，故直接进行就好，无需考虑第二个个体
             *
             */


            // 遍历，根据该个体所在小生境的种群大小，然后塞入到不同集合中
            HashMap<String, ArrayList<String>> inListHashMap = new HashMap<>();
            ArrayList<String> outList = new ArrayList<>();

            Iterator<Map.Entry<String, ArrayList<String>>> iterator = mapArrayListForGene.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, ArrayList<String>> entry = iterator.next();

                // 是否小生境个数 >= 2   2太小，导致outList集合大部分时间为空
                if (entry.getValue().size() >= 5) {
                    inListHashMap.put(entry.getKey(), entry.getValue());
                } else {
                    // 是需要addValue,但绝对不是连key不管   有leader 但没value的,leader 不包含在member中吗？
                    //outList.add(entry.getKey());
                    outList.addAll(entry.getValue());
                }
            }


            // 选择
            // ②对不同的集合进行不同的选择 (小生境内 | 小生境外)
            // 方案：将小生境个数>=2的个体挑出来，这部分个体进行小生境内选择；2<的部分进行小生境外选择
            // 最好是进去啥,返回啥。保持原样不做修改,这样有利于后续的交叉变异

            // 进行小生境内的选择   小生境内有多少个体就执行执行多少次选举，选出适应个体
            HashMap<String, ArrayList<String>>  inSelect = selectionIn(inListHashMap);

            // 进行小生境外的选择
            ArrayList<String> outSelect  = selectionOut(outList);

            // 问题一: 为什么会存在相同的value      已OK
            // 问题二: 总数对不上，2(29+439)+197   已OK


            // 交叉
            // 进行小生境内交叉
            HashMap<String, ArrayList<String>> inCross = crossCoverIn(inSelect);

            // 进行小生境外交叉  方法内部进行了size判断
            ArrayList<String> outCross = crossCoverOut(outSelect);

            // 变异
            // 进行小生境内交叉(采用的是 限制性锦标赛拥挤小生境)
            HashMap<String, ArrayList<String[]>> inMutate = mutateIn(inCross);

            // 进行小生境外交叉
            ArrayList<String[]> outMutate = mutateOut(outCross);

            // 将inMutate和outMutate赋值给 paperGenetic
            paperGenetic = mergeToGene(inMutate,outMutate);

        }

    }

    /**
     * 每次迭代置空容器 leader相关
     */
    private void iterationClear() {

        mapArrayListForGene.clear();
        leaderSetForGene.clear();

    }


    /**
     * 适应度值排序
     */
    private void sortFitnessForGene() {

        // 数据清空
        //sortList.clear();
        sortListForGene.clear();


        // 遍历二维数组，获取其适应度，然后拼接上本身  sortListForGene.add(minrum + "_" + ids)
        int paperSize = paperGenetic.length;
        getFitnessForGene(paperSize);

        Comparator comp = new MyComparator();
        // empty String
        Collections.sort(sortListForGene, comp);

    }



    /**
     * 将群体中的个体分配到不同小生境中 1.0_0.3
     *
     *      步骤：
     *      1.选取leader
     *          注释: 最后有些个体其实不应该成为leader,待后续进行合并和剔除操作
     *
     *      2.选取member
     *          注释:需保证数据总数不变
     *              问题现象：存在一个点分布在两个小生境中   0.4 分别在0.301和0.0407中
     *              解决方案：每次判断完后，对这个个体标记已经清除
     *          方案一: 选完全部leader之后统一进行，可以避免ConcurrentModificationException  √
     *          方案二: 赋予到一个新的集合中
     *
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

                // 使用一个计数器,比对两个集合的相似题目数
                int max = 0;
                // 获取目前leader的信息
                for (String leader : leaderSetForGene) {

                    // b 的判断应该和全部的leader进行判断
                    String bids = leader.split("_")[1];
                    // 判断两套试卷的相似程度,如果相似题目数达到一定数目，则判定为是同一个小生境 如3，将基因型转为list,使用list来判断相似个数
                    List<String> ListA = stringToList(aids);
                    List<String> ListB = stringToList(bids);

                    // 假设上面ListA 和 ListB都存在数据
                    // counter 大部分为0,优化方案：将题目数扩大 10*2=20
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
                // 重复的题目小于3,则不隶属于任何一个小生境,  FIXME 使用1可能过小 count 和 mark 需要相互对应
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

                    // 判断两套试卷的相似程度: 如果相似题目数达到一定数目，则判定为是同一个小生境 如1
                    List<String> ListA = stringToList(aids);
                    List<String> ListB = stringToList(bids);

                    // 假设上面ListA和ListB都存在数据  计算A与B之间的相似题目数
                    // mark 在此处置为0，是否会有偏差，需上移动
                    int mark = 0;
                    for (String a : ListA) {
                        for (String b : ListB) {
                            if (a.equals(b)) {
                                mark = mark + 1;
                            }
                        }
                    }
                    // 如AB足够相似，则存放入集合里，并标记为null
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
            // 验证个体总数是否丢失  FIXME sum存在偏差，待验证，应累加leader+member的总和，防止leader存在，但无member的情况
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
     * 使用构造法选取题目  (轮盘赌） 生成 paperGenetic
     *      1.题型构造解决 （不考虑下限比例）
     *      2.属性构造解决 （不考虑下限比例）
     *      设置比例  可以通过惩罚系数来设定  超出,则急剧减少
     *      总结：在初始化的时候，不需要完全保证题型和属性符合要求，后续使用GA迭代和轮盘赌解决即可
     */
    private void initItemBank() throws SQLException {

        System.out.println("====== 开始选题,构成试卷  轮盘赌构造 ======");

        // 试卷|个体大小  提供遗传变异的大单位
        int paperNum = paperGenetic.length;

        // 题目|基因大小  交叉变异的基本单位
        int questionsNum = paperGenetic[1].length;

        // 单套试卷的集合
        HashSet<String> itemSet = new HashSet<>();

        // 题库310道题  50:100:100:50:10   硬性约束：长度  软性约束：题型、属性比例
        // 获取题库所有题目  [8:CHOSE:(1,0,0,0,0),....] 旁路缓存的概念
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
                    // 获取题目id   轮盘赌构造
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

            //list排序  目前抽取到集合为题目id
            Collections.sort(idList);


            // 根据id从数据库中查询相对应的题目
            String ids = idList.toString().substring(1, idList.toString().length() - 1);
            ArrayList<String> bachItemList = jdbcUtils.selectBachItem(ids);


            // 交叉变异的针对的是题目   即试卷=个体  题目=基因
            String[] itemArray = new String[bachItemList.size()];
            for (int i = 0; i < bachItemList.size(); i++) {
                itemArray[i] = bachItemList.get(i);
            }
            // 赋值  把题库提升为全局变量，方便整体调用 容器：二维数组
            paperGenetic[j] = itemArray;
        }
    }

    /**
     * 返回题库所有题目 id:type:pattern
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

        //计算每道题目的适应度占比   1*0.5*0.8
        double[] fitnessArray = getRouletteFitness(itemSet);

        //id去重操作
        HashSet<Integer> idSet = new HashSet<>();

        //迭代器遍历HashSet  确保取出目前题目中不存在的题目
        Iterator<String> it = itemSet.iterator();
        while (it.hasNext()) {
            idSet.add(Integer.valueOf(it.next().split(":")[0]));
        }

        //累加初始值
        double accumulate = 0;

        //题目占总题目的适应度累加百分比
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
     * 1.根据已选题目，计算题库中每道题目的适应度值比例
     * 2.每道题的概率为 1*penalty^n,总概率为310道题叠加
     * 2.1  初始化的时候将全局的310题查询出来（id:type:pattern）
     * 2.2  求出每道题目的概率 1 * 惩罚系数
     * 2.3  求出每道题目的适应度占比
     * 题型比例 选择[0.2,0.4]   填空[0.2,0.4]  简答[0.1,0.3] 应用[0.1,0.3]
     * 属性比例 第1属性[0.2,0.4]第2属性[0.2,0.4] 第3属性[0.1,0.3] 第4属性[0.1,0.3] 第5属性[0.1,0.3]
     */
    private double[] getRouletteFitness(HashSet<String> itemSet) {

        // 所有题目的适应度总和
        double fitSum = 0.0;

        // 每道题目的适应度值
        double[] fitTmp = new double[bankList.size()];

        // 每道题目的适应度占比   疑问:1/310 会很小,random() 这样产生的值是否符合要求
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

        // 计算出每道题目的各自比例
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

                /**
                 * FiXME 待修复
                 * -->itemList: [null, null, null, null, null, null, null, null, null, null, null, null, null, null]
                 * -->itemList.length: 20
                 * j: 0
                 * itemList[j]: null
                 */
                System.out.println("-->itemList: "+Arrays.asList(itemList));
                System.out.println("-->itemList.length: "+itemList.length);
                System.out.println("j: "+j);
                System.out.println("itemList[j]: "+itemList[j]);

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

        if(outList.size()  > 0) {

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
        }

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
     * 交叉 底层调用 crossCoverOut
     *
     */
    private HashMap<String, ArrayList<String>> crossCoverIn(HashMap<String, ArrayList<String>>  inListHashMap) throws SQLException {


        HashMap<String, ArrayList<String>> inBack = new HashMap<>();

        // 外围嵌上一层循环，然后转化为多个list，挨个执行 selectionOut 即可
        // 因为适应度是降序排列的,执行精英策略时是否会有影响  后续考虑
        for(Map.Entry<String, ArrayList<String>> entry : inListHashMap.entrySet()){

            ArrayList<String> outBack = crossCoverOut(entry.getValue());
            inBack.put(entry.getKey(), outBack);

        }

        return inBack;

    }





    /**
     * 交叉  此处不涉及适应度
     * ①交叉的单位:  题目
     *
     * random.nextInt(n)  指生成一个介于[0,n)的int值
     * 选择（轮盘赌）：择优录取+多样式减低
     * 交叉+变异：增加多样性(外部作用)
     *
     * List 是否需要转化为 paperGenetic[1]
     * 交叉部分可以不转化，因为最小单位为题目，而不是题目里面的题型和属性
     * 变异部分可以不转化，因为最小单位为题目，而不是题目里面的题型和属性
     *
     */
    private ArrayList<String> crossCoverOut(ArrayList<String> outList) {

        //  获取长度基本信息
        //  单点交叉(只保留交叉一个个体)
        int size  = outList.size();
        // 数组转list
        ArrayList<String>  re = new ArrayList<>();

        // size - 2 适配 交叉
        if (size - 2 > 0){
            int point = outList.get(0).split("_")[1].split(",").length;
            // point 一定是20 可以设置为常量,进行性能上的优化

            //  将outList转为数组List<arr>
            ArrayList<String[]>  arr = new ArrayList<>();

            for (int i = 0; i < size  ; i++){
                String str = outList.get(i).split("_")[1];
                arr.add(str.split(","));
            }


            for (int i = 0; i < size -2 ; i++){

                if (arr.get(i).length==20 && arr.get(i+1).length==20) {

                    // 根据概率判断是否进行交叉
                    if (Math.random() < PC) {
                        String[] temp = new String[point];
                        int a = new Random().nextInt(point);

                        for (int j = 0; j < a; j++) {
                            // debug 进行测试  如果size()不等于20 ，则此时先过滤
                            temp[j] = arr.get(i)[j];
                        }

                        for (int j = a; j < point; j++) {
                            // debug 进行测试  如果size()不等于20 ，则此时先过滤
                            temp[j] = arr.get(i + 1)[j];
                        }

                        // FIXME 对tmp的题目大小顺序进行排序,防止数量变小
                        // arr.set(i,sortPatchOut(temp));
                        arr.set(i, temp);

                    }
                }else {
                    System.out.println("arr.size 不符合要求 待优化");
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
     * 变异  (长度，属性类型，属性比例)
     *
     */
    private HashMap<String, ArrayList<String[]>> mutateIn(HashMap<String, ArrayList<String>> inCross) throws SQLException {

        HashMap<String, ArrayList<String[]>> inBack = new HashMap<>();
        // mutePlus(papers,j);
        System.out.println("================== mutateIn ==================");

        // 调用 mutateOut 即可
        for(Map.Entry<String, ArrayList<String>> entry : inCross.entrySet()){

            ArrayList<String[]> outBack = mutateOut(entry.getValue());
            inBack.put(entry.getKey(), outBack);

        }

        return inBack;



    }


    /**
     * 变异  (长度，属性类型，属性比例)
     *
     * outCross:27.783779425230577_2,9,12,34,36,39,42,49,69,72,90,91,102,112,123,137,168,197,219,227
     *
     */
    private ArrayList<String[]> mutateOut(ArrayList<String> outCross) throws SQLException {

        // 创建一个方法内变量,用于接收c1,并最后返回
        ArrayList<String[]> outMutate = new ArrayList<>();

        // mutePlus(papers,j);
        System.out.println("================== mutateOut ==================");

        if (outCross.size() > 0 ) {

            //for (int j = 0; j < outCross.size()-1 ; j++) {
            for (int j = 0; j < outCross.size() ; j++) {

                if(Math.random() < PM){
                    // 限制性锦标赛拥挤小生境
                    // Fixme 传进去是单个个体，返回的单个个体，然后在此方法后做循环给容器赋值  返回string[]
                    ArrayList<Object> rts = niche5.RTS(outCross, j);

                    // 设置一个全局变量，不断的给 paperGenetic 赋值
                    String[] c1 = (String[]) rts.get(0);
                    outMutate.add(c1);

                    // 执行变异后的修补操作 如果替换，则校验子类。如果未替换。si可以不进行获得，且无需校验。但校验也无所谓
                    // 难道这个个体大概率相似 不是,index 很随机
                    //correct(similarPhenIndex);

                    // 确定性拥挤小生境
                    //niche2.DET(paperGenetic);
                }else{
                    ArrayList<String> bachItemList = jdbcUtils.selectBachItem(outCross.get(j));

                    // ArrayList 转 String[]
                    String[] c1 = new String[bachItemList.size()];
                    for (int k = 0; k < bachItemList.size(); k++) {
                        c1[k] = bachItemList.get(k);
                    }

                    outMutate.add(c1);
                }
            }
        }

        return outMutate;

    }


    /**
     *  将变异后的各个小生境内的基因进行合并
     *  gene = inMutate + outMutate
     */
    private String[][] mergeToGene(HashMap<String, ArrayList<String[]>> inMutate, ArrayList<String[]> outMutate) {

        String[][] gene =  new String[200][20];
        int index = 0;

        for(Map.Entry<String, ArrayList<String[]>> entry : inMutate.entrySet()){

            for (String[] strings : entry.getValue()) {
                gene[index] = strings;
                index ++;
            }
        }

        for (String[] strings : outMutate) {
            gene[index] = strings;
            index ++;
        }


        return gene;
    }




}



