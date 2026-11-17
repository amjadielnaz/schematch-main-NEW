package de.uni_marburg.schematch.matching;

import de.uni_marburg.schematch.data.Column;

import java.util.*;

public class ColumnMinHashLSHBlocking {

    /*
     * Recall-orientierte Konfiguration
     */
    private static final int NUM_HASHES = 60;

    private static final int NUM_BANDS = 20;

    private static final int ROWS_PER_BAND = 3;

    private static final long PRIME =
            2147483647L;

    private final int[] hashA;
    private final int[] hashB;

    public ColumnMinHashLSHBlocking() {

        Random random = new Random(42);

        hashA = new int[NUM_HASHES];
        hashB = new int[NUM_HASHES];

        for (int i = 0; i < NUM_HASHES; i++) {

            hashA[i] =
                    random.nextInt(Integer.MAX_VALUE - 1) + 1;

            hashB[i] =
                    random.nextInt(Integer.MAX_VALUE - 1) + 1;
        }
    }

    public Map<Integer, Set<Integer>> generateCandidates(
            List<Column> columns) {

        int size = columns.size();

        @SuppressWarnings("unchecked")
        Set<String>[] distinctValues =
                new Set[size];

        int[][] signatures =
                new int[size][];

        boolean[] validColumn =
                new boolean[size];

        /*
         * Vorverarbeitung:
         * Werte extrahieren und Signaturen erzeugen.
         */
        for (int i = 0; i < size; i++) {

            Column column =
                    columns.get(i);

            Set<String> values =
                    extractDistinctValues(column);

            distinctValues[i] = values;

            if (values.isEmpty()) {
                continue;
            }

            if (column.getDatatype() == null) {
                continue;
            }

            signatures[i] =
                    computeMinHashSignature(values);

            validColumn[i] = true;
        }

        Map<Integer, Set<Integer>> candidates =
                new HashMap<>();

        Map<Long, List<Integer>> buckets =
                new HashMap<>();

        /*
         * LSH
         */
        for (int current = 0;
                current < size;
                current++) {

            if (!validColumn[current]) {
                continue;
            }

            Column column =
                    columns.get(current);

            int[] signature =
                    signatures[current];

            for (int band = 0;
                    band < NUM_BANDS;
                    band++) {

                long bucketKey =
                        buildBucketKey(
                                band,
                                signature,
                                column);

                List<Integer> bucket =
                        buckets.get(bucketKey);

                if (bucket != null) {

                    for (int earlier : bucket) {

                        Column previous =
                                columns.get(earlier);

                        /*
                         * Keine Vergleiche innerhalb
                         * derselben Tabelle.
                         */
                        if (column.getTable()
                                == previous.getTable()) {
                            continue;
                        }

                        candidates
                                .computeIfAbsent(
                                        earlier,
                                        k -> new HashSet<>())
                                .add(current);
                    }
                }

                buckets
                        .computeIfAbsent(
                                bucketKey,
                                k -> new ArrayList<>())
                        .add(current);
            }
        }

        return candidates;
    }

    private int[] computeMinHashSignature(
            Set<String> values) {

        int[] signature =
                new int[NUM_HASHES];

        Arrays.fill(
                signature,
                Integer.MAX_VALUE);

        for (String value : values) {

            int valueHash =
                    value.hashCode()
                            & Integer.MAX_VALUE;

            for (int i = 0;
                    i < NUM_HASHES;
                    i++) {

                int hash =
                        universalHash(
                                valueHash,
                                hashA[i],
                                hashB[i]);

                if (hash < signature[i]) {
                    signature[i] = hash;
                }
            }
        }

        return signature;
    }

    private Set<String> extractDistinctValues(
            Column column) {

        Set<String> result =
                new HashSet<>();

        List<String> values =
                column.getValues();

        if (values == null) {
            return result;
        }

        for (String value : values) {

            if (value == null
                    || value.isBlank()) {
                continue;
            }

            result.add(
                    value.strip()
                            .toLowerCase(
                                    Locale.ROOT));
        }

        return result;
    }

    private long buildBucketKey(
            int band,
            int[] signature,
            Column column) {

        int start =
                band * ROWS_PER_BAND;

        long hash =
                31L * band
                + column.getDatatype().ordinal();

        hash =
                31L * hash
                + signature[start];

        hash =
                31L * hash
                + signature[start + 1];

        return hash;
    }

    private int universalHash(
            int x,
            int a,
            int b) {

        long hash =
                ((long) a * x + b)
                        % PRIME;

        return (int) hash;
    }

    /**
     * MinHash-Schätzung der
     * Jaccard-Ähnlichkeit.
     */
    public double estimateSimilarity(
            Column first,
            Column second) {

        Set<String> values1 =
                extractDistinctValues(first);

        Set<String> values2 =
                extractDistinctValues(second);

        if (values1.isEmpty()
                || values2.isEmpty()) {
            return 0.0;
        }

        int[] sig1 =
                computeMinHashSignature(values1);

        int[] sig2 =
                computeMinHashSignature(values2);

        int equal = 0;

        for (int i = 0;
                i < NUM_HASHES;
                i++) {

            if (sig1[i] == sig2[i]) {
                equal++;
            }
        }

        return (double) equal
                / NUM_HASHES;
    }
}
