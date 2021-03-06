package cn.edu.sysu.niche.a5n3;

import cn.edu.sysu.adi.TYPE;
import cn.edu.sysu.clique.MaxcliqueV2;
import cn.edu.sysu.niche.others.BSF;
import cn.edu.sysu.niche.others.MyComparator;
import cn.edu.sysu.niche.others.Niche6;
import cn.edu.sysu.utils.CorrectUtils;
import cn.edu.sysu.utils.JDBCUtils4;
import cn.edu.sysu.utils.KLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.*;
import java.sql.SQLException;
import java.util.*;


/**
 * @Author : song bei chang
 * @create : 2022/02/08 10:12
 *
 *
 * 本周进度安排:  GA -->  收敛（重难点）  -->  最大圈clique
 *
 * 1.小生境达到稳定后,开始计算最大圈
 * 2.稳定性的判断标准：
 *   2.1 整个集群
 *   2.2 小生境的前50%
 *   2.3 leader的方差
 *   2.4 BSF(best so far) 趋于稳定 (最简单，此版本采纳这个)
 * 3.如何保证或者如何验证呢
 *   3.1 使用BSF(每代保留最优个体,与下一代做并集,随后去重,得到下一代：需验证是否会导致后代相似性过大)
 *   BSF 使用ArrayList 保存(1.大小不受限制   2.去重不方便     3.排序方便 )
 *
 *
 * IDEA 设置断点条件：
 *  在断点的位置，右击断点旁边的小红点，会出来一个界面，在Condition这里填入断点条件即可。
 *
 *
 *  诊断效果、质量(A:最大圈顶点数 平均、 B:fitness 波动)
 *  任务1:指标 计算fitness的波动
 *      1.1 验证BSF的整个适应度平均值+方差  这个更合理些 ,目前采用这个版本(平均+波动)
 *      1.2 取top10,验证适应度平均值+方差
 *
 *
 *  任务2:指标 计算个体的波动
 *      1.3 直接进行保存,然后比较个体是否一致  这个比较麻烦,验证个体变化，需确定用哪个版本做基线版本
 *
 *  任务3：题库的创建
 *      300道题       5个属性
 *      500|1000道题  8个属性
 *
 *
 *
 *  任务4：不同对象的比较
 *      NicheGA、GA、random、P-CDI
 *
 *
 *  niche GA 初步结果:
 *      1.最大圈顶点数：4~5
 *      2.最大圈: 13~48
 *      3.平均适应度值: 17.4~20.3
 *      4.波动情况:6.4~16.0
 *
 *  目前对比情况:
 *      最大圈定点数 niche GA > GA = random
 *      最大圈      GA > niche GA > random  (此处可以做一个去重操作)
 *      平均适应度值 random > GA > niche GA  (这个就很离谱了)
 *      波动情况 random ~ GA ~ niche GA     (此处可以做一个去重操作)
 *
 *  任务5: 仿真
 *      2^5 = 32 pattern、100个被试，rum
 *
 *  任务6：质量下降 查明原因
 *  任务7：改变终止规则
 *
 *
 *
 * 本周任务：
 *      1.  适应度平均值和适应度方差  是基于组卷的,代码的修改   周天上午
 *      2.  复现：P-CDI                                 找资料,并初步看文献,周天上午
 *          [24] D. I. Belov, “Uniform test assembly,” Psychometrika,  2008.
 *          [25] D. I. Belov, “Uniform test assembly: Concepts, problems, solvers, and applications for adaptive testing,”  2017.
 *          基于多目标粒子群优化的认知诊断模型平行组卷算法研究
 *          A Discrete Multiobjective Particle Swarm Optimizer for Automated Assembly of Parallel Cognitive Diagnosis Tests
 *
 *          p-CDI  是随便找一个启发式策略就好了吗?还是必须某个方案   建议先做一个demo出来,不然周五没法交差
 *
 *      3.  比较：4种模型 * 3题库                         周一解决3题库,周二进行比较
 *      4.  迭代次数过小  是否会有影响                      周三
 *
 *
 * 今下午的任务：在3题库 * 3 算法 下进行比较
 *      第一
 * 今晚上的任务：ppt
 *
 */
public class DNDR10 {

    private Logger log = Logger.getLogger(DNDR10.class);
    private KLUtils klUtils = new KLUtils();

    /**
     * 迭代次数      13min 1000代
     * 加入最大圈    17min  500代
     */
    private int iterationSize = 500;


    /**
     * 排序后的  21.32563_3,12,45....
     */
    private ArrayList<String> sortListForGene = new ArrayList<>(100);


    /**
     * 比较器
     */
    Comparator comp = new MyComparator();


    /**
     * set存leader
     */
    private HashSet<String> leaderSetForGene = new HashSet();



    /**
     * map(key是string存小生境leader, value是list存小生境member)
     */
    private HashMap<String, ArrayList<String>> mapArrayListForGene = new HashMap<>();



    /**
     * 100套试卷 20道题
     */
    private static String[][] paperGenetic = new String[100][20];

    private JDBCUtils4 jdbcUtils = new JDBCUtils4();

    /**
     * 获取题库所有题目  [8:CHOSE:(1,0,0,0,0),....] 旁路缓存的概念
     */
    private  ArrayList<String> bankList = jdbcUtils.select();

    /**
     *  size 为310  初始化,塞入到内存中
     */
    ArrayList<String> allItemList = jdbcUtils.selectAllItems();



    /**
     * 小生境对象(变异操作)
     */
    private Niche6 niche5 = new Niche6();

    /**
     * 交叉变异系数  注:变异系数有待降低(锦标赛小生境都不需要Pm)
     */
    private static double PC = 0.9;
    private static double PM = 1;

    /**
     * 程序是否终止标志（收敛成功）
     */
    Boolean timeFlag = false;


    /**
     * 计算最大圈
     */
    MaxcliqueV2 mq = new MaxcliqueV2();

    /**
     * 长度、题型、属性校验
     */
    CorrectUtils cu = new CorrectUtils();


    /**
     * bsf 操作
     */
    BSF bsfBean = new BSF();


    int lastCount = 0;


    public DNDR10() throws SQLException {
    }



    /**
     * 1. 计算每个小生境的平均值
     *      返回的样式 String : 适应度值_ids
     * 2. 过滤出大于均值的那部分
     * 3. 通过相似性形成矩阵关系(形式 vs 效应)
     * 4. 最大圈相关算法调用
     *
     */
    private void gtMeanPart(HashMap<String, ArrayList<String[]>> inMutate, ArrayList<String[]> outMutate) throws SQLException {

        System.out.println("================== calFitnessIn ==================");

        ArrayList<String> sortTo50 = new ArrayList<>();

        // 1. 遍历 inMutate,调用 calFitnessOut 即可
        for (Map.Entry<String, ArrayList<String[]>> entry : inMutate.entrySet()) {

            ArrayList<String> outList = calFitnessOut(entry.getValue());

            sortTo50.addAll(outList);

        }

        // 2. 遍历 outMutate
        ArrayList<String> outList = calFitnessOut(outMutate);
        sortTo50.addAll(outList);

        // 去重
        ArrayList<String> uniqueList  = uniqueDate(sortTo50);


        // 3. 剩下的个体计算相似性
        similarClique(uniqueList,2);

        // 4. 计算fitness的均值 和 波动
        calAvgFitness(uniqueList);


    }

    /**
     * 验证BSF的整个适应度平均值和方差, 校验效果和波动性
     * 如果校验以后发现效果不是很理想，可以换成top的平均值
     *
     * FIXME: 由之前的top50 的适应度的avg和sd转换为  平行试卷的avg
     * 1. avg的检验是否需要过滤或者做某种操作,使其可以正常比较.目前差异过大
     * 2. avg 和 sd 的计算 考虑是否个数，有时是 13套，但有时是168套，波幅较大有很大的影响  此处可以参考老师给的毕业文献
     *
     *
     */
    private void calAvgFitness(ArrayList<String> inBack) {

        // 1.遍历list,计算每个个体的fitness值,并使用变量进行汇总统计
        // 计算平均值
        Double avgsum = 0.0;
        Double avg = 0.0;

        if (inBack.size()>0) {

            for (String s : inBack) {
                avgsum = avgsum + Double.valueOf(s.split("_")[0]);
            }
            avg = avgsum / inBack.size();

        }

        // 2.sum / count
        System.out.println(avg);
        log.info("top 50%的平均适应度值：" + avg);

        // 3.计算波动  方差=1/n(s^2+....)  方差越小越稳定
        Double sdsum = 0.0 ;
        Double sd = 0.0 ;

        if (inBack.size()>0) {

            for (String s : inBack) {
                sdsum =  sdsum + Math.pow(( Double.valueOf(s.split("_")[0]) -  avg ),2);
            }
            sd = sdsum / inBack.size();

        }
        System.out.println(sd);
        log.info("top 50%的波动情况：" + sd);


    }

