package ru.mjkey.storykee.runtime.java;

import ru.mjkey.storykee.runtime.context.ExecutionContext;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;

/**
 * Compiles and executes Java code sections embedded in Storykee scripts.
 * Uses javax.tools for dynamic compilation.
 */
public class JavaSectionCompiler {
    
    private static JavaSectionCompiler instance;
    private final JavaCompiler compiler;
    private final InMemoryClassLoader classLoader;
    private int classCounter = 0;
    
    private JavaSectionCompiler() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (this.compiler == null) {
            throw new IllegalStateException("Java compiler not available. Ensure you're running with a JDK, not just a JRE.");
        }
        this.classLoader = new InMemoryClassLoader();
    }
    
    public static JavaSectionCompiler getInstance() {
        if (instance == null) {
            instance = new JavaSectionCompiler();
        }
        return instance;
    }
    
    /**
     * Compiles a Java code section.
     * 
     * @param javaCode The Java code to compile
     * @param context The execution context
     * @return The compiled section
     * @throws CompilationException If compilation fails
     */
    public CompiledJavaSection compile(String javaCode, ExecutionContext context) throws CompilationException {
        // Generate unique class name
        String className = "StorykeeJavaSection_" + (classCounter++);
        
        // Wrap the code in a class with an execute method
        String fullCode = generateClassWrapper(className, javaCode);
        
        // Compile the code
        Class<?> compiledClass = compileCode(className, fullCode);
        
        return new CompiledJavaSection(compiledClass);
    }
    
    /**
     * Wraps user Java code in a class structure.
     */
    private String generateClassWrapper(String className, String javaCode) {
        return """
            package ru.mjkey.storykee.runtime.java.generated;
            
            import ru.mjkey.storykee.runtime.context.ExecutionContext;
            import ru.mjkey.storykee.runtime.java.StorykeeAPI;
            
            public class %s {
                public Object execute(ExecutionContext context) throws Exception {
                    StorykeeAPI api = new StorykeeAPI(context);
                    %s
                    return null;
                }
            }
            """.formatted(className, javaCode);
    }
    
    /**
     * Compiles Java source code and loads the class.
     */
    private Class<?> compileCode(String className, String sourceCode) throws CompilationException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, null);
        
        // Create in-memory file manager
        InMemoryFileManager fileManager = new InMemoryFileManager(standardFileManager, classLoader);
        
        // Create source file object
        JavaFileObject sourceFile = new InMemoryJavaFileObject(
            "ru.mjkey.storykee.runtime.java.generated." + className,
            sourceCode
        );
        
        // Set up compilation options
        List<String> options = new ArrayList<>();
        options.add("-classpath");
        options.add(System.getProperty("java.class.path"));
        
        // Compile
        JavaCompiler.CompilationTask task = compiler.getTask(
            null,
            fileManager,
            diagnostics,
            options,
            null,
            Collections.singletonList(sourceFile)
        );
        
        boolean success = task.call();
        
        try {
            fileManager.close();
        } catch (IOException e) {
            // Ignore close errors
        }
        
        if (!success) {
            List<CompilationException.CompilationError> errors = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                errors.add(new CompilationException.CompilationError(
                    (int) diagnostic.getLineNumber(),
                    diagnostic.getMessage(null)
                ));
            }
            throw new CompilationException("Java compilation failed", errors);
        }
        
        // Load the compiled class
        try {
            return classLoader.loadClass("ru.mjkey.storykee.runtime.java.generated." + className);
        } catch (ClassNotFoundException e) {
            throw new CompilationException("Failed to load compiled class: " + e.getMessage());
        }
    }
    
    /**
     * In-memory Java source file.
     */
    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String code;
        
        public InMemoryJavaFileObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }
        
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
    
    /**
     * In-memory class file for storing compiled bytecode.
     */
    private static class InMemoryClassFile extends SimpleJavaFileObject {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        public InMemoryClassFile(String className) {
            super(URI.create("mem:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }
        
        @Override
        public OutputStream openOutputStream() {
            return outputStream;
        }
        
        public byte[] getBytes() {
            return outputStream.toByteArray();
        }
    }
    
    /**
     * File manager that stores compiled classes in memory.
     */
    private static class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final InMemoryClassLoader classLoader;
        
        public InMemoryFileManager(StandardJavaFileManager fileManager, InMemoryClassLoader classLoader) {
            super(fileManager);
            this.classLoader = classLoader;
        }
        
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            InMemoryClassFile classFile = new InMemoryClassFile(className);
            classLoader.addClass(className, classFile);
            return classFile;
        }
    }
    
    /**
     * ClassLoader that loads classes from memory.
     */
    private static class InMemoryClassLoader extends ClassLoader {
        private final Map<String, InMemoryClassFile> classFiles = new HashMap<>();
        
        public void addClass(String className, InMemoryClassFile classFile) {
            classFiles.put(className, classFile);
        }
        
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            InMemoryClassFile classFile = classFiles.get(name);
            if (classFile == null) {
                return super.findClass(name);
            }
            byte[] bytes = classFile.getBytes();
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
