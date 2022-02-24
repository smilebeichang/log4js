package cn.edu.sysu.niche.a5n3;

import cn.edu.sysu.adi.TYPE;
import cn.edu.sysu.niche.MyComparator;
import cn.edu.sysu.pojo.Papers;
import cn.edu.sysu.utils.JDBCUtils4;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.sql.SQLException;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 *
 * @Author : song bei chang
 * <p>
 * 模拟GA
 * <p>
 * 1.模拟GA
 * 1.1 选择、交叉、变异
 * 1.2 迭代120代后,计算其最大圈  这个多样性很难保证,数据容易丢失  至少这是一个值得保证的点
 * <p>
 * 2.诊断效果、质量(A:最大圈顶点数 平均、 B:fitness 波动)
 * <p>
 * 3.题库*3 属性500:5 ->1000:8
 * <p>
 * 4.nicheGA、GA、random、p-cdi
 * 4.1 进行打印操作
 * <p>
 * 5.模拟仿真:2^5=32 pattern
 * 1000个被试、真被试、rum
 * <p>
 * 6.质量下降
 * <p>
 * 7.改进BSF
 * 改变抽取条件(规则)
 */
public class DNDR10_GA {


    private Logger log = Logger.getLogger(DNDR10_GA.class);


    /**
     * 容器 全局最优 局部最优
     */
    private static double GlobalOptimal = 0;
    private static double[] LocalOptimal = new double[100];
    private static ArrayList<String> bankList = new ArrayList();


    /**
     * 迭代次数  50
     * 这个参数也需要不停的修改，适配 Niche GA
     */
    private int ITERATION_SIZE = 25;


    /**
     * 100套试卷 20道题
     */
    private static String[][] paperGenetic = new String[100][20];

    int paperSize = paperGenetic.length;


    private ArrayList<String> sortListForRandom = new ArrayList<>(ITERATION_SIZE * 100);
    private ArrayList<String> sortTo50 = new ArrayList<>(50);


    private JDBCUtils4 jdbcUtils = new JDBCUtils4();


    /**
     * 交叉变异系数需和 Niche GA 一致
     */
    private static double PC = 0.6;
    private static double PM = 0.8;


    /**
     * size 为310  初始化,塞入到内存中
     */
    ArrayList<String> allItemList = jdbcUtils.selectAllItems();


    /**
     * 比较器
     */
    Comparator comp = new MyComparator();


    public DNDR10_GA() throws SQLException {
    }


    /**
     * 生成题库(以试卷为单位：id  长度，属性类型，属性比例)
     * 原始比例： 5+10+10+5+1 = 31
     * 属性类型：1个属性的1题[1,50]  2个属性的2题[51,150]  3个属性的2题[151,250] 4个属性的1题[251,300]
     */
    private void initItemBank() throws SQLException {

        System.out.println("====== 开始选题,构成试卷  ======");

        /*  试卷数 */
        int paperNum = paperGenetic.length;

        /* 单张试卷每种题型的题目数量 */
        int oneAttNum = 3;
        int twoAttNum = 6;
        int threeAttNum = 6;
        int fourAttNum = 3;
        int fiveAttNum = 2;


        // 题库310道题  50:100:100:50:10   长度，题型，属性比例
        String sql1 = "SELECT CEILING( RAND () * 49 ) + 1  AS id";
        String sql2 = "SELECT CEILING( RAND () * 99 ) + 51 AS id";
        String sql3 = "SELECT CEILING( RAND () * 99 ) + 151 AS id";
        String sql4 = "SELECT CEILING( RAND () * 49 ) + 251 AS id";
        String sql5 = "SELECT CEILING( RAND () * 9 ) + 301 AS id";



        /*  生成的平行试卷份数  */
        for (int j = 0; j < paperNum; j++) {
            ArrayList<Integer> idList = new ArrayList<>();
            int id;

            //随机抽取1个属性的试题
            Set<Integer> id_set1 = new HashSet<>();
            for (int i = 0; i < oneAttNum; i++) {
                //去重操作
                while (id_set1.size() == i) {
                    id = jdbcUtils.selectItem(sql1);
                    id_set1.add(id);
                }
            }
            idList.addAll(id_set1);

            //随机抽取2个属性的试题
            Set<Integer> id_set2 = new HashSet<>();
            for (int i = 0; i < twoAttNum; i++) {
                while (id_set2.size() == i) {
                    id = jdbcUtils.selectItem(sql2);
                    id_set2.add(id);
                }
            }
            idList.addAll(id_set2);

            //随机抽取3个属性的试题
            Set<Integer> id_set3 = new HashSet<>();
            for (int i = 0; i < threeAttNum; i++) {
                while (id_set3.size() == i) {
                    id = jdbcUtils.selectItem(sql3);
                    id_set3.add(id);
                }
            }
            idList.addAll(id_set3);

            //随机抽取4个属性的试题
            Set<Integer> id_set4 = new HashSet<>();
            for (int i = 0; i < fourAttNum; i++) {
                while (id_set4.size() == i) {
                    id = jdbcUtils.selectItem(sql4);
                    id_set4.add(id);
                }
            }
            idList.addAll(id_set4);

            //随机抽取5个属性的试题
            Set<Integer> id_set5 = new HashSet<>();
            for (int i = 0; i < fiveAttNum; i++) {
                while (id_set5.size() == i) {
                    id = jdbcUtils.selectItem(sql5);
                    id_set5.add(id);
                }
            }
            idList.addAll(id_set5);


            //list 排序
            Collections.sort(idList);

            //输出所有抽取到的试题id
            System.out.println("试卷" + j + "的试题id: " + idList);


            String ids = idList.toString().substring(1, idList.toString().length() - 1);

            ArrayList<String> bachItemList = jdbcUtils.selectBachItem(ids);


            // 把题库提升为全局变量，方便整体调用 容器：二维数组
            // 交叉变异的对象是 试题的题目   即试卷=个体  题目=基因
            String[] itemArray = new String[bachItemList.size()];
            for (int i = 0; i < bachItemList.size(); i++) {
                itemArray[i] = bachItemList.get(i);
            }

            // 赋值
            paperGenetic[j] = itemArray;
        }

    }


