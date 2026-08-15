package com.sromantics.sromantics_api.parent;

import com.sromantics.sromantics_api.dto.ParentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @GetMapping
    public List<ParentDto> list() {
        return parentService.findAll();
    }

    @GetMapping("/{id}")
    public ParentDto get(@PathVariable String id) {
        return parentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParentDto create(@RequestBody ParentDto dto) {
        return parentService.create(dto);
    }

    @PutMapping("/{id}")
    public ParentDto update(@PathVariable String id, @RequestBody ParentDto dto) {
        return parentService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        parentService.delete(id);
    }
}
