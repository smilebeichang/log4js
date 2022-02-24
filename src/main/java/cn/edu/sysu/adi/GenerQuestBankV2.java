package cn.edu.sysu.adi;

import cn.edu.sysu.utils.JDBCUtils4;
import cn.edu.sysu.utils.KLUtilsV2;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * @Author : song bei chang
 * @create 2022/02/11 19:34
 *
 * Reduce-RUM 统一参数化模型  的实现
 *
 * p  = πj * ( (rjk* * γ * β) .... );
 *
 * p* 是正确应用第j项所有必要属性的概率，
 * πj* 为项目难度参数，正确作答item j的概率，其值越大（接近1）表示被试掌握了所需属性很可能成功应答。每道题一个固定值
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
 *      每测试长度必须完全包含L个项目，
 *      每个项目类型t的比例必须满足预定义范围[Vt，Ut]，（t = 1，2，.... T）,
 *      每两个测试之间的comm item不得超过预定义的阈值H。
 *
 *      解决CDM-PTA问题是找到帕累托前沿面（PF）中的，即一组非支配解可以实现FD和FS之间的最佳权衡，同时满足测试长度，项目类型分布和重叠阈值。
 *      Pareto解又称非支配解或不受支配解（nondominated solutions）：在有多个目标时，由于存在目标之间的冲突和无法比较的现象，一个解在某个目标上是最好的，在其他的目标上可能是最差的。这些在改进任何目标函数的同时，必然会削弱至少一个其他目标函数的解称为非支配解或Pareto解。
 *
 *
 *       推导方向：adi --> k_L --> adiList p --> ps/pg、base/penalty
 *
 *
 *       5个属性：[0.0, 0.08937, 0.005, 0.08187, 0.04937.....]    属性：(0,1,1,0,1) 8种rum*4个 = 32或者 4n+2m=32
 *
 *
 *       eg属性全部掌握举例验证
 *
 *
 *    TODO 3.以 RUM 为主，进行扩充
 *    TODO      3.1.题库数量  + 惩罚概率值(和pattern无关/范围的设置)      ok
 *    TODO      3.2.GA的实现（硬性要求:长度   软性要求:题型、属性、  exp表达式）  ok
 *
 *
 *
 */
public class GenerQuestBankV2 {

    private int id ;
    // (0,0,0,1,0)
    private String pattern ;
    private Double base ;
    // (0.0,0.0,0.0,0.68,0.0)
    private String penalty ;
    private Double adi1_r ;
    private Double adi2_r ;
    private Double adi3_r ;
    private Double adi4_r ;
    private Double adi5_r ;
    private Double adi6_r ;
    private Double adi7_r ;
    private Double adi8_r ;

    /**
     * 获取下标 adiIndex
     */
    private ArrayList<String> adiIndexList1 = new ArrayList();
    private ArrayList<String> adiIndexList2 = new ArrayList();
    private ArrayList<String> adiIndexList3 = new ArrayList();
    private ArrayList<String> adiIndexList4 = new ArrayList();
    private ArrayList<String> adiIndexList5 = new ArrayList();
    private ArrayList<String> adiIndexList6 = new ArrayList();
    private ArrayList<String> adiIndexList7 = new ArrayList();
    private ArrayList<String> adiIndexList8 = new ArrayList();

    private static HashMap<Integer,TYPE>  typeMap = new HashMap<>();
    static {
        typeMap.put(0,TYPE.CHOSE);
        typeMap.put(1,TYPE.FILL);
        typeMap.put(2,TYPE.SHORT);
        typeMap.put(3,TYPE.COMPREHENSIVE);
    }


    /**
     * pattern的索引 用来查询 K_L information
     * 2^5 = 32   2^8 = 32*2*2*2 = 256
     */
    private static HashMap<String,Integer> indexMap = new HashMap();



