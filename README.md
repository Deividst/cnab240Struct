# 📄 CNAB Processor

Biblioteca Java para geração e leitura de arquivos **CNAB240** utilizando **Annotation Processing** em tempo de compilação.

O objetivo do projeto é simplificar o mapeamento entre objetos Java e layouts CNAB, eliminando código manual e reduzindo erros.

---

## 🚀 Funcionalidades

* ✔ Geração automática de linhas CNAB (write)
* ✔ Leitura de linhas CNAB para objetos (read)
* ✔ Mapeamento declarativo via anotação `@CnabField`
* ✔ Validação de conflitos de posição em tempo de compilação
* ✔ Suporte a campos numéricos e alfanuméricos
* ✔ Alta performance (uso de `char[]` internamente)

---

## 🧠 Como funciona

A biblioteca utiliza **Annotation Processing** para gerar código durante a compilação.

A partir de uma classe anotada, é gerado automaticamente um mapper com métodos:

```java
write(obj) → gera linha CNAB  
read(line) → converte linha para objeto
```

---

## ✍️ Exemplo de uso

### 1. Definindo a classe

```java
import com.github.deividst.annotations.CnabField;
import com.github.deividst.enums.FieldType;

public class Demo {

    @CnabField(start = 1, end = 3, type = FieldType.ALPHANUMERIC)
    private String banco;

    @CnabField(start = 4, end = 7, type = FieldType.ALPHANUMERIC)
    private String agencia;

    @CnabField(start = 8, end = 19, type = FieldType.NUMERIC)
    private String conta;

    // getters e setters
}
```

---

### 2. Código gerado automaticamente

```java
public class DemoCnabMapper {

    public static String write(Demo obj) {
        return new CnabLineBuilder()
            .add("banco", 1, 3, obj.getBanco(), FieldType.ALPHANUMERIC)
            .add("agencia", 4, 7, obj.getAgencia(), FieldType.ALPHANUMERIC)
            .add("conta", 8, 19, obj.getConta(), FieldType.NUMERIC)
            .build();
    }

    public static Demo read(String line) {
        Demo obj = new Demo();

        obj.setBanco(line.substring(0, 3));
        obj.setAgencia(line.substring(3, 7));
        obj.setConta(line.substring(7, 19));

        return obj;
    }
}
```

---

## ⚙️ Anotação `@CnabField`

Define a posição e o comportamento de cada campo no layout CNAB.

```java
@CnabField(
    start = 1,
    end = 10,
    type = FieldType.ALPHANUMERIC
)
```

### Parâmetros

* `start`: posição inicial (base 1)
* `end`: posição final (base 1)
* `type`: tipo do campo

---

## 🔢 Tipos de campo

### `ALPHANUMERIC`

* Alinhado à esquerda
* Preenchido com espaços ou caractere padrão

### `NUMERIC`

* Alinhado à direita
* Preenchido com zeros à esquerda

---

## 🛡️ Validações

A biblioteca valida automaticamente:

* ❌ Sobreposição de campos
* ❌ Tamanho excedido do conteúdo
* ❌ Configuração inválida de posições

Erros são detectados **em tempo de compilação**.

---

## 🏗️ Arquitetura

```text
CnabProcessor     → Geração de código (compile-time)
CnabLineBuilder   → Construção da linha (write)
CnabLineReader    → Leitura da linha (read)
Mapper gerado     → Orquestração
```

---

## 🧪 Testes

O projeto utiliza:

* JUnit 5
* Google Compile Testing (para testar annotation processor)

---

## 📦 Tecnologias

* Java 21+
* Maven
* Annotation Processing (JSR 269)

---

## 🤝 Contribuição

Contribuições são bem-vindas!

1. Fork o projeto
2. Crie uma branch (`feature/minha-feature`)
3. Commit suas alterações
4. Abra um Pull Request

---

---

## 👨‍💻 Autor

Desenvolvido por Deivid Santos Thomé 🚀
