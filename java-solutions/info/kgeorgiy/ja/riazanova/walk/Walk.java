package info.kgeorgiy.ja.riazanova.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class Walk {
    private static final String INVALID_HASH = "0".repeat(8);
    private static final int BUFFER_SIZE = 4096;

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("2 args needed : <входной файл> <выходной файл> ...");
            return;
        }

        Path inputFile;
        Path outputFile;

        String inputFileName = args[0];
        String outputFileName = args[1];

        if (inputFileName == null || outputFileName == null) {
            System.err.println("invalid arguments.. not null please");
            return;
        }

        try {
            inputFile = Path.of(inputFileName);
        } catch (InvalidPathException e) {
            System.err.println(
                    "An invalid path provided for input file: " + inputFileName + ". Message: " + e.getMessage()
            );
            return;
        }

        if (Files.isDirectory(inputFile)) {
            System.err.println("Directories are not allowed as an input file. Provided path: " + inputFileName);
            return;
        }

        try {
            outputFile = Path.of(outputFileName);
        } catch (InvalidPathException e) {
            System.err.println(
                    "An invalid path provided for output file: " + outputFileName + ". Message: " + e.getMessage()
            );
            return;
        }

        Path outputFileParent = outputFile.getParent();
        if (outputFileParent != null && Files.notExists(outputFileParent)) {
            try {
                Files.createDirectories(outputFileParent);
            } catch (IOException e) {
                System.err.println("Cannot create a directory for the output file: " + e.getMessage());
                return;
            }

        }

        try (BufferedReader in = Files.newBufferedReader(inputFile)) {
            try (BufferedWriter out = Files.newBufferedWriter(outputFile)) {
                String fileName;

                while ((fileName = in.readLine()) != null) {
                    out.write(countHash(fileName) + " " + fileName + System.lineSeparator());

                }
            } catch (IOException e) {
                System.err.println("problem with writing: " + System.lineSeparator() + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("problem with reading: " + System.lineSeparator() + e.getMessage());
        }
    }

    private static String countHash(String fileName) {
        try (FileInputStream in = new FileInputStream(fileName)) {
            byte[] bytes = new byte[BUFFER_SIZE];
            int cnt;
            int hash = 0;

            while ((cnt = in.read(bytes, 0, bytes.length)) >= 0) {

                for (int b = 0; b < cnt; b++) {
                    hash += bytes[b] & 0xff;
                    hash += hash << 10;
                    hash ^= hash >>> 6;

                }

            }
            hash += hash << 3;
            hash ^= hash >>> 11;
            hash += hash << 15;

            return String.format("%08x", hash);
        } catch (IOException | InvalidPathException | SecurityException e) {
            return INVALID_HASH;
        }
    }
}