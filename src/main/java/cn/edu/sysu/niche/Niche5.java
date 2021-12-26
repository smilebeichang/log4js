package cn.edu.sysu.niche;

import cn.edu.sysu.adi.TYPE;
import cn.edu.sysu.controller.ADIController6;
import cn.edu.sysu.utils.JDBCUtils4;

import java.sql.SQLException;
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
*  解决方法：直接在交叉。变异阶段各执行一次小生境
 *          是否在交叉和变异阶段均需进行小生境？
 *                  交叉不需要,限制性锦标赛拥挤本身是没做要求的（文献：交叉变异是同一个个体？）
 *                  单个执行和全局执行是否有偏差：
 *                          交叉阶段：
 *                              单个执行,保证了优胜个体的维持（即交叉后可能未被选取）
 *                              全局执行,具有更高的多样性
 *                          变异阶段：
 *                              无差异
 *                  变异，可以嵌入小生境：
 *                  ①去除小生境的交叉部分，去除原有变异的变异部分，两者进行替换
 *                  ②意味着变异可能无效
 *                      为什么小生境能维持多样性，因为其变异后的个体，会随机选择性的替换最相似的个体
 *                      而正常的变异，则直接进行替换。直接替换的话，可能的问题是，无法定向维持样性
 *                      是否还需要进行修补操作，需要看交叉是否影响了个体的类型属性要求。
 *                         因为是全局交叉+全局变异，则表明类型属性均遭到了破坏。故需要进行修补
 *                         下一阶段任务：将全局交叉变异，变成单个个体的交叉变异。
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
 *
 *
 *
 *
 */
public class Niche5 {

    private JDBCUtils4 jdbcUtils = new JDBCUtils4();

    /* 200套试卷 20道题  */
    private static String[][] paperGenetic =new String[200][20];

    public Niche5() throws SQLException {
    }

    /** 确定性拥挤算法 deterministic crowding */
    public void DET(String[][] paperGenetictmp) throws SQLException {
        //赋值给全局变量
        paperGenetic = paperGenetictmp;

        //有放回的随机选取两个父代个体p1 p2
        Random random = new Random();
        int  index1 = random.nextInt(200);
        int  index2 = random.nextInt(200);
        ArrayList<Integer> iList = new ArrayList<>(2);
        iList.add(index1);
        iList.add(index2);

        String[] p1 = paperGenetic[index1];
        String[] p2 = paperGenetic[index2];


        ArrayList<String[]> pList = new ArrayList<>(2);
        pList.add(p1);
        pList.add(p2);


        //父代交叉、变异，产生新个体c1/c2
        ArrayList<String[]> cList = crossMutateDet(p1, p2);


        //替换(父代子代进行比较)
        closestResembleDet(iList,cList,pList);

    }

    // size 为310
    ArrayList<String> AllItemList = jdbcUtils.selectAllItems();

