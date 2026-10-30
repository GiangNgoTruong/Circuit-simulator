# Electrical Circuit Simulator

Bài tập lớn môn **Lập trình hướng đối tượng (OOP)** – Đại học Bách khoa Hà Nội (HUST).

## 1. Giới thiệu

Xây dựng ứng dụng mô phỏng và phân tích mạch điện RLC đơn giản bằng Java. Người dùng có thể đặt linh kiện, nối dây giữa các chân linh kiện và yêu cầu chương trình phân tích mạng điện đã tạo.

**Chức năng dự kiến:**

- Hỗ trợ điện trở (**Resistor**), tụ điện (**Capacitor**), cuộn cảm (**Inductor**).
- Hỗ trợ nguồn điện áp một chiều (**DC**) và xoay chiều (**AC**).
- Cho phép người dùng thêm, xóa, chỉnh sửa linh kiện và nối dây tùy ý giữa các chân kết nối.
- Biểu diễn mạch nối tiếp, song song, hỗn hợp và các mạng điện có nhiều nhánh bằng **Graph**.
- Tính điện áp, dòng điện và trở kháng trên từng linh kiện; tính trở kháng tương đương nhìn từ hai cực nguồn khi xác định được.
- Phát hiện cấu hình không hợp lệ, mạch hở, linh kiện bị nối tắt và ngắn mạch nguồn.
- Hiển thị sơ đồ mạch và bảng kết quả phân tích.

**Công nghệ dự kiến:** Java, JavaFX (giao diện), Maven (quản lý dự án), JUnit (kiểm thử).

> **Phạm vi:** Cấu trúc Graph là định hướng để hỗ trợ kết nối tùy ý. Nhóm sẽ ưu tiên các mạch RLC tuyến tính với một nguồn áp, ở chế độ DC xác lập hoặc AC xác lập hình sin. Các chức năng mở rộng (nhiều nguồn, phân tích quá độ, tự động vẽ mạch tương đương) chỉ triển khai nếu có thời gian và được nhóm thống nhất.

## 2. Cấu trúc thư mục

```text
Circuit-simulator/
├── pom.xml
├── .gitignore
├── README.md
├── docs/
│   └── design/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── circuitsim/
    │   │       ├── Main.java
    │   │       ├── model/
    │   │       │   ├── Complex.java
    │   │       │   ├── CircuitElement.java
    │   │       │   ├── Resistor.java
    │   │       │   ├── Capacitor.java
    │   │       │   ├── Inductor.java
    │   │       │   ├── VoltageSource.java
    │   │       │   ├── ACSource.java
    │   │       │   ├── DCSource.java
    │   │       │   ├── Circuit.java
    │   │       │   ├── Terminal.java
    │   │       │   ├── Wire.java
    │   │       │   ├── CircuitGraph.java
    │   │       │   └── Branch.java
    │   │       ├── logic/
    │   │       │   ├── TopologyBuilder.java
    │   │       │   ├── CircuitValidator.java
    │   │       │   ├── CircuitAnalyzer.java
    │   │       │   ├── NodalSolver.java
    │   │       │   ├── AnalysisResult.java
    │   │       │   └── ElementResult.java
    │   │       ├── controller/
    │   │       │   └── CircuitController.java
    │   │       └── view/
    │   │           ├── CircuitView.java
    │   │           └── CircuitRenderer.java
    │   └── resources/
    └── test/
        └── java/
            └── circuitsim/
```

Cấu trúc trên là **định hướng thiết kế**, không khẳng định mọi class đã được triển khai. Có thể điều chỉnh sau khi thống nhất API và UML.

## 3. Kiến trúc và trách nhiệm các package

Dự án tổ chức theo **Layered Architecture**, có sự phân chia trách nhiệm theo hướng MVC.

| Package | Trách nhiệm |
|---|---|
| `model` | Biểu diễn linh kiện, nguồn, chân kết nối, dây nối và Graph mạch điện. |
| `logic` | Chuẩn hóa kết nối, kiểm tra mạch, lập và giải hệ phương trình, tổng hợp kết quả. |
| `controller` | Tiếp nhận thao tác người dùng và điều phối `model`, `logic`, `view`. |
| `view` | Hiển thị giao diện, sơ đồ mạch, thao tác nối dây và kết quả. |

### Quy tắc tổ chức code

