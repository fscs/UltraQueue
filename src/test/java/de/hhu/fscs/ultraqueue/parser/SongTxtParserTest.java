package de.hhu.fscs.ultraqueue.parser;

import de.hhu.fscs.ultraqueue.model.Song;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Component
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
    public static final Duration FALLBACK_LENGTH = Duration.ofSeconds(3 * 60);

    @Autowired
    private SongTxtParser songTxtParser;

    @Test
    @DisplayName("parseLines() calculates length from BPM + notes roughly correctly")
    void parseSongFile_calculatesLengthCorrectly() {
        List<String> lines = List.of(SIMPLE_SONG_TXT.split("\\R"));

        Song song = songTxtParser.parseSongFile(lines);

        int mp3Length = 6 * 60 + 16;
        assertThat(song.getLengthSeconds()).isCloseTo(mp3Length, Percentage.withPercentage(10));
    }

    @Test
    @DisplayName("parseLines() gets meta data correctly")
    void parseSongFile_metadata() {
        List<String> lines = List.of(SIMPLE_SONG_TXT.split("\\R"));

        Song song = songTxtParser.parseSongFile(lines);

        assertThat(song.title()).isEqualTo("Anspruchlos dürch die Nacht");
        assertThat(song.artist()).isEqualTo("Schweinemensakapelle");
        assertThat(song.language()).isEqualTo("German");
        assertThat(song.year()).isEqualTo(2015);
        assertThat(song.genre()).isEqualTo("Pop");
    }


    @Test
    @DisplayName("parseLines() uses default 3‑minute length when no data available")
    void parseSongFile_defaultLengthWhenNoInfo() {
        String txt = """
                #TITLE: Empty
                #ARTIST: Nobody
                """;

        List<String> lines = List.of(txt.split("\\R"));
        Song song = songTxtParser.parseSongFile(lines);

        assertThat(song.length()).isEqualTo(FALLBACK_LENGTH);
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

    @Test
    @DisplayName("length estimation supports decimal comma for bpm")
    void test_1a() {
        List<String> lines = List.of("""
                #ARTIST:Schweinemensakapelle
                #TITLE:Anspruchslos dürch die Nacht
                #MP3:08-Anspruchslos.mp3
                #BPM:256,05
                : 1 2 0 Wir\s
                : 4 2 4 zie
                : 8 2 3 hen\s
                : 12 2 4 durch
                - 5822
                : 5914 4 -2  ich
                : 5921 5 -1  Mil
                : 5929 5 2 lio
                : 5938 10 3 när!
                E""".split("\n"));

        Song song = songTxtParser.parseSongFile(lines);

        int mp3Length = 6 * 60 + 16;
        assertThat(song.getLengthSeconds()).isCloseTo(mp3Length, Percentage.withPercentage(10));
    }

    @Test
    @DisplayName("gap is considered in length estimate")
    void test_2() {
        List<String> lines = List.of("""
                #TITLE:The Title
                #ARTIST:Artist 1
                #LANGUAGE:English
                #YEAR:2002
                #BPM:252,1
                #GAP:34070
                : 0 4 5 The
                : 5 20 3  xx
                : 29 3 5  is
                : 33 2 7  o
                : 64 11 3 xx,
                - 78
                : 81 8 10  time
                : 90 1 9  xx
                : 2799 3 14  xx
                : 2804 2 14 xx
                : 2810 5 14 xx
                - 2816
                : 2848 11 14 ty
                : 2880 3 13 ~
                : 2885 29 14 ~
                E""".split("\n"));

        Song song = songTxtParser.parseSongFile(lines);
        int mp3length = 4 * 60 + 20;
        assertThat(song.getLengthSeconds()).isCloseTo(mp3length, Percentage.withPercentage(13));
    }

    @Test
    @DisplayName("parseLines() uses default 3‑minute length when BPM ist invalid")
    void test3() {
        List<String> lines = List.of("""
                #TITLE:The Title
                #ARTIST:Artist 1
                #LANGUAGE:English
                #YEAR:2002
                #BPM:twohundred
                #GAP:34070
                : 0 4 5 The
                : 5 20 3  xx
                : 29 3 5  is
                : 33 2 7  o
                : 64 11 3 xx,
                - 78
                : 81 8 10  time
                : 90 1 9  xx
                : 2799 3 14  xx
                : 2804 2 14 xx
                : 2810 5 14 xx
                - 2816
                : 2848 11 14 ty
                : 2880 3 13 ~
                : 2885 29 14 ~
                E""".split("\n"));

        Song song = songTxtParser.parseSongFile(lines);

        assertThat(song.length()).isEqualTo(FALLBACK_LENGTH);
    }

    @Test
    @DisplayName("fall back to default length when song uses relative timestamps")
    void test4() {
        List<String> lines = List.of("""
                #TITLE:x
                #ARTIST:y
                #LANGUAGE:English
                #MP3 x.ogg
                #COVER:x.jpg
                #VIDEO:x.mp4
                #RELATIVE:yes
                #BPM:256,26
                #GAP:8630
                : 0 2 2 Ich\s
                : 4 2 2 war\s
                - 26 26
                : 2 2 1 Und\s
                : 60 2 4 ben\s
                - 64 64
                : 32 2 6 Wo
                : 37 2 2 von\s
                : 41 2 6 sol
                : 44 2 2 len\s
                : 48 2 6 wir\s
                * 52 4 7 geh
                * 60 2 4 gehn\s
                E""".split("\n"));

        Song song = songTxtParser.parseSongFile(lines);

        assertThat(song.length()).isEqualTo(FALLBACK_LENGTH);
    }
}