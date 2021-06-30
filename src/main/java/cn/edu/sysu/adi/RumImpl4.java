package cn.edu.sysu.adi;

import cn.edu.sysu.utils.JDBCUtils4;
import cn.edu.sysu.utils.KLUtils;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * @Author : song bei chang
 * @create 2021/5/11 19:34
 *
 * Reduce-RUM 统一参数化模型  的实现
 *
 * p  = πj * ( (rjk* * γ * β) .... );
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
 *      每测试必须完全包含L个项目，
 *      每个项目类型t的比例必须满足预定义范围[Vt，Ut]，（t = 1，2，.... T）,
 *      每两个测试之间的comm item不得超过预定义的阈值H。
 *
 *      解决CDM-PTA问题是找到帕累托前沿面（PF）中的，即一组非支配解可以实现FD和FS之间的最佳权衡，同时满足测试长度，项目类型分布和重叠阈值。
 *      Pareto解又称非支配解或不受支配解（nondominated solutions）：在有多个目标时，由于存在目标之间的冲突和无法比较的现象，一个解在某个目标上是最好的，在其他的目标上可能是最差的。这些在改进任何目标函数的同时，必然会削弱至少一个其他目标函数的解称为非支配解或Pareto解。
 *
 *
 *
 *
 *
 *
 *    TODO 1.精简 RUM 和 DINA 公共代码     ok
 *
 *    TODO 2.DINA 属性值，ADI一致的校验    5个属性32*32矩阵 和3个属性8*8矩阵的计算  ok
 *
 *                 推导方向：adi --> k_L --> adiList p --> ps/pg、base/penalty
 *
 *                 3个属性：[0.27, 0.8, 0.27, 0.27, 0.8, 0.8, 0.27, 0.8]    属性:(1,0,0) 4+4=8  6+2=8 7+1=8
 *
 *                 5个属性：[0.0, 0.08937, 0.005, 0.08187, 0.04937.....]    属性：(0,1,1,0,1) 8种rum*4个 = 32或者 4n+2m=32
 *
 *
 *                eg属性全部掌握举例验证
 *
 *
 *    TODO 3.以 RUM 为主，进行扩充
 *    TODO      3.1.题库数量  + 惩罚概率值(和pattern无关/范围的设置)      ok
 *    TODO      3.2.GA的实现（硬性要求:长度   软性要求:题型、属性、  exp表达式）  ok  属性比例暂时未完成
 *
 *
 *
 */
public class RumImpl4 {

    private int id ;
    private String pattern ;
    private Double base ;
    private String penalty ;
    private Double adi1_r ;
    private Double adi2_r ;
    private Double adi3_r ;
    private Double adi4_r ;
    private Double adi5_r ;

    /**
     * 获取下标 adiIndex
     */
    private ArrayList<String> adiIndexList1 = new ArrayList();
    private ArrayList<String> adiIndexList2 = new ArrayList();
    private ArrayList<String> adiIndexList3 = new ArrayList();
    private ArrayList<String> adiIndexList4 = new ArrayList();
    private ArrayList<String> adiIndexList5 = new ArrayList();

    private static HashMap<Integer,TYPE>  typeMap = new HashMap<>();
    static {
        typeMap.put(0,TYPE.CHOSE);
        typeMap.put(1,TYPE.FILL);
        typeMap.put(2,TYPE.SHORT);
        typeMap.put(3,TYPE.COMPREHENSIVE);
    }


    /**
     * pattern的索引 用来查询 K_L information
     */
    private static HashMap<String,Integer> indexMap = new HashMap();

