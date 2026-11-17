
package de.uni_marburg.schematch.matching;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.io.InputStream;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.matchtask.columnpair.ColumnPair;

public class ProductMatcherRunner3 {

    public static void main(String[] args) throws Exception {

        // ==========================================
        // 1. 파일 설정
        // ==========================================

        String folder = args.length > 1 ? args[1] : "examples/product-data";

        int maxFiles = 5;
        int maxRows = 200;

        double threshold = 0.5;


        // ==========================================
        // 2. Product 파일 선택
        // ==========================================

        List<Path> files;

        try (Stream<Path> stream = Files.list(Path.of(folder))) {

            files = stream
                    .filter(p -> p.toString().endsWith(".json")
                            || p.toString().endsWith(".json.gz"))
                    .sorted()
                    .limit(maxFiles)
                    .toList();
        }

        System.out.println("Number of files: " + files.size());


        // ==========================================
        // 3. Column 읽기
        // ==========================================

        ProductDataReader reader = new ProductDataReader();

        List<Column> allColumns = new ArrayList<>();

        for (Path file : files) {

            System.out.println(
                "Reading: " + file.getFileName()
            );

            List<Column> columns =
                reader.readColumns(file.toString(), maxRows);

            System.out.println(
                "Columns: " + columns.size()
            );

            allColumns.addAll(columns);
        }


        // ==========================================
        // 4. 전체 Column Pair 생성
        // ==========================================

        int n = allColumns.size();

        List<ColumnPair> allPairs = new ArrayList<>();

        // 각 Pair에 대응하는 Column 인덱스 저장
        List<Integer> firstIds = new ArrayList<>();
        List<Integer> secondIds = new ArrayList<>();

        for (int i = 0; i < n; i++) {

            for (int j = i + 1; j < n; j++) {

                Column c1 = allColumns.get(i);
                Column c2 = allColumns.get(j);

                allPairs.add(new ColumnPair(c1, c2));

                firstIds.add(i);
                secondIds.add(j);
            }
        }

        int totalPairs = allPairs.size();

        System.out.println("\n===== Before Blocking =====");

        System.out.println("Total columns: " + n);
        System.out.println("Total pairs: " + totalPairs);


        // ==========================================
        // 5. Pseudo Ground Truth 생성
        // ==========================================

        ColumnPairCombinedMatcher matcher =
            new ColumnPairCombinedMatcher();

        // 모든 Pair를 Combined Matcher에 전달
        List<Double> allScores = matcher.matchAll(allPairs);

        int groundTruthCount = 0;

        for (double score : allScores) {

            if (score >= threshold) {

                groundTruthCount++;
            }
        }

        System.out.println("\n===== Pseudo Ground Truth =====");

        System.out.println("Threshold: " + threshold);

        System.out.println(
            "Ground Truth Matches: " + groundTruthCount
        );


        // ==========================================
        // 6. Inverted Index Blocking
        // ==========================================

        String blockerName = args.length == 0 ? "label" : args[0];

        Map<Integer, Set<Integer>> candidates = switch (blockerName) {
            case "label" -> new ColumnPairLabelBlocking().generateCandidates(allColumns);
            case "values" -> new ColumnPairBlocking().generateCandidates(allColumns);
            case "top-values" -> new ColumnPairTopValuesBlocking(5).generateCandidates(allColumns);
            default -> throw new IllegalArgumentException("Unknown blocker: " + blockerName);
        };

        System.out.println("Blocker: " + blockerName);


        // ==========================================
        // 7. 후보를 ColumnPair List로 변환
        // ==========================================

        List<ColumnPair> blockedPairs = new ArrayList<>();

        for (int i : candidates.keySet()) {

            for (int j : candidates.get(i)) {

                Column c1 = allColumns.get(i);
                Column c2 = allColumns.get(j);

                blockedPairs.add(new ColumnPair(c1, c2));
            }
        }

        int candidateCount = blockedPairs.size();


        // ==========================================
        // 8. Blocking 후보 Matching
        // ==========================================

        List<Double> blockedScores =
            matcher.matchAll(blockedPairs);

        System.out.println("\n===== Matching Results =====");

        for (int k = 0; k < blockedPairs.size(); k++) {

            ColumnPair pair = blockedPairs.get(k);

            double score = blockedScores.get(k);

            System.out.println(
                pair.getSourceColumn().getLabel()
                + " <-> "
                + pair.getTargetColumn().getLabel()
                + " : "
                + score
            );
        }


        // ==========================================
        // 9. Blocking Evaluation
        // ==========================================

        int preservedMatches = 0;
        int lostMatches = 0;

        System.out.println("\n===== Lost Matches =====");

        for (int k = 0; k < allPairs.size(); k++) {

            double score = allScores.get(k);

            // Matcher가 정답이라고 판단한 Pair만 검사
            if (score >= threshold) {

                int i = firstIds.get(k);
                int j = secondIds.get(k);

                boolean found = false;

                if (candidates.containsKey(i)) {

                    if (candidates.get(i).contains(j)) {

                        found = true;
                    }
                }

                if (found) {

                    preservedMatches++;

                } else {

                    lostMatches++;

                    ColumnPair pair = allPairs.get(k);

                    System.out.println(
                        "[" + i + "] "
                        + pair.getSourceColumn().getLabel()
                        + " <-> "
                        + "[" + j + "] "
                        + pair.getTargetColumn().getLabel()
                        + " : "
                        + score
                    );
                }
            }
        }


        // ==========================================
        // 10. Reduction Ratio 계산
        // ==========================================

        double reductionRatio = Double.NaN;

        if (totalPairs > 0) {

            reductionRatio =
                1.0 - (double) candidateCount / totalPairs;
        }


        // ==========================================
        // 11. Pair Completeness 계산
        // ==========================================

        double pairCompleteness = Double.NaN;

        if (groundTruthCount > 0) {

            pairCompleteness =
                (double) preservedMatches / groundTruthCount;
        }


        // ==========================================
        // 12. Summary
        // ==========================================

        System.out.println("\n===== Summary =====");

        System.out.println("Total columns: " + n);

        System.out.println(
            "Total possible pairs: " + totalPairs
        );

        System.out.println(
            "Ground Truth Matches: " + groundTruthCount
        );

        System.out.println(
            "Candidates after blocking: " + candidateCount
        );

        System.out.println(
            "Preserved Matches: " + preservedMatches
        );

        System.out.println(
            "Lost Matches: " + lostMatches
        );

        System.out.println(
            "Reduction Ratio: " + reductionRatio
        );

        System.out.println(
            "Pair Completeness: " + pairCompleteness
        );

        System.out.println("===== Finished =====");
    }
}
