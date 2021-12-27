package cn.edu.sysu.clique;

/**
 * @Author : song bei chang
 * @create 2021/12/27 0:00
 */

import java.util.Scanner;

public class BuLuoWeiDui {


    //默认定义数组大小
    public static int N = 100;
    //图用邻接矩阵表示
    static int[][] a = new int[N][N];
    //是否将第i个节点加入团中
    static int[] x = new int[N];
    //记录最优解
    static int[] bestx = new int[N];
    //记录最优值
    static int bestn;
    //当前已放入团中的节点数量
    static int cn;
    //n为图中节点数  m为图中边数
    static int n, m;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("请输入部落的人数n（节点数):");
        n = sc.nextInt();
        System.out.println("请输入人与人的友好关系(边数):");
        m = sc.nextInt();
        System.out.println("请依次输入有友好关系的两个人(有边相连的两个节点u,v）用空格分开:");
        int u, v;                      //有边相连的两个节点u,v
        for (int i = 1; i <= m; i++) {
            u = sc.nextInt();
            v = sc.nextInt();
            //边数为1
            a[u][v] = a[v][u] = 1;
        }


        //初始最优值为0
        bestn = 0;
        //初始的团中节点也为0
        cn = 0;
        //从第一个节点进行深度搜索
        backTrack(1);
        System.out.println("国王护卫队的最大人数为:" + bestn);
        System.out.println("国王护卫队的成员：");
        for (int i = 0; i <= n; i++) {
            //打印最优解中记录为1的节点标号
            if (bestx[i] == 1)
                System.out.print(i + " ");
        }
    }

    /*进行深度搜索*/
    private static void backTrack(int t) { //t：当前扩展节点在第t层
        //达到根节点  记录可行解 并记录此时节点数目
        if (t > n) {
            for (int i = 1; i <= n; i++)
                bestx[i] = x[i];
            bestn = cn;
            return;
        }

        //判断是否满足约束条件（边是否连通）-->左子树-->把节点加入团中
        if (place(t)) {
            //左子树 标记为1
            x[t] = 1;
            cn++;            //当前节点数+1

            //继续搜索t+1层
            backTrack(t + 1);
            cn--;            //回溯   加多少就减多少   回退
        }

        //满足限界条件  -->右子数
        if (cn + n - t > bestn) {
            x[t] = 0;
            backTrack(t + 1);
        }
    }

    private static boolean place(int t) {  //判断是否可以把节点t加入团中
        boolean ok = true;
        for (int j = 1; j < t; j++) {
            if (x[j] == 1 && a[t][j] == 1) {
                ok = false;
                break;
            }
        }
        return ok;
    }


}