    static
    {
        indexMap.put("(0,0,0,0,0,0,0,0)",1);

        indexMap.put("(0,0,0,0,0,0,0,1)",2);
        indexMap.put("(0,0,0,0,0,0,1,0)",3);
        indexMap.put("(0,0,0,0,0,1,0,0)",4);
        indexMap.put("(0,0,0,0,1,0,0,0)",5);
        indexMap.put("(0,0,0,1,0,0,0,0)",6);
        indexMap.put("(0,0,1,0,0,0,0,0)",7);
        indexMap.put("(0,1,0,0,0,0,0,0)",8);
        indexMap.put("(1,0,0,0,0,0,0,0)",9);

        indexMap.put("(0,0,0,0,0,0,1,1)",10);
        indexMap.put("(0,0,0,0,0,1,0,1)",11);
        indexMap.put("(0,0,0,0,1,0,0,1)",12);
        indexMap.put("(0,0,0,1,0,0,0,1)",13);
        indexMap.put("(0,0,1,0,0,0,0,1)",14);
        indexMap.put("(0,1,0,0,0,0,0,1)",15);
        indexMap.put("(1,0,0,0,0,0,0,1)",16);
        indexMap.put("(0,0,0,0,0,1,1,0)",17);
        indexMap.put("(0,0,0,0,1,0,1,0)",18);
        indexMap.put("(0,0,0,1,0,0,1,0)",19);
        indexMap.put("(0,0,1,0,0,0,1,0)",20);
        indexMap.put("(0,1,0,0,0,0,1,0)",21);
        indexMap.put("(1,0,0,0,0,0,1,0)",22);
        indexMap.put("(0,0,0,0,1,1,0,0)",23);
        indexMap.put("(0,0,0,1,0,1,0,0)",24);
        indexMap.put("(0,0,1,0,0,1,0,0)",25);
        indexMap.put("(0,1,0,0,0,1,0,0)",26);
        indexMap.put("(1,0,0,0,0,1,0,0)",27);
        indexMap.put("(0,0,0,1,1,0,0,0)",28);
        indexMap.put("(0,0,1,0,1,0,0,0)",29);
        indexMap.put("(0,1,0,0,1,0,0,0)",30);
        indexMap.put("(1,0,0,0,1,0,0,0)",31);
        indexMap.put("(0,0,1,1,0,0,0,0)",32);
        indexMap.put("(0,1,0,1,0,0,0,0)",33);
        indexMap.put("(1,0,0,1,0,0,0,0)",34);
        indexMap.put("(0,1,1,0,0,0,0,0)",35);
        indexMap.put("(1,0,1,0,0,0,0,0)",36);
        indexMap.put("(1,1,0,0,0,0,0,0)",37);

        indexMap.put("(0,0,0,0,0,1,1,1)",38);
        indexMap.put("(0,0,0,0,1,0,1,1)",39);
        indexMap.put("(0,0,0,1,0,0,1,1)",40);
        indexMap.put("(0,0,1,0,0,0,1,1)",41);
        indexMap.put("(0,1,0,0,0,0,1,1)",42);
        indexMap.put("(1,0,0,0,0,0,1,1)",43);
        indexMap.put("(0,0,0,0,1,1,0,1)",44);
        indexMap.put("(0,0,0,1,0,1,0,1)",45);
        indexMap.put("(0,0,1,0,0,1,0,1)",46);
        indexMap.put("(0,1,0,0,0,1,0,1)",47);
        indexMap.put("(1,0,0,0,0,1,0,1)",48);
        indexMap.put("(0,0,0,1,1,0,0,1)",49);
        indexMap.put("(0,0,1,0,1,0,0,1)",50);
        indexMap.put("(0,1,0,0,1,0,0,1)",51);
        indexMap.put("(1,0,0,0,1,0,0,1)",52);
        indexMap.put("(0,0,1,1,0,0,0,1)",53);
        indexMap.put("(0,1,0,1,0,0,0,1)",54);
        indexMap.put("(1,0,0,1,0,0,0,1)",55);
        indexMap.put("(0,1,1,0,0,0,0,1)",56);
        indexMap.put("(1,0,1,0,0,0,0,1)",57);
        indexMap.put("(1,1,0,0,0,0,0,1)",58);
        indexMap.put("(0,0,0,0,1,1,1,0)",59);
        indexMap.put("(0,0,0,1,0,1,1,0)",60);
        indexMap.put("(0,0,1,0,0,1,1,0)",61);
        indexMap.put("(0,1,0,0,0,1,1,0)",62);
        indexMap.put("(1,0,0,0,0,1,1,0)",63);
        indexMap.put("(0,0,0,1,1,0,1,0)",64);
        indexMap.put("(0,0,1,0,1,0,1,0)",65);
        indexMap.put("(0,1,0,0,1,0,1,0)",66);
        indexMap.put("(1,0,0,0,1,0,1,0)",67);
        indexMap.put("(0,0,1,1,0,0,1,0)",68);
        indexMap.put("(0,1,0,1,0,0,1,0)",69);
        indexMap.put("(1,0,0,1,0,0,1,0)",70);
        indexMap.put("(0,1,1,0,0,0,1,0)",71);
        indexMap.put("(1,0,1,0,0,0,1,0)",72);
        indexMap.put("(1,1,0,0,0,0,1,0)",73);
        indexMap.put("(0,0,0,1,1,1,0,0)",74);
        indexMap.put("(0,0,1,0,1,1,0,0)",75);
        indexMap.put("(0,1,0,0,1,1,0,0)",76);
        indexMap.put("(1,0,0,0,1,1,0,0)",77);
        indexMap.put("(0,0,1,1,0,1,0,0)",78);
        indexMap.put("(0,1,0,1,0,1,0,0)",79);
        indexMap.put("(1,0,0,1,0,1,0,0)",80);
        indexMap.put("(0,1,1,0,0,1,0,0)",81);
        indexMap.put("(1,0,1,0,0,1,0,0)",82);
        indexMap.put("(1,1,0,0,0,1,0,0)",83);
        indexMap.put("(0,0,1,1,1,0,0,0)",84);
        indexMap.put("(0,1,0,1,1,0,0,0)",85);
        indexMap.put("(1,0,0,1,1,0,0,0)",86);
        indexMap.put("(0,1,1,0,1,0,0,0)",87);
        indexMap.put("(1,0,1,0,1,0,0,0)",88);
        indexMap.put("(1,1,0,0,1,0,0,0)",89);
        indexMap.put("(0,1,1,1,0,0,0,0)",90);
        indexMap.put("(1,0,1,1,0,0,0,0)",91);
        indexMap.put("(1,1,0,1,0,0,0,0)",92);
        indexMap.put("(1,1,1,0,0,0,0,0)",93);

        // c84 = 70
        indexMap.put("(0,0,0,0,1,1,1,1)",94);
        indexMap.put("(0,0,0,1,0,1,1,1)",95);
        indexMap.put("(0,0,1,0,0,1,1,1)",96);
        indexMap.put("(0,1,0,0,0,1,1,1)",97);
        indexMap.put("(1,0,0,0,0,1,1,1)",98);
        indexMap.put("(0,0,0,1,1,0,1,1)",99);
        indexMap.put("(0,0,1,0,1,0,1,1)",100);
        indexMap.put("(0,1,0,0,1,0,1,1)",101);
        indexMap.put("(1,0,0,0,1,0,1,1)",102);
        indexMap.put("(0,0,1,1,0,0,1,1)",103);
        indexMap.put("(0,1,0,1,0,0,1,1)",104);
        indexMap.put("(1,0,0,1,0,0,1,1)",105);
        indexMap.put("(0,1,1,0,0,0,1,1)",106);
        indexMap.put("(1,0,1,0,0,0,1,1)",107);
        indexMap.put("(1,1,0,0,0,0,1,1)",108);
        indexMap.put("(0,0,0,1,1,1,0,1)",109);
        indexMap.put("(0,0,1,0,1,1,0,1)",110);
        indexMap.put("(0,1,0,0,1,1,0,1)",111);
        indexMap.put("(1,0,0,0,1,1,0,1)",112);
        indexMap.put("(0,0,1,1,0,1,0,1)",113);
        indexMap.put("(0,1,0,1,0,1,0,1)",114);
        indexMap.put("(1,0,0,1,0,1,0,1)",115);
        indexMap.put("(0,1,1,0,0,1,0,1)",116);
        indexMap.put("(1,0,1,0,0,1,0,1)",117);
        indexMap.put("(1,1,0,0,0,1,0,1)",118);
        indexMap.put("(0,0,1,1,1,0,0,1)",119);
        indexMap.put("(0,1,0,1,1,0,0,1)",120);
        indexMap.put("(1,0,0,1,1,0,0,1)",121);
        indexMap.put("(0,1,1,0,1,0,0,1)",122);
        indexMap.put("(1,0,1,0,1,0,0,1)",123);
        indexMap.put("(1,1,0,0,1,0,0,1)",124);
        indexMap.put("(0,1,1,1,0,0,0,1)",125);
        indexMap.put("(1,0,1,1,0,0,0,1)",126);
        indexMap.put("(1,1,0,1,0,0,0,1)",127);
        indexMap.put("(1,1,1,0,0,0,0,1)",128);

        indexMap.put("(0,0,0,1,1,1,1,0)",129);
        indexMap.put("(0,0,1,0,1,1,1,0)",130);
        indexMap.put("(0,1,0,0,1,1,1,0)",131);
        indexMap.put("(1,0,0,0,1,1,1,0)",132);
        indexMap.put("(0,0,1,1,0,1,1,0)",133);
        indexMap.put("(0,1,0,1,0,1,1,0)",134);
        indexMap.put("(1,0,0,1,0,1,1,0)",135);
        indexMap.put("(0,1,1,0,0,1,1,0)",136);
        indexMap.put("(1,0,1,0,0,1,1,0)",137);
        indexMap.put("(1,1,0,0,0,1,1,0)",138);

        indexMap.put("(0,0,1,1,1,0,1,0)",139);
        indexMap.put("(0,1,0,1,1,0,1,0)",140);
        indexMap.put("(1,0,0,1,1,0,1,0)",141);
        indexMap.put("(0,1,1,0,1,0,1,0)",142);
        indexMap.put("(1,0,1,0,1,0,1,0)",143);
        indexMap.put("(1,1,0,0,1,0,1,0)",144);
        indexMap.put("(0,1,1,1,0,0,1,0)",145);
        indexMap.put("(1,0,1,1,0,0,1,0)",146);
        indexMap.put("(1,1,0,1,0,0,1,0)",147);
        indexMap.put("(1,1,1,0,0,0,1,0)",148);

        indexMap.put("(0,0,1,1,1,1,0,0)",149);
        indexMap.put("(0,1,0,1,1,1,0,0)",150);
        indexMap.put("(1,0,0,1,1,1,0,0)",151);
        indexMap.put("(0,1,1,0,1,1,0,0)",152);
        indexMap.put("(1,0,1,0,1,1,0,0)",153);
        indexMap.put("(1,1,0,0,1,1,0,0)",154);
        indexMap.put("(0,1,1,1,0,1,0,0)",155);
        indexMap.put("(1,0,1,1,0,1,0,0)",156);
        indexMap.put("(1,1,0,1,0,1,0,0)",157);
        indexMap.put("(1,1,1,0,0,1,0,0)",158);
        indexMap.put("(0,1,1,1,1,0,0,0)",159);
        indexMap.put("(1,0,1,1,1,0,0,0)",160);
        indexMap.put("(1,1,0,1,1,0,0,0)",161);
        indexMap.put("(1,1,1,0,1,0,0,0)",162);
        indexMap.put("(1,1,1,1,0,0,0,0)",163);

        indexMap.put("(0,0,0,1,1,1,1,1)",164);
        indexMap.put("(0,0,1,0,1,1,1,1)",165);
        indexMap.put("(0,1,0,0,1,1,1,1)",166);
        indexMap.put("(1,0,0,0,1,1,1,1)",167);
        indexMap.put("(0,0,1,1,0,1,1,1)",168);
        indexMap.put("(0,1,0,1,0,1,1,1)",169);
        indexMap.put("(1,0,0,1,0,1,1,1)",170);
        indexMap.put("(0,1,1,0,0,1,1,1)",171);
        indexMap.put("(1,0,1,0,0,1,1,1)",172);
        indexMap.put("(1,1,0,0,0,1,1,1)",173);
        indexMap.put("(0,0,1,1,1,0,1,1)",174);
        indexMap.put("(0,1,0,1,1,0,1,1)",175);
        indexMap.put("(1,0,0,1,1,0,1,1)",176);
        indexMap.put("(0,1,1,0,1,0,1,1)",177);
        indexMap.put("(1,0,1,0,1,0,1,1)",178);
        indexMap.put("(1,1,0,0,1,0,1,1)",179);
        indexMap.put("(0,1,1,1,0,0,1,1)",180);
        indexMap.put("(1,0,1,1,0,0,1,1)",181);
        indexMap.put("(1,1,0,1,0,0,1,1)",182);
        indexMap.put("(1,1,1,0,0,0,1,1)",183);
        indexMap.put("(0,0,1,1,1,1,0,1)",184);
        indexMap.put("(0,1,0,1,1,1,0,1)",185);
        indexMap.put("(1,0,0,1,1,1,0,1)",186);
        indexMap.put("(0,1,1,0,1,1,0,1)",187);
        indexMap.put("(1,0,1,0,1,1,0,1)",188);
        indexMap.put("(1,1,0,0,1,1,0,1)",189);
        indexMap.put("(0,1,1,1,0,1,0,1)",190);
        indexMap.put("(1,0,1,1,0,1,0,1)",191);
        indexMap.put("(1,1,0,1,0,1,0,1)",192);
        indexMap.put("(1,1,1,0,0,1,0,1)",193);
        indexMap.put("(0,1,1,1,1,0,0,1)",194);
        indexMap.put("(1,0,1,1,1,0,0,1)",195);
        indexMap.put("(1,1,0,1,1,0,0,1)",196);
        indexMap.put("(1,1,1,0,1,0,0,1)",197);
        indexMap.put("(1,1,1,1,0,0,0,1)",198);
        indexMap.put("(0,0,1,1,1,1,1,0)",199);
        indexMap.put("(0,1,0,1,1,1,1,0)",200);
        indexMap.put("(1,0,0,1,1,1,1,0)",201);
        indexMap.put("(0,1,1,0,1,1,1,0)",202);
        indexMap.put("(1,0,1,0,1,1,1,0)",203);
        indexMap.put("(1,1,0,0,1,1,1,0)",204);
        indexMap.put("(0,1,1,1,0,1,1,0)",205);
        indexMap.put("(1,0,1,1,0,1,1,0)",206);
        indexMap.put("(1,1,0,1,0,1,1,0)",207);
        indexMap.put("(1,1,1,0,0,1,1,0)",208);
        indexMap.put("(0,1,1,1,1,0,1,0)",209);
        indexMap.put("(1,0,1,1,1,0,1,0)",210);
        indexMap.put("(1,1,0,1,1,0,1,0)",211);
        indexMap.put("(1,1,1,0,1,0,1,0)",212);
        indexMap.put("(1,1,1,1,0,0,1,0)",213);
        indexMap.put("(0,1,1,1,1,1,0,0)",214);
        indexMap.put("(1,0,1,1,1,1,0,0)",215);
        indexMap.put("(1,1,0,1,1,1,0,0)",216);
        indexMap.put("(1,1,1,0,1,1,0,0)",217);
        indexMap.put("(1,1,1,1,0,1,0,0)",218);
        indexMap.put("(1,1,1,1,1,0,0,0)",219);

        indexMap.put("(0,0,1,1,1,1,1,1)",220);
        indexMap.put("(0,1,0,1,1,1,1,1)",221);
        indexMap.put("(0,1,1,0,1,1,1,1)",222);
        indexMap.put("(0,1,1,1,0,1,1,1)",223);
        indexMap.put("(0,1,1,1,1,0,1,1)",224);
        indexMap.put("(0,1,1,1,1,1,0,1)",225);
        indexMap.put("(0,1,1,1,1,1,1,0)",226);
        indexMap.put("(1,0,0,1,1,1,1,1)",227);
        indexMap.put("(1,0,1,0,1,1,1,1)",228);
        indexMap.put("(1,0,1,1,0,1,1,1)",229);
        indexMap.put("(1,0,1,1,1,0,1,1)",230);
        indexMap.put("(1,0,1,1,1,1,0,1)",231);
        indexMap.put("(1,0,1,1,1,1,1,0)",232);
        indexMap.put("(1,1,0,0,1,1,1,1)",233);
        indexMap.put("(1,1,0,1,0,1,1,1)",234);
        indexMap.put("(1,1,0,1,1,0,1,1)",235);
        indexMap.put("(1,1,0,1,1,1,0,1)",236);
        indexMap.put("(1,1,0,1,1,1,1,0)",237);
        indexMap.put("(1,1,1,0,0,1,1,1)",238);
        indexMap.put("(1,1,1,0,1,0,1,1)",239);
        indexMap.put("(1,1,1,0,1,1,0,1)",240);
        indexMap.put("(1,1,1,0,1,1,1,0)",241);
        indexMap.put("(1,1,1,1,0,0,1,1)",242);
        indexMap.put("(1,1,1,1,0,1,0,1)",243);
        indexMap.put("(1,1,1,1,0,1,1,0)",244);
        indexMap.put("(1,1,1,1,1,0,0,1)",245);
        indexMap.put("(1,1,1,1,1,0,1,0)",246);
        indexMap.put("(1,1,1,1,1,1,0,0)",247);

        indexMap.put("(0,1,1,1,1,1,1,1)",248);
        indexMap.put("(1,0,1,1,1,1,1,1)",249);
        indexMap.put("(1,1,0,1,1,1,1,1)",250);
        indexMap.put("(1,1,1,0,1,1,1,1)",251);
        indexMap.put("(1,1,1,1,0,1,1,1)",252);
        indexMap.put("(1,1,1,1,1,0,1,1)",253);
        indexMap.put("(1,1,1,1,1,1,0,1)",254);
        indexMap.put("(1,1,1,1,1,1,1,0)",255);

        indexMap.put("(1,1,1,1,1,1,1,1)",256);

    }


