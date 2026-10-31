package circuitsim.extension.simple2d;

import circuitsim.logic.CircuitAnalyzer;
import circuitsim.model.*;
import circuitsim.extension.simple2d.CircuitBridge.*;
import java.util.*;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** Optional schematic editor. All state and UI live in this removable package. */
public final class SimpleCircuitApp extends Application {
    private enum Tool { SELECT, WIRE, GROUND, DELETE }
    private static final class Placed {
        Part part; double x,y; int angle;
        Placed(Part part,double x,double y){this.part=part;this.x=x;this.y=y;}
        Point2D pin(int end){double a=Math.toRadians(angle),sign=end==0?-1:1;return new Point2D(x+sign*55*Math.cos(a),y+sign*55*Math.sin(a));}
    }
    private final List<Placed> parts=new ArrayList<>();
    private final List<Wire> wires=new ArrayList<>();
    private final Canvas canvas=new Canvas();
    private final Label status=new Label("Thêm linh kiện, nối các đầu cực rồi nhấn Tính.");
    private final Label errorBanner=new Label();
    private final TabPane resultTabs=new TabPane();
    private final Label selectedLabel=new Label("Chưa chọn linh kiện");
    private final TextField value=new TextField(),frequency=new TextField(),phase=new TextField();
    private final TextArea graphText=new TextArea();
    private final TableView<List<String>> normal=new TableView<>(),rms=new TableView<>();
    private final ToggleGroup tools=new ToggleGroup();
    private Pin ground,pending;
    private Placed selected;
    private Wire selectedWire;
    private Tool tool=Tool.SELECT;
    private int sequence;
    private double grabX,grabY;
    private Point2D pointer;
    private Analysis analysis;