    static
    {
        indexMap.put("(0,0,0,0,0)",1);
        indexMap.put("(0,0,0,0,1)",2);
        indexMap.put("(0,0,0,1,0)",3);
        indexMap.put("(0,0,1,0,0)",4);
        indexMap.put("(0,1,0,0,0)",5);
        indexMap.put("(1,0,0,0,0)",6);

        indexMap.put("(0,0,0,1,1)",7);
        indexMap.put("(0,0,1,0,1)",8);
        indexMap.put("(0,1,0,0,1)",9);
        indexMap.put("(1,0,0,0,1)",10);
        indexMap.put("(0,0,1,1,0)",11);
        indexMap.put("(0,1,0,1,0)",12);
        indexMap.put("(1,0,0,1,0)",13);
        indexMap.put("(0,1,1,0,0)",14);
        indexMap.put("(1,0,1,0,0)",15);
        indexMap.put("(1,1,0,0,0)",16);

        indexMap.put("(0,0,1,1,1)",17);
        indexMap.put("(0,1,0,1,1)",18);
        indexMap.put("(1,0,0,1,1)",19);
        indexMap.put("(0,1,1,0,1)",20);
        indexMap.put("(1,0,1,0,1)",21);
        indexMap.put("(1,1,0,0,1)",22);
        indexMap.put("(0,1,1,1,0)",23);
        indexMap.put("(1,0,1,1,0)",24);
        indexMap.put("(1,1,0,1,0)",25);
        indexMap.put("(1,1,1,0,0)",26);

        indexMap.put("(0,1,1,1,1)",27);
        indexMap.put("(1,0,1,1,1)",28);
        indexMap.put("(1,1,0,1,1)",29);
        indexMap.put("(1,1,1,0,1)",30);
        indexMap.put("(1,1,1,1,0)",31);
        indexMap.put("(1,1,1,1,1)",32);
    }