    /**
     * 15%,算出最大的圈 maximum clique
     * 1.通过题目相似个数,形成距离关系w矩阵
     * 2.写入文件
     * 3.读取文件,计算最大圈
     *
     */
    public void similarClique(ArrayList<String> inBack,int algorithm) {

        // 距离关系w矩阵
        int[][] distanceMatrix =new int[inBack.size()+1][inBack.size()+1];

        // 遍历计算距离关系,并生成01矩阵 初始化矩阵
        for (int i = 0; i < inBack.size()+1; i++) {

            // 赋值
            for (int j = 0; j < inBack.size() + 1; j++) {

                // 第一行
                if (i < 1) {
                    distanceMatrix[i][j] = -1;
                }

                // 第一列
                if (j == 0) {
                    distanceMatrix[i][j] = -1;
                }
            }
        }

        // 遍历集合
        for (int i = 0; i < inBack.size(); i++) {

            // 矩阵 (根据题目的相似个数 判断相似个体,若题目相同数低于4,则赋值为1)
            String aids = inBack.get(i).split("_")[1];

            for (int j = 0; j < inBack.size(); j++) {

                if (!inBack.get(i).equals(inBack.get(j))) {

                    String bids = inBack.get(j).split("_")[1];

                    // 将基因型转为list,使用list来判断相似个数
                    List<String> ListA = stringToList(aids);
                    List<String> ListB = stringToList(bids);

                    // 计算相似题目个数
                    int counter = 0;
                    for (String c : ListB) {
                        for (String d : ListA) {
                            if (c.equals(d)) {
                                counter = counter + 1;
                            }
                        }
                    }

                    // 以15%为界限  第三行开始,第二列开始 最大相似设置的过大,将导致计算缓慢
                    // 而且只能延缓  无法最终解决
                    if (counter < 4 ){

                        // 校验两个集合的相似程度
                        if (checkAttr(ListA,ListB,algorithm)){
                            distanceMatrix[i+1][j+1]=1;
                        }
                    }
                }
            }
        }

        // 打印 遍历二维数组
//        for (int i1 = 0; i1 < distanceMatrix.length; i1++) {
//            for (int i2 = 0; i2 < distanceMatrix[i1].length; i2++) {
//                System.out.print(distanceMatrix[i1][i2]+" , ");
//            }
//            System.out.println();
//        }

        // 写入文件
        sinkToFileV1(distanceMatrix);
        //sinkToFileV2(distanceMatrix);

        // 读取文件
        readFromFileV1();

        System.out.println(" + ----------------------- + ");

    }


    /**
     * 校验两个集合的相似程度
     * @param listA
     * @param listB
     *
     *  FIXME 此处应该加上其他验证逻辑 而不仅仅是题目不同
     *  功能相似： 题型、属性(拿单个题目计算还是一套试卷)
     *  思考：
     *    1.如果是单个题目的话,计算量会比较大
     *      每道题目 其余试卷的每道题挨个进行比较，意义不大
     *    2.如果是单套试卷的话,整套试卷经过了correct,其应该是相似的
     *    3.此处以单套试卷为单位，先进行计算
     *       3.1 通过ids,获取属性和类型
     *       3.2 分别计算属性和类型的数目 sum(abs())
     *       3.3 如果小于了某一个临界值,则表明其是真的相似
     */
    private Boolean checkAttr(List<String> listA, List<String> listB,int diffDegree) {

        Boolean flag = false;

        System.out.println(listA);


        // 根据id从数据库中查询相对应的题目
        String idsA = listA.toString().substring(1, listA.toString().length() - 1);

        List<String> sListA = Arrays.asList(idsA.split(","));

        // 题型
        int typeChoseA = 0;
        int typeFillA = 0;
        int typeShortA = 0;
        int typeCompreA = 0;

        // 属性
        int attributeNum1A = 0;
        int attributeNum2A = 0;
        int attributeNum3A = 0;
        int attributeNum4A = 0;
        int attributeNum5A = 0;


        for (int k = 0; k < sListA.size(); k++) {

             String s = allItemList.get(Integer.parseInt(sListA.get(k).trim())-1 > -1?Integer.parseInt(sListA.get(k).trim())-1:1);

                //计算每种题型个数
                if (TYPE.CHOSE.toString().equals(s.split(":")[1])) {
                    typeChoseA += 1;
                }
                if (TYPE.FILL.toString().equals(s.split(":")[1])) {
                    typeFillA += 1;
                }
                if (TYPE.SHORT.toString().equals(s.split(":")[1])) {
                    typeShortA += 1;
                }
                if (TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])) {
                    typeCompreA += 1;
                }


