package cn.edu.sysu.controller;

import cn.edu.sysu.pojo.Papers;
import cn.edu.sysu.pojo.QuorumPeer;
import cn.edu.sysu.utils.JDBCUtils2;
import org.junit.Test;

import java.sql.SQLException;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 *
 * @Author : song bei chang
 * @create 2021/05/18 0:17
 */
public class ADIController {


    /*  容器 全局最优 局部最优  */

    private static double GlobalOptimal = 0;
    private static double[] LocalOptimal = new double[10];


    /* 10套试卷 6道题  */

    private static String[][] paperGenetic =new String[10][6];



    @Test
    public  void ori() throws SQLException {

        //选择10套的原因，只有基数够大，才能为交叉变异提供相对较多的原始材料  打算先以10套试卷为变更基础,最后取前三
        //抽取试卷  10套、每套试卷6题
        Papers papers = new Papers();
        papers.setPc(0.5);
        papers.setPm(0.5);

        //初始化试卷   从题库中选取题目构成试卷  长度，属性类型，属性比例   （题型）
        initItemBank();

        //计算适应度值  ①计算时机 轮盘赌
        //            ②计算单位（单套试卷）
        //            取min，按adi属性取整套试卷的最小值


        // i 迭代次数
        for (int i = 0; i < 1550; i++) {
            selection();
            crossCover(papers);
            mutate(papers);
            // TODO  待实现 小生境环境的搭建
            elitistStrategy();
        }
    }



    /**
     *
     * 生成题库(以试卷为单位：id  长度，属性类型，属性比例)
     * 原始比例： 5+10+10+5+1 = 31
     * 属性类型 和 属性比例 最好不要取固定值（除初始化外），将会导致所能修补算子执行时选取的题目有限
     * 属性类型：1个属性的1题[1,50]  2个属性的2题[51,150]  3个属性的2题[151,250] 4个属性的1题[251,300]
     *
     * 属性比例
     * 因为有5个属性，6道题，所以共15次选择
     *             1+2*2+3*2+4 = 共选取了15个属性（可重复的前提下）
     * ①每个属性有各自的比例
     * ②比例的范围和权重如何确定   假设第1、2个属性重要，
     * 第1属性[3,5]        第2属性[3,5]        第3属性[2,4]       第4属性[2,4]       第5属性[2,4]
     * 第1属性[0.2,0.34]   第2属性[0.2,0.34]   第3属性[0.1,0.27]  第4属性[0.1,0.27]  第5属性[0.1,0.27]
     *
     *
     * 方案 根据现有结果得出一个评判标准,进行重新抽取题目
     * 方案 进行乘以一个底数e 来进行适应度值的降低
     *
     *
     *
     * TODO  疑问： 1.选出可能导致占比失衡的试题   OutList  set（取交集）  设置权重
     * TODO        2.从题库中选取可能满足的解集   InList  (按顺序遍历，vs 随机  vs 优先级)
     * TODO        3.占比失衡的情况讨论
     * TODO        4.停止规则
     *
     *
     */
    @Test
    public  void initItemBank() throws SQLException {

        System.out.println("====== 开始选题,构成试卷  ======");

        /*  试卷数 */
        int paperNum = 10 ;
        /* 单张试卷每种属性的题目数量 6 */
        int oneAttNum = 1;
        int twoAttNum = 2;
        int threeAttNum = 2;
        int fourAttNum = 1;

        JDBCUtils2 jdbcUtils = new JDBCUtils2();

        // 题库310道题  50:100:100:50:10   长度，属性类型，属性比例
        String sql1 = "SELECT CEILING( RAND () * 49 ) + 1  AS id" ;
        String sql2 = "SELECT CEILING( RAND () * 99 ) + 51 AS id" ;
        String sql3 = "SELECT CEILING( RAND () * 99 ) + 151 AS id" ;
        String sql4 = "SELECT CEILING( RAND () * 49 ) + 251 AS id" ;


        /*  生成的平行试卷个数  */
        for (int j = 0; j < paperNum; j++) {
            ArrayList<Integer> idList = new ArrayList<>();
            int id ;

            //随机抽取1个属性的1题
            for (int i = 0; i < oneAttNum; i++) {
                id = jdbcUtils.selectItem(sql1);
                idList.add(id);
            }

            //随机抽取2个属性的2题
            Set<Integer> id_set2 = new HashSet<>();
            for (int i = 0; i < twoAttNum; i++) {
                //去重操作
                while (id_set2.size() == i) {
                    id = jdbcUtils.selectItem(sql2);
                    id_set2.add(id);
                }
            }
            //添加所有元素到列表中
            idList.addAll(id_set2);

            //随机抽取3个属性的2题
            Set<Integer> id_set3 = new HashSet<>();
            for (int i = 0; i < threeAttNum; i++) {
                //去重操作
                while (id_set3.size() == i) {
                    id = jdbcUtils.selectItem(sql3);
                    id_set3.add(id);
                }
            }
            //添加所有元素到列表中
            idList.addAll(id_set3);

            //随机抽取4个属性的1题
            for (int i = 0; i < fourAttNum; i++) {
                id = jdbcUtils.selectItem(sql4);
                idList.add(id);
            }


            //list  排序
            Collections.sort(idList);
            //输出所有抽取到的试题id
            System.out.println("试卷"+j+"的试题id: "+idList);


            String ids = idList.toString().substring(1,idList.toString().length()-1);

            ArrayList<String> bachItemList = jdbcUtils.selectBachItem(ids);


//**************************    BEGIN   ************************************

            //TODO  根据现有解得出一个评判标准,进行重新抽取题目   是否可以形成一个评判标准分，然后选取最优解
            //TODO  待测试及优化  校验属性比例
            //bachItemList = correctAttribute(bachItemList);


//**************************    END    *************************************



            // 把题库提升为全局变量，方便整体调用 容器：二维数组
            // 交叉变异的对象是 试题的题目   即试卷=个体  题目=基因
            // private static String[][] paperGenetic =new String[10][6];
            String[] itemArray = new String[bachItemList.size()];
            for (int i = 0; i < bachItemList.size(); i++) {
                itemArray[i] = bachItemList.get(i);
            }
            // 赋值
            paperGenetic[j] = itemArray;
        }

    }