- Mỗi class có một trách nhiệm chính; ưu tiên đóng gói, kế thừa và đa hình khi phù hợp.
- `model` không phụ thuộc vào `logic`, `controller` hoặc `view`.
- `logic` sử dụng `model` nhưng không phụ thuộc JavaFX hay các thành phần GUI.
- `controller` điều phối thao tác, không tự thực hiện công thức phân tích mạch.
- `view` phụ trách hiển thị và tương tác, không trực tiếp giải hệ phương trình điện.
- Hành vi gắn với linh kiện (ví dụ xác định trở kháng theo tần số) có thể được triển khai bằng đa hình trong `model`.
- Không tự ý thay đổi API dùng chung khi chưa trao đổi với nhóm.

## 4. Nhiệm vụ của từng class

### 4.1. Package `model`

| Class | Nhiệm vụ |
|---|---|
| `Complex` | Biểu diễn số phức; hỗ trợ cộng, trừ, nhân, chia, môđun và góc pha. Dùng cho trở kháng, điện áp và dòng điện AC. |
| `CircuitElement` | Abstract class cho các linh kiện thụ động hai cực; quản lý định danh, tên và định nghĩa phương thức xác định trở kháng theo tần số. |
| `Resistor` | Lưu điện trở `R`, cung cấp trở kháng `Z = R`. |
| `Capacitor` | Lưu điện dung `C`, cung cấp trở kháng AC `Z = 1/(jωC)`; DC xác lập là hở mạch lý tưởng. |
| `Inductor` | Lưu độ tự cảm `L`, cung cấp trở kháng AC `Z = jωL`; DC xác lập là ngắn mạch lý tưởng. |
| `VoltageSource` | Abstract class biểu diễn nguồn điện áp hai cực, với thông tin cực dương/âm và điện áp. |
| `ACSource` | Nguồn áp xoay chiều với điện áp và tần số. Cần thống nhất điện áp nhập là RMS hay biên độ. |
| `DCSource` | Nguồn áp một chiều có điện áp không đổi. |
| `Circuit` | Lưu trạng thái mạch do người dùng tạo: danh sách linh kiện, nguồn, dây nối và các thao tác chỉnh sửa. Không mặc định toàn mạch chỉ nối tiếp hoặc song song. |
| `Terminal` | Đại diện một chân kết nối cụ thể của linh kiện hoặc nguồn; giúp xác định người dùng nối dây vào đầu nào. |
| `Wire` | Biểu diễn kết nối dây dẫn lý tưởng giữa các terminal hoặc điểm nối; phục vụ lưu/chỉnh sửa sơ đồ gốc. |
| `Branch` | Biểu diễn một phần tử hai cực nối giữa hai **nút điện đã chuẩn hóa**; là cạnh của Graph tính toán. |
| `CircuitGraph` | Lưu các nút điện và nhánh linh kiện/nguồn sau khi chuẩn hóa; có thể có nhiều cạnh giữa cùng một cặp nút (**multigraph**). |

**Quan hệ kế thừa chính:**

```text
CircuitElement
├── Resistor
├── Capacitor
└── Inductor

VoltageSource
├── ACSource
└── DCSource
```

`Circuit` quản lý các thành phần và kết nối của mạch. Quan hệ nối tiếp, song song hoặc hỗn hợp được xác định thông qua cấu trúc Graph của mạch điện.

> **Thống nhất API:** `Branch` phải biểu diễn được cả linh kiện thụ động và nguồn điện áp. Có thể sử dụng interface chung cho phần tử hai cực hoặc thiết kế kiểu nhánh riêng cho nguồn. Không nên khai báo `Branch.element` chỉ có kiểu `CircuitElement` nếu `VoltageSource` không kế thừa class này.

### 4.2. Phân biệt `Terminal`, `Wire`, `Branch`

Ví dụ người dùng nối chân phải của `R1` với chân trái của `R2` và `L1`:

```text
Terminal: R1.right, R2.left, L1.left
Wire:     R1.right -- R2.left
          R1.right -- L1.left

Sau khi gộp dây:
Node B = {R1.right, R2.left, L1.left}
```

`TopologyBuilder` sẽ tạo các `Branch` bằng cách xác định nút điện của **hai đầu mỗi linh kiện**. Ví dụ:

```text
R1: Node A -- Node B
R2: Node B -- Node C
L1: Node B -- Node C
```

Vì `R2` và `L1` có cùng cặp nút B–C, chương trình nhận diện được chúng mắc song song.

**Lưu ý:** Hình dạng dây trên giao diện không quyết định quan hệ điện. Nếu GUI cho phép nối vào giữa dây hoặc tạo điểm rẽ, cần lưu thêm điểm nối (*junction*) hoặc các đoạn dây tương ứng. Hai đường dây giao nhau về hình học không mặc nhiên được nối điện; GUI phải có quy ước rõ ràng.

