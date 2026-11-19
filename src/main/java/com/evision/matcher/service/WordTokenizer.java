package com.evision.matcher.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Component
public class WordTokenizer {

    private static final int BUFFER_SIZE = 64 * 1024;

    public Set<String> extractWords(Path file) throws IOException {
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            return extractWords(reader);
        }
    }

    public Set<String> extractWords(Reader reader) throws IOException {
        Set<String> words = new HashSet<>();
        char[] buffer = new char[BUFFER_SIZE];
        StringBuilder current = new StringBuilder(32);
        boolean valid = true;

        int read;
        while ((read = reader.read(buffer)) != -1) {
            for (int i = 0; i < read; i++) {
                char c = buffer[i];
                if (Character.isWhitespace(c)) {
                    if (valid && current.length() > 0) {
                        words.add(current.toString());
                    }
                    current.setLength(0);
                    valid = true;
                } else if (valid) {
                    if (Character.isLetter(c)) {
                        current.append(Character.toLowerCase(c));
                    } else {
                        valid = false;
                    }
                }
            }
        }
        if (valid && current.length() > 0) {
            words.add(current.toString());
        }
        return words;
    }
}
