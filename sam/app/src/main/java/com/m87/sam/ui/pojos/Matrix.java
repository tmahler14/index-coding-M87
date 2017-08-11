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

    public Matrix(Matrix m) {
        this.vals = new double[m.rowSize][m.colSize];
        this.rowSize = m.rowSize;
        this.colSize = m.colSize;

        // Set default values to 0
        for (int i = 0; i < rowSize; i++) {
            for (int j = 0; j < colSize; j++) {
                vals[i][j] = m.vals[i][j];
            }
        }
    }

    public Matrix(double[][] vals) {
        this.vals = vals;
        this.rowSize = vals[0].length;
        this.colSize = vals.length;
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

    public void setRow(int row, int[] vals) {
        for (int i = 0; i < colSize; i++) {
            this.vals[row][i] = vals[i];
        }
    }


    // print matrix to standard output
    public void show() {
        for (int i = 0; i < rowSize; i++) {
            for (int j = 0; j < colSize; j++)
                System.out.printf("%9.2f ", this.vals[i][j]);
            System.out.println();
        }
    }

    public void clearRow(int row) {
        for (int i = 0; i < colSize; i++) {
            this.vals[row][i] = -1;
        }
    }

    public void clearColumn(int col) {
        for (int i = 0; i < rowSize; i++) {
            this.vals[i][col] = -1;
        }
    }

    public int[] getDiagonals() {
        int [] diag = new int[rowSize];

        for (int i = 0; i < rowSize; i++) {
            diag[i] = (int)this.vals[i][i];
        }

        return diag;
    }

    public static int[][] deleteColumn(int[][] args,int col)
    {
        int[][] nargs = new int[][]{};
        if(args != null && args.length > 0 && args[0].length > col)
        {
            nargs = new int[args.length][args[0].length-1];
            for(int i=0; i<args.length; i++)
            {
                int newColIdx = 0;
                for(int j=0; j<args[i].length; j++)
                {
                    if(j != col)
                    {
                        nargs[i][newColIdx] = args[i][j];
                        newColIdx++;
                    }
                }
            }
        }
        return nargs;
    }

    public static int[][] removeRow(int[][] array, int row){
        int rows = array.length;
        int[][] arrayToReturn = new int[rows-1][];
        for(int i = 0; i < row; i++)
            arrayToReturn[i] = array[i];
        for(int i = row; i < arrayToReturn.length; i++)
            arrayToReturn[i++] = array[i];
        return arrayToReturn;
    }

}

