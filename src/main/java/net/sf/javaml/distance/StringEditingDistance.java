package net.sf.javaml.distance;

import net.sf.javaml.core.Instance;

/**
 * Created by ranyu on 12/11/15.
 */
public class StringEditingDistance extends AbstractDistance  {
    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    public static int computeLevenshteinDistance(String lhs, String rhs) {
        int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];

        for (int i = 0; i <= lhs.length(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= rhs.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= lhs.length(); i++)
            for (int j = 1; j <= rhs.length(); j++)
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1));

        return distance[lhs.length()][rhs.length()];
    }

    public double measure(Instance lhs, Instance rhs) {
        int[][] distance = new int[lhs.noAttributes() + 1][rhs.noAttributes() + 1];

        for (int i = 0; i <= lhs.noAttributes(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= rhs.noAttributes(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= lhs.noAttributes(); i++)
            for (int j = 1; j <= rhs.noAttributes(); j++)
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((lhs.value(i - 1) == rhs.value(j - 1)) ? 0 : 1));

        return distance[lhs.noAttributes()][rhs.noAttributes()];

    }

}
