package de.hhu.fscs.ultraqueue.parser;

import de.hhu.fscs.ultraqueue.model.Song;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Pure utility class that converts the raw lines of an UltraStar *.txt* file
 * into a {@link Song} instance.
 *
 * <p>No logging, no filesystem writes – the only I/O performed here is the
 * optional {@link #parse(Path)} method that reads the file into a
 * {@code List<String>}.  All parsing logic lives in {@link #parseLines(List)}.
 */
public final class SongTxtParser {

    private SongTxtParser() { /* utility – no instances */ }

    /**
     * Convenience wrapper that reads the file with {@link Files#readAllLines}
     * and then delegates to {@link #parseLines(List)}.
     *
     * @param txtFile   the *.txt* definition file
     * @return a fully populated {@link Song}
     * @throws IllegalStateException if the file cannot be read
     */
    public static Song parse(Path txtFile) {
        try {
            List<String> lines = Files.readAllLines(txtFile);
            return parseLines(lines);
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
     * Parses the *already‑read* lines of a UltraStar *.txt* file.
     *
     * <p>The algorithm is exactly the same as the one you previously had
     * inside {@code SongCatalogServiceImpl}, but it is now pure‑functional:
     * given the same list of lines it will always return the same {@link Song}.
     *
     * @param lines      the file content (one element per line, no line‑breaks)
     * @return a {@link Song} instance
     */
    public static Song parseLines(List<String> lines) {
        String title   = null;
        String artist  = null;
        String language = null;
        String genre = null;
        Integer year   = null;
        Duration length = null;

        // values needed for the BPM‑based length calculation
        double bpm            = 0;                // beats per minute
        double firstStartBeat = Double.MAX_VALUE; // earliest start beat seen
        double lastEndBeat    = Double.MIN_VALUE; // latest (start+duration) beat seen

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
            } else if (line.startsWith("#YEAR:")) {
                String y = line.substring(6).trim();
                try {
                    year = Integer.valueOf(y);
                } catch (NumberFormatException ignored) { }
            } else if (line.startsWith("#BPM:")) {
                String v = line.substring(5).trim();
                try {
                    bpm = Double.parseDouble(v);
                } catch (NumberFormatException ignored) { }
            }

            // note lines – start beat + duration
            // UltraStar note lines start with ':', '*', 'F', 'G', 'R' …
            // Format (simplified):  <type> <startBeat> <beatLength> [...]
            else if (line.startsWith(":")
                    || line.startsWith("*")
                    || line.startsWith("F")
                    || line.startsWith("G")
                    || line.startsWith("R")) {

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
                } catch (NumberFormatException ignored) {
                    // ignore a single broken note – it does not affect length
                }
            }
            // any other line is ignored for the length calculation
        }

        if (title == null || title.isBlank()) title = "Untitled";
        if (artist == null || artist.isBlank()) artist = "Unknown";

        if (bpm > 0
                && firstStartBeat != Double.MAX_VALUE
                && lastEndBeat   != Double.MIN_VALUE) {

            double secondsPerBeat = 60.0 / bpm / 4; // the note timestampt are actually 1/4 BPM
            double songSeconds = (lastEndBeat - firstStartBeat) * secondsPerBeat + 20.0; // guessed 20 s for intro/outro

            // Guard against negative / nonsensical results
            if (songSeconds < 0) songSeconds = 0;

            long millis = Math.round(songSeconds * 1000);
            length = Duration.ofMillis(millis);
        }

        if (length == null) {
            length = Duration.ofSeconds(180); // 3 minutes
        }

        return new Song.Builder()
                .title(title)
                .artist(artist)
                .genre(genre)
                .language(language)
                .year(year)
                .length(length)
                .build();
    }

    /**
     * Extracts human-readable lyric lines from note rows.
     */
    public static List<String> parseLyricsLines(List<String> lines) {
        List<String> result = new java.util.ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.startsWith("-")) {
                flushLyricsLine(result, currentLine);
                continue;
            }
            if (line.startsWith("E")) {
                flushLyricsLine(result, currentLine);
                break;
            }

            if (line.startsWith(":")
                    || line.startsWith("*")
                    || line.startsWith("F")
                    || line.startsWith("G")
                    || line.startsWith("R")) {
                String[] parts = line.split("\\s", 5);
                if (parts.length < 5) {
                    continue;
                }

                String token = parts[4];
                if (token.trim().isEmpty()) {
                    continue;
                }

                currentLine.append(token.replace("~", ""));
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