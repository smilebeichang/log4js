package cn.edu.sysu.adi;

import java.io.IOException;

/**
 * @Author : song bei chang
 * @create 2021/5/2 17:52
 *
 *          1.属性级别认知诊断的适应度值代码实现 ADI
 *              1.1 K_L information的实现 抽取到KL utils
 *              1.2 ADI
 *          2.验证
 */
public class AttributeLevel {


    public static void main(String[] args) throws IOException {

        // 最终的计算公式
        double v = (0.8) * Math.log((0.8) / (0.1))+(0.2) * Math.log((0.2) / (0.9));

        System.out.println(v);

    }







}



