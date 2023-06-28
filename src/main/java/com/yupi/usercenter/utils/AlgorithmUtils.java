package com.yupi.usercenter.utils;

import java.util.List;
import java.util.Objects;

/**
 * @Author:HWQ
 * @DateTime:2023/5/5 14:35
 * @Description: 标签列表打分类 分数越低代表,两组标签相似度越高
 **/
public class AlgorithmUtils {

    public static int compareTags(List<String> tag1, List<String> tag2){
        int n = tag1.size();
        int m = tag2.size();

        if(n * m == 0)
            return n + m;

        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i < n + 1; i++){
            d[i][0] = i;
        }

        for (int j = 0; j < m + 1; j++){
            d[0][j] = j;
        }

        for (int i = 1; i < n + 1; i++){
            for (int j = 1; j < m + 1; j++){
                int left = d[i - 1][j] + 1;
                int down = d[i][j - 1] + 1;
                int left_down = d[i - 1][j - 1];
                if (!Objects.equals(tag1.get(i - 1), tag2.get(j - 1)))
                    left_down += 1;
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }
}
