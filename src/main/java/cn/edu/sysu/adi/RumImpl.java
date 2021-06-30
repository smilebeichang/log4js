package cn.edu.sysu.adi;

import cn.edu.sysu.utils.JDBCUtils;
import cn.edu.sysu.utils.KLUtils;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author : song bei chang
 * @create 2021/5/11 19:34
 *
 * Reduce-RUM 统一参数化模型  的实现
 *
 * double que_fit = πj * ( (rjk* * γ * β) .... );
 *
 * p* 是正确应用第j项所有必要属性的概率，
 * πj* 为项目难度参数，正确作答item j的概率，其值越大（接近1）表示被试掌握了所需属性很可能成功应答。  每道题一个固定值
 * rjk*表示被试缺乏属性K,其值越小（接近0）,表示属性K很重要。也被称做属性K在item上的区分度参数。  每道题的每个属性一个固定值
 * super high   high      low
 * [0.05,0.2]  [0.4,0.85] [0.6,0.92]
 *
 *
 *            (0,0,0)(0,0,1)(0,1,0)(1,0,0)(0,1,1)(1,0,1)(1,1,0)(1,1,1)
 *    (0,0,0)
 *    (0,0,1)
 *    (0,1,0)
 *    (1,0,0)
 *    (0,1,1)
 *    (1,0,1)
 *    (1,1,0)
 *    (1,1,1)
 *
 *
 *    TODO 1.精简 RUM 和 DINA 公共代码
 *    TODO 2.DINA 属性值，ADI一致的校验
 *    TODO 3.以 RUM 为主，进行扩充
 *    TODO      1.题库数量
 *    TODO      2.GA的实现（硬编码:长度   软编码:属性、题型  exp表达式）
 *
 *
 *
 *
 */
public class RumImpl {

    private int id ;
    private String pattern ;
    private Double base ;
    private String penalty ;
    private Double adi1_r;
    private Double adi2_r;
    private Double adi3_r;
    private Double adi4_r;
    private Double adi5_r;



    //1. 生成题目的 pattern  5
    //2. 生成 base 和 penalty; 生成rum
    //3. 接收返回ArrayList<Double> rumLists
    //4. 生成 K_L 矩阵
    //5. 计算一道试题 pattern 对应五个adi (index vs klArray)

    //6. 封装单道题的属性值(id  pattern  base  penalty  adi1_r adi2_r adi3_r adi4_r adi5_r)
    //7. 生成题库 要求属性比例均衡



    @Test
    public  void Init() throws InterruptedException, SQLException {

        JDBCUtils jdbcUtils = new JDBCUtils();

        // 5:10:10:5:1 假设题库62道题  则10:20:20:10:2
        for (int i = 1; i <= 10; i++) {
            id = i;
            start(1);
            jdbcUtils.insert(id,pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r);
        }
        for (int i = 11; i <= 30; i++) {
            id = i;
            start(2);
            jdbcUtils.insert(id,pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r);
        }
        for (int i = 31; i <= 50; i++) {
            id = i;
            start(3);
            jdbcUtils.insert(id,pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r);
        }
        for (int i = 51; i <= 60; i++) {
            id = i;
            start(4);
            jdbcUtils.insert(id,pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r);
        }
        for (int i = 61; i <= 62; i++) {
            id = i;
            start(5);
            jdbcUtils.insert(id,pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r);
        }


    }

    /**
     *  rum 的具体实现
     *      1. 根据属性个数，生成试题pattern
     *      2. 获取adi指标
     *      3. 将上述信息，保存到全局变量
     */
    public void start(int num) throws InterruptedException {

        pattern = new KLUtils().randomInit(num);

        GetAdi(pattern);

        System.out.printf("id=%s \t pattern=%s \t base=%s \t penalty=%s \t adi1_r=%s \t adi2_r=%s \t adi3_r=%s \t adi4_r=%s \t adi5_r=%s \n", id, pattern, base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r);

        System.out.println();

    }