    /**
     * 方案一 根据现有解得出一个评判标准,进行重新抽取题目
     *
     *  <属性比例校验>
     *     1.以试卷为单位,分别获取每个题目全部信息
     *     2.截取字段，并统计每个属性所占的比例
     *                  id,pattern,a1,a2,a3,a4,a5
     *     3.统计比例信息
     *     4.校验，并重新选取
     *     5.输出最后的方案
     **/
    public ArrayList correctAttribute(ArrayList<String> bachItemList) throws SQLException {

// ========================= 封装信息 ==========================
        //1.每道试题的全部信息
        String it0 = bachItemList.get(0);
        String it1 = bachItemList.get(1);
        String it2 = bachItemList.get(2);
        String it3 = bachItemList.get(3);
        String it4 = bachItemList.get(4);
        String it5 = bachItemList.get(5);

        //2.截取字段，是否需要封装成对象 or 直接统计判断
        //封装：便于后期直接进行筛选
        String[] sp1 = it0.split(":");
        String[] at1 = sp1[1].split(",");
        QuorumPeer quorumPeer1 = new QuorumPeer(sp1[0], at1[0], at1[1], at1[2], at1[3], at1[4]);

        String[] sp2 = it1.split(":");
        String[] at2 = sp2[1].split(",");
        QuorumPeer quorumPeer2 = new QuorumPeer(sp2[0], at2[0], at2[1], at2[2], at2[3], at2[4]);

        String[] sp3 = it2.split(":");
        String[] at3 = sp3[1].split(",");
        QuorumPeer quorumPeer3 = new QuorumPeer(sp3[0], at3[0], at3[1], at3[2], at3[3], at3[4]);

        String[] sp4 = it3.split(":");
        String[] at4 = sp4[1].split(",");
        QuorumPeer quorumPeer4 = new QuorumPeer(sp4[0], at4[0], at4[1], at4[2], at4[3], at4[4]);

        String[] sp5 = it4.split(":");
        String[] at5 = sp5[1].split(",");
        QuorumPeer quorumPeer5 = new QuorumPeer(sp5[0], at5[0], at5[1], at5[2], at5[3], at5[4]);

        String[] sp6 = it5.split(":");
        String[] at6 = sp6[1].split(",");
        QuorumPeer quorumPeer6 = new QuorumPeer(sp6[0], at6[0], at6[1], at6[2], at6[3], at6[4]);


// ========================= 统计指标信息  ==========================
        //统计校验 属性比例
        // 定义局部变量
        double as1  = 0 ;
        double as2  = 0 ;
        double as3  = 0 ;
        double as4  = 0 ;
        double as5  = 0 ;

        for (int i = 0; i < bachItemList.size(); i++) {

            as1 = as1 +  Double.parseDouble(bachItemList.get(i).split(":")[1].split(",")[0].substring(1,2));
            as2 = as2 +  Double.parseDouble(bachItemList.get(i).split(":")[1].split(",")[1]);
            as3 = as3 +  Double.parseDouble(bachItemList.get(i).split(":")[1].split(",")[2]);
            as4 = as4 +  Double.parseDouble(bachItemList.get(i).split(":")[1].split(",")[3]);
            as5 = as5 +  Double.parseDouble(bachItemList.get(i).split(":")[1].split(",")[4].substring(0,1));

        }

        System.out.println("目前属性数量情况： as1:"+as1+" as2:"+as2+" as3:"+as3+" as4:"+as4+" as5:"+as5);

        //需要判断 属性比例是多了还是少了
        //定义局部变量 ab1  (-1->少于,0->正常,1->大于)
        System.out.println("=========================================");
        int ab1 ;
        double edx1 = as1/15;
        if(edx1>=0.2 && edx1<=0.34){
            ab1 = 0;
        }else if(edx1<0.2){
            ab1 = -1;
        }else {
            ab1 = 1;
        }

        int ab2 ;
        double edx2 = as2/15;
        if(edx2>=0.2 && edx2<=0.34){
            ab2 = 0;
        }else if(edx2<0.2){
            ab2 = -1;
        }else {
            ab2 = 1;
        }

        int ab3 ;
        double edx3 = as3/15;
        if(edx3>=0.1 && edx3<=0.27){
            ab3 = 0;
        }else if(edx2<0.1){
            ab3 = -1;
        }else {
            ab3 = 1;
        }

        int ab4 ;
        double edx4 = as4/15;
        if(edx4>=0.1 && edx4<=0.27){
            ab4 = 0;
        }else if(edx4<0.1){
            ab4 = -1;
        }else {
            ab4 = 1;
        }

        int ab5 ;
        double edx5 = as5/15;
        if(edx5>=0.1 && edx5<=0.27){
            ab5 = 0;
        }else if(edx5<0.1){
            ab5 = -1;
        }else {
            ab5 = 1;
        }

        //是否要进一步考虑 具体多了多少数量的情况
        System.out.println("目前属性占比情况： ab1:"+ab1+"   ab2:"+ab2+"   ab3:"+ab3+"   ab4:"+ab4+"   ab5:"+ab5);


//==================  指标统计完毕   =========================

        //根据属性占比ab1的值得出一个评判标准,进行删除题目 和 重新添加题目
        //拼接成一个flag
        //占比失衡的情况：(设置的属性比例有关)
        //多一个    先遍历匹配可选解，从中选取一个,然后(move,add)   导致的问题均是：属性题型发生变化
        //少一个    可以随机选取一个，不带有这个属性的题目，然后(move,add)
        //多两个    先遍历匹配(完全匹配)，若没有，则进行随机选取两次
        //少两个    随机选取一个,不带这两属性的题目
        //多一个少一个  (原始解，退出解,可选解)
        String flag = "("+ab1+","+ab2+","+ab3+","+ab4+","+ab5+")";

        //标记符：多了的值 mark = 1 ，对于少了的属性 mark = -1
        //优势在于 可以简化从题库中选取可选解的难度（最优解排在前面，弱解排在后面）
        System.out.println("flag: "+flag);

        //此处逻辑适合比例多了的属性值
        //目前属性数量情况： as1:3.0 as2:2.0 as3:5.0 as4:2.0 as5:3.0
        //目前属性占比情况： ab1:0   ab2:0   ab3:1   ab4:0   ab5:0

        //选取的标准是什么？遍历选取，然后重新计算是否符合要求，这样将会导致计算很冗余
        //新解替换掉旧解 （利用到此试题中的相关属性 即as1 as2 as3 as4 as5 便于做更进一步的判断）

        // 旧解的容器  (可能造成属性比例失衡的解集)
        Set<String> resultAll = new HashSet<>();
        resultAll.clear();
        Set<String> resultMore = new HashSet<>();
        Set<String> resultLess = new HashSet<>();


        //取出属性比例过多的集合的交集（会不会存在以下情况: 两个单集合，各自多一个属性）
        //可能需要换数据结构，用arrayList接收，最后用groupBy统计个数， 判断标准：个数越大，则优先级越高
        if(ab1==1 || ab2==1 || ab3==1 || ab4==1 || ab5==1){

            //需要判断 set 是否为空  把判断为空的逻辑 放在上面判断
            resultMore.clear();
            HashSet<String> set1 = new HashSet<>();
            HashSet<String> set2 = new HashSet<>();
            HashSet<String> set3 = new HashSet<>();
            HashSet<String> set4 = new HashSet<>();
            HashSet<String> set5 = new HashSet<>();

            //表明属性1比例过多，用set1集合接收   bachItemList为方法参数，即一套试卷
            if(ab1==1){
                for (int i = 0; i < bachItemList.size(); i++) {
                    if(bachItemList.get(i).split(":")[1].split(",")[0].substring(1,2).equals("1")){
                        set1.add(bachItemList.get(i));
                    }
                }
                if (resultMore.size()==0){
                    resultMore.addAll(set1);
                }else {
                    resultMore.retainAll(set1);
                }
            }

            if(ab2==1){
                for (int i = 0; i < bachItemList.size(); i++) {
                    if(bachItemList.get(i).split(":")[1].split(",")[1].equals("1")){
                        set2.add(bachItemList.get(i));
                    }
                }
                if (resultMore.size()==0){
                    resultMore.addAll(set2);
                }else {
                    resultMore.retainAll(set2);
                }
            }

            if(ab3==1){
                for (int i = 0; i < bachItemList.size(); i++) {
                    if(bachItemList.get(i).split(":")[1].split(",")[2].equals("1")){
                        set3.add(bachItemList.get(i));
                    }
                }
                if (resultMore.size()==0){
                    resultMore.addAll(set3);
                }else {
                    resultMore.retainAll(set3);
                }
            }

            if(ab4==1){
                for (int i = 0; i < bachItemList.size(); i++) {
                    if(bachItemList.get(i).split(":")[1].split(",")[3].equals("1")){
                        set4.add(bachItemList.get(i));
                    }
                }
                if (resultMore.size()==0){
                    resultMore.addAll(set4);
                }else {
                    resultMore.retainAll(set4);
                }
            }

            if(ab5==1){
                for (int i = 0; i < bachItemList.size(); i++) {
                    if(bachItemList.get(i).split(":")[1].split(",")[4].substring(0,1).equals("1")){
                        set5.add(bachItemList.get(i));
                    }
                }
                if (resultMore.size()==0){
                    resultMore.addAll(set5);
                }else {
                    resultMore.retainAll(set5);
                }
            }

            //集合取交集 获取最接近的解
            System.out.println("属性比例过多的交集：" + resultMore);

            //赋值到全局变量
            if (resultAll.size()==0){
                resultAll.addAll(resultMore);
            }else {
                resultAll.retainAll(resultMore);
            }

        }






        //取出属性比例不足的集合的交集
        if(ab1==-1 || ab2==-1 || ab3==-1 || ab4==-1 || ab5==-1){

            //需要判断 set 是否为空  把判断为空的逻辑 放在上面判断
            resultLess.clear();
            HashSet<String> set1 = new HashSet<>();
            HashSet<String> set2 = new HashSet<>();
            HashSet<String> set3 = new HashSet<>();
            HashSet<String> set4 = new HashSet<>();
            HashSet<String> set5 = new HashSet<>();

            //表明属性1比例过少，用set集合接收
            if(ab1==-1){

                for (int i = 0; i < bachItemList.size(); i++) {
                    if(bachItemList.get(i).split(":")[1].split(",")[0].substring(1,2).equals("0")){
                        set1.add(bachItemList.get(i));
                    }
                }
                if (resultLess.size()==0){
                    resultLess.addAll(set1);
                }else {
                    resultLess.retainAll(set1);
                }
            }

            if(ab2==-1){
                for (int i = 0; i < bachItemList.size(); i++) {
                    if(bachItemList.get(i).split(":")[1].split(",")[1].equals("0")){
                        set2.add(bachItemList.get(i));
                    }
                }
                if (resultLess.size()==0){
                    resultLess.addAll(set2);
                }else {
                    resultLess.retainAll(set2);
                }
            }

            if(ab3==-1){
                for (int i = 0; i < bachItemList.size(); i++) {
                    if(bachItemList.get(i).split(":")[1].split(",")[2].equals("0")){
                        set3.add(bachItemList.get(i));
                    }
                }
                if (resultLess.size()==0){
                    resultLess.addAll(set3);
                }else {
                    resultLess.retainAll(set3);
                }
            }

            if(ab4==-1){
                for (int i = 0; i < bachItemList.size(); i++) {
                    if(bachItemList.get(i).split(":")[1].split(",")[3].equals("0")){
                        set4.add(bachItemList.get(i));
                    }
                }
                if (resultLess.size()==0){
                    resultLess.addAll(set4);
                }else {
                    resultLess.retainAll(set4);
                }
            }

            if(ab5==-1){
                //使用 foreach 替换掉 for   (Arraylist时，fori 性能高于 foreach  Linkedlist 时，fori低于foreach)
                for (String aBachItemList : bachItemList) {
                    if (aBachItemList.split(":")[1].split(",")[4].substring(0, 1).equals("0")) {
                        set5.add(aBachItemList);
                    }
                }
                if (resultLess.size()==0){
                    resultLess.addAll(set5);
                }else {
                    resultLess.retainAll(set5);
                }
            }

            //集合取交集 获取最接近的解
            System.out.println("属性比例不足的交集：" + resultLess);

            //赋值到全局变量
            if (resultAll.size()>0){
                resultAll.retainAll(resultLess);
            }else {
                resultAll.addAll(resultLess);
            }
        }


        // resultAll  resultMore resultLess 的关系判断
        //1.三者都有值，则取resultAll  （retainAll 是否存在空集合的情况）
        //2.resultLess无  resultMore有值 则取 resultMore
        //3.resultMore无  resultLess有值 则取 resultLess
        JDBCUtils2 jdbcUtils = new JDBCUtils2();
        if(resultAll.size()>0 && resultMore.size()>0 && resultLess.size()>0){
            System.out.println("本套试卷 既有属性比例过多，又有属性比例不足的。");
            System.out.println(resultAll);

            //这种情况可能是遇到最多的，也好，可以简单的将旧解找出来
            //           将要out的解：随机选取 resultAll 其中的一个
            //删除旧解的要求：（去题库中搜索，取出解集后，循环遍历，直到符合要求的解生成）
            //           1.删除不影响其他属性比例
            //           2.新增不影响其他属性比例
            //拼接SQL  搜索条件 p3 = 1 and p5 = 0

            StringBuilder sb = new StringBuilder();
            if(ab1>0){
                sb.append(" p1=0 and ");
            }else if (ab1<0){
                sb.append(" p1=1 and ");
            }

            if(ab2>0){
                sb.append(" p2=0 and ");
            }else if (ab2<0){
                sb.append(" p2=1 and ");
            }

            if(ab3>0){
                sb.append(" p3=0 and ");
            }else if (ab3<0){
                sb.append(" p3=1 and ");
            }

            if(ab4>0){
                sb.append(" p4=0 and ");
            }else if (ab4<0){
                sb.append(" p4=1 and ");
            }

            if(ab5>0){
                sb.append(" p5=0 and ");
            }else if (ab4<0){
                sb.append(" p5=1 and ");
            }

            String sql = sb.toString().substring(0, sb.toString().length() - 4);
            //获取新解的集合
            ArrayList<String> selectBySqlList = jdbcUtils.selectBySql(sql);

            //ori解集  out解集  in解集 的关系
            //原始解集 - out解 + in解 = 新解(拿新解去再次核对)
            //循环的逻辑，应该是 外层out解，内层in解，不断的调用属性比例校验方法，如满足要求则退出，不满足则继续遍历。最后的终止条件是 遍历终止

            List<String> resultAllList = new ArrayList<>(resultAll);
            System.out.println("校验前的集合:"+bachItemList.toString());
            Boolean b = false;
            for (int i = 0; i < resultAllList.size(); i++) {
                for (int j = 0; j < selectBySqlList.size(); j++) {
                    //目前只适合单进单出
                    //为什么会存在 138的解？
                    b = propCheck(bachItemList,resultAllList.get(i),selectBySqlList.get(j));
                    if(b){
                        // 删除out解，添加in解
                        for (int k = 0; k < bachItemList.size(); k++) {
                            if (bachItemList.get(i).equals(resultAllList.get(i))){
                                bachItemList.set(i,selectBySqlList.get(j));
                            }
                        }
                        // 输出
                        System.out.println("已找到符合要求的解，现退出循环,目前的解集为："+bachItemList.toString());
                        break;
                    }
                }
                if (b){
                    break;
                }
            }
            System.out.println("校验后的集合:"+bachItemList.toString());


        }




        if(resultAll.size()>0 && resultMore.size()==0 && resultLess.size()>0){
            System.out.println("取resultLess，再次抽取");
            System.out.println(resultLess);
        }



        if(resultAll.size()>0 && resultMore.size()>0 && resultLess.size()==0){
            System.out.println("取resultMore，再次抽取");
            System.out.println(resultMore);

            // 替换目的:将有替换为无
            // 替换标准:需要拿上下文进行判断  有点复杂，需要回过头来继续考虑
            // 差额计算  b1 = 5 - as1, max 为最佳
            //第1属性[3,5]        第2属性[3,5]        第3属性[2,4]       第4属性[2,4]       第5属性[2,4]
            //第1属性[0.2,0.34]   第2属性[0.2,0.34]   第3属性[0.1,0.27]  第4属性[0.1,0.27]  第5属性[0.1,0.27]
            //目前属性数量情况： as1:3.0 as2:2.0 as3:5.0 as4:2.0 as5:3.0
            //目前属性占比情况： ab1:0   ab2:0   ab3:1   ab4:0   ab5:0
            System.out.println("目前属性数量情况： as1:"+as1+" as2:"+as2+" as3:"+as3+" as4:"+as4+" as5:"+as5);
            System.out.println("目前属性占比情况： ab1:"+ab1+"   ab2:"+ab2+"   ab3:"+ab3+"   ab4:"+ab4+"   ab5:"+ab5);

            double b1 = 5 - as1  ;
            double b2 = 5 - as2  ;
            double b3 = 4 - as3  ;
            double b4 = 4 - as4  ;
            double b5 = 4 - as5  ;

            //求最大值
            double[] arr = {b1, b2, b3, b4,b5};
            double res = arr[0];
            for (int i = 1; i < arr.length; i++){
                //逻辑为：如果条件表达式成立则执行result，否则执行arr[i]
                res = (arr[i] < res ? res : arr[i]);
            }
            System.out.println("最大差额为：" + res);

            //遍历获取 已超出和最大差额  这两种类型属性的下标 , 便于在数据库中查找优质解  p1=0,p2=1
            //已超出属性下标  p1=0
            ArrayList<Integer> overIndex = new ArrayList<>();
            if(ab1==1){overIndex.add(1);}
            if(ab2==1){overIndex.add(2);}
            if(ab3==1){overIndex.add(3);}
            if(ab4==1){overIndex.add(4);}
            if(ab5==1){overIndex.add(5);}

            //最大值属性下标  p1=1
            ArrayList<Integer> bigIndex = new ArrayList<>();
            for (int i = 0; i < arr.length; i++){
                if (arr[i]==res){
                    bigIndex.add(i+1);
                }
            }

            //overIndex(5) bigIndex(1,2)


            ArrayList<String> initFixItem = jdbcUtils.selectInitFixItem(overIndex,bigIndex);
            System.out.println("优质解集: "+initFixItem);

// ======================  替换的过程可能要多次遍历执行 begin ==========================================
            //从initFixItem 随机选一条替换掉原有的那条数据，然后验证是否符合要求  随机就存在问题，可能导致B树困境
            //①保证选的题目和之前没重复 ②最好属性题型一致 ③满足属性比例要求
            Integer key =new Random().nextInt(initFixItem.size());
            String newItem = initFixItem.get(key);
            System.out.println("将要替补的新解: "+newItem);


            //获取将要out的旧解的下标
            //这段代码逻辑好像有问题，均拿数组的最后一个值赋予给 oi bi
            //overIndex 长度最少是1  最多的3
            int oi = 0;
            for (int i = 0; i < overIndex.size(); i++) {
                oi = overIndex.get(i);
            }
            //bigIndex 长度最少是1  最多的3
            int bi = 0;
            for (int i = 0; i < bigIndex.size(); i++) {
                bi = bigIndex.get(i);
            }


            //匹配顺序：1. 完全匹配  2.匹配oi
            int lastIndex = 0;
            for (int i = 0; i < bachItemList.size(); i++) {
                // (1,0,1,1,1)
                String pattern = bachItemList.get(i).split(":")[1];
                if (pattern.substring(oi*2-2,oi*2-1).equals("1") &&  pattern.substring(bi*2-2,bi*2-1).equals("0")){
                    lastIndex = i;
                }else if(pattern.substring(oi*2-2,oi*2-1).equals("1")){
                    lastIndex = i;
                }
            }

            //修改Arraylist
            bachItemList.set(lastIndex, newItem);
            //计算新解是否符合要求

// ======================  替换的过程可能要多次遍历执行 begin ==========================================

        }

        return bachItemList;
    }