### 4.3. Package `logic`

| Class | Nhiệm vụ |
|---|---|
| `TopologyBuilder` | Chuyển `Circuit` (terminal + wire) thành `CircuitGraph`; gộp các terminal liên thông bằng dây lý tưởng thành nút điện, có thể dùng Disjoint Set Union (DSU). |
| `CircuitValidator` | Kiểm tra tham số, giới hạn linh kiện theo yêu cầu, terminal chưa nối, thành phần rời, mạch hở, ngắn mạch nguồn và các cấu hình không giải được. |
| `CircuitAnalyzer` | Điều phối phân tích: xác định chế độ DC/AC, tạo hệ phương trình, gọi solver và tính thông số từng linh kiện. |
| `NodalSolver` | Lập và giải hệ phương trình điện thế nút theo hướng **Modified Nodal Analysis (MNA)** để hỗ trợ nguồn áp lý tưởng và các nhánh trở kháng bằng 0. |
| `AnalysisResult` | Lưu kết quả toàn mạch: điện áp/dòng nguồn, trở kháng tương đương khi xác định được, kết quả linh kiện và thông báo liên quan. |
| `ElementResult` | Lưu kết quả từng linh kiện: định danh, điện áp hai đầu, dòng điện theo chiều tham chiếu, trở kháng và trạng thái nếu cần. |

**Quy trình phân tích dự kiến:**

1. Nhận `Circuit` chứa các linh kiện, nguồn và dây nối từ `controller`.
2. `TopologyBuilder` chuẩn hóa các terminal nối bằng dây thành nút điện và tạo `CircuitGraph`.
3. `CircuitValidator` kiểm tra tính hợp lệ của cấu trúc và thông số linh kiện.
4. Xác định nguồn DC/AC và tần số; thiết lập mô hình linh kiện tương ứng.
5. `CircuitAnalyzer` phối hợp với `NodalSolver` lập và giải hệ MNA.
6. Từ điện thế nút và các ẩn dòng nhánh cần thiết, tính điện áp/dòng trên từng linh kiện.
7. Tổng hợp `ElementResult` và `AnalysisResult`, trả về `controller`.

**Nguyên tắc phân tích:**

- **Nối tiếp/song song:** Có thể nhận diện từ Graph để giải thích hoặc tối ưu, nhưng không bắt buộc phải rút gọn trước khi giải.
- **Mạch hở:** Kiểm tra đường dẫn giữa hai cực nguồn qua mạng tải (không tính cạnh nguồn); cần xét cả trạng thái hở của tụ điện lý tưởng ở DC.
- **Ngắn mạch:** Phân biệt linh kiện bị nối tắt với việc hai cực nguồn bị nối bằng đường có tổng trở bằng 0. Với DC xác lập, cuộn cảm lý tưởng cũng có thể tạo nhánh ngắn mạch.
- **DC xác lập:** Tụ điện lý tưởng hở mạch, cuộn cảm lý tưởng có điện áp bằng 0. Không tính trực tiếp `1/Z` khi `Z = 0` hoặc `Z` vô hạn; dùng ràng buộc thích hợp trong MNA.
- **AC xác lập:** Sử dụng số phức và `ω = 2πf`, với `f > 0`. Phải thống nhất quy ước pha và RMS.
- **Trường hợp suy biến:** Nếu hệ phương trình vô nghiệm hoặc không có nghiệm duy nhất, trả thông báo phù hợp thay vì giá trị `NaN` hoặc kết quả tùy ý.

`CircuitAnalyzer` không trực tiếp vẽ mạch hoặc tương tác với người dùng.

### 4.4. Package `controller`

| Class | Nhiệm vụ |
|---|---|
| `CircuitController` | Tiếp nhận thao tác đặt/xóa linh kiện, nối/xóa dây, chỉnh tham số nguồn; cập nhật `Circuit`, yêu cầu phân tích và chuyển kết quả/thông báo sang giao diện. |

Các thao tác dự kiến: thêm/xóa/chỉnh sửa linh kiện; kết nối hoặc ngắt dây; thiết lập nguồn AC/DC; gửi yêu cầu phân tích; hiển thị kết quả hoặc cảnh báo.

### 4.5. Package `view`

| Class | Nhiệm vụ |
|---|---|
| `CircuitView` | Quản lý giao diện chính, công cụ chọn linh kiện, nhập tham số, tương tác nối dây, bảng kết quả và thông báo. |
| `CircuitRenderer` | Vẽ vị trí linh kiện, terminal, dây nối, nguồn và các nút/điểm nối trên canvas; làm nổi bật linh kiện được chọn hoặc trạng thái lỗi. |

