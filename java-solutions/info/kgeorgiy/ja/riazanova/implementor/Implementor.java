package info.kgeorgiy.ja.riazanova.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * Class implements {@link } interface.
 *
 * @author anbtrfl
 */

public class Implementor implements JarImpler {
    /**
     * A default constructor for the {@link Implementor} class.
     */
    public Implementor() {
        // Nothing to initialize
    }


    /**
     * Constant value of whitespace.
     */
    private final static String SPACE = " ";
    /**
     * Constant value of opening curly brace.
     */
    private final static String OPENING_CURLY_BRACE = "{";
    /**
     * Constant value of closing curly brace.
     */
    private final static String CLOSING_CURLY_BRACE = "}";
    /**
     * Constant value of opening brace.
     */
    private final static String OPENING_BRACE = "(";
    /**
     * Constant value of closing brace.
     */
    private final static String CLOSING_BRACE = ")";
    /**
     * Constant value of tab.
     */
    private final static String TAB = "\t";
    /**
     * Constant value of semicolon.
     */
    private final static String SEMICOLON = ";";
    /**
     * Constant value of string "return".
     */
    private final static String RETURN = "return";
    /**
     * Constant value of string "@Override".
     */
    private final static String OVERRIDE = "@Override";
    /**
     * Constant value of string "public".
     */
    private final static String PUBLIC = "public";
    /**
     * Constant value of string "null".
     */
    private final static String NULL = " null";
    /**
     * Constant value of string "false".
     */
    private final static String FALSE = " false";
    /**
     * Constant value of comma with whitespace.
     */
    private final static String COMMA_WITH_SPACE = ", ";
    /**
     * Constant value of zero.
     */
    private final static String ZERO = " 0";
    /**
     * Constant value for the empty string.
     */
    private final static String EMPTY_STRING = "";
    /**
     * Constant value of string "class".
     */
    private final static String CLASS = "class";
    /**
     * Constant value of string "implements".
     */
    private final static String IMPLEMENTS = "implements";
    /**
     * Constant value of string "package".
     */
    private final static String PACKAGE = "package";
    /**
     * Constant value of string "throws".
     */
    private final static String THROWS = "throws";
    /**
     * Constant value of string "Impl".
     */
    private final static String IMPL_SUFFIX = "Impl";
    /**
     * Constant value of java extension string.
     */
    private final static String JAVA_EXTENSION = ".java";
    /**
     * Constant value of class file extension string.
     */
    private final static String CLASS_EXTENSION = ".class";
    /**
     * Constant value for system dependent line separator.
     */
    private final static String SYSTEM_LINE_SEPARATOR = System.lineSeparator();
    /**
     * Constant value for the path separator inside a jar file.
     */
    private final static String JAR_FILE_PATH_SEPARATOR = "/";
    /**
     * Constant value for the path to the current directory.
     */
    private final static String CURRENT_DIRECTORY_PATH = ".";
    /**
     * A prefix for temporary directories.
     */
    private final static String TEMP_DIRECTORY_PREFIX = "temp";
    /**
     * Constant value for a key responsible for encoding.
     */
    private final static String ENCODING_KEY = "-encoding";
    /**
     * Constant value for a key responsible for classpath.
     */
    private final static String CLASSPATH_KEY = "-classpath";
    /**
     * Version of a generated manifest.
     */
    private final static String MANIFEST_VERSION = "1.0";
    /**
     * Vendor's name.
     */
    private final static String VENDOR_NAME = "anbtrfl";
    /**
     * Char separator for paths in the system.
     */
    private final static char FILE_PATH_SEPARATOR = File.separatorChar;
    /**
     * Char constant value for the path separator inside a jar file.
     */
    private final static char JAR_FILE_PATH_SEPARATOR_CHAR = '/';
    /**
     * Char constant value for the dot.
     */
    private final static char DOT_CHAR = '.';

    /**
     * Produces code implementing class or interface specified by provided {@code token}.
     * <p>
     * Generated class' name should be the same as the class name of the type token with {@code Impl} suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * {@code root} directory and have correct file name. For example, the implementation of the
     * interface {@link java.util.List} should go to {@code $root/java/util/ListImpl.java}
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException when implementation cannot be
     *                                                                 generated.
     */

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (root == null) {
            throw new IllegalArgumentException("Root path cannot be null");
        }

        checkIfTokenIsImplementable(token);

