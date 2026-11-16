# KẾ HOẠCH: LÀM LẠI CẤU HÌNH + SẮP XẾP LẠI SOURCE

Vấn đề: màn hình cấu hình hiện tại là MỘT trang cuộn dài 1.118 dòng, chứa 45 mục
trộn lẫn kỹ thuật (scale type, gravity, shader, parallel redraw, encoding, system
properties) với những thứ người chơi game cần. Người bình thường mở ra không biết
phải sửa gì để chơi.

Mục tiêu: mở game là chơi được ngay với cấu hình gợi ý; ai cần thì mới đi sâu;
mọi thứ cũ vẫn còn nhưng nằm đúng chỗ.

---

## 1. Hiện trạng (đã kiểm tra)

Một trang `activity_config.xml` (1.118 dòng), 6 nhóm:
1. Screen options (9 mục): giữ tỉ lệ, tự động kích thước, màu nền, phóng to,
   xoay, gravity, scale type, lọc mượt, immediate.
2. Đồ hoạ/hiệu năng (7): graphics mode, shader, parallel redraw, toàn màn hình,
   hiện FPS, giới hạn FPS.
3. Font (5): 3 cỡ chữ, presets, đơn vị sp, anti-aliasing.
4. Input (13): cảm ứng, kiểu bàn phím, key mapping, bật bàn phím ảo, kiểu nút,
   rung, độ mờ, force opacity, ẩn sau, 5 ô màu.
5. Audio (2): soundbank.
6. Khác (4): encoding, system properties, chạy tiếp khi ẩn, tắt chiếu sáng 3D.

Model: 45 trường trong profile, lưu riêng từng game.

---

## 2. Nguyên tắc thiết kế

1. **Mặc định phải chơi được.** Mở game lần đầu → áp cấu hình gợi ý, không bắt
   người dùng chọn gì.
2. **Ba lớp**: Chơi ngay (3 nút) → Cơ bản (6 nhóm, ~12 mục) → Nâng cao (đúng 45 mục cũ).
3. **Nói bằng việc người dùng thấy**, không nói bằng thuật ngữ: "Làm mượt hình"
   thay cho "filtering"; "Vừa khít màn hình" thay cho "scale type".
4. **Không mất tính năng nào.** Mục nào không đưa ra lớp Cơ bản thì nằm ở Nâng cao.
5. **Có đường quay lại.** Mỗi game có nút "Khôi phục gợi ý" để về mặc định.

---

## 3. Cấu trúc mới

### 3.1 Màn hình "Chơi" (mở từ danh sách game)
```
[ Tên game · độ phân giải 240x320 ]
[ Chơi ]                       <- nút lớn, dùng cấu hình hiện tại/gợi ý
[ Chọn tài khoản ]             <- khi có nhiều profile
[ Toàn màn hình ]  [ Rung ]  [ Bàn phím ảo ]
[ Cấu hình khác ▸ ]
```

### 3.2 Cài đặt cơ bản — 6 nhóm (mỗi nhóm 2–4 mục)
| Nhóm | Mục | Ghi chú |
|---|---|---|
| Hiển thị | Kích thước (Vừa khít / Đầy màn hình / Gốc), Xoay (Tự động / Ngang / Dọc), Làm mượt hình, Giữ đúng tỉ lệ, Màu nền | gộp scale type + gravity + scale ratio thành 2 thanh trượt |
| Điều khiển | Kiểu bàn phím (Nokia / Motorola / Số / HUD tự thiết kế), Cảm ứng, Rung khi bấm, Độ mờ bàn phím | "HUD tự thiết kế" mở editor |
| Âm thanh | Bật âm thanh, Bộ âm thanh (soundbank) | |
| Hiệu năng | Giới hạn FPS, Tăng tốc phần cứng, Hiện FPS | mặc định ẩn FPS |
| Dữ liệu | Sao lưu / phục hồi save, Xuất / nhập cấu hình, Khôi phục gợi ý | |
| Nâng cao ▸ | Toàn bộ 45 mục cũ | giữ nguyên thứ tự như trang cũ |

### 3.3 Bảng xếp 45 mục cũ vào đâu
| Mục cũ | Chỗ mới |
|---|---|
| scale_type, gravity, scale_ratio, dynamic_screen_size | Hiển thị → "Kích thước" + "Vị trí khung" (Nâng cao) |
| keep_aspect_ratio, orientation, filter, background color | Hiển thị |
| force_fullscreen | Hiển thị |
| show_fps, fps_limit, hwAcceleration | Hiệu năng |
| immediate, graphics_mode, shader, parallel_redraw | Nâng cao |
| font: 3 cỡ chữ, presets, sp, anti-aliasing | Nâng cao → nhóm "Chữ" |
| touch_input, keyCodesLayout, vkFeedback | Điều khiển |
| virtual keyboard on/off, vkAlpha, vkButtonShape | Điều khiển (style nút + 5 màu → Nâng cao hoặc editor HUD) |
| vkForceOpacity, vkHideDelay | Nâng cao |
| 5 ô màu HEX | Nâng cao → "Màu bàn phím"; editor HUD dùng lại cùng kiểu |
| keyMappings / keyCodeMap | Nâng cao → "Gán phím" (màn hình riêng đang có) |
| soundBank | Âm thanh |
| disablePause | Nâng cao |
| m3gForceDisableLighting | Nâng cao |
| systemProperties, encoding | Nâng cao, mặc định ẩn sau công tắc "Hiện tuỳ chọn hệ thống" |
| shader | ẩn hẳn nếu game không dùng shader (tự phát hiện) |

