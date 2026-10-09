package org.example.earthjukebox.nasa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GistempCsvParserTest {
    private final GistempCsvParser parser = new GistempCsvParser();
    private static final String HEADER = "Year,Jan,Feb,Mar,Apr,May,Jun,Jul,Aug,Sep,Oct,Nov,Dec,J-D";

    @Test
    void readsMonthlyAnomaliesAndIgnoresAnnualAverage() {
        var result = parser.parse(csv("2024,1.25,.50,.40,.30,.20,.10,.00,-.10,-.20,-.30,-.40,-.50,99.99"));
        assertThat(result.values()).hasSize(12);
        assertThat(result.values().getFirst().anomalyC()).isEqualByComparingTo(new BigDecimal("1.25"));
        assertThat(result.values().getLast().month()).isEqualTo(12);
    }

    @Test
    void skipsMissingMonthsWithoutTurningThemIntoZero() {
        var result = parser.parse(csv("2024,1.25,***,,***,***,***,***,***,***,***,***,***,***"));
        assertThat(result.values()).hasSize(1);
        assertThat(result.values().getFirst().month()).isEqualTo(1);
    }

    @Test
    void acceptsUtf8BomInTitle() {
        var result = parser.parse("\uFEFF" + csv("2024,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1"));
        assertThat(result.values()).hasSize(12);
    }

    @Test
    void rejectsHtmlAndIncorrectHeader() {
        assertThatThrownBy(() -> parser.parse("<html>error</html>")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse("Land-Ocean: Global Means\nYear,Jan\n2024,1.25"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateYearsAndYearGaps() {
        String row = "2024,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1";
        assertThatThrownBy(() -> parser.parse(csv(row + "\n" + row)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(csv(row.replace("2024", "2022") + "\n" + row)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidNumbersAndIncompleteRows() {
        assertThatThrownBy(() -> parser.parse(csv("2024,oops,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(csv("2024,.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String csv(String rows) {
        return "Land-Ocean: Global Means\n" + HEADER + "\n" + rows + "\n";
    }
}

