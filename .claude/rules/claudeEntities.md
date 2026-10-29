# Entity Model

All entities live in `org.ebi.sportbets.entity` and map onto the existing MySQL schema `bulitipper2`.
Domain terms are German: Spiel = match, Spieltag = matchday, Tipp = bet/prediction, Tipper = user who bets,
Heim = home, Gast = away, Tore = goals, Remis = draw, Anpfiff = kick-off.

## General Conventions
- Ids: `Long`, `GenerationType.IDENTITY` — except `TippModus` (see below).
- Every `@ManyToOne` is `FetchType.LAZY`; access associations inside a transaction or fetch them explicitly.
- `createdOn` (`created_on`) is set by `@CreationTimestamp`, `updatable = false`. Never set it manually. (`Tipper` and `TipperRole` have none.)
- Collections are `@Builder.Default` initialised `ArrayList`s with `cascade = ALL, orphanRemoval = true` — the parent owns its children; deleting a parent deletes the subtree.
- Lombok: plain entities use `@Builder` + `@AllArgsConstructor`; inheritance hierarchies (`Role`, `TippModus`) use `@SuperBuilder` and no `@AllArgsConstructor`.
- Column names are fixed by the legacy schema — keep the `@Column`/`@JoinColumn` names exactly as they are.

## Aggregate Overview

```
CompetitionFamily 1─* Competition
Competition 1─* CompetitionRound, CompetitionTeam, CompetitionMembership, CompetitionRole
CompetitionRound 1─* Spieltag, CompetitionGroup
Spieltag 1─* Spiel, TippConfig
Spiel 1─* Tipp, SpielFormula
Team *─1 CompetitionTeam / Spiel (heimTeam, gastTeam)
Community 1─* CommunityMembership, CompetitionMembership, TippModus, CommunityRole
Tipper 1─* CommunityMembership, TipperRole
CommunityMembership 1─* Tipp
CompetitionMembership 1─* TippConfig
TippModus 1─* Tipp, TippConfig
Role 1─* TipperRole
```

## Competition Structure

### CompetitionFamily — `competition_family`
A league/tournament series (e.g. "Bundesliga", "World Cup").
| Field | Column | Notes |
|---|---|---|
| name | name | not null, **unique** |
| description, country | | nullable |
| hasClubs | has_clubs | club teams vs. national teams |
| hasLigaModus | has_liga_modus | league format vs. tournament |
| competitions | — | 1:n `Competition`, cascade |

### Competition — `competition`
A concrete season/edition of a family.
| Field | Column | Notes |
|---|---|---|
| name | name | not null |
| description | | nullable |
| winMultiplicator | win_multiplicator | int, not null |
| remisMultiplicator | remis_multiplicator | int, not null |
| competitionFamily | competition_family_id | n:1, required |
| rounds | — | 1:n `CompetitionRound` |
| teams | — | 1:n `CompetitionTeam` |
| memberships | — | 1:n `CompetitionMembership` |
| roles | — | 1:n `CompetitionRole` |

### CompetitionRound — `competition_round`
A phase of a competition (e.g. group stage, knockout round, or the whole league season).
| Field | Column | Notes |
|---|---|---|
| name | name | not null |
| roundNumber | round_number | int |
| firstMatchday | first_matchday | int, number of the first Spieltag |
| matchdaysSize | matchdays_size | int, number of Spieltage |
| teamsSize | teams_size | int |
| hasGroups | has_groups | boolean |
| competition | fk_comp | n:1, required |
| spieltage | — | 1:n `Spieltag` — declared **before** `groups` so Spiele (which reference groups) are deleted first. Keep this order. |
| groups | — | 1:n `CompetitionGroup` |

### CompetitionGroup — `competition_group`
A group within a round (e.g. "Group A").
| Field | Column | Notes |
|---|---|---|
| name | name | not null |
| groupNumber | group_number | int |
| competitionRound | fk_comp_round | n:1, required |

