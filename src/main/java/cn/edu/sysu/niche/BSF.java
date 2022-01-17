package cn.edu.sysu.niche;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @Author : song bei chang
 * @create 2022/1/10 22:36
 */
public class BSF {


    private Logger log = Logger.getLogger(BSF.class);

    /**
     * 每一代保留最优的个体 best so far
     * (并集  去重  截断)
     */
    ArrayList<String> bsf = new ArrayList<>();


    /**
     * set存leader  for  bsf
     */
    private HashSet<String> leaderSetForBSF = new HashSet();

    /**
     * map(key是string存小生境leader, value是list存小生境member) for bsf
     */
    private HashMap<String, ArrayList<String>> mapArrayListForBSF = new HashMap<>();


    /**
     * 比较器
     */
    Comparator comp = new MyComparator();



    /**
     * 1.bsf取并集
     * 2.去重
     * 3.niche
     * 4.cut_off
     * 5.收敛规则
     */
    public Boolean deterministicConvergence(int iter , ArrayList<String> sortListForGene) {


        // 同一个内存地址,会将数据覆盖掉。故需采取遍历赋值，生成新的内存地址
        ArrayList<String> bsfV1 = new ArrayList<>();
        bsfV1.clear();
        for (int i = 0; i < bsf.size(); i++) {
            bsfV1.add(bsf.get(i));
        }


        // bsf 取并集
        bsf.addAll(sortListForGene);

        Boolean timeFlag = false;

        // 去重操作
        if (bsf.size() > 100) {

            // 去重
            Set set = new HashSet(bsf);

            bsf = new ArrayList(set);

            // 排序
            System.out.println(bsf.size() + " <---------- ");
            Collections.sort(bsf, comp);

            // niche(划分)
            distributeNicheForBSF();

            // niche(cutoff  这部分的得到的bsf size 肯定是100)
            gtPart(mapArrayListForBSF);

            // 收敛规则(计算变化趋势 bsfV1 vs bsf)
            if( iter < 500 ){
                timeFlag = calSim(bsf, bsfV1);
            }else {
                timeFlag = true;
            }

        } else {

            System.out.println("验证是否个体很久没变化了");

        }

        return timeFlag;

    }

    /**
     * 比对两个集合的相似题目数
     *
     */
    private Boolean calSim(ArrayList<String> bsf, ArrayList<String> bsfV1) {

        // 需使用 bsfV1 contain bsf,否则会导致数据丢失(bsf 是全局变量,同一内存地址)
        bsfV1.retainAll(bsf);
        log.info(bsfV1.size());

        // 收敛确认规则(定时器)
        Boolean timeFlag = registerTimeTimer(bsfV1.size());

        return timeFlag;

    }

    /**
     * 如果连续15代,相似个数均大于90,则认为其是相似的，此时
     * 1.将数据写入
     * 2.中断程序
     *
     */
    int lastCount = 0;
    int maxCount = 15;
    int judgmentBasis = 90;

    private Boolean registerTimeTimer(int size) {

        Boolean timeFlag = false;

        if (size >= judgmentBasis) {
            lastCount ++;
            if (lastCount > maxCount){
                timeFlag =  true;
            }
        } else {
            lastCount = 0;
        }
        System.out.println(" 不断的尝试进行 终止判断");

        return timeFlag;

    }


    /**
     * 划分为不同的小生境  mapArrayListForBSF
     * 1.选择leader
     * 2.选择member
     */
    private void distributeNicheForBSF() {

        // 选取leader
        for (int i = 0; i < bsf.size(); i++) {

            // 选择第一个小生境leader
            if (leaderSetForBSF.size() == 0) {

                leaderSetForBSF.add(bsf.get(i));

            } else {

                String aids = bsf.get(i).split("_")[1];

                // 使用一个计数器,比对两个集合的相似题目数
                int max = 0;
                // 获取目前leader的信息
                for (String leader : leaderSetForBSF) {

                    // b 的判断应该和其余全部的leader进行判断
                    String bids = leader.split("_")[1];

                    // 判断两套试卷的相似程度,如果相似题目数达到一定数目，则判定为是同一个小生境 如3，
                    // 将基因型转为list,使用list来判断相似个数
                    List<String> ListA = stringToList(aids);
                    List<String> ListB = stringToList(bids);


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
                    leaderSetForBSF.add(bsf.get(i));
                }
            }
        }

        // 选取member leaderSetForGene 表示小生境的峰值
        int sum = 0;
        //log.info("小生境数目: " + leaderSetForGene.size());

        for (String leader : leaderSetForBSF) {

            ArrayList<String> memberList = new ArrayList<>();
            String aids = leader.split("_")[1];

            for (int i = 0; i < bsf.size(); i++) {
                String s = bsf.get(i);
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
                        bsf.set(i, "");
                    }
                }
            }

