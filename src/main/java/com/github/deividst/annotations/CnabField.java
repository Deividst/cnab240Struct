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
