package com.evision.matcher;

import com.evision.matcher.service.WordTokenizer;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WordTokenizerTest {

    private final WordTokenizer tokenizer = new WordTokenizer();

    private Set<String> words(String text) throws Exception {
        return tokenizer.extractWords(new StringReader(text));
    }

    @Test
    void lowercasesAndIgnoresOrderAndRepeats() throws Exception {
        assertThat(words("The the THE quick")).containsExactlyInAnyOrder("the", "quick");
    }

    @Test
    void ignoresChunksWithNonLetters() throws Exception {
        assertThat(words("fox fox2 dog. it's 123 ok")).containsExactlyInAnyOrder("fox", "ok");
    }

    @Test
    void handlesNewlinesTabsAndNoTrailingWhitespace() throws Exception {
        assertThat(words("a\nb\tc  d")).containsExactlyInAnyOrder("a", "b", "c", "d");
    }

    @Test
    void emptyTextGivesNoWords() throws Exception {
        assertThat(words("")).isEmpty();
        assertThat(words("   \n ")).isEmpty();
    }
}
