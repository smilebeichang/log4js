package cn.edu.sysu.utils;

import cn.edu.sysu.adi.TYPE;

import java.sql.*;
import java.util.*;

/**
 * @Author : song bei chang
 * @create 2021/12/30 20:49
 */
public class CorrectUtils {


    private  JDBCUtils4 jdbcUtils = new JDBCUtils4();
    int paperSize = 20;

    /**
     *  size 为310
     */
    ArrayList<String> allItemList = jdbcUtils.selectAllItems();

    public CorrectUtils() throws SQLException {
    }


    /**
     * 题型校验(前提是不会破坏长度)
     *      每次校验完成后，下一次的交叉变异，typeFlag很大概率会再次失衡
     *      无需保证迭代过程中实时的维持一致性，保证每次迭代的选取时题型比例适当即可，即最终一致性
     *
     *
     *      3.0 执行修补操作
     *      目标：将in解替换out解
     *      方法：去题库中搜索，取出新解集后，循环遍历，然后重新计算是否符合要求 (这样将会导致计算很冗余)
     *      要求：
     *           1.完美解：删除/新增不影响其他类型和属性比例 （修改type）
     *           2.替补解：如果找不到，则在较优解中随机选取一个用作替补解 （修改type,修改attr,但符合比例要求）
     *
     *      多    ①多一个  ②多N个 先遍历匹配(完美解)，若没有，则寻找替补解
     *      少    ①少一个  ②少N个 先遍历匹配(完美解)，若没有，则寻找替补解
     *      多&少  多少各执行一次
     *
     */
    public String[] correctType(String[] temp ) throws SQLException {

        //==============  1.0 指标统计   ====================
        // 只是转换,不会导致size上的变动 array--> list --> hashSet


        List<String> sList = Arrays.asList(temp);

        String[] itemArrayV2 = new String[sList.size()];

        for (int k = 0; k < sList.size(); k++) {
            itemArrayV2[k] = allItemList.get(Integer.parseInt(sList.get(k).trim())-1 > -1?Integer.parseInt(sList.get(k).trim())-1:1);
            //itemArrayV2[k] = allItemList.get(Integer.parseInt(sList.get(k).trim())-1);
        }

        HashSet<String> setBegin = new HashSet<>(Arrays.asList(itemArrayV2));



        // 获取个体的题型指标信息
        String typeFlag = getTypeFlag(setBegin);

        int tf1 = Integer.parseInt(typeFlag.split(",")[0]);
        int tf2 = Integer.parseInt(typeFlag.split(",")[1]);
        int tf3 = Integer.parseInt(typeFlag.split(",")[2]);
        int tf4 = Integer.parseInt(typeFlag.split(",")[3]);


        //================  2.0 解集统计   =========================

        //根据typeFlag 得出outMore/outLess解集
        ArrayList<String> batchItemList = new ArrayList<>();
        Collections.addAll(batchItemList,itemArrayV2);

        // out解的容器  (可能造成题型比例失衡的解集)
        Set<String> outMore = getOutMoreType(batchItemList,tf1,tf2,tf3,tf4);
        Set<String> outLess = getOutLessType(batchItemList,tf1,tf2,tf3,tf4);


        //*****************  3.1 outLess有  outMore有值   **********
        //  outLess有  outMore 有 分别进行两次迭代    迭代过程中需实时更新比例信息
        //  outMore 校验其他类型不要多  outLess 正常校验
        if(outMore.size()>0 && outLess.size()>0 ){
            //batchItemList = correctTypeMoreAndLess(outMore,outLess,jdbcUtils,batchItemList,tf1,tf2,tf3,tf4);
        }

        //************** 3.2 outLess有  outMore无值   **************
        // 画横线的地方均需考虑，是否会发生数据的变化
        if(outMore.size()==0 && outLess.size()>0){
            batchItemList = correctTypeLess(outLess,jdbcUtils,batchItemList,tf1,tf2,tf3,tf4);
        }


        //**************  3.3 outLess无  outMore有值   *************
        if(outMore.size()>0 && outLess.size()==0){
            batchItemList = correctTypeMore(outMore,jdbcUtils,batchItemList,tf1,tf2,tf3,tf4);
        }

        // arrayList 转 array
        String[] itemArray = new String[batchItemList.size()];
        for (int i = 0; i < batchItemList.size(); i++) {
            itemArray[i] = batchItemList.get(i).split(":")[0];
        }
        temp = sortPatch(itemArray);

        return temp;

    }

