
package de.uni_marburg.schematch.matching;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.matchtask.columnpair.ColumnPair;

public class ColumnPairLabelMatcher {

   
    public double match(ColumnPair pair) {

        Column c1 = pair.getSourceColumn();
        Column c2 = pair.getTargetColumn();

        
        String label1 = c1.getLabel().toLowerCase(Locale.ROOT);
        String label2 = c2.getLabel().toLowerCase(Locale.ROOT);

        
        if (label1.equals(label2)) {
            return 1.0;
        }

        // 3-gram 생성
        Set<String> gram1 = getQgrams(label1, 3);
        Set<String> gram2 = getQgrams(label2, 3);

        // 교집합 계산
        Set<String> intersection = new HashSet<>(gram1);
        intersection.retainAll(gram2);

        // 합집합 계산
        Set<String> union = new HashSet<>(gram1);
        union.addAll(gram2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    // q-gram 생성
    private Set<String> getQgrams(String text, int q) {

        Set<String> grams = new HashSet<>();

        String padding = "#".repeat(q - 1);
        String paddedText = padding + text + padding;

        for (int i = 0; i <= paddedText.length() - q; i++) {
            String gram = paddedText.substring(i, i + q);

            grams.add(gram);
        }

        return grams;
    }

    
    public List<Double> matchAll(List<ColumnPair> pairs) {

        List<Double> result = new ArrayList<>();

        for (ColumnPair pair : pairs) {

            double sim = match(pair);

            result.add(sim);
        }

        return result;
    }
}