    /**
     * 交叉  此处不涉及适应度
     * ①交叉的单位:  试题
     * ②修补的方面:  长度
     * <p>
     * random.nextInt(100)  指生成一个介于[0,n)的int值
     */
    private void crossCover() throws SQLException {

        for (int k = 0; k < paperGenetic.length - 1; k++) {
            System.out.println("K:"+k);

            //  单点交叉(只保留交叉一个个体)
            int point = paperGenetic[1].length;

            // 根据概率判断是否进行交叉
            if (Math.random() < PC) {
                String[] temp = new String[point];
                int a = new Random().nextInt(point);

                for (int j = 0; j < a; j++) {
                    temp[j] = paperGenetic[k][j];
                }

                for (int j = a; j < point; j++) {
                    temp[j] = paperGenetic[k + 1][j];
                }

                // 对tmp进行排序
                paperGenetic[k] = sortPatch(temp);

                // 每执行一次pc 则校验一次
                correct(k);
            }
        }

    }


    /**
     * 执行修补操作(交叉变异均可能导致长度数量发生了变化)
     * 步骤：
     * (1)校验长度, 随机新增题目
     */
    private void correct(int i) throws SQLException {

        // 长度校验
        correctLength(i);

        if ((new HashSet<>(Arrays.asList(paperGenetic[i]))).size() < paperGenetic[0].length) {
            System.out.println("size 不符合");
        }

    }


    /**
     * 变异  (长度)
     * 目的：为GA提供多样性
     * 方法：以试卷为单位、交换试卷的部分试题
     */
    private void mutate() throws SQLException {

        System.out.println("================== mutate ==================");

        int count = 310;
        String key = "";

        // 以试卷为单位、变异部分试题
        for (int i = 0; i < paperGenetic.length; i++) {

            if (Math.random() < PM) {
                Random random = new Random();
                // length-1
                int mutatePoint = random.nextInt((paperGenetic[0].length) - 1);
                // 将Array 转 hashSet
                Set<String> set = new HashSet<>(Arrays.asList(paperGenetic[i]));
                //System.out.println(i+" 原试卷: "+set);

                // 将要变异的元素   前提是试卷有序排列
                String s = paperGenetic[i][mutatePoint];
                System.out.println("  remove element: " + s);
                set.remove(s);
                int removeId = Integer.parseInt(s.split(":")[0]);
                //System.out.println("  临时试卷：  "+set);

                // 单套试卷临时存储容器
                String[] temp1 = new String[paperGenetic[0].length];

                //生成一个不存在set中的key
                while (set.size() != paperGenetic[0].length) {
                    key = random.nextInt(count) + 1 + "";
                    if (!(key + "").equals(removeId + "")) {
                        // ArrayList<String> list = jdbcUtils.selectBachItem(key);
                        String s1 = allItemList.get(Integer.parseInt(key) - 1);
                        set.add(s1);
                    }
                }
                System.out.println("  add element: " + key);
                set.toArray(temp1);

                //排序
                paperGenetic[i] = sortPatch(temp1);

                //执行变异后的修补操作
                correct(i);

            }

            //System.out.println("  最终试卷： "+Arrays.toString(paperGenetic[i]));
        }

    }

