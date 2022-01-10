package cn.edu.sysu.niche;

import cn.edu.sysu.adi.TYPE;
import cn.edu.sysu.utils.JDBCUtils4;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
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
     * size 为310  初始化,塞入到内存中
     */
    ArrayList<String> allItemList = new JDBCUtils4().selectAllItems();


    public BSF() throws SQLException {
    }


    /**
     * 1.bsf取并集
     * 2.去重
     * 3.niche
     * 4.cut_off
     */
    public void deterministicConvergence(ArrayList<String> sortListForGene) {

        // 将 sortListForGene 赋值给 bsf
        bsf.addAll(sortListForGene);

        // 第二代开始进行去重操作
        if (bsf.size() > 100) {

            // 排序
            System.out.println(bsf.size() + " <---------- ");
            Collections.sort(bsf, comp);

            // niche(划分)
            distributeNicheForBSF();

            // niche(cutoff)
            gtPart(mapArrayListForBSF);


        } else {

            System.out.println("验证是否个体很久没变化了");

        }


    }


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
            double v = (double) (integer) / sum;
            sizeRateList.add(v);
            tempSizeList.add((int) Math.ceil(100 * v));
        }

        // 对最后一个个体进行数字处理
        ArrayList<Integer> finSizeList = new ArrayList<>();
        int sumV2 = 0;
        for (int i = 0; i < tempSizeList.size() - 1; i++) {

            sumV2 = sumV2 + tempSizeList.get(i);

        }

        finSizeList.addAll(tempSizeList);
        finSizeList.set(tempSizeList.size() - 1, 100 - sumV2);

        // 拼接key 和 size 使其成为 map,进行映射
        HashMap<String, Integer> siMap = new HashMap<String, Integer>();
        for (int i = 0; i < finSizeList.size(); i++) {
            siMap.put(keyList.get(i), finSizeList.get(i));
        }


        ArrayList<String> bsfTmp = new ArrayList<>();
        // 3.按大小取出前100个个体  map存在问题，顺序无法保证
        for (Map.Entry<String, ArrayList<String>> entry : mapArrayListForBSF.entrySet()) {

            // -- 大小
            Integer size = siMap.get(entry.getKey());
            ArrayList<String> valueList = entry.getValue();
            Collections.sort(valueList, comp);
            if (valueList.size() > 0) {

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



