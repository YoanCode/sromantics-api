package com.sromantics.sromantics_api.makeup;

import com.sromantics.sromantics_api.entity.MakeUpCredit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/make-up-credits")
@RequiredArgsConstructor
public class MakeUpCreditController {

    private final MakeUpCreditService service;

    @GetMapping
    public List<MakeUpCredit> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public MakeUpCredit get(@PathVariable String id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public MakeUpCredit update(@PathVariable String id, @RequestBody MakeUpCreditRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String id) {
        service.cancel(id);
    }
}
