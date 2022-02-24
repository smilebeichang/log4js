package cn.edu.sysu.adi;


import cn.edu.sysu.controller.ADIController;

import java.sql.SQLException;
import java.util.*;

/**
 * @Author : song bei chang
 * @create 2021/5/26 0:18
 */
public class Test {

    //LogFactory.getLog   Logger.getLogger
    //private static Log logger = LogFactory.getLog(Test.class);


    @org.junit.Test
    public void test2(){

        //随机生成
        //String attributes ;
        Set<String> fill_set = new HashSet<>();
        for (int j = 0; j < 6; j++) {
            //a的ASCII码 数字--字符--pattern
            while (fill_set.size() == j ){
                String c = ((char) (Math.random() * 8 + 'a'))+"";
                fill_set.add(c);
            }
        }
        int p1 = fill_set.contains("a")?1:0;
        int p2 = fill_set.contains("b")?1:0;
        int p3 = fill_set.contains("c")?1:0;
        int p4 = fill_set.contains("d")?1:0;
        int p5 = fill_set.contains("e")?1:0;
        int p6 = fill_set.contains("f")?1:0;
        int p7 = fill_set.contains("g")?1:0;
        int p8 = fill_set.contains("h")?1:0;
        String ip = "("+p1+","+p2+","+p3+","+p4+","+p5+","+p6+","+p7+","+p8+")";
        System.out.println(ip);

    }



    @org.junit.Test
    public void ceil(){
        ArrayList<String> bachItemList = new ArrayList<String>(){
            {
                add("xiao");
                add("bao");
                add("bao2");
                add("bao3");
                add("bao4");
                add("xiao");
                add("bao");
                add("bao2");
                add("bao3");
                add("bao4");
                add("xiao");
                add("bao");
                add("bao2");
                add("bao3");
                add("bao4");
                add("xiao");
                add("bao");
                add("bao2");
                add("bao3");
                add("bao4");
            }
        };

        System.out.println(Math.ceil(bachItemList.size() / 3));
        System.out.println(Math.ceil(bachItemList.size() / 3 * 2 ));

    }




    @org.junit.Test
    public void hashCode2(){
        ArrayList<String> bachItemList = new ArrayList<String>(){
            {
                add("xiao");
                add("bao");
            }
        };
        ArrayList<String> tmp = new ArrayList<>();
        //System.out.println(tmp.hashCode());
        //System.out.println(bachItemList.hashCode());
        for (int i = bachItemList.size(); i > 0; i--) {
            tmp.add(bachItemList.get(i-1));
        }
        //System.out.println(tmp.hashCode());
        //logger.info(tmp.hashCode());

    }

    /**
     * 找出一个数组中一个数字出现次数最多的数字
     * 用HashMap的key来存放数组中存在的数字，value存放该数字在数组中出现的次数
     *
     */
    @org.junit.Test
    public void HashMapTest1(){

           int[] array = {2, 1, 1,2, 3, 4, 5, 2, 2,5, 2, 2};
            //map的key存放数组中存在的数字，value存放该数字在数组中出现的次数
            HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
            for(int i = 0; i < array.length; i++) {
                if(map.containsKey(array[i])) {
                    int temp = map.get(array[i]);
                    map.put(array[i], temp + 1);
                } else {
                    map.put(array[i], 1);
                }
            }

            //分别输出每个个体出现的次数
            for(Map.Entry<Integer, Integer> entry : map.entrySet()) {
                //得到value为maxCount的key，也就是数组中出现次数最多的数字

                    Integer key = entry.getKey();
                    Integer count = entry.getValue();
                    System.out.println("试题编号："+ key+"  次数："+count);

            }

            //找出map的value中最大的数字，也就是数组中数字出现最多的次数
            Collection<Integer> count = map.values();
            int max = Collections.max(count);
            System.out.println(max);

            int maxNumber = 0;
            int maxCount = 0;
            for(Map.Entry<Integer, Integer> entry : map.entrySet()) {
                //得到value为maxCount的key，也就是数组中出现次数最多的数字
                if(maxCount < entry.getValue()) {
                    maxCount = entry.getValue();
                    maxNumber = entry.getKey();
                }
            }
            System.out.println("出现次数最多的数字为：" + maxNumber);
            System.out.println("该数字一共出现" + maxCount + "次");


    }


