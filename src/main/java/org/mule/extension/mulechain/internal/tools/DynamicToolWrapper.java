package org.mule.extension.mulechain.internal.tools;

import dev.langchain4j.agent.tool.Tool;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
//Obsolete
public class DynamicToolWrapper implements Tool {

    private final String name;
    private final String description;

    public DynamicToolWrapper(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String[] value() {
        return new String[]{description};
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Tool.class;
    }

    public static Tool create(String name, String description) {
        return (Tool) Proxy.newProxyInstance(
                Tool.class.getClassLoader(),
                new Class[]{Tool.class},
                new DynamicToolInvocationHandler(name, description)
        );
    }

    private static class DynamicToolInvocationHandler implements InvocationHandler {

        private final String name;
        private final String description;

        public DynamicToolInvocationHandler(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "name":
                    return name;
                case "value":
                    return new String[]{description};
                case "annotationType":
                    return Tool.class;
                default:
                    throw new UnsupportedOperationException("Method not implemented: " + method.getName());
            }
        }
    }
}