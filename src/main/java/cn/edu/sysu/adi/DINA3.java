package cn.edu.sysu.adi;

import cn.edu.sysu.pojo.ADI;
import cn.edu.sysu.utils.JDBCUtils;
import cn.edu.sysu.utils.KLUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author : song bei chang
 * @create 2021/5/1 23:28
 *
 *      DINA 模 型 ( Deterministic Inputs，Noisy“And”gate model)
 *      通过的是 学生、试题、知识点 三者之间的关系 三个核心矩阵
 *
 *      符号	     描述
 *      X 	    学生试题得分矩阵
 *      Xij     学生i在试题j上的得分
 *      Q 	    知识点考察矩阵
 *      qjk     试题j对知识点k的考察情况
 *      αi      学生i的知识点掌握情况
 *      αik     学生i对知识点k的掌握情况
 *      ηij     学生i在试题j的潜在作答情况
 *
 *      在掌握了试题j所考察的所有知识点的情况下做错的概率 ps  也应该是每道试题,一个随机值
 *      在并不完全掌握试题j所考察的所有知识点下猜对的概率 pg
 *      容器：适应度值
 *
 *
 *
 *
 *      实现最佳性能的所需测试数量，且同时满足重要在测试长度，项目类型分布和重叠比例
 *      evaluation  test quality: 1) index-oriented and 2) simulation-oriented.
 *      最大程度地提高整体测试质量，最小化测试之间的最大差异，或两者的加权组合。(基于ADI)
 */
public class DINA3 {


      private int id ;
      private String ip ;
      private double ps ;
      private double pg ;
      private double adi1_d ;
      private double adi2_d ;
      private double adi3_d ;
      private ADI adiBean = new ADI();

    /**
     * 获取下标 adiIndex
     */
    private ArrayList<String> adiIndexList1 = new ArrayList();
    private ArrayList<String> adiIndexList2 = new ArrayList();
    private ArrayList<String> adiIndexList3 = new ArrayList();



    private static HashMap<String,Integer> indexMap = new HashMap();

    static
    {
        indexMap.put("(0,0,0)",1);
        indexMap.put("(0,0,1)",2);
        indexMap.put("(0,1,0)",3);
        indexMap.put("(1,0,0)",4);
        indexMap.put("(0,1,1)",5);
        indexMap.put("(1,0,1)",6);
        indexMap.put("(1,1,0)",7);
        indexMap.put("(1,1,1)",8);

    }


    /**
     * 考生_pattern sps
     */
    ArrayList<String> sps = new ArrayList<String>(){{

        add("(0,0,0)");
        add("(0,0,1)");
        add("(0,1,0)");
        add("(1,0,0)");
        add("(0,1,1)");
        add("(1,0,1)");
        add("(1,1,0)");
        add("(1,1,1)");

    }};




    /**
     * 模仿RUM,实现DINA
     *       pattern -- rum -- kl -- adi
     *       1.找相关文献，dina 如何定义 adi  ( 可能存在点难度，查找 + 翻阅 )
     *          1.1 最直接的方式是 将计算方式   由 rum 换成 dina
     *              目前存在问题在于: 同一道试题adi辨别指标一样,且偏小
     *       2.计算出adi
     *       3.指标信息同步到同一套试卷上
     *       4.评价解的好坏 -- 试卷 -- adi的avg/min
     */

    @Test
    public void start() throws Exception {

        //初始化索引位置 adiIndexList
        getAdiIndex();


        GetAdi();


    }


    /**
     * 1. 获取题目id 和 pattern
     * 2. 使用dina  计算adi
     * @param
     */
    public void GetAdi() throws Exception{

        // 从数据库中获取出试题的 pattern ip

        ip = "(0,1,0)";
        // 根据试题的 pattern ，计算出 dina 分数
        ArrayList<Double> dinaList = GetDinaListsRandom(ip);
        System.out.println("dinaList: "+dinaList);

        // 根据 dinaList 计算出K_L二维数组
        Double[][] klArray = new KLUtils().foreach(dinaList);
        // 打印
        new KLUtils().arrayPrint(klArray);


        System.out.println("list 遍历; 分别拿list的值去Array中匹配寻找,并输出其大小：");
        List<Double> calAdiList = CalAdiImple(klArray, adiIndexList1, adiIndexList2, adiIndexList3);
        System.out.println(calAdiList);


    }

    /**
     *
     * @param ip 试题的考察属性
     * @return 长度为 32 位的dinaList
     */
    public ArrayList<Double> GetDinaListsRandom(String ip){

        ArrayList<Double> dinaList = new ArrayList<>();


        // 题目pattern
        // 根据属性 随机生成 ps pg
        int a1 = Integer.parseInt(ip.substring(1, 2));
        int a2 = Integer.parseInt(ip.substring(3, 4));
        int a3 = Integer.parseInt(ip.substring(5, 6));



        // 生成dinaList  猜对率和猜错率    ps pg 是随机值，和pattern 无关
        // 辨别能力很小   [0.0, 0.0, 0.0175, 0.0175, 0.0175]
        // ps[0.05,0.20]  pg[0.20,0.30]
        ps = new KLUtils().makeRandom(0.20f, 0.05f, 2);
        pg = new KLUtils().makeRandom(0.30f, 0.20f, 2);

        System.out.println( "ps: "+ps+" ,pg: "+pg);

        //根据学生pattern vs 题目pattern 获取答对此题的dina分数
        for (String p : sps) {
            //学生pattern
            int b1 = Integer.parseInt(p.substring(1, 2));
            int b2 = Integer.parseInt(p.substring(3, 4));
            int b3 = Integer.parseInt(p.substring(5, 6));


            //全部掌握所考的属性，则潜在答题概率为1，否则默认为0
            boolean ab1 = b1 >= a1;
            boolean ab2 = b2 >= a2;
            boolean ab3 = b3 >= a3;


            int potentResp = 0;
            if(ab1 && ab2 && ab3  ){
                potentResp = 1;
            }

            // DINA 的适应度值计算公式
            double fit = (Math.pow(pg,(1-potentResp))) * (Math.pow((1-ps),potentResp));

            dinaList.add(fit);
        }

        return  dinaList;
    }