    /**
     * 根据原集合 旧解 新解 三者的关系进行，属性比例要求判断
     * 目前只适合单进单出
     *
     */
    private Boolean propCheck(ArrayList<String> bachItemList,String s, String s1) {

        // 刪除元素s，添加元素s1
        for (int i = 0; i < bachItemList.size(); i++) {
           if (bachItemList.get(i).equals(s)){
               bachItemList.set(i,s1);
           }
        }
        // 输出
        System.out.println(bachItemList.toString());

        //开始校验是否符合属性比例要求
        // ========================= 统计指标信息  ==========================
        //统计校验 属性比例
        // 定义局部变量
        double as1  = 0 ;
        double as2  = 0 ;
        double as3  = 0 ;
        double as4  = 0 ;
        double as5  = 0 ;

        for (int i = 0; i < bachItemList.size(); i++) {

            as1 = as1 +  Double.parseDouble(bachItemList.get(i).split(":")[1].split(",")[0].substring(1,2));
            as2 = as2 +  Double.parseDouble(bachItemList.get(i).split(":")[1].split(",")[1]);
            as3 = as3 +  Double.parseDouble(bachItemList.get(i).split(":")[1].split(",")[2]);
            as4 = as4 +  Double.parseDouble(bachItemList.get(i).split(":")[1].split(",")[3]);
            as5 = as5 +  Double.parseDouble(bachItemList.get(i).split(":")[1].split(",")[4].substring(0,1));

        }

        System.out.println("目前属性数量情况： as1:"+as1+" as2:"+as2+" as3:"+as3+" as4:"+as4+" as5:"+as5);

        //需要判断 属性比例是多了还是少了
        //定义局部变量 ab1  (-1->少于,0->正常,1->大于)
        System.out.println("=========================================");
        int ab1 ;
        double edx1 = as1/15;
        if(edx1>=0.2 && edx1<=0.34){
            ab1 = 0;
        }else if(edx1<0.2){
            ab1 = -1;
        }else {
            ab1 = 1;
        }

        int ab2 ;
        double edx2 = as2/15;
        if(edx2>=0.2 && edx2<=0.34){
            ab2 = 0;
        }else if(edx2<0.2){
            ab2 = -1;
        }else {
            ab2 = 1;
        }

        int ab3 ;
        double edx3 = as3/15;
        if(edx3>=0.1 && edx3<=0.27){
            ab3 = 0;
        }else if(edx2<0.1){
            ab3 = -1;
        }else {
            ab3 = 1;
        }

        int ab4 ;
        double edx4 = as4/15;
        if(edx4>=0.1 && edx4<=0.27){
            ab4 = 0;
        }else if(edx4<0.1){
            ab4 = -1;
        }else {
            ab4 = 1;
        }

        int ab5 ;
        double edx5 = as5/15;
        if(edx5>=0.1 && edx5<=0.27){
            ab5 = 0;
        }else if(edx5<0.1){
            ab5 = -1;
        }else {
            ab5 = 1;
        }

        //是否要进一步考虑 具体多了多少数量的情况
        System.out.println("目前属性占比情况： ab1:"+ab1+"   ab2:"+ab2+"   ab3:"+ab3+"   ab4:"+ab4+"   ab5:"+ab5);
        if(ab1 == 0 && ab2 == 0 && ab3 == 0 && ab4 == 0 && ab5 == 0 ){
            return true;
        }

        return false;
    }