### CompetitionTeam — `competition_team`
Join entity: which `Team` takes part in which `Competition`.
| Field | Column | Notes |
|---|---|---|
| competition | fk_comp | n:1, required |
| team | fk_team | n:1, required |

### Team — `team`
A club or national team, reusable across competitions.
| Field | Column | Notes |
|---|---|---|
| name | name | not null, **unique** |
| acronym | acronym | not null, **unique** |
| hasClub | has_club | club vs. national team |

### Spieltag — `spieltag` (matchday)
| Field | Column | Notes |
|---|---|---|
| spieltagNumber | spieltag_number | int |
| startDate | start_date | `LocalDateTime`, nullable |
| competitionRound | fk_comp_round_id | n:1, required |
| spiele | — | 1:n `Spiel` |
| tippConfigs | — | 1:n `TippConfig` |

### Spiel — `spiel` (match)
| Field | Column | Notes |
|---|---|---|
| spielNumber | spiel_number | int |
| anpfiffDate | anpfiffdate | `LocalDateTime` kick-off, nullable |
| heimTore / gastTore | heim_tore / gast_tore | int goals, not null |
| stattgefunden | stattgefunden | boolean, match has been played |
| heimTeam / gastTeam | fk_heim_team_id / fk_gast_team_id | n:1 `Team`, nullable |
| spieltag | fk_spieltag_id | n:1, nullable |
| competitionGroup | competition_group_id | n:1, nullable (only for group rounds) |
| tipps | — | 1:n `Tipp` |
| spielFormulas | — | 1:n `SpielFormula` |

### SpielFormula — `spiel_formula`
Per-team result record of a Spiel used for standings (one row per team).
| Field | Column | Notes |
|---|---|---|
| spiel | fk_spiel_id | n:1, required |
| teamName | team_name | not null |
| teamNameAcronym | team_name_acronym | nullable |
| heimTeam | is_heim_team | boolean |
| heimTore, gastTore, diffTore | heim_tore, gast_tore, diff_tore | int |
| won, remis, lost, points | | int |

## Users, Communities, Memberships

### Tipper — `tipper` (user)
| Field | Column | Notes |
|---|---|---|
| username | username | not null, **unique** |
| firstname, lastname | | not null |
| email | | nullable |
| passwort | passwort | not null — never expose in DTOs |
| passwortHint | passwort_hint | nullable |
| communityAdmin | is_community_admin | boolean |
| competitionAdmin | is_competition_admin | boolean |
| defaultCommunityId | default_community_id | plain `Long`, **no FK/association** |
| defaultCompetitionId | default_competition_id | plain `Long`, **no FK/association** |
| tipperRoles | — | 1:n `TipperRole` |
| communityMemberships | — | 1:n `CommunityMembership` |
| — | — | no `createdOn` |

### Community — `community`
A betting group of Tippers.
| Field | Column | Notes |
|---|---|---|
| name, description | | nullable |
| communityMemberships | — | 1:n `CommunityMembership`, cascade |
| competitionMemberships | — | 1:n `CompetitionMembership`, cascade |
| tippModi | — | 1:n `TippModus` (polymorphic), cascade |
| roles | — | 1:n `CommunityRole`, cascade |

Deleting a Community removes its memberships, Tipps, TippConfigs, TippModi and CommunityRoles. `Tipper.defaultCommunityId` is not cleared, because it is a plain id.

### CommunityMembership — `community_membership`
A Tipper's membership in a Community; owns that Tipper's Tipps.
| Field | Column | Notes |
|---|---|---|
| community | fk_comm_id | n:1, required |
| tipper | fk_tipper_id | n:1, required |
| tipps | — | 1:n `Tipp` |

### CompetitionMembership — `competition_membership`
A Community taking part in a Competition.
| Field | Column | Notes |
|---|---|---|
| community | fk_comm_id | n:1, required |
| competition | fk_comp_id | n:1, required |
| tippConfigs | — | 1:n `TippConfig` |

## Roles (single-table inheritance)

### Role — `role` (abstract)
`@Inheritance(SINGLE_TABLE)`, discriminator column `role_types`.
| Field | Column | Notes |
|---|---|---|
| name, description | | nullable |
| tipperRoles | — | 1:n `TipperRole` |