    /**
     * 计算adi的具体实现
     */
    public List<Double> CalAdiImple(Double[][] klArray,ArrayList<String> list1,ArrayList<String> list2,ArrayList<String> list3){
        Double sum1 = 0.0;
        Double sum2 = 0.0;
        Double sum3 = 0.0;


        for(String data  :    list1)    {
            String[] spli = data.split("_");
            //注意小数点的位置
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum1+=v;
        }
        adiBean.setAdi1_d(NumCoversion(sum1/list1.size()));
        adi1_d = NumCoversion(sum1/list1.size());
        System.out.println("adi1: "+adiBean.getAdi1_d());

        for(String data  :    list2)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum2+=v;
        }
        adiBean.setAdi2_d(NumCoversion(sum2/list2.size()));
        adi2_d = NumCoversion(sum2/list2.size());
        System.out.println("adi2: "+adiBean.getAdi2_d());

        for(String data  :    list3)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum3+=v;
        }
        adiBean.setAdi3_d(NumCoversion(sum3/list3.size()));
        adi3_d = NumCoversion(sum3/list3.size());
        System.out.println("adi3: "+adiBean.getAdi3_d());




        List<Double> adiList = new ArrayList<Double>(){{
            add(adiBean.getAdi1_d());
            add(adiBean.getAdi2_d());
            add(adiBean.getAdi3_d());

        }};
        return adiList;

    }


    /**
     * 格式转换工具
     */
    public Double NumCoversion(Double adi){

        return Double.valueOf(String.format("%.4f", adi));

    }



    public void getAdiIndex(){

        /*
         * 四个元素，遍历组合即可, 随机遍历顺序没影响
         * combineList 全局变量,保存另外几个属性的选取方案
         * combineNum  二进制选取
         */
        ArrayList<String> combineList = new ArrayList<>();
        int combineNum = 2;
        for(int x=0;x<combineNum; x++){
            for(int y=0;y<combineNum; y++) {
                String value = x +","+ y ;
                combineList.add(value);
            }
        }


        for (int X =0;X<combineList.size();X++){
            //adi1
            Integer index11 = indexMap.get("(1," + combineList.get(X) + ")");
            Integer index12 = indexMap.get("(0," + combineList.get(X) + ")");
            Integer index13 = indexMap.get("(0," + combineList.get(X) + ")");
            Integer index14 = indexMap.get("(1," + combineList.get(X) + ")");

            adiIndexList1.add(""+index11+"_"+index12);
            adiIndexList1.add(""+index13+"_"+index14);

            //adi2         1,0
            Integer index21 = indexMap.get("(" +combineList.get(X).substring(0,1)+ ",1," +combineList.get(X).substring(2,3) + ")");
            Integer index22 = indexMap.get("(" +combineList.get(X).substring(0,1)+ ",0," +combineList.get(X).substring(2,3) + ")");
            Integer index23 = indexMap.get("(" +combineList.get(X).substring(0,1)+ ",0," +combineList.get(X).substring(2,3) + ")");
            Integer index24 = indexMap.get("(" +combineList.get(X).substring(0,1)+ ",1," +combineList.get(X).substring(2,3) + ")");

            adiIndexList2.add(""+index21+"_"+index22);
            adiIndexList2.add(""+index23+"_"+index24);


            //adi3
            Integer index31 = indexMap.get("("+combineList.get(X).substring(0,3)+",1)");
            Integer index32 = indexMap.get("("+combineList.get(X).substring(0,3)+",0)");
            Integer index33 = indexMap.get("("+combineList.get(X).substring(0,3)+",0)");
            Integer index34 = indexMap.get("("+combineList.get(X).substring(0,3)+",1)");

            adiIndexList3.add(""+index31+"_"+index32);
            adiIndexList3.add(""+index33+"_"+index34);



        }

         /*
            adi的计算指标：
            adi1个数: 32 具体指标为:[6_1, 1_6, 10_2, 2_10...]
            adi2个数: 32 具体指标为:[5_1, 1_5, 9_2, 2_9...]
            adi3个数: 32 具体指标为:[4_1, 1_4, 8_2, 2_8...]

         */
        System.out.println("adi的计算指标：");
        System.out.println("adi1个数: "+adiIndexList1.size() +" 具体指标为:"+adiIndexList1);
        System.out.println("adi2个数: "+adiIndexList2.size() +" 具体指标为:"+adiIndexList2);
        System.out.println("adi3个数: "+adiIndexList3.size() +" 具体指标为:"+adiIndexList3);

    }


}