    public String[] correctTypeV2(String[] temp ) throws SQLException {

        //==============  1.0 指标统计   ====================
        // 只是转换,不会导致size上的变动
        HashSet<String> setBegin = new HashSet<>(Arrays.asList(temp));

        // 获取个体的题型指标信息
        String typeFlag = getTypeFlag(setBegin);

        int tf1 = Integer.parseInt(typeFlag.split(",")[0]);
        int tf2 = Integer.parseInt(typeFlag.split(",")[1]);
        int tf3 = Integer.parseInt(typeFlag.split(",")[2]);
        int tf4 = Integer.parseInt(typeFlag.split(",")[3]);


        //================  2.0 解集统计   =========================

        //根据typeFlag 得出outMore/outLess解集
        ArrayList<String> batchItemList = new ArrayList<>();
        Collections.addAll(batchItemList,temp);

        // out解的容器  (可能造成题型比例失衡的解集)
        Set<String> outMore = getOutMoreType(batchItemList,tf1,tf2,tf3,tf4);
        Set<String> outLess = getOutLessType(batchItemList,tf1,tf2,tf3,tf4);


        //*****************  3.1 outLess有  outMore有值   **********
        //  outLess有  outMore 有 分别进行两次迭代    迭代过程中需实时更新比例信息
        //  outMore 校验其他类型不要多  outLess 正常校验
        if(outMore.size()>0 && outLess.size()>0 ){
            //batchItemList = correctTypeMoreAndLess(outMore,outLess,jdbcUtils,batchItemList,tf1,tf2,tf3,tf4);
        }

        //************** 3.2 outLess有  outMore无值   **************
        // 画横线的地方均需考虑，是否会发生数据的变化
        if(outMore.size()==0 && outLess.size()>0){
            batchItemList = correctTypeLess(outLess,jdbcUtils,batchItemList,tf1,tf2,tf3,tf4);
        }


        //**************  3.3 outLess无  outMore有值   *************
        if(outMore.size()>0 && outLess.size()==0){
            batchItemList = correctTypeMore(outMore,jdbcUtils,batchItemList,tf1,tf2,tf3,tf4);
        }

        // 赋值给全局变量 (arrayList 转 array)
        String[] itemArray = new String[batchItemList.size()];
        for (int i = 0; i < batchItemList.size(); i++) {
            itemArray[i] = batchItemList.get(i);
        }
        temp = sortPatch(itemArray);

        return temp;

    }