    /**
     * 交叉  不计算适应度值
     *       长度，属性类型，属性比例
     */
    public  void crossCover(Papers papers) throws SQLException {

        System.out.println("================== cross ==================");
        //paperGenetic[1].length = 6
        //交叉的单位是  试卷的试题，个体的基因，所以交叉试题即可，
        //  交叉的结果： 长度不变化，属性类型可能因为重复而导致减少，进一步用修补算子进行修补，属性比例暂时未做控制  应该用在适应度上进行校验
        // 检验：为什么交叉后会产生这样的结果 （1,58,83,150,244,292）  临界值的问题，出在变异的部分，已修复
        //                          (34, 165, 248, 282, 76, 92, 126)
        //                          random.nextInt(100)  指生成一个介于[0,n)的int值
        Integer point = paperGenetic[1].length;
        for (int i = 0; i < paperGenetic.length-1; i++) {
            if (Math.random() < papers.getPc()) {
                //单点交叉   不会导致多个属性类型变化，最多只有一个属性存在问题（重复导致）
                String [] temp1 = new String[point];
                int a = new Random().nextInt(point);

                for (int j = 0; j < a; j++) {
                    temp1[j] = paperGenetic[i][j];
                }

                for (int j = a; j < point; j++) {
                    temp1[j] = paperGenetic[i+1][j];
                }
                // 放在内存执行，考虑的是空指针，只有发生交叉后，才需要校验
                // 判断size，执行修补算子  只改变了tem1 每执行一次pc 则校验一次
                System.out.println("==== 校验temp1的长度 ======"+temp1.length+ " "+ Arrays.asList(temp1).toString());
                correct(i,temp1);
            }
        }
    }



