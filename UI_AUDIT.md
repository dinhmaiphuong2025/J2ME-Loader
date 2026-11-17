# UI AUDIT — J2ME Loader Lixin

Ngày 2026-11-17 · nhánh `lixin-clean` · phạm vi: 27 layout, 8 menu, 327 chuỗi, 10 màn hình.

Cách kiểm: quét tĩnh toàn bộ XML/JAVA (không bỏ sót file nào) + một lượt chụp cây UI thật
bằng `uiautomator` trên máy. Mọi con số dưới đây đều lấy từ kết quả quét, không ước lượng.

## 1. Đã sửa trong đợt này

| Vấn đề | Trước | Sau |
|---|---|---|
| Chuỗi giao diện chưa dịch | 91 chuỗi tiếng Anh hiện giữa giao diện tiếng Việt | 327/327 đã dịch |
| Nút chỉ có hình, không có nhãn đọc màn hình | 5 (4 layout danh sách + 1 nút trong cấu hình) | 0 |
| Nhãn cứng trong layout (không dịch được) | 3 (`Audio`, `Soundbank:`, ` x `) | 0 |
| Chiều cao mỗi dòng cấu hình | 40dp (dưới chuẩn 48dp) | 48dp |

Chi tiết: `dimens.xml` đặt `height_config_row = 40dp` và **mọi** dòng của trang cấu hình dùng
giá trị này, nên toàn bộ 45 mục đều thấp hơn vùng chạm tối thiểu — đây là lý do thật khiến
trang cấu hình khó bấm, không phải cảm giác.

## 2. Còn mở — cần thiết kế lại, không phải sửa một dòng

1. **Dòng cấu hình vẫn dùng chung một chiều cao cứng.** Đã nâng lên 48dp nhưng cách bố trí
   (mỗi mục một dòng, nhãn + điều khiển chen nhau trong ConstraintLayout) vẫn là gốc của
   cảm giác rối. Hướng đúng: mỗi mục là một card có tiêu đề + mô tả, điều khiển nằm dưới.
2. **Thanh trên cùng của danh sách game cao 38dp**, nút "Tùy chọn khác" rộng 40dp và dính sát
   mép phải màn hình (`x2 = 1080`), đo từ cây UI thật → khó chạm, dễ trượt.
3. **Nút nổi thêm game không có nhãn** (chỉ có icon), TalkBack đọc là "nút".
4. **Các hàng game trong danh sách** bấm được nhưng bản thân hàng không có mô tả; chỉ đọc được
   chữ bên trong nên trình đọc màn hình đọc rời từng mảnh.
5. **1 màu cứng** trong `activity_keymapper.xml` (dòng 278) — nên dùng thuộc tính theme để
   đổi theo Material You.
6. **Trang Cài đặt dùng Preference** nên không theo khuôn Material You như phần còn lại;
   tiêu đề/mô tả là hai kiểu chữ khác nhau so với các màn khác.

## 3. Bị chặn — cần chạy lại khi màn hình không ngủ

Lượt chụp cây UI tự động thất bại một phần: `uiautomator dump` trả
`ERROR: null root node returned by UiTestAutomationBridge` khi màn hình rơi vào
`DOZE_SUSPEND` (AOD). Chỉ màn danh sách game chụp được đầy đủ (36 phần tử).

Muốn audit tự động toàn bộ 10 màn cần: bật `svc power stayon true` trong lúc chạy, rồi mới
đi từng màn. Chưa làm vì sẽ giữ màn hình sáng liên tục trên máy bạn.

## 4. Bề mặt chưa kiểm bằng máy được

HUD editor và bàn phím ảo tự vẽ lên canvas, `uiautomator` không thấy phần tử bên trong.
Phải kiểm bằng tay hoặc bằng ảnh chụp: toolbar, inspector, kéo/thả, snap, undo/redo, cả hai
chiều xoay, phản hồi rung, độ mờ.

## 5. Thứ tự sửa đề xuất

1. Hàng đợi Critical: nút không có nhãn, vùng chạm < 48dp ở thanh trên cùng (đã xong phần nền).
2. Trang cấu hình: chuyển sang card có tiêu đề + mô tả (việc lớn nhất, gộp với đợt "3 lớp").
3. Trang Cài đặt: đồng bộ kiểu chữ và màu với phần còn lại.
4. Rà lại toàn bộ bằng máy khi màn hình giữ sáng, ghi báo cáo có ảnh.