        String packageName = token.getPackageName();
        String[] packagePath = packageName.split("\\.");

        Path path = createDirectories(root, packagePath);

        String fileName = token.getSimpleName() + IMPL_SUFFIX + JAVA_EXTENSION;

        StringBuilder sb = new StringBuilder();

        writePackage(sb, token);
        writeClassName(sb, token);

        Arrays.stream(token.getMethods())
                .filter(this::check)
                .forEach(method -> {
                            writeOverride(sb);
                            writeBeginningOfMethod(sb, method);
                            writeMethodParameters(sb, method);
                            writeMethodThrows(sb, method);
                            writeReturn(sb, method);
                            endMethod(sb);
                        }
                );

        endClass(sb);

        Path outputPath = Paths.get(path.toString(), fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write(toUnicode(sb.toString()));
        } catch (IOException e) {
            throw new ImplerException("Cannot write in the output file", e);
        }
    }

    /**
     * Produces <var>.jar</var> file implementing class or interface specified by provided <var>token</var>.
     * <p>
     * Generated class' name should be the same as the class name of the type token with <var>Impl</var> suffix
     * added.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException when implementation cannot be generated.
     */

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (jarFile == null) {
            throw new IllegalArgumentException("jar file path cannot be null");
        }

        checkIfTokenIsImplementable(token);

        Path path;
        Path fileParent = jarFile.getParent();
        if (fileParent != null) {
            path = createDirectories(fileParent);
        } else {
            path = Paths.get(CURRENT_DIRECTORY_PATH);
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(path, TEMP_DIRECTORY_PREFIX);
        } catch (IOException e) {
            throw new ImplerException("Cannot create a temp directory", e);
        }

        implement(token, tempDir);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Required non-null compiler");
        }

        String tempDirString = tempDir.toString();
        String packageDirsPath = token.getPackageName().replace(DOT_CHAR, FILE_PATH_SEPARATOR);
        String fileNameWithoutExtension = token.getSimpleName() + IMPL_SUFFIX;
        String fullPathToJavaFile = Paths.get(tempDirString, packageDirsPath, fileNameWithoutExtension).toString();

        String[] args = new String[]{
                ENCODING_KEY, StandardCharsets.UTF_8.name(),
                CLASSPATH_KEY, getClassPath(token).toString(),
                fullPathToJavaFile + JAVA_EXTENSION
        };

        if (compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Problems with compiling generative file");
        }

        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION);
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, VENDOR_NAME);

        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            try {
                String packagePath = token.getPackageName().replace(DOT_CHAR, JAR_FILE_PATH_SEPARATOR_CHAR);
                String separator = token.getPackageName().isEmpty() ? EMPTY_STRING : JAR_FILE_PATH_SEPARATOR;
                String classFileName = token.getSimpleName() + IMPL_SUFFIX + CLASS_EXTENSION;
                String zipEntryName = packagePath + separator + classFileName;
                ZipEntry zipEntry = new ZipEntry(zipEntryName);

                jarOutputStream.putNextEntry(zipEntry);

                Files.copy(Path.of(fullPathToJavaFile + CLASS_EXTENSION), jarOutputStream);
            } catch (IOException e) {
                throw new ImplerException("Problems with writing to jar-class: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            throw new ImplerException("Problems with creating or closing jar file: " + e.getMessage(), e);
        } finally {
            clean(tempDir);
        }
    }

    /**
     * Returns classpath for the specified token.
     *
     * @param token is a class instance for which classpath of its location will be retrieved.
     * @return a path for location of the code source of the specified token.
     * @throws ImplerException if an error occurred while transforming classpath location into {@link URI}.
     */

    private static Path getClassPath(Class<?> token) throws ImplerException {
        try {
            CodeSource codeSource = token.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return Path.of(CURRENT_DIRECTORY_PATH);
            } else {
                URI uri = codeSource.getLocation().toURI();
                return Path.of(uri);
            }
        } catch (URISyntaxException e) {
            throw new ImplerException("Cannot retrieve the classpath", e);
        }
    }

    /**
     * Appends to the passed string builder lines responsible for package declaration.
     *
     * @param sb    is a {@link StringBuilder} instance to append to.
     * @param token is a {@link Class} instance to extract info from.
     */
    private void writePackage(StringBuilder sb, Class<?> token) {
        String packageName = token.getPackageName();

        if (!packageName.isEmpty()) {
            sb
                    .append(PACKAGE).append(SPACE).append(packageName).append(SEMICOLON)
                    .append(SYSTEM_LINE_SEPARATOR).append(SYSTEM_LINE_SEPARATOR);
        }
    }

    /**
     * Appends to the passed string builder lines responsible for class name declaration.
     *
     * @param sb    is a {@link StringBuilder} instance to append to.
     * @param token is a {@link Class} instance to extract info from.
     */
    private void writeClassName(StringBuilder sb, Class<?> token) {
        String className = token.getSimpleName() + IMPL_SUFFIX;
        String interfaceName = token.getCanonicalName();
        sb
                .append(PUBLIC).append(SPACE).append(CLASS).append(SPACE)
                .append(className).append(SPACE)
                .append(IMPLEMENTS).append(SPACE)
                .append(interfaceName).append(SPACE)
                .append(OPENING_CURLY_BRACE)
                .append(SYSTEM_LINE_SEPARATOR);
    }

    /**
     * Appends to the passed string builder lines responsible for return from a particular method.
     *
     * @param sb     is a {@link StringBuilder} instance to append to.
     * @param method is a {@link Method} instance return from.
     */
    private void writeReturn(StringBuilder sb, Method method) {
        Class<?> returnType = method.getReturnType();

        if (returnType == Void.TYPE) {
            return;
        }

        String returnValue;

        if (returnType.isPrimitive()) {
            returnValue = returnType == Boolean.TYPE ? FALSE : ZERO;
        } else {
            returnValue = NULL;
        }

        sb
                .append(TAB).append(TAB)
                .append(RETURN).append(returnValue).append(SEMICOLON)
                .append(SYSTEM_LINE_SEPARATOR);
    }

    /**
     * Appends to the passed string builder final lines of the method.
     *
     * @param sb is a {@link StringBuilder} instance to append to.
     */
    private void endMethod(StringBuilder sb) {
        sb.append(TAB).append(CLOSING_CURLY_BRACE).append(SYSTEM_LINE_SEPARATOR);
    }

    /**
     * Appends to the passed string builder override annotation.
     *
     * @param sb is a {@link StringBuilder} instance to append to.
     */
    private void writeOverride(StringBuilder sb) {
        sb
                .append(SYSTEM_LINE_SEPARATOR).append(TAB)
                .append(OVERRIDE)
                .append(SYSTEM_LINE_SEPARATOR);
    }

    /**
     * Appends to the passed string builder beginning lines of the method.
     *
     * @param sb     is a {@link StringBuilder} instance to append to.
     * @param method is a {@link Method} for which we are writing its beginning: access modifier, return type, name
     */
    private void writeBeginningOfMethod(StringBuilder sb, Method method) {
        String methodName = method.getName();
        String returnType = method.getReturnType().getCanonicalName();
        sb
                .append(TAB)
                .append(PUBLIC).append(SPACE).append(returnType)
                .append(SPACE).append(methodName);
    }

    /**
     * Appends to the passed string builder lines responsible for parameters declaration in the signature.
     *
     * @param sb     is a {@link StringBuilder} instance to append to.
     * @param method is a {@link Method} for which we are writing its parameters.
     */
    private void writeMethodParameters(StringBuilder sb, Method method) {
        Stream<String> parameterTypes = Arrays.stream(method.getParameterTypes()).map(Class::getCanonicalName);
        final int[] i = {0};
        sb
                .append(OPENING_BRACE)
                .append(parameterTypes.map(p -> p + " a" + i[0]++).collect(Collectors.joining(COMMA_WITH_SPACE)))
                .append(CLOSING_BRACE).append(SPACE);
    }

    /**
     * Appends to the passed string builder information about possible raised exceptions.
     *
     * @param sb     is a {@link StringBuilder} instance to append to.
     * @param method is a {@link Method} for which we are writing its throws.
     */
    private void writeMethodThrows(StringBuilder sb, Method method) {
        Class<?>[] exceptions = method.getExceptionTypes();
        int len = exceptions.length;
        Stream<String> exceptionTypes = Arrays.stream(method.getExceptionTypes()).map(Class::getCanonicalName);
        if (len != 0) {
            sb.append(THROWS).append(SPACE).append(exceptionTypes.collect(Collectors.joining(COMMA_WITH_SPACE)));
        }
        sb.append(OPENING_CURLY_BRACE).append(SYSTEM_LINE_SEPARATOR);
    }

    /**
     * Appends to the passed string builder closing curly brace.
     *
     * @param sb is a {@link StringBuilder} instance to append to.
     */
    private void endClass(StringBuilder sb) {
        sb.append(CLOSING_CURLY_BRACE);
    }

    /**
     * Checks whether is the passed token might be implemented or not.
     * If it is not, the {@link ImplerException} is thrown.
     *
     * @param token which is supposed to be implemented.
     * @throws ImplerException if the passed token is not implementable.
     */
    private void checkIfTokenIsImplementable(Class<?> token) throws ImplerException {
        if (token == null) {
            throw new ImplerException("token must not be null..");
        }

        if (!(token.isInterface())) {
            throw new ImplerException("token must be interface..");
        }

        int modifiers = token.getModifiers();

        if (Modifier.isPrivate(modifiers)) {
            throw new ImplerException("cannot implement private interfaces..");
        }
    }

    /**
     * Checks whether the passed method should be overridden or not.
     *
     * @param method which should be checked for need to override.
     * @return true if the method doesn't have default implementation, and it is not static.
     */
    private boolean check(Method method) {
        int modifiers = method.getModifiers();
        return !method.isDefault() && !Modifier.isStatic(modifiers);
    }

    /**
     * Deletes all the files from given directory recursively, including directory itself.
     *
     * @param tempDir is a directory to be cleaned up.
     * @throws ImplerException if an error occurred during the cleaning up.
     */
    private void clean(Path tempDir) throws ImplerException {
        File[] files = tempDir.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                clean(file.toPath());
            }
        }

        try {
            Files.delete(tempDir);
        } catch (IOException e) {
            throw new ImplerException("An error during clean up");
        }
    }


    /**
     * Creates directory along the path built from the root directory and following package path directories.
     *
     * @param root        is the first part of the path from which we start to create package directories.
     * @param packagePath is the array of the packages names
     * @return a path to the created directory.
     * @throws ImplerException if an error occurred while creating directories.
     */
    private static Path createDirectories(Path root, String[] packagePath) throws ImplerException {
        Path path;
        try {
            String rootPath = root.toString();
            Path dir = packagePath != null ? Paths.get(rootPath, packagePath) : Paths.get(rootPath);

            path = Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ImplerException("Cannot create directories on the path to the output file", e);
        }

        return path;
    }

    /**
     * Creates directory along the path built from the root directory.
     * It is the same as {@link Implementor#createDirectories(Path, String[])} but with an empty package path.
     *
     * @param root is the path to be created.
     * @return a path to the created directory.
     * @throws ImplerException if an error occurred while creating directories.
     */
    private static Path createDirectories(Path root) throws ImplerException {
        return createDirectories(root, null);
    }

    /**
     * Escapes symbolic that are out of ASCII range.
     *
     * @param input is the line containing symbols to be escaped.
     * @return the same line with symbols in unicode format.
     */
    private String toUnicode(String input) {
        StringBuilder sb = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (c >= 128) {
                sb.append(String.format("\\u%04X", (int) c));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * The entry point of the program.
     *
     * @param args are arguments of the command line.
     * @throws ImplerException if an error occurred during implementation of a particular token.
     */
    public static void main(String[] args) throws ImplerException {
        if (args == null) {
            System.err.println("Expected notnull entry arguments");
            return;
        }

        if (args.length != 2 && args.length != 3) {
            System.err.println("Expected either two or three arguments");
            return;
        }

        for (String arg : args) {
            if (arg == null) {
                System.err.println("Only non-null arguments are allowed");
                return;
            }
        }

        boolean isThreeArgs = args.length == 3;

        if (isThreeArgs && !"-jar".equals(args[0])) {
            System.err.println("Wrong usage, expected either two parameters or correct -jar key");
            return;
        }

        String className = isThreeArgs ? args[1] : args[0];
        String pathString = isThreeArgs ? args[2] : args[1];

        Class<?> token;
        try {
            token = Class.forName(className);
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot found class: " + e.getMessage());
            return;
        }

        Path path;
        try {
            path = Path.of(pathString);
        } catch (InvalidPathException e) {
            System.err.println("Incorrect path given: " + e.getMessage());
            return;
        }

        Implementor impler = new Implementor();

        if (isThreeArgs) {
            impler.implementJar(token, path);
            return;
        }

        impler.implement(token, path);
    }

}