    /**
     * 以试题pattern为单位,算出一个答题概率的集合 rum，然后求出该试题的矩阵 k_L，
     * 求出该试题的矩阵 Da，最后平均求出该试题的 adi
     * 可以理解为一道试题下，所有考生的差异性
     */
    public  void GetAdi(String ip) {

        //index map
        Map<String,Integer> map = new HashMap<>(32);
        map.put("(0,0,0,0,0)",1);
        map.put("(0,0,0,0,1)",2);
        map.put("(0,0,0,1,0)",3);
        map.put("(0,0,1,0,0)",4);
        map.put("(0,1,0,0,0)",5);
        map.put("(1,0,0,0,0)",6);

        map.put("(0,0,0,1,1)",7);
        map.put("(0,0,1,0,1)",8);
        map.put("(0,1,0,0,1)",9);
        map.put("(1,0,0,0,1)",10);
        map.put("(0,0,1,1,0)",11);
        map.put("(0,1,0,1,0)",12);
        map.put("(1,0,0,1,0)",13);
        map.put("(0,1,1,0,0)",14);
        map.put("(1,0,1,0,0)",15);
        map.put("(1,1,0,0,0)",16);

        map.put("(0,0,1,1,1)",17);
        map.put("(0,1,0,1,1)",18);
        map.put("(1,0,0,1,1)",19);
        map.put("(0,1,1,0,1)",20);
        map.put("(1,0,1,0,1)",21);
        map.put("(1,1,0,0,1)",22);
        map.put("(0,1,1,1,0)",23);
        map.put("(1,0,1,1,0)",24);
        map.put("(1,1,0,1,0)",25);
        map.put("(1,1,1,0,0)",26);

        map.put("(0,1,1,1,1)",27);
        map.put("(1,0,1,1,1)",28);
        map.put("(1,1,0,1,1)",29);
        map.put("(1,1,1,0,1)",30);
        map.put("(1,1,1,1,0)",31);
        map.put("(1,1,1,1,1)",32);


        //试题的pattern 和 penalty 个数相关   故随机生成pattern的同时生成penalty
        //基线系数 和 惩罚系数 随机生成
        base = new KLUtils().makeRandom(0.95f, 0.75f, 2);

        ArrayList<Double> rumList = GetRumListsRandom(base,ip);


        //根据 rumList 计算出K_L二维数组
        Double[][] klArray = new KLUtils().foreach(rumList);
        //打印
        new KLUtils().arrayPrint(klArray);


        /*
         * 四个元素，遍历组合即可, 随机遍历顺序没影响
         * combineList 全局变量,保存另外几个属性的选取方案
         */
        ArrayList<String> combineList = new ArrayList<>();
        for(int X=0;X<2; X++){
            for(int Y=0;Y<2; Y++) {
                for (int Z = 0; Z < 2; Z++) {
                    for (int W = 0; W < 2; W++) {
                        String value = X +","+ Y +","+ Z +","+ W;
                        combineList.add(value);
                    }
                }

            }
        }

        //获取下标
        ArrayList<String> list1 = new ArrayList();
        ArrayList<String> list2 = new ArrayList();
        ArrayList<String> list3 = new ArrayList();
        ArrayList<String> list4 = new ArrayList();
        ArrayList<String> list5 = new ArrayList();


        for (int X =0;X<combineList.size();X++){
            //adi1
            Integer index11 = map.get("(1," + combineList.get(X) + ")");
            Integer index12 = map.get("(0," + combineList.get(X) + ")");
            Integer index13 = map.get("(0," + combineList.get(X) + ")");
            Integer index14 = map.get("(1," + combineList.get(X) + ")");

            list1.add(""+index11+"_"+index12);
            list1.add(""+index13+"_"+index14);

            //adi2         1,0,1,0
            Integer index21 = map.get("(" +combineList.get(X).substring(0,1)+ ",1," +combineList.get(X).substring(2,7) + ")");
            Integer index22 = map.get("(" +combineList.get(X).substring(0,1)+ ",0," +combineList.get(X).substring(2,7) + ")");
            Integer index23 = map.get("(" +combineList.get(X).substring(0,1)+ ",0," +combineList.get(X).substring(2,7) + ")");
            Integer index24 = map.get("(" +combineList.get(X).substring(0,1)+ ",1," +combineList.get(X).substring(2,7) + ")");

            list2.add(""+index21+"_"+index22);
            list2.add(""+index23+"_"+index24);


            //adi3
            Integer index31 = map.get("("+combineList.get(X).substring(0,3)+",1,"+combineList.get(X).substring(4,7)+ ")");
            Integer index32 = map.get("("+combineList.get(X).substring(0,3)+",0,"+combineList.get(X).substring(4,7)+ ")");
            Integer index33 = map.get("("+combineList.get(X).substring(0,3)+",0,"+combineList.get(X).substring(4,7)+ ")");
            Integer index34 = map.get("("+combineList.get(X).substring(0,3)+",1,"+combineList.get(X).substring(4,7)+ ")");

            list3.add(""+index31+"_"+index32);
            list3.add(""+index33+"_"+index34);


            //adi4
            Integer index41 = map.get("("+combineList.get(X).substring(0,5)+",1,"+combineList.get(X).substring(6,7)+ ")");
            Integer index42 = map.get("("+combineList.get(X).substring(0,5)+",0,"+combineList.get(X).substring(6,7)+ ")");
            Integer index43 = map.get("("+combineList.get(X).substring(0,5)+",0,"+combineList.get(X).substring(6,7)+ ")");
            Integer index44 = map.get("("+combineList.get(X).substring(0,5)+",1,"+combineList.get(X).substring(6,7)+ ")");

            list4.add(""+index41+"_"+index42);
            list4.add(""+index43+"_"+index44);


            //adi5
            Integer index51 = map.get("("+combineList.get(X)+",1)");
            Integer index52 = map.get("("+combineList.get(X)+",0)");
            Integer index53 = map.get("("+combineList.get(X)+",0)");
            Integer index54 = map.get("("+combineList.get(X)+",1)");

            list5.add(""+index51+"_"+index52);
            list5.add(""+index53+"_"+index54);


        }
        System.out.println("adi的计算指标：");
        System.out.println("adi1个数: "+list1.size() +" 具体指标为:"+list1);
        System.out.println("adi2个数: "+list2.size() +" 具体指标为:"+list2);
        System.out.println("adi3个数: "+list3.size() +" 具体指标为:"+list3);
        System.out.println("adi4个数: "+list4.size() +" 具体指标为:"+list4);
        System.out.println("adi5个数: "+list5.size() +" 具体指标为:"+list5);

        System.out.println("list 遍历; 分别拿list的值去KLArray中匹配寻找：");
        List<Double> calAdiList = CalAdiImple(klArray, list1, list2, list3, list4, list5);
        System.out.println(calAdiList);


    }


