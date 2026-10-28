package org.ebi.sportbets.repository;

import jakarta.persistence.EntityManager;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.TippModus;
import org.ebi.sportbets.entity.TippModusToto;
import org.ebi.sportbets.enums.TippModusType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

// Uses the pooled H2 datasource of the "it" profile instead of an unpooled replacement: H2 evaluates
// check constraints (like the one on "type") through the session that created them, which an
// unpooled datasource closes right after schema creation.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("it")
class TippModusRepositoryTest {

    @Autowired
    private TippModusRepository tippModusRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_storeDisplayNameInTypeColumn_when_savingTippModus() {
        // Arrange
        Community community = communityRepository.save(Community.builder().name("Stammtisch").build());
        TippModus tippModus = TippModusToto.builder()
                .name("Toto")
                .type(TippModusType.TIPPMODUS_TOTO)
                .community(community)
                .build();

        // Act
        TippModus saved = tippModusRepository.saveAndFlush(tippModus);

        // Assert
        String column = jdbcTemplate.queryForObject(
                "select type from tipp_modus_toto where id = ?", String.class, saved.getId());
        assertThat(column).isEqualTo("TotoTipp");
    }

    @Test
    void should_readEnum_when_typeColumnHoldsExistingDisplayName() {
        // Arrange
        Community community = communityRepository.saveAndFlush(Community.builder().name("Stammtisch").build());
        jdbcTemplate.update("insert into tipp_modus_point (id, name, type, fk_comm_id, created_on) "
                + "values (?, ?, ?, ?, current_timestamp)", 900L, "Punkte", "PointTipp", community.getId());
        entityManager.clear();

        // Act
        TippModus result = tippModusRepository.findById(900L).orElseThrow();

        // Assert
        assertThat(result.getType()).isEqualTo(TippModusType.TIPPMODUS_POINT);
    }
}
