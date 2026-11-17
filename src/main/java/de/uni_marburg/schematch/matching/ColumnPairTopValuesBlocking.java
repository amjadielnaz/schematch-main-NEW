package de.uni_marburg.schematch.matching;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_marburg.schematch.data.Column;

public class ColumnPairTopValuesBlocking {

    private final int topN;

    public ColumnPairTopValuesBlocking(int topN) {
        if (topN < 1) {
            throw new IllegalArgumentException("topN must be at least 1");
        }
        this.topN = topN;
    }

    public Map<Integer, Set<Integer>> generateCandidates(List<Column> columns) {
        Map<String, List<Integer>> columnsByValue = new HashMap<>();
        Map<Integer, Set<Integer>> candidates = new HashMap<>();

        for (int current = 0; current < columns.size(); current++) {
            Map<String, Integer> counts = new HashMap<>();
            for (String value : columns.get(current).getValues()) {
                if (value != null && !value.isBlank()) {
                    counts.merge(value.trim().toLowerCase(), 1, Integer::sum);
                }
            }

            List<String> topValues = counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(
                            Comparator.reverseOrder()).thenComparing(Map.Entry::getKey))
                    .limit(topN)
                    .map(Map.Entry::getKey)
                    .toList();

            for (String value : topValues) {
                List<Integer> earlierColumns =
                        columnsByValue.computeIfAbsent(value, key -> new ArrayList<>());

                for (int earlier : earlierColumns) {
                    candidates.computeIfAbsent(earlier, key -> new HashSet<>())
                            .add(current);
                }

                earlierColumns.add(current);
            }
        }

        return candidates;
    }
}