    /**
     * 判断size，执行修补操作  前提是基于交叉后,只有数量减少，不会出现多个属性类型出现问题
     *      基于此 前提是排序没问题
     */
    public  void correct(int i,String[] temp1) throws SQLException {

        System.out.println("第 "+i+" 题,开始交叉后校验 ..... ");

        //去重操作
        Set<String> setBegin = new HashSet<>(Arrays.asList(temp1));
        Set<Integer> setEnd = new HashSet<>();
        Set<Integer> oneSet = new HashSet<>();
        Set<Integer> twoSet = new HashSet<>();
        Set<Integer> threeSet = new HashSet<>();
        Set<Integer> fourSet = new HashSet<>();

        int one = 1;
        int two = 2;

        int oneAttNum = 0;
        int twoAttNum = 0;
        int threeAttNum = 0;
        int fourAttNum = 0;

        //分别将四中属性类型的数量进行统计   题库310道题  则50:100:100:50:10
        //在idea中，idea会为重新分配过地址的变量加上下划线，这是idea的设定，是为了快速发现那些变量被重新分配了地址
        Iterator<String> it = setBegin.iterator();
        while (it.hasNext()) {
            Integer num = Integer.valueOf(it.next().split(":")[0]);
            if (num < 51 ){
                oneAttNum = oneAttNum+1;
                oneSet.add(num);
            }else if (num < 151){
                twoAttNum = twoAttNum+1;
                twoSet.add(num);
            }else if(num < 251 ){
                threeAttNum = threeAttNum+1;
                threeSet.add(num);
            }else if(num < 301 ){
                fourAttNum = fourAttNum+1;
                fourSet.add(num);
            }
        }

        System.out.println(" 1个属性的题目量: "+oneAttNum+" 2个属性的题目量: "+twoAttNum+" 3个属性的题目量: "+threeAttNum+" 4个属性的题目量: "+fourAttNum);


        //目前只校验了属性类型数，未校验各个属性的所占比例
        // TODO  校验属性比例  （查看老师的文献，获取比例信息）
        //                    了解如何保证比例的代码公式
        //累加操作  获取type t 的所占比例，或者个数都可以的       这样的话，初始化难度加大了啊
        if (oneAttNum == one && twoAttNum == two && threeAttNum == two  && fourAttNum == one){
            System.out.println("第 "+i+" 题, 正常交叉,无需处理");
            System.out.println("第 "+i+" 题, 结束交叉修补算子！！");
        }else{
            System.out.println("第 "+i+" 题, 交叉导致属性所占比例不匹配：开始进行交叉修补算子 ....");
            //1个属性的校验
            if(oneAttNum<one){
                while(oneSet.size() != one){
                    Integer key = new Random().nextInt(51);
                    oneSet.add(key);
                }
            }
            //2个属性的校验
            if(twoAttNum<two){
                while(twoSet.size() != two){
                    Integer key = Math.abs(new Random().nextInt()) % 100 + 51;
                    twoSet.add(key);
                }
            }
            //3个属性的校验
            if(threeAttNum<two){
                while(threeSet.size() != two){
                    Integer key = Math.abs(new Random().nextInt()) % 100 + 151;
                    threeSet.add(key);
                }
            }
            //4个属性的校验
            if(fourAttNum<one){
                while(fourSet.size() != one){
                    Integer key = Math.abs(new Random().nextInt()) % 50 + 251;
                    fourSet.add(key);
                }
            }
            //set.size() 为什么会出现7
            setEnd.addAll(oneSet);
            setEnd.addAll(twoSet);
            setEnd.addAll(threeSet);
            setEnd.addAll(fourSet);

            //setEnd存储的是key  temp1存储的是6道试题
            //默认字典序，不会导致变异出现问题吗
            //用新的数组 替换掉 原有数组
            //setEnd.toArray(temp1);
            String ids = Arrays.asList(setEnd).toString().substring(2, Arrays.asList(setEnd).toString().length() - 2);
            JDBCUtils2 jdbcUtils = new JDBCUtils2();
            ArrayList<String> bachItemList = jdbcUtils.selectBachItem(ids);
            //ArrayList to String[]
            temp1 =  bachItemList.toArray(new String[temp1.length]);

            temp1 = sortPatch(temp1);
            //Arrays.sort(temp1);
            paperGenetic[i]=temp1;
            //打印选取的题目，打印的结果 应该是内存地址
            System.out.println("最终修补后的结果如下："+Arrays.toString(paperGenetic[i]));

            System.out.println("第 "+i+" 题, 结束交叉修补算子！！");

        }

    }


