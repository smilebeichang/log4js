package cn.edu.sysu.controller;

import cn.edu.sysu.pojo.Papers;
import cn.edu.sysu.pojo.Questions;
import cn.edu.sysu.utils.JDBCUtils;

import java.math.BigDecimal;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 *
 * @Author : songbeichang
 * @create 2021/02/23 0:17
 */
public class SimpleProcess2 {

    /**
     * 初始化题库
     */
    private static Integer ATTRIBUTE_MAX = 3;
    private static Integer TYPE_CHOICE_NUM = 20;
    private static Integer TYPE_FILL_NUM  = 10;
    private static Integer TYPE_SUMMARY_NUM  = 10;

    /**
     * 单张试卷题目数量
     */
    private static Integer ACT_CHOICE_NUM = 10;
    private static Integer ACT_FILL_NUM  = 5;
    private static Integer ACT_SUMMARY_NUM  = 5;

    /**
     * 容器
     */
    static Questions[] questions =new Questions[40];
    static  double[] all_fitness =new double[40];

    static  double[]   paper_fitness =new double[10];
    static Integer[][] paper_genetic =new Integer[10][20];


    public static void main(String[] args) {

        Papers papers = new Papers();
        papers.setPaperSize(10);
        papers.setQuestSize(20);
        papers.setPc(0.5);
        papers.setPm(0.5);

        //原始方法
        //ori(papers);

        //初始化题库存到数据库
        initItemBank();

    }

    public static void ori(Papers papers) {

        //初始化题库
        initItemBank();
        //计算适应度值  ①什么时候计算                可以初始化时计算，但和被试掌握属性有关 ==》 交叉变异的时候计算
        //            ②计算单位（单套试卷/单个题目）  适应度值以单套试卷为单位，交叉变异也以试卷为单位 但目前，计算的适应度值都是以题目为单位。取平均值，是按属性取列值的平均（横坐标代表pattern  纵坐标如何理解呢）

        //calFitness(questions);
        init(papers);
        //getPaperFitness();
        for (int i = 0; i < 2; i++) {
            selection();
            crossCover(papers);
            mutate(papers);
            //小生境环境的搭建
            elitiststrategy();
        }
    }


    /**
     * 生成题库(类型、属性、id)
     */
    public static void initItemBank(){

        JDBCUtils jdbcUtils = new JDBCUtils();

        //选择题
        System.out.println("====== 选择题  ======");
        for (int i = 0; i < TYPE_CHOICE_NUM; i++) {
            Questions question = new Questions();
            String attributes ;
            int attNum = new Random().nextInt(ATTRIBUTE_MAX);
            Set<String> fill_set = new HashSet<>();
            for (int j = 0; j < attNum+1; j++) {
                //a的ASCII码 将这个运算后的数字强制转换成字符
                //属性的去重操作
                while (fill_set.size() == j ){
                    String c = ((char) (Math.random() * 5 + 'a'))+"";
                    fill_set.add(c);
                }
            }
            attributes = fill_set.toString();
            question.setId(i);
            question.setAttributes(attributes);
            questions[question.getId()]=question;
            System.out.println("id："+question.getId()+" 属性："+question.getAttributes());

            // 将数据保存到数据库
            //jdbcUtils.insert(question.getId(),"'"+question.getAttributes()+"'");

        }
        System.out.println();

        //填空题
        System.out.println("====== 填空题  ======");
        for (int i = 0; i < TYPE_FILL_NUM; i++) {
            Questions question = new Questions();
            String attributes ;
            int attNum = new Random().nextInt(ATTRIBUTE_MAX);
            Set<String> fill_set = new HashSet<>();
            for (int j = 0; j < attNum+1; j++) {
                //属性的去重操作
                while (fill_set.size() == j ){
                    String c = ((char) (Math.random() * 5 + 'a'))+"";
                    fill_set.add(c);
                }
            }
            attributes = fill_set.toString();
            question.setId(i+TYPE_CHOICE_NUM);
            question.setAttributes(attributes);
            questions[question.getId()]=question;
            System.out.println("id："+question.getId()+" 属性："+question.getAttributes());
            System.out.print("");

            // 将数据保存到数据库
            //jdbcUtils.insert(question.getId(),"'"+question.getAttributes()+"'");
        }
        System.out.println();

        //简答题
        System.out.println("====== 简答题  ======");
        for (int i = 0; i < TYPE_SUMMARY_NUM; i++) {
            Questions question = new Questions();
            String attributes  ;
            int attNum = new Random().nextInt(ATTRIBUTE_MAX);
            Set<String> fill_set = new HashSet<>();
            for (int j = 0; j < attNum+1; j++) {
                //属性的去重操作
                while (fill_set.size() == j ){
                    String c = ((char) (Math.random() * 5 + 'a'))+"";
                    fill_set.add(c);
                }
            }
            attributes = fill_set.toString();
            question.setId(i+TYPE_CHOICE_NUM+TYPE_FILL_NUM);
            question.setAttributes(attributes);
            questions[question.getId()]=question;
            System.out.println("id："+question.getId()+" 属性："+question.getAttributes());

            // 将数据保存到数据库
            //jdbcUtils.insert(question.getId(),"'"+question.getAttributes()+"'");
        }

    }



