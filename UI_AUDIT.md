# UI AUDIT — J2ME Loader Lixin

Cập nhật 2026-11-17 · nhánh `lixin-clean` · phạm vi: 27 layout, 8 menu, 327 chuỗi, 10 màn hình.

## 1. Đã sửa (3 vòng, mỗi vòng build + cài thật)

| Vấn đề | Trước | Sau |
|---|---|---|
| Chuỗi thiếu tiếng Việt | 91 | 0 (327/327) |
| Nút chỉ có hình, không nhãn đọc màn hình | 5 | 0 |
| Nhãn cứng trong layout | 3 | 0 |
| Dòng cấu hình cao 40dp | tất cả 45 dòng | 48dp |
| Nút/khoảng chạm khai báo cứng dưới 48dp | 46 chỗ | 0 |
| Thanh trên cùng (ActionBar) | 38dp | 56dp |
| Màu cứng trong KeyMapper | 1 | 0 (dùng màu theme) |

Các chỗ đã nâng lên 48dp: nút thêm tab và menu trong game, hai nút thanh nổi, nút bút chì
trong danh sách tệp và trong hộp thoại tài khoản, toàn bộ nút 38dp/40dp của trang cấu hình.

## 2. Kiểm bằng máy thật — kết quả và giới hạn

- Màn danh sách game: chụp đủ 36 phần tử. Phát hiện: thanh trên cùng 38dp (đã sửa thành 56dp),
  nút "Tùy chọn khác" rộng 40dp dính sát mép phải (nay là action item trong ActionBar 56dp),
  nút nổi thêm game không có mô tả.
- 9 màn còn lại: `uiautomator dump` trả `ERROR: null root node returned by UiTestAutomationBridge`
  dù màn hình đã bật chế độ không tắt và cửa sổ đang vẽ bình thường
  (`mHasSurface=true`, `mDrawState=HAS_DRAWN`, `isReadyForDisplay()=true`).
  Đã thử: đánh thức máy, tắt keyguard, xoá tiến trình uiautomator, đổi đích ghi.
  Kết luận: đây là giới hạn công cụ trên ROM này, **không phải lỗi app**.
  Muốn kiểm phần tử của 9 màn đó phải làm bằng tay hoặc bằng ảnh chụp.

## 3. Còn mở — cần thiết kế lại

1. Trang cấu hình vẫn là "nhãn + điều khiển trên một dòng". Đã đủ 48dp nhưng bố cục vẫn là
   gốc của cảm giác rối; hướng đúng là card có tiêu đề + mô tả, điều khiển nằm dưới.
2. Nút nổi thêm game: mới có icon, chưa có mô tả cho TalkBack (sửa trong mã, không phải layout).
3. Các hàng game trong danh sách bấm được nhưng bản thân hàng không có mô tả gộp.
4. Trang Cài đặt dùng Preference nên lệch kiểu chữ/màu so với phần còn lại.
5. HUD editor và bàn phím ảo vẽ trên canvas: uiautomator không thấy phần tử, phải kiểm bằng tay.

## 4. Thứ tự đề xuất

1. Dựng lại trang cấu hình theo card (gộp với "3 lớp Chơi / Cơ bản / Nâng cao").
2. Thêm mô tả cho nút nổi và hàng game; đồng bộ trang Cài đặt.
3. Kiểm tay 9 màn theo checklist 14 mục trong SOURCE_MAP.md, ghi ảnh từng màn.