            mapArrayListForBSF.put(leader, memberList);

            sum = memberList.size() + sum;
        }
        System.out.println("此次迭代个体总数目BSF：" + sum);


        log.info("小生境个数BSF: " + mapArrayListForBSF.size());
    }


    /**
     * 按照比例取前100个个体
     */
    private void gtPart(HashMap<String, ArrayList<String>> mapArrayListForBSF) {

        // 1. 将size大小塞入集合中
        ArrayList<Integer> sizeList = new ArrayList<>();
        ArrayList<String> keyList = new ArrayList<>();
        int sum = 0;

        for (Map.Entry<String, ArrayList<String>> entry : mapArrayListForBSF.entrySet()) {

            int size = entry.getValue().size();

            sum = sum + size;

            sizeList.add(size);
            keyList.add(entry.getKey());

        }


        // 2. 计算比值
        ArrayList<Double> sizeRateList = new ArrayList<>();
        ArrayList<Integer> tempSizeList = new ArrayList<>();

        for (Integer integer : sizeList) {
            double v = ((double) (integer)) / sum;
            sizeRateList.add(v);
            tempSizeList.add((int) Math.ceil(100 * v));
        }

        // 对最后一个个体进行数字处理
        ArrayList<Integer> finSizeList = new ArrayList<>();
        int sumV2 = 0;
        for (int i = tempSizeList.size() - 1 ; i >0 ; i--) {

            sumV2 = sumV2 + tempSizeList.get(i);
            //finSizeList.add(tempSizeList.get(i));
            if (sumV2 >= 100){
                break;
            }

        }
        //finSizeList.add(tempSizeList.get(tempSizeList.size() - 1));

        // 不能添加所有,只能按需添加
        finSizeList.addAll(tempSizeList);
        if (sumV2 < 100){
            finSizeList.set(0, 100 - sumV2);
        }else{
            // 第一个小生境进行缩减
            int i = tempSizeList.get(tempSizeList.size() - 1) - (sumV2 - 100);
            finSizeList.set(tempSizeList.size() - 1, i);
        }

        int tmpSum = 0;
        for (Integer integer : finSizeList) {
            tmpSum = tmpSum + integer;
        }
        System.out.println(tmpSum);

        // 拼接key 和 size 使其成为 map,进行映射
        HashMap<String, Integer> siMap = new HashMap<String, Integer>();
        for (int i = 0; i < finSizeList.size(); i++) {
            siMap.put(keyList.get(i), finSizeList.get(i));
        }


        ArrayList<String> bsfTmp = new ArrayList<>();
        // 3.按大小取出前100个个体  map存在问题，顺序无法保证
        for (Map.Entry<String, ArrayList<String>> entry : mapArrayListForBSF.entrySet()) {

            // 大小
            Integer size = siMap.get(entry.getKey());
            ArrayList<String> valueList = entry.getValue();
            Collections.sort(valueList, comp);
            if (valueList.size() > 0 && size > 0) {

                for (int i = 0; i < size; i++) {
                    bsfTmp.add(valueList.get(i));
                }

            }

        }

        bsf = bsfTmp;

        sizeRateList.clear();
        sizeList.clear();


    }


    /**
     * string 转 list
     */
    private List<String> stringToList(String strs) {
        String str[] = strs.split(",");
        return Arrays.asList(str);
    }


}



