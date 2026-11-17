package de.uni_marburg.schematch.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.uni_marburg.schematch.data.Column;

public class ColumnPairLabelBlocking {

    public Map<Integer, Set<Integer>> generateCandidates(List<Column> columns) {
        Map<String, List<Integer>> columnsByLabel = new HashMap<>();
        Map<Integer, Set<Integer>> candidates = new HashMap<>();

        for (int current = 0; current < columns.size(); current++) {
            String label = columns.get(current).getLabel();
            if (label == null || label.isBlank()) {
                continue;
            }

            String key = label.strip().toLowerCase(Locale.ROOT);
            List<Integer> earlierColumns =
                    columnsByLabel.computeIfAbsent(key, unused -> new ArrayList<>());

            for (int earlier : earlierColumns) {
                candidates.computeIfAbsent(earlier, unused -> new HashSet<>())
                        .add(current);
            }

            earlierColumns.add(current);
        }

        return candidates;
    }
}