`CircuitRenderer` sử dụng **mạch gốc** để vẽ đúng những gì người dùng đã tạo; không dùng `CircuitGraph` để tự ý thay đổi hình dạng sơ đồ. Tính năng tự động vẽ mạch tương đương, nếu có, sẽ được thiết kế riêng.

### 4.6. Class `Main`

`Main` là điểm khởi chạy ứng dụng, khởi tạo các thành phần cần thiết và bắt đầu giao diện.

### 4.7. Pipeline hoạt động tổng thể

```text
User
  │ Đặt linh kiện / nối dây / chỉnh thông số
  ▼
CircuitView + CircuitRenderer
  │
  ▼
CircuitController
  │
  ▼
Circuit (Components + Terminals + Wires)
  │
  ▼
TopologyBuilder
  │ Gộp các terminal nối dây (DSU)
  ▼
CircuitGraph (Nodes + Branches)
  │
  ▼
CircuitValidator
  │
  ▼
CircuitAnalyzer
  │
  └── NodalSolver (MNA)
  │
  ▼
AnalysisResult + ElementResult[]
  │
  ▼
CircuitController
  │
  └── CircuitView / CircuitRenderer
      Hiển thị bảng kết quả và cảnh báo
```

Khi người dùng **xóa dây**, có thể xây dựng lại topology từ danh sách dây hiện tại thay vì cố tách trực tiếp các tập DSU. Với quy mô bài tập lớn, cách này đơn giản và dễ kiểm thử.

## 5. Quy ước Git

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
feature/circuit-topology
feature/nodal-analysis
feature/gui-wiring
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
feat: build circuit graph from wires
fix: handle ideal inductor short circuit
refactor: separate topology builder from analyzer
test: add nodal solver tests
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

## 6. Quy tắc làm việc nhóm

- Mỗi thành viên chịu trách nhiệm chính với phần được phân công.
- Có thể sửa code ở package khác nếu cần, nhưng phải thông báo khi ảnh hưởng đến công việc của người khác.
- Không tự ý thay đổi chữ ký phương thức hoặc interface đã thống nhất.
- Không commit file build, file tạm hoặc cấu hình IDE cá nhân.
- Kiểm tra project build thành công trước khi tạo PR.
- Với các chức năng tính toán và xử lý topology, nên có unit test.
- Ưu tiên PR nhỏ, tập trung vào một chức năng để dễ review.
- Khi gặp vấn đề ảnh hưởng đến tiến độ hoặc kiến trúc chung, trao đổi trong nhóm trước khi tiếp tục.

## 7. Phân công công việc

Việc phân công cụ thể được quản lý thông qua GitHub Issues.

- **Model:** Xây dựng số phức, linh kiện, nguồn điện, terminal, wire và cấu trúc Graph.
- **Logic / Topology:** Chuẩn hóa nút điện, xây dựng Graph, kiểm tra liên thông, mạch hở và ngắn mạch.
- **Logic / Analysis:** Triển khai MNA, tính điện áp/dòng từng linh kiện và đóng gói kết quả.
- **Controller:** Điều phối thao tác giữa giao diện, dữ liệu và phần xử lý.
- **View:** Thiết kế giao diện, cho phép đặt linh kiện, kéo dây, vẽ sơ đồ và hiển thị kết quả.
- **Testing:** Viết unit test cho số phức, topology, các mạch chuẩn và các trường hợp đặc biệt.
- **Documentation:** Hoàn thiện UML, tài liệu thiết kế và hướng dẫn sử dụng.

**Ưu tiên triển khai:**

1. Thống nhất UML, cách định danh linh kiện/terminal và API chung cho phần tử hai cực.
2. Hoàn thiện `Complex`, các linh kiện và nguồn điện.
3. Hoàn thiện `Circuit`, `Terminal`, `Wire`, `Branch`, `CircuitGraph`.
4. Triển khai `TopologyBuilder` và unit test gộp nút, nối tiếp, song song, nhánh rời.
5. Triển khai `CircuitValidator`, `NodalSolver`, `CircuitAnalyzer` và kiểm thử DC/AC.
6. Kết nối `CircuitController`, giao diện kéo dây và bảng kết quả.
7. Kiểm thử tích hợp, sửa lỗi, hoàn thiện tài liệu và demo.

Các thành viên cần trao đổi trước khi thay đổi class hoặc API được nhiều phần khác sử dụng.