    /**
     * 变异  不计算适应度值
     *      长度，属性类型，属性比例
     */
    public  void mutate(Papers papers) throws SQLException {

        System.out.println("================== mutate ==================");
        JDBCUtils2 jdbcUtils = new JDBCUtils2();
        String key  ="";

        for (int i = 0; i < paperGenetic.length; i++) {
            if(Math.random() < papers.getPm()){
                Random random = new Random();
                //变异为什么要 length-1
                int mutatePoint = random.nextInt((paperGenetic[1].length)-1);
                //将Array 转 hashSet
                Set<String> set = new HashSet<>(Arrays.asList( paperGenetic[i]));
                System.out.println(i+" 原试卷: "+set);

                // 将要变异的元素   前提是试卷有序排列
                String s = paperGenetic[i][mutatePoint];
                System.out.println("  remove element: "+ s);
                set.remove(s);
                int removeId = Integer.parseInt(s.split(":")[0]);
                System.out.println("  临时试卷：  "+set);

                //单套试卷临时存储容器
                String[] temp1 = new String[paperGenetic[i].length];

                //生成一个合适的且不存在set中的key
                //属性类型：1个属性的1题[1,50]  2个属性的2题[51,150]  3个属性的2题[151,250] 4个属性的1题[251,300]
                //因为默认字典序的缘故 Array.sort() 故此处变异需根据id来进行分类讨论
                //if (mutatePoint==0){
                if (removeId<=50){
                    while (set.size() != paperGenetic[i].length ){
                        key = random.nextInt(49)+1+"";
                        if (!key.equals(removeId)){
                            System.out.println("添加元素："+key);
                            ArrayList<String> list = jdbcUtils.selectBachItem(key);
                            set.add(list.get(0)+"");
                        }
                    }
                //}else if(mutatePoint<3){
                }else if(removeId<=150){
                    while (set.size() != paperGenetic[i].length ){
                        key = new Random().nextInt(99) + 51+"";
                        if (!key.equals(removeId)){
                            System.out.println("添加元素："+key);
                            ArrayList<String> list = jdbcUtils.selectBachItem(key);
                            set.add(list.get(0)+"");
                        }
                    }
                //}else if(mutatePoint<5){
                }else if(removeId<=250){
                    while (set.size() != paperGenetic[i].length ){
                        key = new Random().nextInt(99) + 151+"";
                        if (!key.equals(removeId)){
                            System.out.println("添加元素："+key);
                            ArrayList<String> list = jdbcUtils.selectBachItem(key);
                            set.add(list.get(0)+"");
                        }
                    }
                //}else if(mutatePoint==5){
                }else if(mutatePoint<=301){
                    while (set.size() != paperGenetic[i].length ){
                        key = new Random().nextInt(49) + 251+"";
                        if (!key.equals(removeId)){
                            System.out.println("添加元素："+key);
                            ArrayList<String> list = jdbcUtils.selectBachItem(key);
                            set.add(list.get(0)+"");
                        }
                    }
                }
                set.toArray(temp1);
                //原排序方式  21 68 102 163 239 294
                //默认字典序  102 163 239 294 34 68   是否会有影响，对交叉和变异均有影响，故需进行排序修补
                //Arrays.sort(temp1);
                //排序修补 sortPatch
                temp1 =  sortPatch(temp1);


                paperGenetic[i]=temp1;
            }
            System.out.println("  add element: "+ key);
            System.out.println("  最终试卷： "+Arrays.toString(paperGenetic[i]));
        }

    }

