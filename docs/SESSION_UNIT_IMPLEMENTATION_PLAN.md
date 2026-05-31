# Collection Batches & Repeatable Collection Workflow — Mobile Implementation Plan

> **Naming note**: The user-facing feature is called **Collection Batch** throughout. The underlying Room entity, domain model, repository, DTO, mapper, and DAO retain the existing **`SessionUnit*`** names (matching the backend SRS and the table name `session_unit`). This is deliberate: presentation talks "batch", data layer stays "session unit". The two refer to the same thing.

> Scope: Android mobile app only. No backend, no web. No conflict-resolution / discrepancy logic.
> Status: in progress — see Implementation Status below.

---

## Implementation Status (read this first)

This section is the single source of truth for "where are we?". It is updated at the end of every PR. If you are picking this up cold, read this whole section, then jump straight to the **Next up** PR.

### Progress

| PR  | Description                                            | Status        |
| --- | ------------------------------------------------------ | ------------- |
| PR 1 | Schema & domain plumbing                              | ✅ Merged     |
| PR 2 | `CollectionMethodWorkflow` strategy + `IntakeViewModel` refactor | ✅ Merged     |
| Pre-PR 3 | Dead-code cleanup across core repos / DAOs / composites | ✅ Merged     |
| PR 3 | `collection_batch/` feature (UI + VMs) + nav + Imaging scoping — **sliced into 3a–3d** | ⏭️ In progress |
| └ PR 3a | Destination contract + nav scaffold + strategy flip | ✅ Merged     |
| └ PR 3b | `CollectionBatchList` screen + VM                   | ✅ Merged     |
| └ PR 3c | `CollectionBatchForm` screen + VM + validation + saving | ✅ Merged     |
| └ PR 3d | Edit-mode hydration + reactive banner + identity-resolver + delete + submit-dialog + Imaging scoping | ⏭️ In progress |
| PR 4 | Retire legacy `hour_log/` + `add_hour/`               | ⏳ Pending    |
| PR 5 | Sync wiring (DTOs, RemoteSessionUnitDataSource, MetadataUploadWorker, `sessionUnitId` plumbing in FormAnswer/Specimen DTOs) | ⏳ Pending |
| PR 6 | Lock collection method when units exist               | ⏳ Pending    |

### PR 1 — Schema & domain plumbing (✅ Merged)

**What landed.** The data and domain layers can now represent collection batches (session units) and answers/specimens scoped to them. No UI or behavior changed; existing PSC/LTC/HLC flows compile and run identically.

**Files added** (all under `app/src/main/java/com/vci/vectorcamapp/`):

```
core/data/room/entities/SessionUnitEntity.kt
core/data/room/entities/relations/SessionUnitWithFormAnswersRelation.kt
core/data/room/entities/relations/SessionUnitWithSpecimensRelation.kt
core/data/room/dao/SessionUnitDao.kt
core/data/room/migrations/versions/Migration_30_31_AddSessionUnitTable.kt
core/data/room/converters/FormQuestionScopeConverter.kt
core/data/mappers/SessionUnitMapper.kt
core/domain/model/SessionUnit.kt
core/domain/model/enums/FormQuestionScope.kt
core/domain/model/composites/SessionUnitWithFormAnswers.kt
core/domain/model/composites/SessionUnitWithSpecimens.kt
core/domain/repository/SessionUnitRepository.kt
core/data/repository/SessionUnitRepositoryImplementation.kt
```

**Files modified.**

- `core/data/room/VectorCamDatabase.kt` — DB version `30 → 31`, added `SessionUnitEntity::class` to `@Database.entities`, added `FormQuestionScopeConverter::class` to `@TypeConverters`, added `abstract val sessionUnitDao: SessionUnitDao`.
- `core/data/room/migrations/Migrations.kt` — imported and registered `MIGRATION_30_31_ADD_SESSION_UNIT_TABLE`.
- `core/data/room/entities/FormQuestionEntity.kt` — added `answerScope: FormQuestionScope` (default `SESSION`) and `isUnitIdentityComponent: Boolean` (default `false`).
- `core/data/room/entities/FormAnswerEntity.kt` — added nullable `sessionUnitId: UUID?` with FK to `session_unit.localId` and matching index.
- `core/data/room/entities/SpecimenEntity.kt` — added nullable `sessionUnitId: UUID?` with FK to `session_unit.localId` and matching index.
- `core/data/mappers/FormQuestionMapper.kt` — propagates `answerScope` + `isUnitIdentityComponent` in both directions; DTO→domain hard-codes `SESSION` / `false` with a `// TODO` (DTO plumbing lands in PR 5).
- `core/data/mappers/FormAnswerMapper.kt` — `toEntity(sessionId, sessionUnitId, questionId)` (new arg in the middle).
- `core/data/mappers/SpecimenMapper.kt` — `toEntity(sessionId, sessionUnitId)` (new arg appended).
- `core/data/repository/FormAnswerRepositoryImplementation.kt` — passes `sessionUnitId = null` into the mapper. Interface `FormAnswerRepository.upsertFormAnswer(...)` is **unchanged**.
- `core/data/repository/SpecimenRepositoryImplementation.kt` — passes `sessionUnitId = null` for all `insert/update/delete` calls.
- `core/di/RoomDatabaseModule.kt` — added `provideSessionUnitDao`.
- `core/di/CoreRepositoryModule.kt` — added `bindSessionUnitRepository`.

**Deviations from the original plan** (intentional, all consistent with existing codebase conventions):

1. **`answerScope` modeled as an enum, not a `String`.** The plan said keep it as `String` to match `FormQuestion.type`. We introduced `FormQuestionScope { SESSION, SESSION_UNIT }` plus a `FormQuestionScopeConverter`, mirroring the existing `SessionType` / `SessionTypeConverter` pattern. The migration column is still `TEXT NOT NULL DEFAULT 'SESSION'` because the converter writes `enum.name`. No `AnswerScopes` constants object was created.
2. **Domain `SessionUnit` does not carry `sessionId`.** It carries only `localId`, `remoteId`, `unitOrder`, `createdAt`. The session FK is supplied at the mapper boundary via `SessionUnit.toEntity(sessionId: UUID)`. This matches the existing `Session` model (no `siteId`) and `Specimen` model. Callers that need to persist a unit must therefore have `sessionId` in scope (typically from `CurrentSessionCache`).
3. **Domain `FormAnswer` and `Specimen` were NOT extended with `sessionUnitId`.** Same reasoning as above — FK columns are passed in at the mapper boundary, not stored on the domain model. `FormAnswerEntity.sessionUnitId` is still threaded through via `FormAnswer.toEntity(sessionId, sessionUnitId, questionId)`.
4. **`FormAnswerRepository` interface signature unchanged.** The plan suggested adding a `sessionUnitId: UUID? = null` parameter to `upsertFormAnswer(...)`. We deferred this — for PR 1 the impl simply passes `null`. When PR 3 (`CollectionBatchFormViewModel`) needs to persist unit-scoped answers, we'll add the parameter then. Existing callers (`IntakeViewModel`, `MetadataUploadWorker.syncFormAnswersIfNeeded`) remain untouched.
5. **`SessionUnitDao` is intentionally lean.** Only `upsertSessionUnit`, `deleteSessionUnit`, `getSessionUnitById`, `getSessionUnitWithFormAnswers`. The plan listed more methods (`observeSessionUnitsForSession`, `getMaxUnitOrderForSession`, `countSessionUnitsForSession`, `countSpecimensForUnit`, plural `getSessionUnitsForSession`, etc.). These will be added as needed in PR 3 when the list/form screens require them — see §3.8 / §4.3 for the full target shape.
6. **`SessionUnitRepository` interface mirrors the lean DAO.** Currently just `upsertSessionUnit(sessionUnit, sessionId)`, `deleteSessionUnit(sessionUnit, sessionId)`, `getSessionUnitById`, `getSessionUnitWithFormAnswers`. The richer interface in §4.3 of this plan is the eventual target.
7. **Relation file naming.** `SessionUnitWithAnswersRelation` → `SessionUnitWithFormAnswersRelation` and corresponding composite `SessionUnitWithFormAnswers`. The plan used "Answers"; we kept the full "FormAnswers" prefix for consistency with `FormAnswerEntity` / `FormAnswerDao` naming throughout the codebase.
8. **Composite domain models.** Created under `core/domain/model/composites/` (`SessionUnitWithFormAnswers.kt`, `SessionUnitWithSpecimens.kt`) following the existing `SessionWithSpecimens` / `SessionAndSite` convention. The relation classes (Room layer) and composites (domain layer) are intentionally separate.
9. **Migration filename.** `Migration_30_31_AddSessionUnitTable.kt` (singular, `Table`) instead of the plan's `AddSessionUnits.kt`. Constant: `MIGRATION_30_31_ADD_SESSION_UNIT_TABLE`. Inline-prose §3.6 has been updated to reflect this.
10. **Migration uses the full table-rebuild for `form_answer` and `specimen`.** The plan flagged this as required but printed the shorter `ALTER TABLE ... ADD COLUMN ... REFERENCES` snippet for brevity. The shipped migration does the proper rename+create+copy+drop for both tables (so FKs are actually enforced), patterned after `Migration_19_20_MakeHardwareIdColumnNullable.kt`.

**Known follow-ups baked into PR 1** (deliberately deferred, will be picked up by later PRs):

- `FormAnswerRepository.upsertFormAnswer` does not yet accept a `sessionUnitId` argument — PR 3.
- `SessionUnitDao` lacks `observe`, count, `getNext*`, and `getSessionUnitsForSession` queries — PR 3.
- `SessionUnitRepository.deleteSessionUnitIfNoSpecimens` is not yet implemented — PR 3.
- `FormQuestionMapper.toDomain(FormQuestionDto)` hard-codes `answerScope = SESSION` and `isUnitIdentityComponent = false` with a `// TODO: CHANGE THIS WITH ACTUAL DYNAMIC VALUES`. The DTO will gain these fields in PR 5.
- `SpecimenRepositoryImplementation` hard-codes `sessionUnitId = null` in all three CRUD methods. `ImagingViewModel` will eventually pass the real value (PR 3).
- `MetadataUploadWorker` has no awareness of session units yet — PR 5.

**How to verify PR 1 locally.**

- App builds and existing flows (intake → imaging → upload for PSC/LTC/HLC) work without regressions.
- Running an upgrade from a populated v30 database to v31 preserves all `form_answer` and `specimen` rows with `sessionUnitId = NULL`.
- `PRAGMA foreign_key_list('form_answer')` and `PRAGMA foreign_key_list('specimen')` after migration show the new FK to `session_unit(localId)`.

### PR 2 — `CollectionMethodWorkflow` strategy + `IntakeViewModel` refactor (✅ Merged)

**What landed.** Pure refactor — no schema work, no new UI. The hardcoded HLC `if/else` in `IntakeViewModel.SubmitIntakeForm` is replaced by a new `CollectionMethodWorkflow` strategy resolved through `CollectionMethodWorkflowFactory`. End-to-end behavior is unchanged: HLC still navigates to `Destination.HourLog(sessionId)`, everything else still navigates to `Destination.Imaging`. PR 3 will swap those two destinations inside the concretes without touching the call site.

**Files added** (all under `app/src/main/java/com/vci/vectorcamapp/`):

```
intake/domain/strategy/collection_method/CollectionMethodWorkflow.kt
intake/domain/strategy/collection_method/CollectionMethodWorkflowFactory.kt
intake/domain/strategy/collection_method/concrete/SingleBatchWorkflow.kt
intake/domain/strategy/collection_method/concrete/MultipleBatchWorkflow.kt
```

**Files moved** (sibling reorg, scope-creep cleanup):

```
intake/domain/strategy/ProgramFormWorkflow.kt
  → intake/domain/strategy/program_form/ProgramFormWorkflow.kt
intake/domain/strategy/ProgramFormWorkflowFactory.kt
  → intake/domain/strategy/program_form/ProgramFormWorkflowFactory.kt
intake/domain/strategy/concrete/FormPresentWorkflow.kt
  → intake/domain/strategy/program_form/concrete/FormPresentWorkflow.kt
intake/domain/strategy/concrete/ProgramFormAbsentWorkflow.kt
  → intake/domain/strategy/program_form/concrete/ProgramFormAbsentWorkflow.kt
intake/domain/strategy/concrete/SurveillanceFormPresentWorkflow.kt
  → intake/domain/strategy/program_form/concrete/SurveillanceFormPresentWorkflow.kt
```

**Files modified.**

- `intake/presentation/IntakeViewModel.kt` — added `@Inject lateinit var collectionMethodWorkflowFactory` + `private lateinit var collectionMethodWorkflow` fields alongside the existing `programFormWorkflow*` pair. Replaced the HLC `if/else` (previously lines 309-314) with `collectionMethodWorkflow = collectionMethodWorkflowFactory.create(session.localId, session.collectionMethod); _events.send(IntakeEvent.NavigateAfterIntake(collectionMethodWorkflow.postIntakeDestination))`. Removed the now-unused `CollectionMethodOption` import. Updated `ProgramFormWorkflow*` imports to point at the new `program_form/` subpackage.
- `intake/presentation/IntakeEvent.kt` — removed `NavigateToHourLogScreen` and `NavigateToImagingScreen`; added `data class NavigateAfterIntake(val destination: Destination) : IntakeEvent`.
- `navigation/NavGraph.kt` — collapsed the two-branch `composable<Destination.Intake>` event handler into a single `is IntakeEvent.NavigateAfterIntake -> navController.navigate(event.destination)` branch.

**Deviations from the original plan** (intentional, all consistent with existing codebase conventions):

