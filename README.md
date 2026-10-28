# Electrical Circuit Simulator

Bài tập lớn môn Lập trình hướng đối tượng (OOP) – HUST.

## 1. Giới thiệu

Xây dựng ứng dụng mô phỏng và phân tích mạch điện đơn giản bằng Java, hỗ trợ:

- Mạch nối tiếp và mạch song song.
- Điện trở (Resistor), tụ điện (Capacitor), cuộn cảm (Inductor).
- Nguồn điện một chiều (DC) và xoay chiều (AC).
- Tính toán điện áp, dòng điện và trở kháng tương đương.
- Phát hiện ngắn mạch và thông báo lỗi.
- Hiển thị sơ đồ mạch điện và bảng kết quả tính toán.

**Công nghệ dự kiến:**
- Java
- JavaFX (giao diện)
- Maven (quản lý project)
- JUnit (kiểm thử)

## 2. Cấu trúc thư mục

```text
src/
├── main/
│   ├── java/circuitsim/
│   │   ├── Main.java
│   │   │
│   │   ├── model/
│   │   │   ├── Complex.java
│   │   │   ├── CircuitElement.java
│   │   │   ├── Resistor.java
│   │   │   ├── Capacitor.java
│   │   │   ├── Inductor.java
│   │   │   ├── VoltageSource.java
│   │   │   ├── ACSource.java
│   │   │   ├── DCSource.java
│   │   │   ├── Circuit.java
│   │   │   ├── SeriesCircuit.java
│   │   │   └── ParallelCircuit.java
│   │   │
│   │   ├── logic/
│   │   │   ├── CircuitAnalyzer.java
│   │   │   ├── CircuitValidator.java
│   │   │   ├── AnalysisResult.java
│   │   │   └── ElementResult.java
│   │   │
│   │   ├── controller/
│   │   │   └── CircuitController.java
│   │   │
│   │   └── view/
│   │       ├── CircuitView.java
│   │       └── CircuitRenderer.java
│   │
│   └── resources/
│
└── test/
    └── java/com/hust/circuitsim/
```

Cấu trúc trên là định hướng ban đầu, có thể điều chỉnh khi triển khai nếu cả nhóm thống nhất.

## 3. Phân chia trách nhiệm các package

| Package | Chức năng |
|---|---|
| `model` | Biểu diễn các đối tượng, thuộc tính và trạng thái của mạch điện |
| `logic` | Kiểm tra tính hợp lệ, phân tích mạch và tính toán kết quả |
| `controller` | Tiếp nhận thao tác người dùng, điều phối giữa các phần |
| `view` | Hiển thị giao diện, sơ đồ mạch điện và kết quả |

### Quy tắc tổ chức code

- Mỗi class nên đảm nhiệm một trách nhiệm chính.
- Ưu tiên đóng gói dữ liệu, sử dụng kế thừa và đa hình khi phù hợp.
- `model` không phụ thuộc vào `logic`, `controller` hoặc `view`.
- `logic` có thể sử dụng `model`, nhưng không phụ thuộc vào GUI.
- `controller` chịu trách nhiệm kết nối các thành phần.
- `view` chỉ xử lý hiển thị và tương tác giao diện, không trực tiếp tính toán mạch điện.
- Những hành vi đặc trưng của đối tượng, chẳng hạn tính trở kháng của linh kiện, có thể được triển khai trong `model`.
- Không tự ý thay đổi API hoặc cấu trúc chung khi chưa trao đổi với nhóm.

## 4. Quy ước Git

### Các nhánh

- `main`: Nhánh chính, luôn giữ ở trạng thái ổn định.
- `feature/<ten-chuc-nang>`: Phát triển chức năng mới.
- `fix/<ten-loi>`: Sửa lỗi.
- `refactor/<mo-ta>`: Cải thiện cấu trúc code.
- `docs/<mo-ta>`: Chỉnh sửa tài liệu.

Ví dụ:

```text
feature/complex-number
feature/circuit-components
feature/circuit-analysis
feature/gui
fix/short-circuit
```

### Quy ước commit

| Tiền tố | Ý nghĩa |
|---|---|
| `feat:` | Thêm chức năng |
| `fix:` | Sửa lỗi |
| `refactor:` | Chỉnh sửa cấu trúc code |
| `test:` | Thêm hoặc sửa kiểm thử |
| `docs:` | Chỉnh sửa tài liệu |
| `chore:` | Cấu hình, công việc phụ trợ |

Ví dụ:

```text
feat: implement complex number operations
fix: handle division by zero
refactor: simplify circuit analyzer
test: add resistor impedance tests
```

### Quy trình làm việc

1. Nhận công việc thông qua GitHub Issues hoặc phân công trong nhóm.
2. Cập nhật nhánh `main` mới nhất.
3. Tạo branch riêng để thực hiện công việc.
4. Code và commit theo từng phần.
5. Push branch lên GitHub.
6. Tạo Pull Request (PR) vào `main`.
7. Chờ review và chỉnh sửa nếu cần.
8. Merge khi code đã được kiểm tra.

**Không push trực tiếp lên `main`.**

## 5. Quy tắc làm việc nhóm

- Mỗi thành viên chịu trách nhiệm chính với phần được phân công.
- Có thể sửa code ở package khác nếu cần, nhưng phải thông báo khi ảnh hưởng đến công việc của người khác.
- Không tự ý thay đổi chữ ký phương thức hoặc interface đã thống nhất.
- Không commit file build, file tạm hoặc cấu hình IDE cá nhân.
- Kiểm tra project build thành công trước khi tạo PR.
- Với các chức năng tính toán, nên có unit test.
- Ưu tiên PR nhỏ, tập trung vào một chức năng để dễ review.
- Khi gặp vấn đề ảnh hưởng đến tiến độ hoặc kiến trúc chung, trao đổi trong nhóm trước khi tiếp tục.

## 6. Phân công công việc

Việc phân công cụ thể sẽ được quản lý thông qua GitHub Issues.

Các phần chính:

- **Model:** Xây dựng linh kiện, nguồn điện và cấu trúc mạch.
- **Logic
