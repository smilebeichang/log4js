package cn.edu.sysu.niche.a8n10;


import cn.edu.sysu.niche.others.MyComparator;
import cn.edu.sysu.utils.JDBCUtils4;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.sql.SQLException;
import java.util.*;


/**
 * @Author : song bei chang
 * @create : 2022/03/02 00:05
 *
 */
public class DNDR10_P_CDI {


    private Logger log = Logger.getLogger(DNDR10_P_CDI.class);

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

    /**
     * 之前构建最大圈 是直接取top 50,现在是排序,然后构造直接生成平行试卷,不用判断题目与题目之间的相似性
     * 故这里用不到该类,但初期可以先用,也需后续优化
     *
     */
    private ArrayList<String> sortTo50 = new ArrayList<>(50);

    Random rand = new Random();

    private JDBCUtils4 jdbcUtils = new JDBCUtils4();

    /**
     *  size 为310  初始化,塞入到内存中
     */
    ArrayList<String> allItemList = jdbcUtils.selectAllItemsV2();

    int sourceSize =  allItemList.size();


    int psize = 1000;


    /**
     * 比较器
     */
    Comparator comp = new MyComparator();


    public DNDR10_P_CDI() throws SQLException {
    }





    /**
     * 生成 paperGenetic  = new String[500*100][20]  为交叉变异提供原始材料
     *
     */
    private void initItemBank()  {

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

            String[] itemArray = ids.split(",");

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
    private void getFitnessForRandom() {

        // 拼接字符串 每套试卷的适应度值_本身
        String[] fitTmp = new String[paperSize];

        // 计算试卷的适应度值，即衡量试卷优劣的指标之一 Fs
        for (int i = 0; i < paperSize; i++) {

            double adi1r = 0;
            double adi2r = 0;
            double adi3r = 0;
            double adi4r = 0;
            double adi5r = 0;
            double adi6r = 0;
            double adi7r = 0;

            StringBuilder idsb = new StringBuilder();

            // 获取原始adi  数组里面裹数组
            String[] itemList = paperGenetic[i];

            // 强行手动修复 如果不行就判断里面的值是否为null
            // 加一层判断，如果itemList.length=20,则保持不变;如果否,则给paperGenetic[i] 赋予新值
            if(itemList.length != 20){
                itemList = supplementPaperGenetic();
            }

            for (int j = 0; j < itemList.length; j++) {

                int id = Integer.valueOf(itemList[j].trim());
                String[] splits = allItemList.get(id).split(":");
                adi1r = adi1r + Double.parseDouble(splits[3]);
                adi2r = adi2r + Double.parseDouble(splits[4]);
                adi3r = adi3r + Double.parseDouble(splits[5]);
                adi4r = adi4r + Double.parseDouble(splits[6]);
                adi5r = adi5r + Double.parseDouble(splits[7]);
                adi6r = adi6r + Double.parseDouble(splits[8]);
                adi7r = adi7r + Double.parseDouble(splits[9]);

                // 拼接ids
                idsb.append(",").append(splits[0]);

            }

            String ids = idsb.toString().substring(1);


            //均值 和 最小值
            double avgrum = (adi1r + adi2r + adi3r + adi4r + adi5r + adi6r + adi7r) / 7;

            // 本身的基因型选用id拼接,其具有代表性
            fitTmp[i] = avgrum + "_" + ids;
            sortListForRandom.add(avgrum + "_" + ids);

        }


        // 扰动并构造平行试卷
        ArrayList<String> newListForPCDI = perturbToAssembled(sortListForRandom);


        Collections.sort(newListForPCDI, comp);


        for (int i = 0; i < 50; i++) {
            sortTo50.add(newListForPCDI.get(i));
        }

    }



    public String[] supplementPaperGenetic()   {

        // 单套试卷的集合
        HashSet<String> itemSet = new HashSet<>();

        for (int i = 0; i < 20; i++) {
            // 减少频繁的gc
            String item;
            // 去重操作
            while (itemSet.size() == i) {
                // 获取题目id
                item = new Random().nextInt(psize)+"";
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
     *
     * FIXME 此处的去重,可能需要进一步考虑,因为存在题目一样,但适应度值不一样的情况
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
     * 扰动CDI,并返回新的集合
     *
     *
     */
    private ArrayList<String> perturbToAssembled(ArrayList<String> sortListForRandom) {

        ArrayList<String> newListForPCDI =  new ArrayList<>();

        for (String s : sortListForRandom) {
            String[] s1 = s.split("_");
            double pv = rand.nextGaussian() * Double.valueOf(s1[0]);
            newListForPCDI.add(pv+"_"+s1[1]);
        }

        return newListForPCDI;
    }



    /**
     * 主程序
     * 1. 初始化试卷  类似于 random
     * 2. 扰动, 取 top50
     * 3. 计算 max clique
     * 4. 画图
     *
     */
    @Test
    public  void  main() throws  SQLException {

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


            ArrayList<String> mqList = new DNDR10().similarClique(uniqueList, 1);

            // 计算均值 和 波动 情况
            new DNDR10().calAvgFitness(uniqueList,mqList);

        }

    }



}