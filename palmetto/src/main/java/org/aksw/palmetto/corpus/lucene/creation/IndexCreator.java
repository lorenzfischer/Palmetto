package org.aksw.palmetto.corpus.lucene.creation;

import edu.stanford.nlp.process.Morphology;
import org.aksw.palmetto.Palmetto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class creates a lucene index that can then be used by Palmetto to compute word co-occurrences. The main program
 * accepts n + 1 arguments, where the first argument is the output path where the index is to be created in, and
 * each following argument will be treated as a file containing one document per line.
 */
public class IndexCreator {

  private static Morphology stemmer = new Morphology();

  private static String[] stemWords(String[] words) {
    String[] defensiveCopy = Arrays.copyOf(words, words.length);

    for (int i=0; i<defensiveCopy.length; i++) {
      defensiveCopy[i] = stemmer.stem(defensiveCopy[i]);
    }

    return defensiveCopy;
  }


  private static String combineToLine(String[] words) {
    StringBuilder combiner = new StringBuilder();

    for (String w : words) {
      combiner.append(w).append(" ");
    }

    return combiner.toString().trim();
  }


  public static void main(String... args) throws IOException {

    if (args.length < 2) {
      System.out.println(
          "You need to provide at least two arguments (indexPath + inputFile1)\n" +
          "Example call:\n" +
          "IndexCreator /my/index/path /some/input/file1 /some/input/file2"
      );
      System.exit(1);
    }

    List<IndexableDocument> documents = new ArrayList<>();
    File indexDir = new File(args[0]);
    String[] inputPaths = Arrays.copyOfRange(args, 1, args.length);

    for (String inputFile : inputPaths) {

      System.out.println("Opening file " + inputFile);

      try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
        String line;
        while ((line = br.readLine()) != null) {
          System.out.print(".");

          // stem all words
          String[] words = stemWords(line.split("\\s"));

          // combine all stemmed words back into a sentence. todo: update IndexableDocument to support tokens
          String stemmedLine = combineToLine(words);

          IndexableDocument doc = new IndexableDocument(stemmedLine, words.length);
          documents.add(doc);
        }
      }
      System.out.println(" done");
    }

    // create an index and store it in indexDir
    PositionStoringLuceneIndexCreator creator = new PositionStoringLuceneIndexCreator(
      Palmetto.DEFAULT_TEXT_INDEX_FIELD_NAME,
      Palmetto.DEFAULT_DOCUMENT_LENGTH_INDEX_FIELD_NAME
    );
    creator.createIndex(indexDir, documents.iterator());

    // read the index, and create the histogram
    LuceneIndexHistogramCreator hCreator =
        new LuceneIndexHistogramCreator(Palmetto.DEFAULT_DOCUMENT_LENGTH_INDEX_FIELD_NAME);
    hCreator.createLuceneIndexHistogram(indexDir.getAbsolutePath());
  }

}
