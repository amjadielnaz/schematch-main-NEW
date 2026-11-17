
package de.uni_marburg.schematch.matching;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.InputStream;

import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_marburg.schematch.data.Column;

public class ProductDataReader {

    // JSON 데이터를 읽기 위한 객체
    private final ObjectMapper mapper = new ObjectMapper();

    // JSON 파일에서 Column 객체들을 생성하는 메서드
    public List<Column> readColumns(String filePath, int maxRows)
            throws IOException {

        
        Map<String, List<String>> columnValues =
                new LinkedHashMap<>();

        

            try (InputStream input = Files.newInputStream(Path.of(filePath));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(
                         filePath.endsWith(".json.gz") ? new GZIPInputStream(input) : input,
                         StandardCharsets.UTF_8))) {

            String line;
            int count = 0;

            
            while ((line = reader.readLine()) != null
                    && count < maxRows) {

                if (line.isBlank()) {
                    continue;
                }

                
                JsonNode json = mapper.readTree(line);

                
                extractValues("", json, columnValues);

                count++;
            }
        }

        
        List<Column> columns = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry
                : columnValues.entrySet()) {

            String columnName = entry.getKey();
            List<String> values = entry.getValue();

           
            if (values.isEmpty()) {
                continue;
            }

            Column column = new Column(columnName, values);

            columns.add(column);
        }

        return columns;
    }

   
    private void extractValues(
            String prefix,
            JsonNode json,
            Map<String, List<String>> columnValues) {

        if (json == null || json.isNull()) {
            return;
        }

        
        if (json.isObject()) {

            json.fields().forEachRemaining(entry -> {

                String key = prefix.isEmpty()
                        ? entry.getKey()
                        : prefix + "." + entry.getKey();

                extractValues(
                    key,
                    entry.getValue(),
                    columnValues
                );
            });

        } else if (json.isValueNode()) {

           
            if (prefix.equals("row_id")
                    || prefix.equals("page_url")) {
                return;
            }

            String value = json.asText();

            if (!value.isBlank()) {

                columnValues
                    .computeIfAbsent(
                        prefix,
                        k -> new ArrayList<>()
                    )
                    .add(value);
            }
        }
    }
}