    @org.junit.Test
    public  void sonList() {
        String[] arr = {"p1", "p2","p3"};
        Set<Set<String>> f = f(arr.length, arr);
        System.out.println(f);
        for (Set<String> tmp : f) {
            System.out.println(tmp);
        }

    }

    public  Set<Set<String>> f(int k, String[] arr) {
        if (k == 0) {
            Set<Set<String>> set = new HashSet<>();
            //添加一个空集合
            set.add(new HashSet<>());
            return set;
        }

        Set<Set<String>> set = f(k - 1, arr);
        Set<Set<String>> resultSet = new HashSet<>();

        //扫描上一层的集合
        for (Set<String> integerSet : set) {

            //上一层的每个集合都包含两种情况，一种是加入新来的元素，另一种是不加入新的元素
            HashSet<String> subSet = new HashSet<>();

            subSet.addAll(integerSet);
            subSet.add(arr[k - 1]);

            resultSet.add(subSet);
            resultSet.add(integerSet);
        }
        return resultSet;
    }





    @org.junit.Test
    public void flag(){
        Boolean flag = true;
        if(!flag){
            System.out.println("``````````");
        }else {
            System.out.println("***********");
        }
    }


    @org.junit.Test
    public void trans(){

//    将列表转换为集合
//    Set set = new HashSet(list);

//    将集转换为列表
//    List list = new ArrayList(set);

//    double[] 转 ArrayList<Double>  可以省略
//    ArrayList<Double> arrayList = new ArrayList<>(fitPro.length);
//        for (double anArr : fitPro) {
//        arrayList.add(anArr);
//    }

//    Double[]转List
//    List<Double> list = Arrays.asList(arrDouble);

//    将Array 转 hashSet
//    Set<String> set = new HashSet<>(Arrays.asList( paperGenetic[i]));

//    hashSet 转 数组
//    String[] array = new String[setEnd.size()];
//    array = setEnd.toArray(array);

//    数组转list  转hashSet
//    HashSet<String> setBegin = new HashSet<>(Arrays.asList(temp1));

//    arrayList 转 数组
//    String[] itemArray = new String[bachItemList.size()];
//            for (int i = 0; i < bachItemList.size(); i++) {
//        itemArray[i] = bachItemList.get(i);
//    }
    }


    /**
     * 通过交集 并集 差集 来重新对list排序
     */
    @org.junit.Test
    public void rearrange(){

        //定义
        List<String> listA = new ArrayList<>();
        List<String> listB = new ArrayList<>();
        listA.add("A");
        listA.add("B");
        listA.add("C");
        listA.add("D");

        listB.add("B");
        listB.add("C");

        //差集  AD
        listA.removeAll(listB);
        System.out.println(listA);


        //并集  ADBC
        // 不做第一步取的是有重复元素的并集
        listA.removeAll(listB);
        listB.addAll(listA);
        System.out.println(listB);

    }



    @org.junit.Test
    public void mod(){
        System.out.println("4,4取模"+Math.floorMod(4,4));
        System.out.println("5,4取模"+Math.floorMod(5,4));
        System.out.println("6,4取模"+Math.floorMod(6,4));
        System.out.println("7,4取模"+Math.floorMod(7,4));
        System.out.println("8,4取模"+Math.floorMod(8,4));


        System.out.println(5/10.0);
    }

    @org.junit.Test
    public void random(){

        for (int i = 0; i < 10; i++) {

            double randomId = Math.random();
            //打印出随机抽取的random概率值
            System.out.println(randomId);
        }

    }



    @org.junit.Test
    public void arrAsList(){
        int[] arr = new int[4];
        arr[0] = 0;
        arr[1] = 1;
        arr[2] = 2;
        arr[3] = 3;

        ArrayList< String> arrayList = new ArrayList<String>(arr.length);

        for (int anArr : arr) {
            arrayList.add(anArr + "");
        }
        System.out.println(arrayList.toString());

    }