Subclasses (same table):
- **CommunityRole** — discriminator `COMM`, `community` → `fk_comm_id` (nullable at DB level, required semantically).
- **CompetitionRole** — discriminator `COMP`, `competition` → `fk_comp_id` (nullable at DB level, required semantically).

### TipperRole — `tipper_role`
Join entity Tipper ↔ Role.
| Field | Column | Notes |
|---|---|---|
| tipper | tipper_id | n:1, required |
| role | rolle_id | n:1, required |

## Betting

### TippModus (abstract, table-per-class)
Scoring mode defined by a Community. `@Inheritance(TABLE_PER_CLASS)` — no base table.
- Id is generated via `@TableGenerator` on the shared `hibernate_sequences` table (`sequence_name = 'default'`, `allocationSize = 1`) so ids are **unique across all three subtables**. Do not change to IDENTITY.

| Field | Column | Notes |
|---|---|---|
| name | name | not null; unique per community (`name`, `fk_comm_id`) in each subtable |
| type | type | `org.ebi.sportbets.enums.TippModusType`, stored via `TippModusTypeConverter` |
| deadline | deadline | `Integer`, nullable |
| community | fk_comm_id | n:1, required |
| tipps | — | 1:n `Tipp`, cascade |
| tippConfigs | — | 1:n `TippConfig`, cascade |

Subclasses:
| Entity | Table | Extra fields | Expected `type` |
|---|---|---|---|
| TippModusToto | tipp_modus_toto | — | `TIPPMODUS_TOTO` |
| TippModusPoint | tipp_modus_point | `totalPoints` (total_points) | `TIPPMODUS_POINT` |
| TippModusResult | tipp_modus_result | `tendencyPoints` (tendency_points), `bonusPoints` (bonus_points) | `TIPPMODUS_RESULT` |

### TippModusType (enum, `enums` package) + TippModusTypeConverter
- Constants: `TIPPMODUS_TOTO("TotoTipp")`, `TIPPMODUS_POINT("PointTipp")`, `TIPPMODUS_RESULT("ResultTipp")`.
- The DB column stores the **display name** (`TotoTipp` …), not the constant name.
- The converter maps `null` ↔ `null` and throws `IllegalArgumentException` on an unknown DB value (never silently maps to null).

### TippConfig — `tipp_config`
Which TippModus a CompetitionMembership uses on a given Spieltag.
| Field | Column | Notes |
|---|---|---|
| competitionMembership | fk_comp_memb | n:1, required |
| spieltag | fk_matchday | n:1, required |
| tippModus | fk_tipp_modus | n:1 polymorphic, required, **no FK constraint** (`ConstraintMode.NO_CONSTRAINT`) |

### Tipp — `tipp` (bet)
A Tipper's (via CommunityMembership) prediction for a Spiel.
| Field | Column | Notes |
|---|---|---|
| communityMembership | fk_comm_memb_id | n:1, required |
| spiel | fk_spiel_id | n:1, required |
| tippModus | fk_tipp_modus_id | n:1 polymorphic, required, **no FK constraint** |
| heimTipp / gastTipp | heim_tipp / gast_tipp | `Integer`, nullable (predicted goals) |
| remisTipp | remis_tipp | `Integer`, nullable (draw/toto prediction) |
| winPoints | win_points | `Integer`, nullable — points awarded after evaluation |

## Rules When Working With Entities
- Never return entities from controllers; map to DTOs in the service layer.
- To delete a child, remove it from the parent's collection (orphanRemoval) rather than calling the child repository directly when the parent is loaded.
- Set both sides of a bidirectional association when adding children (e.g. `round.setCompetition(c); c.getRounds().add(round);`).
- Polymorphic `tippModus` references (Tipp, TippConfig) are not DB-enforced — validate existence of the TippModus in the service layer.
- `Tipper.defaultCommunityId` / `defaultCompetitionId` are not DB-enforced — validate in the service layer.
