# Simple2D — extension độc lập

Từ thư mục dự án: `.\simple2d\run.cmd` hoặc nhấp đúp file này.
Cần JDK 21+, Maven; lần đầu cần Internet nếu thiếu dependency.
Script dùng JDK/Maven trên PATH hoặc vị trí đã cài trên máy hiện tại.
Không dùng hay thay entry point cũ trong `pom.xml`.

## Lắp và tính mạch

1. Thêm R/C/L hoặc nguồn DC/AC; chọn và kéo linh kiện để bố trí.
2. Bật **Nối dây**, nhấn hai vòng tròn đầu cực. Một đầu cực được nối nhiều dây.
3. Bật **GND**, nhấn một đầu cực làm tham chiếu. Khi thêm nguồn đầu tiên,
   cực 2 được chọn làm ground mặc định; có thể đổi sau.
4. Chọn linh kiện để nhập giá trị **SI**: Ω, F, H, V; dùng `1e-6` cho 1 µF.
   Nguồn AC dùng **V đỉnh**, f > 0 Hz, pha theo **độ**. Bấm Áp dụng.
5. Nhấn **Tính mạch**. Có mẫu chia áp DC và RC AC để kiểm tra nhanh.

Chọn/kéo rồi nhấn một dây để chọn, sau đó **Xóa đã chọn** để tháo.
Chế độ Xóa cho phép nhấn trực tiếp dây/linh kiện. Esc hủy dây đang nối.
Xoay 90° chỉ đổi bố trí, giữ nguyên hai đầu cực. Dây giao nhau không tự nối;
nối vào cùng vòng tròn đầu cực để tạo nhánh. Cực 1 là bên trái ở góc 0°.
Không hỗ trợ điểm rẽ ở giữa dây; có thể nối nhiều dây vào cùng đầu cực.

- **Phasor / norm:** U, I dạng a+jb; norm là |phasor| và pha theo từng tần số.
- **RMS / công suất:** RMS tổng hợp các tần số và DC, công suất trung bình.
  AC phasor được xem là biên độ đỉnh: RMS mỗi tần số = norm / √2.
- **Graph / node:** các đầu cực được gộp, cạnh graph, ground và điện thế node.

Các đầu cực được DSU gộp theo dây nối thành node; mỗi linh kiện trở thành một `Edge`.
`CircuitBridge` gọi `NodalSolver` để nhận `CircuitSolution` rồi chuyển trực tiếp
cho `CircuitAnalyzer`. Solver cung cấp bảng chỉ số node và dòng nhánh theo từng
tần số; UI không tự dựng lại thứ tự ẩn MNA.
Nguồn có thể mắc giữa hai node bất kỳ, nhiều nguồn/tần số, mạch nối tiếp,
song song hoặc hỗn hợp trong phạm vi solver hiện tại. Hệ suy biến/mạch rời,
nguồn mâu thuẫn hoặc bị ngắn mạch sẽ báo lỗi; không hiển thị kết quả cũ.
Khi tính thành công, UI tự mở bảng RMS. Khi lỗi, có khung đỏ ngay dưới
thanh công cụ. Mạch hở vẫn được tính nếu điện thế xác định (dòng có thể bằng 0).
Node thả nổi được báo kèm đầu cực liên quan. Ở DC xác lập, cuộn cảm là ngắn
mạch lý tưởng: mắc nối tiếp điện trở vẫn hợp lệ; mắc trực tiếp qua nguồn áp
khác 0 sẽ báo ngắn mạch. Tụ điện hở mạch ở DC.
Hiện dùng thông số linh kiện lý tưởng, chưa tính dung sai linh kiện.
Chưa có lưu/tải, phân tích quá độ hoặc kiểm tra topology chi tiết.

## Gỡ bỏ

Chỉ xóa ba thư mục mới sau, không cần sửa code lõi hoặc `pom.xml`:

- `src/main/java/circuitsim/extension/simple2d/`
- `src/test/java/circuitsim/extension/simple2d/`
- `simple2d/`

Chạy `mvn clean` sau khi gỡ để bỏ class biên dịch cũ.

Kiểm thử extension: `mvn "-Dtest=CircuitBridgeTest" test`.
