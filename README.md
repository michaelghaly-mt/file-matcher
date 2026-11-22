# File Matcher

A Spring Boot application that compares a reference text file (**file A**) with a pool of other text files and gives each pool file a **similarity score from 0% to 100%** based on the words they contain. The file with the highest score is the best match.

## Requirements

- Java 17 or newer
- Maven 3.8+

## Configuration

Settings are read from `src/main/resources/application.properties`:

| Property | Meaning | Default |
|---|---|---|
| `matcher.fileAPath` | Path of file A | required |
| `matcher.filesDirectory` | Directory containing the pool files | required |
| `matcher.fileExtensions` | Comma-separated extensions of the files to process | `txt` |
| `matcher.threads` | Number of worker threads | number of CPU cores |

Example:

```properties
matcher.fileAPath=sample/fileA.txt
matcher.filesDirectory=sample/pool
matcher.fileExtensions=txt,text,md,log,csv
```

Paths can be relative (to the folder the app is started from) or absolute. On Windows use forward slashes, for example `C:/data/A.txt`.

## Run

```bash
mvn test                  # run the unit tests
mvn spring-boot:run       # run with the sample data in /sample
```

Use your own files without editing the properties file:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--matcher.fileAPath=/data/A.txt --matcher.filesDirectory=/data/pool"
```

## Example output

Using the files in `sample/`:

```
file_testD.txt                   100.00%   <-- best match
file_testE.txt                    75.00%
file_testA.txt                    60.00%
file_testB.txt                    37.50%
file_testC.txt                     0.00%
```

## How it works

1. **Tokenize**: each file is converted into the *set of distinct words* it contains.
2. **Score**: the word sets of file A and a pool file are compared with **Jaccard similarity**:

   `score = (words in both files) / (words in either file) x 100`

3. **Rank**: files are sorted by score, best first (ties are sorted by file name).

### Rules applied to words

- A word is a whitespace-separated chunk made **only of letters**. `fox2`, `it's` and `dog.` are not words.
- Comparison is **case-insensitive**: `The` and `the` are the same word.
- **Order and repetition are ignored**, so "The quick fox" and "fox quick The" are a perfect match.

### Which files are processed

Only files whose extension is listed in `matcher.fileExtensions` are processed. Other files in the pool directory (and sub-directories) are ignored, and file A must have one of the listed extensions too. Only plain-text formats are supported: Word, PDF, Excel and other binary formats would produce meaningless scores.

### Why Jaccard

- Same words in any order: 100%.
- No shared words: 0%.
- Any extra or missing word lowers the score, because it increases the total number of distinct words.

"Percentage of A's words found in the other file" was rejected because a file with many extra words would still score 100%. Word frequency is not taken into account, since the task only concerns which words exist.

## Performance

- Files are **streamed** through a fixed-size buffer. A whole file is never loaded into memory, and no regex or `split()` is used.
- Memory depends on the number of **distinct** words, not on the total number of words.
- File A is read once and shared (read-only) between threads.
- Pool files are processed **in parallel** with a fixed thread pool of `min(configured or CPU-core threads, number of files)` workers.

## Error handling

The app stops with a clear message when:

- file A does not exist or has an unsupported extension,
- the pool directory does not exist,
- the pool directory contains no files with a configured extension.

Invalid UTF-8 bytes are replaced and do not stop the run.

## Project structure

```
src/main/java/com/evision/matcher
├── FileMatcherApplication.java      starts Spring, runs the matching, prints the results
├── model/MatchResult.java           file name + score (record)
└── service
    ├── WordTokenizer.java           file -> Set<String> of words
    ├── SimilarityCalculator.java    Jaccard score
    └── MatchingService.java         validates input, scores files in parallel, sorts
src/test/java/...                    unit tests for the three services
sample/                              example file A and pool
```

## Tech stack

Java 17, Spring Boot 3, Maven, Lombok (logging), JUnit 5 and AssertJ.
