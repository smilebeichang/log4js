package cn.edu.sysu.niche.a5n3;


import cn.edu.sysu.adi.TYPE;
import cn.edu.sysu.niche.others.MyComparator;
import cn.edu.sysu.utils.JDBCUtils4;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.sql.SQLException;
import java.util.*;


/**
 * @Author : song bei chang
 * @create : 2022/01/17 00:11
 *
 * 模拟随机
 *
 *本周计划：
 * 1：random的50000取前50           (不一定是500*100)
 *      随机生成50道题,然后判断其最大圈,这样便可以验证是否有效  待后续完成
 * 2：如果达到一定次数后，则终止          500代
 * 3：收敛(阈值)
 * 4：题库数量  + 进行多轮迭代比较
 * 5：收敛(为什么达不到同一个值)
 * 6：P-CDI
 *
 */
public class DNDR10_Random {


    private Logger log = Logger.getLogger(DNDR10_Random.class);

    /**
     * 这个变量需手动更改
     */
    int iterSize = 25;

    /**
     * 500 * 100 套试卷 20道题
     */
    private  String[][] paperGenetic = new String[iterSize * 100][20];

    int paperSize = paperGenetic.length;

    private ArrayList<String> sortListForRandom = new ArrayList<>(iterSize * 100);
    private ArrayList<String> sortTo50 = new ArrayList<>(50);

    Random rand = new Random();

    private JDBCUtils4 jdbcUtils = new JDBCUtils4();

    /**
     *  size 为310  初始化,塞入到内存中
     */
    ArrayList<String> allItemList = jdbcUtils.selectAllItems();

    int sourceSize =  allItemList.size();

    public DNDR10_Random() throws SQLException {
    }


    /**
     * 比较器
     */
    Comparator comp = new MyComparator();


    /**
     * 生成 paperGenetic  = new String[500*100][20]  为交叉变异提供原始材料
     *
     */
    private void initItemBank() throws InterruptedException {

        System.out.println("====== 开始选题,构成试卷  轮盘赌构造 ======");

        // 题目|基因大小  交叉变异的基本单位
        int questionsNum = paperGenetic[1].length;

        // 单套试卷的集合
        HashSet<String> itemSet = new HashSet<>();


        // 生成了二维数组 paperGenetic
        for (int j = 0; j < paperSize; j++) {

            // 清空上一次迭代的数据
            itemSet.clear();

            for (int i = 0; i < questionsNum; i++) {

                // 去重操作
                while (itemSet.size() == i) {
                    // 获取题目id   轮盘赌构造法
                    int sqlId = rand.nextInt(sourceSize - 1 );
                    itemSet.add(sqlId+"");
                }

            }

            // 将hashSet转ArrayList
            ArrayList<String> idList = new ArrayList<>(itemSet);

            // list排序
            Collections.sort(idList);


            // 根据id从数据库中查询相对应的题目
            String ids = idList.toString().substring(1, idList.toString().length() - 1);

            List<String> sList = Arrays.asList(ids.split(","));
            String[] itemArray = new String[sList.size()];

            for (int k = 0; k < sList.size(); k++) {
                itemArray[k] = sList.get(k);
            }


            // 赋值给全局变量 (容器：二维数组)
            paperGenetic[j] = itemArray;
        }
    }




    /**
     * 返回：每套试卷的适应度_ids
     * <p>
     * 方案  进行乘以一个exp 来进行适应度值的降低，高等数学里以自然常数e为底的指数函数
     * 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3]  应用[0.1,0.3]
     * 属性比例 第1属性[0.2,0.4]  第2属性[0.2,0.4]  第3属性[0.1,0.3] 第4属性[0.1,0.3] 第5属性[0.1,0.3]
     */
    private void getFitnessForRandom() throws SQLException {

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
            if(itemList.length != 20){
                itemList = supplementPaperGenetic();
            }

            //System.out.println("-->itemList: " + Arrays.asList(itemList).toString());
            for (int j = 0; j < itemList.length; j++) {

                int id = Integer.valueOf(itemList[j].trim());
                String[] splits = allItemList.get(id).split(":");
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

                s = allItemList.get(Integer.valueOf(s.trim()));

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
                String[] splits  = allItemList.get(Integer.valueOf(expList[j].trim())).split(":");

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
     * 计算均值 和 波动 情况
     * @param inBack
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
     * 2. 计算 max clique
     * 3. 画图
     * FIXME: 这个500代需要修复,很多时候，NicheGA 并没有跑到500代
     *
     * random初步结果：
     * 最大圈顶点数：3~4
     * 最大圈：6~10
     * 平均适应度值：32.6~34.9
     * 波动情况：7.8~15.5
     *
     * Java.lang.ArrayIndexOutOfBoundsException: 18
     * 会不会是因为太频繁使用内存了
     *
     */
    @Test
    public  void  main() throws InterruptedException, SQLException {

        // 轮询跑50代,方便查看结果
        for (int j = 0; j < 20; j++) {

            sortListForRandom.clear();
            sortTo50.clear();
            // 1.初始化试卷
            initItemBank();

            System.out.println("---------------");


            // 2.数组转list  FIXME 此处的paperList需进行去重操作,待后续优化
            ArrayList<String> paperList = new ArrayList<>();
            for (int i = 0; i < paperGenetic.length; i++) {

                List<String> listB= Arrays.asList(paperGenetic[i]);
                paperList.add("0_"+listB.toString().substring(1, listB.toString().length()-1));

            }

            // 3.计算的适应度值，并取前50个体
            getFitnessForRandom();

            // 去重
            ArrayList<String> uniqueList  = uniqueDate(sortTo50);

            /**
             * 图G的最大圈顶点数为：2
             * 图G的最大圈个为：5
             */
            new DNDR10().similarClique(uniqueList,1);

            // 计算均值 和 波动 情况
            calAvgFitness(uniqueList);

        }

    }



}