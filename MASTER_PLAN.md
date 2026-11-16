# KẾ HOẠCH TỔNG (MASTER) — J2ME LOADER LIXIN

Gộp toàn bộ phạm vi còn lại vào một kế hoạch, thay vì làm lắt nhắt từng mục.
Cách thi công: chia 4 ĐỢT, mỗi đợt làm liền một mạch nhiều hạng mục rồi mới build
và đưa bạn test một lần — không còn cảnh mỗi lượt sửa một nút menu.

Đã có sẵn (không tính vào phạm vi): đa tab, chọn tài khoản theo game, tài khoản đang
chạy bị ẩn khỏi danh sách, pill hiện "game (tài khoản)", Material You, bản dịch tiếng
Việt (values-vi), Lucide icon, backup/restore, ACRA crash report, key mapper.

---

## A. HUD (thay toàn bộ hệ chỉnh phím ảo)

| Mã | Hạng mục | Ghi chú |
|---|---|---|
| A1 | Nối HUD ↔ game: bảng đổi mã phím, postKeyPressed/Released đúng luồng, đa điểm, lặp phím | nền của mọi thứ |
| A2 | Mở editor từ menu ⋮; lưu/đọc layout theo từng game | thay chỗ editor cũ đã xoá |
| A3 | Cắt inspector cho J2ME: ẩn chuột/scroll/modifier; giữ Style + Transform + Event (phím) | |
| A4 | Preset Anland/NRO + bộ 381 icon NRO gán cho từng nút | |
| A5 | Widget joystick analog 4/8 hướng (lõi HudDpadInput + núm kiểu touchpad) | Anland không có |
| A6 | Công tắc renderer: keypad cổ điển ↔ HUD; hai lớp HUD / bàn phím | chống double key |
| A7 | Xuất/nhập layout HUD ra file .json (chia sẻ, sao lưu) | |
| A8 | Layout riêng cho ngang và dọc | Anland có sẵn, chỉ giữ 2 |

## B. Cấu hình — làm lại cho người dùng thường

| Mã | Hạng mục | Ghi chú |
|---|---|---|
| B1 | 3 lớp: Chơi / Cơ bản (6 nhóm) / Nâng cao (đủ 45 mục cũ) | SETTINGS_UX_PLAN.md |
| B2 | Cấu hình gợi ý theo game (độ phân giải, 2D/3D, có bàn phím chưa) | |
| B3 | Nút "Khôi phục gợi ý" cho từng game | |
| B4 | Tìm kiếm trong cấu hình | |
| B5 | Viết lại 302 nhãn + mô tả sang tiếng người (bỏ thuật ngữ) | |
| B6 | 2–3 bộ cấu hình nhanh mỗi game: "Mượt", "Đẹp", "Tiết kiệm pin" | |

## C. Thư viện game

| Mã | Hạng mục |
|---|---|
| C1 | Lưới/danh sách + tìm kiếm + sắp xếp (mới chơi, tên, dung lượng) |
| C2 | Nhóm/tag: đang chơi, đã xong, thể loại (tự gắn + tự do) |
| C3 | Hiện tài khoản đang chạy ngay trên item |
| C4 | Menu giữ: Chơi / Tài khoản / Cấu hình / Xoá / Chia sẻ (gọn, không còn 10 mục) |
| C5 | Ảnh bìa tự sinh từ icon game + màu chủ đạo (Material You) |

## D. Đa nhiệm (lấy từ HungHero)

| Mã | Hạng mục |
|---|---|
| D1 | Bong bóng nổi cho tab đang chạy (thu nhỏ, kéo, mở lại) |
| D2 | Thông báo có nút: mở / đóng game, số tab đang chạy |
| D3 | Khôi phục tab sau khi app bị hệ thống kill |
| D4 | Giới hạn số tab + tự đóng tab nền khi thiếu RAM |

## E. Ổn định & hiệu năng

| Mã | Hạng mục |
|---|---|
| E1 | Màn hình xem log + gửi báo lỗi (nâng từ saveLog hiện có) |
| E2 | FPS overlay + cảnh báo khi tụt dưới ngưỡng |
| E3 | Đo RAM theo tab, cảnh báo trước khi hết |
| E4 | Sửa lỗi im lặng: ghi log mọi ngoại lệ trong vòng lặp game |

## F. Source & build

| Mã | Hạng mục | Lý do |
|---|---|---|
| F1 | Nốt reorg: `ru.woesss.j2me.*` → `com.j2me.lixin.internal.*` | nhất quán |
| F2 | Tách module: core (javax.microedition) / app / hud | build nhanh, rõ ràng |
| F3 | Bỏ sqlite-jdbc (đang phải vá JAR tay) → dùng SQLite của Android | hết phụ thuộc mong manh |
| F4 | CI GitHub Actions + ký số tự động | build không cần máy |
| F5 | Unit test cho serialize layout HUD + account | chống hồi quy |

## G. Hoàn thiện trải nghiệm

| Mã | Hạng mục |
|---|---|
| G1 | Onboarding lần đầu: chọn thư mục, cấp quyền, thêm game |
| G2 | Kéo-thả file .jar vào app để cài |
| G3 | Chế độ một tay (đảo cụm phím trái/phải) |
| G4 | Mẫu rung tuỳ chọn |
| G5 | Sáng/tối theo hệ thống + Material You theo wallpaper (đã có, rà lại) |

---

## Cách thi công: 4 ĐỢT

### ĐỢT 1 — HUD chạy được (A1 → A3)
Kết quả: mở menu → HUD Editor → kéo/thả/resize/snap/đổi màu/đổi phím → Xong → chơi
bằng HUD, lưu riêng từng game. Build + test 1 lần.

### ĐỢT 2 — HUD hoàn chỉnh (A4 → A8)
Kết quả: preset NRO + icon, joystick, công tắc keypad/HUD, xuất nhập layout.

### ĐỢT 3 — Cấu hình + thư viện (B1 → B6, C1 → C5)
Kết quả: mở game là chơi; cấu hình 3 lớp; thư viện có tìm kiếm/nhóm/menu gọn.

### ĐỢT 4 — Đa nhiệm + ổn định + source (D, E, F, G)
Kết quả: bong bóng nổi, khôi phục tab, bỏ phụ thuộc JAR vá tay, CI, onboarding.

Ước lượng: Đợt 1 ~2 ngày; Đợt 2 ~3 ngày; Đợt 3 ~4 ngày; Đợt 4 ~5 ngày.

---

## Thứ tự ưu tiên đề xuất
1. Đợt 1 (HUD chạy) — vì đang dở, xong là thay được editor cũ.
2. Đợt 3 (cấu hình + thư viện) — cái bạn kêu khó dùng nhất, và độc lập với HUD.
3. Đợt 2 (HUD hoàn chỉnh) — làm sau khi có chỗ đặt tính năng trong cấu hình mới.
4. Đợt 4 — dài hơi, làm cuối.

## Nguyên tắc build/test
- Mỗi đợt: làm hết hạng mục → build xanh → đưa 1 APK → bạn test 1 lần.
- Trong đợt, commit theo cụm logic (không commit từng dòng).
- Lỗi phát sinh sau test: gom thành một đợt sửa, không sửa lẻ.
