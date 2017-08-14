package com.m87.sam.ui.pojos;

import android.util.Log;

import com.m87.sam.ui.util.Logger;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

/**
 * Created by tim-azul on 8/12/17.
 */

public class GreedyColoring {

    class SizeComparator implements Comparator<Set<?>> {

        @Override
        public int compare(Set<?> o1, Set<?> o2) {
            if (Integer.valueOf(o1.size()).compareTo(o2.size()) > 0) {
                return -1;
            }
            else if (Integer.valueOf(o1.size()).compareTo(o2.size()) < 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public Matrix greedyColoring(Matrix chReal) {
        Matrix A = new Matrix(chReal.rowSize, chReal.colSize);

        ArrayList<int[]> E = new ArrayList<int[]>();

        // Create adjacency matrix
        for (int i = 0; i < chReal.rowSize; i++) {

            for (int j = 0; j < chReal.colSize; j++) {
                if (chReal.vals[i][j] == 1 && chReal.vals[j][i] == 1) {
                    A.vals[i][j] = 1;
                }
            }

        }

        Logger.debug("A Matrix");
        A.show();

        ArrayList<Integer> fullIdxList = new ArrayList<Integer>();
        for (int i = 0; i < chReal.colSize; i++) {
            fullIdxList.add(i);
        }

        Logger.debug("fullIdxList");
        System.out.println(fullIdxList);

        while (A.rowSize > 0) {
            UndirectedSparseGraph<Integer, String> g = createGraphFromMatrix(A);

            // Run the greedy algo
            BronKerboschCliqueFinder b = new BronKerboschCliqueFinder(g);
            Collection<Set<Integer>> col = b.getAllMaximalCliques();
            ArrayList<Set<Integer>> res = new ArrayList<Set<Integer>>(col);

            Logger.debug("CLIQUES");

            Collections.sort(res, new SizeComparator());    // Sort cliques by size

            for (int i = 0; i < res.size(); i++) {
                Logger.debug(res.get(i).toString());
            }

            Set<Integer> maxClique = res.get(0);    // 1st element is max clique

            // Create clique array
            int[] cliqueArr = new int[chReal.colSize];
            for (int i = 0; i < cliqueArr.length; i++) {

                if (maxClique.contains(i)) {
                    cliqueArr[fullIdxList.get(i)] = 1;
                }
            }

            Logger.debug("Clique arr");
            Logger.debug(Arrays.toString(cliqueArr));

            // Downsize new fullIdxList
            ArrayList<Integer> tempFullIdxList = new ArrayList<Integer>();
            for (int i = 0; i < fullIdxList.size(); i++) {
                if (!maxClique.contains(i)){
                    tempFullIdxList.add(fullIdxList.get(i));
                }
            }
            fullIdxList = tempFullIdxList;

            Logger.debug("fullIdxList");
            System.out.println(fullIdxList);

            // [1, 4, 6]

            // Add to return list E
            E.add(cliqueArr);

            A = reduceAdjacencyMatrix(fullIdxList, A);

            Logger.debug("New A Matrix");
            A.show();
        }

        // Print E
        for (int i = 0; i < E.size(); i++) {
            System.out.println(Arrays.toString(E.get(i)));
        }

        // Create new matrix
        Matrix returnMatrix = new Matrix(E.size(), chReal.colSize);
        for (int i = 0; i < E.size(); i++) {

            for (int j = 0; j < E.get(i).length; j++) {
                returnMatrix.vals[i][j] = E.get(i)[j];
            }

        }

        returnMatrix.show();


        return returnMatrix;
    }

    public static UndirectedSparseGraph<Integer, String> createGraphFromMatrix(Matrix a) {

        UndirectedSparseGraph<Integer, String> g = new UndirectedSparseGraph<Integer, String>();

        for (int i = 0; i < a.rowSize; i++) {
            g.addVertex(i);
        }

        for (int i = 0; i < a.rowSize; i++) {

            for (int j = 0; j < a.colSize; j++) {
                if (a.vals[i][j] == 1) {
                    g.addEdge("Edge-"+i+"-"+j, i, j);
                }
            }

        }

        return g;
    }

    public static Matrix reduceAdjacencyMatrix(ArrayList<Integer> list, Matrix A) {
        Matrix newA = new Matrix(list.size(), list.size());
        int x = 0;
        int y = 0;

        for (int i = 0; i < A.rowSize; i++) {

            if (list.contains(i)) {
                y = 0;
                for (int j = 0; j < A.colSize; j++) {
                    if (list.contains(j)) {
                        newA.vals[x][y] = A.vals[i][j];
                        y++;
                    }
                }
                x++;
            }

        }


        return newA;
    }

}
