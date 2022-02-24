package cn.edu.sysu.niche.others;

import java.util.Comparator;

/**
 * 比较器类
 */
public class MyComparator implements Comparator {
    @Override
    public int compare(Object str1, Object str2) {
        return  Double.valueOf(str2.toString().split("_")[0]).compareTo(Double.valueOf(str1.toString().split("_")[0]));
    }
}



