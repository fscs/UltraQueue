package de.hhu.fscs.ultraqueue.service;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.model.Song;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SongCatalogServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void testRecursiveScanning() throws IOException {
        // Setup:
        // root/
        //   Song1/song1.txt
        //   A/
        //     Artist/
        //       Song2/song2.txt
        
        Path song1Dir = tempDir.resolve("Song1");
        Files.createDirectories(song1Dir);
        Files.writeString(song1Dir.resolve("song1.txt"), "#TITLE:Song 1\n#ARTIST:Artist 1\n");

        Path deepSongDir = tempDir.resolve("A").resolve("Artist").resolve("Song2");
        Files.createDirectories(deepSongDir);
        Files.writeString(deepSongDir.resolve("song2.txt"), "#TITLE:Song 2\n#ARTIST:Artist 2\n");

        UltraQueueProperties props = mock(UltraQueueProperties.class);
        when(props.songFolder()).thenReturn(tempDir.toString());

        SongCatalogServiceImpl service = new SongCatalogServiceImpl(props);
        service.init();

        List<Song> songList = service.findAll(Pageable.ofSize(100)).getContent();

        assertThat(songList).hasSize(2);
        assertThat(songList).extracting(Song::title).containsExactlyInAnyOrder("Song 1", "Song 2");
    }

    @Test
    void testFindLyricsByIdLazyLoadsFromTxt() throws IOException {
        Path songDir = tempDir.resolve("Song1");
        Files.createDirectories(songDir);
        Files.writeString(songDir.resolve("song1.txt"), """
                #TITLE:Song 1
                #ARTIST:Artist 1
                #BPM:120
                : 0 4 0 Hel
                : 4 4 0 lo
                - 8
                E
                """);

        UltraQueueProperties props = mock(UltraQueueProperties.class);
        when(props.songFolder()).thenReturn(tempDir.toString());

        SongCatalogServiceImpl service = new SongCatalogServiceImpl(props);
        service.init();

        Song song = service.findAll(Pageable.ofSize(10)).getContent().getFirst();
        var lyrics = service.findLyricsById(song.id());

        assertThat(lyrics).isPresent();
        assertThat(lyrics.orElseThrow()).contains("Hello");
    }

    @Test
    void testFindLyricsByIdReturnsEmptyForUnknownSong() {
        UltraQueueProperties props = mock(UltraQueueProperties.class);
        when(props.songFolder()).thenReturn(tempDir.toString());

        SongCatalogServiceImpl service = new SongCatalogServiceImpl(props);
        service.init();

        assertThat(service.findLyricsById(UUID.randomUUID())).isEmpty();
    }

    private SongCatalogServiceImpl serviceWithFakeSongs() throws IOException {
        Path songDir = tempDir.resolve("Song1");
        Files.createDirectories(songDir);
        Files.writeString(songDir.resolve("song1.txt"), """
                #TITLE:Song 1
                #ARTIST:Artist 1
                #BPM:120
                : 0 4 0 Hel
                : 4 4 0 lo
                - 8
                E
                """);
        Files.createDirectories(songDir);
        Files.writeString(songDir.resolve("song2.txt"), """
                #TITLE:Blabla
                #ARTIST:Foobar
                #BPM:120
                : 0 4 0 Hel
                : 4 4 0 lo
                - 8
                E
                """);

        UltraQueueProperties props = mock(UltraQueueProperties.class);
        when(props.songFolder()).thenReturn(tempDir.toString());

        SongCatalogServiceImpl service = new SongCatalogServiceImpl(props);
        service.init();

        return service;
    }

    @Test
    @DisplayName("Search returns matching results")
    void test1() throws IOException {
        SongCatalogServiceImpl service = serviceWithFakeSongs();
        Pageable pageable = Pageable.ofSize(10);
        Page<Song> results = service.search("Foobar", pageable);
        assertThat(results).hasSize(1);
        assertThat(results.stream().findFirst().orElseThrow().artist()).isEqualTo("Foobar");
    }

    @Test
    @DisplayName("Search ignores trailing space in title, because of mobile auto space completion")
    void test1b() throws IOException {
        SongCatalogServiceImpl service = serviceWithFakeSongs();
        Pageable pageable = Pageable.ofSize(10);
        Page<Song> results = service.search("Blabla ", pageable);
        assertThat(results).hasSize(1);
        // note that this test passes because we match with a song's string representation, which happen to have a space after the title right now
        assertThat(results.stream().findFirst().orElseThrow().title()).isEqualTo("Blabla");
    }

    @Test
    @DisplayName("Search ignores trailing space in artist, because of mobile auto space completion")
    void test1c() throws IOException {
        SongCatalogServiceImpl service = serviceWithFakeSongs();
        Pageable pageable = Pageable.ofSize(10);
        Page<Song> results = service.search("Foobar ", pageable);
        assertThat(results).hasSize(1);
        // note that this test passes because we match with a song's string representation, which happen to have a space after the artist right now
        assertThat(results.stream().findFirst().orElseThrow().artist()).isEqualTo("Foobar");
    }

    @Test
    @DisplayName("Search is case insensitive matching results")
    void test2() throws IOException {
        SongCatalogServiceImpl service = serviceWithFakeSongs();
        Pageable pageable = Pageable.ofSize(10);
        Page<Song> results = service.search("foobar", pageable);
        assertThat(results).hasSize(1);
        assertThat(results.stream().findFirst().orElseThrow().artist()).isEqualTo("Foobar");
    }

    @Test
    @DisplayName("Search results can be sorted")
    void test3() throws IOException {
        SongCatalogServiceImpl service = serviceWithFakeSongs();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("artist"));
        Page<Song> results = service.search("foobar", pageable);
        assertThat(results).hasSize(1);
        assertThat(results.stream().findFirst().orElseThrow().artist()).isEqualTo("Foobar");
    }

    @Test
    @DisplayName("Complete song list can be sorted")
    void test4() throws IOException {
        SongCatalogServiceImpl service = serviceWithFakeSongs();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("artist"));
        Page<Song> results = service.findAll(pageable);
        assertThat(results).hasSize(2);
        assertThat(results.stream().findFirst().orElseThrow().artist()).isEqualTo("Artist 1");
    }

    @Test
    @DisplayName("Find by Title/Artist return result if present")
    void test5() throws IOException {
        SongCatalogServiceImpl service = serviceWithFakeSongs();
        Optional<Song> result = service.findByTitleArtist("Song 1", "Artist 1");
        assertThat(result).get().matches(song -> song.title().equals("Song 1"));
    }

    @Test
    @DisplayName("Find by Title/Artist return empty if not present")
    void test6() throws IOException {
        SongCatalogServiceImpl service = serviceWithFakeSongs();
        Optional<Song> result = service.findByTitleArtist("Song NOT PRESENT", "Artist 1");
        assertThat(result).isEmpty();
    }
}