    /**
     *  获取个体的题型指标信息  typeFlag
     *      ①遍历获的各个题型的数目
     *      ②除以总数量，得到各个题型的比例
     *      ③预期比例 vs 实际比例 --> flag(1,0,-1)
     *      ④拼接成typeFlag
     *
     */
    private String getTypeFlag(HashSet<String> itemSet){

        //题型数量
        int typeChose = 0;
        int typeFill = 0;
        int typeShort = 0;
        int typeCompre = 0;

        //此次迭代各个题型的数目
        for (String s : itemSet) {
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
        double typeChoseRation = typeChose / 20.0;
        double typeFileRation = typeFill / 20.0;
        double typeShortRation = typeShort / 20.0;
        double typeCompreRation = typeCompre / 20.0;

        //题型flag (-1:少于,0:正常,1:大于)
        int tf1;
        if (typeChoseRation >= 0.2 && typeChoseRation <= 0.4) {
            tf1 = 0;
        } else if (typeChoseRation < 0.2) {
            tf1 = -1;
        } else {
            tf1 = 1;
        }

        int tf2;
        if (typeFileRation >= 0.2 && typeFileRation <= 0.4) {
            tf2 = 0;
        } else if (typeFileRation < 0.2) {
            tf2 = -1;
        } else {
            tf2 = 1;
        }

        int tf3;
        if (typeShortRation >= 0.1 && typeShortRation <= 0.3) {
            tf3 = 0;
        } else if (typeShortRation < 0.1) {
            tf3 = -1;
        } else {
            tf3 = 1;
        }

        int tf4;
        if (typeCompreRation >= 0.1 && typeCompreRation <= 0.3) {
            tf4 = 0;
        } else if (typeCompreRation < 0.1) {
            tf4 = -1;
        } else {
            tf4 = 1;
        }

        String typeFlag =  tf1 + "," + tf2 + "," + tf3 + "," + tf4 ;
        System.out.println("目前题型占比情况： typeFlag:" + typeFlag);
        return typeFlag;
    }


    /**
     * 根据4个tf指标信息，得出outMore解
     *    集合取并集 多了|少了，本质一样（替换前需校验题型比例是否符合要求），不需要做特殊处理
     *
     */
    private Set<String> getOutMoreType(ArrayList<String> batchItemList, int tf1, int tf2, int tf3, int tf4){

        Set<String> outMore = new HashSet<>();

        //取出题型比例过多的集合的并集  ?这不是必须会进来吗
        if (tf1 == 1 || tf2 == 1 || tf3 == 1 || tf4 == 1) {

            //需要判断 set 是否为空
            outMore.clear();

            //表明题型1比例过多，用outMore集合接收
            if (tf1 == 1) {
                for (String aBachItemList : batchItemList) {
                    if (aBachItemList.split(":")[1].equals(TYPE.CHOSE+"")) {
                        outMore.add(aBachItemList);
                    }
                }
            }

            if (tf2 == 1) {
                for (String aBachItemList : batchItemList) {
                    if (aBachItemList.split(":")[1].equals(TYPE.FILL+"")) {
                        outMore.add(aBachItemList);
                    }
                }
            }

            if (tf3 == 1) {
                for (String aBachItemList : batchItemList) {
                    if (aBachItemList.split(":")[1].equals(TYPE.SHORT+"")) {
                        outMore.add(aBachItemList);
                    }
                }
            }

            if (tf4 == 1) {
                for (String aBachItemList : batchItemList) {
                    if (aBachItemList.split(":")[1].equals(TYPE.COMPREHENSIVE+"")) {
                        outMore.add(aBachItemList);
                    }
                }
            }
        }
        return outMore;

    }

    /**
     * 根据4个tf指标信息，得出outLess解
     *    集合取并集 多了|少了，本质一样（替换前需校验题型比例是否符合要求），不需要做特殊处理
     *
     */
    private Set<String> getOutLessType(ArrayList<String> batchItemList, int tf1, int tf2, int tf3, int tf4){

        Set<String> outLess = new HashSet<>();
        //取出题型比例不足的集合的并集
        if(tf1==-1 || tf2==-1 || tf3==-1 || tf4==-1 ){

            //清空set
            outLess.clear();

            //表明属性1比例过少，用set集合接收
            if(tf1==-1){
                for (String aBachItemList : batchItemList) {
                    if (!aBachItemList.split(":")[1].equals(TYPE.CHOSE+"")) {
                        outLess.add(aBachItemList);
                    }
                }
            }

            if(tf2==-1){
                for (String aBachItemList : batchItemList) {
                    if (!aBachItemList.split(":")[1].equals(TYPE.FILL+"")) {
                        outLess.add(aBachItemList);
                    }
                }
            }

            if(tf3==-1){
                for (String aBachItemList : batchItemList) {
                    if (!aBachItemList.split(":")[1].equals(TYPE.SHORT+"")) {
                        outLess.add(aBachItemList);
                    }
                }
            }

            if(tf4 ==-1){
                for (String aBachItemList : batchItemList) {
                    if (!aBachItemList.split(":")[1].equals(TYPE.COMPREHENSIVE+"")) {
                        outLess.add(aBachItemList);
                    }
                }
            }

        }

        return outLess;

    }


    /**
     * 校验题型比例，适用less场景
     *      ①根据tf指标,拼接sql,定向搜索inList  or取并集
     *      ②寻找完美解：  只改变out解的type，不改变attr,进行替换
     *        寻找替补解： 改变out解的type,attr，但能保证attr符合全局比例要求，这个其实在帮下一层做准备
     *
     */
    private ArrayList<String> correctTypeLess(Set<String> outLess, JDBCUtils4 jdbcUtils, ArrayList<String> batchItemList, int tf1, int tf2, int tf3, int tf4) throws SQLException {

        System.out.println("本套试卷 题型比例不足的情况。");

        StringBuilder sb = new StringBuilder();
        if(tf1>0){
            sb.append(" type != 'CHOSE' or ");
        }else if (tf1 <0){
            sb.append(" type = 'CHOSE' or ");
        }

        if(tf2>0){
            sb.append(" type != 'FILL' or ");
        }else if (tf2<0){
            sb.append(" type = 'FILL' or ");
        }

        if(tf3>0){
            sb.append(" type != 'SHORT' or ");
        }else if (tf3<0){
            sb.append(" type = 'SHORT' or ");
        }

        if(tf4>0){
            sb.append(" type != 'COMPREHENSIVE' or ");
        }else if (tf4<0){
            sb.append(" type = 'COMPREHENSIVE' or ");
        }

        // 题型的定向搜索SQL
        String sql = "(" + sb.toString().substring(0, sb.toString().length() - 3) +")";


        // ori集 - out解 + in解 = 新解(拿新解去再次校验)
        // 循环的逻辑：外层out解，内层in解，不断的调用题型比例校验方法，如满足要求则退出，不满足则继续遍历
        List<String> outList = new ArrayList<>(outLess);

        Boolean b = false;
        // 使用out做外层循环,同时寻找完美解和替补解
        for (int i = 0; i < outList.size(); i++) {

            // 校验题型的时候，不破坏原有题目的属性信息   本身是一个矫正因子,是根本
            String p1 = " and( p1 = " + outList.get(i).split(":")[2].split(",")[0].substring(1,2);
            String p2 = " and p2 = " + outList.get(i).split(":")[2].split(",")[1];
            String p3 = " and p3 = " + outList.get(i).split(":")[2].split(",")[2];
            String p4 = " and p4 = " + outList.get(i).split(":")[2].split(",")[3];
            String p5 = " and p5 = " + outList.get(i).split(":")[2].split(",")[4].substring(0,1) + " ) ";

            // 获取完全吻合的解  题型+属性
            String sqlPerfect = sql + (p1 + p2 + p3 + p4 + p5);
            // FIXME 最好通过sql去打乱顺序inList2
            ArrayList<String> inList2 = jdbcUtils.selectBySql(sqlPerfect);

            // 寻找完美解
            if(inList2.size()>0){

                // 循环的意义是什么呢？  逻辑没问题，但需要优化，省去不必要的部分for if
                // ori外层 in内层，这样可以保证替换的解随机性  如果in一开始顺序就乱了，那么可以互换 嵌套本就可以互换，顶多性能上差异
                // 当需要嵌套循环时 外层循环越小 性能越好，目前两种都很小

                Boolean flagPerfect = false;
                for (int j = 0; j < inList2.size(); j++) {

                    // 删除out解，添加in解   保证in解和ori解的不重复
                    for (int k = 0; k < batchItemList.size(); k++) {
                        if (batchItemList.get(k).equals(outList.get(i))){
                            if(!batchItemList.contains(inList2.get(j))){
                                batchItemList.set(k,inList2.get(j));
                                flagPerfect = true;
                                //退出batchItemList替换过程
                                break;
                            }
                        }
                    }
                    // 退出inList2循环
                    if(flagPerfect){
                        break;
                    }
                }
                // 使用flagPerfect判断是否继续寻找完美解，退出outList循环
                // 只替换一次 outList吗?  ①目前只替换一次，除非动态更新4个题型flag以及outList  ②动态更新的好处在于可以一次性将属性校验完成
                if (flagPerfect){
                    //System.out.println("完美解找到,退出less题型校验");
                    break;
                }

            }else{
                // 寻找替补解   替补解虽会导致一定程度的属性比例轻微变化，但能保证最终的比例不失衡 大概率不会跑到这个流程分支来
                // 根据type大面积选题，然后使用typeCheck()进行校验看是否需要保留
                ArrayList<String> inList = jdbcUtils.selectBySql(sql);

                Boolean flagAlternate = false;
                for (int j = 0; j < inList.size(); j++) {

                    //内存地址问题
                    ArrayList<String> tmp = new ArrayList<>();
                    for (int i2 = batchItemList.size(); i2 > 0; i2--) {
                        tmp.add(batchItemList.get(i2-1));
                    }
                    b = typeCheck(tmp,outList.get(i),inList.get(j));

                    if(b){
                        // 删除out解，添加in解   这里需要保证in解不在ori解中
                        for (int k = 0; k < batchItemList.size(); k++) {
                            if (batchItemList.get(k).equals(outList.get(i))){
                                if(!batchItemList.contains(inList.get(j))){
                                    batchItemList.set(k,inList.get(j));
                                    flagAlternate = true;
                                    // 退出batchItemList循环
                                    break;
                                }
                            }
                        }
                        // break 保证里面的第一层循环没找解的情况下，可以进行第二层inList的寻找
                        // 退出inList循环
                        if(flagAlternate){
                            break;
                        }
                    }
                }
                // 使用flagAlternate来做判断是否退出继续寻找替补解，退出outList循环  目前也只寻找了一个outList
                // FIXME  需动态更新4个人typeFlag 和 outList
                if (flagAlternate){
                    break;
                }
            }
        }

        return batchItemList;
    }



    /**
     * 校验题型比例，适用more场景
     *      ①根据tf指标,拼接sql,定向搜索inList  or取并集
     *      ②寻找完美解：  只改变out解的type，不改变attr,进行替换
     *        寻找替补解： 改变out解的type,attr，但能保证attr符合全局比例要求，这个其实在帮下一层做准备
     *
     */
    public ArrayList<String> correctTypeMore(Set<String> outMore,JDBCUtils4 jdbcUtils,ArrayList<String> batchItemList,int tf1,int tf2,int tf3,int tf4) throws SQLException {

        //System.out.println("本套试卷 题型比例过多的情况。");

        //  SQL 均用or应该没影响  影响范围:inList解集变多
        //  CHOSE FILL SHORT COMPREHENSIVE
        StringBuilder sb = new StringBuilder();
        if(tf1>0){
            sb.append(" type != 'CHOSE' and ");
        }else if (tf1 <0){
            sb.append(" type = 'CHOSE' and ");
        }

        if(tf2>0){
            sb.append(" type != 'FILL' and ");
        }else if (tf2<0){
            sb.append(" type = 'FILL' and ");
        }

        if(tf3>0){
            sb.append(" type != 'SHORT' and ");
        }else if (tf3<0){
            sb.append(" type = 'SHORT' and ");
        }

        if(tf4>0){
            sb.append(" type != 'COMPREHENSIVE' and ");
        }else if (tf4<0){
            sb.append(" type = 'COMPREHENSIVE' and ");
        }

        //获取新解的SQL(题型)   大量的解、用在替补解的过程
        String sql = "(" + sb.toString().substring(0, sb.toString().length() - 4) +")";


        // ori解集  out解集  in解集 的关系
        // 原始解集 - out解 + in解 = 新解(拿新解去再次校验)
        // 循环的逻辑：外层out解，内层in解，不断的调用题型比例校验方法，如满足要求则退出，不满足则继续遍历

        List<String> outList = new ArrayList<>(outMore);

        Boolean b = false;
        for (int i = 0; i < outList.size(); i++) {

            // 校验题型的时候，不能影响其他属性的信息   本身是一个矫正因子，是根本
            String p1 = " and( p1 = " + outList.get(i).split(":")[2].split(",")[0].substring(1,2);
            String p2 = " and p2 = " + outList.get(i).split(":")[2].split(",")[1];
            String p3 = " and p3 = " + outList.get(i).split(":")[2].split(",")[2];
            String p4 = " and p4 = " + outList.get(i).split(":")[2].split(",")[3];
            String p5 = " and p5 = " + outList.get(i).split(":")[2].split(",")[4].substring(0,1) + " ) ";

            //  增加where条件(题型 + 属性)，查看是否存在完美解
            String sqlPerfect = sql + (p1 + p2 + p3 + p4 + p5);
            ArrayList<String> inList2 = jdbcUtils.selectBySql(sqlPerfect);

            //  寻找完美解
            if(inList2.size()>0){
                // boolean是基本数据类型  Boolean是它的封装类
                Boolean flagPerfect = false;
                for (int j = 0; j < inList2.size(); j++) {

                    b = true;

                    // 删除out解，添加in解
                    for (int k = 0; k < batchItemList.size(); k++) {
                        if (batchItemList.get(k).equals(outList.get(i))){
                            //需加一层判断  in解不能已存在ori中 否则将出现size=9的情况
                            if(!batchItemList.contains(inList2.get(j))){
                                batchItemList.set(k,inList2.get(j));
                                flagPerfect= true;
                                break;
                            }
                        }
                    }
                    // 这个break 有意思
                    if(flagPerfect){
                        break;
                    }
                }
                if (flagPerfect){
                    break;
                }

            }else{
                // 寻找替补解   替补解虽会导致一定程度的属性比例轻微变化，但一定能保证比例不失衡 大概率不会跑到这个流程分支来
                ArrayList<String> inList = jdbcUtils.selectBySql(sql);
                Boolean flagAlternate = false;
                for (int j = 0; j < inList.size(); j++) {

                    //内存地址问题
                    ArrayList<String> tmp = new ArrayList<>();
                    for (int i2 = batchItemList.size(); i2 > 0; i2--) {
                        tmp.add(batchItemList.get(i2-1));
                    }
                    b = typeCheck(tmp,outList.get(i),inList.get(j));

                    if(b){
                        // 删除out解，添加in解   这里需要保证in解不在ori解中
                        for (int k = 0; k < batchItemList.size(); k++) {
                            if (batchItemList.get(k).equals(outList.get(i))){
                                if(!batchItemList.contains(inList.get(j))){
                                    batchItemList.set(k,inList.get(j));
                                    flagAlternate = true;
                                    // 退出batchItemList循环
                                    break;
                                }
                            }
                        }
                        // break 保证里面的第一层循环没找解的情况下，可以进行第二层inList的寻找
                        // 退出inList循环
                        if(flagAlternate){
                            break;
                        }
                    }
                }
                // 退出outList循环
                if (flagAlternate){
                    break;
                }
            }
        }
        return batchItemList;
    }


    /**
     *
     * 排序
     *      1.获取id,重新据库查询一遍  返回的Array[]
     */
    public String[] sortPatch(String[] temp1) {


        //题型数量
        int  typeNum = paperSize;

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
                if(Integer.parseInt(ts.split(":")[0]) == index){
                    temp2[i] = ts;
                }
            }
        }

