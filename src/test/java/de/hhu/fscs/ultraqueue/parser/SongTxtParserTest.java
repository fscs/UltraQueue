package de.hhu.fscs.ultraqueue.parser;

import de.hhu.fscs.ultraqueue.model.Song;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SongTxtParserTest {

    // -----------------------------------------------------------------
    // A tiny UltraStar song definition that contains:
    //   • TITLE, ARTIST, BPM
    //   • three note lines (start beat 0, 4 and 8, each 4 beats long)
    //   • no #END tag (so the length must be calculated from BPM+notes)
    // -----------------------------------------------------------------
    private static final String SIMPLE_SONG_TXT = """
            #ARTIST:Schweinemensakapelle
            #TITLE:Anspruchlos dürch die Nacht
            #MP3:08-Anspruchslos.mp3
            #GENRE:Pop
            #ALBUM:InPhiMa ESAG-Theater
            #YEAR:2015
            #LANGUAGE:German
            #BPM:256.05
            #GAP:7300
            #GAP_EDITOR:7400
            #GAP_KOR:7900
            #COVER:esagws16logo.jpg
            #BACKGROUND:schweinemensakapelle.jpg
            : 1 2 0 Wir
            : 4 2 4  zie
            : 8 2 3 hen
            : 12 2 4  durch
            : 16 2 3  die
            : 20 2 0  Knei
            : 24 2 0 pen
            : 240 2 3  sind
            : 244 13 0  leer,
            - 261
            : 332 8 2  so,
            * 357 6 3  so
            : 365 6 4  so!
            - 376
            : 380 3 4 Wenn
            : 5780 4 11 ße
            : 5788 4 13  nachts
            : 5796 4 11  ge
            : 5805 3 12 pennt
            : 5809 7 9 ~.
            - 5822
            : 5826 5 12 Rub
            : 5834 4 13 bel
            : 5842 12 9 los
            : 5906 6 7  bin
            : 5914 4 -2  ich
            : 5921 5 -1  Mil
            : 5929 5 2 lio
            : 5938 10 3 när!
            E
            """;

    @Test
    @DisplayName("parseLines() calculates length from BPM + notes roughly correctly")
    void parseLines_calculatesLengthCorrectly() {
        List<String> lines = List.of(SIMPLE_SONG_TXT.split("\\R"));

        Song song = SongTxtParser.parseLines(lines);

        int mp3Length = 6 * 60 + 16;
        assertThat(song.getLengthSeconds()).isCloseTo(mp3Length, Percentage.withPercentage(10));
    }

    @Test
    @DisplayName("parseLines() gets meta data correctly")
    void parseLines_metadata() {
        List<String> lines = List.of(SIMPLE_SONG_TXT.split("\\R"));

        Song song = SongTxtParser.parseLines(lines);

        assertThat(song.title()).isEqualTo("Anspruchlos dürch die Nacht");
        assertThat(song.artist()).isEqualTo("Schweinemensakapelle");
        assertThat(song.language()).isEqualTo("German");
        assertThat(song.year()).isEqualTo(2015);
        assertThat(song.genre()).isEqualTo("Pop");
    }


    @Test
    @DisplayName("parseLines() uses default 3‑minute length when no data available")
    void parseLines_defaultLengthWhenNoInfo() {
        String txt = """
                #TITLE: Empty
                #ARTIST: Nobody
                """;

        List<String> lines = List.of(txt.split("\\R"));
        Song song = SongTxtParser.parseLines(lines);

        assertThat(song.length()).isEqualTo(Duration.ofSeconds(3 * 60));
    }

    @Test
    @DisplayName("parseLyricsLines() extracts lyric lines from note rows")
    void parseLyricsLines_extractsLyrics() {
        List<String> lines = List.of(SIMPLE_SONG_TXT.split("\\R"));

        List<String> lyrics = SongTxtParser.parseLyricsLines(lines);

        assertThat(lyrics).hasSize(4);
        assertThat(lyrics.getFirst()).isEqualTo("Wir ziehen durch die Kneipen sind leer,");
        assertThat(lyrics.get(2)).endsWith(" nachts gepennt.");
        assertThat(lyrics.getLast()).endsWith("Millionär!");
    }

    @Test
    @DisplayName("lyrics with spaces at end of line correctly preserve spaces")
    void test_1() {
        List<String> lines = List.of("""
                #ARTIST:Schweinemensakapelle
                #TITLE:Anspruchslos dürch die Nacht
                #MP3:08-Anspruchslos.mp3
                #BPM:256.05
                : 1 2 0 Wir\s
                : 4 2 4 zie
                : 8 2 3 hen\s
                : 12 2 4 durch""".split("\n"));

        List<String> lyrics = SongTxtParser.parseLyricsLines(lines);
        assertThat(lyrics).containsExactly("Wir ziehen durch");
    }
}