    /**
     * 考生pattern sps
     */
    private ArrayList<String> sps = new ArrayList<String>(){{

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





    //1. 生成题目的 pattern  5个属性，要求属性比例均衡
    //2. 生成 base 和 penalty; 计算 ArrayList<Double> rumLists
    //3. 生成 K_L 矩阵
    //4. 计算试题 一个pattern 对应五个adi (index vs klArray)
    //5. 封装单道题的属性值(id  pattern  base  penalty  adi1_r adi2_r adi3_r adi4_r adi5_r)
    //6. 保存到题库
    
    @Test
    public  void init() throws InterruptedException, SQLException {

        //初始化索引位置 adiIndexList
        getAdiIndex();

        JDBCUtils4 jdbcUtils = new JDBCUtils4();

        //生成题库的试题数  310道  比值:5:10:10:5:1
        int num = 310 ;

        for (int i = 1; i <= num/31*5; i++) {
            id = i;
            start(1);
            //防止题型顺序，导致轮盘赌概率失效问题
            //   解决方案：①类型随机（1/4）  ②hashCode取模
            jdbcUtils.insert(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r);
        }
        for (int i =  num/31*5 + 1 ; i <= num/31*15; i++) {
            id = i;
            start(2);
            jdbcUtils.insert(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r);
        }
        for (int i = num/31*15 + 1; i <= num/31*25; i++) {
            id = i;
            start(3);
            jdbcUtils.insert(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r);
        }
        for (int i = num/31*25 + 1; i <= num/31*30; i++) {
            id = i;
            start(4);
            jdbcUtils.insert(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r);
        }
        for (int i = num/31*30 + 1; i <= num/31*31; i++) {
            id = i;
            start(5);
            jdbcUtils.insert(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r);
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

        getAdi(pattern);

        System.out.printf("id=%s \t pattern=%s \t base=%s \t penalty=%s \t adi1_r=%s \t adi2_r=%s \t adi3_r=%s \t adi4_r=%s \t adi5_r=%s \n", id, pattern, base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r);

        System.out.println();

    }


    /**
     * 以试题pattern为单位,算出一个答题概率的集合 rum，然后求出该试题的矩阵 k_L，
     * 求出该试题的列表 Da，最后平均求出该试题的 adi
     * 可以理解为一道试题下，所有考生的差异性
     */
    private void getAdi(String ip) {


        //试题的pattern 和 base/penalty 个数无关   基线系数/惩罚系数 随机生成
        base = new KLUtils().makeRandom(0.95f, 0.75f, 2);

        ArrayList<Double> rumList = getRumListsRandom(base,ip);
        System.out.println("rumList: " +rumList);


        //根据 rumList 计算出K_L二维数组
        Double[][] klArray = new KLUtils().foreach(rumList);
        //打印
        new KLUtils().arrayPrint(klArray);


        System.out.println("list 遍历; 分别拿list的值去KLArray中匹配寻找：");
        List<Double> calAdiList = calAdiImple(klArray, adiIndexList1, adiIndexList2, adiIndexList3, adiIndexList4, adiIndexList5);
        //打印
        System.out.println(calAdiList);


    }



    /**
     * 返回 rum 的 集合 ArrayList<ArrayList<Double>>
     * @param base  基线系数
     * @param ip 题目的属性pattern
     */
    private ArrayList<Double> getRumListsRandom(Double base, String ip){

        ArrayList<Double> rumLists = new ArrayList<>();


        //题目pattern
        int a1 = Integer.parseInt(ip.substring(1, 2));
        int a2 = Integer.parseInt(ip.substring(3, 4));
        int a3 = Integer.parseInt(ip.substring(5, 6));
        int a4 = Integer.parseInt(ip.substring(7, 8));
        int a5 = Integer.parseInt(ip.substring(9, 10));

        //惩罚概率值[0.65,0.92]   由0.05~0.95 改成0.65~0.92
        double penalty1 = a1 == 1? new KLUtils().makeRandom(0.92f, 0.65f, 2):0;
        double penalty2 = a2 == 1? new KLUtils().makeRandom(0.92f, 0.65f, 2):0;
        double penalty3 = a3 == 1? new KLUtils().makeRandom(0.92f, 0.65f, 2):0;
        double penalty4 = a4 == 1? new KLUtils().makeRandom(0.92f, 0.65f, 2):0;
        double penalty5 = a5 == 1? new KLUtils().makeRandom(0.92f, 0.65f, 2):0;
        penalty = "("+penalty1+","+penalty2+","+penalty3+","+penalty4+","+penalty5+")";

        //根据学生pattern vs 题目pattern 获取答对此题的rum
        for (String ps : sps) {
            //学生pattern
            int b1 = Integer.parseInt(ps.substring(1, 2));
            int b2 = Integer.parseInt(ps.substring(3, 4));
            int b3 = Integer.parseInt(ps.substring(5, 6));
            int b4 = Integer.parseInt(ps.substring(7, 8));
            int b5 = Integer.parseInt(ps.substring(9, 10));

            //若b1 >= a 则*penalty^0; a>b 则*penalty^1  a表示题目考了该属性 b表示考生掌握该属性
            int num1 = b1 >= a1?0:1;
            int num2 = b2 >= a2?0:1;
            int num3 = b3 >= a3?0:1;
            int num4 = b4 >= a4?0:1;
            int num5 = b5 >= a5?0:1;


            double p = base * Math.pow(penalty1, num1) * Math.pow(penalty2, num2) * Math.pow(penalty3, num3) * Math.pow(penalty4, num4) * Math.pow(penalty5, num5) ;

            rumLists.add(p);
        }
        return  rumLists;

    }




    /**
     * 计算adi具体实现
     */
    private List<Double> calAdiImple(Double[][] klArray, ArrayList<String> list1, ArrayList<String> list2, ArrayList<String> list3, ArrayList<String> list4, ArrayList<String> list5){
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
        adi1_r=sum1/list1.size();
        System.out.println("adi1: "+adi1_r);

        for(String data  :    list2)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum2+=v;
        }
        adi2_r=sum2/list2.size();
        System.out.println("adi2: "+adi2_r);

        for(String data  :    list3)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum3+=v;
        }
        adi3_r=sum3/list3.size();
        System.out.println("adi3: "+adi3_r);

        for(String data  :    list4)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum4+=v;
        }
        adi4_r=sum4/list4.size();
        System.out.println("adi4: "+adi4_r);

        for(String data  :    list5)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum5+=v;
        }
        adi5_r=sum5/list5.size();
        System.out.println("adi5: "+adi5_r);


