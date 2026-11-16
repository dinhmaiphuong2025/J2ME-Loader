# KẾ HOẠCH PORT "ANLAND HUD EDITOR" VÀO J2ME LOADER LIXIN

Mục tiêu: giữ bàn phím ảo cũ đúng vai trò giả lập keypad điện thoại (Nokia, Motorola,
bàn phím số…), nhưng THAY TOÀN BỘ PHẦN CHỈNH SỬA bằng editor của Anland, bổ sung
widget mới (joystick analog), và cắt bỏ những phần của Anland không dùng được trong
môi trường J2ME.

---

## 0. Quyết định đã chốt

1. **Runtime**: bàn phím ảo cũ vẫn là bộ hiển thị/nhập cho các preset máy điện thoại
   (Nokia/Motorola/số/Anland). Không đụng phần render này khi chưa cần.
2. **Chỉnh sửa**: thay bằng editor Anland (kéo/thả, resize, snap, inspector, undo/redo,
   icon, nhóm, combo) + thêm widget joystick.
3. **Nguồn port**: Anland HEAD (`/home/Lixin/anland/.../consumer/hud`), không dùng bản
   copy cũ trong repo (bản cũ thiếu 8 commit sửa HUD).
4. **Dọn dẹp**: phần editor viết dở trong `VirtualKeyboard` giữ tạm đến khi HUD chạy
   được rồi mới xoá.

---

## 1. Ràng buộc quyết định việc chọn lọc

J2ME (MIDP/CLDC) chỉ có:
- 21 phím: 0-9, *, #, FIRE, 4 hướng, 2 soft key.
- Không có con trỏ chuột, không có scroll wheel, không có phím chữ (trừ TextBox).
- Không có Ctrl/Alt/Shift/Super.
- Màn hình thấp, chơi ngang là chính.

Anland (desktop shell) có: evdev scancode, chuột, trackpad, scroll, modifier latch,
layout bàn phím, multi-monitor → phần lớn vô nghĩa với J2ME.

---

## 2. Hiện trạng repo (đã kiểm tra)

- `app/src/main/java/ru/playsoftware/j2meloader/hud/` = bản copy CŨ của Anland:
  34 file, 29 file không ai gọi (~13.000 dòng), TOÀN BỘ chưa vào git.
- Code đang chạy chỉ cần 5 class trong đó: HudAssetManager, NroIconPickerDialog,
  HudColors, HudPalette, HudType.
- `VirtualKeyboard` đang có bản editor thứ ba viết dở (LAYOUT_ANLAND_EDITOR) và
  nhánh editor cũ (LAYOUT_KEYS) không còn đường vào.
- Menu có 2 mục mở cùng một editor: "Chỉnh sửa phím HUD (Anland)" và "Layout edit mode".
- Build hiện xanh; không có class/string trùng; lỗi duy nhất về hành vi là hai hệ scale
  chồng nhau (scale nhóm × scale từng phím).

---

## 3. Bảng chọn lọc Anland

