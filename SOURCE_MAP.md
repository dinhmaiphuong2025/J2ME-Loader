# SOURCE MAP — J2ME Loader Lixin (tự sinh từ repo)

Sinh ngày 2026-11-16 · nhánh `multitab` · package `com.j2me.lixin` · 702 file Java

Mục đích: danh sách **mọi bề mặt UI thật** để sửa UI không sót. Toàn bộ số liệu quét từ mã nguồn.

## 1. Số liệu

| Hạng mục | Số lượng |
|---|---|
| Activity (màn hình) | 10 |
| Fragment | 3 |
| Dialog | 10 |
| Layout XML | 27 |
| Menu XML | 8 |
| Drawable | 39 |
| Chuỗi tiếng Anh | 327 |
| Chuỗi tiếng Việt | 236 |
| **Chuỗi thiếu tiếng Việt** | **91** |
| Trường cấu hình/game | 44 |
| Khoá HUD/game | 5 |

## 2. Màn hình (Activity) — 10

| Màn hình | Lớp | Layout | Mở từ | Cổng vào | Ghi chú |
|---|---|---|---|---|---|
| MainActivity | `.MainActivity` | activity_main | Launcher (MAIN/LAUNCHER) | exported · singleTask | Pill tab, danh sách game, menu + và ⋮ |
| ConfigActivity | `.config.ConfigActivity` | activity_config, dialog_change_preset, item_resolution_preset | Danh sách game → long-press → Cấu hình · menu ⋮ → Cấu hình nâng cao | exported | 45 mục — đích của nút 'Nâng cao' |
| QuickConfigActivity | `.config.QuickConfigActivity` | — | Chạm game lần đầu · menu ⋮ → Cấu hình game | nội bộ | Giao diện dựng bằng code (không có XML): CHƠI + Điều khiển + Đồ hoạ + Xoay + 2 công tắc + Nâng cao |
| ConfigFilesActivity | `.config.ConfigFilesActivity` | activity_config_files, item_config_file | Cấu hình → Tệp cấu hình | nội bộ | Danh sách tệp cấu hình dùng chung |
| MicroActivity | `javax.microedition.shell.MicroActivity` | activity_micro, item_dialog_account | QuickConfig → CHƠI · tab bar | nội bộ · standard · :midlet | 2.210 dòng: canvas game, HUD, bàn phím ảo, pill tab, menu ⋮ (menu đổi theo chế độ điều khiển) |
| SettingsActivity | `.settings.SettingsActivity` | activity_settings | MainActivity → menu ⋮ → Cài đặt | nội bộ | Preference toàn app (không dùng layout XML, dùng R.xml) |
| BackupActivity | `.settings.BackupActivity` | activity_backup, dialog_progress | Cài đặt → Sao lưu | nội bộ | Sao lục / phục hồi dữ liệu |
| KeyMapperActivity | `.settings.KeyMapperActivity` | activity_keymapper | Cấu hình → Gán phím (ánh xạ) | nội bộ | Bảng gán phím cứng |
| FilteredFilePickerActivity | `.filepicker.FilteredFilePickerActivity` | — | Chọn tệp (âm thanh, font, shader) | nội bộ | Layout nằm trong thư viện filepicker |
| ProfilesActivity | `.config.ProfilesActivity` | activity_profiles | Cấu hình → Hồ sơ | nội bộ | Danh sách hồ sơ cấu hình |

## 3. Dialog & Fragment — 13

| Loại | Lớp | File | Layout |
|---|---|---|---|
| Dialog | `AboutDialogFragment` | `com/j2me/lixin/info/AboutDialogFragment.java` | — |
| Dialog | `HelpDialogFragment` | `com/j2me/lixin/info/HelpDialogFragment.java` | — |
| Dialog | `InfoDialogFragment` | `com/j2me/lixin/info/InfoDialogFragment.java` | — |
| Dialog | `LicensesDialogFragment` | `com/j2me/lixin/info/LicensesDialogFragment.java` | — |
| Dialog | `EditNameDialog` | `com/j2me/lixin/config/EditNameDialog.java` | dialog_change_name |
| Dialog | `InstallerDialog` | `ru/woesss/j2me/installer/InstallerDialog.java` | dialog_installer |
| Dialog | `BatchInstallerDialog` | `ru/woesss/j2me/installer/BatchInstallerDialog.java` | dialog_installer |
| Dialog | `LoadProfileDialog` | `com/j2me/lixin/config/LoadProfileDialog.java` | dialog_load_profile |
| Dialog | `SaveProfileDialog` | `com/j2me/lixin/config/SaveProfileDialog.java` | dialog_save_profile |
| Dialog | `ShaderTuneDialog` | `com/j2me/lixin/config/ShaderTuneDialog.java` | dialog_shader_tune, dialog_shader_tune_item |
| Fragment | `AppsListFragment` | `com/j2me/lixin/applist/AppsListFragment.java` | fragment_apps_list |
| Fragment | `SettingsFragment` | `com/j2me/lixin/settings/SettingsFragment.java` | — |
| Fragment | `FilteredFilePickerFragment` | `com/j2me/lixin/filepicker/FilteredFilePickerFragment.java` | listitem_checkable, listitem_dir |

