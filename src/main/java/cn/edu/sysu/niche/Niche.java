package cn.edu.sysu.niche;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * @Author : songbeichang
 * @create 2021/3/2 0:34
 */
public class Niche {

    /**
     *  算法 小生境
     *
     * （1）设置进化代数计数器；随机生成M个初始群体P（t），并求出各个个体的适应度F （i=1，2，M）。
     *
     * （2） 依据各个个体的适应度对其进行降序排列，记忆前N个个体（N<M）.
     *
     *  (3) 选择算法。对群体P（t）进行比例选择运算，得到P （t）。
     *
     * （4）交叉选择。对选择的个体集合P （t） 作单点交叉运算，得到P （t）。
     *
     * （5）变异运算。对P （t）作均匀变异运算，得到P （t）。
     *
     * （6）小生境淘汰运算。将第（5）步得到的M个个体和第（2）步所记忆的N个个体合并在一起，得到一个含有M+N 个个体的新群体；对着M+N个个体，按照下式得到两个个体x 和x 之间的海明距离：|| x - x ||= ( )当|| x - x ||<L时，比较个体x 和个体x 的适应度大小，并对其中适应度较低的个体处以罚函数： Fmin(x ，x )=Penalty
     *
     * （7）依据这M+N个个体的新适应度对各个个体进行降序排列，记忆前N个个体。
     *
     * （8）终止条件判断。若不满足终止条件，则：更新进化代数记忆器t t+1， 并将第（7）步排列中的前M个个体作为新的下一代群体P(t),然后转到第（3）步：若满足终止条件，则：输出计算结果，算法结束。

     */

    public static void main(String[] args) {
        init();
        spiltlist();
        decoding();
        cfitness();
        desOrderFitness();
    }

    private static int population_size  = 10;
    private static int Glength = 24;              //Glength = 24
    private static int singlelength = 8;          //singlelength = 8

    static Integer[][] genetic_population =new Integer[10][24];
    static Integer[][] genetic_population_1 =new Integer[10][8];  //genetic_population = []
    static Integer[][] genetic_population_2 =new Integer[10][8];
    static Integer[][] genetic_population_3 =new Integer[10][8];


    static double[]  population_1 =new double[10];        //population_1 = []
    static double[]  population_2 =new double[10];
    static double[]  population_3 =new double[10];

    static  double[] fitness =new double[10];


    //二进制初始化
    public static void  init() {
        for (int i = 0; i < population_size; i++) {
            //初始化
            Integer[] population_i = new Integer[Glength];
            for (int j = 0; j < Glength; j++) {
                population_i[j] = Math.random() >= 0.5?0:1;
            }
            genetic_population[i] = population_i;
        }
    }

    //切割
    public  static void spiltlist(){
        for (int i = 0; i < genetic_population.length ; i++) {
            for (int j = 0; j < singlelength; j++) {
                genetic_population_1[i][j]=genetic_population[i][j];
            }
        }
        for (int i = 0; i < genetic_population.length ; i++) {
            for (int j = singlelength; j < singlelength*2; j++) {
                genetic_population_2[i][j-singlelength]=genetic_population[i][j];
            }
        }
        for (int i = 0; i < genetic_population.length ; i++) {
            for (int j = singlelength*2; j < Glength; j++) {
                genetic_population_3[i][j-(singlelength*2)]=genetic_population[i][j];
            }
        }
    }


    //计算Math.pow(a, b)
    public  static void decoding(){
        for (int i = 0; i < population_size; i++) {
            double value1 = 0;
            double value2 = 0;
            double value3 = 0;
            for (int j = 0; j < singlelength; j++) {
                value1 +=genetic_population_1[i][j]*Math.pow(2, (singlelength-1-j));
                value2 +=genetic_population_2[i][j]*Math.pow(2, (singlelength-1-j));
                value3 +=genetic_population_3[i][j]*Math.pow(2, (singlelength-1-j));
            }
            population_1[i] = value1 - 100;
            population_2[i] = value2 - 100;
            population_3[i] = value3 - 100;
        }
    }

    //计算适应度，并赋值给fitness[]
    public static void cfitness() {
        double funcion_value = 0;
        for (int i = 0; i < population_size; i++) {

            //f2
            double a = Math.abs(population_1[i]/10);double b = Math.abs(population_2[i]/10);double c = Math.abs(population_3[i]/10);
            funcion_value = 30 + 1000 - a - b - c - a*b*c;

            BigDecimal bd   =   new   BigDecimal(funcion_value);
            double   f1   =   bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            if (funcion_value>0){
                fitness[i]=f1;
            }else{
                fitness[i]=0;
            }
        }
    }

    //适应度对其进行降序排列
    public static void  desOrderFitness(){
        double[] scores = fitness;
        Arrays.sort(scores);
        System.out.println("scores.length: "+scores.length);
        double[] newScore = new double[scores.length];

        for (int i = 0; i <scores.length ; i++) {
            newScore[scores.length-i-1]=scores[i];
        }
        for (double i:newScore) {
            System.out.println(i);
        }
    }



}