                //计算每种题型个数
                if ("1".equals(s.split(":")[2].substring(1, 2))) {
                    attributeNum1A += 1;
                }
                if ("1".equals(s.split(":")[2].substring(3, 4))) {
                    attributeNum2A += 1;
                }
                if ("1".equals(s.split(":")[2].substring(5, 6))) {
                    attributeNum3A += 1;
                }
                if ("1".equals(s.split(":")[2].substring(7, 8))) {
                    attributeNum4A += 1;
                }
                if ("1".equals(s.split(":")[2].substring(9, 10))) {
                    attributeNum5A += 1;
                }
        }


        // 根据id从数据库中查询相对应的题目
        String idsB = listB.toString().substring(1, listB.toString().length() - 1);

        List<String> sListB = Arrays.asList(idsB.split(","));

        // 题型
        int typeChoseB = 0;
        int typeFillB = 0;
        int typeShortB = 0;
        int typeCompreB = 0;

        // 属性
        int attributeNum1B = 0;
        int attributeNum2B = 0;
        int attributeNum3B = 0;
        int attributeNum4B = 0;
        int attributeNum5B = 0;


        for (int k = 0; k < sListB.size(); k++) {

            String s = allItemList.get(Integer.parseInt(sListB.get(k).trim())-1 > -1?Integer.parseInt(sListB.get(k).trim())-1:1);

            //计算每种题型个数
            if (TYPE.CHOSE.toString().equals(s.split(":")[1])) {
                typeChoseB += 1;
            }
            if (TYPE.FILL.toString().equals(s.split(":")[1])) {
                typeFillB += 1;
            }
            if (TYPE.SHORT.toString().equals(s.split(":")[1])) {
                typeShortB += 1;
            }
            if (TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])) {
                typeCompreB += 1;
            }


            //计算每种题型个数
            if ("1".equals(s.split(":")[2].substring(1, 2))) {
                attributeNum1B += 1;
            }
            if ("1".equals(s.split(":")[2].substring(3, 4))) {
                attributeNum2B += 1;
            }
            if ("1".equals(s.split(":")[2].substring(5, 6))) {
                attributeNum3B += 1;
            }
            if ("1".equals(s.split(":")[2].substring(7, 8))) {
                attributeNum4B += 1;
            }
            if ("1".equals(s.split(":")[2].substring(9, 10))) {
                attributeNum5B += 1;
            }
        }



        // 属性
        int type = Math.abs(typeChoseA - typeChoseB) + Math.abs(typeFillA - typeFillB) + Math.abs(typeShortA - typeShortB) + Math.abs(typeCompreA - typeCompreB);
        int attr = Math.abs(attributeNum1A - attributeNum1B) + Math.abs(attributeNum2A - attributeNum2B) + Math.abs(attributeNum3A - attributeNum3B) + Math.abs(attributeNum4A - attributeNum4B) + Math.abs(attributeNum5A - attributeNum5B);

        System.out.println(type + ":" + attr);

        // 可在此处做相关推断，并赋予不同的值,或者 switch case
        switch(diffDegree){
            case 1 :
                if (type < 6 && attr < 6){
                    flag = true;
                }
                break;
            case 2 :
                if (type < 6 && attr < 12){
                    flag = true;
                }
                break;
            default :
                if (type < 6 && attr < 18){
                    flag = true;
                }
        }


        return  flag;

    }

    /**
     * 读取文件,输出最大圈顶点数和最大圈的个数
     */
    private void readFromFileV1() {

        mq.readFromFileV1();

    }


    /**
     * string 转 list
     */
    private List<String> stringToList(String strs) {
        String str[] = strs.split(",");
        return Arrays.asList(str);
    }



    /**
     * 写入文件
     * @param distanceMatrix
     */
    private void sinkToFileV1(int[][] distanceMatrix) {
        OutputStream os = null;
        try {
            os = new FileOutputStream("F:\\song\\SYSU\\Log4j\\input\\output.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        PrintWriter pw=new PrintWriter(os);


        // 打印 遍历二维数组
        for (int i1 = 0; i1 < distanceMatrix.length; i1++) {
            if (i1==0){
                for (int i2 = 0; i2 < distanceMatrix[i1].length; i2++) {
                    pw.print(distanceMatrix[i1][i2]+" , ");
                }
                pw.print("333");
                pw.println();
            }

            for (int i2 = 0; i2 < distanceMatrix[i1].length; i2++) {
                pw.print(distanceMatrix[i1][i2]+" , ");
            }
            pw.print("333");
            pw.println();
        }

        pw.close();
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void sinkToFileV2(int[][] distanceMatrix) {
        OutputStream os = null;
        try {
            os = new FileOutputStream("F:\\song\\SYSU\\Log4j\\input\\outputV2.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        PrintWriter pw=new PrintWriter(os);


        // 打印 遍历二维数组
//        for (int i1 = 0; i1 < distanceMatrix.length; i1++) {
//            for (int i2 = 0; i2 < distanceMatrix[i1].length; i2++) {
//                // 将第一行第二行的-1过滤掉了
//                if(distanceMatrix[i1][i2] == 1){
//                    pw.println("V"+i1+" "+"V"+i2);
//                }
//            }
//        }

        pw.close();
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 计算适应度值,并只保留适应度大于均值的部分
     * @param list
     * @return inBack
     * @throws SQLException
     */
    private ArrayList<String> calFitnessOut(ArrayList<String[]> list) throws SQLException {

        System.out.println("================== calFitnessOut ==================");

        // 计算适应度值
        ArrayList<String> outList = getFitForGtMeanPart(list);

        ArrayList<String> inBack = new ArrayList<>();

        if (outList.size()>0){

            // 计算平均值
            Double sum = 0.0;
            Double avg = 0.0;

            for (String s : outList) {
                sum = sum + Double.valueOf(s.split("_")[0]);
            }
            avg = sum / outList.size();

            // 过滤掉小于平均值的部分
            for (String s : outList) {
                if(Double.valueOf(s.split("_")[0]) >= avg){
                    inBack.add(s);
                }
            }
        }

        return inBack;
    }


    /**
     * 计算适应度值
     * 返回：适应度_ids
     */
    private ArrayList<String> getFitForGtMeanPart(ArrayList<String[]> list) throws SQLException {


        ArrayList<String> outList = new ArrayList<>();
        // 计算试卷的适应度值，即衡量试卷优劣的指标之一 Fs
        for (int i = 0; i < list.size(); i++) {

            double adi1r = 0;
            double adi2r = 0;
            double adi3r = 0;
            double adi4r = 0;
            double adi5r = 0;

            StringBuilder idsb = new StringBuilder();

            // 获取原始adi  数组里面裹数组
            String[] itemList = list.get(i);

            // 强行手动修复 如果不行就判断里面的值是否为null
            // 加一层判断，如果itemList.length=20,则保持不变;如果否,则给paperGenetic[i] 赋予新值
            if(itemList.length != 20){
                itemList = supplementPaperGenetic();
            }

            System.out.println("-->itemList: " + Arrays.asList(itemList).toString());
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

            System.out.println("idsb.toString():"+idsb.toString());
            String ids = idsb.toString().substring(1);

            // 题型个数
            String[] expList = itemList;
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
            // 先判断是否在范围内，在的话，为0，不在的话，然后进一步和上下限取差值，绝对值
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
            outList.add(minrum + "_" + ids);

        }
        return outList;

    }



    /**
     * 自适应小生境
     * @param flag
     * @throws SQLException
     */
    private void ifSkip(boolean flag) throws SQLException {

        if (!flag){

            // 判断哪些峰需要合并(矩阵+凹点问题)  需将leaderSet 适配为 leaderSetForGene
            HashSet<String> hs = judgeMerge();

            // 合并  FIXME  个体总数：228
            merge(hs);

            // 调整初始半径
            adjustRadius();
        }

    }


    /**
     * 每次迭代置空容器 leader相关
     */
    private void iterationClear() {

        mapArrayListForGene.clear();
        leaderSetForGene.clear();
        sortListForGene.clear();

    }


    /**
     * 适应度值排序
     * 参数: paperGenetic     (全局变量)
     * 返回: sortListForGene  (全局变量)
     * 19.941320_8,16,19,21,29,30,35,50,62,69,76,107,108,133,136,173,207,222,242,299
     *
     * 新增确定收敛操作
     */
    private void sortFitnessForGene(int iter) throws SQLException {


        // 遍历二维数组,获取其适应度,然后拼接上本身  sortListForGene.add(minrum + "_" + ids)
        int paperSize = paperGenetic.length;
        getFitnessForGene(paperSize);


        // empty String
        Collections.sort(sortListForGene, comp);

        // 进行收敛确认操作
        timeFlag = bsfBean.deterministicConvergence(iter , sortListForGene);


    }



    /**
     * 将群体中的个体分配到不同小生境中 21.9524_3,27,45......
     * <p>
     * 步骤：
     * 1.选取leader
     * 注: 最后有些个体其实不应该成为leader,待后续进行合并和剔除操作
     * <p>
     * 2.选取member
     * 注: 需保证数据总数不变
     *
     * 新增方法:限制每个小生境的数量  不大于
     * 1.判断是否有大于1/2,有的话将其抛成三部分
     * 2.若无,则直接返回
     *
     */
    private void distributeNicheForGene() {

        // 选取leader
        for (int i = 0; i < sortListForGene.size(); i++) {

            // 选择第一个小生境leader
            if (leaderSetForGene.size() == 0) {

                leaderSetForGene.add(sortListForGene.get(i));

            } else {

                String aids = sortListForGene.get(i).split("_")[1];

                // 使用一个计数器,比对两个集合的相似题目数
                int max = 0;
                // 获取目前leader的信息
                for (String leader : leaderSetForGene) {

                    // b 的判断应该和其余全部的leader进行判断
                    String bids = leader.split("_")[1];

                    // 判断两套试卷的相似程度,如果相似题目数达到一定数目，则判定为是同一个小生境 如3，
                    // 将基因型转为list,使用list来判断相似个数
                    List<String> ListA = stringToList(aids);
                    List<String> ListB = stringToList(bids);

                    // 假设上面ListA 和 ListB都存在数据
                    // aids：3,4,9,28,34,36,43,52,59,102,116,126,129,138,145,213,230,233,247,267
                    // bids：8,10,17,18,24,25,26,39,71,72,81,84,92,102,107,141,143,150,273,303


                    // 使用题目相同的个数进行判断相似性
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

                // 若重复的题目小于3,则不隶属于现存的任何一个小生境,故新增一个leader  注释:count 和 mark 需要相互对应
                if (max < 3) {
                    leaderSetForGene.add(sortListForGene.get(i));
                }
            }
        }

        // 选取member leaderSetForGene 表示小生境的峰值
        int sum = 0;
        //log.info("小生境数目: " + leaderSetForGene.size());

        for (String leader : leaderSetForGene) {

            ArrayList<String> memberList = new ArrayList<>();
            String aids = leader.split("_")[1];

            for (int i = 0; i < sortListForGene.size(); i++) {
                String s = sortListForGene.get(i);
                // 判空操作 (因为后续会做标记,将value值重置为空   注释:s 是ArrayList的一个值,循环遍历 200 * 小生境数)
                if (!StringUtils.isBlank(s)) {
                    String bids = s.split("_")[1];

                    // 判断两套试卷的相似程度: 如果相似题目数达到一定数目，则判定为是同一个小生境 如1
                    List<String> ListA = stringToList(aids);
                    List<String> ListB = stringToList(bids);

                    // 计算A与B之间的相似题目数
                    int mark = 0;
                    for (String a : ListA) {
                        for (String b : ListB) {
                            if (a.equals(b)) {
                                mark = mark + 1;
                            }
                        }
                    }
                    // 如AB足够相似，则存放入某一具体小生境中，并将该值在sortListForGene中重置为null
                    if (mark >= 3) {
                        memberList.add(s);
                        sortListForGene.set(i, "");
                    }
                }
            }

            mapArrayListForGene.put(leader, memberList);

            sum = memberList.size() + sum;
        }
        System.out.println("此次迭代个体总数目：" + sum);



        // 新增方法:限制每个小生境的数量  不大于1/2
        HashSet<String> leaderSetForGeneV2 = new HashSet();
        HashMap<String, ArrayList<String>> mapArrayListForGeneV2 = new HashMap<>();

        Iterator<Map.Entry<String, ArrayList<String>>> iterator = mapArrayListForGene.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<String, ArrayList<String>> entry = iterator.next();

            ArrayList<String> numbers = entry.getValue();
            String key = entry.getKey();
            // 是否小生境个数 >= 1/2
            if (entry.getValue().size() >= sortListForGene.size()/2) {
                String hk = numbers.get(0);
                String mk = numbers.get(numbers.size()/3);
                String tk = numbers.get(numbers.size()/3*2);
                List<String> head =  numbers.subList(0, numbers.size()/3);
                List<String> middle =  numbers.subList(numbers.size()/3, numbers.size()/3*2);
                List<String> tail =  numbers.subList(numbers.size()/3*2, numbers.size());
                //3、增强for
                leaderSetForGeneV2.add(hk);
                ArrayList<String> hv = new ArrayList<>();
                for(String a : head){
                    hv.add(a);
                }
                mapArrayListForGeneV2.put(hk, hv);

                leaderSetForGeneV2.add(mk);
                ArrayList<String> mv = new ArrayList<>();
                for(String a : middle){
                    mv.add(a);
                }
                mapArrayListForGeneV2.put(mk, mv);

                leaderSetForGeneV2.add(tk);
                ArrayList<String> tv = new ArrayList<>();
                for(String a : tail){
                    tv.add(a);
                }
                mapArrayListForGeneV2.put(tk, tv);

            }else{
                leaderSetForGeneV2.add(key);
                mapArrayListForGeneV2.put(key, numbers);
            }
        }

        leaderSetForGene = leaderSetForGeneV2;
        mapArrayListForGene = mapArrayListForGeneV2;

        log.info("小生境个数: " + mapArrayListForGene.size());
    }









    /**
     * 使用构造法选取题目  (轮盘赌） 生成 paperGenetic  = new String[100][20] 试卷100套,每套20题  为交叉变异提供原始材料
     * 1.题型构造 （不考虑下限比例）
     * 2.属性构造 （不考虑下限比例）
     * 设置比例  可以通过惩罚系数来设定  超出,则急剧减少
     *
     * 总结：在初始化的时候，不需要完全保证题型和属性符合要求，后续使用GA迭代和轮盘赌解决即可
     *
     */
    private void initItemBank() throws SQLException {

        System.out.println("====== 开始选题,构成试卷  轮盘赌构造 ======");

        // 试卷|个体大小  提供遗传变异的大单位
        int paperNum = paperGenetic.length;

        // 题目|基因大小  交叉变异的基本单位
        int questionsNum = paperGenetic[0].length;

        // 单套试卷的集合
        HashSet<String> itemSet = new HashSet<>();


        // 获取题库所有题目  [8:CHOSE:(1,0,0,0,0),....] 旁路缓存的概念
        //bankList = getBank();

        // 生成了二维数组 paperGenetic
        for (int j = 0; j < paperNum; j++) {

            // 清空上一次迭代的数据
            itemSet.clear();

            for (int i = 0; i < questionsNum; i++) {

                // 减少频繁gc
                String item;

                // 去重操作
                while (itemSet.size() == i) {
                    // 获取题目id   轮盘赌构造法
                    int sqlId = roulette(itemSet);
                    // 两个id相差1,保证选题无偏差  FIXME：此处可优化,使用全局缓存 甚至可完全规避，直接使用ids即可
                    item = jdbcUtils.selectOneItem(sqlId + 1);
                    itemSet.add(item);
                }
            }

            // 将hashSet转ArrayList
            ArrayList<String> list = new ArrayList<>(itemSet);

            // 提取id
            ArrayList<Integer> idList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                idList.add(Integer.valueOf(list.get(i).split(":")[0]));
            }

            // list排序
            Collections.sort(idList);


            // 根据id从数据库中查询相对应的题目
            String ids = idList.toString().substring(1, idList.toString().length() - 1);

            List<String> sList = Arrays.asList(ids.split(","));
            String[] itemArray = new String[sList.size()];

            for (int k = 0; k < sList.size(); k++) {
                itemArray[k] = allItemList.get(
                        Integer.parseInt(sList.get(k).trim())-1 > -1?Integer.parseInt(sList.get(k).trim())-1:1
                );
            }


            // 赋值  把题库提升为全局变量,方便整体调用 容器：二维数组
            paperGenetic[j] = itemArray;
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
                item = new Random().nextInt(310)+"";
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
            itemArray[k] = allItemList.get(Integer.parseInt(sList.get(k))-1 > -1?Integer.parseInt(sList.get(k))-1:1);
        }

        System.out.println(itemArray);

        return itemArray;

    }





    /**
     * 逻辑判断 ：轮盘赌构造选题
     *  1.id不能重复
     *  2.题型|属性比例 ( 影响适应度 )
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
     * 1.根据已选题目，对题库中每道题目的适应度值进行相应的惩罚
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


        // 题型和属性比例 与轮盘赌搭建关系：
        //      已抽取的属性个数越多，则惩罚系数越大 且各个属性是累乘关系
        //      比例和一个固定值做比较即可  eg: typeChose/10    AttributeRatio1/23
        //      如果未超出比例，则按照正常流程走，一旦超过，则适应度值急剧下降
        for (int j = 0; j < bankList.size(); j++) {
            double penalty = 1;
            String[] splits = bankList.get(j).split(":");

            // 题型比例
            // 为什么小于也要做惩罚，因为选取了一次，需要实时统计比例信息，并获取惩罚系数，小于相对于完全未选择那种
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


        return fitPro;
    }


    /**
     * 返回：每套试卷的适应度_ids
     * <p>
     * 方案  进行乘以一个exp 来进行适应度值的降低，高等数学里以自然常数e为底的指数函数
     * 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3]  应用[0.1,0.3]
     * 属性比例 第1属性[0.2,0.4]  第2属性[0.2,0.4]  第3属性[0.1,0.3] 第4属性[0.1,0.3] 第5属性[0.1,0.3]
     */
    private void getFitnessForGene(int paperSize) throws SQLException {

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

            // 强行手动修复 如果不行就判断里面的值是否为null
            // 加一层判断，如果itemList.length=20,则保持不变;如果否,则给paperGenetic[i] 赋予新值
            if(itemList.length != 20){
                 itemList = supplementPaperGenetic();
            }

            System.out.println("-->itemList: " + Arrays.asList(itemList).toString());
            for (int j = 0; j < itemList.length; j++) {

                /**
                 *
                 * -->itemList: [null, null, null, null, null, null, null]
                 * -->itemList.length: 20
                 * j: 0
                 * itemList[j]: null
                 *
                 * paperGenetic 在上一轮出现了问题,长度为20,但里面的值均为null。
                 * 校验方案:在每层做一个长度及null值的校验
                 */

                String[] splits = itemList[j].split(":");
                adi1r = adi1r + Double.parseDouble(splits[3]);
                adi2r = adi2r + Double.parseDouble(splits[4]);
                adi3r = adi3r + Double.parseDouble(splits[5]);
                adi4r = adi4r + Double.parseDouble(splits[6]);
                adi5r = adi5r + Double.parseDouble(splits[7]);

                // 拼接ids
                idsb.append(",").append(splits[0]);


            }

            System.out.println("idsb.toString():"+idsb.toString());
            String ids = idsb.toString().substring(1);

            // 题型个数
            String[] expList = itemList;
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
     * 返回 单套试卷的适应度_ids
     */
    private Double getFitnessForOne(String[] itemArray) {


        // 计算试卷的适应度值，即衡量试卷优劣的指标之一 Fs

        double adi1r = 0;
        double adi2r = 0;
        double adi3r = 0;
        double adi4r = 0;
        double adi5r = 0;

        StringBuilder idsb = new StringBuilder();

        // 获取原始adi  数组里面裹数组
        //String[] itemList = paperGenetic[0];
        for (int j = 0; j < itemArray.length; j++) {

            /**
             *
             * -->itemList: [null, null, null, null, null, null, null, null, null, null, null, null, null, null]
             * -->itemList.length: 20
             * j: 0
             * itemList[j]: null
             */
            //System.out.println("-->itemList: " + Arrays.asList(itemArray));
            //System.out.println("-->itemList.length: " + itemArray.length);
            //System.out.println("j: " + j);
            //System.out.println("itemList[j]: " + itemArray[j]);

            String[] splits = itemArray[j].split(":");
            adi1r = adi1r + Double.parseDouble(splits[3]);
            adi2r = adi2r + Double.parseDouble(splits[4]);
            adi3r = adi3r + Double.parseDouble(splits[5]);
            adi4r = adi4r + Double.parseDouble(splits[6]);
            adi5r = adi5r + Double.parseDouble(splits[7]);

            // 拼接ids
            idsb.append(",").append(splits[0]);

        }

        String ids = idsb.toString().substring(1);
        //System.out.println(ids);

        // 题型个数
        String[] expList = paperGenetic[0];
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

        // 本身的基因型选用id拼接,其具有代表性
        return Double.valueOf(minrum);
    }


    /**
     * 小生境内选择
     * 底层调用 selectionOut()
     */
    private HashMap<String, ArrayList<String>> selectionIn(HashMap<String, ArrayList<String>> inListHashMap) {

        System.out.println("====================== select In ======================");

        HashMap<String, ArrayList<String>> inBack = new HashMap<>();

        // 外围嵌上一层循环，然后转化为多个list，挨个执行 selectionOut 即可
        // 因为适应度是降序排列的,执行精英策略时是否会有影响  后续考虑

        for (Map.Entry<String, ArrayList<String>> entry : inListHashMap.entrySet()) {
            //System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue())
            ArrayList<String> outBack = selectionOut(entry.getValue());
            inBack.put(entry.getKey(), outBack);
        }

        return inBack;
    }


    /**
     * 小生境外选择
     * 选择: 以适应度为导向,轮盘赌为策略, 适者生存和多样性的权衡
     * <p>
     * ①计算适应度：以试卷为单位，min*exp^1
     * ②轮盘赌进行筛选 返回 outBack
     * <p>
     * 选择（轮盘赌）：择优录取+多样式减低
     * 交叉+变异：增加多样性(外部作用)
     */
    private ArrayList<String> selectionOut(ArrayList<String> outList) {

        System.out.println("====================== select Out ======================");

        System.out.println("select 进入outList的size: " + outList.size());
        ArrayList<String> outBack = new ArrayList<>();

        if (outList.size() > 0) {

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
            int newSelectId = 0;

            for (int i = 0; i < paperSize; i++) {
                while (newSelectId < paperSize && randomId[newSelectId] < fitPie[i]) {
                //while (newSelectId < paperSize && randomId[i] < fitPie[newSelectId]) {

                    newSelectId += 1;
                    outBack.add(outList.get(i));
                }
            }

        }
        System.out.println("select 归还outList的size: " + outBack.size());

        return outBack;

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
     * <p>
     * selection 计算适应度值
     * 方案  进行乘以一个exp 来进行适应度值的降低，高等数学里以自然常数e为底的指数函数
     * 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3]  应用[0.1,0.3]
     * 属性比例 第1属性[0.2,0.4]  第2属性[0.2,0.4]  第3属性[0.1,0.3] 第4属性[0.1,0.3] 第5属性[0.1,0.3]
     * <p>
     * <p>
     * 方案一:
     * 将 list 转化为 gene 数组( 其中借助select * from table,初次是没问题的，但其会随着交叉变异而个体的变化，故需定义一个全局变量 时刻维护 )
     * paperGenetic[i] = 1:FILL:(0,0,0,0,1):0.0:0.0:0.0:0.0:0.055000000000000035 * 20
     * value[i] = 19.941320442946314_8,16,19,21,29,30,35,50,62,69,76,107,108,133,136,173,207,222,242,299
     * <p>
     * 方案二:
     * 直接使用 list 计算,因为首位已经是计算过的适应度值  此处采用
     */
    private double[] getFitnessNiche(ArrayList<String> outList) {

        //log.info("适应值 log4j")
        int paperSize = outList.size();

        // 所有试卷的适应度总和
        double fitSum = 0.0;

        // 每套试卷的适应度值
        double[] fitTmp = new double[paperSize];

        // 每套试卷的适应度占比  返回结果部分
        double[] fitPro = new double[paperSize];

        // 计算试卷的适应度值,即衡量试卷优劣的指标之一 Fs
        for (int i = 0; i < paperSize; i++) {
            // 对outList进行拆分,获取其前半部分的适应度值，并赋值给fitTmp[]
            fitTmp[i] = Double.parseDouble(outList.get(i).split("_")[0]);
            fitSum = fitSum + fitTmp[i];
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
     * 交叉
     * 底层调用 crossCoverOut()
     */
    private HashMap<String, ArrayList<String>> crossCoverIn(HashMap<String, ArrayList<String>> inListHashMap) throws SQLException {


        HashMap<String, ArrayList<String>> inBack = new HashMap<>();

        // 调用 selectionOut 即可
        for (Map.Entry<String, ArrayList<String>> entry : inListHashMap.entrySet()) {

            ArrayList<String> outBack = crossCoverOut(entry.getValue());
            inBack.put(entry.getKey(), outBack);

        }

        return inBack;

    }


    /**
     * 交叉  此处不涉及适应度
     * ①交叉的单位:  题目
     * <p>
     * random.nextInt(n)  指生成一个介于[0,n)的int值
     * <p>
     * List 是否需要转化为 paperGenetic[1]
     * 交叉部分可以不转化，因为最小单位为题目，而不是题目里面的题型和属性
     * 变异部分可以不转化，因为最小单位为题目，而不是题目里面的题型和属性
     */
    private ArrayList<String> crossCoverOut(ArrayList<String> outList) throws SQLException {


        System.out.println("cross 进入outList的size: " + outList.size());

        //  获取长度基本信息
        //  单点交叉(只保留交叉一个个体)
        int size = outList.size();
        //  数组转list
        ArrayList<String> re = new ArrayList<>();

        // size - 2 适配 交叉
        if (size - 2 > 0) {
            // 单点交叉与否是否会影响,
            int point = outList.get(0).split("_")[1].split(",").length;

            // 将outList转为数组List<arr>
            ArrayList<String[]> arr = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                String str = outList.get(i).split("_")[1];
                arr.add(str.split(","));
            }


            for (int i = 0; i < size - 2; i++) {

                if (arr.get(i).length == 20 && arr.get(i + 1).length == 20) {

                    // 根据概率判断是否进行交叉
                    if (Math.random() < PC) {
                        String[] temp = new String[point];
                        int a = new Random().nextInt(point);

                        for (int j = 0; j < a; j++) {
                            temp[j] = arr.get(i)[j];
                        }

                        for (int j = a; j < point; j++) {
                            temp[j] = arr.get(i + 1)[j];
                        }

                        // 执行校验(数量 题型 属性)
                        arr.set(i, correct(temp));
                        // arr.set(i, temp);

                    }
                } else {
                    System.out.println("arr.size 不符合要求 待优化");
                }
            }


            // 遍历数组
            for (int i = 0; i < arr.size(); i++) {
                String[] stringArray = arr.get(i);
                String str1 = StringUtils.join(stringArray, ",");
                re.add(str1);
            }
            //System.out.println(re);
        } else {
            // 此处需对outList做处理,否则样式不一致
            for (int i = 0; i < outList.size(); i++) {
                String str = outList.get(i).split("_")[1];
                re.add(str);
            }
            //System.out.println(re);
            //re = outList;
        }


        System.out.println("cross 归还outList的size: " + re.size());
        //if (paperGenetic[19][199] != null ){ System.out.println("cross 归还 paperGenetic[19][199] 不为null"); }
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

        // 根据id的位置，映射，重新排序 tmp2
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
     * 变异
     * 底层调用 mutateOut()
     */
    private HashMap<String, ArrayList<String[]>> mutateIn(HashMap<String, ArrayList<String>> inCross) throws SQLException {

        System.out.println("================== mutateIn ==================");

        HashMap<String, ArrayList<String[]>> inBack = new HashMap<>();

        // 调用 mutateOut 即可
        for (Map.Entry<String, ArrayList<String>> entry : inCross.entrySet()) {

            ArrayList<String[]> outBack = mutateOut(entry.getValue());

            inBack.put(entry.getKey(), outBack);

        }

        return inBack;


    }


    /**
     * 变异  (长度，属性类型，属性比例)
     * <p>
     * outCross.get(1):2,9,12,34,36,39,42,49,69,72,90,91,102,112,123,137,168,197,219,227
     */
    private ArrayList<String[]> mutateOut(ArrayList<String> outCross) throws SQLException {

        System.out.println("================== mutateOut ==================");

        // 创建一个方法内变量,用于接收c1,并最后返回
        ArrayList<String[]> outMutate = new ArrayList<>();

        //  限制性锦标赛拥挤小生境
        //PM = 0.41;

        // 判空操作
        if (outCross.size() > 0) {

            // 遍历
            //for (int j = 0; j < outCross.size()-1 ; j++) {
            for (int j = 0; j < outCross.size(); j++) {


                if (Math.random() < PM) {

                    // 限制性锦标赛拥挤小生境  传进去是单个个体，返回的单个个体
                    ArrayList<String[]> rts = niche5.RTS(outCross, j);

                    String[] c1 =  rts.get(0);
                    // 执行修补操作
                    String[] c2 = cu.correctTypeV2(c1);
                    String[] c3 = cu.correctAttributeV2(c2);

                    outMutate.add(c3);


                } else {

                    String ids = outCross.get(j);
                    List<String> sList = Arrays.asList(ids.split(","));
                    String[] itemArray = new String[sList.size()];

                    for (int k = 0; k < sList.size(); k++) {
                        //System.out.println("K:"+k);
                        //System.out.println("sList:"+sList);
                        itemArray[k] = allItemList.get(Integer.parseInt(sList.get(k))-1 > -1?Integer.parseInt(sList.get(k))-1:1);
                    }
                    String[] c3 = cu.correctAttributeV2(itemArray);
                    outMutate.add(c3);
                }
            }
        }

        return outMutate;

    }


    /**
     * 将变异后的各个小生境内的基因进行合并
     * gene = inMutate + outMutate
     */
    private String[][] mergeToGene(HashMap<String, ArrayList<String[]>> inMutate, ArrayList<String[]> outMutate) {

        String[][] gene = new String[100][20];
        int index = 0;


        for (Map.Entry<String, ArrayList<String[]>> entry : inMutate.entrySet()) {

            for (String[] strings : entry.getValue()) {
                gene[index] = strings;
                index = index + 1;
            }
        }

        for (String[] strings : outMutate) {
            gene[index] = strings;
            index = index + 1;
        }


        return gene;
    }


    /**
     * 判断哪些峰需要合并(矩阵+凹点问题)  leaderSetForGene
     * 用距离w矩阵(动态大小的二维数组)来表示任意两个小生境之间的关系（注：01矩阵,后续需做去重处理）
     * <p>
     * 1.获取距离矩阵
     * 2.将距离矩阵简化(去重、最小值),只将要合并的集合返回
     *
     * leaderSetForGene:
     * 19.9413204_8,16,19,21,29,30,35,50,62,69,76,107,108,133,136,173,207,222,242,299
     */
    private HashSet<String> judgeMerge() throws SQLException {

        // 距离w矩阵
        // leaderSetForGene  即使有为1的情况,也不影响,因为这里做的是获取要合并的集合,为1显然被排除在外。逻辑之间不冲突
        int[][] distanceMatrix = new int[leaderSetForGene.size()][leaderSetForGene.size()];

        // hashSet 转 arrayList
        List<String> leaderList = new ArrayList<>(leaderSetForGene);

        // 遍历计算距离关系,并生成01矩阵
        for (int i = 0; i < leaderList.size(); i++) {

            // 使用一个计数器,比对两个集合的相似题目数
            int max = 0;
            int a = 0;
            int b = 0;

            // 矩阵是根据题目的相似个数 leaderList *  leaderList 寻找最近的个体
            String aids = leaderList.get(i).split("_")[1];

            for (int j = 0; j < leaderList.size(); j++) {
                if (!leaderList.get(i).equals(leaderList.get(j))) {

                    // 获取目前leader的信息
                    // b 的判断应该和全部的leader进行判断
                    String bids = leaderList.get(j).split("_")[1];

                    // 将基因型转为list,使用list来判断相似个数
                    List<String> ListA = stringToList(aids);
                    List<String> ListB = stringToList(bids);

                    // 使用题目个数进行判断相似性
                    int counter = 0;
                    for (String c : ListB) {
                        for (String d : ListA) {
                            if (c.equals(d)) {
                                counter = counter + 1;
                            }
                        }
                    }
                    // 取出最大值
                    if (counter > max) {
                        a = i;
                        b = j;
                        max = counter;
                    }
                }
            }


            // FIXME 难点:现凹点验证是随机交叉来获取中间值
            // 随机选取2个点进行凹点验证, 多个随机值中,只要存在凹点,证明这两个相邻的小生境是独立的,不需要合并
            // 为了消除噪音干扰,定义了一个忍受因子 sf=0.9
            String bids = leaderList.get(b).split("_")[1];
            double sf = 0.9;
            boolean flag = true;
            final int GER_RND = 2;

            for (int j = 0; j < GER_RND; j++) {

                // 1.原横坐标单点交叉  (1-r)a + rb   2.基因交叉
                //  将outList转为数组List<arr>
                String[] arr = aids.split(",");
                String[] brr = bids.split(",");

                int point = 20;
                String[] temp = new String[point];
                int e = new Random().nextInt(point);

                for (int f = 0; f < e; f++) {
                    // debug 进行测试  如果size()不等于20 ，则此时先过滤
                    temp[f] = arr[f];
                }

                for (int g = e; g < point; g++) {
                    // debug 进行测试  如果size()不等于20 ，则此时先过滤
                    temp[g] = brr[g];
                }

                // temp 为交叉后的新个体
                // 数组转字符串(逗号分隔),然后从题库中搜索
                String ids = StringUtils.join(temp, ",");

//                ArrayList<String> bachItemList = jdbcUtils.selectBachItem(ids);
//
//                // ArrayList 转 []
//                // 交叉变异的针对的是题目   即试卷=个体  题目=基因
//                String[] itemArray = new String[bachItemList.size()];
//                for (int h = 0; h < bachItemList.size(); h++) {
//                    itemArray[h] = bachItemList.get(h);
//                }

                List<String> sList = Arrays.asList(ids.split(","));
                String[] itemArray = new String[sList.size()];

                for (int k = 0; k < sList.size(); k++) {
                    itemArray[k] = allItemList.get(Integer.parseInt(sList.get(k))-1 > -1?Integer.parseInt(sList.get(k))-1:1);
                }

                // 获取单个个体的适应度值
                Double cv = getFitnessForOne(itemArray);


                // 计算适应度值,并进行比较
                Double av = Double.valueOf(leaderList.get(a).split("_")[0]);
                Double bv = Double.valueOf(leaderList.get(b).split("_")[0]);


                //System.out.println(cv + "," + sf * av + "," + sf * bv);

                // 存在凹点(注释: 中间值比两端小)
                if (cv < sf * av && cv < sf * bv) {
                    flag = false;
                }
            }

            // 未出现凹点,需要合并的最终因子,通过ab索引,赋值为1,其余赋值为0
            if (flag) {
                distanceMatrix[a][b] = 1;
            }
        }

        // 打印验证 遍历二维数组
        for (int i1 = 0; i1 < distanceMatrix.length; i1++) {
            for (int i2 = 0; i2 < distanceMatrix[i1].length; i2++) {
                System.out.print(distanceMatrix[i1][i2] + " , ");
            }
            System.out.println();
        }


        /**
         * 0 , 0 , 0 , 0 , 0 , 0 , 0 , 0
         * 0 , 0 , 0 , 0 , 1 , 0 , 0 , 0
         * 0 , 0 , 0 , 0 , 0 , 0 , 0 , 0
         * 1 , 0 , 0 , 0 , 0 , 0 , 0 , 0
         * 0 , 0 , 0 , 0 , 0 , 0 , 0 , 0
         * 0 , 0 , 0 , 0 , 0 , 0 , 0 , 0
         * 0 , 0 , 0 , 0 , 0 , 1 , 0 , 0
         * 0 , 1 , 0 , 0 , 0 , 0 , 0 , 0
         */


        // 2. 对距离矩阵进行去重
        // 2.1 获取将要合并的两个峰的下标  对ab排序,将小数字放在前面,然后使用HashSet解决数据重复问题
        HashSet<String> hs = new HashSet<>();
        for (int i1 = 0; i1 < distanceMatrix.length; i1++) {
            for (int i2 = 0; i2 < distanceMatrix.length; i2++) {

                if (distanceMatrix[i1][i2] == 1) {
                    if (i1 < i2) {
                        hs.add(leaderList.get(i1) + ":" + leaderList.get(i2));
                    } else {
                        hs.add(leaderList.get(i2) + ":" + leaderList.get(i1));
                    }
                }

            }
        }
        // 验证去重效果 hs.size(): 0 hs.toString(): []
        //("验证去重效果 hs.size(): " + hs.size() + " hs.toString(): " + hs.toString());


        // 若无需要合并的因子,则跳过以下步骤直接返回null
        if (hs.size() > 0) {
            // 2.2 需进一步考虑 [0:2, 2:5] 即 0,2 和 2,5
            // 在hs中统计各个峰值出现的次数  将横坐标放置到list集合中 hs.toString(): [fitness_ids:fitness_ids]
            ArrayList<String> indexList = new ArrayList<>();
            for (String h : hs) {
                // leader
                String s1 = h.split(":")[0];
                String s2 = h.split(":")[1];
                String l1 = s1.split("_")[1];
                String l2 = s2.split("_")[1];
                // 存峰值的位置(此处是ids)
                indexList.add(l1);
                indexList.add(l2);
            }

            // 统计次数大于1的峰
            List<String> gtOneList = frequencyGtOne(indexList);
            //System.out.println(gtOneList);

            // 将次数等于1的找出来
            List<String> eqOneList = findEqOne(hs, gtOneList);

            // 2. 最终需要合并的集合容器
            HashSet<String> allHs = new HashSet<>();
            // 2.1 全量新增eqOneList
            allHs.addAll(eqOneList);


            // 2.2 智能新增gtOneList 分组计算出哪个最近,本质是进行一次过滤操作
            // 将hs和doubleList遍历,重复的进行进一步选择,每一个doubleList只返回一个距离最近的  set.get() != null
            // 对gtOneList做非空判断
            if (gtOneList != null) {
                for (String aDouble : gtOneList) {

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
                        // 遍历tmpList,进行比较,只留下最近的一个 fitness_ids
                        int max = 0;

                        for (String tmp : tmpList) {
                            String s1 = tmp.split(":")[0];
                            String s2 = tmp.split(":")[1];
                            String aids = s1.split("_")[1];
                            String bids = s2.split("_")[1];
                            // 距离计算,ids的相似个数
                            // string 转 list ,计算两个list的最大相似数
                            // 将基因型转为list,使用list来判断相似个数
                            List<String> listA = stringToList(aids);
                            List<String> listB = stringToList(bids);

                            // 假设上面ListA 和 ListB都存在数据
                            // aids：3,4,9,28,34,36,43,52,59,102,116,126,129,138,145,213,230,233,247,267
                            // bids：8,10,17,18,24,25,26,39,71,72,81,84,92,102,107,141,143,150,273,303

                            // 使用题目个数进行判断相似性
                            int counter = 0;
                            for (String c : listB) {
                                for (String d : listA) {
                                    if (c.equals(d)) {
                                        counter = counter + 1;
                                    }
                                }
                            }

                            if (max < counter) {
                                max = counter;
                                t = tmp;
                            }

                        }
                    }
                    // 最近的一组t
                    //System.out.println("最近的一组以及找到:" + t);
                    allHs.add(t);
                }
            }

            //System.out.println(allHs);
            return allHs;
        }


        return null;
    }


    /**
     * 格式转换工具, 保留小数点后三位
     */
    public Double numbCohesion(Double adi) {

        return Double.valueOf(String.format("%.3f", adi));

    }


    /**
     * java统计List集合中每个元素出现的次数
     * <p>
     * 例如 frequencyOfListElements("111","111","222")
     *     则返回次数大于1的key {"111"=2,"222"=1} --> 111
     */
    public List<String> frequencyGtOne(List<String> items) {

        // 过滤空值
        if (items == null || items.size() == 0) {
            return null;
        }

        Map<String, Integer> map = new HashMap<>();

        for (String temp : items) {
            Integer count = map.get(temp);
            map.put(temp, (count == null) ? 1 : count + 1);
        }

        // 遍历map,将value>1的key存储下来
        List<String> list = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : map.entrySet()) {

            if (entry.getValue() > 1) {
                list.add(entry.getKey());
            }

        }
        return list;
    }


    /**
     * 查找 只出现一次的峰
     * <p>
     * 1.排除掉不等于1的元素即可
     * 2.hs和doubleList的遍历判断过程  √
     */
    private List<String> findEqOne(HashSet<String> hs, List<String> gtOneList) {

        // 未出现在gtOneList中
        boolean oneOnlyAppear = true;
        ArrayList<String> eqOneList = new ArrayList<>();
        for (String h : hs) {
            for (String aDouble : gtOneList) {
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
     * 参数:  hs [fitness_ids:fitness_ids]
     * 集合成员调整: 取大leader做总leader,两个List集合进行合并
     * 半径调整:    选择大leader和小leader的相似程度作为半径
     * 个体剔除:
     * 目的：调整的是各个小生境的半径,剔除不需要的个体以及获取峰半径,
     * 便于随后的交叉变异,在某个具体的小生境范围内进行GA
     */
    private int ct = 0;
    private int nn = 0;

    private void merge(HashSet<String> hs) {

        // 验证当前总个数
        Iterator test = mapArrayListForGene.entrySet().iterator();
        int sum2 = 0;
        while (test.hasNext()) {
            Map.Entry entry = (Map.Entry) test.next();
            String key = (String) entry.getKey();
            ArrayList<String> val = (ArrayList<String>) entry.getValue();
            sum2 = sum2 + val.size();
        }
        System.out.println("merge 前个数："+sum2);


        if (!hs.isEmpty()) {
            for (String h : hs) {
                // 根据适应度值确定leader   合并判断值错误,待优化
                String s1 = h.split(":")[0];
                String s2 = h.split(":")[1];
                double l1 = Double.valueOf(s1.split("_")[0]);
                double l2 = Double.valueOf(s2.split("_")[0]);
                if (l1 > l2) {
                    // 集合成员调整 arrayList + arrayList
                    mapArrayListForGene.get(s1).addAll(mapArrayListForGene.get(s2));
                    mapArrayListForGene.remove(s2);
                    // 半径调整1
                    //radiusMerge(s1, s2);
                } else {
                    // 集合成员调整
                    mapArrayListForGene.get(s2).addAll(mapArrayListForGene.get(s1));
                    mapArrayListForGene.remove(s1);
                    // 半径调整1
                    //radiusMerge(s2, s1);
                }
            }



            // 集合调整
            // 打印操作
//            Iterator iter = mapArrayListForGene.entrySet().iterator();
//            //System.out.println("过滤前:");
//            while (iter.hasNext()) {
//                Map.Entry entry = (Map.Entry) iter.next();
//                Object key = entry.getKey();
//                Object val = entry.getValue();
//                //System.out.println("key: " + key + "  val：" + val);
//            }
//
//            // 将hs进行过滤,保留最新的mapArrayList集合
//            // 此处将低峰也移除,是否会影响  不会,因为之前做过一次合并 addAll()
//            for (String h : hs) {
//                mapArrayListForGene.remove(h.split(":")[0]);
//                mapArrayListForGene.remove(h.split(":")[1]);
//            }
//
//            // HashMap遍历，此种方式效率高
//            //System.out.println("过滤后:");
//            Iterator iter2 = mapArrayListForGene.entrySet().iterator();
//            while (iter2.hasNext()) {
//                Map.Entry entry = (Map.Entry) iter2.next();
//                Object key = entry.getKey();
//                Object val = entry.getValue();
//                //System.out.println("key: " + key + "  val：" + val);
//            }

            // 个体剔除操作
            // 用于存储临时集合
            // FIXME 2.侧输出流。可以两个大的生成一个大的+小的   侧输出流的数据需要进一步保存  此处先省略,待下次优化
            ArrayList<String> sideList = new ArrayList<>();
            int sum = 0;

//        Iterator iter3 = mapArrayListForGene.entrySet().iterator();
//        while (iter3.hasNext()) {
//
//            Map.Entry entry = (Map.Entry) iter3.next();
//            String key = (String) entry.getKey();
//            ArrayList<String> val = (ArrayList<String>) entry.getValue();
//
//            // 过滤出样式为value_key_radius的个体,只有这一部分个体半径需要做修改
//            if (key.split("_").length == 3) {
//                // 用于存储集合
//                ArrayList<String> arrayList = new ArrayList<>();
//                // 进行剔除操作
//                // val：[fitness_ids, fitness_ids,....]
//                // String subStr = val.toString().substring(1, val.toString().length() - 1);
//                // FIXME 以逗号为分隔符是存在问题的
//                // String[] splitArr = subStr.split(",");
//
//                // 寻找适应度值最低和最高的个体
//                // FIXME 此处逻辑也要修改 使用适应度值进行判断不适用，需要使用新半径和leader的关系进行剔除个体的操作
//                // 最终半径
//                int finalRadius = Integer.parseInt(key.split("_")[2]);
//                String aids = key.split("_")[1];
//
//                // 根据距离判断是否需要剔除
//                for (String s : val) {
//                    String bids = s.split("_")[1];
//
//                    // 距离计算,ids的相似个数
//                    // string 转 list ,计算两个list的最大相似数
//                    // 将基因型转为list,使用list来判断相似个数
//                    List<String> listA = stringToList(aids);
//                    List<String> listB = stringToList(bids);
//
//                    // 假设上面ListA 和 ListB都存在数据
//                    // aids：3,4,9,28,34,36,43,52,59,102,116,126,129,138,145,213,230,233,247,267
//                    // bids：8,10,17,18,24,25,26,39,71,72,81,84,92,102,107,141,143,150,273,303
//
//                    // 使用题目个数进行判断相似性
//                    int counter = 0;
//                    for (String c : listB) {
//                        for (String d : listA) {
//                            if (c.equals(d)) {
//                                counter = counter + 1;
//                            }
//                        }
//                    }
//
//                    // 如果距离大于半径,则需要将其剔除
//                    if (finalRadius < counter) {
//                        // 侧输出流
//                        sideList.add(s);
//                    } else {
//                        arrayList.add(s);
//                    }
//                }
//                System.out.println(arrayList.size());
//                System.out.println(sideList.size());
//                System.out.println("==========");
//                // 更新key和value  这个key 包含了半径真的没关系吗?
//                String leader = key.split("_")[0];
//                // java.util.ConcurrentModificationException
//                //mapArrayListForGene.put(leader+"_"+aids, arrayList);
//            }
//
//            // 遍历 sideList,找出leader,然后存入mapArrayList中
//            double maxValue = 0;
//            String maxIndividual = null;
//            for (String s : sideList) {
//                Double aDouble = Double.valueOf(s.split("_")[0]);
//                if (aDouble > maxValue){
//                    maxValue = aDouble;
//                    maxIndividual = s;
//                }
//            }
//            // java.util.ConcurrentModificationException
//            // mapArrayListForGene.put(maxIndividual, sideList);
//
//        }

            // 验证当前总个数
            Iterator iter4 = mapArrayListForGene.entrySet().iterator();
            while (iter4.hasNext()) {
                Map.Entry entry = (Map.Entry) iter4.next();
                String key = (String) entry.getKey();
                ArrayList<String> val = (ArrayList<String>) entry.getValue();
                sum = sum + val.size();
            }

            sum = sum + sideList.size();
            //System.out.println("个体总数：" + sum);
            System.out.println("merge 后个数："+sum);

            // 更新计数器
            if (mapArrayListForGene.size() == nn) {
                ct = ct + 1;
            } else {
                nn = mapArrayListForGene.size();
                ct = 0;
            }
        }
    }


    /**
     * 半径合并(为什么需要保留新半径?不可以直接map.put(key,value1+value2))
     * 参数: s1是高峰,s2是低峰,l1是高峰的适应度值
     * 原有峰不会变化,新增了一个合并后的键值对
     * <p>
     * 峰的移除和保留：
     * 移除目的：为了下一步的小生境剔除操作提供了便利
     * 保留目的：解决合并的过程中存在一个集合多次合并,若提前移除,导致空值现象、
     * (judge过程已经做了去重,所以应该可以不再考虑此逻辑)
     * <p>
     * 将新半径保存到map中,改变其数据结构 value_key_radius
     * map是无法直接修改key值的，所以要采用其他的方案，新增一个键值对，再删除之前那个要修改的
     * 采用迭代器的方式遍历，在迭代中it.remove(),map.put()操作
     */
    private void radiusMerge(String s1, String s2) {

        // 原有的逻辑：最远的个体即最大半径  不再适应

        // 半径的相似个体数 如何确定呢？
        // 1.最大相似个数  no  半径会变小,也不符合要求
        // 2.最小相似个数  no  如果取最小相似数，估计能达到15，明显不符合要求;
        // 暂时定的方案: 直接两个峰值进行合并,其相似个数即为新半径  这样半径会变大些  因为筛选的时候也是使用相似个数来判断,故两处逻辑是一致的


        // 进行合并
        String aids = s1.split("_")[1];
        String bids = s2.split("_")[1];

        // 将基因型转为list,使用list来判断相似个数
        List<String> ListA = stringToList(aids);
        List<String> ListB = stringToList(bids);

        // 假设上面ListA 和 ListB都存在数据
        // aids：3,4,9,28,34,36,43,52,59,102,116,126,129,138,145,213,230,233,247,267
        // bids：8,10,17,18,24,25,26,39,71,72,81,84,92,102,107,141,143,150,273,303

        // 使用题目个数进行判断相似性
        int counter = 0;
        for (String c : ListB) {
            for (String d : ListA) {
                if (c.equals(d)) {
                    counter = counter + 1;
                }
            }
        }

        // 新增一个合并后的键值对
        String s = s1 + "_" + counter;
        mapArrayListForGene.put(s, mapArrayListForGene.get(s1));

        // 现象：个体数小于200，待核实
        //System.out.println(mapArrayList);


    }


    /**
     * 调整初始半径
     * 1)若候选小生境数连续p次等同于实际小生境数，则 R = R * r ;
     * 2)若候选小生境数小于2，则 R = R  / r.
     */
    private static final int CONTINUE_TIMES = 5;
    private static final int NUMBER_OF_NICHES = 1;
    private double radius = 0.1;

    private void adjustRadius() {

        // 缩小初始半径
        if (CONTINUE_TIMES == ct) {
            radius = radius * Math.random();
            ct = 0;
        }

        // 扩大初始半径
        if (NUMBER_OF_NICHES == mapArrayListForGene.size()) {
            radius = radius / Math.random();
        }

    }


    /**
     * 执行修补操作(交叉变异均可能导致数量，题型，属性发生了变化)
     * 步骤：
     * (1)校验长度, 按照题型新增n道题目，or 随机新增题目数
     * (2)校验题型, in/out  完美解、替补解（权重）
     * (3)校验属性, in/out  完美解、替补解（权重 inListRe/inCompose）
     * <p>
     * 46:SHORT:(1,0,0,0,0):0.095001:0.0:0.0:0.0:0.0
     * 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3] 应用[0.1,0.3]
     * 属性比例 第1属性[0.2,0.4]   第2属性[0.2,0.4]   第3属性[0.1,0.3]  第4属性[0.1,0.3]  第5属性[0.1,0.3]
     */
    private String[] correct(String[] temp) throws SQLException {


        // 长度校验
        String[] strings = cu.correctLength(temp);


        // 题型比例校验
        String[] stringsV2 =  cu.correctType(strings);


        // 属性比例校验
        String[] stringsV3 = cu.correctAttribute(stringsV2);


        return stringsV3;


    }


    /**
     * 如果连续10代,相似个数均大于90,则认为其是相似的
     *  1.将数据写入
     *  2.中断程序
     *
     * FIXME ： 此处逻辑有误 待修复  lastCount  被重置了
     *
     */
    int maxCount = 10;
    int judgmentBasis = 90;
    public ArrayList<Object> registerTimeTimer(int size,int lastCount) {

        Boolean timeFlag = false;

        if (size >= judgmentBasis) {
            lastCount = lastCount + 1;
            System.out.println(lastCount);
            if (lastCount > maxCount){
                timeFlag =  true;
            }
        } else {
            lastCount = 0;
        }
        System.out.println(" 不断的尝试进行 终止判断");

        ArrayList<Object> result = new ArrayList<>();

        if (timeFlag){
            lastCount = 0;
        }
        result.add(timeFlag);
        result.add(lastCount);

        if (timeFlag){
            lastCount = 0;
        }

        return result;

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
     * 主程序
     * 1. 初始化试卷
     * 2. 适应度排序
     * 3. 分配个体到不同小生境
     * 4. 根据各个小生境的种群大小,然后塞入到不同集合中
     *   (从后往前可以保留多样性,从前往后可以维持稳定性)
     * 5. 选择、交叉、限制性锦标赛拥挤小生境
     *
     */
    @Test
    public  void  main() throws SQLException {


        // 1.初始化试卷(长度，题型，属性比例) 轮盘赌构造法生成二维数组 String[][] paperGenetic = new String[100][20]
        initItemBank();

        // 遍历50次,形成多次结果,便于比较
        for (int j = 0; j < 20; j++) {
            timeFlag =  false;
            lastCount = 0;
            // 这个是否有必要
            iterationClear();

            // 2.迭代次数 500
            for (int i = 0; i < iterationSize; i++) {


                // 3.置空leader容器
                iterationClear();

                // 4.适应度值排序   sortListForGene = new ArrayList<>(100)
                //  4.1 此处顺便做了收敛flag的判断
                sortFitnessForGene(i);

                // 5.将群体中的个体分配到不同小生境中 leader + members    mapArrayListForGene(key,value)
                distributeNicheForGene();

                // 过完年仔细核实
                ifSkip(true);

                // 6.根据各个小生境的种群大小，然后塞入到不同集合中
                HashMap<String, ArrayList<String>> inListHashMap = new HashMap<>();

                ArrayList<String> outList = new ArrayList<>();

                Iterator<Map.Entry<String, ArrayList<String>>> iterator = mapArrayListForGene.entrySet().iterator();

                while (iterator.hasNext()) {

                    Map.Entry<String, ArrayList<String>> entry = iterator.next();

                    // 是否小生境个数 >= 5  这个固定值,需要设置为全局变量，便于修改
                    if (entry.getValue().size() >= 5) {
                        inListHashMap.put(entry.getKey(), entry.getValue());
                    } else {
                        outList.addAll(entry.getValue());
                    }
                }


                // 7.选择
                //   对两种不同的集合进行不同的选择 (小生境内 | 小生境外)
                //   进去啥,返回啥。保持样式不做修改,这样有利于样式的统一

                //   7.1 进行小生境内的选择   轮盘赌 小生境内有多少个体就执行多少次选举，选出适应个体
                HashMap<String, ArrayList<String>> inSelect = selectionIn(inListHashMap);

                //   7.2 进行小生境外的选择
                ArrayList<String> outSelect = selectionOut(outList);


                // 8.交叉
                //   8.1 进行小生境内交叉
                HashMap<String, ArrayList<String>> inCross = crossCoverIn(inSelect);

                //   8.2 进行小生境外交叉  方法内部进行了size判断
                ArrayList<String> outCross = crossCoverOut(outSelect);


                // 9.变异
                //   9.1 进行小生境内交叉(采用的是 限制性锦标赛拥挤小生境)
                HashMap<String, ArrayList<String[]>> inMutate = mutateIn(inCross);

                //   9.2 进行小生境外交叉
                ArrayList<String[]> outMutate = mutateOut(outCross);


                // 10. 最大圈问题
                // timeFlag =  true;
                if (timeFlag) {
                    // 最大圈效果
                    gtMeanPart(inMutate, outMutate);
                    // 此处重置flag,防止下次迭代时影响稳定性的判断
                    timeFlag =  false;
                    // 使用 break 替代 exit
                    System.out.println("退出！！");
                    // System.exit(0);
                    break;

                }

                // 11.将inMutate和outMutate合并后赋值给 paperGenetic
                paperGenetic = mergeToGene(inMutate, outMutate);

            }


        }
    }



}