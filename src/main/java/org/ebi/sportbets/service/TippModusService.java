package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.TippModusDto;
import org.ebi.sportbets.dto.TippModusPointDto;
import org.ebi.sportbets.dto.TippModusResultDto;
import org.ebi.sportbets.dto.TippModusTotoDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.TippModus;
import org.ebi.sportbets.entity.TippModusPoint;
import org.ebi.sportbets.entity.TippModusResult;
import org.ebi.sportbets.entity.TippModusToto;
import org.ebi.sportbets.enums.TippModusType;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.TippModusRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TippModusService {

    private final TippModusRepository tippModusRepository;
    private final CommunityRepository communityRepository;

    // The filter is optional; null means "all communities"
    public List<TippModusDto> find(Long communityId) {
        List<TippModus> tippModi = communityId != null
                ? tippModusRepository.findByCommunityId(communityId)
                : tippModusRepository.findAll();
        return tippModi.stream()
                .map(this::toDto)
                .toList();
    }

    public TippModusDto findById(Long id) {
        return tippModusRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("TippModus", id));
    }

    @Transactional
    public TippModusDto create(TippModusDto tippModusDto) {
        TippModusType type = typeOf(tippModusDto);
        Community community = communityRepository.findById(tippModusDto.getCommunityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community", tippModusDto.getCommunityId()));
        TippModus tippModus = switch (type) {
            case TIPPMODUS_POINT -> TippModusPoint.builder().build();
            case TIPPMODUS_RESULT -> TippModusResult.builder().build();
            case TIPPMODUS_TOTO -> TippModusToto.builder().build();
        };
        tippModus.setCommunity(community);
        applyChanges(tippModus, tippModusDto, type, null);
        // id and createdOn from the request are ignored; both are generated on insert.
        // The table-generated id defers the insert to the flush, so flush now to get createdOn into the response.
        return toDto(tippModusRepository.saveAndFlush(tippModus));
    }

    @Transactional
    public TippModusDto update(Long id, TippModusDto tippModusDto) {
        TippModus tippModus = tippModusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TippModus", id));
        TippModusType type = typeOf(tippModusDto);
        // Each subtype lives in its own table, so the subtype of a stored TippModus is fixed
        TippModusType storedType = typeOf(tippModus);
        if (type != storedType) {
            throw new BusinessRuleException("TippModus " + id + " is a " + storedType.getDisplayName()
                    + " and cannot be changed to a " + type.getDisplayName());
        }
        // Tipps must use a TippModus of their own community, so moving it would break existing Tipps
        if (!tippModus.getCommunity().getId().equals(tippModusDto.getCommunityId())) {
            throw new BusinessRuleException("TippModus " + id + " cannot be moved to another community");
        }
        // The path id wins over any id in the body; createdOn is never changed
        applyChanges(tippModus, tippModusDto, type, id);
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(tippModus);
    }

    @Transactional
    public void delete(Long id) {
        TippModus tippModus = tippModusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TippModus", id));
        // Tipps and TippConfigs using this TippModus are removed via JPA cascade
        tippModusRepository.delete(tippModus);
    }

    // The subtype in the request decides the type; a type sent alongside must agree with it
    private TippModusType typeOf(TippModusDto tippModusDto) {
        TippModusType type = switch (tippModusDto) {
            case TippModusPointDto _ -> TippModusType.TIPPMODUS_POINT;
            case TippModusResultDto _ -> TippModusType.TIPPMODUS_RESULT;
            case TippModusTotoDto _ -> TippModusType.TIPPMODUS_TOTO;
            default -> throw new IllegalArgumentException("Unknown TippModusDto subtype " + tippModusDto.getClass());
        };
        if (tippModusDto.getType() != null && !tippModusDto.getType().equals(type.getDisplayName())) {
            throw new BusinessRuleException("Type " + tippModusDto.getType()
                    + " does not match the modus of the request, expected " + type.getDisplayName());
        }
        return type;
    }

    // All checks run before the entity is touched, so a rejected request leaves it unchanged
    private void applyChanges(TippModus tippModus, TippModusDto tippModusDto, TippModusType type, Long ownId) {
        ensureUniqueName(tippModus.getCommunity().getId(), tippModusDto.getName(), type, ownId);

        tippModus.setName(tippModusDto.getName());
        tippModus.setType(type);
        tippModus.setDeadline(tippModusDto.getDeadline());
        switch (Hibernate.unproxy(tippModus)) {
            case TippModusPoint point -> point.setTotalPoints(((TippModusPointDto) tippModusDto).getTotalPoints());
            case TippModusResult result -> {
                TippModusResultDto resultDto = (TippModusResultDto) tippModusDto;
                result.setTendencyPoints(resultDto.getTendencyPoints());
                result.setBonusPoints(resultDto.getBonusPoints());
            }
            default -> {
                // TippModusToto has no fields of its own
            }
        }
    }

    // Mirrors the unique (name, fk_comm_id) constraint, which exists separately in each subtype table
    private void ensureUniqueName(Long communityId, String name, TippModusType type, Long ownId) {
        boolean taken = tippModusRepository.findByCommunityIdAndName(communityId, name).stream()
                .anyMatch(existing -> typeOf(existing) == type && !existing.getId().equals(ownId));
        if (taken) {
            throw new DuplicateResourceException(type.getDisplayName() + " with name " + name
                    + " already exists in Community " + communityId);
        }
    }

    // Derived from the subtype table rather than the type column, which legacy rows may leave empty
    private static TippModusType typeOf(TippModus tippModus) {
        return switch (Hibernate.unproxy(tippModus)) {
            case TippModusPoint _ -> TippModusType.TIPPMODUS_POINT;
            case TippModusResult _ -> TippModusType.TIPPMODUS_RESULT;
            case TippModusToto _ -> TippModusType.TIPPMODUS_TOTO;
            default -> throw new IllegalStateException("Unknown TippModus subtype " + tippModus.getClass());
        };
    }

    private TippModusDto toDto(TippModus tippModus) {
        TippModusDto.TippModusDtoBuilder<?, ?> builder = switch (Hibernate.unproxy(tippModus)) {
            case TippModusPoint point -> TippModusPointDto.builder()
                    .totalPoints(point.getTotalPoints());
            case TippModusResult result -> TippModusResultDto.builder()
                    .tendencyPoints(result.getTendencyPoints())
                    .bonusPoints(result.getBonusPoints());
            case TippModusToto _ -> TippModusTotoDto.builder();
            default -> throw new IllegalStateException("Unknown TippModus subtype " + tippModus.getClass());
        };
        // getId() on the lazy community proxy does not trigger a load of the community
        return builder
                .id(tippModus.getId())
                .name(tippModus.getName())
                .type(typeOf(tippModus).getDisplayName())
                .deadline(tippModus.getDeadline())
                .communityId(tippModus.getCommunity().getId())
                .createdOn(tippModus.getCreatedOn())
                .build();
    }
}
