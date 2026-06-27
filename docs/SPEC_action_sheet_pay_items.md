# SPEC — Action-sheet-driven contract pay items (modern view)

> Hand this file to Cowork: "implement docs/SPEC_action_sheet_pay_items.md".
> Follow the repo conventions already in place (Flyway, `ddl-auto=validate`,
> `EffectiveDated`, MUI v6 + TanStack Query). Do NOT change the existing
> supersede logic — only add the action-sheet field and redesign the panel.

## 1. Goal

Make the **action sheet number** a first-class attribute of each contract pay
item, and redesign the pay-items panel so the contract reads as:

1. **Current salary snapshot** — every ACTIVE pay item, each showing the action
   sheet it currently comes from. Unchanged items keep their original action
   sheet; only the changed item carries the new one.
2. **Action sheet history timeline** — pay items grouped by `action_sheet_no`,
   newest first; the group that owns the current values is highlighted.

Business rule (already works via supersede, just carry the new field):
when a new action sheet changes one component, the previous row is superseded
(status INACTIVE, `effective_to` set) and keeps its old `action_sheet_no`; the
new row is created ACTIVE with the new `action_sheet_no`. Untouched components
are not re-inserted, so they retain their original `action_sheet_no`.

## 2. Data model — migration `V13__pay_item_action_sheet.sql`

```sql
-- Action sheet number as a first-class attribute of a contract pay item.
-- Free text (matches legacy numbering, e.g. D-17/C02/PS/2698/14960).
ALTER TABLE contract_pay_item
    ADD COLUMN action_sheet_no VARCHAR(40);

-- Backfill: the legacy import put the action sheet ref in `remarks`.
-- Copy it across so existing rows group correctly, then leave remarks as-is.
UPDATE contract_pay_item
   SET action_sheet_no = NULLIF(TRIM(remarks), '')
 WHERE action_sheet_no IS NULL;

-- Grouping/sort support.
CREATE INDEX ix_pay_item_action_sheet ON contract_pay_item (contract_id, action_sheet_no);
```

> Note: this is a best-effort backfill. If `remarks` held other free text on some
> rows, that's acceptable — the field is editable in the UI afterwards.

## 3. Backend changes

`ddl-auto=validate` means the entity must match the column exactly.

**`ContractPayItem.java`** — add:
```java
@Column(name = "action_sheet_no", length = 40)
private String actionSheetNo;
// + getter/setter
```

**`ContractPayItemDto.java`** — add (free text, optional):
```java
@Size(max = 40)
private String actionSheetNo;
// + getter/setter
```

**`ContractPayItemService.java`** — in `apply(dto, entity)` add:
```java
entity.setActionSheetNo(dto.getActionSheetNo());
```
and in `toDto(entity)` add:
```java
dto.setActionSheetNo(entity.getActionSheetNo());
```
No other service change. The supersede loop already keeps the old row untouched
except for `effective_to`/`status`, so the old `action_sheet_no` is preserved
automatically; the new row gets the new value from the form.

## 4. Frontend changes

**`api/types.ts`** — add to `ContractPayItem`:
```ts
actionSheetNo?: string;
```

**`pages/EmployeesPage.tsx` → `PayItemsPanel`** — redesign. Keep using
`contractPayItemApi`, `usePayComponents`, the `save`/`del` mutations, and the
existing supersede flow. Replace the flat list + renderRow with the two sections
below. Match the mockup the user approved (snapshot card on top, timeline below).

### 4a. Add the action-sheet field to the form
In `EMPTY_ITEM`, add `actionSheetNo: ""`. In the add/change form, add a field
**before** Remarks:
```
Action Sheet No   (TextField, value=form.actionSheetNo, maxLength 40)
```
Keep Remarks as an optional free note (no longer the action-sheet carrier).

### 4b. Current salary snapshot (replaces the plain ACTIVE list)
- Compute `active = data.filter(i => i.status === "ACTIVE")` and the same `net`.
- Render a card: header row `Current salary` + big `net` number with currency.
- One row per active item: component label + `since {effectiveFrom}` on the left;
  on the right a small monospace pill showing `i.actionSheetNo ?? "—"` and the
  amount. The pill uses the accent tint only for the item(s) whose
  `actionSheetNo` equals the newest action sheet (see 4c); others use a neutral
  surface tint. This is what visually proves "unchanged items keep the old AS".

### 4c. Action sheet history timeline (replaces Show history)
- Group ALL rows by `actionSheetNo` (treat null/empty as group `"—"`):
  ```ts
  const groups = Object.values(
    data.reduce((acc, i) => {
      const k = i.actionSheetNo || "—";
      (acc[k] ??= { key: k, items: [], date: i.effectiveFrom }).items.push(i);
      acc[k].date = [acc[k].date, i.effectiveFrom].sort()[0]; // earliest in group
      return acc;
    }, {} as Record<string, { key: string; items: ContractPayItem[]; date: string }>)
  ).sort((a, b) => (a.date < b.date ? 1 : -1)); // newest first
  ```
- The "current" group = the one containing any ACTIVE item with the latest
  `effectiveFrom`. Highlight it (success tint, "Current" chip + check dot).
- Each group renders as a timeline node (vertical 2px line on the left, dot per
  node): header = monospace `actionSheetNo` + group date + chip; body = its items
  (component + amount). For an item that superseded a prior value of the same
  component, show the delta `old → new` (look up the most recent INACTIVE row of
  the same `payComponentId` with an earlier `effectiveFrom`).
- Keep a per-item delete (trash) like today.

### 4d. Visual tokens (match existing MUI design, not a new identity)
Cards: `variant="outlined"`, `borderRadius` 2, `p: 1.5`. Current group accent:
`success.main` dot + `success` Chip. Action-sheet number: `fontFamily: "monospace"`.
Net: `color: primary.main`. Muted meta: `text.secondary`. Reuse the MUI
components already imported in the file (Paper, Stack, Chip, Typography, Divider,
IconButton, Grid, TextField, Button). Add `Chip` to the import if not present.

## 5. Acceptance checks
1. `cd frontend && npx tsc --noEmit` → clean.
2. After deploy, open an employee → Contracts → a contract → Pay items:
   - The current snapshot lists active items, each with its action-sheet pill.
   - The timeline groups items by action sheet, newest first, current group
     highlighted.
3. Add a new amount for an existing component with a **new** action-sheet number
   and a later effective date → old row moves to history under its **old** action
   sheet; new row appears under the **new** action sheet and in the snapshot; the
   untouched components still show their **original** action sheet in the snapshot.
4. `ddl-auto=validate` passes on startup (entity matches V13).

## 6. Out of scope (later)
A dedicated `action_sheet` table (number + date + reason + author + attachment)
that pay items reference by FK, so one action sheet can be created once and many
items attached to it. For now the number is free text on each item.
