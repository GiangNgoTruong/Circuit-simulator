package circuitsim.extension.simple2d;

import circuitsim.logic.CircuitAnalyzer;
import circuitsim.logic.CircuitSolution;
import circuitsim.logic.NodalSolver;
import circuitsim.model.*;
import java.util.*;

/** Removable adapter: schematic terminals -> wire nets -> existing graph/solver/analyzer. */
public final class CircuitBridge {
    private CircuitBridge() {}
    public enum Kind { R, C, L, DC, AC }
    /** R in ohms, C in farads, L in henries, sources in peak volts, phase in degrees. */
    public record Part(String id, Kind kind, double value, double frequency, double phase) {
        public Part {
            Objects.requireNonNull(id); Objects.requireNonNull(kind);
            if(id.isBlank() || !Double.isFinite(value) || !Double.isFinite(frequency) || !Double.isFinite(phase))
                throw new IllegalArgumentException("Tên và thông số phải hợp lệ, hữu hạn.");
            if((kind==Kind.R || kind==Kind.C || kind==Kind.L) && value<=0)
                throw new IllegalArgumentException("R, C, L phải lớn hơn 0.");
            if(kind==Kind.AC && frequency<=0)throw new IllegalArgumentException("Nguồn AC cần f > 0 Hz.");
        }
    }
    public record Pin(String part, int end) {
        public Pin {Objects.requireNonNull(part);if(end!=0 && end!=1)throw new IllegalArgumentException("Invalid terminal");}
    }
    public record Wire(Pin a, Pin b) {
        public Wire {Objects.requireNonNull(a);Objects.requireNonNull(b);}
    }
    public record Compiled(CircuitGraph graph, Map<String,Edge> edges, Map<Pin,Integer> nodes) {}
    public record Analysis(Compiled compiled, CircuitAnalyzer analyzer) {}

    public static Compiled compile(List<Part> parts, List<Wire> wires, Pin ground) {
        if(parts.isEmpty())throw new IllegalArgumentException("Thêm linh kiện trước khi tính.");
        Map<String,Part> ids=new LinkedHashMap<>();
        Map<Pin,Pin> parents=new LinkedHashMap<>();
        for(Part part:parts) {
            if(ids.putIfAbsent(part.id(),part)!=null)throw new IllegalArgumentException("Trùng ID: "+part.id());
            for(int i=0;i<2;i++){Pin p=new Pin(part.id(),i);parents.put(p,p);}
        }
        if(ground==null || !parents.containsKey(ground))throw new IllegalArgumentException("Chọn một đầu cực làm GND.");
        for(Wire wire:wires) {
            if(!parents.containsKey(wire.a()) || !parents.containsKey(wire.b()))throw new IllegalArgumentException("Dây tham chiếu linh kiện đã bị xóa.");
            parents.put(root(parents,wire.a()),root(parents,wire.b()));
        }
        CircuitGraph graph=new CircuitGraph();
        Map<Pin,Integer> roots=new HashMap<>(),nodes=new LinkedHashMap<>();
        for(Pin pin:parents.keySet())nodes.put(pin,roots.computeIfAbsent(root(parents,pin),p -> graph.addNode()));
        graph.setGround(nodes.get(ground));
        Map<String,Edge> edges=new LinkedHashMap<>();
        for(Part part:parts) {
            Component component=switch(part.kind()) {
                case R -> new Resistor(part.id(),part.value());
                case C -> new Capacitor(part.id(),part.value());
                case L -> new Inductor(part.id(),part.value());
                case DC,AC -> new VoltageSource(part.id()) {
                    @Override public double getVoltage(){return part.value();}
                    @Override public double getFrequency(){return part.kind()==Kind.DC?0:part.frequency();}
                    @Override public double getPhase(){return part.kind()==Kind.DC?0:Math.toRadians(part.phase());}
                };
            };
            graph.addEdge(nodes.get(new Pin(part.id(),0)),nodes.get(new Pin(part.id(),1)),component);
            edges.put(part.id(),graph.getEdges().getLast());
        }
        return new Compiled(graph,Collections.unmodifiableMap(edges),Collections.unmodifiableMap(nodes));
    }
    private static Pin root(Map<Pin,Pin> parents,Pin pin) {
        Pin parent=parents.get(pin);
        if(!parent.equals(pin))parents.put(pin,root(parents,parent));
        return parents.get(pin);
    }

    public static Analysis analyze(List<Part> parts,List<Wire> wires,Pin ground) {
        Compiled compiled=compile(parts,wires,ground);
        CircuitChecks.validate(compiled);
        CircuitSolution solution=new NodalSolver().solve(compiled.graph());
        if(solution.getResults().isEmpty())throw new IllegalArgumentException("Cần ít nhất một nguồn DC hoặc AC.");
        for(var result:solution.getResults().values())for(Complex value:result.getSolution())
            if(!value.isFinite())throw new ArithmeticException("Kết quả không hữu hạn.");
        return new Analysis(compiled,new CircuitAnalyzer(solution));
    }
}