    /**
     * 限制性锦标赛选择算法 restricted tournament selection
     *
     *      在组内进行锦标赛选择
     *
     * outCross.get(i):9,16,18,23,35,36,24,34,46,57,61,67,129,142,150,166,173,179,180,267
     *
     */
    public ArrayList<Object>  RTS(ArrayList<String> outCross,int i) throws SQLException {

        //赋值给全局变量
        //paperGenetic = paperGenetictmp;

        // 将原来的参数进行格式的变换,可以省去后续的不必要麻烦(可以在上层进行格式转化: 一次查询，多次使用)
        // outCross 转  paperGenetic[i]
        String s = outCross.get(i);
        // 查询数据库  list.add(id+":"+type+":"+pattern+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r)
//        ArrayList<String> bachItemList = jdbcUtils.selectBachItem(s);


        // 短暂休眠，保证连接缓冲
//        try {
//            Thread.sleep(50);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

//        for (int j = 0; j < bachItemList.size(); j++) {
//            itemArray[j] = bachItemList.get(j);
//        }

        // 通过遍历s,赋值给itemArray
        List<String> sList = Arrays.asList(s.split(","));
        // ArrayList 转 String[]
        String[] itemArray = new String[sList.size()];
        // java.lang.NumberFormatException: For input string: "16.215638876810402_1"
        if(!s.contains("_")){
            for (int j = 0; j < sList.size(); j++) {
                itemArray[j] = AllItemList.get(Integer.parseInt(sList.get(j))-1 > -1?Integer.parseInt(sList.get(j))-1:1);
            }
        }


        // FIXME 防止空指针,待后续优化  使用的是if,随机选取20道

        if (itemArray[1] == null ){
//            ArrayList<String> bachItemListBak = jdbcUtils.selectBachItemBak(s);
//
//            //短暂休眠，保证连接缓冲
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            // ArrayList 转 String[]
//            String[] itemArrayBak = new String[bachItemListBak.size()];
//            for (int j = 0; j < bachItemListBak.size(); j++) {
//                itemArrayBak[j] = bachItemListBak.get(j);
//            }
//            itemArray = itemArrayBak;

            Random r = new Random(1);

            HashSet<Integer> itemSet = new HashSet<>();
            //去重操作
            while (itemSet.size() < 20) {
                //获取试题id   轮盘赌构造
                System.out.println(" --> 验证卡死现象1.0 <-- ");
                int sId = r.nextInt(310);
                itemSet.add(sId);
            }
            List<Integer> list = new ArrayList<>();
            list.addAll(itemSet);

            for (int j = 0; j < sList.size(); j++) {
                itemArray[j] = AllItemList.get(list.get(j)-1);
            }

        }

        // 交叉变异的针对的是题目,展现的形式的试卷   即题目=基因、试卷=个体
        // 父代变异产生新个体c1 本质:替换其中一道题目
        ArrayList<String[]> cList = mutate(itemArray);

        // 为c1从当前种群中随机选取 c*w 个体  2个小生境  4元锦标赛
        // 是否是4元锦标赛过大，待后续优化
        ArrayList<Map<Integer, String[]>[]> cwList = championshipOut(outCross);

        // 替换，并返回索引
        String[] c1 = closestResemble(cList, cwList);

        ArrayList<Object> result = new ArrayList<>(1);

        // 返回的对象需要修改  对象 paperGenetic 转 string
        result.add(c1);

        return  result;

    }


    /** 限制性锦标赛选择算法 restricted tournament selection */
    public void  RTS2(String[][] paperGenetictmp) throws SQLException {
        //赋值给全局变量
        paperGenetic = paperGenetictmp;

        //有放回的随机选取两个父代个体p1 p2
        Random random = new Random();
        String[] p1 = paperGenetic[random.nextInt(100)];
        String[] p2 = paperGenetic[random.nextInt(100)];


        //父代交叉、变异，产生新个体c1/c2
        //ArrayList<String[]> cList = crossMutate(p1, p2);
        //父代变异产生新个体c1/c2
        ArrayList<String[]> cList = mutate(p1);


        //分别为c1/c2从当前种群中随机选取c*w个体  9个小生境  10元锦标赛
        //ArrayList<Map<Integer, String[]>[]> cwList = championship();

        //替换
        //closestResemble(cList,cwList);

    }



    /**
     * 如果f(c1)>f(d1),则用c1替换d1,否则保留d1;
     * 如果f(c2)>f(d2),则用c2替换d2,否则保留d2;
     *
     *      表现型  适应度值，或者 minAdi
     *      基因型  解(2,3,56,24,4,6,89,98,200,23)
     *
     */
    private String[] closestResemble(ArrayList<String[]> cList, ArrayList<Map<Integer, String[]>[]> cwList) {

        String[] c1 = cList.get(0);

        Map<Integer, String[]>[] cw1 = cwList.get(0);

        // 选取表现型做相似性校验
        String[] r1 = similarPhen(c1, cw1);

        // 选取基因型做相似性校验
        //similarGene(c1,cw1);

        return r1;

    }


