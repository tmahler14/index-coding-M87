/**
 * Base class for matrix
 *
 * @author - Tim Mahler
 */

package com.m87.sam.ui.pojos;

public class Matrix {
    public int rowSize;             // number of rows
    public int colSize;             // number of columns
    public double[][] vals;

    // Base Constructor
    public Matrix(int rowSize, int colSize) {
        this.vals = new double[rowSize][colSize];
        this.rowSize = rowSize;
        this.colSize = colSize;

        // Set default values to 0
        for (int i = 0; i < rowSize; i++) {
            for (int j = 0; j < colSize; j++) {
                vals[i][j] = 0;
            }
        }
    }

    /**
     * Sets the row,col in matrix to certain val
     *
     * @param row - int row
     * @param col - int col
     * @param val - int val
     */
    public void setVal(int row, int col, int val) {
        this.vals[row][col] = val;
    }


    // print matrix to standard output
    public void show() {
        for (int i = 0; i < rowSize; i++) {
            for (int j = 0; j < colSize; j++)
                System.out.printf("%9.2f ", this.vals[i][j]);
            System.out.println();
        }
    }

}

