package com.sromantics.sromantics_api.clazz;

import com.sromantics.sromantics_api.entity.Clazz;
import com.sromantics.sromantics_api.repository.ClazzRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClazzController {

    private final ClazzRepository repository;

    @GetMapping
    public List<Clazz> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Clazz get(@PathVariable String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Clazz create(@RequestBody Clazz clazz) {
        clazz.setId(UUID.randomUUID().toString());
        return repository.save(clazz);
    }

    @PutMapping("/{id}")
    public Clazz update(@PathVariable String id, @RequestBody Clazz clazz) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        clazz.setId(id);
        return repository.save(clazz);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
    }
}