    /**
     * 考生pattern sps
     */
    private ArrayList<String> sps = new ArrayList<String>(){{


        add("(0,0,0,0,0,0,0,0)");

        add("(0,0,0,0,0,0,0,1)");
        add("(0,0,0,0,0,0,1,0)");
        add("(0,0,0,0,0,1,0,0)");
        add("(0,0,0,0,1,0,0,0)");
        add("(0,0,0,1,0,0,0,0)");
        add("(0,0,1,0,0,0,0,0)");
        add("(0,1,0,0,0,0,0,0)");
        add("(1,0,0,0,0,0,0,0)");

        add("(0,0,0,0,0,0,1,1)");
        add("(0,0,0,0,0,1,0,1)");
        add("(0,0,0,0,1,0,0,1)");
        add("(0,0,0,1,0,0,0,1)");
        add("(0,0,1,0,0,0,0,1)");
        add("(0,1,0,0,0,0,0,1)");
        add("(1,0,0,0,0,0,0,1)");
        add("(0,0,0,0,0,1,1,0)");
        add("(0,0,0,0,1,0,1,0)");
        add("(0,0,0,1,0,0,1,0)");
        add("(0,0,1,0,0,0,1,0)");
        add("(0,1,0,0,0,0,1,0)");
        add("(1,0,0,0,0,0,1,0)");
        add("(0,0,0,0,1,1,0,0)");
        add("(0,0,0,1,0,1,0,0)");
        add("(0,0,1,0,0,1,0,0)");
        add("(0,1,0,0,0,1,0,0)");
        add("(1,0,0,0,0,1,0,0)");
        add("(0,0,0,1,1,0,0,0)");
        add("(0,0,1,0,1,0,0,0)");
        add("(0,1,0,0,1,0,0,0)");
        add("(1,0,0,0,1,0,0,0)");
        add("(0,0,1,1,0,0,0,0)");
        add("(0,1,0,1,0,0,0,0)");
        add("(1,0,0,1,0,0,0,0)");
        add("(0,1,1,0,0,0,0,0)");
        add("(1,0,1,0,0,0,0,0)");
        add("(1,1,0,0,0,0,0,0)");

        add("(0,0,0,0,0,1,1,1)");
        add("(0,0,0,0,1,0,1,1)");
        add("(0,0,0,1,0,0,1,1)");
        add("(0,0,1,0,0,0,1,1)");
        add("(0,1,0,0,0,0,1,1)");
        add("(1,0,0,0,0,0,1,1)");
        add("(0,0,0,0,1,1,0,1)");
        add("(0,0,0,1,0,1,0,1)");
        add("(0,0,1,0,0,1,0,1)");
        add("(0,1,0,0,0,1,0,1)");
        add("(1,0,0,0,0,1,0,1)");
        add("(0,0,0,1,1,0,0,1)");
        add("(0,0,1,0,1,0,0,1)");
        add("(0,1,0,0,1,0,0,1)");
        add("(1,0,0,0,1,0,0,1)");
        add("(0,0,1,1,0,0,0,1)");
        add("(0,1,0,1,0,0,0,1)");
        add("(1,0,0,1,0,0,0,1)");
        add("(0,1,1,0,0,0,0,1)");
        add("(1,0,1,0,0,0,0,1)");
        add("(1,1,0,0,0,0,0,1)");
        add("(0,0,0,0,1,1,1,0)");
        add("(0,0,0,1,0,1,1,0)");
        add("(0,0,1,0,0,1,1,0)");
        add("(0,1,0,0,0,1,1,0)");
        add("(1,0,0,0,0,1,1,0)");
        add("(0,0,0,1,1,0,1,0)");
        add("(0,0,1,0,1,0,1,0)");
        add("(0,1,0,0,1,0,1,0)");
        add("(1,0,0,0,1,0,1,0)");
        add("(0,0,1,1,0,0,1,0)");
        add("(0,1,0,1,0,0,1,0)");
        add("(1,0,0,1,0,0,1,0)");
        add("(0,1,1,0,0,0,1,0)");
        add("(1,0,1,0,0,0,1,0)");
        add("(1,1,0,0,0,0,1,0)");
        add("(0,0,0,1,1,1,0,0)");
        add("(0,0,1,0,1,1,0,0)");
        add("(0,1,0,0,1,1,0,0)");
        add("(1,0,0,0,1,1,0,0)");
        add("(0,0,1,1,0,1,0,0)");
        add("(0,1,0,1,0,1,0,0)");
        add("(1,0,0,1,0,1,0,0)");
        add("(0,1,1,0,0,1,0,0)");
        add("(1,0,1,0,0,1,0,0)");
        add("(1,1,0,0,0,1,0,0)");
        add("(0,0,1,1,1,0,0,0)");
        add("(0,1,0,1,1,0,0,0)");
        add("(1,0,0,1,1,0,0,0)");
        add("(0,1,1,0,1,0,0,0)");
        add("(1,0,1,0,1,0,0,0)");
        add("(1,1,0,0,1,0,0,0)");
        add("(0,1,1,1,0,0,0,0)");
        add("(1,0,1,1,0,0,0,0)");
        add("(1,1,0,1,0,0,0,0)");
        add("(1,1,1,0,0,0,0,0)");


        add("(0,0,0,0,1,1,1,1)");
        add("(0,0,0,1,0,1,1,1)");
        add("(0,0,1,0,0,1,1,1)");
        add("(0,1,0,0,0,1,1,1)");
        add("(1,0,0,0,0,1,1,1)");
        add("(0,0,0,1,1,0,1,1)");
        add("(0,0,1,0,1,0,1,1)");
        add("(0,1,0,0,1,0,1,1)");
        add("(1,0,0,0,1,0,1,1)");
        add("(0,0,1,1,0,0,1,1)");
        add("(0,1,0,1,0,0,1,1)");
        add("(1,0,0,1,0,0,1,1)");
        add("(0,1,1,0,0,0,1,1)");
        add("(1,0,1,0,0,0,1,1)");
        add("(1,1,0,0,0,0,1,1)");
        add("(0,0,0,1,1,1,0,1)");
        add("(0,0,1,0,1,1,0,1)");
        add("(0,1,0,0,1,1,0,1)");
        add("(1,0,0,0,1,1,0,1)");
        add("(0,0,1,1,0,1,0,1)");
        add("(0,1,0,1,0,1,0,1)");
        add("(1,0,0,1,0,1,0,1)");
        add("(0,1,1,0,0,1,0,1)");
        add("(1,0,1,0,0,1,0,1)");
        add("(1,1,0,0,0,1,0,1)");
        add("(0,0,1,1,1,0,0,1)");
        add("(0,1,0,1,1,0,0,1)");
        add("(1,0,0,1,1,0,0,1)");
        add("(0,1,1,0,1,0,0,1)");
        add("(1,0,1,0,1,0,0,1)");
        add("(1,1,0,0,1,0,0,1)");
        add("(0,1,1,1,0,0,0,1)");
        add("(1,0,1,1,0,0,0,1)");
        add("(1,1,0,1,0,0,0,1)");
        add("(1,1,1,0,0,0,0,1)");

        add("(0,0,0,1,1,1,1,0)");
        add("(0,0,1,0,1,1,1,0)");
        add("(0,1,0,0,1,1,1,0)");
        add("(1,0,0,0,1,1,1,0)");
        add("(0,0,1,1,0,1,1,0)");
        add("(0,1,0,1,0,1,1,0)");
        add("(1,0,0,1,0,1,1,0)");
        add("(0,1,1,0,0,1,1,0)");
        add("(1,0,1,0,0,1,1,0)");
        add("(1,1,0,0,0,1,1,0)");

        add("(0,0,1,1,1,0,1,0)");
        add("(0,1,0,1,1,0,1,0)");
        add("(1,0,0,1,1,0,1,0)");
        add("(0,1,1,0,1,0,1,0)");
        add("(1,0,1,0,1,0,1,0)");
        add("(1,1,0,0,1,0,1,0)");
        add("(0,1,1,1,0,0,1,0)");
        add("(1,0,1,1,0,0,1,0)");
        add("(1,1,0,1,0,0,1,0)");
        add("(1,1,1,0,0,0,1,0)");

        add("(0,0,1,1,1,1,0,0)");
        add("(0,1,0,1,1,1,0,0)");
        add("(1,0,0,1,1,1,0,0)");
        add("(0,1,1,0,1,1,0,0)");
        add("(1,0,1,0,1,1,0,0)");
        add("(1,1,0,0,1,1,0,0)");
        add("(0,1,1,1,0,1,0,0)");
        add("(1,0,1,1,0,1,0,0)");
        add("(1,1,0,1,0,1,0,0)");
        add("(1,1,1,0,0,1,0,0)");
        add("(0,1,1,1,1,0,0,0)");
        add("(1,0,1,1,1,0,0,0)");
        add("(1,1,0,1,1,0,0,0)");
        add("(1,1,1,0,1,0,0,0)");
        add("(1,1,1,1,0,0,0,0)");

        add("(0,0,0,1,1,1,1,1)");
        add("(0,0,1,0,1,1,1,1)");
        add("(0,1,0,0,1,1,1,1)");
        add("(1,0,0,0,1,1,1,1)");
        add("(0,0,1,1,0,1,1,1)");
        add("(0,1,0,1,0,1,1,1)");
        add("(1,0,0,1,0,1,1,1)");
        add("(0,1,1,0,0,1,1,1)");
        add("(1,0,1,0,0,1,1,1)");
        add("(1,1,0,0,0,1,1,1)");
        add("(0,0,1,1,1,0,1,1)");
        add("(0,1,0,1,1,0,1,1)");
        add("(1,0,0,1,1,0,1,1)");
        add("(0,1,1,0,1,0,1,1)");
        add("(1,0,1,0,1,0,1,1)");
        add("(1,1,0,0,1,0,1,1)");
        add("(0,1,1,1,0,0,1,1)");
        add("(1,0,1,1,0,0,1,1)");
        add("(1,1,0,1,0,0,1,1)");
        add("(1,1,1,0,0,0,1,1)");
        add("(0,0,1,1,1,1,0,1)");
        add("(0,1,0,1,1,1,0,1)");
        add("(1,0,0,1,1,1,0,1)");
        add("(0,1,1,0,1,1,0,1)");
        add("(1,0,1,0,1,1,0,1)");
        add("(1,1,0,0,1,1,0,1)");
        add("(0,1,1,1,0,1,0,1)");
        add("(1,0,1,1,0,1,0,1)");
        add("(1,1,0,1,0,1,0,1)");
        add("(1,1,1,0,0,1,0,1)");
        add("(0,1,1,1,1,0,0,1)");
        add("(1,0,1,1,1,0,0,1)");
        add("(1,1,0,1,1,0,0,1)");
        add("(1,1,1,0,1,0,0,1)");
        add("(1,1,1,1,0,0,0,1)");
        add("(0,0,1,1,1,1,1,0)");
        add("(0,1,0,1,1,1,1,0)");
        add("(1,0,0,1,1,1,1,0)");
        add("(0,1,1,0,1,1,1,0)");
        add("(1,0,1,0,1,1,1,0)");
        add("(1,1,0,0,1,1,1,0)");
        add("(0,1,1,1,0,1,1,0)");
        add("(1,0,1,1,0,1,1,0)");
        add("(1,1,0,1,0,1,1,0)");
        add("(1,1,1,0,0,1,1,0)");
        add("(0,1,1,1,1,0,1,0)");
        add("(1,0,1,1,1,0,1,0)");
        add("(1,1,0,1,1,0,1,0)");
        add("(1,1,1,0,1,0,1,0)");
        add("(1,1,1,1,0,0,1,0)");
        add("(0,1,1,1,1,1,0,0)");
        add("(1,0,1,1,1,1,0,0)");
        add("(1,1,0,1,1,1,0,0)");
        add("(1,1,1,0,1,1,0,0)");
        add("(1,1,1,1,0,1,0,0)");
        add("(1,1,1,1,1,0,0,0)");

        add("(0,0,1,1,1,1,1,1)");
        add("(0,1,0,1,1,1,1,1)");
        add("(0,1,1,0,1,1,1,1)");
        add("(0,1,1,1,0,1,1,1)");
        add("(0,1,1,1,1,0,1,1)");
        add("(0,1,1,1,1,1,0,1)");
        add("(0,1,1,1,1,1,1,0)");
        add("(1,0,0,1,1,1,1,1)");
        add("(1,0,1,0,1,1,1,1)");
        add("(1,0,1,1,0,1,1,1)");
        add("(1,0,1,1,1,0,1,1)");
        add("(1,0,1,1,1,1,0,1)");
        add("(1,0,1,1,1,1,1,0)");
        add("(1,1,0,0,1,1,1,1)");
        add("(1,1,0,1,0,1,1,1)");
        add("(1,1,0,1,1,0,1,1)");
        add("(1,1,0,1,1,1,0,1)");
        add("(1,1,0,1,1,1,1,0)");
        add("(1,1,1,0,0,1,1,1)");
        add("(1,1,1,0,1,0,1,1)");
        add("(1,1,1,0,1,1,0,1)");
        add("(1,1,1,0,1,1,1,0)");
        add("(1,1,1,1,0,0,1,1)");
        add("(1,1,1,1,0,1,0,1)");
        add("(1,1,1,1,0,1,1,0)");
        add("(1,1,1,1,1,0,0,1)");
        add("(1,1,1,1,1,0,1,0)");
        add("(1,1,1,1,1,1,0,0)");

        add("(0,1,1,1,1,1,1,1)");
        add("(1,0,1,1,1,1,1,1)");
        add("(1,1,0,1,1,1,1,1)");
        add("(1,1,1,0,1,1,1,1)");
        add("(1,1,1,1,0,1,1,1)");
        add("(1,1,1,1,1,0,1,1)");
        add("(1,1,1,1,1,1,0,1)");
        add("(1,1,1,1,1,1,1,0)");

        add("(1,1,1,1,1,1,1,1)");

    }};