1. **Per-family folder layout under `strategy/`.** The plan put new files flat in `intake/domain/strategy/` (alongside the existing `ProgramFormWorkflow.kt`) and shared a single `concrete/` subfolder. We introduced `intake/domain/strategy/collection_method/` for the new family and moved `ProgramFormWorkflow` and friends into a sibling `intake/domain/strategy/program_form/` folder. Reason: a flat layout would mix two unrelated strategy families inside `concrete/`. Per-family subfolders are self-contained and scale to PR 6 / future strategies cleanly.
2. **Property-style interface, not method-style.** The plan defined `interface CollectionMethodWorkflow { fun postIntakeDestination(sessionId: String): Destination }`. We changed it to `val postIntakeDestination: Destination`. Reason: the per-call input (`sessionId`) is bound at construction time by the factory — same pattern as `ProgramFormWorkflow` (which exposes `val form`, `val formQuestions` etc., all bound in `ProgramFormWorkflowFactory.create(...)`). This eliminates the "unused parameter" smell in `SingleBatchWorkflow`, which doesn't need a `sessionId` because `Destination.Imaging` is a `data object`.
3. **Factory takes `UUID` for `sessionId`, not `String`.** The plan used `String` (matching `Destination.HourLog`'s `sessionId: String`). We took `UUID` — the canonical form everywhere except at the literal `Destination(...)` constructor call. This aligns with the `complete_session/` pattern (`CompleteSessionListEvent.NavigateToCompleteSessionDetails(val sessionId: UUID)`, `CompleteSessionDetailsViewModel` parsing back to `UUID` immediately) rather than the doomed `hour_log/` pattern (`sessionId: String` flowing through Action → Event → State). The `.toString()` is confined to one line inside `MultipleBatchWorkflow`.
4. **Concrete class naming.** The plan named the non-HLC concrete `DirectImagingWorkflow` and the HLC concrete `RepeatableUnitWorkflow`. We renamed them `SingleBatchWorkflow` and `MultipleBatchWorkflow` respectively. Reason: the user-facing concept is "collection batch" (per the naming note at the top of this document). Framing the two concretes as "single batch" vs "multiple batches" keeps the strategy aligned with that vocabulary and reads symmetrically at the factory call site.
5. **Event renamed `NavigateAfterIntake` (vs `IntakeEvent.NavigateAfterIntake` per plan).** Matches the plan's name; included here only because it's the one fully-new event shape replacing two old ones.

**Known follow-ups baked into PR 2** (deliberately deferred, will be picked up by later PRs):

- `MultipleBatchWorkflow.postIntakeDestination` still resolves to `Destination.HourLog(sessionId.toString())`. PR 3 will swap this for `Destination.CollectionBatchList(sessionId.toString())` — a one-line change inside the concrete.
- `SingleBatchWorkflow.postIntakeDestination` still resolves to the parameterless `Destination.Imaging` (data object). PR 3 will flip `Destination.Imaging` to `data class Imaging(val sessionUnitId: String? = null)` and update this concrete to `Destination.Imaging()` (or `Destination.Imaging(sessionUnitId = null)`).
- `IntakeState.isCollectionMethodLocked` is not introduced — that's PR 6.
- The legacy `HourLog` / `AddHour` destinations and their `composable<...>` blocks in `NavGraph.kt` are untouched — retirement is PR 4.

**How to verify PR 2 locally.**

- App builds; Hilt graph resolves with no new module needed (`CollectionMethodWorkflowFactory` uses a plain `@Inject constructor()` like `ProgramFormWorkflowFactory`).
- Grep across the project for `IntakeEvent.NavigateToHourLogScreen` and `IntakeEvent.NavigateToImagingScreen` returns zero hits.
- **PSC / LTC / OTHER**: Intake → Save → lands on the existing `Imaging` screen. Identical to pre-PR behavior.
- **HLC**: Intake → Save → lands on the existing `HourLog` screen. Identical to pre-PR behavior.

### Pre-PR 3 — Dead-code cleanup (✅ Merged)

**Purpose.** Before starting PR 3, the core data + domain layer was swept for unused functions, composites, and Room relations. The motivation is directly tied to PR 3: this feature is the first real consumer of `SessionUnit*` and related repository plumbing, and we did **not** want PR 3 starting from a layer that still carried speculative methods from prior PRs.

**What was removed.** Repository / DAO / composite / relation members with zero call sites at the start of PR 3:

- **Repository methods**: `FormRepository.observeFormsByProgramId`, `SessionRepository.getSessionWithSpecimensById`, `SessionRepository.observeSessionWithSpecimens`, every `SessionUnitRepository` method, `SiteRepository.getSiteById`, `SpecimenRepository.deleteSpecimen`, `SpecimenImageRepository.deleteSpecimenImage`.
- **DAO methods (same names as above, plus)**: `SessionDao.getSessionWithSessionUnits`, every `SessionUnitDao` method.
- **Composite domain models (entire files)**: `SessionWithSpecimens`, `SessionWithSessionUnits`, `SessionWithFormAnswers`, `SessionUnitWithFormAnswers`, `SessionUnitWithSpecimens`.
- **Room relations (entire files)**: `SessionWithSpecimensRelation`, `SessionWithSessionUnitsRelation`, `SessionWithFormAnswersRelation`, `SessionUnitWithFormAnswersRelation`, `SessionUnitWithSpecimensRelation`.

**What was kept (deliberately empty).** `SessionUnitRepository`, `SessionUnitRepositoryImplementation`, and `SessionUnitDao` files remain on disk as empty interfaces/classes, plus their Hilt + Room wiring. PR 3 will populate them one method at a time.

**Implication for PR 3 (read this carefully).** The detailed function lists in §3.8 (`SessionUnitDao`) and §4.3 (`SessionUnitRepository`) of this plan are **stale**. Treat both contracts as empty at the start of PR 3 and **disregard those sections as a checklist**. They remain in the document only as background context.

**Known pre-existing test breakage (not caused by this cleanup).** `testColombiaDebugUnitTest` (and the equivalent flavor variants) currently fail to compile due to PR 1 fixture drift — specifically references to `Site.name`, `Site.locationHierarchy`, `FormQuestion.answerScope`, and `FormQuestion.isUnitIdentityComponent` in test fixtures that no longer match the domain models. These failures exist on the branch prior to the Pre-PR 3 cleanup and were verified to be unrelated. They are not blockers for shipping the cleanup, but should be fixed before PR 3b/3c so VM-level tests in PR 3 land on a green baseline.

**Tooling note for the agent.** Do **not** run any `git` commands (including `stash`, `commit`, `diff` against other refs, etc.) without explicit permission from the maintainer. Verification of the cleanup must rely on `./gradlew assembleDebug`, repo-wide grep, and `read_lints` — never on stashing or otherwise mutating the working tree.

### PR 3a — Destination contract + nav scaffold + strategy flip (✅ Merged)

**What landed.** The navigation surface area for the `collection_batch/` feature is in place — `Destination.Imaging` is now scoped by `sessionUnitId`, and the two new `CollectionBatchList` / `CollectionBatchForm` destinations exist with stub composables. The HLC strategy concrete now points at `CollectionBatchList` instead of the legacy `HourLog`. No repository, DAO, viewmodel, or screen code was touched — `SessionUnitRepository`, `SessionUnitRepositoryImplementation`, and `SessionUnitDao` remain empty post Pre-PR 3 cleanup, in line with the "Read this before writing any code in PR 3" rule.

**Files modified** (all under `app/src/main/java/com/vci/vectorcamapp/`):

- `navigation/Destination.kt` — `Imaging` flipped from `data object` to `data class Imaging(val sessionUnitId: String?)`. Added `data class CollectionBatchList(val sessionId: String)` and `data class CollectionBatchForm(val sessionId: String, val sessionUnitId: String?)`. `HourLog` / `AddHour` left in place (retired in PR 4).
- `navigation/NavGraph.kt` — added empty `composable<Destination.CollectionBatchList>` and `composable<Destination.CollectionBatchForm>` stubs that render `BaseScaffold { SplashScreen() }`. Updated the two stray `Destination.Imaging` call sites inside the legacy `HourLog` / `AddHour` blocks to `Destination.Imaging(sessionUnitId = null)`. The `composable<Destination.Imaging>` block itself is unchanged (uses `Destination.Imaging` only as a type parameter).
- `intake/domain/strategy/collection_method/concrete/SingleBatchWorkflow.kt` — `postIntakeDestination` → `Destination.Imaging(sessionUnitId = null)`.
- `intake/domain/strategy/collection_method/concrete/MultipleBatchWorkflow.kt` — `postIntakeDestination` → `Destination.CollectionBatchList(sessionId = sessionId.toString())`. No longer references `Destination.HourLog`.

**Deviations from the original plan** (intentional, all consistent with existing codebase conventions):

1. **No default values on the new `Destination` constructor params.** The plan called for `Imaging(val sessionUnitId: String? = null)` and `CollectionBatchForm(val sessionId: String, val unitId: String? = null)`. We dropped the defaults so every call site is explicit about whether it's session-scoped or unit-scoped. This matches the rest of `Destination.kt` (`HourLog(sessionId: String)`, `AddHour(sessionId: String)`, `CompleteSessionDetails(sessionId: String)` — none of which use defaults) and forces the `sessionUnitId = null` channel to be visible at every legacy call site.
2. **`CollectionBatchForm.sessionUnitId`, not `CollectionBatchForm.unitId`.** The plan's §9.1 used `unitId: String?`. We renamed it to `sessionUnitId` so the field name matches `Imaging.sessionUnitId` and the underlying `form_answer.sessionUnitId` / `specimen.sessionUnitId` columns. PR 3c will read it back via `savedStateHandle.toRoute<Destination.CollectionBatchForm>().sessionUnitId`; downstream events / state in 3b–3c should mirror this name (e.g. `CollectionBatchListEvent.NavigateToCollectionBatchForm(sessionId, sessionUnitId)`).
3. **Stubs render a `SplashScreen` placeholder, not a redirect to legacy `HourLog`.** The plan offered both options. We picked the placeholder because (a) it makes the stubbed state visible during 3a-only smoke tests, (b) it doesn't bake a now-unreachable legacy code path into the nav graph, and (c) it matches the loading-branch shape (`BaseScaffold { SplashScreen() }`) already used inside the `Imaging` and `Intake` composables — reviewers immediately recognize it as scaffolding.

**Known follow-ups baked into PR 3a** (deliberately deferred to later slices):

- `composable<Destination.CollectionBatchList>` and `composable<Destination.CollectionBatchForm>` bodies are placeholders — PR 3b / 3c replace them with real `hiltViewModel<...>()` + `ObserveAsEvents` + screen wiring.
- `ImagingViewModel` does not yet read `sessionUnitId` from the route — that's PR 3d. Until then, `Destination.Imaging(sessionUnitId = null)` is the only value flowing in (from `SingleBatchWorkflow` and the legacy `HourLog` / `AddHour` blocks), so behavior is identical to pre-3a.
- Legacy `HourLog` / `AddHour` `composable<>` blocks and the `Destination.HourLog` / `Destination.AddHour` entries are still in place — retirement is PR 4.

**How to verify PR 3a locally.**

- App builds (`./gradlew assembleDebug`); no Hilt graph changes required.
- Repo-wide grep for `Destination\.Imaging` in `app/src` returns only four hits: the type-position reference in `composable<Destination.Imaging>`, the two updated legacy call sites, and the `SingleBatchWorkflow` flip.
- **PSC / LTC / OTHER**: Intake → Save → lands on the existing `ImagingScreen` (unchanged behavior, arriving via `Destination.Imaging(sessionUnitId = null)`).
- **HLC**: Intake → Save → lands on the `CollectionBatchList` stub (a `SplashScreen`). This is the intentional 3a end-state; PR 3b fills the body.

### PR 3b — `CollectionBatchList` screen + VM (✅ Merged)

**What landed.** The HLC post-intake flow now lands on a real `CollectionBatchListScreen` instead of the PR 3a `SplashScreen` stub. The screen shows one card per `session_unit` row for the current session, with the specimen count per batch. A FAB navigates to the (still-stubbed) `CollectionBatchForm`; the cloud-upload icon submits the session via the existing worker chain. `SessionUnitRepository` / `SessionUnitDao` grew by exactly the two methods this VM consumes — nothing speculative.

**Files added** (all under `app/src/main/java/com/vci/vectorcamapp/`):

```
collection_batch/list/presentation/CollectionBatchListScreen.kt
collection_batch/list/presentation/CollectionBatchListViewModel.kt
collection_batch/list/presentation/CollectionBatchListState.kt
collection_batch/list/presentation/CollectionBatchListAction.kt
collection_batch/list/presentation/CollectionBatchListEvent.kt
collection_batch/list/presentation/components/CollectionBatchCard.kt
```

**Files modified.**

- `core/data/room/dao/SessionUnitDao.kt` — added `observeSessionUnitsForSession(sessionId): Flow<List<SessionUnitEntity>>` (ordered by `unitOrder ASC`) and `countSpecimensForSessionUnit(sessionUnitId): Int`. Still the only two methods on the DAO — everything else stays empty until PR 3c needs it.
- `core/domain/repository/SessionUnitRepository.kt` — mirrored: `observeSessionUnitsForSession(...)` and `countSpecimensForSessionUnit(...)`. No `Result<>` wrappers (these are reads, matching the existing `SpecimenRepository.observeSpecimensBySession` / `SessionRepository.getImageUrisBySessionId` shape).
- `core/data/repository/SessionUnitRepositoryImplementation.kt` — entity→domain mapping done in impl via `it.toDomain()`, never in the DAO.
- `navigation/NavGraph.kt` — replaced the `composable<Destination.CollectionBatchList>` `SplashScreen` stub with the real wire-up: `hiltViewModel<CollectionBatchListViewModel>()`, `ObserveAsEvents`, and `BaseScaffold { when (state.isLoading) { ... } }`. Imports for the new screen/event/VM added.

**Deviations from the original plan** (intentional, all consistent with existing codebase conventions and the "Read this before…" callout):

1. **Imaging is reachable ONLY through the form, not directly from the list.** The plan's §2 user flow showed two card affordances on the list — "tap arrow → imaging" and "tap body → form (edit mode)". We collapsed these into a single card click that always routes to the form. The form will end its happy path on `Destination.Imaging(sessionUnitId)`, so the form becomes the one and only door into imaging. Rationale: a single chokepoint into imaging keeps batch-identity validation in one place (the form) and avoids a "resume" path that would skip validation for an edited identity.
2. **Single `OpenCollectionBatch` action, not separate `OpenCollectionBatchImaging` + `EditCollectionBatch`.** Direct consequence of #1. The plan listed three card actions; we ship one.
3. **No `DeleteCollectionBatch` action / no `canDelete` flag on state / no `deleteSessionUnitIfNoSpecimens` repo method.** Per the slicing rule, delete has no UI dispatcher in this diff, so the action / event / repo method are all deferred. PR 3d revisits.
4. **No intermediate `CollectionBatchCardData`.** State carries `units: List<SessionUnit>` (canonical domain model) plus `specimenCountsBySessionUnitId: Map<UUID, Int>` for the only piece of side-data that isn't on `SessionUnit`. Card composables consume `SessionUnit` directly. Mirrors `CompleteSessionListViewModel.Map<SessionAndSite, SessionUploadProgress>` rather than inventing a presentation-layer wrapper.
5. **Bucket name is `"Batch ${unit.unitOrder}"` for now.** No identity resolver wired yet — that's PR 3c. The card title falls back cleanly when identity components don't exist, which is also what the plan's §7.3 prescribed as the fallback.
6. **`UUID` everywhere except at the `Destination(...)` constructor.** `Action` / `Event` / VM internals all carry `UUID`. The single `String ↔ UUID` conversion lives on the `NavGraph` line that builds `Destination.CollectionBatchForm(event.sessionId.toString(), event.sessionUnitId?.toString())`. Matches PR 2 deviation #3 and the `CompleteSessionListEvent.NavigateToCompleteSessionDetails` precedent. Flipping `Destination` itself to typed `UUID` requires a `NavType<UUID>` + `typeMap` refactor across all 6 existing routes — deferred to a future "type-safe UUID nav" PR, not folded into 3b.
7. **State pipeline shape matches `IntakeViewModel` / `IncompleteSessionViewModel` precisely.** Source flow → `stateIn(WhileSubscribed(), empty)` → `_state: MutableStateFlow` → `combine(...) { state.copy(...) }` → outer `stateIn(WhileSubscribed(5000L), initial)`. The `_state` MSF is kept (currently unused for mutation) so future UI-state additions in 3c/3d don't have to re-plumb the pipeline. The suspending `countSpecimensForSessionUnit` call lives inside the `combine` lambda — same pattern as `CompleteSessionListViewModel`'s `getTotalCountForSession`.
8. **`SubmitSession` action, not `UploadSession`.** Renamed for naming parity with `ImagingAction.SubmitSession`. The body is a verbatim transliteration of `ImagingViewModel.SubmitSession` (null-guard on cache → `markSessionAsComplete` → `enqueueSessionUpload` → `clearSession` → pop to Landing). Same worker chain, same idempotency guarantees.
9. **`+` is a `FloatingActionButton`, not a `ScreenHeader` leading icon.** Mirrors the `BottomEnd`-aligned FAB pattern in `CompleteSessionListScreen`. The cloud-upload submit stays as a `ScreenHeader` trailing icon — submit is a terminal/destructive action that belongs in chrome, not a primary CTA. Empty-state copy reads "Tap the + button below to add one."
10. **No back-arrow `leadingIcon` and no `ReturnToLandingScreen` / `ReturnToIntakeScreen` action.** Android system back fires `popBackStack()` automatically, which returns the user to Intake (one frame up). The `SubmitSession` happy path covers the Landing exit. Adding a visible back-arrow would create a third entry point for already-covered transitions; revisit if UX feedback says otherwise.
11. **`CollectionBatchCard` uses single `onClick` and only renders `createdAt`** (not "Last Updated"). `SessionUnit` doesn't model an updated timestamp; if PR 3c adds one, the card grows then. Visual structure otherwise carries 1:1 from `HourSessionCard`.

**Known follow-ups baked into PR 3b** (deliberately deferred):

- The `composable<Destination.CollectionBatchForm>` block in `NavGraph.kt` is still a `BaseScaffold { SplashScreen() }` stub — PR 3c.
- `CollectionBatchListEvent.NavigateToCollectionBatchForm` is the only forward navigation; until 3c lands, tapping the FAB or any card sends the user to the form stub.
- `SessionUnitDao` is still only the two methods listed above. The form-side methods (`getMaxUnitOrderForSession`, `getSessionUnitById`, `upsertSessionUnit`, plus any duplicate-identity lookup the form needs) come in 3c when their call sites exist.
- `FormAnswerRepository.upsertFormAnswer` is still unchanged from PR 1 — the `sessionUnitId` parameter lands in 3c.
- Delete affordance / `deleteSessionUnitIfNoSpecimens` / `canDelete` flag on the card — PR 3d.
- Type-safe `UUID` nav arguments — separate future PR (likely after PR 4 so the refactor surface is half the size).

**How to verify PR 3b locally.**

- `./gradlew assembleDebug` is clean. No Hilt module changes — `SessionUnitRepository` / `SessionRepository` / `WorkManagerRepository` / `CurrentSessionCache` were all bound pre-PR-3.
- **HLC**: Intake → Save → `CollectionBatchListScreen` with the empty-state message ("No collection batches yet. Tap the + button below to add one."). FAB navigates to the form stub (`SplashScreen` until 3c). Cloud-upload icon completes the session, enqueues `MetadataUploadWorker → ImageUploadWorker`, and pops to Landing. System back from the empty list returns to Intake.
- **PSC / LTC / OTHER**: untouched — `SingleBatchWorkflow → Destination.Imaging(sessionUnitId = null)` still works exactly as before.
- Repo-wide grep: `CollectionBatchListViewModel` → 2 hits (file + `NavGraph.kt`); `observeSessionUnitsForSession` → 3 hits (DAO + impl + VM); `countSpecimensForSessionUnit` → 3 hits (DAO + impl + VM).

### PR 3c — `CollectionBatchForm` screen + VM + validation + saving (✅ Merged)

**What landed.** Two slices, combined here for the historical record: an initial "view-only" slice (form scaffolding + scope split + cache cleanup) and a final "validation + saving" slice (validation use cases, transactional persist pipeline, error surfaces, navigation to imaging). Together: a user can open the form, fill it, hit per-question + cross-unit-identity validation inline + toast, and on success persist a `SessionUnit` + its `FormAnswer` rows transactionally before landing on Imaging.

**Implemented — Slice 1 (view-only):**

1. **Form-question scope read-path** — `FormQuestionDao.getQuestionsByFormId` was extended with an optional `answerScope: FormQuestionScope?` parameter using the `(:answerScope IS NULL OR answerScope = :answerScope)` predicate, and renamed to `getQuestionsByFormIdAndScope`. Repository interface + impl mirror the rename. Existing scope-blind callers (`MetadataUploadWorker`, `SettingsViewModel`, `RegistrationViewModel`) pass `null` and get every row back; behavior is identical for them.
2. **`ProgramFormWorkflowFactory`** now requests `FormQuestionScope.SESSION`. `IntakeViewModel` / `IntakeScreen` automatically see only SESSION-scoped questions with zero VM/screen changes.
3. **New `collection_batch/form/presentation/` package** — `CollectionBatchFormState` / `Action` / `Event` / `ViewModel` / `Screen` + local `components/CollectionBatchFormTile` (built on `InfoTile`, deliberately not reusing `IntakeTile`). State carries `formAnswersByQuestionId: Map<Int, FormAnswer>` (full `FormAnswer`, not `String`, to mirror `IntakeState`).
4. **`NavGraph.kt`** — the `composable<Destination.CollectionBatchForm>` stub from PR 3a is replaced with real wiring (`hiltViewModel<CollectionBatchFormViewModel>()` + `ObserveAsEvents` + `BaseScaffold { isLoading ? SplashScreen() : CollectionBatchFormScreen(...) }`).
5. **`DefaultIntakeFieldsCache.formAnswers` removed end-to-end.** Five files touched (DTO, interface, impl, `IntakeViewModel`, `SettingsViewModel`). DataStore migration is a no-op — `DefaultIntakeFieldsCacheDtoSerializer` already sets `ignoreUnknownKeys = true`.

**Implemented — Slice 2 (validation + saving):**

6. **Validation domain layer**, all under `collection_batch/domain/`:
   - `util/error/CollectionBatchFormError.kt` — `enum class CollectionBatchFormError : Error { FORM_INVALID, INVALID_FORM_ANSWER, DUPLICATE_IDENTITY, UNKNOWN_ERROR }`. Both top-level (toast) and per-field (inline) members live in one enum — convention-enforced split rather than Intake's two-enum (`IntakeError` + `FormValidationError`) split.
   - `use_cases/ValidateFormAnswersUseCase.kt` — feature-local Hilt-injectable use case, body duplicated from `intake.domain.use_cases.ValidateFormAnswersUseCase` with the `Result<Unit, FormValidationError>` return swapped for `Result<Unit, CollectionBatchFormError>`. Reuses `FormQuestionPrerequisiteEvaluator` (still in `intake.domain.util` — cross-feature leak flagged for promotion to `core.domain.util` separately).
   - `use_cases/ValidateCollectionBatchIdentityUseCase.kt` — structured per-questionId equality check. Filters by `answerScope == SESSION_UNIT && isUnitIdentityComponent`, `.trim()`s, treats any blank identity component as "skip" (returns `Success` so freshly-opened or partially-filled forms don't false-flag).
   - `use_cases/CollectionBatchFormValidationUseCases.kt` — aggregator `data class` bundling the two use cases. Single VM constructor param.
7. **Presentation error model** — `collection_batch/form/presentation/model/CollectionBatchFormErrors.kt`: `data class CollectionBatchFormErrors(val duplicateIdentity: CollectionBatchFormError? = null, val formAnswerErrors: Map<Int, CollectionBatchFormError?> = emptyMap())`. State carries `collectionBatchFormErrors: CollectionBatchFormErrors`. Mirrors `IntakeErrors` shape.
8. **New action + event**: `CollectionBatchFormAction.SubmitSessionUnitForm` + `CollectionBatchFormEvent.NavigateToImagingScreen(sessionUnitId: UUID)`. The event handler in `NavGraph` translates to `Destination.Imaging(sessionUnitId = event.sessionUnitId.toString())`.
9. **DAO additions** (only what the VM actually calls — Rule 1 of "Read this before…"):
   - `FormAnswerDao.getSessionUnitScopedFormAnswersBySessionId(sessionId): List<FormAnswerEntity>` — query: `SELECT * FROM form_answer WHERE sessionId = :sessionId AND sessionUnitId IS NOT NULL`. Lives on `FormAnswerDao` (not `SessionUnitDao`) per the new "filter-by-X belongs on the entity's own repo" convention — see Conventions below.
   - `SessionUnitDao.upsertSessionUnit(entity): Long`, `getSessionUnitById(sessionUnitId)`, `getMaxSessionUnitOrderForSession(sessionId): Int`.
10. **Repository layer**:
    - `FormAnswerRepository.upsertFormAnswer` widened — gained `sessionUnitId: UUID? = null` (per Rule 2, modify don't proliferate). Existing intake + worker callers updated to pass `null` explicitly rather than relying on the default.
    - `FormAnswerRepository.getSessionUnitScopedFormAnswersBySessionId(sessionId): Map<UUID, Map<Int, FormAnswer>>` — null-safe shape using `mapNotNull { entity.sessionUnitId?.let { it to entity } }` to narrow the nullable `sessionUnitId` without `!!`. Outer key is `UUID` (the `sessionUnitId`), inner map is `questionId → FormAnswer`.
    - `SessionUnitRepository` gained `upsertSessionUnit(sessionUnit, sessionId): Result<Unit, RoomDbError>`, `getSessionUnitById(sessionUnitId): SessionUnit?`, `getMaxSessionUnitOrderForSession(sessionId): Int`. Mirrors `SessionRepository`'s shape.
11. **ViewModel persist pipeline** — `CollectionBatchFormViewModel` now injects `sessionUnitRepository`, `formAnswerRepository`, `collectionBatchFormValidationUseCases`, and `@Inject lateinit var transactionHelper: TransactionHelper`. The `SubmitSessionUnitForm` branch runs `validateFormAnswers` + `validateCollectionBatchIdentity` against a fresh `getSessionUnitScopedFormAnswersBySessionId` read, writes both error fields into `collectionBatchFormErrors`, short-circuits on per-field errors (with a `FORM_INVALID` toast) or duplicate-identity (with a `DUPLICATE_IDENTITY` toast), and otherwise persists inside `transactionHelper.runAsTransaction { ... }`. Create vs. edit fork chooses between `UUID.randomUUID()` (create) and `sessionUnitRepository.getSessionUnitById(it) ?: SessionUnit(localId = sessionUnitId ?: ..., ...)` (edit, with defensive soft-recovery fallback — see Deviation #10).
12. **Screen wiring** — both `DynamicFormField` call sites pass `error = state.collectionBatchFormErrors.formAnswerErrors[question.id]`. A final `ActionButton(label = "Continue to Imaging")` item dispatches `SubmitSessionUnitForm`. Inside the Batch Identity tile's content lambda, a `DuplicateIdentityWarningBanner` renders when `state.collectionBatchFormErrors.duplicateIdentity != null` — feature-local component in `collection_batch/form/presentation/components/`, modeled on `PracticeSessionWarningBanner`.
13. **`ErrorExtensions.kt`** — added `is CollectionBatchFormError -> when (this) { ... }` branch resolving each member to its string resource.
14. **`strings.xml`** — four new strings: `collection_batch_form_error_form_invalid`, `collection_batch_form_error_invalid_form_answer`, `collection_batch_form_error_duplicate_identity`, `collection_batch_form_error_unknown_error`.
15. **Cross-feature call-site updates** — `IntakeViewModel.handleSaveIntake` and `MetadataUploadWorker.syncFormAnswersIfNeeded` now pass `sessionUnitId = null` explicitly to `upsertFormAnswer`. **Bonus fix shipped in the same diff**: `IntakeViewModel` no longer overwrites `FormAnswer.submittedAt` with `System.currentTimeMillis()` on local save — that was a pre-existing latent bug. `submittedAt` is the upload timestamp set by `RemoteFormAnswerDataSource`, never by the VM. See Convention #9 below.

**Deviations from plan worth recording.**

1. **`getQuestionsByFormId` was renamed to `getQuestionsByFormIdAndScope`, not just widened in place.** Plan §"Additional scope #2" prescribes widening while keeping the original name. Accepted after audit: still a single method with a single query body, no mapper duplication, no second contract — Rule 2's "no sibling" intent is preserved. Name is a preference, not a Rule 2 violation.
2. **`CollectionBatchFormState.formAnswersByQuestionId` is `Map<Int, FormAnswer>`, not `Map<Int, String>`.** The full-`FormAnswer` shape mirrors `IntakeState.formAnswersByQuestionId` so the persistence step was a smaller delta.
3. **No `BackHandler` on `CollectionBatchFormScreen`.** No draft persistence → system back and the back-icon tap are semantically identical. Add when/if `ReturnToCollectionBatchListScreen` grows side effects.
4. **No `CollectionBatchIdentityResolver` / `CollectionBatchIdentityValidator` standalone util files.** Plan §PR-3c-files-to-add listed both under `collection_batch/domain/util/`. Final implementation models identity validation as the Hilt-injectable `ValidateCollectionBatchIdentityUseCase` instead (mirrors `ValidateFormAnswersUseCase`'s shape). The bucket-name "resolver" (display formatter) was dropped — its only intended consumer is the list-card identity-label upgrade, which is itself deferred to 3d (Option B from the in-PR design discussion: leave `"Batch ${unit.unitOrder}"` as the card title for now, add the resolver when the consumer lands).
5. **Identity comparison is structured per-questionId equality, not bucket-name string match.** Plan §7.3 prescribed deriving a `" · "`-joined bucket name and comparing strings. Final implementation compares values per identity-questionId — eliminates separator/whitespace/ordering ambiguity. The bucket-name string remains a *display-only* concept (when it eventually lands in the list card).
6. **`ValidateFormAnswersUseCase` is duplicated feature-locally rather than reused from `intake.domain.use_cases`.** Cross-feature reach was deemed worse than a small duplicate. Future cleanup (out of 3c's scope): relocate to `core/domain/use_case/` and have both features import from there.
7. **Submit-session confirmation dialog (Additional scope #1) was NOT implemented in 3c.** Plan slated it for 3c, but it proved orthogonal to the form's validation+saving work. Punted to 3d alongside the delete affordance — the dialog primitive can be introduced once and used for both "end session" and "delete unit" confirmations.
8. **`CollectionBatchFormError` collapses both top-level and per-field errors into one enum.** Intake splits them across `IntakeError.FORM_INVALID` + `FormValidationError.INVALID_FORM_ANSWER`. Convention-enforced separation rather than type-enforced. Can split later if drift becomes a problem.
9. **`FormAnswerRepository.upsertFormAnswer` widened with default-null `sessionUnitId`, but existing callers were updated to pass `null` explicitly.** The default exists for Rule-2 compliance; the explicit-null call-site update is a new convention — see Convention #3 below.
10. **`getSessionUnitScopedFormAnswersBySessionId` lives on `FormAnswerRepository` / `FormAnswerDao`, not `SessionUnitRepository` / `SessionUnitDao`.** Earlier drafts placed it next to `countSpecimensForSessionUnit` on `SessionUnitDao`. After discussion, the new consistency principle ("filter-by-X belongs on the entity's own repo") moved it. Side effect: `countSpecimensForSessionUnit` on `SessionUnitDao` is now flagged as misplaced PR-3b expedient debt — should eventually live on a `SpecimenRepository` (which doesn't exist yet). Don't extend that precedent.
11. **`FormAnswer.submittedAt` is never written from the VM.** Plan didn't explicitly call this out; pre-existing `IntakeViewModel:278` bug was overwriting it with `System.currentTimeMillis()` on local save. Discovered during 3c review, fixed in `IntakeViewModel` as a bonus (no longer overwrites). `submittedAt` is set only by `RemoteFormAnswerDataSource` when the upload DTO is constructed. See Convention #9.
12. **Defensive `localId = sessionUnitId ?: UUID.randomUUID()` in the edit-mode persist fork** is unreachable today (no delete affordance exists), kept intentionally for PR 3d's planned delete-then-edit race. Soft-recovery semantics align with the planned `deleteSessionUnitIfNoSpecimens` invariant: the deleted unit had no specimens, so re-inserting under the same id is non-destructive. See Convention #7.
13. **`DuplicateIdentityWarningBanner` renders inside the Batch Identity tile's content lambda**, alongside the `forEach` loop of identity `DynamicFormField`s — not between tiles. In-tile placement didn't require modifying `CollectionBatchFormTile`'s API and gave visual locality. See Convention #10.
14. **Identity-component filter explicitly includes `isUnitIdentityComponent`.** Earlier drafts of `ValidateCollectionBatchIdentityUseCase` filtered only on `answerScope == SESSION_UNIT`, which would have silently widened identity to every unit-scoped question once a non-identity unit-scoped question was added. Caught and corrected during review. Filter is now `it.answerScope == FormQuestionScope.SESSION_UNIT && it.isUnitIdentityComponent`.

**Deferred to PR 3d** (form polish + delete + submit-dialog + Imaging scoping):

1. ~~**Edit-mode answer hydration.** `loadFormDetails` is `sessionUnitId`-blind — tapping an existing card opens a blank form. Add `FormAnswerDao.getFormAnswersBySessionUnitId(sessionUnitId: UUID): List<FormAnswerEntity>` + matching `FormAnswerRepository.getFormAnswersBySessionUnitId(sessionUnitId): Map<Int, FormAnswer>` (same shape as the existing `getFormAnswersBySessionId`). Then in `loadFormDetails` branch on `sessionUnitId`: when non-null, fetch and merge into the seed map, preserving each `FormAnswer.localId` from the DB row. **Critical for `@Upsert` collapse** — generating fresh UUIDs at hydration breaks UPDATE-vs-INSERT and accumulates duplicate `form_answer` rows on every edit save. See Convention #12.~~ ✅ **Shipped in PR 3d, slice 1** — see the PR 3d in-progress subsection below.
2. **Reactive duplicate-identity banner.** Currently only updates on `SubmitSessionUnitForm`. Derive via a `combine(identityDrafts, formQuestions, existingAnswersBySessionUnitId).launchIn(viewModelScope)` Flow that writes the validator's `errorOrNull()` back into `state.collectionBatchFormErrors.duplicateIdentity`. Snapshot the existing answers once in `loadFormDetails` (no concurrent unit-write path today — `MutableStateFlow<Map<UUID, Map<Int, FormAnswer>>>` field on the VM is sufficient). `SubmitSessionUnitForm` becomes read-only against the flow-derived value. Naturally side-steps the eager-validation UX problem because the validator returns `Success` for blank identities.
3. ~~**`CollectionBatchIdentityResolver` + list-card identity-label upgrade.** Pure display formatter (sort identity questions by `id`, join with `" · "`, fall back to `"Batch ${unit.unitOrder}"` when no identity questions exist). Wired into `CollectionBatchListViewModel`, consuming the same `FormAnswerRepository.getSessionUnitScopedFormAnswersBySessionId` snapshot the form VM uses. `CollectionBatchCard` renders the derived title.~~ ✅ **Shipped in PR 3d, slice 3** — see the PR 3d in-progress subsection. Naming, signature-shape, and silent-failure follow-ups rolled into the new "Audit & cleanup pass" subsection.
4. **Submit-session confirmation dialog (deferred from 3c — Additional scope #1).** Split `CollectionBatchListAction.SubmitSession` into dialog-open + `SaveSessionAsInProgress` + `ConfirmSubmitSession`. Open question still: does the same dialog belong on `ImagingScreen.SubmitSession`? Decide at 3d kickoff.
5. **Per-field error reactive clearing.** Same `combine` Flow pattern as the banner but for `formAnswerErrors`. Carries an additional UX subproblem — eager validation on form-open would show "required field" everywhere unless gated by a per-field "touched" tracker. Lower priority than the banner; consider only if QA flags inline-error timing as a usability issue.
6. **Delete affordance** — `SessionUnitRepository.deleteSessionUnitIfNoSpecimens` + matching DAO query, `DeleteCollectionBatch` action, `canDelete` flag on `CollectionBatchCard`. From original 3d scope. Once this lands, Deviation #12's defensive `?:` becomes reachable.
7. **Imaging scoping** (original 3d scope) — `ImagingViewModel` / `ImagingState` / `ImagingScreen` read `sessionUnitId`, gate submit/upload UI on `isUnitScoped`, propagate `Specimen.sessionUnitId`, emit `ImagingEvent.NavigateBackToCollectionBatchList` when unit-scoped.
8. **`DuplicateIdentityWarningBanner` text consolidation.** Banner hardcodes a string (`"Another collection batch with these identity values already exists!"`) while the toast goes through `R.string.collection_batch_form_error_duplicate_identity` (`"A collection batch with this identity already exists. Please change one of the identity fields."`). Two strings for the same condition. Banner should consume the same string resource. Minor polish.
9. **Silent failure handling in `loadFormDetails`.** `programId == null` / `program == null` / `form == null` still just drops `isLoading` to `false` and leaves an empty screen. Add `emitError(CollectionBatchFormError.UNKNOWN_ERROR)` + `NavigateBackToCollectionBatchListScreen` emission, mirroring `IntakeViewModel:636-650`. **Note (post-slice-3):** the same swallow-and-return-empty pattern now also exists in `CollectionBatchListViewModel.loadSessionUnitFormQuestions`. Fix both VMs in the same diff during the "Audit & cleanup pass" subsection above.

**Deferred to PR 5** (sync wiring):

10. **`FormAnswerRepository.getFormAnswersBySessionId` collapse-by-questionId bug.** Returns `Map<Int, FormAnswer>` which silently collapses multiple units' answers for the same questionId — uploads break for sessions with >1 unit because only the last unit's answers per `questionId` survive the `.associate { ... }` step. Fix: reshape return to `List<FormAnswer>` (or `Map<UUID?, Map<Int, FormAnswer>>` with `null`-keyed bucket for session-scoped). Worker + intake-hydration call sites updated correspondingly.
11. **Intake hydration scope fix.** `IntakeViewModel` calls `getFormAnswersBySessionId` which now (post-3c) may return unit-scoped rows the SESSION-only intake form has no place for. Add `getSessionScopedFormAnswersBySessionId` (or similar scoped variant) and route intake through it. Sibling of the worker fix; bundle in the same PR.
12. **`@Relation`-based fetch** — `SessionUnitWithFormAnswersRelation` queried via `@Transaction @Query("SELECT * FROM session_unit WHERE sessionId = :sessionId")` on `SessionUnitDao`. Lands when both the list-card identity-resolver *and* the duplicate-identity validator can share the relation as their fetch path. Until then, the bare `getSessionUnitScopedFormAnswersBySessionId` is the single consumer and a relation would be premature.

**Outstanding latent / debt items called out during 3c review** (not blocking, capture for tracking):

13. **`countSpecimensForSessionUnit` is misplaced** on `SessionUnitDao` — should live on a `SpecimenDao` / `SpecimenRepository`, but neither exists yet. PR 3b's expedient. Address when a `SpecimenRepository` is introduced. **Do not extend this precedent** with further cross-table queries on `SessionUnitDao`.
14. **`FormAnswer.submittedAt` is non-nullable `Long`** (sentinel `0L`) while every other entity uses `Long? = null` for the same "set after upload" semantic. Structural inconsistency in the model layer. Future cleanup: convert to `Long?`.
15. **`DynamicFormField` and `FormQuestionPrerequisiteEvaluator` still live under `intake.presentation.components` / `intake.domain.util`** but operate purely on core domain types. Cross-feature leak from Slice 1. Clean fix: promote to `core.presentation.components.form/` and `core.domain.util/` (where `DropdownField`, `TextEntryField`, etc. already live). Not in 3c's scope.

**New conventions / best practices that emerged from this slice and should propagate to PR 3d+.**

(Slice-1 conventions retained verbatim below as 1–5. Slice-2 conventions added as 6–17.)

1. **Derived state belongs in the screen, not the VM or state.** If a value is a cheap, pure transform of state already on screen, derive it inline. Only promote to `remember` when something downstream needs identity stability (e.g. `LazyColumn` `items` keys, `LaunchedEffect` keys, `derivedStateOf` sources). Only promote to state when a sibling reads it or persistence depends on it. Precedent: `IntakeScreen`'s in-line `answerMap`, `isOtherCollectionMethod`, `isOtherSpecimenCondition`. Concrete cleanup candidates in `IntakeState` identified for a future pass:
   - **`isCurrentCollectorMissing`** → move to the screen as a derived `val` over `session.collectorName/title` × `allCollectors`.
   - **`isCollectionMethodTooltipVisible`** → `rememberSaveable` in the screen. Pure UI ephemeral.
   - **Adjacent bug**: `_allCollectors` is observed twice in `IntakeViewModel` (outer `combine` at 86-96 + inner at ~760). Inner write is dead.
2. **Feature boundary discipline.** Presentation components from one feature package should not be imported by another. Cross-feature leaks still present: `DynamicFormField` + `FormQuestionPrerequisiteEvaluator` from `intake.*` (see Outstanding debt #15).
3. **`DefaultIntakeFieldsCache` is for stable user/device/site context only.** Volatile per-session content belongs in `FormAnswerRepository`. Cross-session memory is strictly: collector identity, last-trained-on, hardware ID, district, village, locationSelections.
4. **`@Serializable(ignoreUnknownKeys = true)` is the migration story for DataStore DTO field removal.** No version bump, no migration code.
5. **Rule 2 nuance — "no sibling" ≠ "no rename".** Widening an existing function and renaming it is still Rule 2 compliant: the anti-pattern is *adding a second method* with overlapping responsibility. Single-method-body is the actual rule.
6. **`Map<Int, FormAnswer>`-keyed reads only work for single-scope data.** As soon as a session contains both SESSION and SESSION_UNIT answers, a single `questionId` can map to multiple rows (one session-scoped + one per unit). Repository reads that collapse via `.associate { it.questionId to ... }` are scope-dangerous. Correct shapes: `Map<UUID, Map<Int, FormAnswer>>` (grouped by unit) or `List<FormAnswer>` (flat). Treat `Map<Int, FormAnswer>` returns as a "single scope per call" contract — the call site has to know which scope it's reading.
7. **Filter-by-X methods belong on the repository named after the entity being filtered, regardless of what X is.** `FormAnswerRepository.getSessionUnitScopedFormAnswersBySessionId` belongs on `FormAnswerRepository` (returns `FormAnswer`), not on `SessionUnitRepository` (uses `sessionUnitId` only as a filter discriminator). Same for the underlying DAO query. Mirror for all future cross-table reads. Counter-precedent `countSpecimensForSessionUnit` is misplaced debt, not a pattern.
8. **Modify call sites to pass explicit defaults rather than relying on them.** When widening a function with a defaulted parameter (e.g. `upsertFormAnswer(..., sessionUnitId: UUID? = null)`), update existing callers to pass `null` explicitly rather than letting the default cover them. Forces every call site to read self-documenting about scope and prevents new callers from accidentally inheriting unit-scope behavior. Applied to `IntakeViewModel.handleSaveIntake` and `MetadataUploadWorker.syncFormAnswersIfNeeded` in 3c.
9. **Do NOT mutate `submittedAt` from the VM.** That field is the upload-side timestamp, set by `RemoteFormAnswerDataSource` when constructing the request DTO. Local saves leave it at the model default (`0L` for `FormAnswer`, `null` for everything else — see Outstanding debt #14). Pre-existing `IntakeViewModel:278` bug overwrote it on local save; fixed as part of 3c.
10. **Feature-local error enums with exhaustive `ErrorExtensions` branches.** Each feature defines its own `*Error` enum implementing `Error`, and `ErrorExtensions` resolves to feature-specific string resources via `is FeatureError -> when (this) { ... }`. Exhaustive `when` catches forgotten branches at compile time. Pattern: `IntakeError` / `FormValidationError` / `CollectionBatchFormError`.
11. **Validation lives in `Validate*UseCase` classes, not `object` validators.** Hilt-injectable, testable, mirrors `ValidateFormAnswersUseCase`. Aggregator data class (`*ValidationUseCases`) bundles multiple validators — single VM constructor param, easy to grow. Don't use `object` for new validation (the earlier `CollectionBatchIdentityValidator` object proposal was explicitly rejected in favor of the use case shape).
12. **Identity comparison is structured per-questionId equality, never a derived-string match.** `.trim()` only; no `.lowercase()` (matches `ValidateFormAnswersUseCase`'s `select` casing). Blank-identity drafts skip duplicate detection (let `required` field validation surface the missing field instead). Display-side string formatting (bucket-name resolver) is a separate concern, never reused for validation.
13. **Defensive code earns its place by aligning with planned future semantics.** The `sessionUnitId ?: UUID.randomUUID()` in persist is "unreachable today" but its soft-recovery behavior matches the planned `deleteSessionUnitIfNoSpecimens` invariant. Keep defensive code only when (a) the case becomes reachable in a known-future PR AND (b) the recovery semantics are forward-compatible. Add a one-line comment explaining the planned reachability so a future reader doesn't simplify it away.
14. **Persistence pipelines wrap multi-row writes in `transactionHelper.runAsTransaction { ... }`.** The pattern: each repo call returns `Result<Unit, RoomDbError>`; `.onError { emitError(it); return@runAsTransaction false }` short-circuits the transaction; the block returns `true` on success; the VM gates post-success side effects (event emit, cache write, navigation) on the boolean. Mirrors `IntakeViewModel.handleSaveIntake`.
15. **In-tile error rendering is acceptable when scoped to the tile's content lambda.** Feature-local banners (e.g. `DuplicateIdentityWarningBanner`) render inside `*Tile`'s content slot alongside the `forEach` of `DynamicFormField`s. Does not modify the tile's API. Achieves visual locality without coupling cost. Mirror for future per-tile error/warning surfaces.
16. **Banner copy and toast copy should share a single string resource.** Cuts the maintenance/translation surface; ensures consistency. (3c currently has a minor divergence — see Deferred-to-3d #8.)
17. **`@Upsert` semantics require stable `localId` on hydrated entities.** When loading existing rows into editable state, preserve the DB row's primary key on the in-memory model. Generating fresh UUIDs at hydration breaks the upsert collapse and accumulates duplicate rows on every save. Critical contract for 3d's edit-mode hydration — applies to `FormAnswer.localId` specifically when hydrating the form's seed map.
18. **Reactive state derivation via `combine(...).launchIn(viewModelScope)` is preferred over imperative `_state.update { ... }` calls scattered across action handlers** when the derived field's inputs all live in flows the VM already exposes. Pattern: derive once at VM construction (or inside `onStart`), write the result back to `_state` via `.onEach`. Eliminates the "did you remember to clear this field" review burden — the field mirrors its inputs, full stop. Slated for 3d to retrofit `duplicateIdentity` (and optionally `formAnswerErrors`) onto this pattern.

### PR 3d — In progress

PR 3d is being landed as a sequence of small, independently-reviewable slices rather than a single diff. Each slice ticks off one item from the "Deferred to PR 3d" list under PR 3c. This subsection logs what has shipped so far; remaining work is summarized in the "Next up — PR 3d" section below.

**Slice 1 — Edit-mode answer hydration (✅ Shipped).** Closes Deferred-to-3d #1.

Before: tapping an existing batch card on `CollectionBatchListScreen` routed to `CollectionBatchForm` with a non-null `sessionUnitId`, but `loadFormDetails` ignored it and seeded `formAnswersByQuestionId` with fresh defaults — the user saw a blank form in edit mode. Saving from that blank form would also have generated brand-new `FormAnswer.localId`s, breaking `@Upsert` collapse and accumulating duplicate `form_answer` rows on every edit save (Convention #17).

**Files modified:**

- `core/data/room/dao/FormAnswerDao.kt` — added `getFormAnswersBySessionUnitId(sessionUnitId: UUID): List<FormAnswerEntity>`. Query mirrors `getFormAnswersBySessionId` line-for-line, just swapping the `WHERE` column.
- `core/domain/repository/FormAnswerRepository.kt` + `core/data/repository/FormAnswerRepositoryImplementation.kt` — added `getFormAnswersBySessionUnitId(sessionUnitId: UUID): Map<Int, FormAnswer>`. Same `.associate { it.questionId to it.toDomain() }` mapper shape as `getFormAnswersBySessionId`.
- `collection_batch/form/presentation/CollectionBatchFormViewModel.kt` — `loadFormDetails` now fetches `val savedFormAnswers = sessionUnitId?.let { formAnswerRepository.getFormAnswersBySessionUnitId(it) }.orEmpty()` and seeds `formAnswersByQuestionId` via the Elvis idiom `savedFormAnswers[question.id] ?: FormAnswer(...defaults...)`. When a saved answer exists it is passed through whole — preserving the DB row's `localId` so subsequent saves `UPDATE` instead of `INSERT`. Mirrors `IntakeViewModel:735–743` verbatim.

**Why a sibling and not widening.** The two existing reads on `FormAnswerRepository` — `getFormAnswersBySessionId(sessionId): Map<Int, FormAnswer>` and `getSessionUnitScopedFormAnswersBySessionId(sessionId): Map<UUID, Map<Int, FormAnswer>>` — both take a `sessionId` and return either a flat session-scoped map or a unit-bucketed map. Edit-hydration needs the third combination: a `sessionUnitId` parameter returning the flat `questionId → FormAnswer` map for that one unit. Widening either existing function would change its return shape (forcing all current call sites to adapt) and invert the parameter from "session" to "unit". The new method sits at the same naming tier as `getFormAnswersBySessionId` — Rule 2 satisfied, Convention #6 honored.

**Plan-compliance checklist.**

- ✅ **Rule 1** (add methods only when the call site lands) — this diff is exactly the call site.
- ✅ **Rule 2** (modify-don't-proliferate evaluated) — widening rejected for the reason above; sibling is the right shape.
- ✅ **Rule 4** (match conventions) — DAO query, repo mapper, and VM hydration loop all mirror existing peers.
- ✅ **Convention #6** (filter-by-X belongs on the entity being filtered) — lookup on `FormAnswerRepository`, not `SessionUnitRepository`.
- ✅ **Convention #17** (`@Upsert` requires stable `localId`) — preserved by passing the saved `FormAnswer` whole.

**No banner-path interaction.** `SubmitSessionUnitForm` already passes `editingSessionUnitId = sessionUnitId` to `validateCollectionBatchIdentity`, so a hydrated edit-mode form does not false-flag against its own persisted identity. Slice 1 leaves the banner path untouched — the eventual reactive-banner refactor (Deferred-to-3d #2) remains independently shippable.

**Deviations from plan.** None. The slice landed as specified in Deferred-to-3d #1 above.

**Slice 3 — Identity-string display on list cards (✅ Shipped).** Closes Deferred-to-3d #3.

Before: every collection-batch card on `CollectionBatchListScreen` rendered `"Batch ${unit.unitOrder}"` regardless of identity values — a PR 3b placeholder. After: the card title is the joined identity string (e.g. `"18:00 · 19:00 · Indoor"`), falling back to `"Batch ${unit.unitOrder}"` only when no identity values exist.

**Files added:**

- `collection_batch/domain/util/CollectionBatchIdentityResolver.kt` — `object` with a single pure function `deriveBucketName(formQuestions, answersByQuestionId): String`. Filters by `answerScope == SESSION_UNIT && isUnitIdentityComponent`, sorts by `questionId` for stable output, trims values, joins non-blank ones with `" · "`. Returns `""` when nothing applies — callers own the fallback.

**Files modified:**

- `collection_batch/list/presentation/CollectionBatchListState.kt` — added `val bucketNameBySessionUnitId: Map<UUID, String> = emptyMap()`.
- `collection_batch/list/presentation/CollectionBatchListViewModel.kt` — gained five Hilt injections (`deviceCache`, `programRepository`, `formRepository`, `formQuestionRepository`, `formAnswerRepository`) and a `loadSessionUnitFormQuestions()` suspend helper that mirrors `CollectionBatchFormViewModel.loadFormDetails`'s program→form→questions chain. The existing `combine(_sessionUnits, _state) { ... }` lambda was extended to fetch `formQuestions` + `existingAnswersBySessionUnitId` once per emission and derive `bucketNameBySessionUnitId` alongside the existing `specimenCountsBySessionUnitId`.
- `collection_batch/list/presentation/components/CollectionBatchCard.kt` — gained a `title: String` parameter; the card applies the `.ifBlank { "Batch ${sessionUnit.unitOrder}" }` fallback locally.
- `collection_batch/list/presentation/CollectionBatchListScreen.kt` — passes `title = state.bucketNameBySessionUnitId[sessionUnit.localId].orEmpty()`.

**Design decisions worth recording:**

1. **Resolver lives in `domain/util/`, not `presentation/`.** The VM is the consumer; the screen sees only the precomputed `Map<UUID, String>`. Mirrors the precedent set by `intake/domain/util/FormQuestionPrerequisiteEvaluator` (pure function `object`, no DI). Validation conventions (use case + Hilt) don't apply because this is display formatting, not validation.
2. **Bucket name lives on state, not derived in the component.** Convention #1 ("derive in the screen") was evaluated against the alternative ("put both raw inputs on state and let the component derive"). The alternative grows state surface more than the chosen approach (one `Map<UUID, String>` field vs. a `List<FormQuestion>` + `Map<UUID, Map<Int, FormAnswer>>` pair). Convention #1's spirit is "minimize state surface", which the chosen approach honors even while deviating from the letter. Mirrors the `specimenCountsBySessionUnitId` shape as a precomputed per-unit attribute map.
3. **Fallback `"Batch ${unit.unitOrder}"` lives in the card, not the resolver or the VM.** Keeps the resolver's contract clean ("derive identity, return empty if none") and lets future consumers (delete-confirmation dialog, submit dialog) distinguish "this unit has no identity values yet" from "this unit's name happens to be 'Batch 3'". Card has `unitOrder` in scope so the fallback is free at the consumer.
4. **No `@Relation`-based fetch in this slice.** Deferred-to-PR-5 #12 (`SessionUnitWithFormAnswersRelation`-backed `@Transaction @Query` on `SessionUnitDao`) would let this VM consume `Flow<List<SessionUnitWithFormAnswers>>` and drop the separate `formAnswerRepository` lookup. The plan's threshold for landing it — "two consumers of the relation" — is now met (validator in 3c + resolver in 3d), so pulling it forward in PR 5 (or a dedicated cleanup PR) is the next obvious refactor. Slice 3 deliberately did not bundle it to keep this diff focused.

**Deviations from plan worth recording.**

1. **`CollectionBatchIdentityResolver` is a top-level `object`, not the Hilt-injectable class the plan described.** Plan §307 implied a use-case-like shape. Final implementation matches `FormQuestionPrerequisiteEvaluator`'s shape (display utility ≠ validator — Convention #11 only mandates classes for validation). No DI graph cost, no test setup cost.

**Deferred / outstanding from this slice (rolled into the audit and follow-ups below).** Item-by-item nits surfaced during slice 3 review are tracked in the new "Audit & cleanup pass" subsection below — not refiled per-item under Deferred-to-3d to avoid double-bookkeeping.

### PR 3d follow-up — Audit & cleanup pass (⏭️ Required before PR 3 closes)

The slice-by-slice cadence of PR 3d has been productive but has accumulated micro-debt that should not bleed into PR 4. **Before declaring PR 3 done, run a dedicated cleanup pass across the entire `collection_batch/` feature surface.** Scope at minimum the following classes of issue — but treat the list as a starting point, not a ceiling. The goal is *no surprising surface left*.

**1. Function signatures: pass the minimum the callee needs, not the maximum the caller has.**

Example: `CollectionBatchIdentityResolver.deriveBucketName(formQuestions, answersByQuestionId)` takes the *full* `List<FormQuestion>` and filters internally. The VM call site already has access to this filter — passing pre-filtered `identityQuestions: List<FormQuestion>` would:

- Make the function's contract self-documenting ("here are the identity questions, here are the answers")
- Push the filter to the call site where caching across multiple cards is trivial (one filter pass vs. N)
- Reduce coupling — the function no longer needs to know about `FormQuestionScope` or `isUnitIdentityComponent`

`ValidateCollectionBatchIdentityUseCase` has the same shape and the same opportunity. Audit every `collection_batch/`-feature function for "could this callee accept narrower / pre-processed inputs?".

**2. Variable names: be specific about what's in the map.**

Examples surfaced in slice 3 review:

- `answersBySessionUnitId: Map<UUID, Map<Int, FormAnswer>>` — the inner map is `questionId → FormAnswer`, not `FormAnswer`. Name should be `formAnswersByQuestionIdBySessionUnitId` (cumbersome) or restructured (see #3 below). At minimum the type and name should agree.
- `bucketNameBySessionUnitId: Map<UUID, String>` (singular) vs. `specimenCountsBySessionUnitId: Map<UUID, Int>` (plural) — same shape, two conventions. Pick one and apply across the file.
- `existingAnswersBySessionUnitId` (in `CollectionBatchFormViewModel`) vs. `answersBySessionUnitId` (in `CollectionBatchListViewModel`) — different names for the same shape returned by the same repo call. Pick one.

General rule: **the name of a `Map<K, V>` field should answer "what V is, indexed by K", and both the K-name and the V-name should match the types.**

**3. Nested map shapes: ask whether a `data class` or a domain model would be clearer.**

`Map<UUID, Map<Int, FormAnswer>>` is functional but opaque at every call site. Candidates for cleanup:

- Introduce a domain composite (e.g. `SessionUnitWithFormAnswers`) and consume it directly — connects to Deferred-to-PR-5 #12 (the `@Relation`-based fetch). Pulling the relation forward would replace nested maps with a flat `List<SessionUnitWithFormAnswers>` everywhere.
- If the relation refactor is out of audit scope, at minimum use a `typealias` (e.g. `typealias FormAnswersByQuestionId = Map<Int, FormAnswer>` and `typealias FormAnswersBySessionUnitId = Map<UUID, FormAnswersByQuestionId>`) so the intent reads at the type signature.

**4. Repository / DAO method names: audit for `*Scoped*` cruft now that `sessionUnitId` is a first-class column.**

`getSessionUnitScopedFormAnswersBySessionId` made sense in PR 3c when the only filter was "WHERE sessionUnitId IS NOT NULL". Now that the form-answer table has multiple legitimate access patterns (session-scoped, unit-scoped, all-of-session), the name is descriptive but verbose. Audit whether `getUnitScopedFormAnswersBySessionId` reads cleaner, or whether a different parameterization (e.g. `getFormAnswersBySessionId(sessionId, scope: FormQuestionScope?)`) is more consistent with the `getQuestionsByFormIdAndScope` precedent from PR 3c.

**5. Cross-VM duplication.**

`CollectionBatchListViewModel.loadSessionUnitFormQuestions` and `CollectionBatchFormViewModel.loadFormDetails` both walk the same `deviceCache → programRepository → formRepository → formQuestionRepository` chain. Two consumers is the threshold the plan typically uses to justify a use case. Audit whether `GetSessionUnitFormQuestionsUseCase` (or a more general `GetFormQuestionsByScopeUseCase`) is overdue.

**6. Silent failure handling.**

Already tracked as Deferred-to-3d #9 — but the slice 3 helper inherits the same swallow-and-return-empty pattern, so the bug is now in *two* VMs, not one. Audit should fix both call sites in the same diff and standardize on the `emitError + log + navigate-away` shape from `IntakeViewModel:622–638`. May require introducing a `CollectionBatchListError` enum (mirror of `CollectionBatchFormError`) — fold the audit's `ErrorExtensions.kt` and `strings.xml` additions into the same diff.

**7. Magic strings and constants.**

`"Batch ${sessionUnit.unitOrder}"`, `" · "` separator, etc. should live in `strings.xml` (display) or a `const val` (formatting) — not in `.kt` literals. Audit every literal in `collection_batch/` for promotion.

**8. Composable param names and ordering.**

E.g. `CollectionBatchCard(title, sessionUnit, specimenCount, onClick, modifier)` — does `title` describe what the param is, or does `bucketName` (the conceptual identity) read better? Same for ordering — Compose convention places `modifier` last, lambdas after data, but the rest of `collection_batch/` may not be consistent. Audit feature-wide.

**9. KDoc on display-only / convention-load-bearing functions.**

`CollectionBatchIdentityResolver` deliberately must not be reused for identity validation (Convention #12). Its purpose-comment was dropped during slice 3 — the audit should re-add KDoc that flags the contract. Same audit for other `domain/util` functions in the feature.

**10. Anything else.**

The list above is the seeded set from slice 3 review. The audit should be approached as a *fresh read* of the feature — open every file and ask "would a new contributor understand this in one read, or does it need a comment / rename / refactor to be obvious?". Items found during the audit should be added here so future audits can see the cumulative cleanup history.

**Recommended ordering:** run the audit *after* slice 6 (Imaging scoping) lands and *before* PR 3 is declared closed. Reasons:

- The Imaging slice will touch shared types (`Specimen.sessionUnitId`, `FormAnswerRepository.upsertFormAnswer`-style widening for specimens) that may surface additional naming inconsistencies worth bundling.
- Holding the audit until last lets it act as the "ship gate" for PR 3, rather than being interleaved with feature work.

**What NOT to defer to PR 4 or beyond.** The point of this audit is that the feature surface should be reviewable, consistent, and convention-aligned the moment PR 3 closes. Item #6 (silent failures) is the only thing already on the deferred-to-3d list that should be folded in here; the rest is new debt surfaced during slice-by-slice review.

### Next up — PR 3d: form polish (reactive banner) + delete affordance + submit-session dialog + Imaging scoping + feature-wide audit

> Scope is summarized in the "Deferred to PR 3d" subsection under PR 3c above. The general PR-3 rules and slicing reference material below still apply.

> ## ⚠️ Read this before writing any code in PR 3
>
> **DO NOT start PR 3 by dumping all the repository and DAO functions we will eventually need.** The Pre-PR 3 cleanup deliberately emptied `SessionUnitRepository`, `SessionUnitRepositoryImplementation`, and `SessionUnitDao` (and pruned other dead methods across the core layer) precisely so this feature can grow function-by-function on demand.
>
> The rules for PR 3:
>
> 1. **Add repository / DAO methods one at a time, and only when the call site that needs them exists in the same diff.** No speculative `observeX`, `getMaxX`, `countX`, etc. up front.
> 2. **Prefer extending an existing function over adding a sibling.** Before introducing a new DAO/repo/mapper/util function, check whether the existing function can be widened (typically with an optional/nullable parameter or a sensible default) to cover both the old and new call sites. The bar for "add a new function instead" is: the modified function's body would become materially harder to read, the old callers would need non-trivial updates, or the new behavior diverges in return shape / nullability / locking. If none of those apply, **modify, don't proliferate**. Concrete examples already in the codebase: `FormAnswer.toEntity(sessionId, questionId, sessionUnitId = null)` (PR 1 — added the third param with a default rather than creating `toEntityForSessionUnit`), and `FormAnswerRepository.upsertFormAnswer` slated to grow a `sessionUnitId: UUID? = null` param in PR 3c rather than getting a `upsertUnitScopedFormAnswer` sibling. When in doubt, mirror those moves.
> 3. **Disregard the function lists in §3.8 and §4.3 of this plan.** Those were drafted when the layer was assumed full. The empty contracts post-cleanup are the new starting point; §3.8 / §4.3 are reference material, not a checklist.
> 4. **Match existing codebase conventions every time.** When you add a new repo method, mirror the shape of nearby `SessionRepository` / `SpecimenRepository` methods (e.g. `Result<Unit, RoomDbError>` returns, `observe*` for Flow, `get*ById` for suspend, `fun toEntity(sessionId: UUID)` mapper threading). Consistency over cleverness — this codebase has strong patterns and PR 3 should not introduce new ones.
> 5. **Slice PR 3 into smaller PRs.** A single PR carrying two new screens, two new viewmodels, two domain utilities, an `Imaging` destination contract change, and a `FormAnswerRepository` signature change is too large to review well. The recommended slicing is below.

**Scope.** §5 (file migration), §7 (list screen), §8 (form screen), §9 (imaging scoping), plus the two one-line concrete-strategy flips from PR 2:

- `SingleBatchWorkflow.postIntakeDestination` → `Destination.Imaging()` (after `Imaging` becomes a `data class`).
- `MultipleBatchWorkflow.postIntakeDestination` → `Destination.CollectionBatchList(sessionId.toString())`.

**Recommended slicing of PR 3.** Land these as separate PRs in order — each is independently reviewable, leaves `master` in a working state, and surfaces a single concern:

- **PR 3a — Destination contract + nav scaffold + strategy flip.** ✅ Merged — see the dedicated PR 3a subsection above for the as-shipped contract. Shipped shape: `Imaging(val sessionUnitId: String?)`, `CollectionBatchList(val sessionId: String)`, `CollectionBatchForm(val sessionId: String, val sessionUnitId: String?)` — no defaults, and the form's unit field is named `sessionUnitId` (not `unitId` as this bullet originally read). Downstream slices must use the `sessionUnitId` name verbatim.
- **PR 3b — `CollectionBatchList` screen + VM.** Add the list-side files under `collection_batch/list/presentation/`. **Add to `SessionUnitDao` / `SessionUnitRepository` only the methods the ViewModel actually calls in this diff** — most likely one observe-style query for units in a session, and whatever count is needed for the card. Do not pre-add edit/delete methods until the actions that consume them exist.
- **PR 3c — `CollectionBatchForm` screen + VM + identity utilities.** Add `collection_batch/form/presentation/` and `collection_batch/domain/util/{CollectionBatchIdentityResolver,CollectionBatchIdentityValidator}.kt`. Add only the repo / DAO methods this VM actually invokes (e.g. upsert + the lookup needed for edit mode + the cross-unit duplicate check). Extend `FormAnswerRepository.upsertFormAnswer` with the `sessionUnitId` parameter here, since this is the first caller that needs it. **Also addresses two cross-cutting follow-ups carried over from PR 3b — see "Additional scope" below.**
- **PR 3d — Imaging scoping.** `ImagingViewModel` / `ImagingState` / `ImagingScreen` read `sessionUnitId`, gate submit/upload UI on `isUnitScoped`, propagate `Specimen.sessionUnitId`, and emit `ImagingEvent.NavigateBackToCollectionBatchList` when unit-scoped. Add any final delete-guard repo method needed by 3b's card actions if it was deferred.

**Files to add across 3a–3d** (high level — see §5.3 for the full directory layout):

```
collection_batch/domain/util/
  CollectionBatchIdentityResolver.kt          # PR 3c
  CollectionBatchIdentityValidator.kt         # PR 3c
collection_batch/list/presentation/
  CollectionBatchListScreen.kt + ViewModel/State/Action/Event   # PR 3b
  components/CollectionBatchCard.kt           # PR 3b
collection_batch/form/presentation/
  CollectionBatchFormScreen.kt + ViewModel/State/Action/Event   # PR 3c
```

**Files to modify across 3a–3d** (high level):

- `navigation/Destination.kt` — PR 3a.
- `navigation/NavGraph.kt` — PR 3a (stubs), 3b (list wiring), 3c (form wiring), 3d (imaging back-nav branch).
- `imaging/presentation/ImagingViewModel.kt` + `ImagingState.kt` + `ImagingScreen.kt` + `ImagingEvent.kt` — PR 3d.
- `core/domain/repository/FormAnswerRepository.kt` + impl — PR 3c (when the form VM first needs unit-scoped answers).
- `core/data/room/dao/SessionUnitDao.kt` + `core/domain/repository/SessionUnitRepository.kt` + impl — grown method-by-method across 3b / 3c / 3d, only as call sites require.

**Additional scope decided after PR 3b** (✅ both items landed in PR 3c — retained here as reference for the rationale; see the merged PR 3c subsection for what shipped):

1. **Submit-session confirmation dialog with "save as in-progress" branch.** Today `CollectionBatchListAction.SubmitSession` immediately calls `sessionRepository.markSessionAsComplete(...)` + `workManagerRepository.enqueueSessionUpload(...)` + `currentSessionCache.clearSession()` and pops to Landing — a single tap commits the user. PR 3c replaces this with a confirmation dialog that exposes two branches:
   - **"Save as in-progress"** — leaves the session unchanged (no `markSessionAsComplete`, no `enqueueSessionUpload`), only `currentSessionCache.clearSession()` + pop to Landing. The session shows up in the incomplete-sessions list and can be resumed later. This is the new path.
   - **"Confirm submission"** — the existing PR 3b body (verbatim transliteration of `ImagingViewModel.SubmitSession`). Marks complete, enqueues, clears cache, pops to Landing.

   Implementation shape: split `CollectionBatchListAction.SubmitSession` into the user-tap action that just opens the dialog, plus two terminal actions `SaveSessionAsInProgress` and `ConfirmSubmitSession`. The dialog lives in state (`val isSubmitDialogVisible: Boolean = false`) and is rendered by `CollectionBatchListScreen` using whatever the codebase's house dialog primitive is (grep `AlertDialog` / `ConfirmationDialog` first; reuse rather than re-invent). The cloud-upload header icon dispatches the dialog-open action; the dialog's two buttons dispatch the terminal actions.

   Open question for PR 3c implementation: **does this dialog pattern also belong on `ImagingScreen.SubmitSession`?** The current `ImagingViewModel.SubmitSession` is the source the PR 3b code copied verbatim, and it has the same single-tap commit behavior. If we accept the dialog UX as the standard for "ending a session", `ImagingViewModel.SubmitSession` should likely grow the same branching at the same time — at minimum, the legacy PSC/LTC/OTHER paths through `ImagingScreen` should not silently keep the old one-tap behavior. Decide before starting 3c whether the dialog change applies there too, or whether it stays scoped to the new collection-batch list for now.

2. **Scope-filtered form questions: SESSION on intake, SESSION_UNIT on the batch form.** PR 1 added the `answerScope` column on `form_question` precisely to support this split, but it is not yet honored anywhere on the read path. `FormQuestionDao.getQuestionsByFormId(formId)` returns *all* questions for a form regardless of scope, and `ProgramFormWorkflow.formQuestions` (consumed by `IntakeViewModel`) inherits that. Effects today: the Intake screen would render `SESSION_UNIT` questions inside the SESSION form once any real form data ships, and `CollectionBatchFormViewModel` would have to client-side-filter the same list. PR 3c fixes both ends **by widening the existing function, not adding a sibling** (per Rule 2 of the "Read this before…" callout):
   - **`FormQuestionDao.getQuestionsByFormId`** — extend the existing method to accept an optional scope filter:

     ```kotlin
     @Query("""
         SELECT * FROM form_question
         WHERE formId = :formId
           AND (:answerScope IS NULL OR answerScope = :answerScope)
         ORDER BY `order` ASC
     """)
     suspend fun getQuestionsByFormId(
         formId: Int,
         answerScope: FormQuestionScope? = null,
     ): List<FormQuestionEntity>
     ```

     `null` preserves the existing scope-blind behavior, so callers that don't care about scope (sync workers, registration, settings) are completely untouched — they continue calling `getQuestionsByFormId(formId)` and get every row back.
   - **`FormQuestionRepository.getQuestionsByFormId`** — mirror the signature change: `suspend fun getQuestionsByFormId(formId: Int, scope: FormQuestionScope? = null): List<FormQuestion>`. Impl forwards the scope argument to the DAO; mapping stays `entities.map { it.toDomain() }`.
   - **Intake side**: `ProgramFormWorkflowFactory.create(...)` (line 22) currently calls `formQuestionRepository.getQuestionsByFormId(form.id)`. Change to `getQuestionsByFormId(form.id, FormQuestionScope.SESSION)`. `FormPresentWorkflow.formQuestions` then carries only SESSION-scoped questions, and `IntakeViewModel` / `IntakeScreen` automatically render only those — no VM change required.
   - **CollectionBatchForm side**: when the new `CollectionBatchFormViewModel` lands in 3c, fetch its questions via `getQuestionsByFormId(form.id, FormQuestionScope.SESSION_UNIT)`. The plan's §8.2 step 1 ("Partition by `answerScope == SESSION_UNIT`") becomes redundant — the read path already returns only `SESSION_UNIT`-scoped questions, so the VM just partitions further by `isUnitIdentityComponent` to derive `identityQuestions` vs `otherUnitQuestions`.
   - **Scope-blind callers stay unchanged.** `MetadataUploadWorker.syncFormAnswersIfNeeded` (sync), `SettingsViewModel.upsertFormQuestion` (admin), and `RegistrationViewModel.upsertFormQuestion` (admin) all continue calling `getQuestionsByFormId(formId)` with no scope argument — the default `null` preserves their existing behavior verbatim.
   - **`FormQuestionScope` is already an enum** (PR 1 deviation #1 — `FormQuestionScope { SESSION, SESSION_UNIT }` with a `FormQuestionScopeConverter`). The DAO can take the enum directly — Room's converter handles persistence; no `String` round-trip needed at the call site.

   **Why widening over a sibling.** A `getQuestionsByFormIdAndScope` sibling would duplicate the query shape, the mapper plumbing, and the repo contract for what is functionally a single read with an optional `WHERE` clause. The widened version reads as plain English at every call site (`getQuestionsByFormId(id)` = "all", `getQuestionsByFormId(id, SESSION)` = "session ones"), keeps a single unit of test surface, and matches the precedent set by `FormAnswer.toEntity(sessionId, questionId, sessionUnitId = null)` in PR 1.

**What NOT to change in PR 3 (any slice).**

- Don't delete the `hour_log/` or `add_hour/` packages yet — that's PR 4. The legacy `composable<Destination.HourLog>` / `composable<Destination.AddHour>` blocks stay in `NavGraph.kt` until PR 4.
- Don't touch the upload worker — that's PR 5.
- Don't introduce the `isCollectionMethodLocked` flag — that's PR 6.

**Why slicing matters here.** PR 3 introduces two new screens, two new viewmodels, two new domain utilities, a destination contract change with cross-cutting `Imaging` impact, and the `FormAnswerRepository` signature change deferred from PR 1. Bundled together that's too much for a single review pass. Sliced as 3a–3d, each PR has a single conceptual change and a small, contained diff.

When all of PR 3a–3d are merged, update the Progress table above, fill in a "PR 3 — … (✅ Merged)" subsection mirroring the PR 1 / PR 2 ones, and mark PR 4 as ⏭️ Next up.

---

## 0. TL;DR

Today, when the user picks **HLC** as the collection method on the Intake screen, we route them through the `hour_log/` + `add_hour/` flow. That flow already nails the UX — a dashboard of repeatable "bucket" cards with an `[+]` to add another bucket, each bucket scoped to its own imaging session. The fields inside it are currently HLC-specific (`wind`, `rain`, `humidity`, `temperature`, `indoor/outdoor`).

We now need the same repeatable-bucket UX to work for *any* program whose form questions are tagged `answerScope = SESSION_UNIT`, with the bucket *identity* derived from questions marked `isUnitIdentityComponent = true`. The work is essentially: keep the proven two-screen shape from `hour_log/` + `add_hour/`, lift it to be form-driven instead of HLC-hardcoded, and rename the package to reflect its broader role.

We will:

1. Add a new `session_unit` Room table + new columns on `form_question`, `form_answer`, `specimen`.
2. Generalize the `hour_log/` + `add_hour/` packages into a single `collection_batch/` feature (preserving the existing list+form shape) — see §5 for the migration of files.
3. The new feature still has two screens: a **list** screen (the dashboard of cards, evolved from `HourLogScreen`) and a **form** screen (a dynamic form for one batch, evolved from `AddHourScreen`).
4. Wire HLC routing via a new `CollectionMethodWorkflow` strategy (mirroring the existing `ProgramFormWorkflow`).
5. Scope `ImagingScreen` to a particular collection batch via a nav arg, and hide the session-submit UI from imaging when used inside the batch flow (matching how the current HLC flow already defers submit to the dashboard).
6. Extend the existing `MetadataUploadWorker` with `syncSessionUnitsIfNeeded`, following the exact same `GET → POST → upsert remoteId` pattern used for sessions/specimens.

No new caching, no new logging, no use cases (utilities + repo methods instead).

---

## 1. Definitions

- **Session**: the overall collection event (e.g. one HLC visit at one site). Already exists.
- **Collection Batch** (data-layer name: `SessionUnit`): one repeatable sub-collection within a session (e.g. one HLC hour/location, one PSC room, one larval dip). **New.** The terms "collection batch" (UI/feature) and "session unit" (data) refer to the same thing.
- **Session-level answer**: `form_answer` row whose `sessionUnitId` is `null`. Applies to the whole session.
- **Batch-level answer**: `form_answer` row whose `sessionUnitId` is set. Applies to a specific collection batch.
- **Bucket name / derived identity**: a string built from the values of all `SESSION_UNIT`-scoped questions that have `isUnitIdentityComponent = true`, in `questionId` order, joined with " · " (or similar). Examples: `"18:00 · 19:00 · Indoor"`.
- **Identity-component questions**: form questions with `answerScope = SESSION_UNIT` AND `isUnitIdentityComponent = true`. These uniquely name a unit.

---

## 2. High-level user flow (HLC only)

```
Landing
  → Intake (SESSION-level questions)
      [user picks collectionMethod = HLC]
      ↓ after Save
  → CollectionBatchListScreen  (dashboard of batch cards)
      [+] taps:
        → CollectionBatchFormScreen (new)
            user answers SESSION_UNIT-scoped questions
            on Save:
              * validate required fields
              * validate derived bucket name unique within this session
              * upsert SessionUnitEntity + FormAnswerEntity rows
              → ImagingScreen(sessionUnitId)
                  user captures specimens (each tagged with sessionUnitId)
                  back → CollectionBatchListScreen
      [card] tap arrow:
        → ImagingScreen(sessionUnitId) for that unit
      [card] tap card body:
        → CollectionBatchFormScreen in edit mode (re-validates uniqueness)
      [card] long-press / swipe / overflow delete:
        → only if 0 specimens for the unit
      [cloud-upload] taps:
        → mark session complete + enqueue MetadataUploadWorker
```

For non-HLC sessions (PSC, LTC, OTHER) nothing changes: intake → imaging (with `sessionUnitId = null`) → submit from imaging, exactly as today.

---

## 3. Room schema changes

### 3.1 Bump DB version

`app/src/main/java/com/vci/vectorcamapp/core/data/room/VectorCamDatabase.kt`

- Change `version = 30` → `version = 31`.
- Add `SessionUnitEntity::class` to the `@Database(entities = [...])` array.
- Add `abstract val sessionUnitDao: SessionUnitDao` at the bottom of the class.

### 3.2 New entity: `SessionUnitEntity`

Create `app/src/main/java/com/vci/vectorcamapp/core/data/room/entities/SessionUnitEntity.kt`:

```kotlin
package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "session_unit",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["localId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class SessionUnitEntity(
    @PrimaryKey val localId: UUID = UUID(0, 0),
    val sessionId: UUID = UUID(0, 0),
    val remoteId: Int? = null,
    val unitOrder: Int = 0,
    val createdAt: Long = 0L,
)
```

This mirrors `SessionEntity` (localId UUID PK, nullable remoteId).

### 3.3 Modify `FormQuestionEntity`

`app/src/main/java/com/vci/vectorcamapp/core/data/room/entities/FormQuestionEntity.kt`

Add two fields (keep `answerScope` as `String` to match the existing `type: String` pattern — do NOT create an `AnswerScope` enum domain model):

```kotlin
data class FormQuestionEntity(
    @PrimaryKey val id: Int = -1,
    val formId: Int = -1,
    val parentId: Int? = null,
    val label: String,
    val type: String,
    val required: Boolean = false,
    val prerequisite: FormQuestionPrerequisiteExpression? = null,
    val options: List<String>? = null,
    val order: Int? = null,
    val answerScope: String = "SESSION",           // NEW. "SESSION" or "SESSION_UNIT"
    val isUnitIdentityComponent: Boolean = false,  // NEW.
)
```

Allowed values for `answerScope`: `"SESSION"` (default) or `"SESSION_UNIT"`. Define them as `const val` in a small companion if useful (not required).

### 3.4 Modify `FormAnswerEntity`

`app/src/main/java/com/vci/vectorcamapp/core/data/room/entities/FormAnswerEntity.kt`

Add nullable `sessionUnitId` with a foreign key to `session_unit.localId`:

```kotlin
@Entity(
    tableName = "form_answer",
    foreignKeys = [
        ForeignKey(
            entity = FormQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["localId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SessionUnitEntity::class,                      // NEW
            parentColumns = ["localId"],
            childColumns = ["sessionUnitId"],
            onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("questionId"), Index("sessionId"), Index("sessionUnitId")]
)
data class FormAnswerEntity(
    @PrimaryKey val localId: UUID = UUID(0, 0),
    val remoteId: Int? = null,
    val sessionId: UUID = UUID(0, 0),
    val sessionUnitId: UUID? = null,   // NEW
    val questionId: Int = -1,
    val value: String = "",
    val dataType: String = "",
    val submittedAt: Long = 0L
)
```

### 3.5 Modify `SpecimenEntity`

`app/src/main/java/com/vci/vectorcamapp/core/data/room/entities/SpecimenEntity.kt`

```kotlin
@Entity(
    tableName = "specimen",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["localId"], childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SessionUnitEntity::class,                     // NEW
            parentColumns = ["localId"], childColumns = ["sessionUnitId"],
            onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("sessionUnitId")],
    primaryKeys = ["id", "sessionId"]
)
data class SpecimenEntity(
    val id: String = "",
    val sessionId: UUID = UUID(0, 0),
    val sessionUnitId: UUID? = null,   // NEW
    val remoteId: Int? = null,
    val shouldProcessFurther: Boolean = false
)
```

### 3.6 Migration `30 → 31`

Create `app/src/main/java/com/vci/vectorcamapp/core/data/room/migrations/versions/Migration_30_31_AddSessionUnits.kt`:

```kotlin
package com.vci.vectorcamapp.core.data.room.migrations.versions

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_30_31_ADD_SESSION_UNITS = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create the new session_unit table.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `session_unit` (
                `localId` TEXT NOT NULL PRIMARY KEY,
                `sessionId` TEXT NOT NULL,
                `remoteId` INTEGER,
                `unitOrder` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`sessionId`) REFERENCES `session`(`localId`)
                    ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_unit_sessionId` ON `session_unit` (`sessionId`)")

        // 2. Add new columns on form_question.
        db.execSQL("ALTER TABLE `form_question` ADD COLUMN `answerScope` TEXT NOT NULL DEFAULT 'SESSION'")
        db.execSQL("ALTER TABLE `form_question` ADD COLUMN `isUnitIdentityComponent` INTEGER NOT NULL DEFAULT 0")

        // 3. Add nullable sessionUnitId on form_answer (FK).
        db.execSQL("ALTER TABLE `form_answer` ADD COLUMN `sessionUnitId` TEXT DEFAULT NULL REFERENCES `session_unit`(`localId`) ON UPDATE CASCADE ON DELETE CASCADE")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_form_answer_sessionUnitId` ON `form_answer` (`sessionUnitId`)")

        // 4. Add nullable sessionUnitId on specimen (FK).
        db.execSQL("ALTER TABLE `specimen` ADD COLUMN `sessionUnitId` TEXT DEFAULT NULL REFERENCES `session_unit`(`localId`) ON UPDATE CASCADE ON DELETE CASCADE")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_specimen_sessionUnitId` ON `specimen` (`sessionUnitId`)")
    }
}
```

> ⚠️ Some prior migrations used the "rename + create + copy + drop" technique to add a FK. The `ALTER TABLE ... ADD COLUMN ... REFERENCES` form **does not** install a FK on existing SQLite tables. If we want strict FK enforcement on `form_answer.sessionUnitId` and `specimen.sessionUnitId`, follow the `Migration_19_20_MakeHardwareIdColumnNullable.kt` pattern: rename to `_old`, create the new schema, `INSERT … SELECT`, drop old, recreate indexes. **For this feature, FK enforcement on the new column is desired** — implement the full table-rebuild for both `form_answer` and `specimen` in this migration. The snippet above is the shorter version for clarity.

Then register it in `app/src/main/java/com/vci/vectorcamapp/core/data/room/migrations/Migrations.kt`:

```kotlin
import com.vci.vectorcamapp.core.data.room.migrations.versions.MIGRATION_30_31_ADD_SESSION_UNITS
// ...
val ALL_MIGRATIONS = arrayOf(
    // existing migrations…
    MIGRATION_29_30_ADD_PREREQUISITE_TO_FORM_QUESTION,
    MIGRATION_30_31_ADD_SESSION_UNITS,   // NEW
)
```

### 3.7 New relations

Create `app/src/main/java/com/vci/vectorcamapp/core/data/room/entities/relations/SessionUnitWithAnswersRelation.kt`:

```kotlin
package com.vci.vectorcamapp.core.data.room.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.vci.vectorcamapp.core.data.room.entities.FormAnswerEntity
import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity

data class SessionUnitWithAnswersRelation(
    @Embedded val sessionUnitEntity: SessionUnitEntity,
    @Relation(parentColumn = "localId", entityColumn = "sessionUnitId")
    val answerEntities: List<FormAnswerEntity>,
)
```

Create `app/src/main/java/com/vci/vectorcamapp/core/data/room/entities/relations/SessionUnitWithSpecimensRelation.kt`:

```kotlin
package com.vci.vectorcamapp.core.data.room.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity
import com.vci.vectorcamapp.core.data.room.entities.SpecimenEntity

data class SessionUnitWithSpecimensRelation(
    @Embedded val sessionUnitEntity: SessionUnitEntity,
    @Relation(parentColumn = "localId", entityColumn = "sessionUnitId")
    val specimenEntities: List<SpecimenEntity>,
)
```

### 3.8 New DAO

> 🛑 **Out of date for PR 3.** Post Pre-PR 3 cleanup, `SessionUnitDao` is intentionally empty. The list below is reference material only — **do not** seed all of these in one go. Add each method in the PR 3 slice (3b / 3c / 3d) where its first call site lives. See the "Read this before writing any code in PR 3" callout in the Implementation Status section.

Create `app/src/main/java/com/vci/vectorcamapp/core/data/room/dao/SessionUnitDao.kt`:

```kotlin
package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity
import com.vci.vectorcamapp.core.data.room.entities.relations.SessionUnitWithAnswersRelation
import com.vci.vectorcamapp.core.data.room.entities.relations.SessionUnitWithSpecimensRelation
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SessionUnitDao {

    @Upsert
    suspend fun upsertSessionUnit(unit: SessionUnitEntity): Long

    @Delete
    suspend fun deleteSessionUnit(unit: SessionUnitEntity): Int

    @Query("SELECT * FROM session_unit WHERE localId = :unitId")
    suspend fun getSessionUnitById(unitId: UUID): SessionUnitEntity?

    @Query("SELECT * FROM session_unit WHERE sessionId = :sessionId ORDER BY unitOrder ASC")
    suspend fun getSessionUnitsForSession(sessionId: UUID): List<SessionUnitEntity>

    @Query("SELECT * FROM session_unit WHERE sessionId = :sessionId ORDER BY unitOrder ASC")
    fun observeSessionUnitsForSession(sessionId: UUID): Flow<List<SessionUnitEntity>>

    @Query("SELECT COALESCE(MAX(unitOrder), 0) FROM session_unit WHERE sessionId = :sessionId")
    suspend fun getMaxUnitOrderForSession(sessionId: UUID): Int

    @Query("SELECT COUNT(*) FROM session_unit WHERE sessionId = :sessionId")
    suspend fun countSessionUnitsForSession(sessionId: UUID): Int

    @Query("SELECT COUNT(*) FROM specimen WHERE sessionUnitId = :unitId")
    suspend fun countSpecimensForUnit(unitId: UUID): Int

    @Transaction
    @Query("SELECT * FROM session_unit WHERE sessionId = :sessionId ORDER BY unitOrder ASC")
    suspend fun getSessionUnitsWithAnswersForSession(sessionId: UUID): List<SessionUnitWithAnswersRelation>

    @Transaction
    @Query("SELECT * FROM session_unit WHERE sessionId = :sessionId ORDER BY unitOrder ASC")
    fun observeSessionUnitsWithAnswersForSession(sessionId: UUID): Flow<List<SessionUnitWithAnswersRelation>>

    @Transaction
    @Query("SELECT * FROM session_unit WHERE localId = :unitId")
    suspend fun getSessionUnitWithAnswers(unitId: UUID): SessionUnitWithAnswersRelation?
}
```

Then register provider in `app/src/main/java/com/vci/vectorcamapp/core/di/RoomDatabaseModule.kt`:

```kotlin
@Provides
fun provideSessionUnitDao(db: VectorCamDatabase): SessionUnitDao = db.sessionUnitDao
```

### 3.9 Update existing DAOs (small additions)

`app/src/main/java/com/vci/vectorcamapp/core/data/room/dao/FormAnswerDao.kt`: add

```kotlin
@Query("SELECT * FROM form_answer WHERE sessionUnitId = :sessionUnitId")
suspend fun getFormAnswersBySessionUnitId(sessionUnitId: UUID): List<FormAnswerEntity>

@Query("DELETE FROM form_answer WHERE sessionUnitId = :sessionUnitId")
suspend fun deleteFormAnswersForSessionUnit(sessionUnitId: UUID): Int
```

`app/src/main/java/com/vci/vectorcamapp/core/data/room/dao/SpecimenDao.kt`: add

```kotlin
@Query("SELECT COUNT(*) FROM specimen WHERE sessionUnitId = :sessionUnitId")
suspend fun countSpecimensForUnit(sessionUnitId: UUID): Int
```

---

## 4. Domain layer

### 4.1 Domain model `SessionUnit`

`app/src/main/java/com/vci/vectorcamapp/core/domain/model/SessionUnit.kt`:

```kotlin
package com.vci.vectorcamapp.core.domain.model

import java.util.UUID

data class SessionUnit(
    val localId: UUID,
    val sessionId: UUID,
    val remoteId: Int?,
    val unitOrder: Int,
    val createdAt: Long,
)
```

### 4.2 Extend existing models

`core/domain/model/FormQuestion.kt`:

```kotlin
data class FormQuestion(
    val id: Int,
    val label: String,
    val type: String,
    val required: Boolean,
    val prerequisite: FormQuestionPrerequisiteExpression?,
    val options: List<String>?,
    val order: Int?,
    val answerScope: String,                   // NEW
    val isUnitIdentityComponent: Boolean,      // NEW
)
```

`core/domain/model/FormAnswer.kt`: add `val sessionUnitId: UUID? = null` field (do not change the existing constructor order — append at the bottom).

`core/domain/model/Specimen.kt`: add `val sessionUnitId: UUID? = null`.

> Do **not** create an `AnswerScope` enum domain model. `answerScope` stays a `String` on the entity, DTO, and domain model — same pattern as `FormQuestion.type`. If you need constants for comparisons, declare them once near the resolver utility:
> ```kotlin
> object AnswerScopes {
>     const val SESSION = "SESSION"
>     const val SESSION_UNIT = "SESSION_UNIT"
> }
> ```

### 4.3 Repository

> 🛑 **Out of date for PR 3.** Post Pre-PR 3 cleanup, `SessionUnitRepository` is intentionally empty. The interface shown below is the eventual end-state, not a starting checklist. Add each method in the PR 3 slice (3b / 3c / 3d) where its first call site lives, mirroring the shape of the surrounding `SessionRepository` / `SpecimenRepository` methods.

`core/domain/repository/SessionUnitRepository.kt`:

```kotlin
package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SessionUnitRepository {
    suspend fun upsertSessionUnit(unit: SessionUnit): Result<Unit, RoomDbError>
    suspend fun getSessionUnitById(unitId: UUID): SessionUnit?
    suspend fun getSessionUnitsForSession(sessionId: UUID): List<SessionUnit>
    fun observeSessionUnitsForSession(sessionId: UUID): Flow<List<SessionUnit>>
    suspend fun getNextUnitOrder(sessionId: UUID): Int
    suspend fun countSessionUnits(sessionId: UUID): Int
    suspend fun countSpecimensForUnit(unitId: UUID): Int

    /** Deletes the unit only if it has no specimens. Returns true on delete, false on guard. */
    suspend fun deleteSessionUnitIfNoSpecimens(unit: SessionUnit): Boolean
}
```

`core/data/repository/SessionUnitRepositoryImplementation.kt` mirrors `SessionRepositoryImplementation`.

### 4.4 Mapper

`core/data/mappers/SessionUnitMapper.kt`:

```kotlin
package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity
import com.vci.vectorcamapp.core.domain.model.SessionUnit

fun SessionUnitEntity.toDomain() = SessionUnit(
    localId = localId, sessionId = sessionId, remoteId = remoteId,
    unitOrder = unitOrder, createdAt = createdAt,
)

fun SessionUnit.toEntity() = SessionUnitEntity(
    localId = localId, sessionId = sessionId, remoteId = remoteId,
    unitOrder = unitOrder, createdAt = createdAt,
)
```

Extend `FormQuestionMapper`, `FormAnswerMapper`, `SpecimenMapper` with the new fields. For `FormAnswer.toEntity(...)`, change the signature to also accept the `sessionUnitId: UUID?`:

```kotlin
fun FormAnswer.toEntity(sessionId: UUID, questionId: Int, sessionUnitId: UUID? = null) = FormAnswerEntity(
    localId = localId, remoteId = remoteId, sessionId = sessionId,
    sessionUnitId = sessionUnitId, questionId = questionId,
    value = value, dataType = dataType, submittedAt = submittedAt
)
```

Update `FormAnswerRepository.upsertFormAnswer(...)` to accept `sessionUnitId: UUID? = null` and propagate it. **Backwards-compatible**: existing callers (e.g. `IntakeViewModel`) pass nothing → defaults to null → identical legacy behavior.

### 4.5 Domain utilities (NOT use cases)

The previous draft had use cases for these — they are too small for that. Follow the existing pattern of `intake/domain/util/FormQuestionPrerequisiteEvaluator.kt`: declare `object`s with pure functions.

Create `app/src/main/java/com/vci/vectorcamapp/collection_batch/domain/util/CollectionBatchIdentityResolver.kt`:

```kotlin
package com.vci.vectorcamapp.collection_batch.domain.util

import com.vci.vectorcamapp.core.domain.model.FormQuestion

object CollectionBatchIdentityResolver {

    private const val SEPARATOR = " · "

    /**
     * Returns the derived bucket name for a unit, given the unit's answers keyed by questionId
     * and the form's questions. Considers only questions where
     * answerScope = SESSION_UNIT AND isUnitIdentityComponent = true, sorted by questionId.
     *
     * Returns an empty string if no identity components are present (caller decides fallback,
     * e.g. "Batch ${unitOrder}").
     */
    fun deriveBucketName(
        questions: List<FormQuestion>,
        unitAnswersByQuestionId: Map<Int, String>,
    ): String {
        val identityQuestions = questions
            .filter { it.answerScope == "SESSION_UNIT" && it.isUnitIdentityComponent }
            .sortedBy { it.id }

        if (identityQuestions.isEmpty()) return ""

        return identityQuestions.joinToString(SEPARATOR) { q ->
            normalize(unitAnswersByQuestionId[q.id].orEmpty())
        }
    }

    private fun normalize(value: String): String = value.trim()
}
```

Create `app/src/main/java/com/vci/vectorcamapp/collection_batch/domain/util/CollectionBatchIdentityValidator.kt`:

```kotlin
package com.vci.vectorcamapp.collection_batch.domain.util

import com.vci.vectorcamapp.core.domain.model.FormQuestion
import java.util.UUID

object CollectionBatchIdentityValidator {

    /**
     * Returns true if the proposed (draft) bucket name for `editingUnitId` would collide with any
     * existing unit's bucket name in `existingUnits`. Used to block duplicate identity within a
     * single session.
     *
     * existingUnits: map of unitLocalId -> that unit's answers keyed by questionId
     * editingUnitId: pass the localId of the unit being edited, or null when creating a new one.
     */
    fun wouldDuplicate(
        questions: List<FormQuestion>,
        draftAnswers: Map<Int, String>,
        existingUnits: Map<UUID, Map<Int, String>>,
        editingUnitId: UUID?,
    ): Boolean {
        val draft = CollectionBatchIdentityResolver.deriveBucketName(questions, draftAnswers)
        if (draft.isBlank()) return false

        return existingUnits.any { (unitId, answers) ->
            unitId != editingUnitId &&
                CollectionBatchIdentityResolver.deriveBucketName(questions, answers) == draft
        }
    }
}
```

These are pure functions, easily unit-tested, no DI required.

### 4.6 Hilt bindings

In `core/di/CoreRepositoryModule.kt`, add:

```kotlin
@Binds
@Singleton
abstract fun bindSessionUnitRepository(
    impl: SessionUnitRepositoryImplementation
): SessionUnitRepository
```

---

## 5. Generalize `hour_log/` + `add_hour/` into `collection_batch/`

The existing `hour_log/` + `add_hour/` packages already encode the right two-screen shape (dashboard-of-cards + per-batch form). We are lifting them into a single `collection_batch/` feature where the fields are form-driven instead of HLC-hardcoded. Treat this as a rename + generalization, not a rewrite — the screen flow, state/action/event pattern, navigation contract, and card layout all carry over.

### 5.1 What carries over (and how)

| Old | New | Notes |
| --- | --- | --- |
| `hour_log/presentation/HourLogScreen.kt` | `collection_batch/list/presentation/CollectionBatchListScreen.kt` | Same scaffold, same card grid, same `[+]` affordance. Cards now show derived bucket names instead of hardcoded hour ranges. |
| `hour_log/presentation/HourLogViewModel.kt` | `collection_batch/list/presentation/CollectionBatchListViewModel.kt` | Same `SavedStateHandle.toRoute(...)` pattern to read `sessionId`. Replaces hardcoded `HourTimeSlots` with `observeSessionUnitsForSession(sessionId)`. |
| `hour_log/presentation/components/HourSessionCard.kt` | `collection_batch/list/presentation/components/CollectionBatchCard.kt` | Visual structure preserved; label source becomes the identity-resolved string. |
| `add_hour/presentation/AddHourScreen.kt` | `collection_batch/form/presentation/CollectionBatchFormScreen.kt` | Same scaffold; hardcoded `TextEntryField` / `DropdownField` blocks become a loop over `SESSION_UNIT`-scoped form questions (see §8.2). |
| `add_hour/presentation/AddHourViewModel.kt` + `State`/`Action`/`Event` | `collection_batch/form/presentation/CollectionBatchFormViewModel.kt` + matching trio | Same MVI shape. |
| `hour_log/domain/model/HourSession.kt`, `HourTimeSlots.kt` | (no replacement — bucket identity is now form-derived; see `CollectionBatchIdentityResolver` in §7) | Hardcoded HLC slots no longer needed once identity comes from form questions. |

### 5.2 Files to delete (after the new ones land)

Once the new `collection_batch/` feature is wired through navigation and verified equivalent, the following can be removed:

- `app/src/main/java/com/vci/vectorcamapp/hour_log/` (entire directory).
- `app/src/main/java/com/vci/vectorcamapp/add_hour/` (entire directory).
- In `navigation/Destination.kt`, the `HourLog` and `AddHour` entries.
- In `navigation/NavGraph.kt`, the `composable<Destination.HourLog>` and `composable<Destination.AddHour>` blocks and their imports.
- In `intake/presentation/IntakeEvent.kt`, `NavigateToHourLogScreen`.

This is the last step of the slicing in §11 — not the first — so the legacy flow continues to work until the new one is fully in.

### 5.3 New feature directory layout

```
app/src/main/java/com/vci/vectorcamapp/collection_batch/
  domain/
    util/
      CollectionBatchIdentityResolver.kt
      CollectionBatchIdentityValidator.kt
  list/
    presentation/
      CollectionBatchListScreen.kt
      CollectionBatchListViewModel.kt
      CollectionBatchListState.kt
      CollectionBatchListAction.kt
      CollectionBatchListEvent.kt
      components/
        CollectionBatchCard.kt
  form/
    presentation/
      CollectionBatchFormScreen.kt
      CollectionBatchFormViewModel.kt
      CollectionBatchFormState.kt
      CollectionBatchFormAction.kt
      CollectionBatchFormEvent.kt
```

Naming rationale: we model after `complete_session/list/` + `complete_session/details/` which has the same shape (one feature, two related screens).

---

## 6. Strategy pattern for HLC routing

Today `IntakeViewModel` lines 309-314 hardcode:

```kotlin
val isHlc = session.collectionMethod == CollectionMethodOption.HUMAN_LANDING_CATCH.label
if (isHlc) {
    _events.send(IntakeEvent.NavigateToHourLogScreen(session.localId.toString()))
} else {
    _events.send(IntakeEvent.NavigateToImagingScreen)
}
```

Replace with a strategy mirroring `intake/domain/strategy/ProgramFormWorkflow`:

### 6.1 Strategy interface

`app/src/main/java/com/vci/vectorcamapp/intake/domain/strategy/CollectionMethodWorkflow.kt`:

```kotlin
package com.vci.vectorcamapp.intake.domain.strategy

import com.vci.vectorcamapp.navigation.Destination

interface CollectionMethodWorkflow {
    /** Returns the destination the Intake screen should navigate to after saving the session. */
    fun postIntakeDestination(sessionId: String): Destination
}
```

### 6.2 Concrete strategies

`intake/domain/strategy/concrete/DirectImagingWorkflow.kt`:

```kotlin
class DirectImagingWorkflow : CollectionMethodWorkflow {
    override fun postIntakeDestination(sessionId: String): Destination =
        Destination.Imaging(sessionUnitId = null)
}
```

`intake/domain/strategy/concrete/RepeatableUnitWorkflow.kt`:

```kotlin
class RepeatableUnitWorkflow : CollectionMethodWorkflow {
    override fun postIntakeDestination(sessionId: String): Destination =
        Destination.CollectionBatchList(sessionId = sessionId)
}
```

### 6.3 Factory

`intake/domain/strategy/CollectionMethodWorkflowFactory.kt`:

```kotlin
package com.vci.vectorcamapp.intake.domain.strategy

import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions.CollectionMethodOption
import com.vci.vectorcamapp.intake.domain.strategy.concrete.DirectImagingWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.concrete.RepeatableUnitWorkflow
import javax.inject.Inject

class CollectionMethodWorkflowFactory @Inject constructor() {
    fun create(collectionMethod: String): CollectionMethodWorkflow {
        return when (collectionMethod) {
            CollectionMethodOption.HUMAN_LANDING_CATCH.label -> RepeatableUnitWorkflow()
            else -> DirectImagingWorkflow()
        }
    }
}
```

> Constructor injection on the factory; no Hilt module needed for these (the existing `ProgramFormWorkflowFactory` is wired the same way — `@Inject lateinit var` field in `IntakeViewModel`).

### 6.4 Wire into `IntakeViewModel`

Inject the factory like the existing one:

```kotlin
@Inject
lateinit var collectionMethodWorkflowFactory: CollectionMethodWorkflowFactory
```

Replace the HLC-specific block (lines 309-314) with:

```kotlin
val workflow = collectionMethodWorkflowFactory.create(session.collectionMethod)
_events.send(IntakeEvent.NavigateAfterIntake(workflow.postIntakeDestination(session.localId.toString())))
```

In `intake/presentation/IntakeEvent.kt`, replace `NavigateToHourLogScreen` and `NavigateToImagingScreen` with one event:

```kotlin
data class NavigateAfterIntake(val destination: Destination) : IntakeEvent
```

And update the corresponding `composable<Destination.Intake>` block in `NavGraph.kt` to call `navController.navigate(event.destination)`.

### 6.5 Lock collection method when units exist

Inside `IntakeViewModel`, when loading existing state for an in-progress session (re-entry from incomplete list), call `sessionUnitRepository.countSessionUnits(sessionId)`. If `> 0`, set a new `IntakeState.isCollectionMethodLocked = true` flag. In `IntakeScreen.kt`, when this flag is true, render the collection-method dropdown as disabled with a small helper text:

> "Cannot change collection method — collection batches have already been created for this session."

---

## 7. CollectionBatchList screen

### 7.1 State

`collection_batch/list/presentation/CollectionBatchListState.kt`:

```kotlin
package com.vci.vectorcamapp.collection_batch.list.presentation

import java.util.UUID

data class CollectionBatchListState(
    val isLoading: Boolean = true,
    val sessionId: String = "",
    val units: List<CollectionBatchCardData> = emptyList(),
)

data class CollectionBatchCardData(
    val localId: UUID,
    val unitOrder: Int,
    val bucketName: String,          // derived; falls back to "Batch ${unitOrder}" when empty
    val specimenCount: Int,
    val createdAt: Long,
    val canDelete: Boolean,          // specimenCount == 0
)
```

### 7.2 Action / Event

```kotlin
// CollectionBatchListAction.kt
sealed interface CollectionBatchListAction {
    data object ReturnToPreviousScreen : CollectionBatchListAction
    data object AddCollectionBatch : CollectionBatchListAction
    data class OpenCollectionBatchImaging(val unitId: UUID) : CollectionBatchListAction
    data class EditCollectionBatch(val unitId: UUID) : CollectionBatchListAction
    data class DeleteCollectionBatch(val unitId: UUID) : CollectionBatchListAction
    data object UploadSession : CollectionBatchListAction
}

// CollectionBatchListEvent.kt
sealed interface CollectionBatchListEvent {
    data object NavigateBackToLandingScreen : CollectionBatchListEvent
    data class NavigateToCollectionBatchForm(val sessionId: String, val unitId: String?) : CollectionBatchListEvent
    data class NavigateToImaging(val sessionUnitId: String) : CollectionBatchListEvent
}
```

### 7.3 ViewModel responsibilities

`CollectionBatchListViewModel`:

- Read `sessionId` from `SavedStateHandle.toRoute<Destination.CollectionBatchList>()` (like `HourLogViewModel`).
- On `init`, also read `CurrentSessionCache.getSession()` to sanity-check the `sessionId` matches the cached session. (No `currentSessionUnitId` cache — point confirmed.)
- Combine three sources:
  1. `sessionUnitRepository.observeSessionUnitsForSession(sessionId)`
  2. for each unit, look up its `FormAnswer`s via `SessionUnitDao.getSessionUnitWithAnswers(unitId)`
  3. `formQuestionRepository.getQuestionsByFormId(formId)` once at startup (form known from session → program).
- For each `SessionUnitEntity`, build a `CollectionBatchCardData`:
  - `bucketName = CollectionBatchIdentityResolver.deriveBucketName(formQuestions, answersByQuestionId).ifBlank { "Batch ${unit.unitOrder}" }`
  - `specimenCount = sessionUnitRepository.countSpecimensForUnit(unit.localId)`
  - `canDelete = specimenCount == 0`
- Actions:
  - `AddCollectionBatch` → `NavigateToCollectionBatchForm(sessionId, unitId = null)`.
  - `OpenCollectionBatchImaging(id)` → `NavigateToImaging(id.toString())`.
  - `EditCollectionBatch(id)` → `NavigateToCollectionBatchForm(sessionId, unitId = id.toString())`.
  - `DeleteCollectionBatch(id)` → call `repo.deleteSessionUnitIfNoSpecimens(...)`; if false, `emitError(...)` "Cannot delete: this batch has specimens. Delete its images in Imaging first."
  - `UploadSession` → mark session complete and enqueue the upload chain via the existing `WorkManagerRepository.enqueueSessionUpload(sessionId, siteId)` call (mirror the call site currently in `ImagingViewModel` around line 263; find it via grep `enqueueSessionUpload`). This worker chain runs `MetadataUploadWorker` then `ImageUploadWorker`.
  - `ReturnToPreviousScreen` → `NavigateBackToLandingScreen`.

### 7.4 Screen

`CollectionBatchListScreen.kt` carries over directly from `HourLogScreen`, with `HourSessionCard` becoming `CollectionBatchCard`. The card UI stays the same conceptually:

- Title: `bucketName`
- Pill: `"Specimen Count: $specimenCount"`
- Trailing arrow icon → `OpenCollectionBatchImaging`
- Body click → `EditCollectionBatch`
- Created / Last Updated timestamps in the lower section
- Long-press or overflow menu → `DeleteCollectionBatch` (the visual decision can mirror existing patterns elsewhere in the codebase)

Header:
- Title: `"Collection Batches"` (subtitle `"Tap a card to edit, the arrow to image, or + to add"`)
- Leading icon: `+` → `AddCollectionBatch`
- Trailing icon: cloud-upload → `UploadSession`

---

## 8. CollectionBatchForm screen

### 8.1 State

`collection_batch/form/presentation/CollectionBatchFormState.kt`:

```kotlin
data class CollectionBatchFormState(
    val isLoading: Boolean = true,
    val sessionId: String = "",
    val editingUnitId: String? = null,           // null = create mode
    val identityQuestions: List<FormQuestion> = emptyList(),  // rendered FIRST
    val otherUnitQuestions: List<FormQuestion> = emptyList(), // rendered after
    val answers: Map<Int, String> = emptyMap(),  // questionId -> value
    val errorsByQuestionId: Map<Int, FormValidationError> = emptyMap(),
    val duplicateIdentityError: String? = null,
)
```

### 8.2 ViewModel responsibilities

`CollectionBatchFormViewModel`:

1. **Load** (on `init`):
   - Read `sessionId` and optional `unitId` from `SavedStateHandle.toRoute<Destination.CollectionBatchForm>()`.
   - Look up the active `FormQuestion`s for the session (already available via `programFormWorkflow` pattern — `formQuestionRepository.getQuestionsByFormId(formId)`).
   - Partition by `answerScope == "SESSION_UNIT"`. Within that, partition by `isUnitIdentityComponent`. Result:
     - `identityQuestions` — sorted by `id` (so the derived bucket name is stable), then by `order`.
     - `otherUnitQuestions` — sorted by `order`.
   - **Identity questions must render first** in the screen (per the spec).
   - If `editingUnitId != null`, preload answers from `formAnswerDao.getFormAnswersBySessionUnitId(...)`.
   - Otherwise pre-populate `answers` with empty strings for required questions and leave the rest blank.

2. **Update**: an `EnterAnswer(questionId: Int, value: String)` action updates `answers`. Live-clear that question's error.

3. **Save** (the user taps the `Confirm` button):
   - Validate required questions (re-use the existing `intake/domain/util/FormValidationError` machinery — same error types).
   - Fetch other units' answers for this session: `dao.getSessionUnitsWithAnswersForSession(sessionId)` → map `unitId → answersByQuestionId`.
   - Call `CollectionBatchIdentityValidator.wouldDuplicate(questions, answers, existingAnswersMap, editingUnitId)`. If `true`, set `duplicateIdentityError = "A collection batch with this identity already exists. Please change one of the highlighted fields."` and return (do not save).
   - Inside `transactionHelper.runAsTransaction { ... }`:
     - If create mode: build a `SessionUnit` with new `UUID.randomUUID()`, `unitOrder = repo.getNextUnitOrder(sessionId) + 1`, `createdAt = now`.
     - If edit mode: load existing unit and reuse it as-is (the FormAnswer rows are what change in edit mode).
     - `repo.upsertSessionUnit(unit)`.
     - For each answered question, `formAnswerRepository.upsertFormAnswer(answer, sessionId, questionId, sessionUnitId = unit.localId)`. Answers for `SESSION_UNIT`-scoped questions only — `SESSION`-scoped answers are not edited here.
   - On success, `_events.send(CollectionBatchFormEvent.NavigateToImagingScreen(unit.localId.toString()))`.

### 8.3 Action / Event

```kotlin
// CollectionBatchFormAction.kt
sealed interface CollectionBatchFormAction {
    data object ReturnToPreviousScreen : CollectionBatchFormAction
    data class EnterAnswer(val questionId: Int, val value: String) : CollectionBatchFormAction
    data object Confirm : CollectionBatchFormAction
}

// CollectionBatchFormEvent.kt
sealed interface CollectionBatchFormEvent {
    data object NavigateBackToPreviousScreen : CollectionBatchFormEvent
    data class NavigateToImagingScreen(val sessionUnitId: String) : CollectionBatchFormEvent
}
```

### 8.4 Screen

Mirror `AddHourScreen.kt`. Instead of the HLC-specific `TextEntryField`/`DropdownField` blocks, iterate twice over the question lists:

```kotlin
items(state.identityQuestions, key = { it.id }) { q ->
    DynamicFormField(
        question = q,
        value = state.answers[q.id].orEmpty(),
        onValueChange = { onAction(CollectionBatchFormAction.EnterAnswer(q.id, it)) },
        error = state.errorsByQuestionId[q.id],
    )
}

items(state.otherUnitQuestions, key = { it.id }) { q ->
    DynamicFormField(...)
}
```

Display `state.duplicateIdentityError` as a banner above the Confirm button (use the existing error banner styling, e.g. `PracticeSessionWarningBanner` as a layout reference).

`DynamicFormField` already exists at `intake/presentation/components/DynamicFormField.kt` and supports `text`, `number`, `boolean`, `date`, `select` — reuse as-is.

---

## 9. Imaging screen scoping

### 9.1 Destination

In `navigation/Destination.kt`:

```kotlin
@Serializable
data class Imaging(val sessionUnitId: String? = null) : Destination

@Serializable
data class CollectionBatchList(val sessionId: String) : Destination

@Serializable
data class CollectionBatchForm(val sessionId: String, val unitId: String? = null) : Destination
```

> ⚠️ This changes `Imaging` from `data object` to `data class`. All existing `navController.navigate(Destination.Imaging)` call sites must be updated to `navController.navigate(Destination.Imaging())` (single change in `NavGraph.kt` and in `IntakeEvent`/strategy plumbing).

### 9.2 NavGraph wiring

In `navigation/NavGraph.kt`, replace the deleted `HourLog`/`AddHour` blocks with `CollectionBatchList` and `CollectionBatchForm` composables, each `hiltViewModel<...>()` + `ObserveAsEvents` + `BaseScaffold` body exactly mirroring the existing destinations.

For `composable<Destination.CollectionBatchList>`, the events map to:

```kotlin
CollectionBatchListEvent.NavigateBackToLandingScreen ->
    navController.popBackStack(Destination.Landing, false)

is CollectionBatchListEvent.NavigateToCollectionBatchForm ->
    navController.navigate(Destination.CollectionBatchForm(event.sessionId, event.unitId))

is CollectionBatchListEvent.NavigateToImaging ->
    navController.navigate(Destination.Imaging(sessionUnitId = event.sessionUnitId))
```

For `composable<Destination.CollectionBatchForm>`:

```kotlin
CollectionBatchFormEvent.NavigateBackToPreviousScreen ->
    navController.popBackStack()

is CollectionBatchFormEvent.NavigateToImagingScreen ->
    navController.navigate(Destination.Imaging(sessionUnitId = event.sessionUnitId)) {
        popUpTo(Destination.CollectionBatchForm::class) { inclusive = true }
    }
```

For the existing `composable<Destination.Imaging>` block, when handling `ImagingEvent.NavigateBackToLandingScreen` we need to branch: if the current `Imaging.sessionUnitId != null`, instead pop back to `CollectionBatchList` (popBackStack on `Destination.CollectionBatchList`); otherwise behave exactly as today. The simplest implementation: add a second event variant `ImagingEvent.NavigateBackToCollectionBatchList`.

### 9.3 ImagingViewModel changes

`app/src/main/java/com/vci/vectorcamapp/imaging/presentation/ImagingViewModel.kt`:

- Read `sessionUnitId: String?` from `SavedStateHandle.toRoute<Destination.Imaging>()`.
- Pass `sessionUnitId` through whenever a `SpecimenEntity` is persisted. `SpecimenRepository` already exposes `insertSpecimen(specimen, sessionId)` and `updateSpecimen(specimen, sessionId)`; the new field rides on `Specimen` itself (see §4.4) so no signature change is needed — just ensure `Specimen.sessionUnitId` is set by `ImagingViewModel` before each insert/update.
- When `sessionUnitId != null`:
  - Hide the "Complete session / upload" UI (the cloud-upload icon and any "Submit" button in `ImagingScreen.kt`). State flag: `val isUnitScoped: Boolean = sessionUnitId != null`.
  - On back: emit `ImagingEvent.NavigateBackToCollectionBatchList` instead of `NavigateBackToLandingScreen`.
- When `sessionUnitId == null` (legacy PSC/LTC flow): no behavioral change.

`ImagingState`: add `val isUnitScoped: Boolean = false`.

`ImagingScreen.kt`: wrap submit/upload-related composables with `if (!state.isUnitScoped)`.

---

## 10. Sync to backend (MetadataUploadWorker)

We mirror the existing pattern **exactly**: `GET` by frontendId → if `NOT_FOUND` then `POST` → take the response → `upsert` it locally to capture `remoteId`. Same as `syncSessionIfNeeded` in `MetadataUploadWorker.kt` (lines 327-406).

### 10.1 New DTOs

`core/data/dto/session_unit/SessionUnitDto.kt`:

```kotlin
package com.vci.vectorcamapp.core.data.dto.session_unit

import com.vci.vectorcamapp.core.data.dto.serializers.UuidSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SessionUnitDto(
    val id: Int? = null,                              // backend remoteId
    @Serializable(with = UuidSerializer::class)
    val frontendId: UUID = UUID(0, 0),
    val sessionId: Int = -1,                          // backend session id
    val unitOrder: Int = 0,
    val createdAt: Long = 0L,
)
```

`core/data/dto/session_unit/PostSessionUnitRequestDto.kt`:

```kotlin
@Serializable
data class PostSessionUnitRequestDto(
    @Serializable(with = UuidSerializer::class)
    val frontendId: UUID,
    val unitOrder: Int,
    val createdAt: Long,
)
```

`core/data/dto/session_unit/PostSessionUnitResponseDto.kt`:

```kotlin
@Serializable
data class PostSessionUnitResponseDto(
    val message: String = "",
    val unit: SessionUnitDto = SessionUnitDto(),
)
```

### 10.2 New data source

`core/domain/network/api/SessionUnitDataSource.kt`:

```kotlin
package com.vci.vectorcamapp.core.domain.network.api

import com.vci.vectorcamapp.core.data.dto.session_unit.PostSessionUnitResponseDto
import com.vci.vectorcamapp.core.data.dto.session_unit.SessionUnitDto
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import java.util.UUID

interface SessionUnitDataSource {
    suspend fun postSessionUnit(
        unit: SessionUnit,
        sessionRemoteId: Int,
    ): Result<PostSessionUnitResponseDto, NetworkError>

    suspend fun getSessionUnitByFrontendId(
        sessionRemoteId: Int,
        localId: UUID,
    ): Result<SessionUnitDto, NetworkError>
}
```

`core/data/network/api/RemoteSessionUnitDataSource.kt`: copy `RemoteSessionDataSource.kt` structure. Endpoints follow the SRS:

- `POST sessions/{sessionRemoteId}/units` with `PostSessionUnitRequestDto` body, response `PostSessionUnitResponseDto`.
- `GET sessions/{sessionRemoteId}/units/{localId}` returning `SessionUnitDto`.

Register the Hilt binding in `core/di/DataSourceModule.kt`:

```kotlin
@Binds
@Singleton
abstract fun bindSessionUnitDataSource(
    remoteSessionUnitDataSource: RemoteSessionUnitDataSource,
): SessionUnitDataSource
```

### 10.3 Extend `FormQuestionDto`, `FormAnswerDto`, `SpecimenDto`

- `FormQuestionDto`: add `val answerScope: String = "SESSION"`, `val isUnitIdentityComponent: Boolean = false`. Update `FormQuestionMapper.toEntity()` / `toDomain()` / DTO `.toDomain()` to propagate.
- `FormAnswerDto`: add `val sessionUnitId: Int? = null` (backend remoteId of the unit).
- `SpecimenDto`: add `val sessionUnitId: Int? = null`.

Adjust `RemoteFormAnswerDataSource` and `RemoteSpecimenDataSource` POST bodies to include `sessionUnitId` (the synced unit's `remoteId`) where appropriate. **Pass `null` for legacy PSC/LTC flows so they continue unchanged.**

### 10.4 `MetadataUploadWorker.doWork()` additions

`app/src/main/java/com/vci/vectorcamapp/core/data/upload/metadata/MetadataUploadWorker.kt`:

After `syncSessionIfNeeded(...)` succeeds (around line 138) and **before** `syncFormAnswersIfNeeded` / `syncSpecimenIfNeeded`, insert:

```kotlin
// Sync collection batches / session units (must come before form answers and specimens because
// both reference sessionUnitId via the unit's remoteId).
val syncedUnitsByLocalId: Map<UUID, SessionUnit> = run {
    val localUnits = sessionUnitRepository.getSessionUnitsForSession(syncedSession.localId)
    val result = mutableMapOf<UUID, SessionUnit>()
    for (localUnit in localUnits) {
        when (val r = syncSessionUnitIfNeeded(localUnit, syncedSession.remoteId)) {
            is DomainResult.Success -> result[localUnit.localId] = r.data
            is DomainResult.Error -> return retryOrFailure(r.error.toString(context))
        }
    }
    result
}
```

Add the helper `syncSessionUnitIfNeeded` modeled after `syncSessionIfNeeded`:

```kotlin
private suspend fun syncSessionUnitIfNeeded(
    localUnit: SessionUnit,
    syncedSessionRemoteId: Int,
): DomainResult<SessionUnit, NetworkError> {
    return try {
        val remoteDto = when (val r = sessionUnitDataSource.getSessionUnitByFrontendId(
            syncedSessionRemoteId, localUnit.localId
        )) {
            is DomainResult.Success -> r.data
            is DomainResult.Error -> when (r.error) {
                NetworkError.NOT_FOUND -> {
                    when (val post = sessionUnitDataSource.postSessionUnit(localUnit, syncedSessionRemoteId)) {
                        is DomainResult.Success -> post.data.unit
                        is DomainResult.Error -> return DomainResult.Error(post.error)
                    }
                }
                else -> return DomainResult.Error(r.error)
            }
        }

        val remoteUnit = localUnit.copy(remoteId = remoteDto.id)
        // Persist any backend-assigned changes locally (notably remoteId).
        sessionUnitRepository.upsertSessionUnit(remoteUnit)
        DomainResult.Success(remoteUnit)
    } catch (e: IOException) {
        DomainResult.Error(NetworkError.NO_INTERNET)
    } catch (e: Exception) {
        DomainResult.Error(NetworkError.UNKNOWN_ERROR)
    }
}
```

Inject `sessionUnitRepository: SessionUnitRepository` and `sessionUnitDataSource: SessionUnitDataSource` into the worker's `@AssistedInject` constructor.

Update `syncFormAnswersIfNeeded(...)` to also send `sessionUnitId = syncedUnitsByLocalId[localAnswer.sessionUnitId]?.remoteId` for each posted answer. Update the local upsert path to write back the unit's local UUID from the response (the response's `sessionUnitId: Int?` is the remoteId; we already have the unit's `localId` keyed by it).

Update `syncSpecimenIfNeeded(...)` similarly: pass the unit's remoteId on POST; persist the unit's local UUID locally on response.

> **Symmetry rule**: every sync helper follows the same shape — `GET` by frontendId, on `NOT_FOUND` `POST`, then `upsert` the response locally so `remoteId` (and any backend-canonical fields) are persisted to Room. Do not deviate from this shape; it is the only way our offline-first state stays consistent.

### 10.5 CollectionBatchList "Upload" action

In `CollectionBatchListViewModel.onAction(UploadSession)`:

1. Call `sessionRepository.markSessionAsComplete(sessionId)`.
2. Call `workManagerRepository.enqueueSessionUpload(sessionId, siteId)` (same call as the existing imaging-screen submit at `ImagingViewModel.kt` line 263). This kicks off the metadata-then-images worker chain.
3. Emit `CollectionBatchListEvent.NavigateBackToLandingScreen` on success.

Re-enqueue is safe because the worker is idempotent (`GET` then `POST`).

---

## 11. Step-by-step build order (suggested PR slicing)

To keep changes shippable in slices, recommended ordering:

1. **PR 1 — Schema & domain plumbing** ✅ Merged
   - Entities, DAO, migration, DB version bump, mappers, domain model, repo + impl, Hilt bindings.
   - No behavioral change yet; existing flows compile and pass.
   - See **Implementation Status** at the top of this document for the exact list of files touched and deviations from this plan.

2. **PR 2 — Strategy + IntakeViewModel refactor** ✅ Merged
   - Add `CollectionMethodWorkflow` + factory (`SingleBatchWorkflow` / `MultipleBatchWorkflow`).
   - Replace HLC branch in `IntakeViewModel` with `NavigateAfterIntake(destination)`; HLC still navigates to `HourLog`, others to `Imaging`.
   - See **Implementation Status** at the top of this document for the exact list of files touched and deviations from this plan.

3. **PR 3 — collection_batch feature (UI + VMs)** — sliced into **3a / 3b / 3c / 3d**, see the "Next up — PR 3" section above. Build incrementally; do not pre-populate `SessionUnitRepository` / `SessionUnitDao` with the full method set from §3.8 / §4.3 — add functions only as their call sites land.
   - **3a**: Destination contract (`Imaging` → `data class`, add `CollectionBatchList` / `CollectionBatchForm`), nav stubs, strategy concretes flipped.
   - **3b**: `CollectionBatchList` screen + VM; grow `SessionUnitDao` / `SessionUnitRepository` with only the methods this VM needs.
   - **3c**: `CollectionBatchForm` screen + VM + `CollectionBatchIdentityResolver` / `CollectionBatchIdentityValidator`; extend `FormAnswerRepository.upsertFormAnswer` with `sessionUnitId` here (first caller).
   - **3d**: `ImagingViewModel` / `ImagingScreen` honor `sessionUnitId`, hide submit when scoped, propagate `Specimen.sessionUnitId`.

4. **PR 4 — Retire legacy `hour_log/` + `add_hour/`**
   - Now that the generalized `collection_batch/` feature is in place and wired, remove the superseded packages, nav destinations, and event variants. See §5.2 for the exact list.

5. **PR 5 — Sync wiring**
   - DTOs, RemoteSessionUnitDataSource, worker changes, `sessionUnitId` plumbing in FormAnswer/Specimen DTOs.

6. **PR 6 — Lock collection method when units exist**
   - Disabled dropdown + helper text in `IntakeScreen`.

Each slice is independently mergeable and behind-the-scenes until PR 3 wires the new screens in.

---

## 12. Test plan

Pure-Kotlin JVM tests (preferred):

- `CollectionBatchIdentityResolverTest`
  - empty identity questions → empty string
  - single identity question → its value
  - multiple identity questions → joined in `id` order with `" · "`
  - missing answer for an identity question → blank slot
  - normalization trims whitespace

- `CollectionBatchIdentityValidatorTest`
  - no other units → false
  - other unit with identical bucket name → true
  - other unit with same identity but different non-identity answer → true (still duplicate)
  - editing-mode excluding self → false
  - draft with blank derived name → false (we don't block empty identities at the validator; required-field validation handles that)

Instrumented Room tests:

- Migration `30 → 31` over a database snapshot containing PSC/LTC data: assert old rows have `sessionUnitId = null` and queries still work.
- `SessionUnitDao` round-trip: insert unit, query by session, count specimens, max unit order.
- Specimen FK cascade: deleting a unit deletes its specimens (and via existing FK chain, their images).

Manual QA:

- PSC and LTC flows end-to-end (no regression).
- HLC: create 2 units (Indoor + Outdoor at same hour) → accepted.
- HLC: attempt to create 2 Indoor units at same hour → blocked with duplicate-identity banner.
- HLC: image a unit, return to dashboard, delete it → blocked. Delete its specimens, then delete → succeeds.
- HLC: tap upload → session syncs; verify on backend that session_units, form_answers (with sessionUnitId), and specimens (with sessionUnitId) all reference the correct remote ids.
- Re-enter an in-progress HLC session via incomplete-sessions list → collection method dropdown is locked.

---

## 13. Files added / modified / deleted (final checklist)

### Added (~25 files)

```
core/data/room/entities/SessionUnitEntity.kt
core/data/room/entities/relations/SessionUnitWithAnswersRelation.kt
core/data/room/entities/relations/SessionUnitWithSpecimensRelation.kt
core/data/room/dao/SessionUnitDao.kt
core/data/room/migrations/versions/Migration_30_31_AddSessionUnits.kt
core/data/mappers/SessionUnitMapper.kt
core/domain/model/SessionUnit.kt
core/domain/repository/SessionUnitRepository.kt
core/data/repository/SessionUnitRepositoryImplementation.kt
core/data/dto/session_unit/SessionUnitDto.kt
core/data/dto/session_unit/PostSessionUnitRequestDto.kt
core/data/dto/session_unit/PostSessionUnitResponseDto.kt
core/domain/network/api/SessionUnitDataSource.kt
core/data/network/api/RemoteSessionUnitDataSource.kt

intake/domain/strategy/CollectionMethodWorkflow.kt
intake/domain/strategy/CollectionMethodWorkflowFactory.kt
intake/domain/strategy/concrete/DirectImagingWorkflow.kt
intake/domain/strategy/concrete/RepeatableUnitWorkflow.kt

collection_batch/domain/util/CollectionBatchIdentityResolver.kt
collection_batch/domain/util/CollectionBatchIdentityValidator.kt
collection_batch/list/presentation/CollectionBatchListScreen.kt
collection_batch/list/presentation/CollectionBatchListViewModel.kt
collection_batch/list/presentation/CollectionBatchListState.kt
collection_batch/list/presentation/CollectionBatchListAction.kt
collection_batch/list/presentation/CollectionBatchListEvent.kt
collection_batch/list/presentation/components/CollectionBatchCard.kt
collection_batch/form/presentation/CollectionBatchFormScreen.kt
collection_batch/form/presentation/CollectionBatchFormViewModel.kt
collection_batch/form/presentation/CollectionBatchFormState.kt
collection_batch/form/presentation/CollectionBatchFormAction.kt
collection_batch/form/presentation/CollectionBatchFormEvent.kt
```

### Modified

```
core/data/room/VectorCamDatabase.kt                    # version 30→31, add entity & DAO
core/data/room/migrations/Migrations.kt                # register new migration
core/data/room/entities/FormQuestionEntity.kt          # +answerScope, +isUnitIdentityComponent
core/data/room/entities/FormAnswerEntity.kt            # +sessionUnitId (FK)
core/data/room/entities/SpecimenEntity.kt              # +sessionUnitId (FK)
core/data/room/dao/FormAnswerDao.kt                    # +query by sessionUnitId
core/data/room/dao/SpecimenDao.kt                      # +count specimens for unit
core/data/mappers/FormQuestionMapper.kt                # propagate new fields
core/data/mappers/FormAnswerMapper.kt                  # propagate sessionUnitId
core/data/mappers/SpecimenMapper.kt                    # propagate sessionUnitId
core/domain/model/FormQuestion.kt                      # +answerScope, +isUnitIdentityComponent
core/domain/model/FormAnswer.kt                        # +sessionUnitId
core/domain/model/Specimen.kt                          # +sessionUnitId
core/domain/repository/FormAnswerRepository.kt         # upsert signature +sessionUnitId
core/data/repository/FormAnswerRepositoryImplementation.kt
core/data/dto/form_question/FormQuestionDto.kt         # +answerScope, +isUnitIdentityComponent
core/data/dto/form_answer/FormAnswerDto.kt             # +sessionUnitId
core/data/dto/specimen/SpecimenDto.kt                  # +sessionUnitId
core/di/CoreRepositoryModule.kt                        # bind SessionUnitRepository
core/di/DataSourceModule.kt                            # bind SessionUnitDataSource
core/di/RoomDatabaseModule.kt                          # provide SessionUnitDao
core/data/upload/metadata/MetadataUploadWorker.kt      # syncSessionUnitsIfNeeded + plumb sessionUnitId
core/data/network/api/RemoteFormAnswerDataSource.kt    # forward sessionUnitId on POST
core/data/network/api/RemoteSpecimenDataSource.kt      # forward sessionUnitId on POST

navigation/Destination.kt                              # +CollectionBatchList, +CollectionBatchForm; Imaging→data class with sessionUnitId
navigation/NavGraph.kt                                 # remove HourLog/AddHour, add new composables, propagate sessionUnitId

intake/presentation/IntakeViewModel.kt                 # use CollectionMethodWorkflow
intake/presentation/IntakeEvent.kt                     # replace events with NavigateAfterIntake
intake/presentation/IntakeState.kt                     # +isCollectionMethodLocked
intake/presentation/IntakeScreen.kt                    # disable dropdown when locked

imaging/presentation/ImagingViewModel.kt               # read sessionUnitId, scope persistence, alter back behavior
imaging/presentation/ImagingState.kt                   # +isUnitScoped
imaging/presentation/ImagingScreen.kt                  # hide submit UI when scoped
imaging/presentation/ImagingEvent.kt                   # +NavigateBackToCollectionBatchList
```

### Retired (superseded by `collection_batch/`)

```
hour_log/   (entire directory — replaced by collection_batch/list/)
add_hour/   (entire directory — replaced by collection_batch/form/)
```

---

## 14. Glossary of file paths referenced

| Pattern | File |
| --- | --- |
| Existing strategy interface | `intake/domain/strategy/ProgramFormWorkflow.kt` |
| Existing strategy factory | `intake/domain/strategy/ProgramFormWorkflowFactory.kt` |
| Existing utility object | `intake/domain/util/FormQuestionPrerequisiteEvaluator.kt` |
| Existing dynamic form field | `intake/presentation/components/DynamicFormField.kt` |
| Existing two-screen feature shape | `complete_session/list/` + `complete_session/details/` |
| Existing sync worker (template) | `core/data/upload/metadata/MetadataUploadWorker.kt` |
| Existing entity/migration patterns | `core/data/room/entities/SessionEntity.kt`, `core/data/room/migrations/versions/Migration_19_20_MakeHardwareIdColumnNullable.kt` |
| Existing single-method strategy call site to replace | `intake/presentation/IntakeViewModel.kt` lines 309-314 |

When in doubt, mirror these files exactly. Consistency with existing patterns is more important than cleverness.