    /**
     * 计算每张试卷的适应度
     */
    public static void getPaperFitness() {
        double sum = 0;
        for (int i = 0; i < 10; i++) {
            double tmp_value =0;
            Integer[] integers = paper_genetic[i];
            for (int j = 0; j < 20; j++) {
                tmp_value += all_fitness[integers[j]];
            }
            double   f   =   tmp_value;
            BigDecimal b   =   new   BigDecimal(f);
            double   f1   =   b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            paper_fitness[i]=f1;
            sum = sum + f1;
        }
        System.out.println("总和："+sum);
    }

    /**
     * 随机生成初代种群
     */
    public static void  init(Papers papers) {
        System.out.println("=== init begin ===");
        for (int i = 0; i < papers.getPaperSize(); i++) {
            //随机生成取题目id序列

            Integer[] testGene= new Integer[papers.getQuestSize()];
            Set<Integer> set = new HashSet<Integer>();
            for(int j = 0; j < ACT_CHOICE_NUM; j++){
                //保证题目不重复,且满足题型约束
                while (set.size() == j ){
                    Integer key = new Random().nextInt(TYPE_CHOICE_NUM);
                    set.add(key);
                }
            }
            for(int j = 0; j < ACT_FILL_NUM; j++){
                while (set.size()-ACT_CHOICE_NUM == j ){
                    Integer key = new Random().nextInt(TYPE_FILL_NUM)+TYPE_CHOICE_NUM;
                    set.add(key);
                }
            }
            for(int j = 0; j < ACT_SUMMARY_NUM; j++){
                while (set.size()-ACT_CHOICE_NUM-ACT_FILL_NUM == j ){
                    Integer key = new Random().nextInt(TYPE_SUMMARY_NUM)+TYPE_FILL_NUM+TYPE_CHOICE_NUM;
                    set.add(key);
                }
            }
            set.toArray(testGene);
            Arrays.sort(testGene);
            System.out.println(i+" 选取题ID，构成初始试卷："+Arrays.toString(testGene));
            paper_genetic[i] = testGene;
        }
        System.out.println("=== init end ===");

    }


