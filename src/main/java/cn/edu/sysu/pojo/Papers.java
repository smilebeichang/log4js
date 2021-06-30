package cn.edu.sysu.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Created by songb.
 */
@Data
public class Papers implements Serializable{
    /**
     * 试卷编号
     */
	private int id;

    /**
     * 难度分布
     */
    private List<String> difDistribute;

    /**
     * 章节覆盖度
     */
    private List<String> chapterCoverage;

    /**
     * 试卷套数
     */
    private  int paperSize;

    /**
     * 单套试卷题目数
     */
    private  int questSize;

    /**
     * 交叉概率
     */
    private double pc;

    /**
     * 变异概率
     */
    private double pm;

    /**
     * 题库试卷的适应度值
     */
    private  double[] fitness =new double[40];

    /**
     * 单套试卷中最好的基因
     */
    private Integer[] bestGene =new Integer[20];



}
