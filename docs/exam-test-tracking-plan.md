# Exam/test tracking and schedule improvements

Status: Confirmed by the user on 2026-10-05. Planning only; implementation is a separate task.

## Goals

- Show today's classes, tests, and exams near the top of Home.
- Add a navigable week/month calendar to subject details.
- Track assessment attempts, raw scores, and weighted results separately from class attendance.
- Support optional class links for tests and explain the distinction between tests and exams.
- Improve the main calendar, holiday management, and extra-class management.
- Allow persistent deletion of one scheduled class on a specific date.

## 1. Assessment types and explanatory copy

Tests can be independent events or linked to a class. Exams are always scheduled independently of classes.

Place clear explanatory notes near the assessment type selector, for example:

- **Test:** Schedule independently or link to a class. For a linked test, class attendance automatically records whether you attempted it; you can override this.
- **Exam:** Schedule independently of classes and track its attempt and result separately.

Multiple tests can link to one class, including an extra class. The test and class must belong to the same subject. A linked test inherits its class's date/time and follows class rescheduling.

## 2. Attempt status and linked-test lifecycle

Independent tests and exams support manual Pending, Attempted, and Missed status.

Linked tests default to automatic status:

| Class attendance | Test attempt status |
| --- | --- |
| Present | Attempted |
| Absent | Missed |
| Unmarked | Pending |
| Cancelled | Needs rescheduling, unless manually overridden |

Users can manually override the automatic attempt status and reset it to automatic. Later attendance changes must respect an active override.

If a class is cancelled, or a holiday removes its scheduled occurrence, retain the linked tests and flag them Needs rescheduling unless overridden. Keep affected tests visible on their original date with a clear label. Stop reminders for tests needing rescheduling until they are rescheduled. Preserve entered scores.

Needs rescheduling is a scheduling condition: preserve attempt/score history rather than erasing it. It must not imply a missed attempt or automatically contribute zero.

Entered scores require Attempted status. Changing an assessment with a score to Missed or Pending asks before clearing its score; dismissing that confirmation preserves the existing state. Apply the same protection when an automatic attendance change would create this conflict.

## 3. Scores and weightage

### Subject configuration

Each subject optionally chooses one weightage system: Percentage or Weighted marks. Assessments within that subject use the selected system; do not mix systems in one subject.

For Weighted marks, allow a total possible weighted score, such as 100, to calculate an overall percentage. Without that total, show earned weighted marks and performance on graded assessments only.

Changing the subject's weightage system requires users to re-enter existing weights. Do not silently reinterpret or automatically convert weights.

### Assessment fields and calculations

- Retain existing title, date/time, portions, total marks, and reminder fields.
- Add obtained marks, optional weight, attempt status, and optional test/class link.
- Raw scores, total marks, and weights support decimals.
- Unweighted assessments still track attempts and raw scores but are excluded from weighted calculations.
- Total marks must be positive when supplied. Obtained marks require total marks and must be between zero and that total, inclusive. Bonus scores above the maximum are not supported.

For a scored assessment:

`contribution = obtained marks / total marks * weight`

Examples:

- 30/40 weighted to 20 marks contributes 15 weighted marks.
- 30/40 weighted at 20% contributes 15 percentage points.

Attempted assessments with unknown marks remain ungraded. Pending results do not count as zero. Missed assessments contribute zero for their configured weight.

Display earned contribution, graded weight so far, and remaining ungraded weight separately. Distinguish graded performance from progress toward the full subject result; do not normalize completed assessments and present that as the final subject result.

For Weighted marks with a configured subject total:

`earned percentage toward subject total = earned weighted marks / subject total weighted marks * 100`

For Percentage, earned contribution is already expressed in percentage points. Show the sum of entered weights so users can see how much is configured; do not invent assessments for an unallocated portion of the subject total.

Warn if combined weights exceed 100% or the configured weighted-marks total, but allow saving. Weighted results must remain completely separate from class-attendance percentages.

## 4. Home

- Put Today's schedule directly below the greeting and before statistics.
- Include all classes, tests, and exams scheduled for the current local date, ordered by time.
- Retain completed and cancelled events in the day's list, with their status visible.
- Show a linked test within its class card with a clear Test label, avoiding a duplicate standalone schedule entry.
- Show independent tests and exams as distinct events.
- Handle an empty schedule and refresh the date when the day changes.

## 5. Subject details

- Add a calendar filtered to the current subject with a week/month toggle, date selection, and previous/next navigation.
- Show selected-day classes, tests, and exams with attendance and assessment actions.
- Include the subject's grading configuration and weighted-result summary alongside existing attendance information.
- Reuse existing calendar presentation and interaction patterns where practical.

## 6. Main calendar and navigation

- Show all tests and exams for the selected date.
- Include assessment indicators in both week and month views.
- Show linked-test relationships and Needs rescheduling labels clearly.
- Tapping a subject name or avatar opens that subject's detail page.
- Tapping an assessment title/body opens its editor.
- Keep subject navigation distinct from attendance controls and assessment-edit actions.

## 7. Manage Holidays

- Add a month calendar with month navigation and date selection.
- Highlight single-date holidays, every date in holiday ranges, and applicable recurring holidays.
- Show selected-day holiday details and an Add holiday CTA.
- Preserve existing single-date/range creation and editing capabilities.
- Holiday changes must use the linked-test lifecycle above when affected scheduled classes are removed.

## 8. Extra Classes

