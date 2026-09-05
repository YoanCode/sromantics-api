package com.sromantics.sromantics_api.attendance;

import com.sromantics.sromantics_api.entity.Attendance;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;

    @GetMapping
    public List<Attendance> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Attendance get(@PathVariable String id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Attendance create(@RequestBody Attendance attendance) {
        return service.create(attendance);
    }

    @PutMapping("/{id}")
    public Attendance update(@PathVariable String id, @RequestBody Attendance attendance) {
        return service.update(id, attendance);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
