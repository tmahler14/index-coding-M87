package com.m87.sam.ui.pojos;

import android.util.Log;

import com.m87.sam.ui.util.Logger;

import java.util.Arrays;

public class GaussianElimination {

    public static double[][] run(double[][] mat)
    {
        double[][] rref = new double[mat.length][mat[0].length];

    /* Copy matrix */
        for (int r = 0; r < rref.length; ++r)
        {
            for (int c = 0; c < rref[r].length; ++c)
            {
                rref[r][c] = mat[r][c];
            }
        }

        for (int p = 0; p < rref.length; ++p)
        {
        /* Make this pivot 1 */
            double pv = rref[p][p];
            if (pv != 0)
            {
                double pvInv = 1.0 / pv;
                for (int i = 0; i < rref[p].length; ++i)
                {
                    rref[p][i] *= pvInv;
                }
            }

        /* Make other rows zero */
            for (int r = 0; r < rref.length; ++r)
            {
                if (r != p)
                {
                    double f = rref[r][p];
                    for (int i = 0; i < rref[r].length; ++i)
                    {
                        rref[r][i] -= f * rref[p][i];
                    }
                }
            }
        }

        return rref;
    }

    public static void printMatrix(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            System.out.println(Arrays.toString(matrix[i]));
        }

        System.out.println("------------------");
    }

    public static Matrix createGaussianMatrix(Matrix roundMessageMatrix) {
        Matrix newMatrix = new Matrix(roundMessageMatrix.rowSize, roundMessageMatrix.colSize*2);

        Logger.debug("ASS");
        newMatrix.show();
        roundMessageMatrix.show();

        // Create matrix
        for (int i = 0; i < roundMessageMatrix.rowSize; i++) {

            for (int j = 0; j < roundMessageMatrix.colSize; j++) {
                Logger.debug("i="+i);
                Logger.debug("j="+j);

                newMatrix.vals[i][j] = roundMessageMatrix.vals[i][j];
            }

            if (roundMessageMatrix.colSize+i < newMatrix.colSize) {
                newMatrix.vals[i][roundMessageMatrix.colSize+i] = 1;
            }
        }

        return newMatrix;

    }

}
