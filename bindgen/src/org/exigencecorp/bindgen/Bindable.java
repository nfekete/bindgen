package org.exigencecorp.bindgen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(value = { ElementType.PACKAGE, ElementType.TYPE })
public @interface Bindable {

}