# Home screen widget: show block end time, tap to add 30 min

## Goal

Add a 1x1 home screen widget that:
- shows the time when the current Instagram block ends (for example `14:35`)
- shows `0:00` when no block is active
- adds 30 minutes to the block when tapped, using the same rule as the app's "30 min" button

## Decisions

| Topic | Decision | Reason |
|---|---|---|
| What is shown | The time the block ends, not a countdown | Widgets can't redraw every second. A fixed end time stays correct without frequent updates. |
| Idle display | `0:00` | User choice. |
| Tap action | Add 30 min only, with no toast or activity launch | The widget's text changing is the feedback. |
| Framework | Plain `AppWidgetProvider` + `RemoteViews` (XML layout) | Needs no new dependencies. Glance would add a library and coroutine/worker setup for one TextView. |
| Time format | `android.text.format.DateFormat.getTimeFormat(context)` | Follows the device's 12h/24h setting. |
| Blocks that end on a later day | Add a short weekday prefix, for example `Thu 09:10` | Without it, `09:10` would be ambiguous. Rare, but cheap to handle. |

## Behavior

1. **Render:** read `NextOpenTimeStore.getNextOpenAt()`.
   - If the value is `<= now`, show `0:00`.
   - Otherwise show the formatted end time, with the weekday prefix if the end isn't today.
2. **Tap:** a `PendingIntent` broadcast to the widget provider with a custom action (`me.jdevlin.reelkilla.widget.ADD_30_MIN`). The provider calls `NextOpenTimeStore.advanceNextOpenAt(context, 30 min)` and then refreshes all widget instances.
3. **Expiry:** whenever the widget is refreshed with an active block, schedule one alarm for `nextOpenAt` that refreshes the widget so it switches to `0:00`.
   - Use inexact `AlarmManager.set(RTC, …)`. This needs no `SCHEDULE_EXACT_ALARM` permission. A few minutes of delay is acceptable, because the end time shown stays correct.
   - Use one `PendingIntent` with a fixed request code and `FLAG_UPDATE_CURRENT`, so rescheduling replaces the previous alarm.
   - Cancel the alarm when the block is inactive or when the last widget is removed (`onDisabled`).
4. **Keeping the widget in sync with the app:** when a duration button in `MainActivity` is pressed, call `ReelkillaWidget.refreshAll(context)` so the widget updates right away.
5. **Reboot and app update:** the system sends `APPWIDGET_UPDATE` after boot and after a package update. `onUpdate` renders and reschedules the alarm, so no `BOOT_COMPLETED` receiver is needed.
6. Set `updatePeriodMillis="0"`. All refreshes are driven by the events above.

## Files

### New
- `app/src/main/java/me/jdevlin/reelkilla/widget/ReelkillaWidget.kt`
  - `class ReelkillaWidget : AppWidgetProvider`
    - `onUpdate` → `refreshAll`
    - `onReceive` → handles `ACTION_ADD_30_MIN` and `ACTION_REFRESH` (the alarm), and otherwise calls `super`
    - `onDisabled` → cancels the alarm
  - `companion object`
    - `fun refreshAll(context: Context)`: builds `RemoteViews`, calls `updateAppWidget` for all IDs, and schedules or cancels the expiry alarm
  - A pure function `formatWidgetText(nextOpenAt: Long, now: Long, timeFormat: java.text.DateFormat, zone/Calendar): String`, kept separate so it's unit-testable
- `app/src/main/res/layout/widget_reelkilla.xml`: a `FrameLayout` with a rounded background and a centered `TextView` (bold, roughly 16–18sp, single line, autosize if needed so `Thu 12:45 PM` fits in 1x1)
- `app/src/main/res/drawable/widget_background.xml`: a rounded-rectangle shape. Use `@android:color/system_accent1_100`-style dynamic colours if desired (API 31+), or a fixed colour with `values-night` variant. minSdk is 30, so a fixed colour plus a night variant is the simple default.
- `app/src/main/res/xml/reelkilla_widget_info.xml`: `<appwidget-provider>` with:
  - `minWidth`/`minHeight` = `40dp`, `targetCellWidth`/`targetCellHeight` = `1`
  - `updatePeriodMillis="0"`
  - `initialLayout="@layout/widget_reelkilla"`
  - `resizeMode="none"`, `widgetCategory="home_screen"`
  - `description="@string/widget_description"`, and optionally a `previewLayout`
- `app/src/test/java/me/jdevlin/reelkilla/widget/ReelkillaWidgetTest.kt`

### Modified
- `AndroidManifest.xml`: register the receiver:
  ```xml
  <receiver android:name=".widget.ReelkillaWidget" android:exported="false">
      <intent-filter>
          <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
      </intent-filter>
      <meta-data android:name="android.appwidget.provider"
                 android:resource="@xml/reelkilla_widget_info" />
  </receiver>
  ```
  `exported="false"` is fine because the launcher reaches the receiver through the system's AppWidget service, and the custom actions are sent by our own explicit `PendingIntent`s.
- `MainActivity.kt`: in `onDurationSelected`, call `ReelkillaWidget.refreshAll(context)` after `advanceNextOpenAt`.
- `strings.xml`: add `widget_description` ("Shows when Instagram unblocks. Tap to add 30 minutes.") and `widget_idle` (`0:00`).

No changes to `NextOpenTimeStore` or `ReelkillaService`.

## Implementation steps

1. Add `formatWidgetText` and its unit tests (pure logic first).
2. Add the layout, background drawable, widget info XML and strings.
3. Add `ReelkillaWidget` with render only (`onUpdate`), and register it in the manifest. Check by hand that it can be placed and shows the right text.
4. Add the tap action (`ACTION_ADD_30_MIN`).
5. Add the expiry alarm (`ACTION_REFRESH`) and cancel it in `onDisabled`.
6. Call `refreshAll` from `MainActivity`.

## Tests

Unit tests (Robolectric, like `NextOpenTimeStoreTest`):
- `formatWidgetText`:
  - `nextOpenAt = 0` → `0:00`
  - `nextOpenAt` in the past → `0:00`
  - `nextOpenAt` later today → time only
  - `nextOpenAt` tomorrow or later → weekday prefix plus time
- The tap broadcast (`onReceive` with `ACTION_ADD_30_MIN`):
  - with no active block → stored value ≈ now + 30 min
  - with an active block → stored value = previous + 30 min
- `refreshAll` with an active block schedules an alarm at `nextOpenAt`, and with no active block it schedules none (check with Robolectric `ShadowAlarmManager`).

Manual checks on a device:
- Add the widget and confirm it shows `0:00`.
- Tap it and confirm it shows now + 30 min. Tap again and confirm it shows +60 min.
- Press "2 hr" in the app, go home, and confirm the widget updated.
- Set a short block (temporarily edit the duration, or use adb to set the pref), wait for it to pass, and confirm the widget returns to `0:00`.
- Reboot with a block active and confirm the widget still shows the end time and later resets.
- Switch the system 12h/24h setting and confirm the format follows it on the next refresh.

## Known limitations

- If the system time or timezone changes while a block is active, the widget isn't refreshed until the next tap, app action or alarm. Optionally fix this later by listening for `ACTION_TIME_CHANGED` and `ACTION_TIMEZONE_CHANGED`.
- `0:00` can look like midnight. This was accepted as the idle display.