        return new ArrayList<Double>(){{
            add(adi1_r);
            add(adi2_r);
            add(adi3_r);
            add(adi4_r);
            add(adi5_r);
        }};

    }


    private void getAdiIndex(){

        /*
         * 四个元素，遍历组合即可, 随机遍历顺序没影响
         * combineList 全局变量,保存另外几个属性的选取方案
         * combineNum  二进制选取
         */
        ArrayList<String> combineList = new ArrayList<>();
        int combineNum = 2;
        for(int x=0;x<combineNum; x++){
            for(int y=0;y<combineNum; y++) {
                for (int z = 0; z < combineNum; z++) {
                    for (int w = 0; w < combineNum; w++) {
                        String value = x +","+ y +","+ z +","+ w;
                        combineList.add(value);
                    }
                }
            }
        }


        for (int X =0;X<combineList.size();X++){
            //adi1 计算所需要考虑的 pattern
            Integer index11 = indexMap.get("(1," + combineList.get(X) + ")");
            Integer index12 = indexMap.get("(0," + combineList.get(X) + ")");
            Integer index13 = indexMap.get("(0," + combineList.get(X) + ")");
            Integer index14 = indexMap.get("(1," + combineList.get(X) + ")");

            adiIndexList1.add(""+index11+"_"+index12);
            adiIndexList1.add(""+index13+"_"+index14);

            //adi2 计算所需要考虑的 pattern        1,0,1,0
            Integer index21 = indexMap.get("(" +combineList.get(X).substring(0,1)+ ",1," +combineList.get(X).substring(2,7) + ")");
            Integer index22 = indexMap.get("(" +combineList.get(X).substring(0,1)+ ",0," +combineList.get(X).substring(2,7) + ")");
            Integer index23 = indexMap.get("(" +combineList.get(X).substring(0,1)+ ",0," +combineList.get(X).substring(2,7) + ")");
            Integer index24 = indexMap.get("(" +combineList.get(X).substring(0,1)+ ",1," +combineList.get(X).substring(2,7) + ")");

            adiIndexList2.add(""+index21+"_"+index22);
            adiIndexList2.add(""+index23+"_"+index24);


            //adi3 计算所需要考虑的 pattern
            Integer index31 = indexMap.get("("+combineList.get(X).substring(0,3)+",1,"+combineList.get(X).substring(4,7)+ ")");
            Integer index32 = indexMap.get("("+combineList.get(X).substring(0,3)+",0,"+combineList.get(X).substring(4,7)+ ")");
            Integer index33 = indexMap.get("("+combineList.get(X).substring(0,3)+",0,"+combineList.get(X).substring(4,7)+ ")");
            Integer index34 = indexMap.get("("+combineList.get(X).substring(0,3)+",1,"+combineList.get(X).substring(4,7)+ ")");

            adiIndexList3.add(""+index31+"_"+index32);
            adiIndexList3.add(""+index33+"_"+index34);


            //adi4 计算所需要考虑的 pattern
            Integer index41 = indexMap.get("("+combineList.get(X).substring(0,5)+",1,"+combineList.get(X).substring(6,7)+ ")");
            Integer index42 = indexMap.get("("+combineList.get(X).substring(0,5)+",0,"+combineList.get(X).substring(6,7)+ ")");
            Integer index43 = indexMap.get("("+combineList.get(X).substring(0,5)+",0,"+combineList.get(X).substring(6,7)+ ")");
            Integer index44 = indexMap.get("("+combineList.get(X).substring(0,5)+",1,"+combineList.get(X).substring(6,7)+ ")");

            adiIndexList4.add(""+index41+"_"+index42);
            adiIndexList4.add(""+index43+"_"+index44);


            //adi5 计算所需要考虑的 pattern
            Integer index51 = indexMap.get("("+combineList.get(X)+",1)");
            Integer index52 = indexMap.get("("+combineList.get(X)+",0)");
            Integer index53 = indexMap.get("("+combineList.get(X)+",0)");
            Integer index54 = indexMap.get("("+combineList.get(X)+",1)");

            adiIndexList5.add(""+index51+"_"+index52);
            adiIndexList5.add(""+index53+"_"+index54);

        }

         /*
            adi的计算指标：
            adi1个数: 32 具体指标为:[6_1, 1_6, 10_2, 2_10...]
            adi2个数: 32 具体指标为:[5_1, 1_5, 9_2, 2_9...]
            adi3个数: 32 具体指标为:[4_1, 1_4, 8_2, 2_8...]
            adi4个数: 32 具体指标为:[3_1, 1_3, 7_2, 2_7...]
            adi5个数: 32 具体指标为:[2_1, 1_2, 7_3, 3_7...]
         */
        System.out.println("adi的计算指标：");
        System.out.println("adi1个数: "+adiIndexList1.size() +" 具体指标为:"+adiIndexList1);
        System.out.println("adi2个数: "+adiIndexList2.size() +" 具体指标为:"+adiIndexList2);
        System.out.println("adi3个数: "+adiIndexList3.size() +" 具体指标为:"+adiIndexList3);
        System.out.println("adi4个数: "+adiIndexList4.size() +" 具体指标为:"+adiIndexList4);
        System.out.println("adi5个数: "+adiIndexList5.size() +" 具体指标为:"+adiIndexList5);

    }

}



