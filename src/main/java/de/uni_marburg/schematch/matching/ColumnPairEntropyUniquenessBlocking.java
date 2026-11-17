package de.uni_marburg.schematch.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.uni_marburg.schematch.data.Column;

public class ColumnPairEntropyUniquenessBlocking {

    /*
     * Feste Parameter für das Blocking.
     *
     * Entropy:
     *  0.00 - 0.25 -> Bucket 0
     *  0.25 - 0.50 -> Bucket 1
     *  0.50 - 0.75 -> Bucket 2
     *  0.75 - 1.00 -> Bucket 3
     *
     * Uniqueness:
     *  0.00 - 0.20 -> Bucket 0
     *  0.20 - 0.40 -> Bucket 1
     *  0.40 - 0.60 -> Bucket 2
     *  0.60 - 0.80 -> Bucket 3
     *  0.80 - 1.00 -> Bucket 4
     */
    private static final int ENTROPY_BUCKETS = 4;
    private static final int UNIQUENESS_BUCKETS = 5;
    private static final int NEIGHBOR_RADIUS = 1;

    public Map<Integer, Set<Integer>> generateCandidates(
            List<Column> columns) {

        Map<BucketKey, List<Integer>> columnsByBucket =
                new HashMap<>();

        Map<Integer, Set<Integer>> candidates =
                new HashMap<>();

        for (int current = 0; current < columns.size(); current++) {

            Column column = columns.get(current);

            ColumnStatistics stats =
                    computeStatistics(column);

            /*
             * Leere Spalten ignorieren
             */
            if (stats.totalValues == 0) {
                continue;
            }

            int entropyBucket = Math.min(
                    ENTROPY_BUCKETS - 1,
                    (int) (stats.entropy * ENTROPY_BUCKETS));

            int uniquenessBucket = Math.min(
                    UNIQUENESS_BUCKETS - 1,
                    (int) (stats.uniqueness * UNIQUENESS_BUCKETS));

            /*
             * Volles Nachbarschaftsquadrat durchsuchen.
             *
             * Beispiel für Radius = 1:
             *
             * (e-1,u-1) (e-1,u) (e-1,u+1)
             * (e  ,u-1) (e  ,u) (e  ,u+1)
             * (e+1,u-1) (e+1,u) (e+1,u+1)
             */
            for (int de = -NEIGHBOR_RADIUS;
                    de <= NEIGHBOR_RADIUS;
                    de++) {

                for (int du = -NEIGHBOR_RADIUS;
                        du <= NEIGHBOR_RADIUS;
                        du++) {

                    int e = entropyBucket + de;
                    int u = uniquenessBucket + du;

                    if (e < 0
                            || e >= ENTROPY_BUCKETS
                            || u < 0
                            || u >= UNIQUENESS_BUCKETS) {
                        continue;
                    }

                    BucketKey key =
                            new BucketKey(e, u);

                    List<Integer> earlierColumns =
                            columnsByBucket.get(key);

                    if (earlierColumns == null) {
                        continue;
                    }

                    for (int earlier : earlierColumns) {

                        Column previous =
                                columns.get(earlier);

                        /*
                         * Nur Spalten aus unterschiedlichen
                         * Tabellen vergleichen.
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
            }

            /*
             * Aktuelle Spalte ihrem Haupt-Bucket zuordnen.
             */
            BucketKey ownBucket =
                    new BucketKey(
                            entropyBucket,
                            uniquenessBucket);

            columnsByBucket
                    .computeIfAbsent(
                            ownBucket,
                            k -> new ArrayList<>())
                    .add(current);
        }

        return candidates;
    }

    private ColumnStatistics computeStatistics(
            Column column) {

        Map<String, Integer> frequencies =
                new HashMap<>();

        int total = 0;

        if (column.getValues() == null) {
            return new ColumnStatistics(
                    0.0,
                    0.0,
                    0);
        }

        for (String value : column.getValues()) {

            if (value == null || value.isBlank()) {
                continue;
            }

            String normalized =
                    value.strip()
                            .toLowerCase(Locale.ROOT);

            frequencies.merge(
                    normalized,
                    1,
                    Integer::sum);

            total++;
        }

        if (total == 0) {
            return new ColumnStatistics(
                    0.0,
                    0.0,
                    0);
        }

        int distinct =
                frequencies.size();

        /*
         * Uniqueness Ratio:
         *
         * distinct / total
         */
        double uniqueness =
                (double) distinct / total;

        /*
         * Normalisierte Shannon-Entropie:
         *
         * H = -Σ p log(p)
         *
         * Hnorm = H / log(distinct)
         *
         * Ergebnis liegt immer im Bereich [0,1].
         */
        double entropy = 0.0;

        if (distinct > 1) {

            for (int count : frequencies.values()) {

                double probability =
                        (double) count / total;

                entropy -= probability
                        * Math.log(probability);
            }

            entropy /= Math.log(distinct);
        }

        return new ColumnStatistics(
                entropy,
                uniqueness,
                total);
    }

    private record ColumnStatistics(
            double entropy,
            double uniqueness,
            int totalValues) {
    }

    private record BucketKey(
            int entropyBucket,
            int uniquenessBucket) {
    }
}


