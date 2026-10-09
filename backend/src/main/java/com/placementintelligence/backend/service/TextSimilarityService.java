package com.placementintelligence.backend.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class TextSimilarityService {

    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("[a-zA-Z][a-zA-Z0-9+#.-]*");

    private static final Set<String> STOP_WORDS =
            Set.of(
                    "a",
                    "an",
                    "and",
                    "are",
                    "as",
                    "at",
                    "be",
                    "by",
                    "for",
                    "from",
                    "has",
                    "have",
                    "in",
                    "is",
                    "it",
                    "of",
                    "on",
                    "or",
                    "that",
                    "the",
                    "this",
                    "to",
                    "using",
                    "with",
                    "will",
                    "you",
                    "your",
                    "we",
                    "our",
                    "they",
                    "their",
                    "into",
                    "through",
                    "can",
                    "should",
                    "must",
                    "work",
                    "working",
                    "role",
                    "team"
            );

    /**
     * Calculates TF-IDF cosine similarity between two pieces of text.
     *
     * Returns a percentage from 0 to 100.
     */
    public double calculateSimilarity(
            String firstText,
            String secondText) {

        if (firstText == null ||
                secondText == null ||
                firstText.isBlank() ||
                secondText.isBlank()) {

            return 0.0;
        }

        Map<String, Double> firstVector =
                createTfIdfVector(
                        firstText,
                        firstText,
                        secondText
                );

        Map<String, Double> secondVector =
                createTfIdfVector(
                        secondText,
                        firstText,
                        secondText
                );

        double similarity =
                cosineSimilarity(
                        firstVector,
                        secondVector
                );

        return Math.min(
                100.0,
                Math.max(
                        0.0,
                        similarity * 100.0
                )
        );
    }

    /**
     * Creates a TF-IDF vector for one document.
     */
    private Map<String, Double> createTfIdfVector(
            String document,
            String firstDocument,
            String secondDocument) {

        List<String> documentTokens =
                tokenize(document);

        Map<String, Integer> termFrequency =
                new HashMap<>();

        for (String token : documentTokens) {

            termFrequency.merge(
                    token,
                    1,
                    Integer::sum
            );
        }

        Set<String> vocabulary =
                new HashSet<>();

        vocabulary.addAll(
                tokenize(firstDocument)
        );

        vocabulary.addAll(
                tokenize(secondDocument)
        );

        Map<String, Double> vector =
                new HashMap<>();

        for (String term : vocabulary) {

            int frequency =
                    termFrequency.getOrDefault(
                            term,
                            0
                    );

            if (frequency == 0) {
                vector.put(term, 0.0);
                continue;
            }

            double tf =
                    calculateTermFrequency(
                            frequency,
                            documentTokens.size()
                    );

            double idf =
                    calculateInverseDocumentFrequency(
                            term,
                            firstDocument,
                            secondDocument
                    );

            vector.put(
                    term,
                    tf * idf
            );
        }

        return vector;
    }

    /**
     * Term Frequency:
     *
     * TF = number of occurrences of term
     *      --------------------------------
     *      total number of terms
     */
    private double calculateTermFrequency(
            int frequency,
            int totalTerms) {

        if (totalTerms == 0) {
            return 0.0;
        }

        return (double) frequency
                / totalTerms;
    }

    /**
     * Inverse Document Frequency.
     *
     * Since our comparison contains two documents,
     * IDF rewards terms that are useful for distinguishing
     * the documents.
     */
    private double calculateInverseDocumentFrequency(
            String term,
            String firstDocument,
            String secondDocument) {

        int documentCount = 2;

        int documentsContainingTerm = 0;

        if (tokenize(firstDocument)
                .contains(term)) {

            documentsContainingTerm++;
        }

        if (tokenize(secondDocument)
                .contains(term)) {

            documentsContainingTerm++;
        }

        return Math.log(
                (double) documentCount
                        / (1 + documentsContainingTerm)
        ) + 1.0;
    }

    /**
     * Cosine similarity:
     *
     *             A · B
     * -----------------------------
     *       |A| × |B|
     */
    private double cosineSimilarity(
            Map<String, Double> firstVector,
            Map<String, Double> secondVector) {

        Set<String> vocabulary =
                new HashSet<>();

        vocabulary.addAll(
                firstVector.keySet()
        );

        vocabulary.addAll(
                secondVector.keySet()
        );

        double dotProduct = 0.0;
        double firstMagnitude = 0.0;
        double secondMagnitude = 0.0;

        for (String term : vocabulary) {

            double firstValue =
                    firstVector.getOrDefault(
                            term,
                            0.0
                    );

            double secondValue =
                    secondVector.getOrDefault(
                            term,
                            0.0
                    );

            dotProduct +=
                    firstValue * secondValue;

            firstMagnitude +=
                    firstValue * firstValue;

            secondMagnitude +=
                    secondValue * secondValue;
        }

        if (firstMagnitude == 0.0 ||
                secondMagnitude == 0.0) {

            return 0.0;
        }

        return dotProduct
                / (
                Math.sqrt(firstMagnitude)
                        * Math.sqrt(secondMagnitude)
        );
    }

    /**
     * Tokenizes text and removes common stop words.
     */
    private List<String> tokenize(
            String text) {

        if (text == null ||
                text.isBlank()) {

            return Collections.emptyList();
        }

        List<String> tokens =
                new ArrayList<>();

        var matcher =
                TOKEN_PATTERN.matcher(
                        text.toLowerCase()
                );

        while (matcher.find()) {

            String token =
                    matcher.group();

            if (token.length() <= 1) {
                continue;
            }

            if (STOP_WORDS.contains(token)) {
                continue;
            }

            tokens.add(
                    normalizeToken(token)
            );
        }

        return tokens;
    }

    /**
     * Basic normalization for common technical terminology.
     */
    private String normalizeToken(
            String token) {

        return switch (token) {

            case "springboot" ->
                    "spring";

            case "restful" ->
                    "rest";

            case "apis" ->
                    "api";

            case "databases" ->
                    "database";

            case "applications" ->
                    "application";

            case "developers" ->
                    "developer";

            case "developed",
                 "developing",
                 "development" ->
                    "develop";

            case "engineers",
                 "engineering" ->
                    "engineer";

            default ->
                    token;
        };
    }
}