    /**
     * 交叉
     */
    public static void crossCover(Papers papers){
        System.out.println();
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
                int a1=1;
                correct(i,temp1);
            }
        }
        System.out.println("=== crossCover end ===");
    }

    /**
     * 判断size，执行修补操作
     */
    public static void correct(int i,Integer[] temp1) {

        int a1=1;
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
                    int a3=1;
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
        System.out.println();
        System.out.println("=== mutate begin ===");
        Integer key = 0;
        for (int i = 0; i < papers.getPaperSize(); i++) {
            if(Math.random() < papers.getPm()){
                Random random = new Random();
                int mutate_point = random.nextInt(papers.getQuestSize()-1);
                Set<Integer> set = new HashSet<Integer>(Arrays.asList( paper_genetic[i]));
                Integer s = paper_genetic[i][mutate_point];
                int a=1;
                System.out.println(i+" 原试卷: "+set);
                System.out.println("  remove element: "+ s);
                set.remove(s);
                System.out.println("  临时试卷：  "+set);

                Integer[] temp1 = new Integer[20];

                if (mutate_point<10){
                    int a2=1;
                    //生成一个合适的且不存在set中的key
                    while (set.size() != 20 ){
                        key = random.nextInt(20);
                        if (!key.equals(s)){
                            set.add(key);
                        }
                    }
                }else if(mutate_point<15){
                    int a3=1;
                    while (set.size() != 20 ){
                        key = Math.abs(new Random().nextInt()) % 10 + 20;
                        if (!key.equals(s)){
                            set.add(key);
                        }
                    }
                }else if(mutate_point<20){
                    int a4=1;
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





    public static void  elitiststrategy(){
//        getPaperFitness(paper_genetic);
//        Object[] objects = best_value();
//        // 全局最优解替换掉局部最优解
//        paper_genetic[(Integer) objects[0]]= best_genetic_one;

    }



    public static   void selection( ){
        //10个体（试卷）   20个基因（题目）
        int population_size = 10;
        double fitness_sum = 0;
        double[] fitness_tmp = new double[population_size];
        double[] fitness_proportion = new double[population_size];
        double cumsum = 0;
        double[] pie_fitness = new double[population_size];


        Integer[][] new_genetic_population =new Integer[population_size][];
        int random_selection_id = 0;


        for (int i = 0; i < population_size; i++) {

            double tmp_value =0;
            double tmp_fitness_sum =0;
            Integer[] integers = paper_genetic[i];
            for (int j = 0; j < 20; j++) {
                tmp_value += all_fitness[integers[j]];
            }
            BigDecimal b  =   new   BigDecimal(tmp_value);
            tmp_fitness_sum =   b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            double tmp =tmp_fitness_sum;
            fitness_tmp[i]=tmp;
            System.out.println(i+"试卷的适应度： "+tmp_fitness_sum);
            fitness_sum = tmp_fitness_sum + fitness_sum;
            System.out.println("目前总试卷的适应度： "+fitness_sum);
        }

        for (int i = 0; i < population_size; i++) {
            //各自的比例
            fitness_proportion[i] = fitness_tmp[i] / fitness_sum;
        }

        //越大的适应度，其叠加时增长越快，所以有更大的概率被选中
        for (int i = 0; i < population_size; i++) {
            pie_fitness[i] = cumsum + fitness_proportion[i];
            cumsum += fitness_proportion[i];
            System.out.println(i+"目前总试卷的适应度百分比： "+pie_fitness[i]);
        }

        //累加的概率为1
        pie_fitness[population_size-1] = 1;

        //初始化容器
        double[] random_selection = new double[population_size];

        for (int i = 0; i < population_size; i++) {
            random_selection[i] = Math.random();
            //System.out.println(random_selection[i]);
        }
        //排序
        Arrays.sort(random_selection);

        //轮盘赌可能存在点问题
        //随着random_selection_id的递增,random_selection[random_selection_id]逐渐变大
        for (int i = 0; i < population_size; i++) {
            while (random_selection_id < population_size && random_selection[random_selection_id] < pie_fitness[i]){
                new_genetic_population[random_selection_id]   = paper_genetic[i];
                random_selection_id += 1;
            }
        }
        System.out.println();
        //输出老种群的适应度值
        getPaperFitness();
        //重新赋值种群的编码
        paper_genetic=new_genetic_population;
        //输出新种群的适应度值
        getPaperFitness();
    }

}



