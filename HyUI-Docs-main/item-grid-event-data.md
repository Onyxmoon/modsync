# Item Grid Event Data

Below is a list of the event payloads passed to event listeners for particular events associated with Item Grids. These are useful to know which item is being dragged, from where it came and where it is going.

This page documents the event data classes used for item grids. Each payload is only passed to listeners registered for the matching `CustomUIEventBindingType` value.

### Mapping: Event Type -> Payload

| CustomUIEventBindingType        | Payload Class                            |
| ------------------------------- | ---------------------------------------- |
| `SlotClicking`                  | `SlotClickingEventData`                  |
| `SlotDoubleClicking`            | `SlotDoubleClickingEventData`            |
| `SlotMouseEntered`              | `SlotMouseEnteredEventData`              |
| `SlotMouseExited`               | `SlotMouseExitedEventData`               |
| `DragCancelled`                 | `DragCancelledEventData`                 |
| `Dropped`                       | `DroppedEventData`                       |
| `SlotMouseDragCompleted`        | `SlotMouseDragCompletedEventData`        |
| `SlotMouseDragExited`           | `SlotMouseDragExitedEventData`           |
| `SlotClickReleaseWhileDragging` | `SlotClickReleaseWhileDraggingEventData` |
| `SlotClickPressWhileDragging`   | `SlotClickPressWhileDraggingEventData`   |

### Payload Details

#### `SlotClickingEventData`

* Passed only to listeners registered for `CustomUIEventBindingType.SlotClicking`.
* Fields:
  * `slotIndex` from `SlotIndex`.

#### `SlotDoubleClickingEventData`

* Passed only to listeners registered for `CustomUIEventBindingType.SlotDoubleClicking`.
* Fields:
  * `slotIndex` from `SlotIndex`.

#### `SlotMouseEnteredEventData`

* Passed only to listeners registered for `CustomUIEventBindingType.SlotMouseEntered`.
* Fields:
  * `slotIndex` from `SlotIndex`.

#### `SlotMouseExitedEventData`

* Passed only to listeners registered for `CustomUIEventBindingType.SlotMouseExited`.
* Fields:
  * `slotIndex` from `SlotIndex`.

#### `DragCancelledEventData`

* Passed only to listeners registered for `CustomUIEventBindingType.DragCancelled`.
* Fields:
  * none.

#### `DroppedEventData`

* Passed only to listeners registered for `CustomUIEventBindingType.Dropped`.
* Fields:
  * `sourceItemGridIndex` from `SourceItemGridIndex`.
  * `sourceSlotId` from `SourceSlotId`.
  * `itemStackQuantity` from `ItemStackQuantity`.
  * `pressedMouseButton` from `PressedMouseButton`.
  * `itemStackId` from `ItemStackId`.
  * `sourceInventorySectionId` from `SourceInventorySectionId`.

#### `SlotMouseDragCompletedEventData`

* Passed only to listeners registered for `CustomUIEventBindingType.SlotMouseDragCompleted`.
* Fields:
  * `sourceItemGridIndex` from `SourceItemGridIndex`.
  * `sourceSlotId` from `SourceSlotId`.
  * `itemStackQuantity` from `ItemStackQuantity`.
  * `pressedMouseButton` from `PressedMouseButton`.
  * `itemStackId` from `ItemStackId`.
  * `sourceInventorySectionId` from `SourceInventorySectionId`.

#### `SlotMouseDragExitedEventData`

* Passed only to listeners registered for `CustomUIEventBindingType.SlotMouseDragExited`.
* Fields:
  * `mouseOverIndex` from `MouseOverIndex`.

#### `SlotClickReleaseWhileDraggingEventData`

* Passed only to listeners registered for `CustomUIEventBindingType.SlotClickReleaseWhileDragging`.
* Fields:
  * `slotIndex` from `SlotIndex`.
  * `clickMouseButton` from `ClickMouseButton`.
  * `clickCount` from `ClickCount`.

#### `SlotClickPressWhileDraggingEventData`

* Passed only to listeners registered for `CustomUIEventBindingType.SlotClickPressWhileDragging`.
* Fields:
  * `slotIndex` from `SlotIndex`.
  * `dragItemStackId` from `DragItemStackId`.
  * `dragItemStackQuantity` from `DragItemStackQuantity`.
  * `dragSourceInventorySectionId` from `DragSourceInventorySectionId`.
  * `dragSourceItemGridIndex` from `DragSourceItemGridIndex`.
  * `dragSourceSlotId` from `DragSourceSlotId`.
  * `dragPressedMouseButton` from `DragPressedMouseButton`.
  * `clickMouseButton` from `ClickMouseButton`.
  * `clickCount` from `ClickCount`.