    @Override public void start(Stage stage) {
        BorderPane root=createView();
        Scene scene=new Scene(root,1340,850);
        scene.setOnKeyPressed(e -> {if(e.getCode()==KeyCode.ESCAPE){pending=null;tools.getToggles().getFirst().setSelected(true);tool=Tool.SELECT;redraw();}});
        stage.setTitle("Simple2D • Circuit Graph / Solver");stage.setMinWidth(1040);stage.setMinHeight(720);stage.setScene(scene);stage.show();
    }
    BorderPane createView() {
        BorderPane root=new BorderPane();root.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:12;-fx-background-color:#f2f5f9;");
        HBox toolbar=new HBox(8);toolbar.setAlignment(Pos.CENTER_LEFT);toolbar.setPadding(new Insets(12));
        toolbar.getChildren().add(new Label("SIMPLE 2D"));
        String[] labels={"Chọn / kéo","Nối dây","GND","Xóa"};
        for(Tool t:Tool.values()) {
            ToggleButton b=new ToggleButton(labels[t.ordinal()]);b.setToggleGroup(tools);b.setSelected(t==Tool.SELECT);
            b.setOnAction(e -> {b.setSelected(true);tool=t;pending=null;redraw();});toolbar.getChildren().add(b);
        }
        toolbar.getChildren().addAll(button("Tính mạch",this::calculate),button("Mẫu DC",() -> example(false)),button("Mẫu AC",() -> example(true)),button("Dọn bàn",() -> {parts.clear();wires.clear();ground=null;pending=null;select(null);invalidate();redraw();}));
        errorBanner.setWrapText(true);errorBanner.setMaxWidth(Double.MAX_VALUE);
        errorBanner.setStyle("-fx-background-color:#ffede7;-fx-text-fill:#9b3023;-fx-padding:12;-fx-font-weight:bold;");
        errorBanner.setVisible(false);errorBanner.setManaged(false);
        root.setTop(new VBox(toolbar,errorBanner));
        Pane board=new Pane(canvas);board.setMinSize(300,220);
        canvas.widthProperty().bind(board.widthProperty());canvas.heightProperty().bind(board.heightProperty());
        canvas.widthProperty().addListener(o -> redraw());canvas.heightProperty().addListener(o -> redraw());
        canvas.setOnMousePressed(e -> press(e.getX(),e.getY()));
        canvas.setOnMouseDragged(e -> {
            pointer=new Point2D(e.getX(),e.getY());
            if(tool==Tool.SELECT && selected!=null){selected.x=Math.max(65,Math.min(canvas.getWidth()-65,e.getX()-grabX));selected.y=Math.max(55,Math.min(canvas.getHeight()-55,e.getY()-grabY));}
            redraw();
        });
        canvas.setOnMouseMoved(e -> {pointer=new Point2D(e.getX(),e.getY());if(pending!=null)redraw();});
        VBox editor=new VBox(7,new Label("Đơn vị SI: Ω, F, H, V đỉnh • Pha nhập bằng độ • Dây giao nhau không tự nối"),board);
        editor.setPadding(new Insets(8,12,8,12));VBox.setVgrow(board,Priority.ALWAYS);
        configureTable(normal,"Linh kiện","f (Hz)","U (a + jb) V","|U| V","∠U °","I (a + jb) A","|I| A","∠I °");
        configureTable(rms,"Linh kiện","U RMS (V)","I RMS (A)","P trung bình (W)");
        graphText.setEditable(false);graphText.setStyle("-fx-font-family:Consolas;-fx-font-size:12;");
        resultTabs.getTabs().setAll(tab("Phasor / norm",normal),tab("RMS / công suất",rms),tab("Graph / node",graphText));
        resultTabs.setMinHeight(160);
        SplitPane split=new SplitPane(editor,resultTabs);split.setOrientation(Orientation.VERTICAL);split.setDividerPositions(.60);root.setCenter(split);
        root.setRight(properties());
        status.setPadding(new Insets(10,14,10,14));status.setMaxWidth(Double.MAX_VALUE);status.setWrapText(true);root.setBottom(status);
        example(true);return root;
    }
    private VBox properties() {
        VBox box=new VBox(10,new Label("THÊM LINH KIỆN"));box.setPrefWidth(215);box.setPadding(new Insets(12));
        for(Kind kind:Kind.values())box.getChildren().add(button(switch(kind){case R -> "R — Điện trở (Ω)";case C -> "C — Tụ điện (F)";case L -> "L — Cuộn cảm (H)";case DC -> "V — Nguồn DC (V)";case AC -> "~ — Nguồn AC (V đỉnh)";},() -> add(kind)));
        value.setPromptText("Ví dụ 1000 hoặc 1e-6");frequency.setPromptText("Hz, AC > 0");phase.setPromptText("Độ");
        box.getChildren().addAll(new Separator(),selectedLabel,new Label("Giá trị (SI / V đỉnh)"),value,new Label("Tần số AC (Hz)"),frequency,new Label("Pha AC (độ)"),phase,
            button("Áp dụng",this::apply),button("Xoay 90°",() -> {if(selected!=null){selected.angle=(selected.angle+90)%360;redraw();}}),button("Xóa đã chọn",this::deleteSelected));
        Label help=new Label("1. Thêm và kéo linh kiện.\n2. Nối dây: nhấn 2 vòng tròn.\n3. GND: nhấn 1 đầu cực.\n4. Tính mạch.\n\nMột cực được nối nhiều dây.\nChọn dây rồi Xóa để tháo.\nEsc hủy dây đang nối.\nNguồn: cực 1 (+) → cực 2 (−).");help.setWrapText(true);box.getChildren().addAll(new Separator(),help);
        return box;
    }
    private static Button button(String label,Runnable action){Button b=new Button(label);b.setOnAction(e -> action.run());return b;}
    private static Tab tab(String name,javafx.scene.Node content){Tab t=new Tab(name,content);t.setClosable(false);return t;}
    private static void configureTable(TableView<List<String>> table,String... headings) {
        for(int i=0;i<headings.length;i++){final int index=i;TableColumn<List<String>,String> col=new TableColumn<>(headings[i]);col.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().get(index)));col.setPrefWidth(i==0?90:135);table.getColumns().add(col);}
        table.setPlaceholder(new Label("Nhấn Tính mạch để xem kết quả."));
    }
    private Placed append(Kind kind,double amount,double hz,double degrees,double x,double y) {
        String id=(kind==Kind.DC||kind==Kind.AC?"V":kind.name())+(++sequence);
        Placed p=new Placed(new Part(id,kind,amount,hz,degrees),x,y);parts.add(p);
        if(ground==null && (kind==Kind.DC||kind==Kind.AC))ground=new Pin(id,1);
        return p;
    }
    private void add(Kind kind) {
        double initial=switch(kind){case R -> 1000;case C -> 1e-6;case L -> .01;case DC,AC -> 10;};
        int n=parts.size();Placed p=append(kind,initial,50,0,120+(n%4)*165,90+(n/4%3)*100);select(p);invalidate();redraw();
    }
    private void select(Placed p) {
        selected=p;selectedWire=null;selectedLabel.setText(p==null?"Chưa chọn linh kiện":p.part.id()+" · "+p.part.kind());
        value.setDisable(p==null);frequency.setDisable(p==null||p.part.kind()!=Kind.AC);phase.setDisable(frequency.isDisable());
        value.setText(p==null?"":Double.toString(p.part.value()));frequency.setText(p==null?"":Double.toString(p.part.frequency()));phase.setText(p==null?"":Double.toString(p.part.phase()));
    }
    private void apply() {
        if(selected==null)return;
        try {
            Part old=selected.part;selected.part=new Part(old.id(),old.kind(),Double.parseDouble(value.getText().trim()),
                old.kind()==Kind.AC?Double.parseDouble(frequency.getText().trim()):0,old.kind()==Kind.AC?Double.parseDouble(phase.getText().trim()):0);
            invalidate();redraw();status.setText("Đã cập nhật "+old.id()+". Nhấn Tính mạch.");
        }catch(IllegalArgumentException ex){status.setText("Thông số chưa hợp lệ: "+ex.getMessage());}
    }
    private Placed find(String id){return parts.stream().filter(p -> p.part.id().equals(id)).findFirst().orElseThrow();}
    private Point2D point(Pin pin){return find(pin.part()).pin(pin.end());}
    private Pin hitPin(Point2D point) {
        for(int i=parts.size()-1;i>=0;i--)for(int end=0;end<2;end++)if(parts.get(i).pin(end).distance(point)<12)return new Pin(parts.get(i).part.id(),end);
        return null;
    }
    private Placed hitPart(Point2D point) {for(int i=parts.size()-1;i>=0;i--){Placed p=parts.get(i);if(Math.abs(p.x-point.getX())<55&&Math.abs(p.y-point.getY())<45)return p;}return null;}
    private void press(double x,double y) {
        Point2D position=new Point2D(x,y);Pin pin=hitPin(position);
        if(tool==Tool.GROUND) {if(pin!=null){ground=pin;pending=null;invalidate();redraw();status.setText("GND = "+pin.part()+"."+(pin.end()+1));}return;}
        if(tool==Tool.WIRE) {
            if(pin==null){status.setText("Nhấn đúng vòng tròn ở đầu cực linh kiện.");return;}
            if(pending==null){pending=pin;status.setText("Chọn đầu cực thứ hai; Esc để hủy.");}
            else {
                Wire wire=new Wire(pending,pin);
                boolean duplicate=wires.stream().anyMatch(w -> (w.a().equals(wire.a())&&w.b().equals(wire.b()))||(w.a().equals(wire.b())&&w.b().equals(wire.a())));
                if(!pending.equals(pin)&&!duplicate){wires.add(wire);invalidate();status.setText("Đã nối dây. Một cực có thể nối nhiều nhánh.");}
                else status.setText("Đã có dây hoặc bạn chọn lại cùng đầu cực.");
                pending=null;
            }
            redraw();return;
        }
        Placed p=hitPart(position);select(p);
        if(p!=null){grabX=x-p.x;grabY=y-p.y;}
        else selectedWire=hitWire(position);
        if(tool==Tool.DELETE)deleteSelected();redraw();
    }
    private void deleteSelected() {
        if(selected!=null){String id=selected.part.id();parts.remove(selected);wires.removeIf(w -> w.a().part().equals(id)||w.b().part().equals(id));if(ground!=null&&ground.part().equals(id))ground=null;select(null);}
        else if(selectedWire!=null){wires.remove(selectedWire);selectedWire=null;}
        pending=null;invalidate();redraw();
    }
    private void invalidate(){analysis=null;normal.getItems().clear();rms.getItems().clear();graphText.clear();errorBanner.setVisible(false);errorBanner.setManaged(false);status.setText("Mạch đã thay đổi — nhấn Tính mạch để cập nhật kết quả.");}
    private void calculationFailed(String message) {
        invalidate();status.setText(message);errorBanner.setText("KHÔNG TÍNH ĐƯỢC MẠCH\n"+message);
        errorBanner.setVisible(true);errorBanner.setManaged(true);
    }
    private static double segmentDistance(Point2D p,Point2D a,Point2D b) {
        Point2D d=b.subtract(a);double len=d.dotProduct(d);double t=len==0?0:Math.max(0,Math.min(1,p.subtract(a).dotProduct(d)/len));return p.distance(a.add(d.multiply(t)));
    }
    private Wire hitWire(Point2D p) {
        for(Wire w:wires){Point2D a=point(w.a()),b=point(w.b());double mid=(a.getX()+b.getX())/2;Point2D c=new Point2D(mid,a.getY()),d=new Point2D(mid,b.getY());if(Math.min(segmentDistance(p,a,c),Math.min(segmentDistance(p,c,d),segmentDistance(p,d,b)))<7)return w;}return null;
    }
    private void redraw() {
        GraphicsContext g=canvas.getGraphicsContext2D();double w=canvas.getWidth(),h=canvas.getHeight();g.setFill(Color.WHITE);g.fillRect(0,0,w,h);
        g.setFill(Color.web("#dce4ed"));for(int x=15;x<w;x+=20)for(int y=15;y<h;y+=20)g.fillOval(x,y,1.5,1.5);
        for(Wire wire:wires)drawWire(g,point(wire.a()),point(wire.b()),wire.equals(selectedWire)?"#e18b28":"#56839d");
        if(pending!=null && pointer!=null)drawWire(g,point(pending),pointer,"#d59b2d");
        for(Placed p:parts)drawPart(g,p);
        if(ground!=null){Point2D p=point(ground);g.setStroke(Color.web("#299367"));g.setLineWidth(2);g.strokeLine(p.getX(),p.getY(),p.getX(),p.getY()+18);for(int i=0;i<3;i++)g.strokeLine(p.getX()-12+i*4,p.getY()+18+i*5,p.getX()+12-i*4,p.getY()+18+i*5);}
    }
    private static void drawWire(GraphicsContext g,Point2D a,Point2D b,String color){g.setStroke(Color.web(color));g.setLineWidth(2.5);double mid=(a.getX()+b.getX())/2;g.strokePolyline(new double[]{a.getX(),mid,mid,b.getX()},new double[]{a.getY(),a.getY(),b.getY(),b.getY()},4);}
    private void drawPart(GraphicsContext g,Placed p) {
        g.save();g.translate(p.x,p.y);g.rotate(p.angle);g.setFill(Color.WHITE);g.fillRect(-37,-22,74,44);g.setStroke(Color.web(p==selected?"#e19b24":"#283f55"));g.setLineWidth(p==selected?3:2);
        g.strokeLine(-55,0,-28,0);g.strokeLine(28,0,55,0);
        switch(p.part.kind()) {
            case R -> g.strokeRect(-28,-12,56,24);
            case C -> {g.strokeLine(-28,0,-7,0);g.strokeLine(-7,-20,-7,20);g.strokeLine(7,-20,7,20);g.strokeLine(7,0,28,0);}
            case L -> {for(int i=0;i<4;i++)g.strokeArc(-28+i*14,-14,14,28,0,180,javafx.scene.shape.ArcType.OPEN);}
            case DC,AC -> {g.strokeOval(-25,-25,50,50);g.setFill(Color.web("#283f55"));g.setFont(javafx.scene.text.Font.font(16));g.fillText(p.part.kind()==Kind.DC?"+   −":"~",p.part.kind()==Kind.DC?-21:-6,6);}
        }
        g.restore();g.setFont(javafx.scene.text.Font.font(12));g.setFill(Color.web("#304b60"));
        String suffix=switch(p.part.kind()){case R -> " Ω";case C -> " F";case L -> " H";case DC -> " V DC";case AC -> " Vpk / "+number(p.part.frequency())+" Hz";};
        g.fillText(p.part.id()+"  "+number(p.part.value())+suffix,p.x-55,p.y-35);
        for(int end=0;end<2;end++) {Pin pin=new Pin(p.part.id(),end);Point2D pos=p.pin(end);g.setFill(pin.equals(pending)?Color.ORANGE:Color.WHITE);g.setStroke(Color.web("#426d8a"));g.setLineWidth(2);g.fillOval(pos.getX()-5,pos.getY()-5,10,10);g.strokeOval(pos.getX()-5,pos.getY()-5,10,10);}
    }
    private void example(boolean ac) {
        parts.clear();wires.clear();ground=null;pending=null;sequence=0;
        Placed v=append(ac?Kind.AC:Kind.DC,10,ac?1000/(2*Math.PI):0,0,150,160);v.angle=90;
        Placed r=append(Kind.R,ac?1000:100,0,0,360,105);
        Placed load=append(ac?Kind.C:Kind.R,ac?1e-6:200,0,0,555,160);load.angle=90;
        wires.add(new Wire(new Pin(v.part.id(),0),new Pin(r.part.id(),0)));
        wires.add(new Wire(new Pin(r.part.id(),1),new Pin(load.part.id(),0)));
        wires.add(new Wire(new Pin(load.part.id(),1),new Pin(v.part.id(),1)));
        select(r);invalidate();redraw();status.setText(ac?"Mẫu RC: nguồn đỉnh 10 V, ωRC=1; |U_C|≈7.071 V, U_C RMS=5 V.":"Mẫu chia áp DC: R1=100 Ω, R2=200 Ω; U_R2≈6.667 V.");
    }
    private void calculate() {
        errorBanner.setVisible(false);errorBanner.setManaged(false);
        try {
            analysis=CircuitBridge.analyze(parts.stream().map(p -> p.part).toList(),wires,ground);
            normal.getItems().clear();rms.getItems().clear();CircuitAnalyzer a=analysis.analyzer();
            List<Double> frequencies=a.getFrequencies().stream().sorted().toList();
            for(var entry:analysis.compiled().edges().entrySet()) {
                Edge edge=entry.getValue();
                for(double hz:frequencies){Complex u=a.getVoltage(edge,hz),i=a.getCurrent(edge,hz);normal.getItems().add(List.of(entry.getKey(),number(hz),complex(u),number(u.magnitude()),number(Math.toDegrees(u.phase())),complex(i),number(i.magnitude()),number(Math.toDegrees(i.phase()))));}
                rms.getItems().add(List.of(entry.getKey(),number(a.getVoltageRMS(edge)),number(a.getCurrentRMS(edge)),number(a.getAveragePower(edge))));
            }
            StringBuilder text=new StringBuilder("Chiều tham chiếu U = V(cực 1) − V(cực 2); I: cực 1 → cực 2.\nAC dùng phasor biên độ đỉnh; norm = |phasor|. RMS gộp DC và các tần số.\n\n");
            Compiled c=analysis.compiled();text.append("GND = N").append(c.graph().getGround()).append("\n");
            Map<Integer,List<String>> nets=new TreeMap<>();c.nodes().forEach((pin,node) -> nets.computeIfAbsent(node,n -> new ArrayList<>()).add(pin.part()+"."+(pin.end()+1)));
            nets.forEach((node,pins) -> text.append("N").append(node).append(" = ").append(String.join(", ",pins)).append("\n"));
            text.append("\nCạnh graph:\n");c.edges().forEach((id,e) -> text.append(id).append(": N").append(e.getFrom()).append(" → N").append(e.getTo()).append("\n"));
            for(double hz:frequencies){text.append("\nf = ").append(number(hz)).append(" Hz\n");for(int node:nets.keySet())text.append("V(N").append(node).append(") = ").append(complex(a.getNodeVoltage(node,hz))).append(" V\n");}
            graphText.setText(text.toString());resultTabs.getSelectionModel().select(1);
            status.setText("Đã tính: "+c.graph().getNodes().size()+" node, "+c.edges().size()+" nhánh, "+frequencies.size()+" tần số. P âm: nguồn cấp công suất.");redraw();
        }catch(ArithmeticException ex){calculationFailed("Hệ phương trình không có nghiệm duy nhất: kiểm tra nguồn áp mắc song song, vòng nguồn lý tưởng hoặc các ràng buộc mâu thuẫn. "+ex.getMessage());}
        catch(IllegalArgumentException|IllegalStateException ex){calculationFailed(Objects.toString(ex.getMessage(),"Thông số hoặc kết nối không hợp lệ."));}
        catch(RuntimeException ex){calculationFailed(ex.getClass().getSimpleName()+": "+Objects.toString(ex.getMessage(),"Lỗi không có mô tả."));ex.printStackTrace();}
    }
    private static String number(double n){return String.format(Locale.ROOT,"%.6g",Math.abs(n)<1e-12?0:n);}
    private static String complex(Complex n){return number(n.real())+(n.imag()<0?" − ":" + ")+number(Math.abs(n.imag()))+"j";}
    public static void main(String[] args){launch(args);}
}
