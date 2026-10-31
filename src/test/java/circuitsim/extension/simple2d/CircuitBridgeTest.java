package circuitsim.extension.simple2d;

import circuitsim.extension.simple2d.CircuitBridge.*;
import circuitsim.model.Complex;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CircuitBridgeTest {
    private static Part part(String id,Kind kind,double value){return new Part(id,kind,value,0,0);}
    private static Pin pin(String id,int n){return new Pin(id,n);}
    private static Wire wire(String a,int n,String b,int m){return new Wire(pin(a,n),pin(b,m));}
    private static void complex(double real,double imag,Complex value){assertEquals(real,value.real(),1e-9);assertEquals(imag,value.imag(),1e-9);}

    @Test void seriesDividerRunsThroughExistingSolverAndAnalyzer() {
        var parts=List.of(part("V",Kind.DC,10),part("R1",Kind.R,100),part("R2",Kind.R,200));
        var wires=List.of(wire("V",0,"R1",0),wire("R1",1,"R2",0),wire("R2",1,"V",1));
        var analysis=CircuitBridge.analyze(parts,wires,pin("V",1));
        var r2=analysis.compiled().edges().get("R2");
        assertEquals(3,analysis.compiled().graph().getNodes().size());
        complex(20.0/3,0,analysis.analyzer().getVoltage(r2,0));
        complex(1.0/30,0,analysis.analyzer().getCurrent(r2,0));
        assertEquals(20.0/3,analysis.analyzer().getVoltageRMS(r2),1e-9);
        assertEquals(-1.0/3,analysis.analyzer().getAveragePower(analysis.compiled().edges().get("V")),1e-9);
    }
    @Test void branchingWiresMergeTransitivelyAndKeepParallelEdges() {
        var parts=List.of(part("V",Kind.DC,12),part("R1",Kind.R,6),part("R2",Kind.R,3));
        var wires=List.of(wire("V",0,"R1",0),wire("R1",0,"R2",0),wire("R1",1,"R2",1),wire("R2",1,"V",1),wire("R1",0,"V",0));
        var a=CircuitBridge.analyze(parts,wires,pin("R1",1));
        assertEquals(2,a.compiled().graph().getNodes().size());
        assertEquals(3,a.compiled().edges().size());
        assertEquals(a.compiled().nodes().get(pin("V",0)),a.compiled().nodes().get(pin("R2",0)));
        complex(-6,0,a.analyzer().getCurrent(a.compiled().edges().get("V"),0));
    }
    @Test void acRcCornerProducesPhasorNormAndRms() {
        double f=1000/(2*Math.PI);
        var parts=List.of(new Part("V",Kind.AC,10,f,0),part("R",Kind.R,1000),part("C",Kind.C,1e-6));
        var a=CircuitBridge.analyze(parts,List.of(wire("V",0,"R",0),wire("R",1,"C",0),wire("C",1,"V",1)),pin("V",1));
        var c=a.compiled().edges().get("C");
        complex(5,-5,a.analyzer().getVoltage(c,f));
        assertEquals(Math.sqrt(50),a.analyzer().getVoltage(c,f).magnitude(),1e-9);
        assertEquals(5,a.analyzer().getVoltageRMS(c),1e-9);
        assertEquals(.005,a.analyzer().getCurrentRMS(c),1e-9);
        assertEquals(0,a.analyzer().getAveragePower(c),1e-9);
    }
    @Test void convertsSourcePhaseFromDegreesToRadians() {
        var parts=List.of(new Part("V",Kind.AC,10,50,90),part("R",Kind.R,5));
        var a=CircuitBridge.analyze(parts,List.of(wire("V",0,"R",0),wire("V",1,"R",1)),pin("V",1));
        complex(0,10,a.analyzer().getVoltage(a.compiled().edges().get("R"),50));
        complex(0,-2,a.analyzer().getCurrent(a.compiled().edges().get("V"),50));
    }
    @Test void adaptsDifferentMnaCurrentLayoutsForDcAndAcInductor() {
        var parts=List.of(part("DC",Kind.DC,10),new Part("AC",Kind.AC,20,50,0),part("R",Kind.R,10),part("L",Kind.L,.01));
        var wires=List.of(wire("DC",0,"AC",1),wire("AC",0,"R",0),wire("R",1,"L",0),wire("L",1,"DC",1));
        var a=CircuitBridge.analyze(parts,wires,pin("DC",1));
        assertEquals(Set.of(0.0,50.0),a.analyzer().getFrequencies());
        var l=a.compiled().edges().get("L");
        complex(1,0,a.analyzer().getCurrent(l,0));
        complex(0,0,a.analyzer().getVoltage(l,0));
        double z=2*Math.PI*50*.01,denominator=100+z*z;
        complex(200/denominator,-20*z/denominator,a.analyzer().getCurrent(l,50));
    }
    @Test void mixedFrequenciesProduceCombinedRmsRatherThanSumOfAmplitudes() {
        var parts=List.of(part("DC",Kind.DC,10),new Part("AC",Kind.AC,20,50,0),part("R",Kind.R,10));
        var a=CircuitBridge.analyze(parts,List.of(wire("DC",0,"AC",1),wire("AC",0,"R",0),wire("R",1,"DC",1)),pin("DC",1));
        var r=a.compiled().edges().get("R");
        assertEquals(Math.sqrt(300),a.analyzer().getVoltageRMS(r),1e-9);
        assertEquals(Math.sqrt(3),a.analyzer().getCurrentRMS(r),1e-9);
        assertEquals(30,a.analyzer().getAveragePower(r),1e-9);
    }
    @Test void removingWireRebuildsNetsRatherThanRetainingOldUnion() {
        var parts=List.of(part("V",Kind.DC,10),part("R",Kind.R,5));
        var connected=List.of(wire("V",0,"R",0),wire("R",1,"V",1));
        var before=CircuitBridge.compile(parts,connected,pin("V",1));
        var after=CircuitBridge.compile(parts,connected.subList(0,1),pin("V",1));
        assertEquals(2,before.graph().getNodes().size());assertEquals(3,after.graph().getNodes().size());
        assertNotEquals(after.nodes().get(pin("R",1)),after.nodes().get(pin("V",1)));
    }
    @Test void shortedVoltageSourceFailsRatherThanDisplayingInventedResults() {
        var error=assertThrows(IllegalArgumentException.class,() -> CircuitBridge.analyze(List.of(part("V",Kind.DC,5)),List.of(wire("V",0,"V",1)),pin("V",1)));
        assertTrue(error.getMessage().contains("ngắn mạch"));
    }
    @Test void disconnectedIslandFailsRatherThanGettingAnArbitraryReferenceVoltage() {
        var parts=List.of(part("V",Kind.DC,5),part("R",Kind.R,10));
        var error=assertThrows(IllegalArgumentException.class,() -> CircuitBridge.analyze(parts,List.of(),pin("V",1)));
        assertTrue(error.getMessage().contains("Node thả nổi"));
        assertTrue(error.getMessage().contains("R.1"));
    }
    @Test void openResistorBranchIsValidAndHasZeroCurrent() {
        var a=CircuitBridge.analyze(List.of(part("V",Kind.DC,10),part("R",Kind.R,5)),List.of(wire("V",0,"R",0)),pin("V",1));
        complex(0,0,a.analyzer().getCurrent(a.compiled().edges().get("R"),0));
    }
    @Test void inductorDirectlyAcrossDcSourceExplainsShortCircuit() {
        var parts=List.of(part("V",Kind.DC,10),part("L",Kind.L,.01));
        var error=assertThrows(IllegalArgumentException.class,() -> CircuitBridge.analyze(parts,List.of(wire("V",0,"L",0),wire("L",1,"V",1)),pin("V",1)));
        assertTrue(error.getMessage().contains("Cuộn cảm lý tưởng ở DC"));
    }
    @Test void dcInductorWithSeriesResistanceRemainsValid() {
        var parts=List.of(part("V",Kind.DC,10),part("R",Kind.R,5),part("L",Kind.L,.01));
        var a=CircuitBridge.analyze(parts,List.of(wire("V",0,"R",0),wire("R",1,"L",0),wire("L",1,"V",1)),pin("V",1));
        complex(2,0,a.analyzer().getCurrent(a.compiled().edges().get("L"),0));
    }
    @Test void dcCapacitorCannotReferenceAnOtherwiseFloatingNode() {
        var parts=List.of(part("V",Kind.DC,10),part("C",Kind.C,1e-6));
        var error=assertThrows(IllegalArgumentException.class,() -> CircuitBridge.analyze(parts,List.of(wire("C",1,"V",1)),pin("V",1)));
        assertTrue(error.getMessage().contains("Tụ điện hở mạch ở DC"));
    }
    @Test void rejectsMissingGroundSourceAndStaleTerminals() {
        var passive=List.of(part("R",Kind.R,10));
        assertThrows(IllegalArgumentException.class,() -> CircuitBridge.compile(passive,List.of(),null));
        assertThrows(IllegalArgumentException.class,() -> CircuitBridge.analyze(passive,List.of(),pin("R",0)));
        assertThrows(IllegalArgumentException.class,() -> CircuitBridge.compile(passive,List.of(wire("R",0,"deleted",1)),pin("R",0)));
        assertThrows(IllegalArgumentException.class,() -> CircuitBridge.compile(List.of(),List.of(),pin("R",0)));
    }
    @Test void rejectsDuplicateIdsAndInvalidElectricalParameters() {
        var r=part("R",Kind.R,10);
        assertThrows(IllegalArgumentException.class,() -> CircuitBridge.compile(List.of(r,r),List.of(),pin("R",0)));
        assertThrows(IllegalArgumentException.class,() -> part("R",Kind.R,0));
        assertThrows(IllegalArgumentException.class,() -> part("C",Kind.C,-1));
        assertThrows(IllegalArgumentException.class,() -> part("L",Kind.L,Double.NaN));
        assertThrows(IllegalArgumentException.class,() -> new Part("V",Kind.AC,5,0,0));
        assertThrows(IllegalArgumentException.class,() -> new Pin("R",2));
    }
}
