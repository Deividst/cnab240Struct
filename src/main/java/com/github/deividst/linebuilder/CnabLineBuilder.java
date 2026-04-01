package com.github.deividst.linebuilder;

import com.github.deividst.enums.FieldType;
import com.github.deividst.exceptions.ContentFieldExceedsSizeLimitException;

import java.util.Arrays;

public class CnabLineBuilder {

    private final char[] line;

    public CnabLineBuilder() {
        this.line = new char[240];
        Arrays.fill(line, ' ');
    }

    /**
     * Método responsável por adicionar um campo na linha CNAB.
     *
     * @param fieldName nome lógico do campo (usado para mensagens de erro)
     * @param start posição inicial (base 1)
     * @param end posição final (base 1)
     * @param value valor do campo
     * @param fieldType tipo do campo, que define o comportamento de formatação:
     * <ul>
     *     <li>{@link FieldType#ALPHANUMERIC}
     *         <ul>
     *             <li>Alinhado à esquerda</li>
     *             <li>Preenchido com espaços à direita</li>
     *         </ul>
     *     </li>
     *     <li>{@link FieldType#NUMERIC}
     *         <ul>
     *             <li>Alinhado à direita</li>
     *             <li>Preenchido com zeros à esquerda</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    public CnabLineBuilder add(String fieldName, int start, int end, String value, FieldType fieldType) {
        int size = end - start + 1;

        if (value.length() > size) {
            throw new ContentFieldExceedsSizeLimitException("The content [" + value + "] of the field [" + fieldName + "] exceeds the size [" + size + "] limit.");
        }

        if (FieldType.ALPHANUMERIC == fieldType) {
            value.getChars(0, value.length(), line, start - 1);
            if (value.length() < size) {
                Arrays.fill(line, start - 1 + value.length(), start - 1 + size, ' ');
            }

        } else {
            if (value.length() < size) {
                int initialIndex = (size - value.length()) + start;
                value.getChars(0, value.length(), line, initialIndex -1);
                Arrays.fill(line, start - 1, initialIndex - 1, '0');
            } else  {
                value.getChars(0, value.length(), line, start - 1);
            }
        }

        return this;
    }

    public String build() {
        return new String(line);
    }
}
