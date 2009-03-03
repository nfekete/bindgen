package org.exigencecorp.bindgen.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

public class GenerationQueue {

    private final ProcessingEnvironment processingEnv;
    private final Properties properties = new Properties();
    private final Set<String> written = new HashSet<String>();
    private final List<TypeElement> queue = new ArrayList<TypeElement>();

    public GenerationQueue(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;

        // Default properties--this is ugly, but I could not get a bindgen.properties to be found on the classpath
        this.properties.put("fixRawType.javax.servlet.ServletConfig.initParameterNames", "String");
        this.properties.put("fixRawType.javax.servlet.ServletContext.attributeNames", "String");
        this.properties.put("fixRawType.javax.servlet.ServletContext.initParameterNames", "String");
        this.properties.put("fixRawType.javax.servlet.ServletRequest.attributeNames", "String");
        this.properties.put("fixRawType.javax.servlet.ServletRequest.parameterNames", "String");
        this.properties.put("fixRawType.javax.servlet.ServletRequest.locales", "Locale");
        this.properties.put("fixRawType.javax.servlet.ServletRequest.parameterMap", "String, String[]");
        this.properties.put("fixRawType.javax.servlet.http.HttpServletRequest.headerNames", "String");
        this.properties.put("fixRawType.javax.servlet.http.HttpSession.attributeNames", "String");
        this.properties.put("skipAttribute.javax.servlet.http.HttpSession.sessionContext", "true");
        this.properties.put("skipAttribute.javax.servlet.http.HttpServletRequest.requestedSessionIdFromUrl", "true");
        this.properties.put("skipAttribute.javax.servlet.ServletContext.servletNames", "true");
        this.properties.put("skipAttribute.javax.servlet.ServletContext.servlets", "true");

        // Now get the user-defined properties
        for (Map.Entry<String, String> entry : processingEnv.getOptions().entrySet()) {
            this.properties.put(entry.getKey(), entry.getValue());
        }
    }

    public void enqueueForcefully(TypeElement element) {
        this.queue.add(element);
    }

    public void enqueueIfNew(TypeElement element) {
        if (!this.shouldIgnore(element)) {
            this.queue.add(element);
        }
    }

    public void processQueue() {
        while (this.queue.size() != 0) {
            TypeElement element = this.queue.remove(0);
            new ClassGenerator(this, element).generate();
        }
    }

    public ProcessingEnvironment getProcessingEnv() {
        return this.processingEnv;
    }

    private boolean shouldIgnore(TypeElement element) {
        if (element.getQualifiedName().toString().endsWith("Binding")) {
            return true;
        }

        // We recursively walk into bindings, so first check our in-memory set of what we've
        // seen so far this round. Eclipse resets our processor every time, so this only has
        // things from this round.
        if (this.written.contains(element.toString())) {
            return true;
        }

        // If we haven't seen it this round, see if we've already output awhile ago, in which
        // case we can skip it. If we really needed to do this one again, Eclipse would have
        // deleted our last output and this check wouldn't find anything.
        try {
            ClassName bindingClassName = new ClassName(new ClassName(element.asType()).getBindingType());
            FileObject fo = this.getProcessingEnv().getFiler().getResource(
                StandardLocation.SOURCE_OUTPUT,
                bindingClassName.getPackageName(),
                bindingClassName.getSimpleName() + ".java");
            if (fo.getLastModified() > 0) {
                return true; // exists already
            }
        } catch (IOException io) {
        }

        // Store that we've now seen this element
        this.written.add(element.toString());

        return false;
    }

    public Properties getProperties() {
        return this.properties;
    }

}