    /**
     * 返回 rum 的 集合 ArrayList<ArrayList<Double>>
     * @param base  基线系数
     * @param ip 题目的属性pattern
     */
    public ArrayList<Double> GetRumListsRandom(Double base,String ip){

        ArrayList<Double> rumLists = new ArrayList<>();

        //考生_pattern sps
        ArrayList<String> sps = new ArrayList<String>(){{

            add("(0,0,0,0,0)");
            add("(0,0,0,0,1)");
            add("(0,0,0,1,0)");
            add("(0,0,1,0,0)");
            add("(0,1,0,0,0)");
            add("(1,0,0,0,0)");

            add("(0,0,0,1,1)");
            add("(0,0,1,0,1)");
            add("(0,1,0,0,1)");
            add("(1,0,0,0,1)");
            add("(0,0,1,1,0)");
            add("(0,1,0,1,0)");
            add("(1,0,0,1,0)");
            add("(0,1,1,0,0)");
            add("(1,0,1,0,0)");
            add("(1,1,0,0,0)");

            add("(0,0,1,1,1)");
            add("(0,1,0,1,1)");
            add("(1,0,0,1,1)");
            add("(0,1,1,0,1)");
            add("(1,0,1,0,1)");
            add("(1,1,0,0,1)");
            add("(0,1,1,1,0)");
            add("(1,0,1,1,0)");
            add("(1,1,0,1,0)");
            add("(1,1,1,0,0)");

            add("(0,1,1,1,1)");
            add("(1,0,1,1,1)");
            add("(1,1,0,1,1)");
            add("(1,1,1,0,1)");
            add("(1,1,1,1,0)");
            add("(1,1,1,1,1)");
        }};

        //题目pattern
        int a1 = Integer.parseInt(ip.substring(1, 2));
        int a2 = Integer.parseInt(ip.substring(3, 4));
        int a3 = Integer.parseInt(ip.substring(5, 6));
        int a4 = Integer.parseInt(ip.substring(7, 8));
        int a5 = Integer.parseInt(ip.substring(9, 10));
        //0.65~0.92

        Double penalty1 = a1 == 1? new KLUtils().makeRandom(0.95f, 0.05f, 2):0;
        Double penalty2 = a2 == 1? new KLUtils().makeRandom(0.95f, 0.05f, 2):0;
        Double penalty3 = a3 == 1? new KLUtils().makeRandom(0.95f, 0.05f, 2):0;
        Double penalty4 = a4 == 1? new KLUtils().makeRandom(0.95f, 0.05f, 2):0;
        Double penalty5 = a5 == 1? new KLUtils().makeRandom(0.95f, 0.05f, 2):0;
        penalty = "("+penalty1+","+penalty2+","+penalty3+","+penalty4+","+penalty5+")";

        //根据学生pattern vs 题目pattern 获取答对此题的rum
        for (String ps : sps) {
            //学生pattern
            int b1 = Integer.parseInt(ps.substring(1, 2));
            int b2 = Integer.parseInt(ps.substring(3, 4));
            int b3 = Integer.parseInt(ps.substring(5, 6));
            int b4 = Integer.parseInt(ps.substring(7, 8));
            int b5 = Integer.parseInt(ps.substring(9, 10));

            //a>=b则*1, a<b 则*penalty^1
            boolean ab1 = b1 >= a1;
            boolean ab2 = b2 >= a2;
            boolean ab3 = b3 >= a3;
            boolean ab4 = b4 >= a4;
            boolean ab5 = b5 >= a5;
            int num1 = ab1?0:1;
            int num2 = ab2?0:1;
            int num3 = ab3?0:1;
            int num4 = ab4?0:1;
            int num5 = ab5?0:1;


            double p = base * Math.pow(penalty1, num1) * Math.pow(penalty2, num2) * Math.pow(penalty3, num3) * Math.pow(penalty4, num4) * Math.pow(penalty5, num5) ;

            rumLists.add(p);
        }
        return  rumLists;

    }




