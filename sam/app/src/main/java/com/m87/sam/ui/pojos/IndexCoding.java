package com.m87.sam.ui.pojos;

import java.util.Random;

/**
 * Created by tim-azul on 7/9/17.
 */

public class IndexCoding {

    public static Matrix reduceMatrix(Matrix m) {
        int[] devices = new int[m.rowSize];
        int newMatrixSize = 0;

        for (int i = 0; i < m.rowSize; i++) {

            // If the device got message successfully, then delete row and column
            if (m.vals[i][i] == 1) {
                m.clearRow(i);
                m.clearColumn(i);
            } else {
                newMatrixSize++;
            }

        }

        Matrix newMatrix = new Matrix(newMatrixSize, newMatrixSize);

        if (newMatrixSize > 0) {
            int x = 0;
            int y = 0;
            boolean added = false;

            for (int i = 0; i < m.rowSize; i++) {

                y = 0;
                added = false;

                for (int j = 0; j < m.colSize; j++) {

                    if (m.vals[i][j] == -1) {
                        continue;
                    }

                    newMatrix.vals[x][y] = m.vals[i][j];

                    y++;

                    added = true;
                }

                if (added) {
                    x++;
                }
            }
        }

        return newMatrix;

    }

}