### 3.4 Gợi ý cấu hình (khi mở game lần đầu)
- Độ phân giải game ≤ 240x320 và màn hình cao hơn → Vừa khít + giữ tỉ lệ + toàn màn hình.
- Game 2D (không dùng M3G) → Làm mượt hình bật.
- Chưa có bàn phím → bật bàn phím ảo kiểu Nokia, rung bật.
- Không bao giờ bật: hiện FPS, immediate, parallel redraw.

---

## 4. Sắp xếp lại source

### 4.1 Hiện trạng gói
```
com.j2me.lixin            (3)  Application, MainActivity, ForegroundService
  applist(5) appdb(4) base(1) config(13) crashes(4+7) filepicker(2)
  info(4) settings(5) storage(1) util(15)
javax.microedition.*           API giả lập — KHÔNG đổi tên
org.microemu.*                 CLDC impl — KHÔNG đổi tên
com.nokia/siemens/motorola/... shim hãng máy — KHÔNG đổi tên
ru.woesss.j2me.{micro3d,jar,installer}   thư viện nội bộ
ru.playsoftware.j2meloader.hud           HUD (sẽ thành com.j2me.lixin.hud)
```

### 4.2 Đích
```
com.j2me.lixin
  .app            EmulatorApplication, MainActivity, ForegroundService
  .ui             applist, settings, filepicker, info  (màn hình)
  .ui.config      ConfigActivity + màn hình cấu hình mới
  .config         ProfileModel, ProfilesManager, Config
  .hud            toàn bộ HUD (port từ Anland)
  .data           appsdb, storage
  .util           util, crashes
  .internal       micro3d, jar, installer  (từ ru.woesss)
javax.microedition.* / org.microemu.* / shim hãng máy: giữ nguyên
```
Quy tắc: chỉ đổi tên gói của code CỦA MÌNH và thư viện nội bộ.
Không đụng `javax.microedition`, `org.microemu`, các gói shim (game J2ME tham chiếu
trực tiếp bằng tên gói), không đụng `com.j2me.lixin.dexlib` (module riêng).

### 4.3 Cách làm (một commit máy móc, không sửa logic)
1. Di chuyển thư mục + đổi `package`/`import` bằng sed theo đúng bảng trên.
2. Sửa tên class trong `AndroidManifest.xml` và mọi chuỗi phản chiếu (reflection).
3. Build + cài + mở thử toàn bộ màn hình chính, cấu hình, danh sách game, backup.
4. Chỉ commit khi build xanh và không có ClassNotFound lúc chạy.

---

## 5. Pha triển khai

| Pha | Nội dung | Thời gian | Ghi chú |
|---|---|---|---|
| S1 | Sắp xếp lại source (mục 4) | 0,5 ngày | làm TRƯỚC khi port HUD để 32 file mới vào đúng chỗ |
| S2 | Tách cấu hình: dựng 6 nhóm + "Nâng cao" giữ nguyên trang cũ | 1,5 ngày | chỉ di chuyển UI, không đổi logic lưu |
| S3 | Màn hình "Chơi" + cấu hình gợi ý + "Khôi phục gợi ý" | 1 ngày | |
| S4 | Dọn chữ nghĩa: đổi nhãn kỹ thuật sang tiếng người | 1 ngày | 45 nhãn + mô tả 1 dòng |
| S5 | Nhóm dữ liệu: sao lưu / xuất nhập cấu hình | 1 ngày | |
| S6 | Rà lại toàn bộ + ảnh chụp từng màn | 0,5 ngày | |

Tổng ~5,5 ngày. Có thể làm song song với HUD port nhưng nên xong S1 trước.

---

## 6. Rủi ro

| Rủi ro | Cách chặn |
|---|---|
| Đổi gói làm vỡ manifest/reflection → crash lúc mở | grep toàn bộ chuỗi tên class trước khi commit; test từng màn |
| Người dùng cũ mất thói quen vì mục bị ẩn | "Nâng cao" chứa đủ 45 mục, y nguyên thứ tự |
| Hai nơi cùng chỉnh một giá trị (cấu hình cũ vs editor HUD) | Quy tắc mục 4.6 trong HUD_PORT_PLAN: renderer quyết định nhóm nào hiện |
| Nhãn mới sai nghĩa | Mỗi mục giữ 1 dòng mô tả lấy từ tài liệu upstream |

---

## 7. Nghiệm thu
1. Cài mới → mở game → chơi được ngay, không phải mở cấu hình.
2. Mở "Cấu hình khác" → 6 nhóm, mỗi nhóm ≤ 4 mục, không có thuật ngữ khó hiểu.
3. "Nâng cao" có đủ 45 mục như trang cũ, chỉnh được như cũ.
4. "Khôi phục gợi ý" trả về đúng bộ mặc định.
5. Không màn hình nào crash sau khi đổi tên gói.
6. Toàn bộ cấu hình cũ của từng game vẫn đọc đúng sau khi nâng cấp.
