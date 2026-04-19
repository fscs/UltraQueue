package de.hhu.fscs.ultraqueue.service;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.model.Song;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SongCatalogServiceImplScanningTest {

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
}