    /**
     * 排序
     * 1.获取id,重新据库查询一遍  返回的Array[]
     */
    public String[] sortPatch(String[] temp1) {

        //题目数量
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
     * 选择: 以适应度为导向,轮盘赌为策略, 适者生存和多样性的权衡
     * <p>
     * ①计算适应度：以试卷为单位，min
     * ②轮盘赌进行筛选 paperGenetic=newPaperGenetic;
     * <p>
     * <p>
     * 精度取小数点后三位,  指标信息取前top10的avg
     * <p>
     * *    选择（轮盘赌）：择优录取+多样式减低
     * *    交叉+变异：增加多样性(外部作用)
     */
    public void selection() {

        System.out.println("====================== select ======================");

        //100套试卷
        int paperSize = paperGenetic.length;

        //每套试卷的适应度占比  min
        double[] fitPro = getFitness(paperSize);

        //轮盘赌 累加百分比
        double[] fitPie = new double[paperSize];

        //累加初始值
        double accumulate = 0;

        //试卷占总试卷的适应度累加百分比
        for (int i = 0; i < paperSize; i++) {
            fitPie[i] = accumulate + fitPro[i];
            accumulate += fitPro[i];
        }

        //累加的概率总和为1 数组下标从0开始
        fitPie[paperSize - 1] = 1;

        //初始化容器 随机生成的random概率值
        double[] randomId = new double[paperSize];

        //不需要去重
        for (int i = 0; i < paperSize; i++) {
            randomId[i] = Math.random();
        }

        // 排序
        Arrays.sort(randomId);


        //轮盘赌 越大的适应度,其叠加时增长越快,即有更大的概率被选中
        // 同一套试卷可能会被选取多次（轮盘赌的意义）
        // GA 的通病：多样性的维持
        //      择优录取,个体越来越相似,所以才需要变异,但变异后的个体，因为经过轮盘赌,也不一定能够保存下来
        String[][] newPaperGenetic = new String[paperSize][];
        int newSelectId = 0;

        for (int i = 0; i < paperSize; i++) {
            while (newSelectId < paperSize && randomId[newSelectId] < fitPie[i]) {
                //需要确保fitPie[i] 和 paperGenetic[i] 对应的i 是同一套试卷
                newPaperGenetic[newSelectId] = paperGenetic[i];
                newSelectId += 1;

            }
        }

        //重新赋值种群的编码
        paperGenetic = newPaperGenetic;

    }

    /**
     * 打印数组double[]
     */
    private void printDoubleArray(double[] randomId) {

        //把基本数据类型转化为列表 double[]转Double[]
        int num = randomId.length;
        Double[] arrDouble = new Double[num];
        for (int i = 0; i < num; i++) {
            arrDouble[i] = randomId[i];
        }

        //Double[]转List
        List<Double> list = Arrays.asList(arrDouble);
        //System.out.println("随机抽取的random概率值："+list);

    }


    /**
     * 每套试卷的适应度占比
     * <p>
     * selection 计算适应度值
     * 方案  进行乘以一个exp 来进行适应度值的降低，高等数学里以自然常数e为底的指数函数
     * 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3]  应用[0.1,0.3]
     * 属性比例 第1属性[0.2,0.4]  第2属性[0.2,0.4]  第3属性[0.1,0.3] 第4属性[0.1,0.3] 第5属性[0.1,0.3]
     */
    private double[] getFitness(int paperSize) {

        //log.info("适应值 log4j")

        // 所有试卷的适应度总和
        double fitSum = 0.0;
        // 每套试卷的适应度值
        double[] fitTmp = new double[paperSize];
        // 每套试卷的适应度占比
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

            //System.out.printf("exp(%.3f) 为 %.3f%n", expNum, Math.exp(expNum));

            //均值 和 最小值
            double avgrum = (adi1r + adi2r + adi3r + adi4r + adi5r) / 5;
            double minrum = Math.min(Math.min(Math.min(Math.min(adi1r, adi2r), adi3r), adi4r), adi5r) * 100;

            //System.out.println("minrum: "+minrum);

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
        //klUtils.bubbleSort(fitTmp);

        return fitPro;
    }


    /**
     * 格式转换工具
     */
    public Double numbCohesion(Double adi) {


        return Double.valueOf(String.format("%.4f", adi));

    }

    /**
     * 精英策略
     * 1.获取全局最优,和局部最优/局部最差（计算适应度值，并保存为全局变量）
     * 计算时机可能要做改变，因为中间存在 交叉变异的情况，适应度值发生了一定程度的变化
     * 2.用全局最优替换掉局部最优
     * 局部最优和全局最优,都只是一个容器  需要挨个和每套试卷进行比较替换
     */
    private void elitistStrategy() {


        System.out.println("================== elitistStrategy ==================");
        //getFitness(paperGenetic.length);

        // 全局最优解替换掉局部最优解
        for (int i = 0; i < LocalOptimal.length; i++) {
            LocalOptimal[i] = GlobalOptimal;
        }

    }


    /**
     * 返回题库所有试题 id:type:pattern
     */
    private ArrayList<String> getBank() throws SQLException {

        return jdbcUtils.select();

    }


    /**
     * hashMap 根据排序
     */
    private String hashMapSort(Map<String, Double> map) {


        //System.out.println("============排序前============");
        Set<Map.Entry<String, Double>> entrySet = map.entrySet();
        for (Map.Entry s : entrySet) {
            //System.out.println(s.getKey()+"--"+s.getValue());
        }

        //System.out.println("============排序后============");

        //借助list实现hashMap排序

        List<Map.Entry<String, Double>> list = new ArrayList<>(map.entrySet());
        Collections.sort(list, (o1, o2) -> {

            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            if (o1.getValue() > o2.getValue()) {
                return -1;
            }
            if (o2.getValue() > o1.getValue()) {
                return 1;
            }
            return 0;


            //按照value值，重小到大排序
//                return o1.getValue() - o2.getValue();

            //按照value值，从大到小排序
//                return o2.getValue() - o1.getValue();
            //return o2.getValue() >= o1.getValue()?1:-1;

            //按照value值，用compareTo()方法默认是从小到大排序
            //return o1.getValue().compareTo(o2.getValue());
        });

        //注意这里遍历的是list，也就是我们将map.Entry放进了list，排序后的集合
        for (Map.Entry s : list) {
            //System.out.println(s.getKey()+"--"+s.getValue());
        }

        return list.get(0).getKey() + ":" + list.get(0).getValue();

    }


    /**
     * 通过差集 并集  来重新对list排序
     */
    private ArrayList<String> rearrange(String type, ArrayList<String> listA) {

        //定义
        ArrayList<String> listB = new ArrayList<>();
        for (String s : listA) {
            if (s.contains(type)) {
                listB.add(s);
            }
        }

        //差集 并集
        listA.removeAll(listB);
        listB.addAll(listA);
        //System.out.println(listB);

        return listB;

    }


    /**
     * 长度校验
     * 检验完成后，更新 paperGenetic
     * <p>
     * 解决方案：    ①size==10,退出
     * ②如果在小于范围下限，则按照题型选取
     * ③如果都符合要求，则随机选取一题，再在下层做处理
     */
    private void correctLength(int w) throws SQLException {

        // 去重操作  (id:type:attributes:adi1_r:adi2_r:adi3_r:adi4_r:adi5_r)
        HashSet<String> setBegin = new HashSet<>(Arrays.asList(paperGenetic[w]));

        if (setBegin.size() != paperGenetic[0].length) {

            System.out.println("第 " + w + " 题, 交叉/变异导致size不匹配：开始进行长度修补 ");

            // 随机选题
            while (setBegin.size() != paperGenetic[0].length) {
                //  where 1=1
                String sql = " 1=1 order by RAND() limit 1 ";
                ArrayList<String> arrayList = jdbcUtils.selectBySql(sql);
                HashSet<String> tmp = new HashSet<>(arrayList);
                setBegin.addAll(tmp);
            }

            // 输出集合的大小  setEnd.size()
            System.out.println("setEnd.size(): " + setBegin.size());

            // hashSet 转 array
            String[] array = new String[setBegin.size()];
            array = setBegin.toArray(array);
            array = sortPatch(array);
            paperGenetic[w] = array;

            //打印选取的题目，打印的结果 应该是内存地址
            //System.out.println("步骤1 size修补后的结果如下："+Arrays.toString(paperGenetic[w]));

        }

    }


    /**
     * mutePlus
     * 这个需要检查，应该是要去掉的
     * 是否可以不进行变异，直接进行修补，好像不行，原始部分就已经是直接修补了
     * 试试将变异概率设置为1 看看效果  无效
     */
    public void mutePlus(Papers papers, int j) throws SQLException {

        // 将其植入，增大变异
        // 以试卷为单位、交换试卷的部分试题
        if (Math.random() < papers.getPm()) {
            Random random = new Random();
            //length-1
            int mutatePoint = random.nextInt((paperGenetic[1].length) - 1);
            Set<String> set = new HashSet<>(Arrays.asList(paperGenetic[j]));

            //将要变异的元素
            String s = paperGenetic[j][mutatePoint];
            set.remove(s);
            int removeId = Integer.parseInt(s.split(":")[0]);

            //单套试卷临时存储容器
            String[] temp1 = new String[paperGenetic[j].length];

            //生成一个不存在set中的key
            String key;
            while (set.size() != paperGenetic[j].length) {
                key = (1 + random.nextInt(310)) + "";
                if (!(key + "").equals(removeId + "")) {
                    ArrayList<String> list = jdbcUtils.selectBachItem(key);
                    try {
                        //频繁建立链接，将导致无从数据中拿到数据 解决方案①thread.sleep  ②放在map中
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    set.add(list.get(0) + "");
                }
            }
            set.toArray(temp1);

            //排序修补
            paperGenetic[j] = sortPatch(temp1);

            //执行变异后的修补操作
            correct(j);
        }

    }


    private void getFitnessForGA() throws SQLException {

        // 拼接字符串 每套试卷的适应度值_本身
        String[] fitTmp = new String[paperSize];

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

            // 强行手动修复 如果不行就判断里面的值是否为null
            // 加一层判断，如果itemList.length=20,则保持不变;如果否,则给paperGenetic[i] 赋予新值
            if (itemList.length != 20) {
                itemList = supplementPaperGenetic();
            }

            //System.out.println("-->itemList: " + Arrays.asList(itemList).toString());
            for (int j = 0; j < itemList.length; j++) {

                // int id = Integer.valueOf(itemList[j].trim());
                // String[] splits = allItemList.get(id).split(":");

                String[] splits = itemList[j].trim().split(":");
                adi1r = adi1r + Double.parseDouble(splits[3]);
                adi2r = adi2r + Double.parseDouble(splits[4]);
                adi3r = adi3r + Double.parseDouble(splits[5]);
                adi4r = adi4r + Double.parseDouble(splits[6]);
                adi5r = adi5r + Double.parseDouble(splits[7]);

                // 拼接ids
                idsb.append(",").append(splits[0]);


            }

            //System.out.println("idsb.toString():"+idsb.toString());
            String ids = idsb.toString().substring(1);

            // 题型个数
            String[] expList = itemList;
            int typeChose = 0;
            int typeFill = 0;
            int typeShort = 0;
            int typeCompre = 0;


            //此次迭代各个题型的数目
            for (String s : expList) {

                //s = allItemList.get(Integer.valueOf(s.trim()));

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
            double typeChoseRation = typeChose / 20.0;
            double typeFileRation = typeFill / 20.0;
            double typeShortRation = typeShort / 20.0;
            double typeCompreRation = typeCompre / 20.0;

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
                // String[] splits  = allItemList.get(Integer.valueOf(expList[j].trim())).split(":");
                String[] splits = expList[j].trim().split(":");
                exp1 = exp1 + Integer.parseInt(splits[2].split(",")[0].substring(1, 2));
                exp2 = exp2 + Integer.parseInt(splits[2].split(",")[1]);
                exp3 = exp3 + Integer.parseInt(splits[2].split(",")[2]);
                exp4 = exp4 + Integer.parseInt(splits[2].split(",")[3]);
                exp5 = exp5 + Integer.parseInt(splits[2].split(",")[4].substring(0, 1));
            }

            // 属性比例 第1属性[0.2,0.4]   第2属性[0.2,0.4]   第3属性[0.1,0.3]  第4属性[0.1,0.3]  第5属性[0.1,0.3]
            //先判断是否在范围内，在的话，为0，不在的话，然后进一步和上下限取差值，绝对值
            // 23.0 可能存在误差,待研究
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
            sortListForRandom.add(minrum + "_" + ids);

        }

        Collections.sort(sortListForRandom, comp);
        sortTo50.clear();


        for (int i = 0; i < 50; i++) {
            sortTo50.add(sortListForRandom.get(i));
        }

    }


    public String[] supplementPaperGenetic() throws SQLException {

        // 单套试卷的集合
        HashSet<String> itemSet = new HashSet<>();

        for (int i = 0; i < 20; i++) {
            // 减少频繁的gc
            String item;
            // 去重操作
            while (itemSet.size() == i) {
                // 获取题目id
                item = new Random().nextInt(310) + "";
                itemSet.add(item);
            }
        }

        // 将hashSet转ArrayList 并排序
        ArrayList<String> list = new ArrayList<>(itemSet);

        // idList容器
        ArrayList<Integer> idList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            idList.add(Integer.valueOf(list.get(i)));
        }

        //list排序  目前抽取到集合为题目id
        Collections.sort(idList);


        // 根据id从数据库中查询相对应的题目
        String ids = idList.toString().substring(1, idList.toString().length() - 1);

        List<String> sList = Arrays.asList(ids.split(","));
        String[] itemArray = new String[sList.size()];

        for (int k = 0; k < sList.size(); k++) {
            itemArray[k] = allItemList.get(Integer.parseInt(sList.get(k)) - 1 > -1 ? Integer.parseInt(sList.get(k)) - 1 : 1);
        }

        System.out.println(itemArray);

        return itemArray;

    }