    /**
     *
     * 排序修补
     *      1.获取id,排序，map映射
     *      2.获取id,重新据库查询一遍  返回的Array[]
     */
    private String[] sortPatch(String[] temp1) {

        //抽取id,封装成int[]
        int[] sortArray = new int[6];
        for (int i = 0; i < temp1.length; i++) {
            sortArray[i] = Integer.parseInt(temp1[i].split(":")[0]);
        }
        Arrays.sort(sortArray);
        for (int i = 0; i < sortArray.length; i++) {
            System.out.print(sortArray[i]+" ");
        }

        //根据id的位置，映射，重新排序 tmp1
        String[] temp2 = new String[6];
        for (int i = 0; i < sortArray.length; i++) {
            int index = sortArray[i];
            for (String ts : temp1) {
                if(Integer.parseInt(ts.split(":")[0]) == index){
                    temp2[i] = ts;
                }
            }
        }
        for (int i = 0; i < temp2.length; i++) {
            System.out.print(temp2[i]+" ");
        }

        return  temp2;
    }


    /**
     * 选择: 根据轮盘赌选择适应度高的个体
     * 将适应度值打印出来看一下，方便后续比较
     *     ①如何去计算适应度：以试卷为单位，min*exp^1
     *     ②初始化时直接将适应度值计算，并保存在数据库中  这种方式是最快的，但试题的选取及构成试卷是随机的，且包括交叉变异，故不适合
     *     如果需要可以，将最终的结果的汇总到数据库。
     *
     *     仅仅依靠选择和轮盘赌，其变化是很小的，故需要更大的外部作用(交叉+变异）
     *
     */
    public    void selection( ){

        System.out.println("====================== select ======================");
        //10套试卷   6道题目
        int paperSize = paperGenetic.length;

        //轮盘赌 累加百分比
        double[] fitPie = new double[paperSize];

        //执行计算适应度的操作   min*exp^1
        ArrayList fitnessArray = getFitness(paperSize);

        //每套试卷的适应度占比
        double[] fitPro = (double[]) fitnessArray.get(2);

        //累加初始值
        double accumulate = 0;

        //试卷占总试卷的适应度累加百分比
        for (int i = 0; i < paperSize; i++) {
            fitPie[i] = accumulate + fitPro[i];
            accumulate += fitPro[i];
            //System.out.println("试卷"+ i+"占目前总试卷的适应度累加百分比： "+fitPie[i]);
        }

        //累加的概率为1   数组下标从0开始
        fitPie[paperSize-1] = 1;

        //初始化容器 随机生成的random概率值
        double[] randomId = new double[paperSize];

        //不需要去重
        for (int i = 0; i < paperSize; i++) {
            randomId[i] = Math.random();
        }

        // 排序
        Arrays.sort(randomId);

        //打印出随机抽取的random概率值
        printDoubleArray(randomId);

        //轮盘赌 越大的适应度，其叠加时增长越快，即有更大的概率被选中
        String[][] newPaperGenetic =new String[paperSize][];
        int newSelectId = 0;
        for (int i = 0; i < paperSize; i++) {
            while (newSelectId < paperSize && randomId[newSelectId] < fitPie[i]){
                //需要确保fitPie[i] 和 paperGenetic[i] 对应的i 是同一套试卷
                newPaperGenetic[newSelectId]   = paperGenetic[i];
                newSelectId += 1;
            }
        }
        //输出老种群的适应度值
        //System.out.print("老种群");


        //重新赋值种群的编码
        paperGenetic=newPaperGenetic;

        //输出新种群的适应度值
        //System.out.print("新种群");

    }

    private void printDoubleArray( double[] randomId) {

        //把基本数据类型转化为列表 double[]转Double[]
        int num = randomId.length;
        Double [] arrDouble=new Double[num];
        for(int i=0;i<num;i++){
            arrDouble[i]=randomId[i];
        }

        //Double[]转List
        List<Double> list = Arrays.asList(arrDouble);
        System.out.println("随机抽取的random概率值："+list);

    }


