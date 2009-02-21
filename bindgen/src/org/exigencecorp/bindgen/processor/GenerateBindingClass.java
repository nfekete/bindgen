package org.exigencecorp.bindgen.processor;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.tools.JavaFileObject;
import javax.tools.Diagnostic.Kind;

import org.apache.commons.lang.StringUtils;
import org.exigencecorp.bindgen.Binding;
import org.exigencecorp.gen.GClass;
import org.exigencecorp.gen.GMethod;

public class GenerateBindingClass {

    private final BindingGenerator generator;
    private final TypeElement element;
    private GClass bindingClass;

    public GenerateBindingClass(BindingGenerator generator, TypeElement element) {
        this.generator = generator;
        this.element = element;
    }

    public void generate() {
        this.initializeBindingClass();
        this.addConstructors();
        this.addValueGetAndSet();
        this.addNameAndType();
        this.generateProperties();
        this.saveCode();
    }

    private void initializeBindingClass() {
        String className = Massage.packageName(this.element.getQualifiedName() + "Binding") + this.getTypeParametersOrEmpty();
        this.bindingClass = new GClass(className);
        this.bindingClass.implementsInterface(Binding.class.getName() + "<{}>", this.getNameWithTypeParameters());
    }

    private void addConstructors() {
        this.bindingClass.getConstructor();
        this.bindingClass.getConstructor(this.getNameWithTypeParameters() + " value").body.line("this.set(value);");
    }

    private void addValueGetAndSet() {
        this.bindingClass.getField("value").type(this.getNameWithTypeParameters());

        GMethod set = this.bindingClass.getMethod("set").argument(this.getNameWithTypeParameters(), "value");
        set.body.line("this.value = value;");

        GMethod get = this.bindingClass.getMethod("get").returnType(this.getNameWithTypeParameters());
        get.body.line("return this.value;");
    }

    private void addNameAndType() {
        GMethod name = this.bindingClass.getMethod("getName").returnType(String.class);
        name.body.line("return \"\";");

        GMethod type = this.bindingClass.getMethod("getType").returnType("Class<?>", this.element.getSimpleName());
        type.body.line("return {}.class;", this.element.getSimpleName());
    }

    private void generateProperties() {
        for (Element enclosed : this.getProcessingEnv().getElementUtils().getAllMembers(this.element)) {
            if (enclosed.getModifiers().contains(Modifier.PUBLIC) && !enclosed.getModifiers().contains(Modifier.STATIC)) {
                if (enclosed.getKind() == ElementKind.FIELD && this.isFieldProperty(enclosed)) {
                    new GenerateFieldProperty(this.generator, this.bindingClass, enclosed).generate();
                } else if (enclosed.getKind() == ElementKind.METHOD && this.isMethodProperty(enclosed)) {
                    new GenerateMethodProperty(this.generator, this.bindingClass, (ExecutableElement) enclosed).generate();
                } else if (enclosed.getKind() == ElementKind.METHOD && this.isMethodCallable(enclosed)) {
                    new GenerateMethodCallable(this.generator, this.bindingClass, (ExecutableElement) enclosed).generate();
                }
            }
        }
    }

    private boolean isFieldProperty(Element enclosed) {
        String fieldType = this.getProcessingEnv().getTypeUtils().erasure(enclosed.asType()).toString();
        return !fieldType.endsWith("Binding");
    }

    private boolean isMethodProperty(Element enclosed) {
        String methodName = enclosed.getSimpleName().toString();
        ExecutableType e = (ExecutableType) enclosed.asType();
        return methodName.startsWith("get")
            && e.getThrownTypes().size() == 0
            && e.getParameterTypes().size() == 0
            && !methodName.equals("getClass")
            && !e.getReturnType().toString().endsWith("Binding");
    }

    private boolean isMethodCallable(Element enclosed) {
        String methodName = enclosed.getSimpleName().toString();
        ExecutableType e = (ExecutableType) enclosed.asType();
        return e.getParameterTypes().size() == 0
            && e.getReturnType().getKind() == TypeKind.VOID
            && e.getThrownTypes().size() == 0
            && !methodName.equals("wait")
            && !methodName.equals("notify")
            && !methodName.equals("notifyAll");
    }

    private void saveCode() {
        try {
            JavaFileObject jfo = this.getProcessingEnv().getFiler().createSourceFile(
                Massage.stripGenerics(this.bindingClass.getFullClassName()),
                this.element);
            Writer w = jfo.openWriter();
            w.write(this.bindingClass.toCode());
            w.close();
        } catch (IOException io) {
            this.getProcessingEnv().getMessager().printMessage(Kind.ERROR, io.getMessage());
        }
    }

    private ProcessingEnvironment getProcessingEnv() {
        return this.generator.getProcessingEnv();
    }

    private String getNameWithTypeParameters() {
        return this.element.getQualifiedName() + this.getTypeParametersOrEmpty();
    }

    private String getTypeParametersOrEmpty() {
        String typeParameters = "";
        if (this.element.getTypeParameters().size() != 0) {
            typeParameters = "<" + StringUtils.join(this.element.getTypeParameters(), ", ") + ">";
        }
        return typeParameters;
    }

}