## 4. Layout → nơi dùng

| Layout | Lớp dùng |
|---|---|
| `activity_backup` | BackupActivity |
| `activity_config` | ConfigActivity |
| `activity_config_files` | ConfigFilesActivity |
| `activity_keymapper` | KeyMapperActivity |
| `activity_main` | MainActivity |
| `activity_micro` | MicroActivity |
| `activity_profiles` | ProfilesActivity |
| `activity_settings` | SettingsActivity |
| `dialog_change_name` | EditNameDialog |
| `dialog_change_preset` | ConfigActivity |
| `dialog_installer` | BatchInstallerDialog, InstallerDialog |
| `dialog_load_profile` | LoadProfileDialog |
| `dialog_progress` | BackupActivity |
| `dialog_save_profile` | SaveProfileDialog |
| `dialog_shader_tune` | ShaderTuneDialog |
| `dialog_shader_tune_item` | ShaderTuneDialog |
| `fragment_apps_list` | AppsListFragment |
| `item_config_file` | ConfigFilesActivity |
| `item_dialog_account` | MicroActivity |
| `item_resolution_preset` | ConfigActivity |
| `list_row_grid_jar` | AppsListAdapter |
| `list_row_jar` | AppsListAdapter |
| `list_row_profile` | ProfilesAdapter |
| `listitem_checkable` | FilteredFilePickerFragment |
| `listitem_dir` | FilteredFilePickerFragment |
| `simple_list_item_multiple_choice` | CompoundListAdapter |
| `soft_button_bar` | Screen, ScreenSoftBar |

## 5. Menu → nơi dùng

| Menu | Lớp |
|---|---|
| `activity_profiles` | ProfilesActivity |
| `config` | ConfigActivity, ConfigFilesActivity |
| `config_files` | ConfigFilesActivity |
| `context_main` | AppsListFragment |
| `keymapper` | KeyMapperActivity |
| `main` | AppsListFragment |
| `midlet_displayable` | MicroActivity |
| `profile` | ProfilesActivity |

## 6. Cấu hình & lưu trữ

**44 trường `ProfileModel`** (mỗi game một hồ sơ JSON):

Kích thước/hiển thị: `screenWidth`, `screenHeight`, `screenBackgroundColor`, `screenScaleRatio`, `orientation`, `screenScaleToFit`, `screenKeepAspectRatio`, `screenScaleType`, `screenGravity`

Đồ hoạ: `screenFilter`, `immediateMode`, `hwAcceleration`, `graphicsMode`, `m3gForceDisableLighting`, `shader`, `parallelRedrawScreen`, `showFps`, `fpsLimit`, `forceFullscreen`

Font: `fontSizeSmall`, `fontSizeMedium`, `fontSizeLarge`, `fontApplyDimensions`, `fontAA`

Nhập liệu: `touchInput`, `showKeyboard`, `vkType`, `vkButtonShape`, `vkAlpha`, `vkForceOpacity`, `vkFeedback`, `vkHideDelay`, `vkBgColor`, `vkBgColorSelected`, `vkFgColor`, `vkFgColorSelected`, `vkOutlineColor`

Phím & âm thanh: `keyCodesLayout`, `keyCodeMap`, `keyMappings`, `soundBank`, `systemProperties`, `disablePause`

**5 khoá HUD riêng mỗi game** (`SharedPreferences` `hud_<hex(appPath.hashCode())>`): `hud_layout_profile_v2`, `use_hud_overlay`, `hud_layout_slot` (ép 0), `code_space_midp_v2`, `hud_editor_seen`