    //1. 生成题目的 pattern  8个属性，要求属性比例均衡
    //2. 生成 base 和 penalty; 计算 ArrayList<Double> rumLists
    //3. 生成 K_L 矩阵
    //4. 计算试题 一个pattern 对应8个adi (index vs klArray)
    //5. 封装单道题的属性值(id  pattern  base  penalty  adi1_r adi2_r ...)
    //6. 保存到题库
    
    @Test
    public  void init() throws InterruptedException, SQLException {

        //初始化索引位置 adiIndexList 目的是什么
        getAdiIndex();

        JDBCUtils4 jdbcUtils = new JDBCUtils4();

        //生成题库的试题数  1000道
        int num = 1000 ;

        // 需要确认一下比例, 1000道题8个属性
        /**
         * 之前是 500 道题 5 个属性
         * 1~80,80~240,240~400,400~480,480~500
         * 80:160:160:80:20
         * c51:c52:c53:c54:c55
         * c80:c81:c82:c83:c84:c85:c86:c87:c88+c88
         * 8:28:56:70:56:28:8:2
         * (8+28+56+70+56+28+8)/8
         *
         */

        for (int i = 1; i <= num/250*8; i++) {
            id = i;
            start(1);
            //防止题型顺序，导致轮盘赌概率失效问题
            //   解决方案：①类型随机（1/4）  ②hashCode取模
            jdbcUtils.insertV2(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r,adi6_r,adi7_r,adi8_r);
        }
        for (int i =  (num/250)*8 + 1 ; i <= (num/250)*8*4; i++) {
            id = i;
            start(2);
            jdbcUtils.insertV2(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r,adi6_r,adi7_r,adi8_r);
        }
        for (int i = (num/250*8*4) + 1; i <= num/250*8*10; i++) {
            id = i;
            start(3);
            jdbcUtils.insertV2(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r,adi6_r,adi7_r,adi8_r);
        }
        for (int i = (num/250)*8*10 + 1; i <= (num/250)*8*20; i++) {
            id = i;
            start(4);
            jdbcUtils.insertV2(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r,adi6_r,adi7_r,adi8_r);
        }
        for (int i = (num/250)*8*20 + 1; i <= (num/250)*8*27; i++) {
            id = i;
            start(5);
            jdbcUtils.insertV2(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r,adi6_r,adi7_r,adi8_r);
        }
        for (int i = (num/250)*8*27 + 1; i <= (num/250)*8*30; i++) {
            id = i;
            start(6);
            jdbcUtils.insertV2(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r,adi6_r,adi7_r,adi8_r);
        }
        for (int i = (num/250)*8*30 + 1; i <= (num/250)*8*31; i++) {
            id = i;
            start(7);
            jdbcUtils.insertV2(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r,adi6_r,adi7_r,adi8_r);
        }
        for (int i = (num/250)*8*31 + 1; i <= 1000; i++) {
            id = i;
            start(8);
            jdbcUtils.insertV2(id,typeMap.get(Math.floorMod(id,4)),pattern,base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r,adi6_r,adi7_r,adi8_r);
        }


    }

