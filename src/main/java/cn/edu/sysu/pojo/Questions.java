package cn.edu.sysu.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Created by songb
 */
@Data
public class Questions implements Serializable{
    /**
     * 编号
     */
	private int id;
    /**
     * 类型
     */
	private int types;
    /**
     * 章节
     */
	private int chapter;

    /**
     * 难度
     */
	private String difficult;
    /**
     * 曝光度
     */
	private String exposure;

    /**
     * 属性
     * 5个不同的属性:a b c d e
     */
    private String attributes;

}