    /**
     * 计算adi具体实现
     */
    public List<Double> CalAdiImple(Double[][] klArray,ArrayList<String> list1,ArrayList<String> list2,ArrayList<String> list3,ArrayList<String> list4,ArrayList<String> list5){
        Double sum1 = 0.0;
        Double sum2 = 0.0;
        Double sum3 = 0.0;
        Double sum4 = 0.0;
        Double sum5 = 0.0;

        for(String data  :    list1)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum1+=v;
        }
        adi1_r=sum1/31;
        System.out.println("adi1: "+adi1_r);

        for(String data  :    list2)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum2+=v;
        }
        adi2_r=sum2/31;
        System.out.println("adi2: "+adi2_r);

        for(String data  :    list3)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum3+=v;
        }
        adi3_r=sum3/31;
        System.out.println("adi3: "+adi3_r);

        for(String data  :    list4)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum4+=v;
        }
        adi4_r=sum4/31;
        System.out.println("adi4: "+adi4_r);

        for(String data  :    list5)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum5+=v;
        }
        adi5_r=sum5/31;
        System.out.println("adi5: "+adi5_r);

        List<Double> adiList = new ArrayList<Double>(){{
            add(adi1_r);
            add(adi2_r);
            add(adi3_r);
            add(adi4_r);
            add(adi5_r);
        }};
        return adiList;

    }




}



