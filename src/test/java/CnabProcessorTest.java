import com.github.deividst.processor.CnabProcessor;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.Compiler.javac;
import static com.google.testing.compile.CompilationSubject.assertThat;

class CnabProcessorTest {

    @Test
    void shouldGenerateMapper() {
        JavaFileObject sourceCnabField = getSourceCnabField();
        JavaFileObject sourceFieldType = getSourceFieldType();
        JavaFileObject sourceDemo = getSourceDemo();

        var compilation = javac()
                .withProcessors(new CnabProcessor())
                .compile(sourceFieldType, sourceCnabField, sourceDemo);

        assertThat(compilation).succeeded();

        assertThat(compilation).generatedSourceFile("com.github.deividst.ExampleCnabMapper");

        assertThat(compilation)
                .generatedSourceFile("com.github.deividst.ExampleCnabMapper")
                .containsElementsIn(
                        JavaFileObjects.forSourceString(
                                "com.github.deividst.ExampleCnabMapper",
                                """
                                package com.github.deividst;
                                
                                import com.github.deividst.enums.FieldType;
                                import com.github.deividst.linebuilder.CnabLineBuilder;
                                import com.github.deividst.linebuilder.CnabLineReader;
                                
                                public class ExampleCnabMapper {
                                
                                  public static String write(Example obj) {
                                    return new CnabLineBuilder()
                                      .add("bank",1, 3, obj.getBank(), FieldType.ALPHANUMERIC)
                                      .add("agency",4, 7, obj.getAgency(), FieldType.ALPHANUMERIC)
                                      .add("account",8, 19, obj.getAccount(), FieldType.NUMERIC)
                                      .build();
                                  }
                                
                                  public static Example read(String line) {
                                    Example obj = new Example();
                                
                                    obj.setBank(CnabLineReader.read(line, 0, 3));
                                    obj.setAgency(CnabLineReader.read(line, 3, 7));
                                    obj.setAccount(CnabLineReader.read(line, 7, 19));
                                
                                    return obj;
                                  }
                                }
                                """
                        )
                );
    }

    private JavaFileObject getSourceCnabField() {
        return JavaFileObjects.forSourceString(
                "com.github.deividst.annotations.CnabField",
                """
                        package com.github.deividst.annotations;
                        
                        import com.github.deividst.enums.FieldType;
                        
                        import java.lang.annotation.ElementType;
                        import java.lang.annotation.Retention;
                        import java.lang.annotation.RetentionPolicy;
                        import java.lang.annotation.Target;
                        
                        @Retention(RetentionPolicy.SOURCE)
                        @Target(ElementType.FIELD)
                        public @interface CnabField {
                        
                            int start();
                        
                            int end();
                        
                            FieldType type() default FieldType.ALPHANUMERIC;
                        }
                        """);
    }

    private JavaFileObject getSourceFieldType() {
        return JavaFileObjects.forSourceString(
                "com.github.deividst.enums.FieldType",
                """
                        package com.github.deividst.enums;
                        
                        public enum FieldType {
                            NUMERIC,
                            ALPHANUMERIC
                        }
                        """);
    }

    private JavaFileObject getSourceDemo() {
        return JavaFileObjects.forSourceString(
                "com.github.deividst.example.Example",
                """
                package com.github.deividst;
    
                import com.github.deividst.annotations.CnabField;
                import com.github.deividst.enums.FieldType;
    
                public class Example {
    
                   @CnabField(start = 1, end = 3, type = FieldType.ALPHANUMERIC)
                   private String bank;
                
                   @CnabField(start = 4, end = 7, type = FieldType.ALPHANUMERIC)
                   private String agency;
                
                   @CnabField(start = 8, end = 19, type = FieldType.NUMERIC)
                   private String account;
                
                   public String getBank() {
                       return bank;
                   }
                
                   public String getAgency() {
                       return agency;
                   }
                
                   public String getAccount() {
                       return account;
                   }
                
                   public void setBank(String bank) {
                       this.bank = bank;
                   }
                
                   public void setAgency(String agency) {
                       this.agency = agency;
                   }
                
                   public void setAccount(String account) {
                       this.account = account;
                   }
                }
                """
        );
    }
}