    /**
     *  rum 的具体实现
     *      1. 根据属性个数，生成试题pattern
     *      2. 获取adi指标
     *      3. 将上述信息，保存到全局变量
     */
    public void start(int num) throws InterruptedException {

        // 随机生成带有num 个属性的 pattern
        pattern = new KLUtilsV2().randomInit(num);

        // 计算每个属性 adi 的值
        getAdi(pattern);

        System.out.printf(
                "id=%s \t pattern=%s \t base=%s \t penalty=%s \t adi1_r=%s \t adi2_r=%s \t adi3_r=%s \t adi4_r=%s \t adi5_r=%s \n adi6_r=%s \n adi7_r=%s \n adi8_r=%s \n",
                id, pattern, base,penalty,adi1_r,adi2_r,adi3_r,adi4_r,adi5_r,adi6_r,adi7_r,adi8_r);

        System.out.println();

    }


    /**
     * 以试题pattern为单位,算出一个答题概率的集合 rum，然后求出该试题的矩阵 k_L，
     * 求出该试题的列表 Da，最后平均求出该试题的 adi
     * 可以理解为一道试题下，所有考生的差异性
     */
    private void getAdi(String ip) {


        //试题的pattern 和 base/penalty 个数无关   基线系数/惩罚系数 随机生成
        base = new KLUtilsV2().makeRandom(0.95f, 0.75f, 2);

        ArrayList<Double> rumList = getRumListsRandom(base,ip);
        System.out.println("rumList: " +rumList);


        //根据 rumList 计算出 K_L 二维数组
        Double[][] klArray = new KLUtilsV2().foreach(rumList);
        //打印
        new KLUtilsV2().arrayPrint(klArray);


        System.out.println("list 遍历; 分别拿list的值去KLArray中匹配寻找：");
        List<Double> calAdiList = calAdiImple(
                klArray, adiIndexList1, adiIndexList2, adiIndexList3,
                adiIndexList4, adiIndexList5, adiIndexList6,
                adiIndexList7, adiIndexList8);
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
        int a6 = Integer.parseInt(ip.substring(11, 12));
        int a7 = Integer.parseInt(ip.substring(13, 14));
        int a8 = Integer.parseInt(ip.substring(15, 16));

        //惩罚概率值[0.65,0.92]   由0.05~0.95 改成0.65~0.92
        double penalty1 = a1 == 1? new KLUtilsV2().makeRandom(0.92f, 0.65f, 2):0;
        double penalty2 = a2 == 1? new KLUtilsV2().makeRandom(0.92f, 0.65f, 2):0;
        double penalty3 = a3 == 1? new KLUtilsV2().makeRandom(0.92f, 0.65f, 2):0;
        double penalty4 = a4 == 1? new KLUtilsV2().makeRandom(0.92f, 0.65f, 2):0;
        double penalty5 = a5 == 1? new KLUtilsV2().makeRandom(0.92f, 0.65f, 2):0;
        double penalty6 = a6 == 1? new KLUtilsV2().makeRandom(0.92f, 0.65f, 2):0;
        double penalty7 = a7 == 1? new KLUtilsV2().makeRandom(0.92f, 0.65f, 2):0;
        double penalty8 = a8 == 1? new KLUtilsV2().makeRandom(0.92f, 0.65f, 2):0;
        penalty = "("+penalty1+","+penalty2+","+penalty3+","+penalty4+","+penalty5+","+penalty6+","+penalty7+","+penalty8+")";

        //根据学生pattern vs 题目pattern 获取答对此题的rum
        for (String ps : sps) {
            //学生pattern
            int b1 = Integer.parseInt(ps.substring(1, 2));
            int b2 = Integer.parseInt(ps.substring(3, 4));
            int b3 = Integer.parseInt(ps.substring(5, 6));
            int b4 = Integer.parseInt(ps.substring(7, 8));
            int b5 = Integer.parseInt(ps.substring(9, 10));
            int b6 = Integer.parseInt(ps.substring(11, 12));
            int b7 = Integer.parseInt(ps.substring(13, 14));
            int b8 = Integer.parseInt(ps.substring(15, 16));

            //若b1 >= a 则*penalty^0; a>b 则*penalty^1  a表示题目考了该属性 b表示考生掌握该属性
            int num1 = b1 >= a1?0:1;
            int num2 = b2 >= a2?0:1;
            int num3 = b3 >= a3?0:1;
            int num4 = b4 >= a4?0:1;
            int num5 = b5 >= a5?0:1;
            int num6 = b6 >= a6?0:1;
            int num7 = b7 >= a7?0:1;
            int num8 = b8 >= a8?0:1;


            double p = base * Math.pow(penalty1, num1) * Math.pow(penalty2, num2) * Math.pow(penalty3, num3)
                    * Math.pow(penalty4, num4) * Math.pow(penalty5, num5)* Math.pow(penalty6, num6)
                    * Math.pow(penalty7, num7)* Math.pow(penalty8, num8) ;

            rumLists.add(p);
        }
        return  rumLists;

    }