    /**
     * 1.selection 计算适应度值,
     * 2.elitistStrategy 保存全局变量(全局最优  局部最优  局部最差)
     *
     */
    private ArrayList getFitness(int paperSize){

        // 所有试卷的适应度总和
        double fitSum = 0.0;
        // 每套试卷的适应度值
        double[] fitTmp = new double[paperSize];
        // 每套试卷的适应度占比
        double[] fitPro = new double[paperSize];

        // 计算试卷的适应度值，即衡量试卷的指标之一 Fs
        for (int i = 0; i < paperSize; i++) {

            double adi1r =0;
            double adi2r =0;
            double adi3r =0;
            double adi4r =0;
            double adi5r =0;

            double adi1d =0;
            double adi2d =0;
            double adi3d =0;
            double adi4d =0;
            double adi5d =0;

            //ArrayList -> []
            String [] itemList = paperGenetic[i];
            for (int j = 0; j < itemList.length; j++) {

                String[] splits = itemList[j].split(":");
                adi1r = adi1r + Double.parseDouble(splits[2]);
                adi2r = adi2r + Double.parseDouble(splits[3]);
                adi3r = adi3r + Double.parseDouble(splits[4]);
                adi4r = adi4r + Double.parseDouble(splits[5]);
                adi5r = adi5r + Double.parseDouble(splits[6]);

                adi1d = adi1d + Double.parseDouble(splits[7]);
                adi2d = adi2d + Double.parseDouble(splits[8]);
                adi3d = adi3d + Double.parseDouble(splits[9]);
                adi4d = adi4d + Double.parseDouble(splits[10]);
                adi5d = adi5d + Double.parseDouble(splits[11]);

            }

            // 方案  进行乘以一个exp 来进行适应度值的降低    高等数学里以自然常数e为底的指数函数
            //       System.out.printf("exp(%.3f) 为 %.3f%n", -2.7183, Math.exp(-2.7183));
            //       - 表示取倒数  1/(2.7183 * 2.7183)  =  0.135
            // 参数,需要考虑到属性比例
            // 原有的adi min小于1是否有影响（没影响，只是额外新增一个惩罚参数） 但因为实在太小了，所以*100
            // 计算每套试卷的惩罚系数  实际比例 vs 期望比例
            // 5个属性，6个题目，15次选择  1+2*2+3*2+4 = 15
            // 第1属性[3,5]        第2属性[3,5]        第3属性[2,4]       第4属性[2,4]       第5属性[2,4]
            // 第1属性[0.2,0.34]   第2属性[0.2,0.34]   第3属性[0.1,0.27]  第4属性[0.1,0.27]  第5属性[0.1,0.27]
            // 按照比例计算 可以减少后续的修改
            String [] expList = paperGenetic[i];
            int exp1 = 0;
            int exp2 = 0;
            int exp3 = 0;
            int exp4 = 0;
            int exp5 = 0;

            for (int j = 0; j < expList.length; j++) {
                String[] splits = expList[j].split(":");
                exp1 = exp1 + Integer.parseInt(splits[1].split(",")[0].substring(1,2));
                exp2 = exp2 + Integer.parseInt(splits[1].split(",")[1]);
                exp3 = exp3 + Integer.parseInt(splits[1].split(",")[2]);
                exp4 = exp4 + Integer.parseInt(splits[1].split(",")[3]);
                exp5 = exp5 + Integer.parseInt(splits[1].split(",")[4].substring(0,1));
            }
            System.out.println("目前属性数量情况： exp1:"+exp1+" exp2:"+exp2+" exp3:"+exp3+" exp4:"+exp4+" exp5:"+exp5);

            // 第1属性[0.2,0.34]   第2属性[0.2,0.34]   第3属性[0.1,0.27]  第4属性[0.1,0.27]  第5属性[0.1,0.27]
            //先判断是否在范围内，在的话，为0，不在的话，然后进一步和上下限取差值，绝对值
            double ed1 ;
            double edx1 = exp1/15.0;
            if(edx1>=0.2 && edx1<0.34){
                ed1 = 0;
            }else if(edx1<0.2){
                ed1 =  Math.abs(0.2 - edx1);
            }else {
                ed1 =  Math.abs(edx1 - 0.34);
            }

            double ed2 ;
            double edx2 = exp2/15.0;
            if(edx2>=0.2 && edx2<0.34){
                ed2 = 0;
            }else if(edx2<0.2){
                ed2 =  Math.abs(0.2 - edx2);
            }else {
                ed2 =  Math.abs(edx2 - 0.34);
            }

            double ed3 ;
            double edx3 = exp3/15.0;
            if(edx3>=0.1 && edx3<0.27){
                ed3 = 0;
            }else if(edx3<0.1){
                ed3 =  Math.abs(0.1 - edx3);
            }else {
                ed3 =  Math.abs(edx3 - 0.27);
            }

            double ed4 ;
            double edx4 = exp4/15.0;
            if(edx4>=0.1 && edx4<0.27){
                ed4 = 0;
            }else if(edx4<0.1){
                ed4 =  Math.abs(0.1 - edx4);
            }else {
                ed4 =  Math.abs(edx4 - 0.27);
            }

            double ed5 ;
            double edx5 = exp5/15.0;
            if(edx5>=0.1 && edx5<0.27){
                ed5 = 0;
            }else if(edx5<0.1){
                ed5 =  Math.abs(0.1 - edx5);
            }else {
                ed5 =  Math.abs(edx5 - 0.27);
            }

            double expNum = -(ed1 + ed2 + ed3 + ed4 + ed5);

            //System.out.printf("exp(%.3f) 为 %.3f%n", expNum, Math.exp(expNum));


            //均值 和 最小值
            double avgrum = (adi1r + adi2r + adi3r + adi4r + adi5r)/5 ;
            double minrum = Math.min(Math.min(Math.min(Math.min(adi1r,adi2r),adi3r),adi4r),adi5r) * 100 ;
            double avgdina = (adi1d + adi2d + adi3d + adi4d + adi5d)/5 ;
            double mindina = Math.min(Math.min(Math.min(Math.min(adi1d,adi2d),adi3d),adi4d),adi5d) ;

            //System.out.printf("avgrum=%s \t minrum=%s \t avgdina=%s \t mindina=%s \n", avgrum, minrum, avgdina,mindina);


            //System.out.println("minrum: "+minrum);
            //新增 惩罚系数
            minrum = minrum * Math.exp(expNum);
            fitTmp[i] = minrum ;

            fitSum = fitSum + minrum ;

// ==================== elitistStrategy  ====================
            //  全局最优 和 局部最优 的个数关系
            //  导致的影响是什么： ①如果全局最优只有一个值,那么是否会导致过于相似  only 6道题
            //                  ②如果全局最优只有多个值,复杂性增大
            //  此处使用全局最优一个解

            //  tmp > 局部最优，则替换局部最优
            if(minrum > LocalOptimal[i]){
                LocalOptimal[i] = minrum;
            }
            System.out.println("局部最优："+LocalOptimal[i]);

            //  全局最优，从局部最优中取值  local > global,则替换全局最优
            for (double local : LocalOptimal) {
                if(local > GlobalOptimal){
                    GlobalOptimal = local;
                }
                System.out.println("全局最优："+GlobalOptimal);
            }
        }


        for (int i = 0; i < paperSize; i++) {
            //  各自的比例
            fitPro[i] = fitTmp[i] / fitSum;
        }

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(fitSum);
        arrayList.add(fitTmp);
        arrayList.add(fitPro);

        return  arrayList;
    }



    /**
     * 格式转换工具
     */
    public Double numbCohesion(Double adi){

        return Double.valueOf(String.format("%.4f", adi));

    }

    /**
     * 精英策略
     *      1.获取全局最优,和局部最优/局部最差（计算适应度值，并保存为全局变量）
     *      2.用全局最优替换掉局部最优
     */
    public  void  elitistStrategy(){

        System.out.println("================== elitistStrategy ==================");
        //getFitness(paperGenetic.length);

        // 全局最优解替换掉局部最优解
        for (int i = 0; i < LocalOptimal.length; i++) {
            LocalOptimal[i] = GlobalOptimal;
        }

    }

}



