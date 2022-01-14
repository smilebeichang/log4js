package cn.edu.sysu.niche;

import cn.edu.sysu.adi.TYPE;
import cn.edu.sysu.clique.MaxcliqueV2;
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
 * @create : 2022/01/13 00:11
 *
 * 模拟随机的样式
 *
 */
public class DNDR10_Random {


    /**
     * 50套试卷 20道题
     */
    private static String[][] paperGenetic = new String[50][20];


    Random rand = new Random();

    /**
     * 生成 paperGenetic  = new String[100*100][20] 试卷100*100套,每套20题  为交叉变异提供原始材料
     *
     */
    private void initItemBank() throws InterruptedException {

        System.out.println("====== 开始选题,构成试卷  轮盘赌构造 ======");

        // 试卷|个体大小  提供遗传变异的大单位
        int paperNum = paperGenetic.length;

        // 题目|基因大小  交叉变异的基本单位
        int questionsNum = paperGenetic[1].length;

        // 单套试卷的集合
        HashSet<String> itemSet = new HashSet<>();


        // 生成了二维数组 paperGenetic
        for (int j = 0; j < paperNum; j++) {

            // 清空上一次迭代的数据
            itemSet.clear();

            for (int i = 0; i < questionsNum; i++) {

                // 去重操作
                while (itemSet.size() == i) {
                    // 获取题目id   轮盘赌构造法
                    int sqlId = rand.nextInt(309);
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
                //itemArray[k] = allItemList.get(Integer.parseInt(sList.get(k).trim())-1 > -1?Integer.parseInt(sList.get(k).trim())-1:1);
                itemArray[k] = sList.get(k);
            }


            // 赋值给全局变量 (容器：二维数组)
            paperGenetic[j] = itemArray;
            //Thread.sleep(20);
        }
    }








    /**
     * 主程序
     * 1. 初始化试卷
     * 2. 计算 max clique
     * 3. 画图
     */
    @Test
    public  void  main() throws InterruptedException, SQLException {


        // 1.初始化试卷(长度，题型，属性比例) String[][] paperGenetic = new String[200][20]
        initItemBank();

        System.out.println("---------------");


        // 2.数组转list
        ArrayList<String> paperList = new ArrayList<>();
        for (int i = 0; i < paperGenetic.length; i++) {

            List<String> listB= Arrays.asList(paperGenetic[i]);
            paperList.add("0_"+listB.toString().substring(1, listB.toString().length()-1));

        }

        System.out.println(paperList);

        new DNDR10().similarClique(paperList);

    }



}