    /**
     * 如果f(c1)>f(d1),则用c1替换d1,否则保留d1;
     * 如果f(c2)>f(d2),则用c2替换d2,否则保留d2;
     *
     */
    private void closestResemble2(ArrayList<String[]> cList, ArrayList<Map<Integer, String[]>[]> cwList) {
        //  表现型  适应度值，或者minAdi
        //  基因型  解(2,3,56,24,4,6,89,98,200,23)
        String[] c1 = cList.get(0);
        String[] c2 = cList.get(1);

        Map<Integer, String[]>[] cw1 = cwList.get(0);
        Map<Integer, String[]>[] cw2 = cwList.get(1);

        // 选取表现型做相似性校验
        similarPhen(c1,cw1);
        similarPhen(c2,cw2);


        //选取基因型做相似性校验
        //similarGene(c1,cw1);


    }


    /**
     * 在cw1中寻找c1的近似解  2个小生境  10元锦标赛  c1是一套试卷  cw1是c*w套试卷
     * 根据adi来找出最相似的值 返回索引，替换全局基因
     *      adi：适应度值*exp惩罚系数
     *      FIXME  为什么通过表现型ADI来判断相似性呢？
     *
     *
     *
     * 替换的是minPhen,属性校验的也应该是minPhen
     * 解决方案：①将minPhen返回，ADI层进行修补
     *         为了节省内存开销，采取第一种方案
     *
     */
    private String[] similarPhen(String[] c1, Map<Integer, String[]>[] cw1) {

        double minADI = getMinADI(c1);
        double min = 9999;
        int minPhen = 0;

        // 外层C小生境数，内层W元锦标赛
        for (Map<Integer, String[]> aCw11 : cw1) {
            // cwList.get(0)[1].get(2)
            String[] itemArray;
            for (int j = 0; j < aCw11.size(); j++) {
                // map的每个value,直接赋值给数组,拿ADI求出相似个体 57:FILL:(0,0,1,0,1):0.0:0.0:0.18002:0.0:0.0174
                for (Object o : aCw11.keySet()) {
                    int key = (int) o;
                    itemArray = aCw11.get(key);
                    // 获取最相似的解 key
                    double abs = Math.abs(minADI - getMinADI(itemArray));
                    if (min > abs) {
                        min = abs;
                        minPhen = key;
                    }
                }
            }
        }


        //System.out.println("最相似的个体为："+minPhen + cw1[0].get(minPhen));

        // 替换c1 将abs放开，直接替换呢？看看是否还会导致大面积相似
        //if (minADI - getMinADI(paperGenetic[minPhen])<0){
            //判断哪个多元小生境环境下存在最相似的个体  contain
            boolean flag = true;
            for (Map<Integer, String[]> aCw1 : cw1) {
                if (aCw1.get(minPhen) != null && flag) {
                    // 替换  替换的全局变量需做修改
                    cw1[0].put(minPhen, c1);
                    c1 = cw1[0].get(minPhen);
                    flag = false;
                }
            }
        //}

        return c1;

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
     * 获取min adi
     * 进行乘以一个exp 来进行适应度值的降低    高等数学里以自然常数e为底的指数函数
     * 题型比例 选择[0.2,0.4]  填空[0.2,0.4]  简答[0.1,0.3]  应用[0.1,0.3]
     * 属性比例 第1属性[0.2,0.4]   第2属性[0.2,0.4]   第3属性[0.1,0.3]  第4属性[0.1,0.3]  第5属性[0.1,0.3]
     */
    private double getMinADI(String[] c1){

        double adi1r =0;
        double adi2r =0;
        double adi3r =0;
        double adi4r =0;
        double adi5r =0;

        String [] itemList = c1;
        for (int j = 0; j < itemList.length; j++) {

            String[] splits = itemList[j].split(":");
            adi1r = adi1r + Double.parseDouble(splits[3]);
            adi2r = adi2r + Double.parseDouble(splits[4]);
            adi3r = adi3r + Double.parseDouble(splits[5]);
            adi4r = adi4r + Double.parseDouble(splits[6]);
            adi5r = adi5r + Double.parseDouble(splits[7]);

        }


        // 题型个数
        String [] expList = c1;
        int typeChose  = 0;
        int typeFill   = 0;
        int typeShort  = 0;
        int typeCompre = 0;


        for (String s:expList) {

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


        // 属性个数
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


        // 惩罚个数  只有比例不符合要求时才惩罚，故不会有太大的影响
        double expNum = -(td1 + td2 + td3 + td4 + ed1 + ed2 + ed3 + ed4 + ed5);


        //最小值
        double minrum = Math.min(Math.min(Math.min(Math.min(adi1r,adi2r),adi3r),adi4r),adi5r) * 100 ;

        ////System.out.println("minrum: "+minrum);

        //适应度值 (min * 惩罚系数)
        minrum = minrum * Math.exp(expNum);

        return minrum;

    }

    /**
     * 使用基因型做相似性判断
     *
     */
    private void similarGene(String[] c1, ArrayList[] cw1){

        HashSet c1Set = new HashSet<>(10);

        //获取c1 c2 的基因型  list.add(id+":"+type+":"+attributes)
        for (String s : c1) {
            String id = s.split(":")[0];
            c1Set.add(id);
        }

        //在cw1中寻找c1的近似解
        for (int i = 0; i < cw1.length; i++) {

            ArrayList cw = cw1[i];
            HashSet<String> idSet = new HashSet<>(10);

            for (int j = 0; j < cw.size(); j++) {
                String id = cw.get(j).toString().split(":")[0];
                idSet.add(id);
            }
            //计算相似性,并保存最优解  大面积出现相似性为0或者1 的情况
            idSet.retainAll(c1Set);
            //System.out.println("t"+i+" : "+idSet.size());
        }
        //System.out.println("********************");
    }

    /**
     *  分别为c1从当前小生境中随机选取c*w个体
     *  种群: 2*10<=20（不同小生境存在重复+交叉变异）
     *
     */
    private ArrayList<Map<Integer, String[]>[]> championshipOut(ArrayList<String> outCross) throws SQLException {

        //2个小生境,4元锦标赛, 此处修改为了1
        int num = 1 ;
        int nv = outCross.size()>=2?2:1 ;
        Map<Integer, String[]>[] cwList1 = new HashMap[num];


        // 基本单位:试卷。故随机生成一个下标 (需保存下标,方便后续替补 map(k,v))
        // 数组 map
        for (int i = 0; i < num; i++) {
            Set<String> set1 = new HashSet<>();
            // 将个体保存为map结构
            Map<Integer, String[]> mapc1w = new HashMap<>(nv);
            while (set1.size() != nv) {
                //  从同属于某一小生境中选取不同的个体(如果size小于N元锦标赛，就会陷入死循环，待优化)
                int i1 = new Random().nextInt(outCross.size());
                if (!set1.contains(":"+i1)) {
                    set1.add(":"+i1 );
                    System.out.println(" --> 验证卡死现象3.0 <-- ");
                    String s = outCross.get(i1);
//                    // 查询数据库
//                    ArrayList<String> bachItemList = jdbcUtils.selectBachItem(s);
//
//                    //短暂休眠，保证连接缓冲
//                    try {
//                        Thread.sleep(50);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                    String[] itemArray = new String[bachItemList.size()];
//                    for (int j = 0; j < bachItemList.size(); j++) {
//                        itemArray[j] = bachItemList.get(j);
//                    }
//
//                    mapc1w.put(i1,itemArray);

                    // 通过遍历s,来获取itemArray
                    List<String> sList = Arrays.asList(s.split(","));
                    String[] itemArray = new String[sList.size()];
                    if(!s.contains("_")){
                        for (int j = 0; j < sList.size(); j++) {
                            itemArray[j] = AllItemList.get(Integer.parseInt(sList.get(j))-1 > -1?Integer.parseInt(sList.get(j))-1:1);
                        }
                    }
                    mapc1w.put(i1,itemArray);


                }
                /**
                 * FIXME  某一个变异就进入了死循环
                 */
                cwList1[i] = mapc1w;
            }
        }

        ArrayList<Map<Integer, String[]>[]> cwList = new ArrayList<>(1);
        cwList.add(cwList1);
        // 获取个体的方法:   cwList.get(0)[1].get(2)
        return cwList;

    }



    /**
     *  分别为c1/c2从当前种群中随机选取c*w个体
     *  当前种群和题库的关系
     *  题库: 310 道题
     *  种群: 100*10<=1000（存在重复+交叉变异）
     *
     */
    private ArrayList<Map<Integer, String[]>[]> championship2()  {

        //9个小生境  10元锦标赛
        int num = 9 ;
        Map<Integer, String[]>[] cwList1 = new HashMap[num];
        Map<Integer, String[]>[] cwList2 = new HashMap[num];


        //基本单位:试卷。故随机生成一个下标即可 (需保存下标,方便后续替补 map(k,v))
        //数组 map
        for (int i = 0; i < num; i++) {
            Set<String> set1 = new HashSet<>();
            Set<String> set2 = new HashSet<>();
            //ArrayList<String> c1w = new ArrayList<>(10);
            //ArrayList<String> c2w = new ArrayList<>(10);
            // 将个体保存为map结构
            Map<Integer, String[]> mapc1w = new HashMap<>(10);
            Map<Integer, String[]> mapc2w = new HashMap<>(10);
            while (set1.size() != 10) {
                int i1 = new Random().nextInt(paperGenetic.length);
                if (!set1.contains(":"+i1)) {
                    set1.add(":"+i1 );
                    //String s = ArrayUtils.toString(paperGenetic[i1])+"_"+i1;
                    //c1w.add(s);
                    mapc1w.put(i1,paperGenetic[i1]);
                }
                cwList1[i] = mapc1w;
            }
            while (set2.size() != 10) {
                int i1 = new Random().nextInt(paperGenetic.length);
                if (!set2.contains(":"+i1)) {
                    set2.add(":"+i1 );
                    //String s = ArrayUtils.toString(paperGenetic[i1])+"_"+i1;
                    //c2w.add(s);
                    mapc2w.put(i1,paperGenetic[i1]);
                }
                cwList2[i] = mapc2w;
            }
        }

//        for (int i = 0; i < num; i++) {
//            Set<String> set1 = new HashSet<>();
//            Set<String> set2 = new HashSet<>();
//            ArrayList<String> c1w = new ArrayList<>(10);
//            ArrayList<String> c2w = new ArrayList<>(10);
//
//            while (set1.size() != 10) {
//                int i1 = new Random().nextInt(paperGenetic.length);
//                int j1 = new Random().nextInt(paperGenetic[0].length);
//                if (!set1.contains(i1 + ":" + j1)) {
//                    set1.add(i1 + ":" + j1);
//                    String s = paperGenetic[i1][j1]+":"+i1 + ":" + j1;
//                    c1w.add(s);
//                }
//                cwList1[i] = c1w;
//            }
//
//            while (set2.size() != 10) {
//                int i1 = new Random().nextInt(paperGenetic.length);
//                int j1 = new Random().nextInt(paperGenetic[0].length);
//                if (!set2.contains(i1 + ":" + j1)) {
//                    set2.add(i1 + ":" + j1);
//                    String s = paperGenetic[i1][j1];
//                    c2w.add(s);
//                }
//                cwList2[i] = c2w;
//            }
//        } Map<Integer, String[]>[]

        ArrayList<Map<Integer, String[]>[]> cwList = new ArrayList<>(2);
        cwList.add(cwList1);
        cwList.add(cwList2);
        // 获取个体的方法:   cwList.get(0)[1].get(2)
        return cwList;

    }




    /**
     *  即使p1/p2相同,交叉无效,但可进一步通过变异获得c1/c2个体
     *  单点交叉 + 随机变异
     *
     */
    private ArrayList<String[]> crossMutate(String[] p1, String[] p2) throws SQLException {
        //  单点交叉
        int length = 10;
        int a = new Random().nextInt(length);
        String [] c1 = new String[length];
        String [] c2 = new String[length];

        if (a >= 0) {
            System.arraycopy(p1, 0, c1, 0, a);
        }

        if (length - a >= 0) {
            System.arraycopy(p2, a, c1, a, length - a);
        }

        if (a >= 0) {
            System.arraycopy(p2, 0, c2, 0, a);
        }

        if (length - a >= 0) {
            System.arraycopy(p1, a, c2, a, length - a);
        }

        //c1变异
        Random random = new Random();
        int mutatePoint = random.nextInt(length-1);
        //将Array 转 hashSet  去除重复的元素了,有效，但需向上核实为什么会重复？
        Set<String> set = new HashSet<>(Arrays.asList(c1));

        //将要变异的元素
        String s = c1[mutatePoint];
        set.remove(s);
        int removeId = Integer.parseInt(s.split(":")[0]);

        //试卷临时存储容器
        String[] c11 = new String[length];

        //生成一个不存在set中的key  新增不同的元素了,有效，交叉本身就会导致基因size丢失
        while (set.size() != length ){
            String key = random.nextInt(310)+1+"";
            if (!(key+"").equals(removeId+"")){
                ArrayList<String> list = jdbcUtils.selectBachItem(key);
                set.add(list.get(0)+"");
            }
        }
        set.toArray(c11);

        //c2变异
        int mutatePoint2 = random.nextInt(length-1);
        //将Array 转 hashSet
        Set<String> set2 = new HashSet<>(Arrays.asList(c2));

        //将要变异的元素
        String s2 = c2[mutatePoint2];
        set2.remove(s2);
        int removeId2 = Integer.parseInt(s2.split(":")[0]);

        //试卷临时存储容器
        String[] c21 = new String[length];

        //生成一个不存在set中的key
        while (set2.size() != length ){
            String key = random.nextInt(310)+1+"";
            if (!(key+"").equals(removeId2+"")){
                ArrayList<String> list = jdbcUtils.selectBachItem(key);
                set2.add(list.get(0)+"");
            }
        }
        set2.toArray(c21);


        ArrayList<String[]> cList = new ArrayList<>(2);
        cList.add(c11);
        cList.add(c21);
        return  cList;

    }



    /**
     *  通过变异获得c1个体   怕p1--> c1
     *  随机变异,去除某一道题，然后从题库中抽取,新增一道题
     *
     */
    private ArrayList<String[]> mutate(String[] c1) throws SQLException {

        //基因长度 | 试题个数
        int length = 20;
//      String [] c1 = p1;

        //c1变异
        Random random = new Random();
        int mutatePoint = random.nextInt(length-1);
        //将Array 转 hashSet  去除重复的元素了,有效，但需向上核实为什么会重复？
        Set<String> set = new HashSet<>(Arrays.asList(c1));

        //将要变异的元素
        String s = c1[mutatePoint];
        set.remove(s);
        int removeId = Integer.parseInt(s.split(":")[0]);

        //临时存储容器
        String[] c11 = new String[length];

        //生成一个不存在set中的key  保证题型长度符合要求
        while (set.size() != length ){
            System.out.println(" --> 验证卡死现象2.0 <--");
            String key = random.nextInt(310)+"";
            if (!(key+"").equals(removeId+"")){
                // list.add(id+":"+type+":"+pattern+":"+adi1_r+":"+adi2_r+":"+adi3_r+":"+adi4_r+":"+adi5_r)
//                ArrayList<String> list = jdbcUtils.selectBachItem(key);
                //短暂休眠，保证连接缓冲
//                try {
//                    Thread.sleep(50);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                set.add(list.get(0)+"");
                String s1 = AllItemList.get(Integer.parseInt(key));
                set.add(s1);

            }
        }
        set.toArray(c11);
        //排序
        new ADIController6().sortPatch(c11);


        ArrayList<String[]> cList = new ArrayList<>(1);
        cList.add(c11);
        return  cList;

    }



    /**
     *  即使p1/p2相同,交叉无效,但可进一步通过变异获得c1/c2个体
     *  单点交叉 + 随机变异
     *
     */
    private ArrayList<String[]> crossMutateDet(String[] p1, String[] p2) throws SQLException {
        //  单点交叉
        int length = 10;
        int a = new Random().nextInt(length);
        String [] c1 = new String[length];
        String [] c2 = new String[length];

        if (a >= 0) {
            System.arraycopy(p1, 0, c1, 0, a);
        }

        if (length - a >= 0) {
            System.arraycopy(p2, a, c1, a, length - a);
        }

        if (a >= 0) {
            System.arraycopy(p2, 0, c2, 0, a);
        }

        if (length - a >= 0) {
            System.arraycopy(p1, a, c2, a, length - a);
        }

        //c1变异
        Random random = new Random();
        int mutatePoint = random.nextInt(length-1);
        //将Array 转 hashSet  去除重复的元素了,有效，但需向上核实为什么会重复？
        Set<String> set = new HashSet<>(Arrays.asList(c1));

        //将要变异的元素
        String s = c1[mutatePoint];
        set.remove(s);
        int removeId = Integer.parseInt(s.split(":")[0]);

        //试卷临时存储容器
        String[] c11 = new String[length];

        //生成一个不存在set中的key  新增不同的元素了,有效，交叉本身就会导致基因size丢失
        while (set.size() != length ){
            String key = random.nextInt(310)+1+"";
            if (!(key+"").equals(removeId+"")){
                ArrayList<String> list = jdbcUtils.selectBachItem(key);
                set.add(list.get(0)+"");
            }
        }
        set.toArray(c11);

        //c2变异
        int mutatePoint2 = random.nextInt(length-1);
        //将Array 转 hashSet
        Set<String> set2 = new HashSet<>(Arrays.asList(c2));

        //将要变异的元素
        String s2 = c2[mutatePoint2];
        set2.remove(s2);
        int removeId2 = Integer.parseInt(s2.split(":")[0]);

        //试卷临时存储容器
        String[] c21 = new String[length];

        //生成一个不存在set中的key
        while (set2.size() != length ){
            String key = random.nextInt(310)+1+"";
            if (!(key+"").equals(removeId2+"")){
                ArrayList<String> list = jdbcUtils.selectBachItem(key);
                set2.add(list.get(0)+"");
            }
        }
        set2.toArray(c21);


        ArrayList<String[]> cList = new ArrayList<>(2);
        cList.add(c11);
        cList.add(c21);
        return  cList;

    }


    /**
     *
     */
    private void closestResembleDet(ArrayList<Integer> iList,ArrayList<String[]> cList, ArrayList<String[]> pList) {
        //  表现型  适应度值，或者minAdi
        //  基因型  解(2,3,56,24,4,6,89,98,200,23)
        int i1 = iList.get(0);
        int i2 = iList.get(1);



        String[] c1 = cList.get(0);
        String[] c2 = cList.get(1);

        String[] p1 = pList.get(0);
        String[] p2 = pList.get(1);

        // 选取表现型做相似性校验
        similarPhenDet(i1,i2,c1,c2,p1,p2);


        //选取基因型做相似性校验
        //similarGene(c1,cw1);

    }


    /**
     *   3.1 如果[d(p1,c1)+d(p2,c2)]<=[d(p1,c2)+d(p2,c1)]
     *          如果f(c1)>f(p1),则用c1替换p1,否则保留p1;
     *          如果f(c2)>f(p2),则用c2替换p2,否则保留p2;
     *   3.2 否则
     *          如果f(c1)>f(p2),则用c1替换p2,否则保留p2;
     *          如果f(c2)>f(p1),则用c2替换p1,否则保留p1;
     *
     */
    private void similarPhenDet(int i1,int i2,String[] c1, String[] c2,String[] p1,String[] p2) {

        double minADIC1 = getMinADI(c1);
        double minADIC2 = getMinADI(c2);
        double minADIP1 = getMinADI(p1);
        double minADIP2 = getMinADI(p2);


        //距离计算
        if(Math.abs(minADIC1 - minADIP1) +Math.abs(minADIC2 - minADIP2) <= Math.abs(minADIC1 - minADIP2) +Math.abs(minADIC2 - minADIP1)){
            System.out.println("距离1小,选择分支1");
            //判断适应度值，执行替换
            if(minADIC1 > minADIP1){
                paperGenetic[i1] = c1;
            }
            if(minADIC2 > minADIP2){
                paperGenetic[i2] = c2;
            }
        }else{
            System.out.println("距离2小,选择分支2");
            //判断适应度值，执行替换
            if(minADIC1 > minADIP2){
                paperGenetic[i2] = c1;
            }
            if(minADIC2 > minADIP1){
                paperGenetic[i1] = c2;
            }
        }

    }





}



