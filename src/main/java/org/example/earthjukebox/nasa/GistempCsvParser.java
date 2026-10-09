package org.example.earthjukebox.nasa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class GistempCsvParser {
    private static final List<String> MONTHS = List.of(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

    public ParsedDataset parse(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("NASA CSV data မရှိပါ။");
        }
        try (BufferedReader reader = new BufferedReader(new StringReader(csv))) {
            String title = reader.readLine();
            if (title == null || !title.replace("\uFEFF", "").startsWith("Land-Ocean: Global Means")) {
                throw new IllegalArgumentException("GISTEMP global temperature CSV format မဟုတ်ပါ။");
            }
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .get();
            try (CSVParser parser = format.parse(reader)) {
                if (!parser.getHeaderMap().containsKey("Year")
                        || !parser.getHeaderMap().keySet().containsAll(MONTHS)) {
                    throw new IllegalArgumentException("NASA CSV header မှာ Year/Jan–Dec မပြည့်စုံပါ။");
                }
                List<MonthlyValue> values = new ArrayList<>();
                int firstYear = 0;
                int previousYear = 0;
                for (CSVRecord record : parser) {
                    if (!record.isConsistent()) {
                        throw new IllegalArgumentException("NASA CSV row မပြည့်စုံပါ။");
                    }
                    int year = Integer.parseInt(record.get("Year"));
                    if (year < 1880 || year > Year.now().getValue()
                            || (previousYear != 0 && year != previousYear + 1)) {
                        throw new IllegalArgumentException("NASA CSV နှစ်အစဉ် မမှန်ပါ။");
                    }
                    if (firstYear == 0) {
                        firstYear = year;
                    }
                    previousYear = year;
                    for (int index = 0; index < MONTHS.size(); index++) {
                        String raw = record.get(MONTHS.get(index));
                        if (raw.isBlank() || raw.equals("***")) {
                            continue;
                        }
                        BigDecimal anomaly = new BigDecimal(raw).setScale(2, RoundingMode.UNNECESSARY);
                        if (anomaly.precision() > 6) {
                            throw new IllegalArgumentException("NASA temperature value ကို သိမ်း၍မရပါ။");
                        }
                        values.add(new MonthlyValue(year, index + 1, anomaly));
                    }
                }
                if (values.isEmpty()) {
                    throw new IllegalArgumentException("NASA CSV မှာ temperature data မရှိပါ။");
                }
                return new ParsedDataset(firstYear, previousYear, List.copyOf(values));
            }
        } catch (IOException | NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("NASA CSV ကို ဖတ်၍မရပါ။", e);
        }
    }

    public record MonthlyValue(int year, int month, BigDecimal anomalyC) {
        public int id() {
            return year * 100 + month;
        }
    }

    public record ParsedDataset(int firstYear, int lastYear, List<MonthlyValue> values) {
    }
}

