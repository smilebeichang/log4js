package cn.edu.sysu.controller;

import cn.edu.sysu.pojo.Papers;

import java.util.*;

/**
 * @author Created by songb
 */
public class SimpleProcess {

    private static Integer MAX = 40;
    private static Integer TYPE_CHOICE = 20;
    private static Integer TYPE_FILL  = 10;
    private static Integer TYPE_SUMMARY  = 10;

    private static Integer ACT_CHOICE = 10;
    private static Integer ACT_FILL  = 5;
    private static Integer ACT_SUMMARY  = 5;
    private static Integer[][] paper_genetic =new Integer[10][20];

    public static void main(String[] args) {

        Papers papers = new Papers();
        papers.setPaperSize(10);
        papers.setQuestSize(20);
        papers.setPc(1);
        papers.setPm(1);
        init(papers);
        crossCover(papers);
        mutate(papers);

    }



    /**
     * 随机生成初代的实数编码的种群
     */
    public static void  init(Papers papers) {
        System.out.println("=== init begin ===");
        for (int i = 0; i < papers.getPaperSize(); i++) {
            //随机生成基因序列
            Integer[] testGene= new Integer[papers.getQuestSize()];
            Set<Integer> set = new HashSet<Integer>();
            for(int j = 0; j < ACT_CHOICE; j++){
                //保证题目不重复,且满足题型约束
                while (set.size() == j ){
                    Integer key = new Random().nextInt(TYPE_CHOICE);
                    set.add(key);
                }
            }
            for(int j = 0; j < ACT_FILL; j++){
                while (set.size()-ACT_CHOICE == j ){
                    Integer key = new Random().nextInt(TYPE_FILL)+TYPE_CHOICE;
                    set.add(key);
                }
            }
            for(int j = 0; j < ACT_SUMMARY; j++){
                while (set.size()-ACT_CHOICE-ACT_FILL == j ){
                    Integer key = new Random().nextInt(TYPE_SUMMARY)+TYPE_FILL+TYPE_CHOICE;
                    set.add(key);
                }
            }
            set.toArray(testGene);
            Arrays.sort(testGene);
            System.out.println(i+" 初始化："+Arrays.toString(testGene));
            paper_genetic[i] = testGene;
        }
        System.out.println("=== init end ===");

    }


    /**
     * 交叉
     */
    public static void crossCover(Papers papers){
        System.out.println("=== crossCover begin ===");
        Integer point = papers.getQuestSize();
        for (int i = 0; i < papers.getPaperSize()-1; i++) {
            if (Math.random() < papers.getPc()) {
                //单点交叉
                Integer[] temp1 = new Integer[point];
                int a = new Random().nextInt(point);

                for (int j = 0; j < a; j++) {
                    temp1[j] = paper_genetic[i][j];
                }
                for (int j = a; j < point; j++) {
                    temp1[j] = paper_genetic[i+1][j];
                }
                //执行修补操作
                correct(i,temp1);
            }
        }
        System.out.println("=== crossCover end ===");
    }

    /**
     * 判断size，执行修补操作
     */
    public static void correct(int i,Integer[] temp1) {

        Set<Integer> set_begin = new HashSet<Integer>(Arrays.asList(temp1));
        Set<Integer> set_end = new HashSet<Integer>();
        Set<Integer> set_choice = new HashSet<Integer>();
        Set<Integer> set_fill = new HashSet<Integer>();
        Set<Integer> set_summary = new HashSet<Integer>();
        int size = set_begin.size();
        int num_choice = 0;
        int num_fill = 0;
        int num_summary = 0;
        if (size == 20){
            System.out.println(i+ " 正常交叉,无需处理");
        }else{
            System.out.println(i+ " 交叉导致类型不匹配： "+set_begin.size());

            //分别将三张类型的数量进行统计
            Iterator<Integer> it = set_begin.iterator();
            while (it.hasNext()) {
                Integer num =  it.next();
                if (num<20){
                    num_choice = num_choice+1;
                    set_choice.add(num);
                }else if (num < 30){
                    num_fill = num_fill+1;
                    set_fill.add(num);
                }else if(num < 40){
                    num_summary = num_summary+1;
                    set_summary.add(num);
                }
            }
            System.out.println("  choice: "+num_choice+" fill: "+num_fill+" summary: "+num_summary);

            if(num_choice<10){
                while(set_choice.size() != 10){
                    Integer key = new Random().nextInt(20);
                    set_choice.add(key);
                }
            }

            if(num_fill<5){
                while(set_fill.size() != 5){
                    Integer key = Math.abs(new Random().nextInt()) % 10 + 20;
                    set_fill.add(key);
                }
            }

            if(num_summary<5){
                while(set_summary.size() != 5){
                    Integer key = Math.abs(new Random().nextInt()) % 10 + 30;
                    set_summary.add(key);
                }
            }


            set_end.addAll(set_choice);
            set_end.addAll(set_fill);
            set_end.addAll(set_summary);
            set_end.toArray(temp1);
            Arrays.sort(temp1);
            paper_genetic[i]=temp1;
        }
        System.out.println("  "+Arrays.toString(paper_genetic[i]));
    }


    /**
     * 变异
     */
    public static void mutate(Papers papers){
        System.out.println("=== mutate begin ===");
        Integer key = 0;
        for (int i = 0; i < papers.getPaperSize(); i++) {
            if(Math.random() < papers.getPm()){
                Random random = new Random();
                int mutate_point = random.nextInt(papers.getQuestSize()-1);
                Set<Integer> set = new HashSet<Integer>(Arrays.asList( paper_genetic[i]));
                Integer s = paper_genetic[i][mutate_point];

                System.out.println(i+" 原试卷: "+set);
                System.out.println("  remove element: "+ s);
                set.remove(s);
                System.out.println("  临时试卷： "+set);

                Integer[] temp1 = new Integer[20];

                if (mutate_point<10){
                    //生成一个合适的且不存在set中的key
                    while (set.size() != 20 ){
                        key = random.nextInt(20);
                        if (!key.equals(s)){
                            set.add(key);
                        }
                    }
                }else if(mutate_point<15){
                    while (set.size() != 20 ){
                        key = Math.abs(new Random().nextInt()) % 10 + 20;
                        if (!key.equals(s)){
                            set.add(key);
                        }
                    }
                }else if(mutate_point<20){
                    while (set.size() != 20 ){
                        key = Math.abs(new Random().nextInt()) % 10 + 30;
                        if (!key.equals(s)){
                            set.add(key);
                        }
                    }
                }
                set.toArray(temp1);
                Arrays.sort(temp1);
                paper_genetic[i]=temp1;
            }
            System.out.println("  add element: "+ key);
            System.out.println("  最终试卷： "+Arrays.toString(paper_genetic[i]));
            System.out.println();
        }
        System.out.println("=== mutate end ===");
    }

}