    /**
     * 计算均值 和 波动 情况
     *
     * @param inBack
     */
    private void calAvgFitness(ArrayList<String> inBack) {

        // 1.遍历list,计算每个个体的fitness值,并使用变量进行汇总统计
        // 计算平均值
        Double avgsum = 0.0;
        Double avg = 0.0;

        if (inBack.size() > 0) {

            for (String s : inBack) {
                avgsum = avgsum + Double.valueOf(s.split("_")[0]);
            }
            avg = avgsum / inBack.size();

        }

        // 2.sum / count
        System.out.println(avg);
        log.info("top 50%的平均适应度值：" + avg);

        // 3.计算波动  方差=1/n(s^2+....)  方差越小越稳定
        Double sdsum = 0.0;
        Double sd = 0.0;

        if (inBack.size() > 0) {

            for (String s : inBack) {
                sdsum = sdsum + Math.pow((Double.valueOf(s.split("_")[0]) - avg), 2);
            }
            sd = sdsum / inBack.size();

        }
        System.out.println(sd);
        log.info("top 50%的波动情况：" + sd);


    }



    /**
     * top 50 数据去重
     * 目的: 下游做最大圈和方差的时候,可规避去重
     * 方式：list --> set --> list
     *
     * 迭代5轮,去重效果不明显
     *
     */
    private ArrayList<String> uniqueDate(ArrayList<String> sortTo50) {

        //List转Set
        Set<String> set = new HashSet<>(sortTo50);
        System.out.println("set.size(): " + set.size());

        //Set转List
        ArrayList<String> uniqueList = new ArrayList<>(set);
        System.out.println("uniqueList.size(): " + uniqueList.size());

        return uniqueList;
    }




