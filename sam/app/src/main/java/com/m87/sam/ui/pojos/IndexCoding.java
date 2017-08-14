package com.m87.sam.ui.pojos;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by tim-azul on 7/9/17.
 */

public class IndexCoding {

    public static int[] getRetransmissionDevices(int[] diagonals) {
        ArrayList<Integer> x = new ArrayList<Integer>();

        for (int i = 0; i < diagonals.length; i++) {
            if (diagonals[i] == 0) {
                x.add(i);
            }
        }

        int [] devices = new int[x.size()];

        for (int i = 0; i < x.size(); i++) {
            devices[i] = x.get(i);
        }

        return devices;
    }

    public static int[] constructRetransmissionMessages(Matrix g, int[] initialMessages) {
        int[] messages = new int[g.rowSize];

        for (int i = 0; i < g.rowSize; i++) {

            int binaryNum = -1;

            for (int j = 0; j < g.colSize; j++) {
                if (g.vals[i][j] == 1) {
                    if (binaryNum == -1) {
                        binaryNum = initialMessages[j];
                    } else {
                        binaryNum = binaryNum ^ initialMessages[j];
                    }
                }
            }

            messages[i] = binaryNum;

        }

        return messages;

    };

    public static String[] constructRetransmissionMessagesHex(Matrix g, String[] initialHexMessages) {
        String[] messages = new String[g.rowSize];

        for (int i = 0; i < g.rowSize; i++) {

            String binaryNum = "";

            for (int j = 0; j < g.colSize; j++) {
                if (g.vals[i][j] == 1) {
                    if (binaryNum == "") {
                        binaryNum = initialHexMessages[j];
                    } else {
                        binaryNum = xorHexString(binaryNum, initialHexMessages[j]);
                    }
                }
            }

            messages[i] = binaryNum;

        }

        return messages;

    };

    public static String xorHexString(String h1, String h2) {
        String s = "";

        for (int i = 0; i < h1.length(); i++) {
            String s1 = Character.toString(h1.charAt(i));
            String s2 = Character.toString(h2.charAt(i));

            int i1 = Integer.parseInt(s1, 16);
            int i2 = Integer.parseInt(s2, 16);

            int total = i1 ^ i2;


            String result = Integer.toString(total, 16);

            s += result;

        }

        return s;
    }

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