### GIỮ NGUYÊN
| Thành phần | Vì sao |
|---|---|
| HudOverlayView | Khung overlay, chế độ chơi/chế độ sửa, toolbar, hook inspector |
| HudPropertyInspectorView | Bảng thuộc tính nút (ẩn mục không dùng) |
| HudButton | Model: vị trí %, kích thước dp, shape, màu, icon, nhãn, action, long-press, swipe |
| HudLayout / HudLayoutProfile | JSON serialize; đổi nơi lưu sang profile từng game |
| SnapGeometryEngine | Hít cạnh / bám nút / căn lưới |
| HudHistory | Undo/redo 40 bước |
| HudVectorIcons | Icon vẽ bằng code → không cần drawable |
| design/* | Theme, màu, font, shape, control phụ trợ |
| HudCornerShapePickerView | Chọn bo góc |
| HudGroupView | Nhóm nút |
| ComboBuilderView | Macro nhiều phím cùng lúc (J2ME giữ nhiều phím được) |
| HudDpadView + HudDpadInput | Lõi phím giữ-nhả và chồng chéo hướng — dùng làm nền cho joystick |

### SỬA
| Thành phần | Sửa gì |
|---|---|
| HudAction | Bỏ TYPE_MOUSE/MODIFIER/TEXT; `code` đổi từ evdev sang mã phím J2ME; TYPE_SYSTEM chỉ giữ: mở menu, bật/tắt bàn phím ảo, chụp màn hình, thoát game |
| HudKeyPickerDialog | Danh sách 21 phím J2ME + nhãn tiếng Việt + chọn icon NRO |
| HudDpadView | Giữ hình, thêm chế độ 8 hướng cho joystick |
| HudLayoutProfile | Bỏ preset Steam Deck/Productivity/Hotkeys/keyboard layouts; giữ portrait+landscape; thêm preset NRO; bỏ slot ngoài |
| DefaultHudLayout3 | Thay bằng preset Anland/NRO |
| HudOverlayView | Bỏ nhánh mouse/scroll/modifier; SharedPreferences → file profile từng game; `isNativeReady` luôn true |
| SuperGestureButtonView | Giữ tap/long-press/swipe → map sang phím; bỏ pointer |
| HudTouchpadView | KHÔNG dùng phần chuột, nhưng dùng lại phần vẽ knob/vùng cảm ứng để làm widget joystick |

### BỎ HẲN
| Thành phần | Lý do |
|---|---|
| HudTouchpadView phần phát chuột (move/click/scroll/2 ngón) | J2ME không có con trỏ |
| HudScrollBarView | Không có scroll wheel |
| IModifierProvider + latched modifier | Không có Ctrl/Alt/Shift/Super |
| HudButtonPress phần mouse | Chỉ giữ nghĩa "gửi khi nhấn / khi thả" |
| Dock bar dưới (mặc định tắt) | Ăn màn hình game |
| Preset Steam Deck / Productivity / Hotkeys / layout bàn phím | Dành cho desktop |

### MỚI (không có trong Anland)
- **Widget joystick analog**: một pad tròn + núm kéo; chia 8 hướng; phát phím J2ME
  tương ứng; vùng chết ở giữa; dùng lại `HudDpadInput` (đã có đếm tham chiếu và xử lý
  chồng chéo) và phần vẽ núm của HudTouchpadView. Đây là thứ Anland thiếu vì desktop
  đã có chuột thật.

Sau khi cắt: khoảng 7.000 dòng thay vì 13.113.

### 3.1 Thuộc tính hiển thị — Anland đã có sẵn (giữ nguyên)

Tab **Style** trong inspector (đã kiểm tra trong mã Anland HEAD):

| Thuộc tính | Dải giá trị | Ghi chú |
|---|---|---|
| Độ mờ (Opacity) | 0–100 % | Áp lên toàn bộ nút khi vẽ |
| Màu nền | 6 ô màu cố định | trắng, tím #9D85C2, cyan #80DEEA, hồng #F38BA8, vàng #F9E2AF, xanh #A6E3A1 |
| Độ dày viền | 0–10 dp | 0 = không viền |
| Màu viền | 6 ô màu cố định | có cả ô trong suốt |
| Bo góc | 0–50 dp | thanh trượt |
| Kiểu bo góc | 8 kiểu | góc trên/dưới/trái/phải, đối xứng, tất cả, không |
| Kiểu nút | 3 kiểu | Minimal flat / Classic bevel / Glass outline |
| Màu chữ | 6 ô màu cố định | |
| Căn chữ | 3 kiểu | trái / giữa / phải |
| Nhãn hoặc Icon | 2 chế độ | kèm dialog chọn icon NRO |

Tab **Transform**: vị trí X/Y theo %, rộng/cao theo px, số cột/hàng lưới, chế độ
grid hay free, "reserve space", nút áp kích thước cho toàn bộ nút.

Vì runtime của layout tự chỉnh là chính HUD của Anland vẽ (mục 4.2), các thuộc tính
này hiện đúng 1:1 khi chơi — không phải viết lại cách vẽ.

### 3.2 Cần BỔ SUNG cho đủ ý bạn muốn

| Thiếu | Cách làm |
|---|---|
| Màu khi NHẤN (đã có UI ở cấu hình bàn phím ảo cũ, chưa có trong editor HUD) | Thêm hàng "Màu khi nhấn"; giá trị lấy từ trường "nền khi nhấn" đang có trong profile |
| Nhập mã màu tuỳ ý | Dùng lại đúng kiểu 5 ô HEX của màn hình cấu hình bàn phím ảo (bảng 6 màu vẫn giữ để chọn nhanh) |
| Đổ bóng / viền phát sáng | Chưa có trong Anland — để sau, không nằm trong bản port đầu |

### 3.3 Phải ẨN khi port (thuộc tab Event của Anland)

Độ nhạy chuột, kiểu kéo, khoá chạm, chế độ mouse/scroll — vô nghĩa với J2ME.
Giữ lại "gắn nút vào lớp bàn phím" (attach to keyboard) vì hợp với ý tưởng nút chỉ
hiện khi mở bàn phím ảo.

### 3.4 Cái gì bị THAY THẾ hẳn (không cùng tồn tại)

Nguyên tắc: mỗi chức năng chỉ có MỘT nơi chỉnh. Anland thắng ở các mục dưới đây thì
xoá bản cũ, không để hai bộ song song.

| App đang có | Anland tương đương | Xử lý |
|---|---|---|
| Editor bàn phím ảo cũ: 5 mục menu (Layout edit mode / Scale mode / Layout switch / Hide buttons / Finish) + nhánh LAYOUT_KEYS, LAYOUT_SCALES | Editor HUD: kéo, resize, snap, undo/redo, inspector | **XOÁ** toàn bộ editor cũ; menu chỉ còn 1 mục "HUD Editor" |
| Nhóm màu/độ mờ/kiểu nút trong ConfigActivity (5 ô HEX, alpha, shape, force opacity) | Inspector tab Style + Transform | **CHUYỂN** hết vào editor; xoá nhóm khỏi ConfigActivity (giữ bật/tắt, rung, ẩn sau) |
| Kiểu bàn phím "Custom" + "Anland" (TYPE_CUSTOM/TYPE_ANLAND tự sinh layout) | HudLayoutProfile + preset | Preset máy (Nokia/Motorola/số) **GIỮ**; "Custom/Anland" → **HUD tự thiết kế** |
| Icon từng phím kiểu drawable xếp lớp | HudVectorIcons + icon NRO (asset) | **BỎ** icon drawable, chỉ dùng vector + asset |
| "Hide buttons" (ẩn nút) | hud_hidden_mode (giữ phím KB để quay lại) | **THAY** bằng của Anland — xem quy tắc 3.6 |
| 4 slot layout của Anland (portrait/landscape/keyboard × 4) | ProfilesManager của app (mỗi game 1 profile) | **BỎ slot**, nhúng 1 HUD layout vào profile từng game sẵn có |
| Key mapping (activity_keymapper: gán phím cứng) | Key picker của HUD (gán phím cho nút) | **GIỮ CẢ HAI** vì khác mục đích (cứng vs mềm), nhưng key picker dùng đúng bảng mã của keymapper |
| "Nút chỉ hiện khi mở bàn phím" (LAYER_HUD/LAYER_KEYBOARD) | attach to keyboard | **THAY** bằng cơ chế layer của Anland |
| Screenshot / khoá xoay / giới hạn FPS trong menu ⋮ | HUD system command | **GIỮ menu**, nhưng nút HUD gọi lại đúng hành động đó |

### 3.5 Lỗi toolbar của editor dở (sẽ biến mất khi thay)

Đã đọc phần vẽ toolbar hiện tại, có 4 lỗi hiển thị thật:
1. Kích thước dùng pixel thô (cao 46 px, rộng tối đa 540 px, bo 14 px) → trên màn
   1080p toolbar chỉ cao ~15 dp, chữ bị cắt, nút dẹt.
2. Nhãn nhồi vào nút: "−1.0x" nằm trong ô rộng ~90 px nên tràn; các nút khác không
   có nhãn chức năng nên không biết bấm để làm gì.
3. Không có đường phân cách giữa 6 nút → nhìn thành một khối.
4. Toolbar nằm đè lên dải trên của vùng phím; nút nào nằm dưới toolbar thì không
   chọn được khi đang sửa (ưu tiên chạm thuộc về toolbar).

→ Khi dùng toolbar thật của Anland (Android View, kích thước dp, có icon vector,
tách lớp khỏi vùng phím) cả 4 lỗi này không còn. Nếu muốn dùng tạm bản hiện tại
trước khi port xong thì phải sửa tối thiểu mục 1 và 4 (~15 dòng).

### 3.6 Ẩn phím: chỉ còn MỘT cơ chế, không đụng "+ thêm nút"

Hiện tại app có mục "Hide buttons": bật là đặt `visible = false` cho toàn bộ phím,
muốn hiện lại chỉ còn cách mở lại đúng menu đó. Cơ chế này xung đột với editor Anland
ở hai điểm: (a) phím đang bị ẩn thì vào editor nhìn như layout rỗng, dễ tưởng mất nút;
(b) nút "+ thêm nút" thêm vào một layout mà các phím cũ vẫn mang cờ ẩn, nên thêm xong
không thấy đâu.

Quy tắc chốt:
1. **Trong editor luôn hiện đủ mọi nút**, bất kể đang ẩn hay không. Anland đã làm sẵn
   điều này (`mHudHidden && !mIsEditMode`), nên "+ thêm nút" luôn thấy toàn bộ layout.
2. **HUD**: ẩn/hiện dùng đúng cơ chế của Anland — ẩn thì vẫn giữ một nút để bật lại;
   nếu layout không còn nút bật lại (đổi layout, xoay màn hình) thì tự thoát trạng thái
   ẩn. Anland đã có sẵn phần kiểm tra này.
3. **"Ẩn" trong HUD không phải thuộc tính của nút** — không có nút nào bị ẩn; muốn bớt
   nút thì xoá nút. Vì vậy không thể xung đột với "+ thêm nút".
4. **Bàn phím cổ điển (Nokia/Motorola)** giữ mục "Ẩn phím" như hiện tại, vì người chơi
   bằng phím cứng cần ẩn hết phím ảo; đường quay lại là thanh pill (luôn hiện), không
   phải HUD. Mục này chỉ áp dụng khi renderer = `keypad`.
5. Xoá nhánh `states[]` dùng chung: mỗi renderer tự quản trạng thái ẩn của mình.

---

## 4. Kiến trúc

### 4.1 Điểm nối: `HudOverlayView.HudHost`
Implement trong `MicroActivity`:

```
sendKey(action, evdev)  -> đổi evdev sang mã phím J2ME, gọi
                           displayable.postKeyPressed / postKeyReleased
sendTextInput(utf8)     -> noop (hoặc bơm vào TextBox nếu đang mở)
sendMouse*, toggleSystemKeyboard -> noop
toggleVirtualKeyboard   -> mở bàn phím ảo cũ
openSettings            -> showMenu() hiện có
onLayoutSaved           -> ghi JSON vào profile của game đang chạy
isNativeReady           -> true
onEditModeExited        -> lưu + đóng editor
```

### 4.2 Hai bộ hiển thị, một bộ chỉnh sửa
| Trường hợp | Bộ hiển thị khi chơi |
|---|---|
| Preset keypad máy điện thoại (Nokia/Motorola/số/Anland) | `VirtualKeyboard` như hiện tại (Canvas) |
| Layout tự chỉnh bằng editor (có joystick, nhóm, icon) | `HudOverlayView` (Android View) |

- Cùng một file profile mỗi game, có trường `renderer` = `keypad` | `hud`.
- Ưu điểm: WYSIWYG — cái gì thấy trong editor là cái chạy thật; không phải viết lại
  joystick/nhóm/icon bằng Canvas.
- Đổi lại: phải bảo đảm hai bộ không bật cùng lúc (cờ trong profile) để không nhân đôi phím.

### 4.3 Bảng đổi mã phím
evdev (mã Anland đang dùng) → mã phím J2ME. Khoảng 21 dòng:
số, KEY_STAR, KEY_KP*, 4 hướng, FIRE/ENTER, soft key trái/phải, CLEAR.
Dùng đúng bảng mã mà `VirtualKeyboard` đang dùng để hai bộ hiển thị phát phím giống nhau.

### 4.4 Chỗ đặt overlay
- `HudOverlayView` là FrameLayout → thêm vào `binding.overlayView` (nằm dưới thanh pill).
- Khi mở editor: ẩn thanh pill để inspector/toolbar không chồng; thoát bằng nút trong HUD.

### 4.5 Lưu trữ
- Bỏ `hud_layout_profile_v2` (SharedPreferences toàn cục).
- Ghi `hud_layout.json` trong thư mục profile của từng game (cạnh layout bàn phím ảo),
  khớp `ProfilesManager` hiện có.
- Giữ `version` trong JSON + migrate; đọc file cũ một lần nếu có.

### 4.6 Quan hệ với cấu hình bàn phím ảo đang có (không nhân đôi)

App hiện đã có, lưu theo từng game trong profile:
- Bật/tắt bàn phím ảo, rung, force-opacity, ẩn sau N ms
- Kiểu nút (oval / rect / round), độ mờ 0–255
- **5 ô nhập mã màu HEX**: nền nút, màu chữ, nền khi nhấn, chữ khi nhấn, màu viền

Quy tắc khi port HUD:

| Trường hợp | Xử lý |
|---|---|
| Game dùng renderer `keypad` (Nokia/Motorola/số) | Giữ nguyên toàn bộ cấu hình cũ, không đổi gì |
| Game dùng renderer `hud` | Ẩn nhóm màu/độ mờ/kiểu nút khỏi màn hình cấu hình cũ (đã có trong editor), chỉ giữ bật/tắt, rung, ẩn sau |
| Tạo nút HUD mới | Lấy giá trị từ cấu hình cũ làm mặc định: nền ← nền nút, chữ ← màu chữ, **màu khi nhấn ← nền khi nhấn**, viền ← màu viền, độ mờ ← alpha, bo góc ← kiểu nút |
| Lần đầu mở editor của một game đã có layout keypad | Sinh layout HUD từ layout keypad hiện tại + màu hiện tại → không phải làm lại từ đầu |

Nhờ vậy: "màu khi nhấn" không phải khái niệm mới (app đã có sẵn), và ô nhập mã màu
tự do cũng không phải làm mới — dùng lại đúng kiểu 5 ô HEX đang có trong màn hình
cấu hình bàn phím ảo.

---

## 5. Kế hoạch theo pha

### P0 — Chốt phạm vi (0,5 ngày)
- Bảng GIỮ/SỬA/BỎ ở mục 3; kiến trúc 2 bộ hiển thị.
- Đầu ra: file này.

### P1 — Dọn hiện trạng (0,5 ngày)
- Commit phần đang chạy được (VirtualKeyboard, MicroActivity, xml, hud_strings, assets)
  để không mất việc.
- Xoá 29 class hud chết + asset không dùng (analog_base.png, fire_pressed.png).
- Bỏ mục menu trùng "Layout edit mode".
- KHÔNG xoá editor dở trong VirtualKeyboard (để dự phòng).
- Test: build xanh, game chạy như trước, layout đã lưu còn nguyên.

### P2 — Bê khung từ Anland HEAD (1 ngày)
- Copy 32 file hud từ Anland HEAD → `com.j2me.lixin.hud`.
- Copy `hud_strings.xml` (267 string) + xử lý 13 `R.attr` + 2 `R.id`.
- Cắt mouse/scroll/modifier theo mục 3.
- Test: build xanh, APK tăng không quá ~1 MB.

### P3 — Adapter + bảng phím (1 ngày)
- `HudHost` trong MicroActivity; bảng evdev → J2ME; post key đúng luồng.
- Test: gán nút vào phím 5 → game nhận phím 5; giữ → lặp; nhả → nhả; 2 ngón → 2 phím.

### P4 — Trình biên tập (1,5 ngày)
- Menu → mở editor; kéo/thả, resize, snap, shape/màu/nhãn/icon, undo/redo.
- Inspector chỉ hiện thuộc tính dùng được.
- Lưu/mở theo từng game.
- Test: sửa layout game A, mở game B → layout riêng; quay lại A → đúng.

### P5 — Preset + icon NRO (1 ngày)
- Preset Anland/NRO thay DefaultHudLayout3.
- Gán icon NRO cho từng nút; lưu/đọc icon đúng.
- Test: chọn preset → layout chuẩn; gán icon → hiện đúng sau khi khởi động lại.

### P6 — Widget joystick (1 ngày)
- View joystick mới: núm + vùng chết + 4/8 hướng; lõi `HudDpadInput` phát phím.
- Thêm vào inspector: chọn hướng, kích thước vùng chết, độ nhạy, 4 hay 8 hướng.
- Test: kéo chéo → 2 phím cùng lúc; nhả → nhả đủ; không dính phím khi vuốt qua tâm.

### P7 — Nối runtime HUD (1,5 ngày)
- Bật HUD khi profile của game là `hud`; tắt `VirtualKeyboard` cùng lúc.
- Preset keypad vẫn dùng `VirtualKeyboard` như cũ.
- Test: chơi 10 phút, 2 ngón + joystick, chuyển tab, xoay màn hình, thoát vào lại.

### P8 — Dọn cuối (0,5 ngày)
- Xoá editor dở trong `VirtualKeyboard` (LAYOUT_ANLAND_EDITOR) và các nhánh editor cũ
  không còn đường vào (LAYOUT_KEYS, LAYOUT_SCALES) + 4 mục menu còn lại
  (Scale mode, Layout switch, Hide buttons, Finish).
- Xoá nhóm màu/độ mờ/kiểu nút khỏi ConfigActivity (đã chuyển vào editor).
- Cập nhật skill + tài liệu.

Tổng: ~8 ngày làm việc.

---

## 6. Rủi ro và cách chặn

| Rủi ro | Cách chặn |
|---|---|
| Thiếu string/attr → build gãy | Copy nguyên `hud_strings.xml` từ Anland; attr thiếu thay bằng style M3 có sẵn |
| HUD và bàn phím ảo cùng phát phím | Cờ `renderer` trong profile; chỉ một lớp nhập bật tại một thời điểm |
| `postKeyPressed` sai luồng | Đẩy qua handler của Displayable như bàn phím ảo đang làm |
| Inspector/toolbar chồng thanh pill | Editor toàn màn hình, ẩn pill, thoát bằng nút trong HUD |
| Mất layout khi đổi định dạng | `version` trong JSON + migrate + sao lưu trước khi ghi |
| APK phình vì 381 asset | Chỉ đóng gói bộ icon đang dùng; bộ khác nạp ngoài |
| Joystick dính phím khi vuốt qua tâm | Dùng đếm tham chiếu của `HudDpadInput`, nhả hết khi ra vùng chết |

---

## 7. Nghiệm thu toàn bộ
1. Preset Nokia/Motorola chạy y như trước khi port (không hồi quy).
2. Menu chỉ còn một mục "HUD Editor"; mở được, kéo/resize/snap/màu/shape/nhãn/icon.
3. Gán 21 phím J2ME + combo nhiều phím.
4. Joystick 4/8 hướng chạy đúng, đa điểm với nút khác.
5. Layout lưu riêng từng game, mở lại đúng, undo/redo hoạt động.
6. Không double key, không crash khi chuyển tab / thoát / xoay màn hình.
7. Không còn class chết, không còn mục menu trùng, toàn bộ HUD nằm trong git.
