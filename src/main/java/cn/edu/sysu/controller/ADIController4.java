package cn.edu.sysu.gmall_publisher.controller;

import cn.edu.sysu.adi.TYPE;
import cn.edu.sysu.pojo.Papers;
import cn.edu.sysu.utils.JDBCUtils4;
import cn.edu.sysu.utils.KLUtils;
import org.junit.Test;

import java.sql.SQLException;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 *
 * @Author : song bei chang
 * @create 2021/05/18 0:17
 */
public class ADIController4 {


    /*  容器 全局最优 局部最优  */

    private static double GlobalOptimal = 0;
    private static double[] LocalOptimal = new double[10];
    private static ArrayList<String> bankList = new ArrayList();


    /* 10套试卷 6道题  */

    private static String[][] paperGenetic =new String[10][10];


    private  JDBCUtils4 jdbcUtils = new JDBCUtils4();


    /**
     * 计算适应度值  ①计算方式 轮盘赌
     *             ②计算单位（单套试卷）
     *             ③比较方式 取min，按adi属性取整套试卷的最小值
     *
     */
    @Test
    public  void ori() throws SQLException {

        //选择10套的原因，只有基数够大，才能为交叉变异提供相对较多的原始材料  打算先以10套试卷为变更基础,最后正序取前三
        //抽取试卷  10套、每套试卷10题
        Papers papers = new Papers();
        papers.setPc(0.5);
        papers.setPm(0.5);

        //初始化试卷   从题库中选取题目构成试卷  长度，题型，属性比例
        //原始
        //initItemBank();
        //轮盘赌
        initItemBank4();
        //锦标赛
        //initItemBank5();


        // i 迭代次数
        // 迭代3次  出现只有三套试卷可选
        // 迭代6次  大概率十套试卷一模一样
        for (int i = 0; i < 2000; i++) {
            //selection();
            crossCover(papers);
            //mutate(papers);
            // FIXME  待实现 小生境环境的搭建
            //elitistStrategy();
        }
        System.out.println();
    }


    /**
     * TODO 遗传算法中的锦标赛选择策略
     * TODO 每次从种群中取出一定数量个体（放回抽样），然后选择其中最好的一个进入子代种群。
     *      具体的操作步骤如下：
     *          1、确定每次选择的个体数量N。（N元锦标赛选择即选择N个个体）
     *          2、从种群中随机选择N个个体(每个个体被选择的概率相同) ，根据每个个体的适应度值，
     *             选择其中适应度值最好的个体进入下一代种群。
     *          3、重复步骤(2)多次（重复次数为种群的大小），直到新的种群规模达到原来的种群规模。
     */
    private void initItemBank5() throws SQLException {

        System.out.println("====== 开始选题,构成试卷  锦标赛构造  ======");

        // 试卷大小
        int  paperNum = 1 ;
        // 试题大小
        int questionsNum = 10 ;

        // 全部试卷的集合
        ArrayList<ArrayList<String>> itemList = new ArrayList<>();
        // 单套试卷的集合 [8:CHOSE:(1,0,0,0,0), 3:CHOSE:(0,0,0,1,0)]
        HashSet<String> itemSet = new HashSet<>();

        // 硬性约束：长度   软性约束：题型，属性比例
        for (int j = 0; j < paperNum; j++) {

            //清空上一次迭代的数据
            itemSet.clear();

            for (int i = 0; i < questionsNum; i++) {
                //减少频繁的gc
                String item ;
                //去重操作
                while (itemSet.size() == i) {
                    //获取试题id
                    int sqlId = championship(itemSet);
                    //为了防止数组越界,角标0   但id+1 是否会有影响？
                    item = jdbcUtils.selectOneItem(sqlId);
                    itemSet.add(item);
                }
            }
            // 将hashSet转ArrayList 并排序
            ArrayList<String> list = new ArrayList<>(itemSet);
            Collections.sort(list);
            itemList.add(list);

            // idList容器
            ArrayList<Integer> idList = new ArrayList<>();
            for (int i = 0; i <list.size() ; i++) {
                idList.add(Integer.valueOf(list.get(i).split(":")[0]));
            }
            //list  排序
            Collections.sort(idList);
            //输出所有抽取到的试题id
            System.out.println("试卷"+j+"的试题id: "+idList);

            String ids = idList.toString().substring(1,idList.toString().length()-1);

            ArrayList<String> bachItemList = jdbcUtils.selectBachItem(ids);

            // 把题库提升为全局变量，方便整体调用 容器：二维数组
            // 交叉变异的对象是 试题   即试卷=个体  试题=基因
            String[] itemArray = new String[bachItemList.size()];
            for (int i = 0; i < bachItemList.size(); i++) {
                itemArray[i] = bachItemList.get(i);
            }
            // 赋值
            paperGenetic[j] = itemArray;

        }

        System.out.println(itemList.toString());


    }



    /**
     *      TODO  本周任务  1.通过构造的方法初始化题目 (轮盘赌  + 锦标赛   ok）
     *      TODO          2.将属性类型个数改为题型，以及比例   （校验   ok）
     *      TODO          3.交叉变异后 校验比例（题型+属性） 直接替换  （迭代一百次以后，没有就直接退出） （待实现）
     *
     *
     * FIXME 使用构造法选取题目
     *          1.题型构造解决 （完全随机模式,不考虑下限比例）
     *          2.属性构造解决 （完全随机模式,不考虑下限比例）
     *          3.设置属性比例  可以通过惩罚系数来设定  超出,则急剧减少
     *          4.缺陷在于无法设置权重 => 解决方案：在初始化的时候，就不一定保证题型和属性符合要求，使用GA
     *                迭代和轮盘赌解决。
     *
     *
     */
    private void initItemBank4() throws SQLException {

        System.out.println("====== 开始选题,构成试卷  轮盘赌构造  ======");

        // 试卷大小  份数很有争议，少：具有现实含义   多：提供遗传变异的基本单位
        int  paperNum = 10 ;
        // 试题大小
        int questionsNum = 10 ;

        // 全部试卷的集合   可以省略,使用全局变量数组来接收 paperGenetic
        ArrayList<ArrayList<String>> itemList = new ArrayList<>();
        // 单套试卷的集合
        HashSet<String> itemSet = new HashSet<>();

        // 题库310道题  50:100:100:50:10   硬性约束：长度   软性约束：题型，属性比例
        // 获取全局解  [8:CHOSE:(1,0,0,0,0), 3:CHOSE:(0,0,0,1,0)]
        bankList = getBank();


        for (int j = 0; j < paperNum; j++) {

            //清空上一次迭代的数据
            itemSet.clear();

            for (int i = 0; i < questionsNum; i++) {
                //减少频繁的gc
                String item ;
                //去重操作
                while (itemSet.size() == i) {
                    //获取试题id
                    int sqlId = roulette(itemSet);
                    item = jdbcUtils.selectOneItem(sqlId+1);
                    itemSet.add(item);
                }
            }

            // 将hashSet转ArrayList 并排序
            ArrayList<String> list = new ArrayList<>(itemSet);
            // 以下两步可以省略
            Collections.sort(list);
            itemList.add(list);

            // idList容器
            ArrayList<Integer> idList = new ArrayList<>();
            for (int i = 0; i <list.size() ; i++) {
                idList.add(Integer.valueOf(list.get(i).split(":")[0]));
            }

            //list  排序
            Collections.sort(idList);
            //输出所有抽取到的试题id
            System.out.println("试卷"+j+"的试题id: "+idList);

            // 根据id从数据库中查询相对应的试题
            String ids = idList.toString().substring(1,idList.toString().length()-1);
            ArrayList<String> bachItemList = jdbcUtils.selectBachItem(ids);

            // 把题库提升为全局变量，方便整体调用 容器：二维数组
            // 交叉变异的对象是 试题   即试卷=个体  试题=基因
            String[] itemArray = new String[bachItemList.size()];
            for (int i = 0; i < bachItemList.size(); i++) {
                itemArray[i] = bachItemList.get(i);
            }
            // 赋值
            paperGenetic[j] = itemArray;

        }

        System.out.println(itemList.toString());


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
     * TODO  本周任务  1.通过构造的方法初始化题目 (轮盘赌  + 锦标赛)
     * TODO          2.将属性类型个数改为题型，以及比例
     * TODO          3.交叉变异后 校验比例（题型+属性） 直接替换  （迭代一百次以后，没有就直接退出）
     *
     *
     */
    private void initItemBank() throws SQLException {

        System.out.println("====== 开始选题,构成试卷  ======");

        /*  试卷数 */
        int paperNum = 10 ;
        /* 单张试卷每种题型的题目数量 6 */
        int oneAttNum = 1;
        int twoAttNum = 2;
        int threeAttNum = 2;
        int fourAttNum = 1;

        //JDBCUtils4 jdbcUtils = new JDBCUtils4();

        // 题库310道题  50:100:100:50:10   长度，题型，属性比例
        String sql1 = "SELECT CEILING( RAND () * 49 ) + 1  AS id" ;
        String sql2 = "SELECT CEILING( RAND () * 99 ) + 51 AS id" ;
        String sql3 = "SELECT CEILING( RAND () * 99 ) + 151 AS id" ;
        String sql4 = "SELECT CEILING( RAND () * 59 ) + 251 AS id" ;



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
     *  TODO  属性比例校验
     *  TODO  根据现有解得出一个评判标准,然后重新抽取题目选取最优解
     *
     *  <属性比例校验>
     *  具体步骤如下：
     *      1.获取各个属性的比例信息
     *      2.和预期做比较  得出差值
     *      3.找寻最合适的解
     *          ①是否需要计算题型的差值（上一步只能保证size正常,题型基本正常,属性未做控制）
     *            建议要，无非多几行代码的事，一次性对适应度值进行最终赋值，避免了类型和属性的来回比较和权衡
     *  题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3] 应用[0.1,0.3]
     *  属性比例 第1属性[0.2,0.4]   第2属性[0.2,0.4]   第3属性[0.1,0.3]  第4属性[0.1,0.3]  第5属性[0.1,0.3]
     *            (0,0,1,-1) (1,0,0,0,0)
     *          ②计算公式
     *      4.输出最后的方案
     *
     */
    private ArrayList<String> correctAttribute(ArrayList<String> bachItemList) throws SQLException {

//=========================  1.0 指标统计   ================================
        System.out.println("=================  指标信息初步统计  =====================");
        //ArrayList<String> 转 hashSet<String>
        HashSet<String> itemSet = new HashSet<>(bachItemList);

        //题型个数
        int typeChose  = 0;
        int typeFill   = 0;
        int typeShort  = 0;
        int typeCompre = 0;


        //各个题型的数目
        for (String s:itemSet) {
            System.out.println(s);

            //计算每种题型个数
            if(TYPE.CHOSE.toString().equals(s.split(":")[1])){
                typeChose += 1;
            }
            if(TYPE.FILL.toString().equals(s.split(":")[1])){
                typeFill += 1;
            }
            if(TYPE.SHORT.toString().equals(s.split(":")[1])){
                typeShort += 1;
            }
            if(TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])){
                typeCompre += 1;
            }
        }

