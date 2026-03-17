import ir.*;
import java.util.*;

public class BasicBlock {
    public List<IRInstruction> instructions = new ArrayList<>();
    public List<BasicBlock> predecessors = new ArrayList<>();
    public List<BasicBlock> successors = new ArrayList<>();

    // Dataflow sets
    public BitSet gen = new BitSet();
    public BitSet kill = new BitSet();
    public BitSet in = new BitSet();
    public BitSet out = new BitSet();

    public void addInstruction(IRInstruction inst) {
        instructions.add(inst);
    }
}