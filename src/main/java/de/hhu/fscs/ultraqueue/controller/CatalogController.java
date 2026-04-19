package de.hhu.fscs.ultraqueue.controller;

import de.hhu.fscs.ultraqueue.model.Song;
import de.hhu.fscs.ultraqueue.service.SongCatalogService;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/")
public class CatalogController {

    private final SongCatalogService catalog;
    private final UltraQueueProperties props;

    /**
     * Shows the searchable & sortable song list.
     *
     * @param page   zero‑based page index
     * @param size   page size (fallback to config)
     * @param sort   property to sort by (title, artist, lengthSec …)
     * @param dir    ASC/DESC
     * @param query  optional free‑text search term
     */
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "title") String sort,
            @RequestParam(defaultValue = "ASC") String dir,
            @RequestParam(required = false) String query,
            Model model) {

        int pageSize = (size != null) ? size : props.getPagination().getPageSize();

        Sort.Direction direction = Sort.Direction.fromString(dir);
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by(direction, sort));

        Page<Song> songPage = (query == null || query.isBlank())
                ? catalog.findAll(pageable)
                : catalog.search(query, pageable);

        model.addAttribute("songs", songPage.getContent());
        model.addAttribute("page", songPage);
        model.addAttribute("query", query);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "catalog";       // src/main/resources/templates/catalog.html
    }
}