        //题型比例
        double typeChoseRation  =  typeChose/10.0;
        double typeFileRation   =  typeFill/10.0;
        double typeShortRation  =  typeShort/10.0;
        double typeCompreRation =  typeCompre/10.0;


        //属性个数
        int attributeNum1  = 0;
        int attributeNum2  = 0;
        int attributeNum3  = 0;
        int attributeNum4  = 0;
        int attributeNum5  = 0;

        //各个属性的数目
        for (String s:itemSet) {
            System.out.println(s);

            //计算每种属性个数
            if("1".equals(s.split(":")[2].substring(1,2))){
                attributeNum1 += 1;
            }
            if("1".equals(s.split(":")[2].substring(3,4))){
                attributeNum2 += 1;
            }
            if("1".equals(s.split(":")[2].substring(5,6))){
                attributeNum3 += 1;
            }
            if("1".equals(s.split(":")[2].substring(7,8))){
                attributeNum4 += 1;
            }
            if("1".equals(s.split(":")[2].substring(9,10))){
                attributeNum5 += 1;
            }
        }
        System.out.println("AttributeRatio1: "+attributeNum1+"\tAttributeRatio2: "+attributeNum2+"\tAttributeRatio3: "+attributeNum3+"\tAttributeRatio4: "+attributeNum4+"\tAttributeRatio5: "+attributeNum5);

        //属性比例
        double attributeRatio1 = attributeNum1/23.0;
        double attributeRatio2 = attributeNum2/23.0;
        double attributeRatio3 = attributeNum3/23.0;
        double attributeRatio4 = attributeNum4/23.0;
        double attributeRatio5 = attributeNum5/23.0;


        System.out.println("=================  指标信息flag统计  =====================");

        //题型flag
        //定义局部变量 ab1  (-1->少于,0->正常,1->大于)
        int tf1 ;
        if(typeChoseRation>=0.2 && typeChoseRation<=0.4){
            tf1 = 0;
        }else if(typeChoseRation<0.2){
            tf1 = -1;
        }else {
            tf1 = 1;
        }

        int tf2 ;
        if(typeFileRation>=0.2 && typeFileRation<=0.4){
            tf2 = 0;
        }else if(typeFileRation<0.2){
            tf2 = -1;
        }else {
            tf2 = 1;
        }

        int tf3 ;
        if(typeShortRation>=0.1 && typeShortRation<=0.3){
            tf3 = 0;
        }else if(typeShortRation<0.1){
            tf3 = -1;
        }else {
            tf3 = 1;
        }

        int tf4 ;
        if(typeCompreRation>=0.1 && typeCompreRation<=0.3){
            tf4 = 0;
        }else if(typeCompreRation<0.1){
            tf4 = -1;
        }else {
            tf4 = 1;
        }
        //typeFlag 是否要进一步考虑 具体多了多少数量的情况  不需要,有兜底的方案，用一个较优解inList.get(0)替换一个outList.get(先排序 在递归查询)
        //标记符：比预期多 mark = 1 ，对于少了的属性 mark = -1
        //优势: 可以简化从题库中选取可选解的难度（最优解排在前面，弱解排在后面）
        String typeFlag = "("+tf1+","+tf2+","+tf3+","+tf4+")";
        System.out.println("目前题型占比情况： typeFlag:"+typeFlag);


        int af1 ;
        if(attributeRatio1>=0.2 && attributeRatio1<=0.4){
            af1 = 0;
        }else if(attributeRatio1<0.2){
            af1 = -1;
        }else {
            af1 = 1;
        }

        int af2 ;
        if(attributeRatio2>=0.2 && attributeRatio2<=0.4){
            af2 = 0;
        }else if(attributeRatio2<0.2){
            af2 = -1;
        }else {
            af2 = 1;
        }

        int af3 ;
        if(attributeRatio3>=0.1 && attributeRatio3<=0.3){
            af3 = 0;
        }else if(attributeRatio3<0.1){
            af3 = -1;
        }else {
            af3 = 1;
        }

        int af4 ;
        if(attributeRatio4>=0.1 && attributeRatio4<=0.3){
            af4 = 0;
        }else if(attributeRatio4<0.1){
            af4 = -1;
        }else {
            af4 = 1;
        }

        int af5 ;
        if(attributeRatio5>=0.1 && attributeRatio5<=0.3){
            af5 = 0;
        }else if(attributeRatio5<0.1){
            af5 = -1;
        }else {
            af5 = 1;
        }
        //输出 attributeFlag
        String attributeFlag = "("+af1+","+af2+","+af3+","+af4+","+af5+")";
        System.out.println("目前属性占比情况： attributeFlag:"+attributeFlag);


//=========================  2.0 解集统计   ================================

        //根据flagFlag 和 attributeFlag 得出一个评判标准,进行获得 ori in out
        //占比失衡的情况：(设置的属性比例有关)
        //多    可以随机选取一个带有这个属性的题目,然后(move,add)   导致的问题均是：属性题型发生变化
        //      ①多一个  ②多N个 先遍历匹配(完全匹配)，若没有，则进行随机选取两次
        //少    可以随机选取一个不带有这个属性的题目，然后(move,add)
        //      ①少一个  ②少N个 先遍历匹配(完全匹配)，若没有，则进行随机选取两次

        //多&少  (原始解，退出解,可选解)



        //选取的标准: 遍历选取，然后重新计算是否符合要求，这样将会导致计算很冗余
        //in解替换掉旧out解 （利用到此试题中的相关属性 即as1 as2 as3 as4 as5 便于做更进一步的判断）

        // out解的容器  (可能造成属性比例失衡的解集)
        Set<String> resultAll = new HashSet<>();
        Set<String> resultMore = new HashSet<>();
        Set<String> resultLess = new HashSet<>();