- Open to a list of all existing extra classes rather than the creation form.
- Group entries into Upcoming and Past sections.
- Put Schedule extra class at the top.
- Provide edit/delete actions on entries and show relevant date/time, subject, room, and status.
- Use a separate creation/editing flow with the existing form capabilities and appropriate unsaved-change protection.
- Preserve date-prefilled entry points from the calendar.
- Apply the same linked-test rules when an extra class is edited, cancelled, or deleted.

## 9. Delete one scheduled class

- Permit deletion of a single normal timetable class on a particular date from calendar and subject details.
- This action does not remove its recurring timetable slot.
- Exclude deleted occurrences from attendance calculations and schedules, and cancel their class reminders.
- Persist the deletion so timetable regeneration, holiday changes, or refreshes cannot recreate that occurrence.
- If linked tests exist, ask the user to either delete those tests too or retain them as independent assessments. Allow cancelling the operation.
- Retained tests keep their date/time, attempt state, score, weight, and other assessment details; remove their class link and automatic attendance dependency.
- Deleting assessments must cancel their reminders. Refresh affected widget/schedule data.

## 10. Notifications

- Combine linked test and class notifications when their reminder times coincide, preserving the test information and appropriate actions.
- Preserve distinct linked-test reminders at other times.
- Independent tests retain their own notifications.
- Exams always retain separate notifications.
- Reschedule/cancel reminders consistently after edits, deletion, attendance changes, and holiday effects.
- Stop reminders for tests flagged Needs rescheduling until rescheduled.

## 11. Current implementation findings

Relevant source root: `app/src/main/java/com/iattend/app/`.

- `core/data/db/Assessment.kt` already defines Test/Exam, subject, date/time, total marks, portions, title, and reminder offsets. It lacks class links, attempt tracking, obtained marks, and weightage.
- `feature/assessment/AssessmentEditorViewModel.kt` and `AssessmentEditorScreen.kt` provide the existing assessment editor.
- `feature/calendar/CalendarViewModel.kt` already loads assessments for the selected day and supplies week assessment indicators. `CalendarScreen.kt` has week/month presentation that can be reused.
- `feature/home/HomeViewModel.kt` currently focuses on attendance summaries and does not expose a complete today schedule.
- `feature/subject/SubjectDetailViewModel.kt` currently exposes subject attendance history and statistics.
- `feature/holidays/HolidayListScreen.kt` currently leads with a holiday form and list.
- `feature/timetable/ExtraClassesScreen.kt` currently leads with the creation form rather than full history.
- `core/domain/occurrence/OccurrenceRepository.kt` deletes and regenerates unmarked scheduled occurrences. Occurrence IDs therefore cannot be assumed stable across regeneration.
- Timetable editing can also replace slot IDs. A simple link to a transient occurrence or slot ID alone is insufficient.

Implementation must preserve test/class relationships and deletion exceptions through regeneration, including holiday and timetable edits. Choose the smallest durable approach after tracing those flows; the precise database design remains an implementation decision.

## 12. Implementation sequence

1. Extend assessment and subject data, add safe database migration, and update export/import compatibility. Existing assessments start unweighted and without recorded attempts or obtained marks; existing data must not be lost or assigned invented results.
2. Make test/class relationships durable and persist individual class deletions. Handle regeneration, attendance overrides, cancellation, holidays, rescheduling, and linked-test deletion consistently.
3. Extend assessment editing with explanatory notes, links, attempts, scores, weight validation, and subject grading summaries.
4. Reuse calendar components for subject details and holidays; add Home's today schedule and main-calendar assessment indicators/navigation; reorganize Extra Classes.
5. Update reminder combination, scheduling, cancellation, and navigation.
6. Run focused logic checks and manually verify affected screen flows.

Prefer existing components and platform capabilities. No new dependencies or speculative grading features are required by this plan.

## 13. Acceptance and verification criteria

- Home shows today's entire schedule, in time order, with linked tests displayed once inside class cards.
- Tests can be independent or linked; exams cannot link to classes; type explanations are visible.
- Links support same-subject normal/extra classes and multiple tests per class.
- Automatic attempt changes, manual overrides, and reset-to-automatic behave consistently across attendance entry points.
- Cancellation and holiday effects preserve tests/scores and surface Needs rescheduling correctly.
- Class links survive ordinary timetable regeneration and editing; ambiguous or removed matches do not silently attach to the wrong class.
- Decimal grading matches the documented examples; pending/unknown results remain ungraded; missed results contribute zero; unweighted results remain excluded.
- Invalid raw scores cannot save; excess total weights warn but can save.
- Changing weightage systems does not silently reinterpret values.
- Single-class deletions remain deleted after regeneration and correctly update attendance totals.
- Linked-test deletion choices preserve or delete assessment data as selected, with no unintended reminder left behind.
- Week/month assessment indicators, selected-day lists, and subject/editor navigation work.
- Holiday ranges and recurring holidays appear on the holiday calendar.
- Extra Classes opens to complete Upcoming/Past lists and supports its top CTA, editing, deletion, and date-prefilled creation.
- Combined linked-test/class notifications retain relevant information; independent tests and exams follow the agreed separate-notification rules.
- Database migration preserves existing records. Export/import round trips preserve new grading data, links, overrides, and deletion exceptions; older backups remain supported.
- Verify empty states, date rollover, navigation/back behavior, and unsaved-change handling for affected forms.

## 14. Scope boundary

This document records the confirmed product plan. It does not authorize starting implementation in the planning session. Recurring timetable-slot deletion, bonus scores, and mixing weightage systems within a subject are outside this scope.
