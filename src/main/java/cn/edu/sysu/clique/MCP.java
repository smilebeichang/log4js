package cn.edu.sysu.clique;

import java.util.*;

/**
 * @Author : song bei chang
 * @create 2021/12/27 22:26
 *
 */
public class MCP {
    static int[] x; // 当前解

    static int n; // 图G的顶点数

    static int cn; // 当前顶点数

    static int bestn; // 当前最大顶点数

    static int[] bestx; // 当前最优解

    static boolean[][] matrix; // 图G的邻接矩阵

    /**
     * @param m
     *            是邻接矩阵
     * @param v
     *            out the best solution 最优解
     * @return the best result 最优值
     */
    public static int maxClique(boolean[][] m, int[] v) {
        matrix = m;    //邻接矩阵必然是正方形
        n = matrix.length;
        x = new int[n];   //当前解
        cn = 0;
        bestn = 0;
        bestx = v;   //刚开始V是什么都没有

        backtrack(0);
        return bestn;
    }

    private static void backtrack(int i) {

        //只有到达5才到这里。
        if (i == n) {
            // 到达叶结点
            for (int j = 0; j < n; j++) {
                bestx[j] = x[j];
            }
            bestn = cn;
        }

        // 检查顶点 i 与当前圈的连接
        boolean connected = true;
        for (int j = 0; j < i; j++) {
            if (x[j] == 1 && !matrix[i][j]) {
                // i 和 j 不相连
                connected = false;
                break;
            }
        }
        if (connected) {
            // 进入左子树
            x[i] = 1;
            cn++;   //当前进了几步
            backtrack(i + 1);
            cn--;
        }
        if (cn + n - i > bestn) {    //剪枝函数，如果比最优解还大，继续才有意义   n是可能的最优解有n个，加上cn表示已经找到解的个数，i表示已经走了的步数，才可能有最优解
            // 进入右子树
            x[i] = 0;
            backtrack(i + 1);
        }
    }

    private static void test() {
        boolean[][] matrix = {
                { false, true, false, true, true },
                { true, false, true, false, true },
                { false, true, false, false, true },
                { true, false, false, false, true },
                { true, true, true, true, false }
        };
        int[] v = new int[matrix.length];
        int bestn = maxClique(matrix, v);   //返回值计算最大圈个数
        System.out.println("最优解是: " + bestn);
        System.out.println(Arrays.toString(bestx));
    }

    public static void main(String[] args) {
        test();
    }
}