        //取出属性比例过多的集合的交集（TODO 需考虑以下情况: 两个单集合，各自多一个属性，其无交集   ok）
        //这个需要和要求比例进行考虑  0.4*23 = 9.2  十道题 意味着属性12没问题  0.3*23=6.9  没问题，只要保证超过半数即可
        // 5/23 = 0.217
        //解决方案： 可能需要换数据结构，用arrayList接收，最后用groupBy统计个数， 判断标准：个数越大，则优先级越高
        if(af1==1 || af2==1 || af3==1 || af4==1 || af5==1){

            //需要判断 set 是否为空  把判断为空的逻辑 放在上面判断
            resultMore.clear();
            HashSet<String> set1 = new HashSet<>();
            HashSet<String> set2 = new HashSet<>();
            HashSet<String> set3 = new HashSet<>();
            HashSet<String> set4 = new HashSet<>();
            HashSet<String> set5 = new HashSet<>();

            //表明属性1比例过多，用set1集合接收   bachItemList为方法参数，即一套试卷
            if(af1==1){
                for (String aBachItemList : bachItemList) {
                    if (aBachItemList.split(":")[2].split(",")[0].substring(1, 2).equals("1")) {
                        set1.add(aBachItemList);
                    }
                }
                if (resultMore.size()==0){
                    resultMore.addAll(set1);
                }else {
                    resultMore.retainAll(set1);
                }
            }

            if(af2==1){
                for (String aBachItemList : bachItemList) {
                    if (aBachItemList.split(":")[2].split(",")[1].equals("1")) {
                        set2.add(aBachItemList);
                    }
                }
                if (resultMore.size()==0){
                    resultMore.addAll(set2);
                }else {
                    resultMore.retainAll(set2);
                }
            }

            if(af3==1){
                for (String aBachItemList : bachItemList) {
                    if (aBachItemList.split(":")[2].split(",")[2].equals("1")) {
                        set3.add(aBachItemList);
                    }
                }
                if (resultMore.size()==0){
                    resultMore.addAll(set3);
                }else {
                    resultMore.retainAll(set3);
                }
            }

            if(af4==1){
                for (String aBachItemList : bachItemList) {
                    if (aBachItemList.split(":")[2].split(",")[3].equals("1")) {
                        set4.add(aBachItemList);
                    }
                }
                if (resultMore.size()==0){
                    resultMore.addAll(set4);
                }else {
                    resultMore.retainAll(set4);
                }
            }

            if(af5==1){
                for (String aBachItemList : bachItemList) {
                    if (aBachItemList.split(":")[2].split(",")[4].substring(0, 1).equals("1")) {
                        set5.add(aBachItemList);
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

            //赋值到全局变量  赋值给resultAll的意义是什么？
            if (resultAll.size()==0){
                resultAll.addAll(resultMore);
            }else {
                resultAll.retainAll(resultMore);
            }

        }



        //取出属性比例不足的集合的交集
        //这个需要和要求比例进行考虑  0.2*23 = 4.6  十道题 意味着属性12没问题  0.1*23=2.3  没问题，只要保证低于半数即可
        // 5/23 = 0.217
        if(af1==-1 || af2==-1 || af3==-1 || af4==-1 || af5==-1){

            //需要判断 set 是否为空  把判断为空的逻辑 放在上面判断
            resultLess.clear();
            HashSet<String> set1 = new HashSet<>();
            HashSet<String> set2 = new HashSet<>();
            HashSet<String> set3 = new HashSet<>();
            HashSet<String> set4 = new HashSet<>();
            HashSet<String> set5 = new HashSet<>();

            //表明属性1比例过少，用set集合接收
            if(af1==-1){

                for (String aBachItemList : bachItemList) {
                    if (aBachItemList.split(":")[2].split(",")[0].substring(1, 2).equals("0")) {
                        set1.add(aBachItemList);
                    }
                }
                if (resultLess.size()==0){
                    resultLess.addAll(set1);
                }else {
                    resultLess.retainAll(set1);
                }
            }

            if(af2==-1){
                for (String aBachItemList : bachItemList) {
                    if (aBachItemList.split(":")[2].split(",")[1].equals("0")) {
                        set2.add(aBachItemList);
                    }
                }
                if (resultLess.size()==0){
                    resultLess.addAll(set2);
                }else {
                    resultLess.retainAll(set2);
                }
            }

            if(af3==-1){
                for (String aBachItemList : bachItemList) {
                    if (aBachItemList.split(":")[2].split(",")[2].equals("0")) {
                        set3.add(aBachItemList);
                    }
                }
                if (resultLess.size()==0){
                    resultLess.addAll(set3);
                }else {
                    resultLess.retainAll(set3);
                }
            }

            if(af4==-1){
                for (String aBachItemList : bachItemList) {
                    if (aBachItemList.split(":")[2].split(",")[3].equals("0")) {
                        set4.add(aBachItemList);
                    }
                }
                if (resultLess.size()==0){
                    resultLess.addAll(set4);
                }else {
                    resultLess.retainAll(set4);
                }
            }

            if(af5==-1){
                //使用 foreach 替换掉 for   (Arraylist时，fori 性能高于 foreach  Linkedlist 时，fori低于foreach)
                for (String aBachItemList : bachItemList) {
                    if (aBachItemList.split(":")[2].split(",")[4].substring(0, 1).equals("0")) {
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



//=========================  3.0 执行修补操作   ================================

        /*
         * resultAll  resultMore resultLess 的关系判断
         *      1.resultLess有  resultMore有值  则取resultAll
         *      ①FIXME  retainAll 是否存在空集合的情况  resultMore 和 resultLess 用set接收,more和less分别转list后，用list接收，然后转set 得到最终解
         *      ②考虑比例信息0.2~0.4  0.1~0.3  不存在空集合的情况
         *      2.resultLess无  resultMore有值 则取 resultMore
         *      3.resultMore无  resultLess有值 则取 resultLess
         *
         * 目标：将新解替换旧解
         * 方法：去题库中搜索，取出新解集后，循环遍历，直到符合要求的解生成
         * 要求：
         *           1.删除不影响其他类型和属性比例
         *           2.新增不影响其他类型和属性比例
         *           3.如果找不到，则在较优解中随机选取一个用作替换解即可
         *
         */

//*************************  3.1 resultLess有  resultMore有值   *************************

        //目标：将新解替换旧解
        //方法：去题库中搜索，取出新解集后，循环遍历，直到符合要求的解生成
        //删除旧解的要求：
        //           1.删除不影响其他类型和属性比例
        //           2.新增不影响其他类型和属性比例
        //           3.如果找不到，则在较优解中随机选取一个用作替换解即可
        //拼接SQL  搜索条件 p3 = 1 and p5 = 0
        if(resultMore.size()>0 && resultLess.size()>0){
            System.out.println("本套试卷 既有属性比例过多，又有属性比例不足的情况");
            System.out.println(resultAll);

            StringBuilder sb = new StringBuilder();
            if(af1>0){
                sb.append(" p1=0 and ");
            }else if (af1<0){
                sb.append(" p1=1 and ");
            }

            if(af2>0){
                sb.append(" p2=0 and ");
            }else if (af2<0){
                sb.append(" p2=1 and ");
            }

            if(af3>0){
                sb.append(" p3=0 and ");
            }else if (af3<0){
                sb.append(" p3=1 and ");
            }

            if(af4>0){
                sb.append(" p4=0 and ");
            }else if (af4<0){
                sb.append(" p4=1 and ");
            }

            if(af5>0){
                sb.append(" p5=0 and ");
            }else if (af5<0){
                sb.append(" p5=1 and ");
            }

            String sql = sb.toString().substring(0, sb.toString().length() - 4);
            //获取新解的集合
            ArrayList<String> inList = jdbcUtils.selectBySql(sql);

            // ori解集  out解集  in解集 的关系
            // 原始解集 - out解 + in解 = 新解(拿新解去再次核对)
            // 循环的逻辑，应该是 外层out解，内层in解，不断的调用属性比例校验方法，如满足要求则退出，不满足则继续遍历。最后的终止条件是 遍历终止

            List<String> outList = new ArrayList<>(resultAll);
            System.out.println("校验前的集合:"+bachItemList.toString());
            Boolean b = false;
            for (int i = 0; i < outList.size(); i++) {

                // inList 按照type排序 46:SHORT:(1,0,0,0,0):0.09500000000000001:0.0:0.0:0.0:0.0
                // 解决方法: ①实现了Comparator接口的类，并重写抽象方法compare（Student o1, Student o2）
                //          ②String.contain()

                String type = outList.get(i).split(":")[1];
                System.out.println("type:"+type);
                ArrayList<String> inListRe = rearrange(type, inList);

                for (int j = 0; j < inListRe.size(); j++) {

                    // 此处逻辑适合单进单出
                    // 根据原集合 out解 in解 三者的关系进行，属性比例要求的判断  同时不破坏了原有题型的约束
                    b = propCheck(bachItemList,outList.get(i),inListRe.get(j));

                    if(b){
                        // 删除out解，添加in解
                        for (int k = 0; k < bachItemList.size(); k++) {
                            if (bachItemList.get(i).equals(outList.get(i))){
                                bachItemList.set(i,inListRe.get(j));
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

            //此处添加替补逻辑
            //如果没能找到合适的解,则择优录取
            //随机替换一个(前提是不影响 其他属性，故最好完全互补替代 type attribute)  前提是 题库中已有互补的解

            if(!b) {
                //遍历out解 需考虑终止条件
                for (int i = 0; i < outList.size(); i++) {


                    /*
                     * out解信息 此处不能和 单独的多和少一样做简单替换处理 其需要做一定的分类讨论
                     * 利用af 信息，如果=1 则p=0    如果=-1 则p=1
                     *
                     */

                    //保证原有信息不做变更
                    String t1 = " and type = '" + outList.get(i).split(":")[1] +"'";


                    //目前空缺解信息  af1==-1 af2  af3 af4 af5
                    //取出-1的解,使用集合接收,并直接转换为 p1 p2 p3 p4 p5
                    ArrayList<String> lessTemp = new ArrayList<>();
                    ArrayList<String> moreTemp = new ArrayList<>();
                    if(af1==-1){
                        lessTemp.add("p1");
                    }else if(af1==1) {
                        moreTemp.add("p1");
                    }
                    if(af2==-1){
                        lessTemp.add("p2");
                    }else if(af2==1){
                        moreTemp.add("p2");
                    }
                    if(af3==-1){
                        lessTemp.add("p3");
                    }else if(af3==1){
                        moreTemp.add("p3");
                    }
                    if(af4==-1){
                        lessTemp.add("p4");
                    }else if(af4==1){
                        moreTemp.add("p4");
                    }
                    if(af5==-1){
                        lessTemp.add("p5");
                    }else if(af5==1){
                        moreTemp.add("p5");
                    }

                    //ArrayList<String> 转 数组
                    String[] lessArray = new String[lessTemp.size()];
                    for (int j = 0; j < lessTemp.size(); j++) {
                        lessArray[j] = lessTemp.get(j);
                    }

                    String[] moreArray = new String[moreTemp.size()];
                    for (int j = 0; j < moreTemp.size(); j++) {
                        moreArray[j] = moreTemp.get(j);
                    }


                    //递归遍历 （先取全解，再取部分解，最后取一个解）
                    Set<Set<String>> lessSet = new KLUtils().getSubCollection(lessArray);
                    Set<Set<String>> moreSet = new KLUtils().getSubCollection(moreArray);

                    //Set 转 ArrayList 需考虑重新定义排序方法
                    /*
                    for (Set<String> tmp : lessSet) {
                        lessFinally.add(tmp);
                    }
                    */
                    ArrayList<Set<String>> lessFinally = new ArrayList<>(lessSet);
                    ArrayList<Set<String>> moreFinally = new ArrayList<>(moreSet);

                    //倒序取出  [p1]    [p1, p2]、[p2]、[p1]
                    for (int i1 = lessFinally.size() -1 ; i1 >= 1; i1--) {
                        System.out.println(lessFinally.get(i1));
                        String tmp1 = lessFinally.get(i1).toString().substring(1);
                        String tmp2 = tmp1.replaceAll(","," = 1 and ");
                        String tmp3 = tmp2.replace("]"," = 1 ");
                        System.out.println(tmp3);

                        for (int j = moreFinally.size()-1; j >= 1; j--) {
                            System.out.println(moreFinally.get(j));
                            String tmp4 = moreFinally.get(j).toString().substring(1);
                            String tmp5 = tmp4.replaceAll(","," = 0 and ");
                            String tmp6 = tmp5.replace("]"," = 0 ");
                            System.out.println(tmp6);



                            //p1=1 p5=0 的条件已经完成
                            String sqlFinally = tmp3 + " and "+ tmp6 + t1;
                            System.out.println(sqlFinally);

                            ArrayList<String> arrayList = jdbcUtils.selectBySql(sqlFinally);
                            if(arrayList.size()>0){

                                System.out.println("out解："+outList.get(i));
                                System.out.println("in解："+arrayList.get(0));

                                // 删除out解，添加in解
                                for (int k = 0; k < outList.size(); k++) {
                                    if (outList.get(i).equals(outList.get(i))){
                                        outList.set(i,arrayList.get(0));
                                    }
                                }
                                System.out.println("找到合适解,退出循环");
                                b = true;
                                break;
                            }else {
                                System.out.println("未找到合适解,继续递归查找");
                            }

                        }
                        if(b){
                            break;
                        }
                    }
                    if(b){
                        break;
                    }

                }
            }


            System.out.println("校验后的集合:"+bachItemList.toString());

        }


//*************************  3.2 resultLess有  resultMore无值   *************************
        /*
         * FIXME  1.resultMore resultLess 的求集合时，应该将题型纳入考虑
         *          查看其影响逻辑   唯一的影响在于  考虑的因素过多，可能导致resultLess解集变少,甚至导致交集为空
         *          目前交集一般为3~6个解  如果将type纳入，解集为0~2
         *          故 取交集并集时 暂时不考虑题型比例
         * FIXME  2.propCheck() 也应该将题型纳入考虑
         *          这个是应该进行考虑的，因为可选项较多，一般是能满足要求的，若无法满足，则通过下层处理
         *
         */

        if(resultMore.size()==0 && resultLess.size()>0){
            System.out.println("本套试卷 属性比例不足的情况。");
            System.out.println(resultLess);

            //SQL 均用交集应该没影响  需进一步校验
            StringBuilder sb = new StringBuilder();
            if(af1>0){
                sb.append(" p1=0 and ");
            }else if (af1<0){
                sb.append(" p1=1 and ");
            }

            if(af2>0){
                sb.append(" p2=0 and ");
            }else if (af2<0){
                sb.append(" p2=1 and ");
            }

            if(af3>0){
                sb.append(" p3=0 and ");
            }else if (af3<0){
                sb.append(" p3=1 and ");
            }

            if(af4>0){
                sb.append(" p4=0 and ");
            }else if (af4<0){
                sb.append(" p4=1 and ");
            }

            if(af5>0){
                sb.append(" p5=0 and ");
            }else if (af5<0){
                sb.append(" p5=1 and ");
            }

            String sql = sb.toString().substring(0, sb.toString().length() - 4);
            //获取新解的集合
            ArrayList<String> inList = jdbcUtils.selectBySql(sql);

            // ori解集  out解集  in解集 的关系
            // 原始解集 - out解 + in解 = 新解(拿新解去再次校验)
            // 循环的逻辑：外层out解，内层in解，不断的调用属性比例校验方法，如满足要求则退出，不满足则继续遍历。最后的终止条件是 遍历终止，然后随机替换一个(前提是不影响 其他属性，故最好完全互补替代)

            List<String> outList = new ArrayList<>(resultLess);
            System.out.println("校验前的集合:"+bachItemList.toString());
            Boolean b = false;
            for (int i = 0; i < outList.size(); i++) {

                // inList 按照type排序 46:SHORT:(1,0,0,0,0):0.09500000000000001:0.0:0.0:0.0:0.0
                // 解决方法: ①实现了Comparator接口的类，并重写抽象方法compare（Student o1, Student o2）
                //          ②String.contain()

                String type = outList.get(i).split(":")[1];
                System.out.println("type:"+type);
                ArrayList<String> inListRe = rearrange(type, inList);


                for (int j = 0; j < inListRe.size(); j++) {

                    // 此处逻辑适合属性只缺少一个单位的情况（23*0.2=4.6 23*0.1=2.3  所以均无法满足）
                    // 根据原集合 out解 in解 三者的关系进行，属性比例要求的判断  同时尽量不破坏了原有题型的约束
                    b = propCheck(bachItemList,outList.get(i),inListRe.get(j));

                    if(b){
                        // 删除out解，添加in解
                        for (int k = 0; k < bachItemList.size(); k++) {
                            if (bachItemList.get(i).equals(outList.get(i))){
                                bachItemList.set(i,inListRe.get(j));
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

            //如果没能找到合适的解,则择优录取
            //随机替换一个(前提是不影响 其他属性，故最好完全互补替代 type attribute)
            //前提是 题库中已有互补的解
            if(!b) {
                //遍历out解 需考虑终止条件
                for (int i = 0; i < outList.size(); i++) {

                    //out解信息
                    String t1 = " and type = '" + outList.get(i).split(":")[1] +"'";
                    String[] arr = outList.get(i).split(":")[2].split(",");
                    String a1 = "0".equals(arr[0].substring(1,2))?"": " and p1 = 1 ";
                    String a2 = "0".equals(arr[1])?"": " and p2 = 1 ";
                    String a3 = "0".equals(arr[2])?"": " and p3 = 1 ";
                    String a4 = "0".equals(arr[3])?"": " and p4 = 1 ";
                    String a5 = "0".equals(arr[4].substring(0,1))?"": " and p5 = 1 ";

                    //目前空缺解信息  af1==-1 af2  af3 af4 af5
                    //取出-1的解,使用集合接收,并直接转换为 p1 p2 p3 p4 p5
                    ArrayList<String> lessTemp = new ArrayList<>();
                    if(af1==-1){
                        lessTemp.add("p1");
                    }
                    if(af2==-1){
                        lessTemp.add("p2");
                    }
                    if(af3==-1){
                        lessTemp.add("p3");
                    }
                    if(af4==-1){
                        lessTemp.add("p4");
                    }
                    if(af5==-1){
                        lessTemp.add("p5");
                    }

                    //ArrayList<String> 转 数组
                    String[] lessArray = new String[lessTemp.size()];
                    for (int j = 0; j < lessTemp.size(); j++) {
                        lessArray[j] = lessTemp.get(j);
                    }

                    //递归遍历 （先取全解，再取部分解，最后取一个解）
                    Set<Set<String>> lessSet = new KLUtils().getSubCollection(lessArray);

                    //Set 转 ArrayList 需考虑重新定义排序方法
                    /*
                    for (Set<String> tmp : lessSet) {
                        lessFinally.add(tmp);
                    }
                    */
                    ArrayList<Set<String>> lessFinally = new ArrayList<>(lessSet);

                    //倒序取出  [p1]    [p1, p2]、[p2]、[p1]
                    for (int i1 = lessFinally.size() -1 ; i1 >= 1; i1--) {
                        System.out.println(lessFinally.get(i1));
                        String tmp1 = lessFinally.get(i1).toString().substring(1);
                        String tmp2=tmp1.replaceAll(","," = 1 and ");
                        String tmp3=tmp2.replace("]"," = 1 ");
                        System.out.println(tmp3);

                        //p1=1 的条件已经完成，现在再拼接 p5=0
                        String sqlFinally = tmp3 + a1 + a2 + a3 + a4 + a5 + t1;
                        System.out.println(sqlFinally);

                        ArrayList<String> arrayList = jdbcUtils.selectBySql(sqlFinally);
                        if(arrayList.size()>0){

                            System.out.println("out解："+outList.get(i));
                            System.out.println("in解："+arrayList.get(0));

                            // 删除out解，添加in解
                            for (int k = 0; k < outList.size(); k++) {
                                if (outList.get(i).equals(outList.get(i))){
                                    outList.set(i,arrayList.get(0));
                                }
                            }
                            System.out.println("找到合适解,退出循环");
                            b = true;
                            break;
                        }else {
                            System.out.println("未找到合适解,继续递归查找");
                        }
                    }
                    if(b){
                        break;
                    }
                }
            }

            System.out.println("校验后的集合:"+bachItemList.toString());

        }


//*************************  3.3 resultLess无  resultMore有值   *************************


        if(resultMore.size()>0 && resultLess.size()==0){
            System.out.println("本套试卷 属性比例过高的情况。");
            System.out.println(resultLess);

            //SQL 均用交集应该没影响  需进一步校验
            StringBuilder sb = new StringBuilder();
            if(af1>0){
                sb.append(" p1=0 and ");
            }else if (af1<0){
                sb.append(" p1=1 and ");
            }

            if(af2>0){
                sb.append(" p2=0 and ");
            }else if (af2<0){
                sb.append(" p2=1 and ");
            }

            if(af3>0){
                sb.append(" p3=0 and ");
            }else if (af3<0){
                sb.append(" p3=1 and ");
            }

            if(af4>0){
                sb.append(" p4=0 and ");
            }else if (af4<0){
                sb.append(" p4=1 and ");
            }

            if(af5>0){
                sb.append(" p5=0 and ");
            }else if (af5<0){
                sb.append(" p5=1 and ");
            }

            String sql = sb.toString().substring(0, sb.toString().length() - 4);
            //获取新解的集合
            ArrayList<String> inList = jdbcUtils.selectBySql(sql);

            // ori解集  out解集  in解集 的关系
            // 原始解集 - out解 + in解 = 新解(拿新解去再次校验)
            // 循环的逻辑：外层out解，内层in解，不断的调用属性比例校验方法，如满足要求则退出，不满足则继续遍历。最后的终止条件是 遍历终止，然后随机替换一个(前提是不影响 其他属性，故最好完全互补替代)

            List<String> outList = new ArrayList<>(resultLess);
            System.out.println("校验前的集合:"+bachItemList.toString());
            Boolean b = false;
            for (int i = 0; i < outList.size(); i++) {

                // inList 按照type排序 46:SHORT:(1,0,0,0,0):0.09500000000000001:0.0:0.0:0.0:0.0
                // 解决方法: ①实现了Comparator接口的类，并重写抽象方法compare（Student o1, Student o2）
                //          ②String.contain()

                String type = outList.get(i).split(":")[1];
                System.out.println("type:"+type);
                ArrayList<String> inListRe = rearrange(type, inList);


                for (int j = 0; j < inListRe.size(); j++) {

                    // 此处逻辑适合属性只缺少一个单位的情况（23*0.2=4.6 23*0.1=2.3  所以均无法满足）
                    // 根据原集合 out解 in解 三者的关系进行，属性比例要求的判断  同时尽量不破坏了原有题型的约束
                    b = propCheck(bachItemList,outList.get(i),inListRe.get(j));

                    if(b){
                        // 删除out解，添加in解
                        for (int k = 0; k < bachItemList.size(); k++) {
                            if (bachItemList.get(i).equals(outList.get(i))){
                                bachItemList.set(i,inListRe.get(j));
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

            //如果没能找到合适的解,则择优录取
            //随机替换一个(前提是不影响 其他属性，故最好完全互补替代 type attribute)
            //前提是 题库中已有互补的解
            if(!b) {
                //遍历out解 需考虑终止条件
                for (int i = 0; i < outList.size(); i++) {

                    //out解信息
                    String t1 = " and type = '" + outList.get(i).split(":")[1] +"'";
                    String[] arr = outList.get(i).split(":")[2].split(",");
                    String a1 = "1".equals(arr[0].substring(1,2))?"": " and p1 = 0 ";
                    String a2 = "1".equals(arr[1])?"": " and p2 = 0 ";
                    String a3 = "1".equals(arr[2])?"": " and p3 = 0 ";
                    String a4 = "1".equals(arr[3])?"": " and p4 = 0 ";
                    String a5 = "1".equals(arr[4].substring(0,1))?"": " and p5 = 0 ";

                    //目前空缺解信息  af1==-1 af2  af3 af4 af5
                    //取出-1的解,使用集合接收,并直接转换为 p1 p2 p3 p4 p5
                    ArrayList<String> moreTemp = new ArrayList<>();
                    if(af1==1){
                        moreTemp.add("p1");
                    }
                    if(af2==1){
                        moreTemp.add("p2");
                    }
                    if(af3==1){
                        moreTemp.add("p3");
                    }
                    if(af4==1){
                        moreTemp.add("p4");
                    }
                    if(af5==1){
                        moreTemp.add("p5");
                    }

                    //ArrayList<String> 转 数组
                    String[] moreArray = new String[moreTemp.size()];
                    for (int j = 0; j < moreTemp.size(); j++) {
                        moreArray[j] = moreTemp.get(j);
                    }

                    //递归遍历 （先取全解，再取部分解，最后取一个解）
                    Set<Set<String>> moreSet = new KLUtils().getSubCollection(moreArray);

                    //Set 转 ArrayList 需考虑重新定义排序方法
                    /*
                    for (Set<String> tmp : lessSet) {
                        lessFinally.add(tmp);
                    }
                    */
                    ArrayList<Set<String>> moreFinally = new ArrayList<>(moreSet);

                    //倒序取出  [p1]    [p1, p2]、[p2]、[p1]
                    for (int i1 = moreFinally.size() -1 ; i1 >= 1; i1--) {
                        System.out.println(moreFinally.get(i1));
                        String tmp1 = moreFinally.get(i1).toString().substring(1);
                        String tmp2=tmp1.replaceAll(","," = 0 and ");
                        String tmp3=tmp2.replace("]"," = 0 ");
                        System.out.println(tmp3);

                        //p1=0 的条件已经完成，现在再拼接 p5=1
                        String sqlFinally = tmp3 + a1 + a2 + a3 + a4 + a5 + t1;
                        System.out.println(sqlFinally);

                        ArrayList<String> arrayList = jdbcUtils.selectBySql(sqlFinally);
                        if(arrayList.size()>0){

                            System.out.println("out解："+outList.get(i));
                            System.out.println("in解："+arrayList.get(0));

                            // 删除out解，添加in解
                            for (int k = 0; k < outList.size(); k++) {
                                if (outList.get(i).equals(outList.get(i))){
                                    outList.set(i,arrayList.get(0));
                                }
                            }
                            System.out.println("找到合适解,退出循环");
                            b = true;
                            break;
                        }else {
                            System.out.println("未找到合适解,继续递归查找");
                        }
                    }
                    if(b){
                        break;
                    }
                }
            }

            System.out.println("校验后的集合:"+bachItemList.toString());

        }


        return bachItemList;
    }




    /**
     * 根据原集合 out解 in解 三者的关系进行，属性比例要求的判断  同时尽量不破坏了原有题型的约束
     * 目前只适合单进单出
     * 46:SHORT:(1,0,0,0,0):0.09500000000000001:0.0:0.0:0.0:0.0
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
        System.out.println("tmp解(bachItemList) :"+bachItemList.toString());

        //开始校验是否符合属性比例要求
        //ArrayList<String> 转 hashSet<String>
        HashSet<String> itemSet = new HashSet<>(bachItemList);

        //属性个数
        int attributeNum1  = 0;
        int attributeNum2  = 0;
        int attributeNum3  = 0;
        int attributeNum4  = 0;
        int attributeNum5  = 0;

        //此次迭代各个属性的数目
        for (String tmp:itemSet) {
            System.out.println(tmp);

            //计算每种题型个数
            if("1".equals(tmp.split(":")[2].substring(1,2))){
                attributeNum1 += 1;
            }
            if("1".equals(tmp.split(":")[2].substring(3,4))){
                attributeNum2 += 1;
            }
            if("1".equals(tmp.split(":")[2].substring(5,6))){
                attributeNum3 += 1;
            }
            if("1".equals(tmp.split(":")[2].substring(7,8))){
                attributeNum4 += 1;
            }
            if("1".equals(tmp.split(":")[2].substring(9,10))){
                attributeNum5 += 1;
            }
        }
        System.out.println("AttributeRatio1: "+attributeNum1+"\tAttributeRatio2: "+attributeNum2+"\tAttributeRatio3: "+attributeNum3+"\tAttributeRatio4: "+attributeNum4+"\tAttributeRatio5: "+attributeNum5);

        //属性比例
        double attributeRatio1 = attributeNum1/23.0;
        double attributeRatio2 = attributeNum2/23.0;
        double attributeRatio3 = attributeNum3/23.0;
        double attributeRatio4 = attributeNum4/23.0;
        double attributeRatio5 = attributeNum5/23.0;


        int af1 ;
        if(attributeRatio1>=0.2 && attributeRatio1<=0.4){
            af1 = 0;
        }else if(attributeRatio1<0.2){
            af1 = -1;
        }else {
            af1 = 1;
        }

        int af2 ;
        if(attributeRatio2>=0.2 && attributeRatio2<=0.4){
            af2 = 0;
        }else if(attributeRatio2<0.2){
            af2 = -1;
        }else {
            af2 = 1;
        }

        int af3 ;
        if(attributeRatio3>=0.1 && attributeRatio3<=0.3){
            af3 = 0;
        }else if(attributeRatio3<0.1){
            af3 = -1;
        }else {
            af3 = 1;
        }

        int af4 ;
        if(attributeRatio4>=0.1 && attributeRatio4<=0.3){
            af4 = 0;
        }else if(attributeRatio4<0.1){
            af4 = -1;
        }else {
            af4 = 1;
        }

        int af5 ;
        if(attributeRatio5>=0.1 && attributeRatio5<=0.3){
            af5 = 0;
        }else if(attributeRatio5<0.1){
            af5 = -1;
        }else {
            af5 = 1;
        }
        //typeFlag 是否要进一步考虑 具体多了多少数量的情况
        String attributeFlag = "("+af1+","+af2+","+af3+","+af4+","+af5+")";
        System.out.println("目前属性占比情况： attributeFlag:"+attributeFlag);

        //是否要进一步考虑 具体多了多少数量的情况
        if(attributeFlag.contains("1")){
            return false ;
        }

        return true;
    }


    /**
     * 交叉  不计算适应度值
     *       长度，属性类型，属性比例
     */
    private void crossCover(Papers papers) throws SQLException {

        System.out.println("================== cross ==================");

        //  交叉的单位:  试题
        //  交叉的结果： 长度可能因为重复而导致减少，题型和属性比例 进一步用修补算子进行修补 (适应度上)
        //  检验：为什么交叉后会产生这样的结果 （1,58,83,150,244,292）  临界值的问题，出在变异的部分，已修复
        //                          (34, 165, 248, 282, 76, 92, 126)
        //                          random.nextInt(100)  指生成一个介于[0,n)的int值
        //  初始化时,保证了试题id的升序排序
        int point = paperGenetic[1].length;
        for (int i = 0; i < paperGenetic.length-1; i++) {
            if (Math.random() < papers.getPc()) {
                //单点交叉
                String [] temp = new String[point];
                int a = new Random().nextInt(point);

                for (int j = 0; j < a; j++) {
                    temp[j] = paperGenetic[i][j];
                }

                for (int j = a; j < point; j++) {
                    temp[j] = paperGenetic[i+1][j];
                }
                // 放在内存执行,每执行一次pc 则校验一次
                // 判断size,执行修补算子  只改变了tem1
                System.out.println("==== 校验temp的长度 ===="+temp.length+ " "+ Arrays.asList(temp).toString());
                correct(i,temp);
            }
        }
    }



    /**
     * 执行修补操作(交叉变异均可能导致数量，题型，属性发生了变化)
     *      步骤：
     *          (1)判断size,按照题型新增n道题目，or 按照 题型和属性进行筛选
     *                  建议后者,可以省去了不必要的查询操作。但复杂度可能要高一些了
     *                  这次使用前者，因为可以套用之前的逻辑
     *          (2)题型和属性是同时进行判断 or 单独判断，然后考虑权重
     *                  建议前者，可以不用考虑多次，直接对适应度值进行一次计算即可
     *
     *
     * 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3] 应用[0.1,0.3]
     * 属性比例 第1属性[0.2,0.4]   第2属性[0.2,0.4]   第3属性[0.1,0.3]  第4属性[0.1,0.3]  第5属性[0.1,0.3]
     *
     */
    private void correct(int i, String[] temp1) throws SQLException {

        System.out.println("第 "+i+" 题,开始交叉后校验 ..... ");

        // TODO  长度 题型比例校验
        correctType(i, temp1);

        //TODO  属性比例校验
        ArrayList<String> bachItemList = new ArrayList();
        Collections.addAll(bachItemList, paperGenetic[i]);
        //46:SHORT:(1,0,0,0,0):0.09500000000000001:0.0:0.0:0.0:0.0
        correctAttribute(bachItemList);




    }


    /**
     * 变异  不计算适应度值
     *      长度，属性类型，属性比例
     */
    public  void mutate(Papers papers) throws SQLException {

        System.out.println("================== mutate ==================");
        //JDBCUtils4 jdbcUtils = new JDBCUtils4();
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
        //题型数量
        int  typeNum = paperGenetic[0].length;

        //抽取id,封装成int[]
        int[] sortArray = new int[typeNum];
        for (int i = 0; i < temp1.length; i++) {
            sortArray[i] = Integer.parseInt(temp1[i].split(":")[0]);
        }
        Arrays.sort(sortArray);
        // 以下for循环打印id 可以省略
        for (int i = 0; i < sortArray.length; i++) {
            System.out.print(sortArray[i]+" ");
        }

        //根据id的位置，映射，重新排序 tmp1
        String[] temp2 = new String[typeNum];
        for (int i = 0; i < sortArray.length; i++) {
            int index = sortArray[i];
            for (String ts : temp1) {
                if(Integer.parseInt(ts.split(":")[0]) == index){
                    temp2[i] = ts;
                }
            }
        }
        System.out.println("试题id：");
        for (int i = 0; i < temp2.length; i++) {
            System.out.print(temp2[i]+" ");
        }
        System.out.println();
        return  temp2;
    }


    /**
     * 选择: 根据轮盘赌选择适应度高的个体
     * 将适应度值打印出来看一下，方便后续比较
     *     ①如何去计算适应度：以试卷为单位，min*exp^1
     *     ②初始化时直接将adi值计算，并保存在数据库中  这种方式是最快的，但题型和属性的选取是随机的，且包括交叉变异，故不适合
     *     如果需要，可以将最终的结果的汇总到数据库。
     *
     *     仅仅依靠选择和轮盘赌，其变化是很小的，故需要更大的外部作用(交叉+变异）
     *
     */
    public  void  selection(){

        System.out.println("====================== select ======================");

        //10套试卷   10道题目
        int paperSize = paperGenetic.length;

        //轮盘赌 累加百分比
        double[] fitPie = new double[paperSize];


        //每套试卷的适应度占比  min*exp^1
        double[] fitPro = getFitness(paperSize);

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
        //FIXME 同一套试卷可能会被选取两次   需要做修改  最后导致十套试卷一模一样
        //FIXME       如果根据id去重，将导致轮盘赌失去了本身的意义
        //            可以减少paperSize的大小,达到减少重复率过高的情况，但需保证paperGenetic=newPaperGenetic;时不报错
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
        System.out.print("新种群");

    }

    /**
     * 打印数组double[]
     *
     */
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
    private double[] getFitness(int paperSize){

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

            //ArrayList -> []
            String [] itemList = paperGenetic[i];
            for (int j = 0; j < itemList.length; j++) {

                String[] splits = itemList[j].split(":");
                adi1r = adi1r + Double.parseDouble(splits[3]);
                adi2r = adi2r + Double.parseDouble(splits[4]);
                adi3r = adi3r + Double.parseDouble(splits[5]);
                adi4r = adi4r + Double.parseDouble(splits[6]);
                adi5r = adi5r + Double.parseDouble(splits[7]);

            }

            // 方案  进行乘以一个exp 来进行适应度值的降低    高等数学里以自然常数e为底的指数函数
            //       System.out.printf("exp(%.3f) 为 %.3f%n", -2.7183, Math.exp(-2.7183));
            //       - 表示取倒数  1/(2.7183 * 2.7183)  =  0.135
            // 适应度值需考虑   题型和属性比例
            // 原有的adi min小于1是否有影响（没影响，只是额外新增一个惩罚参数） 但因为实在太小了，所以*100
            // 计算每套试卷的惩罚系数  实际比例 vs 期望比例
            // 5个属性，10个题目，23次选择  3*1 + 3*2 + 2*3 + 2*4 = 23

            // 按照比例计算 可以减少后续的修改
            //     题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3]  应用[0.1,0.3]
            //     属性比例 第1属性[0.2,0.4]   第2属性[0.2,0.4]   第3属性[0.1,0.3]  第4属性[0.1,0.3]  第5属性[0.1,0.3]
            //     [4.6,9.2]  [2.3,6.9]


            // 题型的曝光度
            //题型个数
            String [] expList = paperGenetic[i];
            int typeChose  = 0;
            int typeFill   = 0;
            int typeShort  = 0;
            int typeCompre = 0;


            //此次迭代各个题型的数目
            for (String s:expList) {
                //System.out.println(s);

                //计算每种题型个数
                if(TYPE.CHOSE.toString().equals(s.split(":")[1])){
                    typeChose += 1;
                }
                if(TYPE.FILL.toString().equals(s.split(":")[1])){
                    typeFill += 1;
                }
                if(TYPE.SHORT.toString().equals(s.split(":")[1])){
                    typeShort += 1;
                }
                if(TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])){
                    typeCompre += 1;
                }
            }
            System.out.println("目前题型数量情况： typeChose:"+typeChose+" typeFill:"+typeFill+" typeShort:"+typeShort+" typeCompre:"+typeCompre);

            // 题型比例
            double typeChoseRation  =  typeChose/10.0;
            double typeFileRation   =  typeFill/10.0;
            double typeShortRation  =  typeShort/10.0;
            double typeCompreRation =  typeCompre/10.0;

            // 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3]  应用[0.1,0.3]
            // 先判断是否在范围内，在的话，为0，不在的话，然后进一步和上下限取差值，绝对值
            double td1 ;
            if(typeChoseRation>=0.2 && typeChoseRation<0.4){
                td1 = 0;
            }else if(typeChoseRation<0.2){
                td1 =  Math.abs(0.2 - typeChoseRation);
            }else {
                td1 =  Math.abs(typeChoseRation - 0.4);
            }

            double td2 ;
            if(typeFileRation>=0.2 && typeFileRation<0.4){
                td2 = 0;
            }else if(typeFileRation<0.2){
                td2 =  Math.abs(0.2 - typeFileRation);
            }else {
                td2 =  Math.abs(typeFileRation - 0.4);
            }

            double td3 ;
            if(typeShortRation>=0.1 && typeShortRation<0.3){
                td3 = 0;
            }else if(typeShortRation<0.1){
                td3 =  Math.abs(0.1 - typeShortRation);
            }else {
                td3 =  Math.abs(typeShortRation - 0.3);
            }

            double td4 ;
            if(typeCompreRation>=0.1 && typeCompreRation<0.3){
                td4 = 0;
            }else if(typeCompreRation<0.1){
                td4 =  Math.abs(0.1 - typeCompreRation);
            }else {
                td4 =  Math.abs(typeCompreRation - 0.3);
            }



            // 各个属性的曝光度
            int exp1 = 0;
            int exp2 = 0;
            int exp3 = 0;
            int exp4 = 0;
            int exp5 = 0;

            for (int j = 0; j < expList.length; j++) {
                String[] splits = expList[j].split(":");
                exp1 = exp1 + Integer.parseInt(splits[2].split(",")[0].substring(1,2));
                exp2 = exp2 + Integer.parseInt(splits[2].split(",")[1]);
                exp3 = exp3 + Integer.parseInt(splits[2].split(",")[2]);
                exp4 = exp4 + Integer.parseInt(splits[2].split(",")[3]);
                exp5 = exp5 + Integer.parseInt(splits[2].split(",")[4].substring(0,1));
            }
            System.out.println("目前属性数量情况： exp1:"+exp1+" exp2:"+exp2+" exp3:"+exp3+" exp4:"+exp4+" exp5:"+exp5);

            // 属性比例 第1属性[0.2,0.4]   第2属性[0.2,0.4]   第3属性[0.1,0.3]  第4属性[0.1,0.3]  第5属性[0.1,0.3]
            //先判断是否在范围内，在的话，为0，不在的话，然后进一步和上下限取差值，绝对值
            double ed1 ;
            double edx1 = exp1/23.0;
            if(edx1>=0.2 && edx1<0.4){
                ed1 = 0;
            }else if(edx1<0.2){
                ed1 =  Math.abs(0.2 - edx1);
            }else {
                ed1 =  Math.abs(edx1 - 0.4);
            }

            double ed2 ;
            double edx2 = exp2/23.0;
            if(edx2>=0.2 && edx2<0.4){
                ed2 = 0;
            }else if(edx2<0.2){
                ed2 =  Math.abs(0.2 - edx2);
            }else {
                ed2 =  Math.abs(edx2 - 0.4);
            }

            double ed3 ;
            double edx3 = exp3/23.0;
            if(edx3>=0.1 && edx3<0.3){
                ed3 = 0;
            }else if(edx3<0.1){
                ed3 =  Math.abs(0.1 - edx3);
            }else {
                ed3 =  Math.abs(edx3 - 0.3);
            }

            double ed4 ;
            double edx4 = exp4/23.0;
            if(edx4>=0.1 && edx4<0.3){
                ed4 = 0;
            }else if(edx4<0.1){
                ed4 =  Math.abs(0.1 - edx4);
            }else {
                ed4 =  Math.abs(edx4 - 0.3);
            }

            double ed5 ;
            double edx5 = exp5/23.0;
            if(edx5>=0.1 && edx5<0.3){
                ed5 = 0;
            }else if(edx5<0.1){
                ed5 =  Math.abs(0.1 - edx5);
            }else {
                ed5 =  Math.abs(edx5 - 0.3);
            }

            System.out.println("目前题型和属性超额情况： td1:"+td1+" td2:"+td2+" td3:"+td3+" td4:"+td4 + "ed1:"+ed1+" ed2:"+ed2+" ed3:"+ed3+" ed4:"+ed4+" ed5:"+ed5);

            // 惩罚个数  只有比例不符合要求时才惩罚，故不会有太大的影响
            double expNum = -(td1 + td2 + td3 + td4 + ed1 + ed2 + ed3 + ed4 + ed5);

            //System.out.printf("exp(%.3f) 为 %.3f%n", expNum, Math.exp(expNum));


            //均值 和 最小值
            double avgrum = (adi1r + adi2r + adi3r + adi4r + adi5r)/5 ;
            double minrum = Math.min(Math.min(Math.min(Math.min(adi1r,adi2r),adi3r),adi4r),adi5r) * 100 ;

            System.out.println("minrum: "+minrum);

            //适应度值  (新增 惩罚系数)
            minrum = minrum * Math.exp(expNum);
            //个体和总和
            fitTmp[i] = minrum ;
            fitSum = fitSum + minrum ;


// ==================== elitistStrategy  ====================
            //  全局最优：根据适应度判断
            //       ①全局最优和局部最优的比较：应该不能直接比较和替换，评判标准不一样
            //       ②一共10个体，为什么进行了1500 次迭代，为什么不会出现大面积重复的情况  替换单位是试卷
            //            因为只是进行了计算，而没有进行替换操作 elitistStrategy()
            //  全局最优 和 局部最优 的个数关系
            //  导致的影响是什么： ①如果全局最优只有一个值,那么是否会导致过于相似  only 6道题
            //                  ②如果全局最优只有多个值,复杂性增大
            //  此处使用全局最优一个解

            //  局部最优和全局最优  都只是一个容器
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
            }
        }

        System.out.println("全局最优："+GlobalOptimal);

        for (int i = 0; i < paperSize; i++) {
            //  各自的比例
            fitPro[i] = fitTmp[i] / fitSum;
        }


        return  fitPro;
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
     *          计算时机可能要做改变，因为中间存在 交叉变异的情况，适应度值发生了一定程度的变化
     *      2.用全局最优替换掉局部最优
     *          局部最优和全局最优,都只是一个容器  需要挨个和每套试卷进行比较替换
     */
    private void  elitistStrategy(){

        System.out.println("================== elitistStrategy ==================");
        //getFitness(paperGenetic.length);

        // 全局最优解替换掉局部最优解
        for (int i = 0; i < LocalOptimal.length; i++) {
            LocalOptimal[i] = GlobalOptimal;
        }

    }


    /**
     *   逻辑判断
     *        1.id不能重复
     *        2.题型比例 + 属性比例   构造是否会导致那些具有多个属性值的试题  适应度急剧下降
     *        3.构造 -> 轮盘赌
     *        [8:CHOSE:(1,0,0,0,0), 3:CHOSE:(0,0,0,1,0)]
     */
    private int roulette(HashSet<String> itemSet)  {

        System.out.println("====================== 构造选题 ======================");

        //轮盘赌 累加百分比
        double[] fitPie = new double[bankList.size()];

        //计算每道试题的适应度占比   1*0.5*0.8
        double[] fitnessArray = getRouletteFitness(itemSet);

        //id去重操作
        HashSet<Integer> idSet = new HashSet<>();

        //通过迭代器遍历HashSet
        Iterator<String> it = itemSet.iterator();
        while(it.hasNext()) {

            idSet.add(Integer.valueOf(it.next().split(":")[0]));
        }

        //累加初始值
        double accumulate = 0;

        //试题占总试题的适应度累加百分比
        for (int i = 0; i < bankList.size(); i++) {
            fitPie[i] = accumulate + fitnessArray[i];
            accumulate += fitnessArray[i];
            //System.out.println("试题"+ i+"占目前总试题的适应度累加百分比： "+fitPie[i]);
        }

        //累加的概率为1   数组下标从0开始
        fitPie[310-1] = 1;

        //随机生成的random概率值  [0,1)
        double randomProbability = Math.random();

        //打印出random概率值   有点意思，这样将导致最后一条永远无法选取到
        //select  * from  adi20210528 ra order by rand() 随机排序 ;
        //轮盘赌 越大的适应度，其叠加时增长越快，即有更大的概率被选中
        int answerSelectId = 0;
        int i = 0;

        while (i < bankList.size() && randomProbability > fitPie[i]){
            //id 去重
            if(idSet.contains(i)){
                i += 1;
            }else{
                i ++;
                answerSelectId   = i;
            }
        }

        //输出
        System.out.print("轮盘赌选取的ID:"+ answerSelectId);

        return answerSelectId;

    }




    /**
     * 1.计算每道试题的适应度值比例
     * 2.每道题的概率为 1*penalty^n,总概率为310道题叠加
     *      2.1  初始化的时候将全局的310题查询出来,（id,type,pattern）
     *      2.2  求出每道试题的概率 1 * 惩罚系数
     *      2.3  求出每道试题的适应度占比
     *  题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3] 应用[0.1,0.3]
     *  属性比例 第1属性[0.2,0.4]   第2属性[0.2,0.4]   第3属性[0.1,0.3]  第4属性[0.1,0.3]  第5属性[0.1,0.3]
     *  3*1 + 3*2 + 2*3 + 2*4 = 23
     *  [4.6,9.2]  [2.3,6.9]
     *
     */
    private double[] getRouletteFitness(HashSet<String> itemSet)  {

        // 所有试题的适应度总和
        double fitSum = 0.0;

        // 每道试题的适应度值
        double[] fitTmp = new double[bankList.size()];

        // 每道试题的适应度占比   疑问:1/310 会很小,random() 这样产生的值是否符合要求
        double[] fitPro = new double[bankList.size()];


        // 是否会因为数据库属性排列的规则，导致随机选取的题目不具有代表性  50~150均为填空题,这样的话，题目就算概率受到惩罚系数的影响，因为基数大导致影响波动变小
        // 解决方案:题目顺序打乱（修改数据库的属性排序、查询返回的结果进行随机化、bankList使用hashSet接收）


            //题型个数
            int typeChose  = 0;
            int typeFill   = 0;
            int typeShort  = 0;
            int typeCompre = 0;


            //此次迭代各个题型的数目
            for (String s:itemSet) {
                System.out.println(s);

                //计算每种题型个数
                if(TYPE.CHOSE.toString().equals(s.split(":")[1])){
                    typeChose += 1;
                }
                if(TYPE.FILL.toString().equals(s.split(":")[1])){
                    typeFill += 1;
                }
                if(TYPE.SHORT.toString().equals(s.split(":")[1])){
                    typeShort += 1;
                }
                if(TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])){
                    typeCompre += 1;
                }
            }

            //题型比例
            double typeChoseRation  =  typeChose/10.0;
            double typeFileRation   =  typeFill/10.0;
            double typeShortRation  =  typeShort/10.0;
            double typeCompreRation =  typeCompre/10.0;


            //属性个数
            int attributeNum1  = 0;
            int attributeNum2  = 0;
            int attributeNum3  = 0;
            int attributeNum4  = 0;
            int attributeNum5  = 0;

            //此次迭代各个属性的数目
            for (String s:itemSet) {
                System.out.println(s);

                //计算每种题型个数
                if("1".equals(s.split(":")[2].substring(1,2))){
                    attributeNum1 += 1;
                }
                if("1".equals(s.split(":")[2].substring(3,4))){
                    attributeNum2 += 1;
                }
                if("1".equals(s.split(":")[2].substring(5,6))){
                    attributeNum3 += 1;
                }
                if("1".equals(s.split(":")[2].substring(7,8))){
                    attributeNum4 += 1;
                }
                if("1".equals(s.split(":")[2].substring(9,10))){
                    attributeNum5 += 1;
                }
            }
        //System.out.println("AttributeRatio1: "+attributeNum1+"\tAttributeRatio2: "+attributeNum2+"\tAttributeRatio3: "+attributeNum3+"\tAttributeRatio4: "+attributeNum4+"\tAttributeRatio5: "+attributeNum5);

            //属性比例
            double attributeRatio1 = attributeNum1/23.0;
            double attributeRatio2 = attributeNum2/23.0;
            double attributeRatio3 = attributeNum3/23.0;
            double attributeRatio4 = attributeNum4/23.0;
            double attributeRatio5 = attributeNum5/23.0;



            // 题型和属性比例 和轮盘赌搭建关系：
            //      已抽取的属性个数越多，则惩罚系数越大 且各个属性是累乘关系
            //      比例和一个固定值做比较即可
            //      eg: typeChose/10    AttributeRatio1/23
            //      如果未超出比例，则按照正常流程走，一旦超过，则适应度值急剧下降

            for (int j = 0; j < bankList.size(); j++) {
                double penalty = 1;
                String[] splits = bankList.get(j).split(":");

                //题型比例
                if(splits[1].contains(TYPE.CHOSE+"")){
                    if(typeChoseRation<0.4){
                        penalty = penalty * Math.pow(0.5,typeChose);
                    }else {
                        penalty = penalty * Math.pow(0.5,typeChose*typeChose);
                    }
                }
                if(splits[1].contains(TYPE.FILL+"")){
                    if(typeFileRation<0.4){
                        penalty = penalty * Math.pow(0.5,typeFill);
                    }else {
                        penalty = penalty * Math.pow(0.5,typeFill*typeFill);
                    }
                }
                if(splits[1].contains(TYPE.SHORT+"")){
                    if(typeShortRation<0.3){
                        penalty = penalty * Math.pow(0.5,typeShort);
                    }else {
                        penalty = penalty * Math.pow(0.5,typeShort*typeShort);
                    }
                }
                if(splits[1].contains(TYPE.COMPREHENSIVE+"")){
                    if(typeCompreRation<0.3){
                        penalty = penalty * Math.pow(0.5,typeCompre);
                    }else {
                        penalty = penalty * Math.pow(0.5,typeCompre*typeCompre);
                    }
                }

                //属性比例
                if("1".equals(splits[2].substring(1,2))){
                    if(attributeRatio1<0.4){
                        penalty = penalty * Math.pow(0.8,attributeNum1);
                    }else {
                        penalty = penalty * Math.pow(0.8,attributeNum1*attributeNum1);
                    }
                }
                if("1".equals(splits[2].substring(3,4))){
                    if(attributeRatio2<0.4){
                        penalty = penalty * Math.pow(0.8,attributeNum2);
                    }else {
                        penalty = penalty * Math.pow(0.8,attributeNum2*attributeNum2);
                    }
                }
                if("1".equals(splits[2].substring(5,6))){
                    if(attributeRatio3<0.3){
                        penalty = penalty * Math.pow(0.8,attributeNum3);
                    }else {
                        penalty = penalty * Math.pow(0.8,attributeNum3*attributeNum3);
                    }
                }
                if("1".equals(splits[2].substring(7,8))){
                    if(attributeRatio4<0.3){
                        penalty = penalty * Math.pow(0.8,attributeNum4);
                    }else {
                        penalty = penalty * Math.pow(0.8,attributeNum4*attributeNum4);
                    }
                }
                if("1".equals(splits[2].substring(9,10))){
                    if(attributeRatio5<0.3){
                        penalty = penalty * Math.pow(0.8,attributeNum5);
                    }else {
                        penalty = penalty * Math.pow(0.8,attributeNum5*attributeNum5);
                    }
                }


                //个体值 和 总和
                fitTmp[j] = penalty ;
                fitSum = fitSum + penalty ;

            }

        // 计算出每道试题的各自比例
        for (int i = 0; i < bankList.size(); i++) {
            fitPro[i] = fitTmp[i] / fitSum;
        }

        //返回类型为 double[] 转 ArrayList<Double>  可以省略
        ArrayList<Double> arrayList = new ArrayList<>(fitPro.length);
        for (double anArr : fitPro) {
            arrayList.add(anArr);
        }
        //System.out.println(arrayList.toString());


        return  fitPro;
    }



    private ArrayList<String> getBank() throws SQLException {

        return jdbcUtils.select();

    }

    /**
     * 锦标赛选取题目id
     *       评价题目的好坏指标也应该和已选取的题目相挂钩，不然无法满足题型和属性的比例要求
     *
     *
     * TODO 遗传算法中的锦标赛选择策略每次从种群中取出一定数量个体（放回抽样），然后选择其中最好的一个进入子代种群。
     *      具体的操作步骤如下：
     *          1、确定每次选择的个体数量N。（N元锦标赛选择即选择N个个体）
     *          2、从种群中随机选择N个个体(每个个体被选择的概率相同) ，根据每个个体的适应度值，
     *             选择其中适应度值最好的个体进入下一代种群。
     *          3、重复步骤(2)多次（重复次数为种群的大小），直到新的种群规模达到原来的种群规模。
     */
    private int championship(HashSet<String> itemSet) throws SQLException {
        //9元锦标赛   当N的个数无限接近题库大小时,其和轮盘赌的前半部分是一致的
        int num = 9 ;

        //概率相等的选取n个体
        ArrayList<String> arrayList = jdbcUtils.selectChampionship(num);

        //拿itemSet衡量arrayList,并选取最优
        // 每道试题的适应度值
        Map<String, Double> map = new HashMap<>();


        //题型个数
        int typeChose  = 0;
        int typeFill   = 0;
        int typeShort  = 0;
        int typeCompre = 0;


        //此次迭代各个题型的数目
        for (String s:itemSet) {
            System.out.println(s);

            //计算每种题型个数
            if(TYPE.CHOSE.toString().equals(s.split(":")[1])){
                typeChose += 1;
            }
            if(TYPE.FILL.toString().equals(s.split(":")[1])){
                typeFill += 1;
            }
            if(TYPE.SHORT.toString().equals(s.split(":")[1])){
                typeShort += 1;
            }
            if(TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])){
                typeCompre += 1;
            }
        }

        //题型比例
        double typeChoseRation  =  typeChose/10.0;
        double typeFileRation   =  typeFill/10.0;
        double typeShortRation  =  typeShort/10.0;
        double typeCompreRation =  typeCompre/10.0;


        //属性个数
        int attributeNum1  = 0;
        int attributeNum2  = 0;
        int attributeNum3  = 0;
        int attributeNum4  = 0;
        int attributeNum5  = 0;

        //属性数目总和
        for (String s:itemSet) {
            System.out.println(s);

            //计算每种题型个数
            if("1".equals(s.split(":")[2].substring(1,2))){
                attributeNum1 += 1;
            }
            if("1".equals(s.split(":")[2].substring(3,4))){
                attributeNum2 += 1;
            }
            if("1".equals(s.split(":")[2].substring(5,6))){
                attributeNum3 += 1;
            }
            if("1".equals(s.split(":")[2].substring(7,8))){
                attributeNum4 += 1;
            }
            if("1".equals(s.split(":")[2].substring(9,10))){
                attributeNum5 += 1;
            }
        }
        System.out.println("AttributeRatio1: "+attributeNum1+"\tAttributeRatio2: "+attributeNum2+"\tAttributeRatio3: "+attributeNum3+"\tAttributeRatio4: "+attributeNum4+"\tAttributeRatio5: "+attributeNum5);

        //属性比例
        double attributeRatio1 = attributeNum1/23.0;
        double attributeRatio2 = attributeNum2/23.0;
        double attributeRatio3 = attributeNum3/23.0;
        double attributeRatio4 = attributeNum4/23.0;
        double attributeRatio5 = attributeNum5/23.0;



        // 属性要求和比例要求 和轮盘赌搭建关系：
        //      已抽取的属性个数越多，则惩罚系数越大 且各个属性是累乘关系
        //      比例和一个固定值做比较即可
        //      eg: typeChose/10    AttributeRatio1/23
        //      如果未超出比例，则按照正常流程走，一旦超过，则适应度值急剧下降

        for (int j = 0; j < arrayList.size(); j++) {
            double penalty = 1;
            String[] splits = arrayList.get(j).split(":");

            //题型比例
            if(splits[1].contains(TYPE.CHOSE+"")){
                if(typeChoseRation<0.4){
                    penalty = penalty * Math.pow(0.5,typeChose);
                }else {
                    penalty = penalty * Math.pow(0.5,typeChose*typeChose);
                }
            }
            if(splits[1].contains(TYPE.FILL+"")){
                if(typeFileRation<0.4){
                    penalty = penalty * Math.pow(0.5,typeFill);
                }else {
                    penalty = penalty * Math.pow(0.5,typeFill*typeFill);
                }
            }
            if(splits[1].contains(TYPE.SHORT+"")){
                if(typeShortRation<0.3){
                    penalty = penalty * Math.pow(0.5,typeShort);
                }else {
                    penalty = penalty * Math.pow(0.5,typeShort*typeShort);
                }
            }
            if(splits[1].contains(TYPE.COMPREHENSIVE+"")){
                if(typeCompreRation<0.3){
                    penalty = penalty * Math.pow(0.5,typeCompre);
                }else {
                    penalty = penalty * Math.pow(0.5,typeCompre*typeCompre);
                }
            }

            //属性比例
            if("1".equals(splits[2].substring(1,2))){
                if(attributeRatio1<0.3){
                    penalty = penalty * Math.pow(0.8,attributeNum1);
                }else {
                    penalty = penalty * Math.pow(0.8,attributeNum1*attributeNum1);
                }
            }
            if("1".equals(splits[2].substring(3,4))){
                if(attributeRatio2<0.3){
                    penalty = penalty * Math.pow(0.8,attributeNum2);
                }else {
                    penalty = penalty * Math.pow(0.8,attributeNum2*attributeNum2);
                }
            }
            if("1".equals(splits[2].substring(5,6))){
                if(attributeRatio3<0.3){
                    penalty = penalty * Math.pow(0.8,attributeNum3);
                }else {
                    penalty = penalty * Math.pow(0.8,attributeNum3*attributeNum3);
                }
            }
            if("1".equals(splits[2].substring(7,8))){
                if(attributeRatio4<0.3){
                    penalty = penalty * Math.pow(0.8,attributeNum4);
                }else {
                    penalty = penalty * Math.pow(0.8,attributeNum4*attributeNum4);
                }
            }
            if("1".equals(splits[2].substring(9,10))){
                if(attributeRatio5<0.3){
                    penalty = penalty * Math.pow(0.8,attributeNum5);
                }else {
                    penalty = penalty * Math.pow(0.8,attributeNum5*attributeNum5);
                }
            }

            //个体值  splits[0] 不应该会出现角标为0的情况啊
            map.put(splits[0],penalty);

        }

        //选出适应度最佳的个体
        String s = hashMapSort(map);

        System.out.println("锦标赛选取的试题: "+s);

        return Integer.parseInt(s.split(":")[0]);
    }


    /**
     *  hashMap 根据排序
     *
     */
    private String hashMapSort(Map<String, Double> map) {

        System.out.println("============排序前============");
        Set<Map.Entry<String, Double>> entrySet = map.entrySet();
        for (Map.Entry s : entrySet) {
            System.out.println(s.getKey()+"--"+s.getValue());
        }

        System.out.println("============排序后============");

        //借助list实现hashMap排序

        List<Map.Entry<String, Double>> list = new ArrayList<>(map.entrySet());
        Collections.sort(list, (o1, o2) -> {

            if(o1 == null && o2 == null) {
                return 0;
            }
            if(o1 == null) {
                return -1;
            }
            if(o2 == null) {
                return 1;
            }
            if(o1.getValue() > o2.getValue()) {
                return -1;
            }
            if(o2.getValue() > o1.getValue()) {
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
            System.out.println(s.getKey()+"--"+s.getValue());
        }

        return list.get(0).getKey()+":"+list.get(0).getValue();

    }



    /**
     *  长度 题型比例校验
     *  检验完成后，更新 paperGenetic
     *
     */
    private void correctType(int i, String[] temp1) throws SQLException {

        if (temp1.length == 10){

            System.out.println("第 "+i+" 题, size正常交叉");

        }else{

            System.out.println("第 "+i+" 题, 交叉导致size不匹配：开始进行交叉修补算子 步骤1 ....");
            /*
             *  原始的方法是 直接按照固定值进行匹配的， 需做修改成比例
             *  解决方案：   ①size的大小为10时退出
             *             ②如果在小于范围下限，则从数据库中选取
             *             ③如果都符合要求，则随机选取一题，再在下层做处理
             *
             */
            //去重操作
            HashSet<String> setBegin = new HashSet<>(Arrays.asList(temp1));
            HashSet<String> setEnd = new HashSet<>();

            double typeLower1 = 0.1;
            double typeLower2 = 0.2;

            //分别将题型的数量进行统计
            //在idea中，会为重新分配过地址的变量加上下划线，是为了快速发现那些变量被重新分配了地址

            int typeChose  = 0;
            int typeFill   = 0;
            int typeShort  = 0;
            int typeCompre = 0;


            //此次迭代各个题型的数目
            for (String s:setBegin) {
                //System.out.println(s);
                //计算每种题型个数
                if(TYPE.CHOSE.toString().equals(s.split(":")[1])){
                    typeChose += 1;
                }
                if(TYPE.FILL.toString().equals(s.split(":")[1])){
                    typeFill += 1;
                }
                if(TYPE.SHORT.toString().equals(s.split(":")[1])){
                    typeShort += 1;
                }
                if(TYPE.COMPREHENSIVE.toString().equals(s.split(":")[1])){
                    typeCompre += 1;
                }
            }

            System.out.println("目前题型数量情况： typeChose:"+typeChose+" typeFill:"+typeFill+" typeShort:"+typeShort+" typeCompre:"+typeCompre);

            //题型比例
            double typeChoseRation  =  typeChose/10.0;
            double typeFileRation   =  typeFill/10.0;
            double typeShortRation  =  typeShort/10.0;
            double typeCompreRation =  typeCompre/10.0;

            setEnd.addAll(setBegin);
            //选择题
            while(typeChoseRation<typeLower2 && setEnd.size() != 10){
                //  where type = Chose
                String sql = " type = 'CHOSE' order by RAND() limit 1 ";
                ArrayList<String> arrayList = jdbcUtils.selectBySql(sql);
                HashSet<String> tmp = new HashSet<>(arrayList);
                //动态更新类型比例信息
                if(!setEnd.contains(tmp)){
                    setEnd.addAll(tmp);
                    typeChose +=1;
                    typeChoseRation  =  typeChose/10.0;
                }
            }
            //填空题
            while(typeFileRation<typeLower2 && setEnd.size() != 10){
                //  where type = Chose
                String sql = " type = 'FILL' order by RAND() limit 1 ";
                ArrayList<String> arrayList = jdbcUtils.selectBySql(sql);
                HashSet<String> tmp = new HashSet<>(arrayList);
                if(!setEnd.contains(tmp)){
                    setEnd.addAll(tmp);
                    typeFill +=1;
                    typeFileRation  =  typeFill/10.0;
                }
            }
            //简答题
            while(typeShortRation<typeLower1 && setEnd.size() != 10){
                //  where type = Chose
                String sql = " type = 'SHORT' order by RAND() limit 1 ";
                ArrayList<String> arrayList = jdbcUtils.selectBySql(sql);
                HashSet<String> tmp = new HashSet<>(arrayList);
                if(!setEnd.contains(tmp)){
                    setEnd.addAll(tmp);
                    typeShort +=1;
                    typeShortRation  =  typeShort/10.0;
                }
            }
            //综合题
            while(typeCompreRation<typeLower1 && setEnd.size() != 10){
                //  where type = Chose
                String sql = " type = 'COMPREHENSIVE' order by RAND() limit 1 ";
                ArrayList<String> arrayList = jdbcUtils.selectBySql(sql);
                HashSet<String> tmp = new HashSet<>(arrayList);
                if(!setEnd.contains(tmp)){
                    setEnd.addAll(tmp);
                    typeCompre +=1;
                    typeCompreRation  =  typeCompre/10.0;
                }
            }
            //随机选题
            if(setEnd.size() != 10){
                //  where 1=1
                String sql = " 1=1 order by RAND() limit 1 ";
                ArrayList<String> arrayList = jdbcUtils.selectBySql(sql);
                HashSet<String> tmp = new HashSet<>(arrayList);
                setEnd.addAll(tmp);
            }
            //输出集合的大小  setEnd.size()
            System.out.println("setEnd.size(): "+setEnd.size());

            //默认字典序，不会导致变异出现问题吗   应该不会了，数据库的题型不是按照字典序排序
            // hashSet 转 数组
            String[] array = new String[setEnd.size()];
            array = setEnd.toArray(array);
            array = sortPatch(array);

            paperGenetic[i]=array;
            //打印选取的题目，打印的结果 应该是内存地址
            System.out.println("步骤1 size修补后的结果如下："+Arrays.toString(paperGenetic[i]));

        }

    }


    /**
     * 通过差集 并集  来重新对list排序
     */
    private ArrayList<String> rearrange(String type, ArrayList<String> listA){

        //定义
        ArrayList<String> listB = new ArrayList<>();
        for (String s : listA) {
            if(s.contains(type)){
                listB.add(s);
            }
        }

        //差集 并集
        listA.removeAll(listB);
        listB.addAll(listA);
        System.out.println(listB);

        return listB;

    }


}