    @org.junit.Test
    public void stringBuilder(){
        StringBuilder sb = new StringBuilder();
        int ab2 = -1;
        if(ab2>0){
            sb.append(" p2=0 and ");
        }else if (ab2<0){
            sb.append(" p2=1 and ");
        }
        System.out.println( sb.toString());
    }

    @org.junit.Test
    public void remove(){
        /**
         * 根据原集合 旧解 新解 三者的关系进行，属性比例要求判断
         *
         */
        ArrayList<String> bachItemList = new ArrayList<>();
        bachItemList.add("w");
        bachItemList.add("s");
        bachItemList.add("x");
        String s  = "s";
        String s1 = "s1";


        // 刪除元素s
        for (int i = 0; i < bachItemList.size(); i++) {
            if (bachItemList.get(i).equals(s)){
                bachItemList.set(i,s1);
            }
        }
        System.out.println(bachItemList.toString());
        // 输出 [0, 1, 2, 3, 4, 5, 6, 7, 9]，



    }


    @org.junit.Test
    public void test() throws SQLException {
        String[] temp1 = new String[6];
        temp1[0] = "51:(0,0,1,0,1):0.0:0.0:0.0075:0.0:0.055000000000000035:0.0:0.0:0.0:0.0:0.0";
        temp1[1] = "73:(1,0,0,1,0):0.08000000000000002:0.0:0.0:0.022500000000000003:0.0:0.0:0.0:0.0:0.0:0.0";
        temp1[2] = "99:(0,0,1,0,1):0.0:0.0:0.032500000000000015:0.0:0.045000000000000026:0.0:0.0:0.0:0.0:0.0";
        temp1[3] = "173:(1,0,1,0,1):0.00125:0.0:0.03625000000000002:0.0:0.043750000000000025:0.0:0.0:0.0:0.0:0.0";
        temp1[4] = "193:(0,1,0,1,1):0.0:0.03500000000000002:0.0:0.0075:0.07625000000000001:0.0:0.0:0.0:0.0:0.0";
        temp1[5] = "284:(1,1,1,1,0):0.06625:0.019375000000000003:0.024375000000000008:0.024375000000000008:0.0:0.0:0.0:0.0:0.0:0.0";
        new ADIController().correct(1,temp1);
    }


    /**
     * 排序
     */
    @org.junit.Test
    public void sort(){
        int[] ints = {20,1,4,8,3};
        Arrays.sort(ints);
        for (int i = 0; i < ints.length; i++) {
            System.out.print(ints[i]+" ");
        }
    }

    /**
     * 临时方法 测试专用
     */
    @org.junit.Test
    public void sss(){

        for (int i = 0; i < 100 ; i++) {
            //10~20
            Integer key = Math.abs(new Random().nextInt()) % 20 + 10;
            System.out.println(key);
            //10~30
//            int j = new Random().nextInt(20) + 10;
//            System.out.println(j);
        }
    }


    @org.junit.Test
    public void exp(){
        double x = 11.635;
        double y = 2.76;

        System.out.printf("e 的值为 %.4f%n", Math.E);
        System.out.printf("exp(%.3f) 为 %.3f%n", x, Math.exp(x));
        // 2.7183 * 2.7183 = 7.389
        System.out.printf("exp(%.3f) 为 %.3f%n", 2.0, Math.exp(2.0));
        // - 表示取倒数  1/(2.7183 * 2.7183)  =  0.135
        System.out.printf("exp(%.3f) 为 %.3f%n", -0.26, Math.exp(-0.26));
        System.out.printf("exp(%.3f) 为 %.3f%n", -0.55, Math.exp(-0.55));
        System.out.printf("exp(%.3f) 为 %.3f%n", -0.95, Math.exp(-0.95));
    }


    @org.junit.Test
    public void test1() {

        Set<String> set1 = new HashSet<String>() {
            {
                add("王者荣耀2");
                add("英雄联盟");
                add("穿越火线");
                add("地下城与勇士");
            }
        };

        Set<String> set2 = new HashSet<String>() {
            {
                add("王者荣耀");
                add("地下城与勇士");
                add("魔兽世界");
            }
        };

        set1.retainAll(set2);
        if(set1.size()>0){
            System.out.println("存在交集：" + set1);
        }else{
            System.out.println("不存在交集：" + set1);
        }


    }
}



