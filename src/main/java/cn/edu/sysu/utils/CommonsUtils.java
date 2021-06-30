package cn.edu.sysu.utils;

import cn.edu.sysu.pojo.Questions;

/**
 * @Author : songbeichang
 * @create 2021/3/5 1:05
 */
public class CommonsUtils {

    /**
     * 初始化题库
     */
    private static Integer ATTRIBUTE_MAX = 3;
    private static Integer TYPE_CHOICE_NUM = 20;
    private static Integer TYPE_FILL_NUM  = 10;
    private static Integer TYPE_SUMMARY_NUM  = 10;

    private static Integer ACT_CHOICE_NUM = 10;
    private static Integer ACT_FILL_NUM  = 5;
    private static Integer ACT_SUMMARY_NUM  = 5;
    private static Integer[][] paper_genetic =new Integer[10][20];

    /**
     * 容器
     */
    static Questions[] questions =new Questions[40];
    static  double[] all_fitness =new double[40];

    static  double[] paper_fitness =new double[10];




}



