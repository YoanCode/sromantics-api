package com.sromantics.sromantics_api.parent;

import com.sromantics.sromantics_api.dto.ParentDto;
import com.sromantics.sromantics_api.entity.Parent;
import com.sromantics.sromantics_api.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = false)
public class ParentService {

    private final ParentRepository parentRepository;

    @Transactional(readOnly = true)
    public List<ParentDto> findAll() {
        return parentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ParentDto findById(String id) {
        return parentRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public ParentDto create(ParentDto dto) {
        dto.setId(UUID.randomUUID().toString());
        return toDto(parentRepository.save(toEntity(dto)));
    }

    public ParentDto update(String id, ParentDto dto) {
        if (!parentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        dto.setId(id);
        return toDto(parentRepository.save(toEntity(dto)));
    }

    public void delete(String id) {
        if (!parentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        parentRepository.deleteById(id);
    }

    private ParentDto toDto(Parent p) {
        return new ParentDto(p.getId(), p.getName(), p.getPhone(),
                p.getEmail(), p.getRelationship().name());
    }

    private Parent toEntity(ParentDto dto) {
        return new Parent(dto.getId(), dto.getName(), dto.getPhone(),
                dto.getEmail(), Parent.Relationship.valueOf(dto.getRelationship()));
    }
}
