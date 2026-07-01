package de.hhu.fscs.ultraqueue.parser;

import de.hhu.fscs.ultraqueue.UltraQueueApplication;
import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.model.Song;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Pure utility class that converts the raw lines of an UltraStar *.txt* file
 * into a {@link Song} instance.
 *
 * <p>No logging, no filesystem writes – the only I/O performed here is the
 * optional {@link #parse(Path)} method that reads the file into a
 * {@code List<String>}.  All parsing logic lives in {@link #parseSongFile(List)}.
 */
@Component
public final class SongTxtParser {

    private static final Duration FALLBACK_SONG_DURATION = Duration.ofSeconds(180);
    private static final double INTRO_OUTRO_DURATION = 20.0;
    private static Path folder;

    private SongTxtParser() {
    }


    /**
     * Convenience wrapper that reads the file with {@link Files#readAllLines}
     * and then delegates to {@link #parseSongFile(List)}.
     *
     * @param txtFile   the *.txt* definition file
     * @return a fully populated {@link Song}
     * @throws IllegalStateException if the file cannot be read
     */
    public static Song parse(Path txtFile) {
        folder = Path.of(txtFile.getParent().toString().substring("/home/felix/.ultrastardx/songs/".length()));
        try {
            List<String> lines = Files.readAllLines(txtFile);
            return parseSongFile(lines);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read song definition: " + txtFile, e);
        }
    }

    /**
     * Reads and extracts plain lyric lines from an UltraStar *.txt* file.
     */
    public static List<String> parseLyrics(Path txtFile) {
        try {
            List<String> lines = Files.readAllLines(txtFile);
            return parseLyricsLines(lines);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read song lyrics: " + txtFile, e);
        }
    }

    /**
     * Parse meta data and extract estimated length from Ultrastar txt
     *
     * @param lines      the file content (one element per line, no line‑breaks)
     * @return a {@link Song} instance
     */
    public static Song parseSongFile(List<String> lines) {
        String title = null;
        String artist = null;
        String language = null;
        String genre = null;
        Integer year = null;
        String cover = null;

        // values needed for the BPM‑based length calculation
        double bpm = 0;
        double firstStartBeat = Double.MAX_VALUE;
        double lastEndBeat = Double.MIN_VALUE;
        double gap = 0;
        boolean isRelativeFormat = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.startsWith("#TITLE:")) {
                title = line.substring(7).trim();
            } else if (line.startsWith("#ARTIST:")) {
                artist = line.substring(8).trim();
            } else if (line.startsWith("#LANGUAGE:")) {
                language = line.substring(10).trim();
            } else if (line.startsWith("#GENRE:")) {
                genre = line.substring(7).trim();
            } else if (line.startsWith("#GAP:")) {
                gap = parseDoubleProperty(line, gap);
            } else if (line.startsWith("#YEAR:")) {
                String y = line.substring(6).trim();
                try {
                    year = Integer.valueOf(y);
                } catch (NumberFormatException _) { }
            } else if (line.startsWith("#BPM:")) {
                bpm = parseDoubleProperty(line, bpm);
            } else if (line.startsWith("#RELATIVE:")) {
                isRelativeFormat = line.toLowerCase(Locale.ROOT).contains("yes");
            } else if (line.startsWith("#COVER:")) {
                cover = line.substring(7).trim();
            }

            else if (isNoteLine(line)) {

                String[] parts = line.split("\\s+");
                if (parts.length < 3) {
                    continue;   // malformed note line – ignore safely
                }
                try {
                    double startBeat  = Double.parseDouble(parts[1]);
                    double beatLength = Double.parseDouble(parts[2]);

                    if (startBeat < firstStartBeat) {
                        firstStartBeat = startBeat;
                    }
                    double noteEnd = startBeat + beatLength;
                    if (noteEnd > lastEndBeat) {
                        lastEndBeat = noteEnd;
                    }
                } catch (NumberFormatException _) {
                    // ignore a single broken note – it does not affect length
                }
            }
            // any other line is ignored for the length calculation
        }

        if (isUnset(title)) title = "Untitled";
        if (isUnset(artist)) artist = "Unknown";

        Duration length = isRelativeFormat ? FALLBACK_SONG_DURATION : songLength(bpm, firstStartBeat, lastEndBeat, gap);

        String encoded = StreamSupport.stream(folder.resolve(cover).spliterator(), false)
                .map(Path::toString)
                .map(segment -> UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));

        return new Song.Builder()
                .title(title)
                .artist(artist)
                .genre(genre)
                .language(language)
                .year(year)
                .length(length)
                .coverPath("files/"+encoded)
                .build();
    }

    private static double parseDoubleProperty(String line, double orElse) {
        try {
            String v = line.split(":", 2)[1].trim();
            return Double.parseDouble(v.replace(",", "."));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException _) {
            // swallow
        }
        return orElse;
    }

    /**
     * Note lines have the format <type> <startBeat> <beatLength> <text>
     * NB: Text may have leading/trailing whitespace.
     */
    private static boolean isNoteLine(String line) {
        return line.startsWith(":")
                || line.startsWith("*")
                || line.startsWith("F")
                || line.startsWith("G")
                || line.startsWith("R");
    }

    private static boolean isUnset(String title) {
        return title == null || title.isBlank();
    }

    private static Duration songLength(double bpm, double firstStartBeat, double lastEndBeat, double gap) {
        if (bpm <= 0 || firstStartBeat == Double.MAX_VALUE || lastEndBeat == Double.MIN_VALUE) {
            return FALLBACK_SONG_DURATION;
        }

        double secondsPerBeat = 60.0 / bpm / 4; // the note timestamps are actually 1/4 BPM
        double songSeconds = (lastEndBeat - firstStartBeat) * secondsPerBeat + INTRO_OUTRO_DURATION; // guessed duration for intro/outro

        // Guard against negative / nonsensical results
        if (songSeconds < 0) songSeconds = 0;

        long millis = Math.round(songSeconds * 1000);
        return Duration.ofMillis(millis + (long)gap);

    }

    public static List<String> parseLyricsLines(List<String> lines) {
        List<String> result = new java.util.ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("-") || line.startsWith("E")) {
                flushLyricsLine(result, currentLine);
            } else if (isNoteLine(line)) {
                String[] parts = line.split("\\s", 5);
                if (parts.length >= 5) {
                    String token = parts[4];
                    if (!token.trim().isEmpty()) {
                        currentLine.append(token.replace("~", ""));
                    }
                }
            }
        }

        flushLyricsLine(result, currentLine);
        return result;
    }

    private static void flushLyricsLine(List<String> target, StringBuilder currentLine) {
        if (!currentLine.isEmpty()) {
            target.add(currentLine.toString().trim());
            currentLine.setLength(0);
        }
    }
}