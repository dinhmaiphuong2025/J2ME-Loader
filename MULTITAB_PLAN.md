# Kế hoạch multi-tab J2ME Loader Lixin

## Mục tiêu
Biến J2ME-Loader thành bản "treo nhiều tab": mỗi tab là một game J2ME full-screen,
chuyển tab nhanh, game nền không bị giết, mỗi tab cấu hình + profile riêng.

## Trạng thái hiện tại (đã xong, build xanh)
Nhánh `multitab`, package `com.j2me.lixin` (debug `com.j2me.lixin.debug`).

Đã làm:
1. `J2meContext` — registry (ConcurrentHashMap) + InheritableThreadLocal `current()`.
2. `MidletThread` → per-context (bỏ static `instance`).
3. `Display` + `EventQueue` → per-context.
4. `MicroActivity` implement `UiHandler` (routing displayable theo context).
5. Top bar: pill tab bar (bỏ Toolbar) + PopupMenu (nút ⋮).
6. Tab list + switch + close 1 tab.
7. Nút "+" → dialog chọn game in-process (đọc filesystem).

Đang lỗi (3 vấn đề chính):
- (A) Vuốt home → game thoát (process `:midlet` bị Android giết khi về nền).
- (B) Màn hình đen khi 2 tab (config render dùng chung, chưa tách per-context).
- (C) 2 tab cùng game dùng chung profile/save (AppClassLoader dataDir static).

---

## Pha 1 — Foreground service: game sống khi về nền
Giải quyết (A). Đây là việc HungHero làm bằng foreground service + bong bóng nổi.

Các bước:
1. Tạo class `ForegroundService` (javax.microedition.shell hoặc com.j2me.lixin):
   - `onStartCommand` → `startForeground(id, notification)`.
   - Notification channel + notification tối giản (tên "J2ME Loader đang chạy").
2. `AndroidManifest.xml`:
   - `<service android:name="...ForegroundService" android:process=":midlet"
       android:foregroundServiceType="specialUse" />`
   - `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />`
   - `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />`
   - `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`
3. `MicroActivity`:
   - `onCreate` (tab đầu) → `startForegroundService(...)`.
   - Khi đóng tab CUỐI → `stopService(...)`.
   - Xin quyền `POST_NOTIFICATIONS` khi cần (Android 13+).
4. Verify: mở game → vuốt home → game vẫn sống (quay lại vẫn đang chạy, không bị thoát).

Rủi ro: notification hiện trên thanh trạng thái (bắt buộc với foreground service).
Có thể ẩn notification nếu chấp nhận dùng `dataSync`/loại khác tuỳ máy.

---

## Pha 2 — Config render per-context: hết đen màn hình
Giải quyết (B). Đây là phần lớn nhất.

Vấn đề: các setter config dưới đây là STATIC, dùng chung cho 2 game:
- `Canvas.setScale / setFilterBitmap / setGraphicsMode / setShaderFilter /
  setBackgroundColor / setShowFps / setLimitFps / setForceFullscreen /
  setHasTouchInput / setScreenshotRawMode`
- `Displayable.setVirtualSize`
- `Font.applySettings`
- `KeyMapper.setKeyMapping`
- `Graphics3D.initGraphics3D / setUnlitTexturesWorkaround`

Cách làm (theo đúng pattern HungHero đã port):
1. Thêm các field config vào `J2meContext` (scale, filter, graphicsMode, shader,
   backgroundColor, fpsLimit, showFps, forceFullscreen, touchInput, virtualW/H,
   font settings, key mapping...).
2. `MicroLoader.applyConfiguration()` → ghi config vào `J2meContext.current()`
   thay vì gọi setter static.
3. `Canvas`/`Displayable`/`Font`/`KeyMapper`/`Graphics3D` đọc config qua
   `J2meContext.current()` khi render, thay vì đọc field static.
4. Làm TỪNG class một, build + test sau mỗi class (giữ game đơn vẫn chạy).

Verify: mở 2 game khác config (scale/độ phân giải khác) → chuyển tab, cả 2
render đúng, không đen.

Rủi ro: `Canvas` dài (~1436 dòng) + SurfaceView/GL render phức tạp; đây là phần
dễ phát sinh bug render nhất. Cần test game đơn kỹ sau mỗi bước.

---

## Pha 3 — Data dir per-tab: tách profile
Giải quyết (C). Chỉ quan trọng khi mở CÙNG 1 game nhiều tab.

Vấn đề: `AppClassLoader` có `instance / zipFile / dataDir` static → 2 tab cùng
game dùng chung thư mục save → chung tài khoản → game chặn anti-DDOS.

Các bước:
1. `AppClassLoader` bỏ static → mỗi context giữ 1 instance riêng.
2. `J2meContext` thêm field classLoader + dataDir.
3. Data dir: tab đầu giữ nguyên (`Config.getDataDir() + appDirName`) để không
   mất save cũ; tab thứ 2 trở đi dùng hậu tố (`..._2`, `..._3`) → profile mới toanh.
4. Verify: mở cùng game 2 tab → tab 2 tài khoản mới, tab 1 vẫn tài khoản cũ.

Rủi ro: đụng `ContextHolder.openFileInput/openFileOutput` + `getFileByName` +
`AppClassLoader.getDataDir` (nhiều chỗ gọi). Phải đổi hết về dataDir của context.

---

## Pha 4 — Hoàn thiện UI + lifecycle
Sau khi 3 pha trên ổn:
1. Nút X trên mỗi pill để đóng tab (hiện chỉ đóng qua Exit của game).
2. Pill hiển thị trạng thái (đang chạy / đang pause).
3. Lưu danh sách tab khi app bị kill (tuỳ chọn, restore lại sau).
4. (Tuỳ chọn) bong bóng nổi kiểu HungHero — thu nhỏ tab thành bubble.

---

## Thứ tự + tiêu chí dừng
Làm lần lượt Pha 1 → 2 → 3 → 4. Mỗi pha build + test game đơn vẫn OK rồi mới
sang pha sau. Pha 2 là nặng nhất; nếu Pha 2 quá phức tạp có thể tách nhỏ hơn
(Canvas trước, rồi Font/KeyMapper/Graphics3D).
