package cn.edu.sysu.clique;

import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

/**
 * 最大团问题--回溯法
 * @Author : song bei chang
 * @create 2021/12/27 22:30
 */
public class MaxcliqueV2 {

    /* 当前解（x[i]=1表示i点在最大团中，=0表示不在团中） */
    public int[] x;

    //图G的顶点数
    public int n;

    //当前顶点数
    public int cn;

    //当前最大顶点数
    public int bestn;

    //当前最优解
    public int[] bestx;

    //图G的邻接矩阵,0：不连通；1：连通
    public int[][] a;

    //图G的最大团个数
    public int count;

    public void backtrack(int i){
        if(i>n){
            for(int j=1;j<=n;j++){
                bestx[j]=x[j];
                System.out.print(x[j]+" ");
            }
            System.out.println();
            bestn=cn;
            count++;
            return;
        }
        else{
            boolean ok=true;
            //检查顶点i是否与当前团全部连接
            for(int j=1;j<i;j++){
                if(x[j]==1&&a[i][j]==0){
                    ok=false;
                    break;
                }
            }

            //从顶点i到已选入的顶点集中每一个顶点都有边相连
            if(ok){
                //进入左子树
                x[i]=1;
                cn++;
                backtrack(i+1);
                x[i]=0;
                cn--;
            }

            //当前顶点数加上未遍历的课选择顶点>=当前最优顶点数目时才进入右子树;如果不需要找到所有的解，则不需要等于
            if(cn+n-i>=bestn){
                //进入右子树
                x[i]=0;
                backtrack(i+1);
            }
        }
    }

    public int maxclique(int nn,int[][] aa){
        //初始化
        n=nn;
        a=aa;
        x=new int[n+1];
        bestx=x;
        cn=0;
        bestn=0;
        count=0;
        backtrack(1);
        return bestn;
    }

    public static void main(String[] args) {

        //a的下标从1开始，-1的值无用
        //int[][] a={{-1,-1,-1,-1,-1,-1},{-1,0,1,0,1,1},{-1,1,0,1,0,1},{-1,0,1,0,0,1},{-1,1,0,0,0,1},{-1,1,1,1,1,0}};
        int[][] a={
                {-1,-1,-1,-1,-1,-1},
                {-1,0,1,0,1,1},
                {-1,1,0,1,0,1},
                {-1,0,1,0,0,1},
                {-1,1,0,0,0,1},
                {-1,1,1,1,1,0}
        };
        int n=5;

//        int[][] a={
//                {-1,-1,-1,-1,-1},
//                {-1,0,1,1,0},
//                {-1,1,0,1,0},
//                {-1,1,1,0,0},
//                {-1,0,0,1,0}
//        };
//
//        int n=4;

//        int[][] a={
//                {-1,-1,-1,-1,-1,-1,-1},
//                {-1,0,1,1,0,0,1},
//                {-1,1,0,1,0,1,1},
//                {-1,1,1,0,1,0,1},
//                {-1,0,0,1,0,0,1},
//                {-1,0,1,0,0,0,0},
//                {-1,1,1,1,1,0,0},
//        };
//        int n=6;



        MaxcliqueV2 m=new MaxcliqueV2();
        System.out.println("图G的最大团解向量为：");
        System.out.println("图G的最大团顶点数为："+m.maxclique(n, a));
        System.out.println("图G的最大团个为："+m.count);
    }



    @Test
    public  void test03() {
        FileReader file = null;
        try {
            file = new FileReader("F:\\song\\SYSU\\Log4j\\input\\dataV4.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("文件测试数据如下：");
        txtString(file);
    }

    public static void txtString(FileReader file){

        //读取文件
        BufferedReader br = new BufferedReader(file);
        try {
            //读取一行数据
            String line = br.readLine();
            int lines = line.split(",").length;

            System.out.println(lines);

            String []sp = null;
            String [][]c = new String[lines][lines];
            int [][]cc = new int[lines][lines];
            int count=0;
            //按行读取
            while((line=br.readLine())!=null) {
                //按空格进行分割
                sp = line.split("\t");
                for(int i=0;i<sp.length;i++){
                    c[count][i] = sp[i];
                }
                count++;
            }
            for(int i=0;i<lines;i++){
                for(int j=0;j<lines;j++){
                    cc[i][j] = Integer.parseInt(c[i][0].split(",")[j].trim());
                    System.out.print(cc[i][j]+",");
                }
                System.out.println();
            }

            MaxcliqueV2 m=new MaxcliqueV2();
            System.out.println("图G的最大团解向量为：");
            System.out.println("图G的最大团顶点数为："+m.maxclique(lines-1, cc));
            System.out.println("图G的最大团个为："+m.count);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Test
    public  void test04() throws IOException {

        int number=1;
        OutputStream os = new FileOutputStream("F:\\song\\SYSU\\Log4j\\input\\output.txt");
        PrintWriter pw=new PrintWriter(os);
        for(int i=0;i<10;i++) {
            String s=""+number;
            //每输入一个数据，自动换行，便于我们每一行每一行地进行读取
            pw.println(s);
            //pw.print(s+",");//不会自动换行，必要时可以自己添加分隔符
            number++;
        }
        pw.close();
        os.close();
    }



}

/*
输出：
图G的最大团解向量为：
1 1 0 0 1
1 0 0 1 1
0 1 1 0 1
图G的最大团顶点数为：3
图G的最大团个为：3
*/