    /**
     * 计算adi具体实现
     */
    private List<Double> calAdiImple(Double[][] klArray,
                                     ArrayList<String> list1,
                                     ArrayList<String> list2,
                                     ArrayList<String> list3,
                                     ArrayList<String> list4,
                                     ArrayList<String> list5,
                                     ArrayList<String> list6,
                                     ArrayList<String> list7,
                                     ArrayList<String> list8){
        Double sum1 = 0.0;
        Double sum2 = 0.0;
        Double sum3 = 0.0;
        Double sum4 = 0.0;
        Double sum5 = 0.0;
        Double sum6 = 0.0;
        Double sum7 = 0.0;
        Double sum8 = 0.0;


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

        for(String data  :    list6)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum6+=v;
        }
        adi6_r=sum6/list6.size();
        System.out.println("adi6: "+adi6_r);

        for(String data  :    list7)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum7+=v;
        }
        adi7_r=sum7/list7.size();
        System.out.println("adi7: "+adi7_r);

        for(String data  :    list8)    {
            String[] spli = data.split("_");
            Double v  = klArray[Integer.parseInt(spli[0])-1][Integer.parseInt(spli[1])-1];
            sum8+=v;
        }
        adi8_r=sum8/list8.size();
        System.out.println("adi8: "+adi8_r);


        return new ArrayList<Double>(){{
            add(adi1_r);
            add(adi2_r);
            add(adi3_r);
            add(adi4_r);
            add(adi5_r);
            add(adi6_r);
            add(adi7_r);
            add(adi8_r);
        }};

    }


    private void getAdiIndex(){

        /**
         * 目的是什么：遍历题型吗？
         * 四个元素，遍历组合即可, 随机遍历顺序没影响 (0,1,0,1)  16 种方案
         * combineList 全局变量,保存另外几个属性的选取方案
         * combineNum  二进制选取
         */
        ArrayList<String> combineList = new ArrayList<>();
        int combineNum = 2;
        for(int x=0;x<combineNum; x++){
            for(int y=0;y<combineNum; y++) {
                for (int z = 0; z < combineNum; z++) {
                    for (int w = 0; w < combineNum; w++) {
                        for (int a = 0; a < combineNum; a++) {
                            for (int b = 0; b < combineNum; b++) {
                                for (int c = 0; c < combineNum; c++) {
                                    String value = x +","+ y +","+ z +","+ w+","+ a +","+ b +","+ c;
                                    combineList.add(value);
                                }
                            }
                        }
                    }
                }
            }
        }


        /**
         * 这个遍历的目的是什么？
         */
        for (int X =0;X<combineList.size();X++){
            //adi1 计算所需要考虑的 pattern
            Integer index11 = indexMap.get("(1," + combineList.get(X) + ")");
            Integer index12 = indexMap.get("(0," + combineList.get(X) + ")");
            Integer index13 = indexMap.get("(0," + combineList.get(X) + ")");
            Integer index14 = indexMap.get("(1," + combineList.get(X) + ")");

            adiIndexList1.add(""+index11+"_"+index12);
            adiIndexList1.add(""+index13+"_"+index14);

            //adi2 计算所需要考虑的 pattern        1,0,1,0
            Integer index21 = indexMap.get("(" +combineList.get(X).substring(0,1)+ ",1," +combineList.get(X).substring(2,13) + ")");
            Integer index22 = indexMap.get("(" +combineList.get(X).substring(0,1)+ ",0," +combineList.get(X).substring(2,13) + ")");
            Integer index23 = indexMap.get("(" +combineList.get(X).substring(0,1)+ ",0," +combineList.get(X).substring(2,13) + ")");
            Integer index24 = indexMap.get("(" +combineList.get(X).substring(0,1)+ ",1," +combineList.get(X).substring(2,13) + ")");

            adiIndexList2.add(""+index21+"_"+index22);
            adiIndexList2.add(""+index23+"_"+index24);


            //adi3 计算所需要考虑的 pattern
            Integer index31 = indexMap.get("("+combineList.get(X).substring(0,3)+",1,"+combineList.get(X).substring(4,13)+ ")");
            Integer index32 = indexMap.get("("+combineList.get(X).substring(0,3)+",0,"+combineList.get(X).substring(4,13)+ ")");
            Integer index33 = indexMap.get("("+combineList.get(X).substring(0,3)+",0,"+combineList.get(X).substring(4,13)+ ")");
            Integer index34 = indexMap.get("("+combineList.get(X).substring(0,3)+",1,"+combineList.get(X).substring(4,13)+ ")");

            adiIndexList3.add(""+index31+"_"+index32);
            adiIndexList3.add(""+index33+"_"+index34);


            //adi4 计算所需要考虑的 pattern
            Integer index41 = indexMap.get("("+combineList.get(X).substring(0,5)+",1,"+combineList.get(X).substring(6,13)+ ")");
            Integer index42 = indexMap.get("("+combineList.get(X).substring(0,5)+",0,"+combineList.get(X).substring(6,13)+ ")");
            Integer index43 = indexMap.get("("+combineList.get(X).substring(0,5)+",0,"+combineList.get(X).substring(6,13)+ ")");
            Integer index44 = indexMap.get("("+combineList.get(X).substring(0,5)+",1,"+combineList.get(X).substring(6,13)+ ")");

            adiIndexList4.add(""+index41+"_"+index42);
            adiIndexList4.add(""+index43+"_"+index44);

            //adi5 计算所需要考虑的 pattern
            Integer index51 = indexMap.get("("+combineList.get(X).substring(0,7)+",1,"+combineList.get(X).substring(8,13)+ ")");
            Integer index52 = indexMap.get("("+combineList.get(X).substring(0,7)+",0,"+combineList.get(X).substring(8,13)+ ")");
            Integer index53 = indexMap.get("("+combineList.get(X).substring(0,7)+",0,"+combineList.get(X).substring(8,13)+ ")");
            Integer index54 = indexMap.get("("+combineList.get(X).substring(0,7)+",1,"+combineList.get(X).substring(8,13)+ ")");

            adiIndexList5.add(""+index51+"_"+index52);
            adiIndexList5.add(""+index53+"_"+index54);

            //adi6 计算所需要考虑的 pattern
            Integer index61 = indexMap.get("("+combineList.get(X).substring(0,9)+",1,"+combineList.get(X).substring(10,13)+ ")");
            Integer index62 = indexMap.get("("+combineList.get(X).substring(0,9)+",0,"+combineList.get(X).substring(10,13)+ ")");
            Integer index63 = indexMap.get("("+combineList.get(X).substring(0,9)+",0,"+combineList.get(X).substring(10,13)+ ")");
            Integer index64 = indexMap.get("("+combineList.get(X).substring(0,9)+",1,"+combineList.get(X).substring(10,13)+ ")");

            adiIndexList6.add(""+index61+"_"+index62);
            adiIndexList6.add(""+index63+"_"+index64);

            //adi7 计算所需要考虑的 pattern
            Integer index71 = indexMap.get("("+combineList.get(X).substring(0,11)+",1,"+combineList.get(X).substring(12,13)+ ")");
            Integer index72 = indexMap.get("("+combineList.get(X).substring(0,11)+",0,"+combineList.get(X).substring(12,13)+ ")");
            Integer index73 = indexMap.get("("+combineList.get(X).substring(0,11)+",0,"+combineList.get(X).substring(12,13)+ ")");
            Integer index74 = indexMap.get("("+combineList.get(X).substring(0,11)+",1,"+combineList.get(X).substring(12,13)+ ")");

            adiIndexList7.add(""+index71+"_"+index72);
            adiIndexList7.add(""+index73+"_"+index74);



            //adi8 计算所需要考虑的 pattern
            Integer index81 = indexMap.get("("+combineList.get(X)+",1)");
            Integer index82 = indexMap.get("("+combineList.get(X)+",0)");
            Integer index83 = indexMap.get("("+combineList.get(X)+",0)");
            Integer index84 = indexMap.get("("+combineList.get(X)+",1)");

            adiIndexList8.add(""+index81+"_"+index82);
            adiIndexList8.add(""+index83+"_"+index84);

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
        System.out.println("adi6个数: "+adiIndexList6.size() +" 具体指标为:"+adiIndexList6);
        System.out.println("adi7个数: "+adiIndexList7.size() +" 具体指标为:"+adiIndexList7);
        System.out.println("adi8个数: "+adiIndexList8.size() +" 具体指标为:"+adiIndexList8);

    }

}



