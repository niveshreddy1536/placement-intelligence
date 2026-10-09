package com.placementintelligence.backend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LocalTextSimilarityService {

    /*
     * Common English words that provide very little useful
     * information for technical job matching.
     */
    private static final Set<String> STOP_WORDS = Set.of(
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
            "he",
            "in",
            "is",
            "it",
            "its",
            "of",
            "on",
            "or",
            "that",
            "the",
            "their",
            "this",
            "to",
            "was",
            "were",
            "will",
            "with",
            "you",
            "your",
            "our",
            "we",
            "they",
            "them",
            "I",
            "me",
            "my",
            "but",
            "not",
            "can",
            "may",
            "should",
            "using",
            "used",
            "use"
    );

    /*
     * ---------------------------------------------------------
     * PUBLIC METHOD
     * ---------------------------------------------------------
     *
     * Calculates TF-IDF cosine similarity between two texts.
     *
     * Result:
     *
     * 0   = no meaningful similarity
     * 1   = very high similarity
     * ---------------------------------------------------------
     */

    public double calculateSimilarity(
            String text1,
            String text2) {

        if (isBlank(text1)
                || isBlank(text2)) {

            return 0;
        }

        List<String> documents =
                new ArrayList<>();

        documents.add(text1);
        documents.add(text2);

        return calculateSimilarity(
                text1,
                text2,
                documents
        );
    }

    /*
     * ---------------------------------------------------------
     * TF-IDF COSINE SIMILARITY
     * ---------------------------------------------------------
     *
     * The corpus parameter allows us to calculate IDF using
     * more documents than just the two compared documents.
     *
     * This will become useful when we compare:
     *
     * resume
     * projects
     * experience
     * education
     * job description
     * ---------------------------------------------------------
     */

    public double calculateSimilarity(
            String text1,
            String text2,
            List<String> corpus) {

        if (isBlank(text1)
                || isBlank(text2)) {

            return 0;
        }

        List<String> tokens1 =
                tokenize(text1);

        List<String> tokens2 =
                tokenize(text2);

        if (tokens1.isEmpty()
                || tokens2.isEmpty()) {

            return 0;
        }

        /*
         * Build vocabulary from the corpus and the
         * two documents being compared.
         */

        Set<String> vocabulary =
                new HashSet<>();

        vocabulary.addAll(tokens1);
        vocabulary.addAll(tokens2);

        if (corpus != null) {

            for (String document :
                    corpus) {

                if (document != null
                        && !document.isBlank()) {

                    vocabulary.addAll(
                            tokenize(document)
                    );
                }
            }
        }

        /*
         * Calculate TF-IDF vectors.
         */

        Map<String, Double> vector1 =
                buildTfIdfVector(
                        tokens1,
                        vocabulary,
                        corpus
                );

        Map<String, Double> vector2 =
                buildTfIdfVector(
                        tokens2,
                        vocabulary,
                        corpus
                );

        /*
         * Calculate cosine similarity.
         */

        double similarity =
                cosineSimilarity(
                        vector1,
                        vector2
                );

        /*
         * Keep result safely inside 0-1.
         */

        similarity =
                Math.max(
                        0,
                        Math.min(
                                similarity,
                                1
                        )
                );

        return round(similarity);
    }

    /*
     * ---------------------------------------------------------
     * TOKENIZATION
     * ---------------------------------------------------------
     */

    public List<String> tokenize(
            String text) {

        List<String> tokens =
                new ArrayList<>();

        if (text == null
                || text.isBlank()) {

            return tokens;
        }

        /*
         * Lowercase text.
         */

        String normalized =
                text.toLowerCase();

        /*
         * Keep useful technical characters.
         *
         * Examples:
         *
         * c++
         * c#
         * node.js
         * spring-boot
         *
         * Most punctuation is replaced with spaces.
         */

        normalized =
                normalized.replaceAll(
                        "[^a-z0-9+#.\\-]+",
                        " "
                );

        String[] words =
                normalized.split("\\s+");

        for (String word :
                words) {

            String cleaned =
                    cleanToken(word);

            if (cleaned.isBlank()) {
                continue;
            }

            if (cleaned.length() < 2) {
                continue;
            }

            if (STOP_WORDS.contains(cleaned)) {
                continue;
            }

            /*
             * Ignore pure numbers.
             */

            if (cleaned.matches("\\d+")) {
                continue;
            }

            tokens.add(cleaned);
        }

        return tokens;
    }

    /*
     * ---------------------------------------------------------
     * CLEAN TOKEN
     * ---------------------------------------------------------
     */

    private String cleanToken(
            String token) {

        if (token == null) {

            return "";
        }

        String cleaned =
                token.trim();

        /*
         * Remove punctuation from the beginning/end while
         * preserving meaningful technical symbols internally.
         */

        cleaned =
                cleaned.replaceAll(
                        "^[.\\-]+",
                        ""
                );

        cleaned =
                cleaned.replaceAll(
                        "[.\\-]+$",
                        ""
                );

        return cleaned;
    }

    /*
     * ---------------------------------------------------------
     * BUILD TF-IDF VECTOR
     * ---------------------------------------------------------
     */

    private Map<String, Double> buildTfIdfVector(
            List<String> tokens,
            Set<String> vocabulary,
            List<String> corpus) {

        Map<String, Integer> termFrequency =
                new HashMap<>();

        for (String token :
                tokens) {

            termFrequency.put(
                    token,
                    termFrequency.getOrDefault(
                            token,
                            0
                    ) + 1
            );
        }

        Map<String, Double> vector =
                new HashMap<>();

        int totalTerms =
                tokens.size();

        for (String term :
                vocabulary) {

            int frequency =
                    termFrequency.getOrDefault(
                            term,
                            0
                    );

            if (frequency == 0) {

                vector.put(
                        term,
                        0.0
                );

                continue;
            }

            /*
             * TF = term frequency / total terms
             */

            double tf =
                    (double) frequency
                            / totalTerms;

            /*
             * IDF
             */

            double idf =
                    calculateIdf(
                            term,
                            corpus
                    );

            /*
             * TF-IDF
             */

            double tfIdf =
                    tf * idf;

            vector.put(
                    term,
                    tfIdf
            );
        }

        return vector;
    }

    /*
     * ---------------------------------------------------------
     * IDF
     * ---------------------------------------------------------
     *
     * IDF = log((N + 1) / (DF + 1)) + 1
     *
     * The +1 smoothing prevents division-by-zero problems.
     * ---------------------------------------------------------
     */

    private double calculateIdf(
            String term,
            List<String> corpus) {

        /*
         * If no corpus was supplied, use a two-document
         * corpus containing the compared texts indirectly
         * through the default method.
         */

        if (corpus == null
                || corpus.isEmpty()) {

            return 1.0;
        }

        int totalDocuments =
                corpus.size();

        int documentsContainingTerm =
                0;

        for (String document :
                corpus) {

            if (document == null
                    || document.isBlank()) {

                continue;
            }

            List<String> tokens =
                    tokenize(document);

            if (tokens.contains(term)) {

                documentsContainingTerm++;
            }
        }

        return Math.log(
                ((double) totalDocuments + 1)
                        / (documentsContainingTerm + 1)
        ) + 1;
    }

    /*
     * ---------------------------------------------------------
     * COSINE SIMILARITY
     * ---------------------------------------------------------
     *
     * cosine(A,B) =
     *
     *       A · B
     * ----------------
     *    |A| × |B|
     * ---------------------------------------------------------
     */

    private double cosineSimilarity(
            Map<String, Double> vector1,
            Map<String, Double> vector2) {

        double dotProduct = 0;

        double magnitude1 = 0;

        double magnitude2 = 0;

        Set<String> vocabulary =
                new HashSet<>();

        vocabulary.addAll(
                vector1.keySet()
        );

        vocabulary.addAll(
                vector2.keySet()
        );

        for (String term :
                vocabulary) {

            double value1 =
                    vector1.getOrDefault(
                            term,
                            0.0
                    );

            double value2 =
                    vector2.getOrDefault(
                            term,
                            0.0
                    );

            dotProduct +=
                    value1 * value2;

            magnitude1 +=
                    value1 * value1;

            magnitude2 +=
                    value2 * value2;
        }

        if (magnitude1 == 0
                || magnitude2 == 0) {

            return 0;
        }

        return dotProduct
                / (
                Math.sqrt(magnitude1)
                        * Math.sqrt(magnitude2)
        );
    }

    /*
     * ---------------------------------------------------------
     * TEXT RELEVANCE SCORE
     * ---------------------------------------------------------
     *
     * Converts 0-1 similarity into a percentage.
     *
     * Example:
     *
     * 0.72 -> 72
     * ---------------------------------------------------------
     */

    public double calculatePercentage(
            String text1,
            String text2) {

        return round(
                calculateSimilarity(
                        text1,
                        text2
                ) * 100
        );
    }

    /*
     * ---------------------------------------------------------
     * COMBINED SECTION SIMILARITY
     * ---------------------------------------------------------
     *
     * This method will be especially useful later.
     *
     * Different resume sections can have different weights.
     *
     * Example:
     *
     * Projects    = 40%
     * Experience  = 40%
     * Summary     = 20%
     * ---------------------------------------------------------
     */

    public double calculateWeightedSimilarity(
            String jobText,
            String projects,
            String experience,
            String summary) {

        List<String> corpus =
                new ArrayList<>();

        corpus.add(
                safeText(jobText)
        );

        corpus.add(
                safeText(projects)
        );

        corpus.add(
                safeText(experience)
        );

        corpus.add(
                safeText(summary)
        );

        double projectScore =
                calculateSimilarity(
                        projects,
                        jobText,
                        corpus
                );

        double experienceScore =
                calculateSimilarity(
                        experience,
                        jobText,
                        corpus
                );

        double summaryScore =
                calculateSimilarity(
                        summary,
                        jobText,
                        corpus
                );

        /*
         * Projects are the strongest evidence for a student.
         */

        double weightedScore =
                (projectScore * 0.45)
                        + (experienceScore * 0.40)
                        + (summaryScore * 0.15);

        return round(
                weightedScore * 100
        );
    }

    /*
     * ---------------------------------------------------------
     * SAFE TEXT
     * ---------------------------------------------------------
     */

    private String safeText(
            String text) {

        if (text == null) {

            return "";
        }

        return text;
    }

    /*
     * ---------------------------------------------------------
     * BLANK CHECK
     * ---------------------------------------------------------
     */

    private boolean isBlank(
            String text) {

        return text == null
                || text.isBlank();
    }

    /*
     * ---------------------------------------------------------
     * ROUND
     * ---------------------------------------------------------
     */

    private double round(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}