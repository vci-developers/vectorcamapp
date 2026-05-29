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

### Next up — PR 3: `collection_batch/` feature (UI + VMs) + nav + Imaging scoping

> ## ⚠️ Read this before writing any code in PR 3
>
> **DO NOT start PR 3 by dumping all the repository and DAO functions we will eventually need.** The Pre-PR 3 cleanup deliberately emptied `SessionUnitRepository`, `SessionUnitRepositoryImplementation`, and `SessionUnitDao` (and pruned other dead methods across the core layer) precisely so this feature can grow function-by-function on demand.
>
> The rules for PR 3:
>
> 1. **Add repository / DAO methods one at a time, and only when the call site that needs them exists in the same diff.** No speculative `observeX`, `getMaxX`, `countX`, etc. up front.
> 2. **Disregard the function lists in §3.8 and §4.3 of this plan.** Those were drafted when the layer was assumed full. The empty contracts post-cleanup are the new starting point; §3.8 / §4.3 are reference material, not a checklist.
> 3. **Match existing codebase conventions every time.** When you add a new repo method, mirror the shape of nearby `SessionRepository` / `SpecimenRepository` methods (e.g. `Result<Unit, RoomDbError>` returns, `observe*` for Flow, `get*ById` for suspend, `fun toEntity(sessionId: UUID)` mapper threading). Consistency over cleverness — this codebase has strong patterns and PR 3 should not introduce new ones.
> 4. **Slice PR 3 into smaller PRs.** A single PR carrying two new screens, two new viewmodels, two domain utilities, an `Imaging` destination contract change, and a `FormAnswerRepository` signature change is too large to review well. The recommended slicing is below.

**Scope.** §5 (file migration), §7 (list screen), §8 (form screen), §9 (imaging scoping), plus the two one-line concrete-strategy flips from PR 2:

- `SingleBatchWorkflow.postIntakeDestination` → `Destination.Imaging()` (after `Imaging` becomes a `data class`).
- `MultipleBatchWorkflow.postIntakeDestination` → `Destination.CollectionBatchList(sessionId.toString())`.

**Recommended slicing of PR 3.** Land these as separate PRs in order — each is independently reviewable, leaves `master` in a working state, and surfaces a single concern:

- **PR 3a — Destination contract + nav scaffold + strategy flip.** Change `Destination.Imaging` from `data object` to `data class Imaging(val sessionUnitId: String? = null)`. Add `Destination.CollectionBatchList(sessionId: String)` and `Destination.CollectionBatchForm(sessionId: String, unitId: String? = null)`. Flip `SingleBatchWorkflow` / `MultipleBatchWorkflow` to point at the new destinations. Add empty `composable<...>` stubs in `NavGraph.kt` that render a placeholder (or temporarily route back to legacy `HourLog` until 3b lands). No repo or DAO methods added in this slice. Update all existing `Destination.Imaging` call sites to `Destination.Imaging()`.
- **PR 3b — `CollectionBatchList` screen + VM.** Add the list-side files under `collection_batch/list/presentation/`. **Add to `SessionUnitDao` / `SessionUnitRepository` only the methods the ViewModel actually calls in this diff** — most likely one observe-style query for units in a session, and whatever count is needed for the card. Do not pre-add edit/delete methods until the actions that consume them exist.
- **PR 3c — `CollectionBatchForm` screen + VM + identity utilities.** Add `collection_batch/form/presentation/` and `collection_batch/domain/util/{CollectionBatchIdentityResolver,CollectionBatchIdentityValidator}.kt`. Add only the repo / DAO methods this VM actually invokes (e.g. upsert + the lookup needed for edit mode + the cross-unit duplicate check). Extend `FormAnswerRepository.upsertFormAnswer` with the `sessionUnitId` parameter here, since this is the first caller that needs it.
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