        return  temp2;
    }


    /**
     * 题型校验的（寻找替补解的过程）
     *      通过指标信息，判断是否可以作为替换out的替补解
     *      返回：true优解（可作为替补解)， false表示劣解
     *      需将属性指标信息考虑进行，保证属性比例不在这个阶段被破坏，甚至得到进一步的优化
     *
     */
    private Boolean typeCheck(ArrayList<String> tmp,String s0, String s1) {

        // 刪除元素s，添加元素s1
        for (int i = 0; i < tmp.size(); i++) {
            if (tmp.get(i).equals(s0)){
                tmp.set(i,s1);
            }
        }

        //校验题型比例
        //ArrayList<String> 转 hashSet<String>
        HashSet<String> itemSet = new HashSet<>(tmp);

        //题型数量
        int typeChose = 0;
        int typeFill = 0;
        int typeShort = 0;
        int typeCompre = 0;

        //此次迭代各个题型的数目
        for (String s : itemSet) {
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

        //题型flag (-1->少于,0->正常,1->大于)
        int tf1;
        if (typeChoseRation >= 0.2 && typeChoseRation <= 0.4) {
            tf1 = 0;
        } else if (typeChoseRation < 0.2) {
            tf1 = -1;
        } else {
            tf1 = 1;
        }

        int tf2;
        if (typeFileRation >= 0.2 && typeFileRation <= 0.4) {
            tf2 = 0;
        } else if (typeFileRation < 0.2) {
            tf2 = -1;
        } else {
            tf2 = 1;
        }

        int tf3;
        if (typeShortRation >= 0.1 && typeShortRation <= 0.3) {
            tf3 = 0;
        } else if (typeShortRation < 0.1) {
            tf3 = -1;
        } else {
            tf3 = 1;
        }

        int tf4;
        if (typeCompreRation >= 0.1 && typeCompreRation <= 0.3) {
            tf4 = 0;
        } else if (typeCompreRation < 0.1) {
            tf4 = -1;
        } else {
            tf4 = 1;
        }
        //typeFlag
        String typeFlag = "(" + tf1 + "," + tf2 + "," + tf3 + "," + tf4 + ")";
        //System.out.println("目前题型占比情况： typeFlag:" + typeFlag);


        // 校验属性比例
        int attributeNum1  = 0;
        int attributeNum2  = 0;
        int attributeNum3  = 0;
        int attributeNum4  = 0;
        int attributeNum5  = 0;

        //各个属性的数目
        for (String s:itemSet) {

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
        //输出 attributeFlag
        String attributeFlag = "("+af1+","+af2+","+af3+","+af4+","+af5+")";
        //System.out.println("目前属性占比情况： attributeFlag:"+attributeFlag);


        if((typeFlag.contains("1")) || (typeFlag.contains("-1")) || (attributeFlag.contains("1")) || (attributeFlag.contains("-1"))){
            return false ;
        }

        return true;

    }


    /**
     * 查询，并返回list
     */
    public  ArrayList<String> selectAllItems() throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        Connection conn  = null;
        PreparedStatement ps = null;
        ResultSet rs ;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            //System.out.println("驱动加载失败");
            ex.printStackTrace();
        }

        try {
            conn =
                    DriverManager.getConnection("jdbc:mysql://localhost/sysu?"+"user=root&password=root&useSSL=false");
            ps = conn.prepareStatement("select * from sysu.adi20210528 ;");
            rs = ps.executeQuery();
            //System.out.println("select * from sysu.adi20210528 where id in (" + ids +  ");");

            while(rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String pattern = rs.getString("pattern");
                double adi1_r = rs.getDouble("adi1_r");
                double adi2_r = rs.getDouble("adi2_r");
                double adi3_r = rs.getDouble("adi3_r");
                double adi4_r = rs.getDouble("adi4_r");
                double adi5_r = rs.getDouble("adi5_r");

                list.add(id+":"+type+":"+pattern+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r);
            }

        } catch (SQLException ex) {
            //System.out.println("SQLException: " + ex.getMessage());
            //System.out.println("SQLState: " + ex.getSQLState());
            //System.out.println("VendorError: " + ex.getErrorCode());
        }finally {
            if(ps!= null) {
                ps.close();
            }
            if(conn!= null) {
                conn.close();
            }
        }
        //System.out.println();
        return list;
    }



}