## 7. 91 chuỗi chưa dịch sang tiếng Việt

Đây là lỗ hổng UI thật (người dùng Việt sẽ thấy tiếng Anh lẫn vào):

```
PREF_COLOR_HINT
PREF_FORCE_DISABLE_3D_LIGHTING
PREF_SYS_PROPS_HINT
_FONT_SEPARATOR
_MINUS_ONE
_RESOLUTION_SEPARATOR
about_4pda
about_copyright
about_crowdin
about_discord
about_emugen_wiki
about_github
about_xda
alert_map_menu
alert_rewrite_profile
backup_export_done
backup_export_failed
backup_export_title
backup_import_confirm
backup_import_done
backup_import_failed
backup_import_title
browse
config_files_shader_help
config_files_soundbank_help
delete_file_message
delete_file_title
delete_resolution_message
delete_resolution_title
edit_resolution_title
error_add_file
error_delete_file
error_no_picker
install_multiple_done
install_multiple_failed
install_multiple_more
install_multiple_progress
msg_error_loading_shader
msg_immediate_mode_disabled
msg_mascot_capsule
msg_no_matches
msg_unsupported_soundbank
navigation_drawer_close
navigation_drawer_open
pref_add_cutout_area_title
pref_apps_view
pref_backup
pref_backup_summary
pref_black_background_summary
pref_black_background_title
pref_category_experimental
pref_disable_pause
pref_export_data
pref_import_data
pref_language
pref_mascot_summary
pref_mascot_title
pref_other_options_title
pref_screenshot_summary
pref_screenshot_title
pref_theme_default
preset_cannot_delete
save
shader_setting
shaders
show
soundbanks
virtual_key_0
virtual_key_1
virtual_key_2
virtual_key_3
virtual_key_4
virtual_key_5
virtual_key_6
virtual_key_7
virtual_key_8
virtual_key_9
virtual_key_a
virtual_key_b
virtual_key_c
virtual_key_d
virtual_key_down
virtual_key_f
virtual_key_left
virtual_key_left_soft
virtual_key_menu
virtual_key_pound
virtual_key_right
virtual_key_right_soft
virtual_key_star
virtual_key_up
```

## 8. Checklist trạng thái — áp cho MỌI màn hình

| # | Hạng mục | Cách kiểm |
|---|---|---|
| 1 | Sáng / Tối | Bật tối hệ thống, xem lại từng màn |
| 2 | Dynamic color | Đổi màu nhấn hệ thống, kiểm nút/chip/selection |
| 3 | Cỡ chữ 130% và 200% | Không cắt chữ, không chồng nút |
| 4 | Dọc và Ngang | Xoay ở từng màn, kể cả khi dialog mở |
| 5 | Tiếng Việt / Anh | Kiểm nhãn dài nhất; 91 chuỗi còn thiếu ở mục 7 |
| 6 | Cutout / tai thỏ | Pill tab, status bar, HUD không bị che |
| 7 | Vùng chạm ≥48dp | Đo bằng uiautomator dump |
| 8 | Trạng thái rỗng | Xoá hết game, mở lại từng màn |
| 9 | Trạng thái lỗi | .jar hỏng, quyền bị từ chối, bộ nhớ đầy |
| 10 | TalkBack | Mỗi nút đọc được tên có nghĩa |
| 11 | Phản hồi chạm | Ripple/đổi màu ở mọi nút |
| 12 | Nút Back | Không mất dữ liệu đang nhập |
| 13 | Bàn phím che nội dung | Mở bàn phím hệ thống ở mọi ô nhập |
| 14 | Đa nhiệm | 2 game, chuyển tab, thoát, quay lại |

## 9. Bề mặt UI không có trong bảng XML

- **HUD editor** (port Anland): vẽ bằng `HudOverlayView` — toolbar, inspector, kéo/thả, snap, undo/redo, cả 2 chiều.
- **Bàn phím ảo**: `VirtualKeyboard` tự vẽ trên canvas overlay — kiểu Nokia/Motorola/Custom, rung, độ mờ.
- **Nội dung trong game**: do MIDlet tự vẽ, ngoài phạm vi sửa UI.
- **Thông báo foreground service**, **bong bóng nổi**, **shortcut ngoài launcher**.