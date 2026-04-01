package com.github.deividst.processor;

import com.github.deividst.annotations.CnabField;
import com.github.deividst.enums.FieldType;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.*;

@SupportedAnnotationTypes("com.github.deividst.annotations.CnabField")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class CnabProcessor extends AbstractProcessor {

    /**
     * Método principal do annotation processor.
     * <p>
     * É invocado pelo compilador Java a cada rodada de processamento.
     * Responsável por:
     * <ul>
     *     <li>Coletar todos os campos anotados com {@link CnabField}</li>
     *     <li>Agrupar por classe</li>
     *     <li>Gerar um mapper CNAB para cada classe</li>
     * </ul>
     *
     * @param annotations conjunto de anotações encontradas
     * @param roundEnv ambiente da rodada de processamento
     * @return true indica que a annotation foi processada
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Map<TypeElement, List<VariableElement>> classFields = collectAnnotatedFields(roundEnv);

        for (Map.Entry<TypeElement, List<VariableElement>> entry : classFields.entrySet()) {
            generateMapper(entry.getKey(), entry.getValue());
        }

        return true;
    }

    /**
     * Coleta todos os campos anotados com {@link CnabField} e agrupa por classe.
     *
     * @param roundEnv ambiente da rodada de processamento
     * @return mapa contendo a classe como chave e a lista de campos anotados como valor
     */
    private Map<TypeElement, List<VariableElement>> collectAnnotatedFields(RoundEnvironment roundEnv) {

        Map<TypeElement, List<VariableElement>> classFields = new HashMap<>();

        for (Element element : roundEnv.getElementsAnnotatedWith(CnabField.class)) {

            if (element.getKind() != ElementKind.FIELD) {
                continue;
            }

            VariableElement field = (VariableElement) element;
            TypeElement classElement = (TypeElement) field.getEnclosingElement();

            classFields
                    .computeIfAbsent(classElement, k -> new ArrayList<>())
                    .add(field);
        }

        return classFields;
    }

    /**
     * Gera a classe mapper responsável por converter um objeto em uma linha CNAB.
     *
     * @param classElement classe original anotada
     * @param fields lista de campos anotados
     */
    private void generateMapper(TypeElement classElement, List<VariableElement> fields) {
        validateFieldConflicts(classElement, fields);

        try {
            String packageName = getPackageName(classElement);
            String className = classElement.getSimpleName().toString();
            String mapperName = className + "CnabMapper";

            JavaFileObject file = createSourceFile(packageName, mapperName);

            try (Writer writer = file.openWriter()) {

                writePackage(writer, packageName);
                writeImports(writer);
                writeClassStart(writer, mapperName);
                writeMethod(writer, className, fields);
                writeReadMethod(writer, className, fields);
                writeClassEnd(writer);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Escreve a declaração do pacote no arquivo gerado.
     *
     * @param writer writer do arquivo
     * @param packageName nome do pacote
     * @throws Exception erro de escrita
     */
    private void writePackage(Writer writer, String packageName) throws Exception {
        writer.write("package " + packageName + ";\n\n");
    }

    /**
     * Escreve os imports necessários para o mapper gerado.
     *
     * @param writer writer do arquivo
     * @throws Exception erro de escrita
     */
    private void writeImports(Writer writer) throws Exception {
        writer.write("import com.github.deividst.enums.FieldType;\n");
        writer.write("import com.github.deividst.linebuilder.CnabLineBuilder;\n\n");
        writer.write("import com.github.deividst.linebuilder.CnabLineReader;\n\n");
    }

    /**
     * Inicia a declaração da classe mapper.
     *
     * @param writer writer do arquivo
     * @param mapperName nome da classe gerada
     * @throws Exception erro de escrita
     */
    private void writeClassStart(Writer writer, String mapperName) throws Exception {
        writer.write("public class " + mapperName + " {\n\n");
    }

    /**
     * Finaliza a declaração da classe mapper.
     *
     * @param writer writer do arquivo
     * @throws Exception erro de escrita
     */
    private void writeClassEnd(Writer writer) throws Exception {
        writer.write("}\n");
    }

    /**
     * Escreve o método responsável por converter o objeto em linha CNAB.
     *
     * @param writer writer do arquivo
     * @param className nome da classe de entrada
     * @param fields campos anotados
     * @throws Exception erro de escrita
     */
    private void writeMethod(Writer writer, String className, List<VariableElement> fields) throws Exception {

        writer.write("  public static String write(" + className + " obj) {\n");
        writer.write("    return new CnabLineBuilder()\n");

        for (VariableElement field : fields) {
            writeFieldMapping(writer, field);
        }

        writer.write("      .build();\n");
        writer.write("  }\n\n");
    }

    /**
     * Escreve a linha de mapeamento de um campo específico.
     *
     * @param writer writer do arquivo
     * @param field campo anotado
     * @throws Exception erro de escrita
     */
    private void writeFieldMapping(Writer writer, VariableElement field) throws Exception {

        CnabField annotation = field.getAnnotation(CnabField.class);

        int start = annotation.start();
        int end = annotation.end();
        FieldType fieldType = annotation.type();

        String fieldName = field.getSimpleName().toString();
        String getter = buildGetter(fieldName);

        writer.write(
                "      .add(\"" + fieldName + "\", " +
                        start + ", " +
                        end + ", obj." + getter + "(), FieldType." + fieldType + ")\n"
        );
    }

    /**
     * Escreve no arquivo gerado o método responsável por realizar a leitura
     * de uma linha CNAB e convertê-la em uma instância da classe alvo.
     * <p>
     * O método gerado possui a seguinte responsabilidade:
     * <ul>
     *     <li>Instanciar o objeto de destino</li>
     *     <li>Extrair os valores da linha com base nas posições definidas em {@link CnabField}</li>
     *     <li>Delegar a leitura de cada campo para {@link #writeReadField(Writer, VariableElement)}</li>
     *     <li>Retornar o objeto preenchido</li>
     * </ul>
     *
     * <p>Exemplo de método gerado:</p>
     * <pre>
     * public static Demo read(String line) {
     *     Demo obj = new Demo();
     *     obj.setBanco(line.substring(0, 3));
     *     return obj;
     * }
     * </pre>
     *
     * @param writer writer utilizado para escrever o código fonte gerado
     * @param className nome da classe que será instanciada
     * @param fields lista de campos anotados com {@link CnabField}
     * @throws Exception caso ocorra erro durante a escrita do código
     */
    private void writeReadMethod(Writer writer, String className, List<VariableElement> fields) throws Exception {

        writer.write("  public static " + className + " read(String line) {\n");
        writer.write("    " + className + " obj = new " + className + "();\n\n");

        for (VariableElement field : fields) {
            writeReadField(writer, field);
        }

        writer.write("\n    return obj;\n");
        writer.write("  }\n\n");
    }

    /**
     * Escreve a lógica de leitura de um campo específico no método {@code read}.
     * <p>
     * Para cada campo anotado com {@link CnabField}, este método:
     * <ul>
     *     <li>Obtém as posições {@code start} e {@code end} da annotation</li>
     *     <li>Calcula os índices adequados para uso com {@link String#substring(int, int)}</li>
     *     <li>Gera a chamada ao setter correspondente no objeto</li>
     * </ul>
     *
     * <p>Importante:</p>
     * <ul>
     *     <li>O índice inicial é ajustado de base 1 (CNAB) para base 0 (Java)</li>
     *     <li>O índice final é mantido, pois o {@code substring} é exclusivo</li>
     * </ul>
     *
     * <p>Exemplo gerado:</p>
     * <pre>
     * obj.setBanco(line.substring(0, 3));
     * </pre>
     *
     * @param writer writer utilizado para escrever o código fonte gerado
     * @param field campo anotado com {@link CnabField}
     * @throws Exception caso ocorra erro durante a escrita do código
     */
    private void writeReadField(Writer writer, VariableElement field) throws Exception {

        CnabField annotation = field.getAnnotation(CnabField.class);

        int start = annotation.start();
        int end = annotation.end();

        String fieldName = field.getSimpleName().toString();
        String setter = buildSetter(fieldName);

        int beginIndex = start - 1;

        String valueExtraction = "CnabLineReader.read("+ "line, " + beginIndex + ", " + end + ")";

        writer.write(
                "    obj." + setter + "(" + valueExtraction + ");\n"
        );
    }

    /**
     * Constrói o nome do método setter a partir do nome do campo,
     * seguindo o padrão JavaBeans.
     * <p>
     * A transformação consiste em:
     * <ul>
     *     <li>Prefixar o nome do campo com {@code set}</li>
     *     <li>Capitalizar a primeira letra do nome do campo</li>
     * </ul>
     *
     * <p>Exemplo:</p>
     * <pre>
     * banco  -> setBanco
     * agencia -> setAgencia
     * </pre>
     *
     * @param fieldName nome do campo
     * @return nome do método setter correspondente
     */
    private String buildSetter(String fieldName) {
        return "set" +
                fieldName.substring(0, 1).toUpperCase() +
                fieldName.substring(1);
    }

    /**
     * Obtém o nome do pacote da classe.
     *
     * @param classElement elemento da classe
     * @return nome do pacote
     */
    private String getPackageName(TypeElement classElement) {
        return processingEnv.getElementUtils()
                .getPackageOf(classElement)
                .getQualifiedName()
                .toString();
    }

    /**
     * Cria o arquivo fonte Java para o mapper gerado.
     *
     * @param packageName nome do pacote
     * @param mapperName nome da classe gerada
     * @return arquivo Java
     * @throws Exception erro ao criar o arquivo
     */
    private JavaFileObject createSourceFile(String packageName, String mapperName) throws Exception {
        return processingEnv
                .getFiler()
                .createSourceFile(packageName + "." + mapperName);
    }

    /**
     * Constrói o nome do método getter a partir do nome do campo.
     *
     * @param fieldName nome do campo
     * @return nome do getter correspondente
     */
    private String buildGetter(String fieldName) {
        return "get" +
                fieldName.substring(0, 1).toUpperCase() +
                fieldName.substring(1);
    }

    /**
     * Valida conflitos de posição entre campos anotados com {@link CnabField}.
     * <p>
     * A validação garante que não existam sobreposições entre os intervalos
     * definidos pelos atributos {@code start} e {@code end} de cada campo.
     * <p>
     * O algoritmo funciona da seguinte forma:
     * <ul>
     *     <li>Ordena os campos pela posição inicial ({@code start})</li>
     *     <li>Compara cada campo com o próximo da lista</li>
     *     <li>Detecta conflito quando o próximo campo inicia antes ou na mesma
     *     posição em que o campo atual termina</li>
     * </ul>
     *
     * <p>Exemplo de conflito:</p>
     * <pre>
     * campoA: start=1, end=10
     * campoB: start=5, end=15  //sobreposição
     * </pre>
     *
     * <p>
     * Quando um conflito é encontrado, um erro de compilação é emitido através do
     * {@link javax.annotation.processing.Messager}, interrompendo a geração do código.
     * </p>
     *
     * @param classElement a classe que contém os campos anotados
     * @param fields lista de campos anotados com {@link CnabField}
     */
    private void validateFieldConflicts(TypeElement classElement, List<VariableElement> fields) {
        fields.sort(Comparator.comparingInt(f -> f.getAnnotation(CnabField.class).start()));

        for (int i = 0; i < fields.size() - 1; i++) {

            VariableElement current = fields.get(i);
            VariableElement next = fields.get(i + 1);

            CnabField currentField = current.getAnnotation(CnabField.class);
            CnabField nextField = next.getAnnotation(CnabField.class);

            int currentEnd = currentField.end();
            int nextStart = nextField.start();

            if (nextStart <= currentEnd) {

                String message = String.format(
                        "Conflito de posição CNAB na classe %s: campo '%s' (%d-%d) sobrepõe '%s' (%d-%d)",
                        classElement.getSimpleName(),
                        current.getSimpleName(), currentField.start(), currentField.end(),
                        next.getSimpleName(), nextField.start(), nextField.end()
                );

                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        message,
                        next
                );
            }
        }
    }
}