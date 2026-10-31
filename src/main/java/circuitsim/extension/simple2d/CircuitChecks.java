package circuitsim.extension.simple2d;

import circuitsim.model.*;
import java.util.*;

/** Editor-only explanations before the core solver reports a singular matrix. */
final class CircuitChecks {
    private CircuitChecks() {}
    static void validate(CircuitBridge.Compiled compiled) {
        CircuitGraph graph=compiled.graph();
        Set<Double> frequencies=new TreeSet<>();
        for(Edge e:graph.getEdges())if(e.getComponent() instanceof VoltageSource v)frequencies.add(v.getFrequency());
        for(double frequency:frequencies) {
            Map<Integer,Set<Integer>> conductive=new HashMap<>(),shorts=new HashMap<>();
            for(int node:graph.getNodes()){conductive.put(node,new HashSet<>());shorts.put(node,new HashSet<>());}
            for(Edge edge:graph.getEdges()) {
                Component c=edge.getComponent();
                if(!(frequency==0 && c instanceof Capacitor))join(conductive,edge);
                boolean zero=(frequency==0 && c instanceof Inductor) ||
                        (c instanceof VoltageSource v && (v.getFrequency()!=frequency || v.getVoltage()==0));
                if(zero)join(shorts,edge);
            }
            for(Edge edge:graph.getEdges())if(edge.getComponent() instanceof VoltageSource v &&
                    v.getFrequency()==frequency && v.getVoltage()!=0 && reachable(shorts,edge.getFrom()).contains(edge.getTo())) {
                throw new IllegalArgumentException("Nguồn "+v.getId()+" bị ngắn mạch ở "+frequency+" Hz. "+
                        (frequency==0?"Cuộn cảm lý tưởng ở DC là dây nối tắt. Hãy mắc tải/điện trở nối tiếp; không nối cuộn cảm trực tiếp qua nguồn áp khác 0.":"Kiểm tra dây đang nối tắt hai cực nguồn."));
            }
            Set<Integer> referenced=reachable(conductive,graph.getGround());
            if(referenced.size()!=graph.getNodes().size()) {
                Set<String> floating=new TreeSet<>();
                compiled.nodes().forEach((pin,node) -> {if(!referenced.contains(node))floating.add(pin.part()+"."+(pin.end()+1));});
                throw new IllegalArgumentException("Node thả nổi ở "+frequency+" Hz: "+String.join(", ",floating)+". "+
                        "Nối phần mạch rời về mạng có GND. "+(frequency==0?"Tụ điện hở mạch ở DC nên không tạo đường tham chiếu điện thế.":""));
            }
        }
    }
    private static void join(Map<Integer,Set<Integer>> graph,Edge edge) {
        graph.get(edge.getFrom()).add(edge.getTo());graph.get(edge.getTo()).add(edge.getFrom());
    }
    private static Set<Integer> reachable(Map<Integer,Set<Integer>> graph,int start) {
        Set<Integer> seen=new HashSet<>();ArrayDeque<Integer> queue=new ArrayDeque<>();seen.add(start);queue.add(start);
        while(!queue.isEmpty())for(int next:graph.get(queue.removeFirst()))if(seen.add(next))queue.add(next);
        return seen;
    }
}