    /**
     * 计算适应度值
     *      ①计算方式 轮盘赌
     *      ②计算单位（单套试卷）
     *      ③比较方式 取min，按adi属性取整套试卷的最小值
     *
     * GA 初步结果:
     *       1.最大圈顶点数：3~4
     *       2.最大圈: 16~176
     *       3.平均适应度值: 26.3~30.5
     *       4.波动情况: 9.8~12.5
     *
     *
     *
     */
    @Test
    public void main() throws SQLException {

        // 轮询跑50代,方便查看结果
        for (int j = 0; j < 20; j++) {

            //构造初始试卷  100套、每套20题
            initItemBank();

            // i 迭代次数  选择交叉变异,从个体的角度出发 解决方案：加一个嵌套循环,size为种群index,然后在交叉变异中使用index
            for (int i = 0; i < ITERATION_SIZE; i++) {
                //选择
                selection();

                //交叉
                crossCover();

                //变异  新增了变异部分后,变得这么慢了吗 嵌套了一层 paperGenetic.length
                mutate();

                //精英策略
                //elitistStrategy();

            }

            System.out.println();


            // 计算适应度值,并取top 50
            getFitnessForGA();
            System.out.println(sortTo50);

            // 去重
            ArrayList<String> uniqueList  = uniqueDate(sortTo50);


            // 最大圈
            new DNDR10().similarClique(uniqueList, 1);

            // 计算均值 和 波动 情况
            calAvgFitness(uniqueList);

        }

        try {
            //防止频率过快，服务器承受不住，宕机
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }



}
