package com.github.deividst.linebuilder;

public class CnabLineReader {

    /**
     * Método responsável por ler um campo da linha com base nas posições
     *
     * @param line linha com 240 posições do arquivo
     * @param start posição inicial (base 1)
     * @param end posição final (base 1)
     */
    public static String read(String line, int start, int end) {

        int size = end - start + 1;

        char[] buffer = new char[size];

        line.getChars(start - 1, end, buffer, 0);

        return new String(buffer);
    }

}
