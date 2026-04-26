import ir.*;
import ir.operand.*;
import java.util.*;

public class IROptimizer {

    private Map<IRInstruction, Integer> instToId = new HashMap<>();
    private Map<Integer, IRInstruction> idToInst = new HashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java IROptimizer <input.ir> <output.ir>");
            return;
        }

        IRReader reader = new IRReader();
        IRProgram program = reader.parseIRFile(args[0]);

        for (IRFunction function : program.functions) {
            IROptimizer optimizer = new IROptimizer();

            boolean changed = true;
            while (changed) {
                String before = function.instructions.toString();

                // assign IDs to instructions for dataflow tracking
                optimizer.mapInstructions(function);

                // break function into basic blocks (CFG nodes)
                List<BasicBlock> blocks = optimizer.splitBlocks(function);

                // compute GEN/KILL sets for reaching definitions
                optimizer.computeGenKill(blocks, function);

                // solve dataflow equations (IN/OUT sets)
                optimizer.solveDataflow(blocks, function.instructions.size());

                // replace variables using simple copy propagation
                optimizer.copyPropagate(blocks);

                // remove instructions that don't affect program output
                optimizer.removeDeadCode_reachingDefs(function, blocks);

                String after = function.instructions.toString();
                changed = !before.equals(after); // stop when stable
            }
        }

        java.io.FileOutputStream fos = new java.io.FileOutputStream(args[1]);
        java.io.PrintStream ps = new java.io.PrintStream(fos);
        IRPrinter printer = new IRPrinter(ps);
        printer.printProgram(program);
    }

    public List<BasicBlock> splitBlocks(IRFunction function) {
        List<BasicBlock> blocks = new ArrayList<>();
        Map<String, BasicBlock> labelMap = new HashMap<>();

        BasicBlock cur = new BasicBlock();
        blocks.add(cur);

        for (IRInstruction inst : function.instructions) {
            // labels start a new basic block
            if (inst.opCode == IRInstruction.OpCode.LABEL) {
                if (!cur.instructions.isEmpty()) {
                    cur = new BasicBlock();
                    blocks.add(cur);
                }

                String label = ((IRLabelOperand) inst.operands[0]).getName();
                labelMap.put(label, cur);// map label → block
            }

            cur.addInstruction(inst);

            if (isControlFlow(inst)) { // control flow ends the current block
                cur = new BasicBlock();
                blocks.add(cur);
            }
        }

        blocks.removeIf(b -> b.instructions.isEmpty()); // remove any empty blocks created accidentally
        return linkBlocks(blocks, labelMap);
    }

    private boolean isControlFlow(IRInstruction inst) {
        IRInstruction.OpCode op = inst.opCode;
        return op == IRInstruction.OpCode.GOTO
            || op == IRInstruction.OpCode.RETURN
            || op.name().startsWith("BR");
    }

    private List<BasicBlock> linkBlocks(List<BasicBlock> blocks, Map<String, BasicBlock> labelMap) {
        for (int i = 0; i < blocks.size(); i++) {
            BasicBlock block = blocks.get(i);
            IRInstruction last = block.instructions.get(block.instructions.size() - 1); // connect blocks based on last instruction

            if (last.opCode == IRInstruction.OpCode.GOTO) {
                String target = ((IRLabelOperand) last.operands[0]).getName();
                // jump to label
                addEdge(block, labelMap.get(target));

            } else if (last.opCode.name().startsWith("BR")) {
                String target = ((IRLabelOperand) last.operands[0]).getName();
                addEdge(block, labelMap.get(target));

                if (i + 1 < blocks.size()) {
                    addEdge(block, blocks.get(i + 1));
                }

            } else if (last.opCode == IRInstruction.OpCode.RETURN) {
                // no successor
            } else {
                if (i + 1 < blocks.size()) {
                    addEdge(block, blocks.get(i + 1));
                }
            }
        }

        return blocks;
    }

    private void addEdge(BasicBlock from, BasicBlock to) {
        if (to != null) {
            from.successors.add(to);
            to.predecessors.add(from);
        }
    }

    private void mapInstructions(IRFunction function) {
    // assign each instruction a unique ID
        instToId.clear();
        idToInst.clear();

        int id = 0;
        for (IRInstruction inst : function.instructions) {
            instToId.put(inst, id);
            idToInst.put(id, inst);
            id++;
        }
    }

    // compute GEN/KILL per block
    public void computeGenKill(List<BasicBlock> blocks, IRFunction function) {
        for (BasicBlock block : blocks) {
            block.gen.clear();
            block.kill.clear();

            Map<String, Integer> lastDef = new HashMap<>();

            for (IRInstruction inst : block.instructions) {
                int instId = instToId.get(inst);
                String dst = getDestVar(inst);

                if (dst != null) {
                    block.gen.set(instId); // this instruction generates a def

                    // remove previous def of same var in this block
                    if (lastDef.containsKey(dst)) {
                        block.gen.clear(lastDef.get(dst));
                    }
                    lastDef.put(dst, instId);

                    // kill all other defs of this variable in function
                    for (IRInstruction other : function.instructions) {
                        int otherId = instToId.get(other);
                        String otherDst = getDestVar(other);

                        if (otherId != instId && dst.equals(otherDst)) {
                            block.kill.set(otherId);
                        }
                    }
                }
            }
        }
    }

    private String getDestVar(IRInstruction inst) {
        switch (inst.opCode) {
            case ASSIGN:
                if (inst.operands.length == 2 && inst.operands[0] instanceof IRVariableOperand) {
                    return ((IRVariableOperand) inst.operands[0]).getName();
                }
                break;

            case ADD:
            case SUB:
            case MULT:
            case DIV:
            case AND:
            case OR:
            case ARRAY_LOAD:
            case CALLR:
                return ((IRVariableOperand) inst.operands[0]).getName();

            default:
                return null;
        }

        return null;
    }

    // initialize OUT = GEN
    public void solveDataflow(List<BasicBlock> blocks, int totalInsts) {
        for (BasicBlock block : blocks) {
            block.out = (BitSet) block.gen.clone();
        }

        boolean changed = true;
        while (changed) {
            changed = false;

            for (BasicBlock block : blocks) {
                block.in.clear();
                for (BasicBlock pred : block.predecessors) {
                    block.in.or(pred.out);
                }

                BitSet oldOut = (BitSet) block.out.clone();
                BitSet survivors = (BitSet) block.in.clone();
                survivors.andNot(block.kill);

                block.out = (BitSet) block.gen.clone();
                block.out.or(survivors);

                if (!block.out.equals(oldOut)) {
                    changed = true;
                }
            }
        }
    }

    public void removeDeadCode_reachingDefs(IRFunction function, List<BasicBlock> blocks) {
        List<IRInstruction> insts = function.instructions;
        Map<IRInstruction, BitSet> rdIn = computeInstr_reachingDefs(function);

        Set<IRInstruction> marked = new HashSet<>();
        Deque<IRInstruction> work = new ArrayDeque<>();

        // start from critical instructions
        for (IRInstruction inst : insts) {
            if (isCritical(inst)) {
                marked.add(inst);
                work.add(inst);
            }
        }

        // walk backwards
        while (!work.isEmpty()) {
            IRInstruction inst = work.removeFirst();
            Set<String> used = getUsed(inst);
            BitSet reaching = rdIn.get(inst);
            if (reaching == null) {
                continue;
            }

            for (String var : used) {
                for (int defId = reaching.nextSetBit(0); defId >= 0; defId = reaching.nextSetBit(defId + 1)) {
                    IRInstruction defInst = idToInst.get(defId);
                    if (defInst == null) {
                        continue;
                    }

                    String dst = getDestVar(defInst);
                    if (var.equals(dst) && !marked.contains(defInst)) {
                        marked.add(defInst);
                        work.add(defInst);
                    }
                }
            }
        }

        List<IRInstruction> kept = new ArrayList<>();
        for (IRInstruction inst : insts) {
            String dst = getDestVar(inst);

            if (marked.contains(inst) || dst == null || isCritical(inst)) {
                kept.add(inst);
            }
        }

        function.instructions = kept;
    }

    private Map<IRInstruction, BitSet> computeInstr_reachingDefs(IRFunction function) {
        List<IRInstruction> insts = function.instructions;
        int n = insts.size();

        Map<IRInstruction, BitSet> inMap = new HashMap<>();
        Map<IRInstruction, BitSet> outMap = new HashMap<>();
        Map<String, Set<Integer>> defsByVar = new HashMap<>();

        // collect all definitions for each variable
        for (IRInstruction inst : insts) {
            String dst = getDestVar(inst);
            if (dst != null) {
                defsByVar.computeIfAbsent(dst, k -> new HashSet<>()).add(instToId.get(inst));
            }
        }
        // get instruction-level predecessors
        Map<IRInstruction, List<IRInstruction>> preds = buildInstrPreds(function);
        // initialize IN and OUT sets
        for (IRInstruction inst : insts) {
            inMap.put(inst, new BitSet(n));

            BitSet out = new BitSet(n);
            String dst = getDestVar(inst);
            if (dst != null) {
                out.set(instToId.get(inst));
            }
            outMap.put(inst, out);
        }
        // solve reaching definitions until nothing changes
        boolean changed = true;
        while (changed) {
            changed = false;

            for (IRInstruction inst : insts) {
                BitSet newIn = new BitSet(n);
                // IN = union of predecessors' OUT
                for (IRInstruction pred : preds.getOrDefault(inst, Collections.emptyList())) {
                    newIn.or(outMap.get(pred));
                }

                BitSet newOut = (BitSet) newIn.clone();
                String dst = getDestVar(inst);

                if (dst != null) {
                    // kill older defs of the same variable
                    Set<Integer> oldDefs = defsByVar.getOrDefault(dst, Collections.emptySet());
                    for (int id : oldDefs) {
                        newOut.clear(id);
                    }
                    // add current definition
                    newOut.set(instToId.get(inst));
                }

                if (!newIn.equals(inMap.get(inst)) || !newOut.equals(outMap.get(inst))) {
                    inMap.put(inst, newIn);
                    outMap.put(inst, newOut);
                    changed = true;
                }
            }
        }

        return inMap;
    }

    private Map<IRInstruction, List<IRInstruction>> buildInstrPreds(IRFunction function) {
        List<IRInstruction> insts = function.instructions;
        Map<IRInstruction, List<IRInstruction>> preds = new HashMap<>();
        Map<String, IRInstruction> labelToInst = new HashMap<>();

        // set up predecessor lists and record labels
        for (IRInstruction inst : insts) {
            preds.put(inst, new ArrayList<>());

            if (inst.opCode == IRInstruction.OpCode.LABEL) {
                String label = ((IRLabelOperand) inst.operands[0]).getName();
                labelToInst.put(label, inst);
            }
        }

        for (int i = 0; i < insts.size(); i++) {
            IRInstruction inst = insts.get(i);

            if (inst.opCode == IRInstruction.OpCode.GOTO) {          // goto only goes to its target label
                String target = ((IRLabelOperand) inst.operands[0]).getName();
                IRInstruction targetInst = labelToInst.get(target);
                if (targetInst != null) {
                    preds.get(targetInst).add(inst);
                }

            } else if (inst.opCode.name().startsWith("BR")) {        // branch can go to target or fall through
                String target = ((IRLabelOperand) inst.operands[0]).getName();
                IRInstruction targetInst = labelToInst.get(target);
                if (targetInst != null) {
                    preds.get(targetInst).add(inst);
                }

                if (i + 1 < insts.size()) {
                    preds.get(insts.get(i + 1)).add(inst);
                }

            } else if (inst.opCode != IRInstruction.OpCode.RETURN) {    // normal instruction falls through to next one
                if (i + 1 < insts.size()) {
                    preds.get(insts.get(i + 1)).add(inst);
                }
            }
        }

        return preds;
    }

    private boolean isCritical(IRInstruction inst) { // these instructions must be kept
        return inst.opCode == IRInstruction.OpCode.CALL
            || inst.opCode == IRInstruction.OpCode.CALLR
            || inst.opCode == IRInstruction.OpCode.RETURN
            || inst.opCode == IRInstruction.OpCode.ARRAY_STORE
            || inst.opCode == IRInstruction.OpCode.LABEL
            || inst.opCode == IRInstruction.OpCode.GOTO
            || inst.opCode.name().startsWith("BR");
    }

    private Set<String> getUsed(IRInstruction inst) {
        Set<String> used = new HashSet<>();
        int start = 0;
        // skip dest
        switch (inst.opCode) {
            case ASSIGN:
            case ADD:
            case SUB:
            case MULT:
            case DIV:
            case AND:
            case OR:
            case ARRAY_LOAD:
            case CALLR:
                start = 1;
                break;
            default:
                start = 0;
        }
        // collect variable operands
        for (int i = start; i < inst.operands.length; i++) {
            IROperand op = inst.operands[i];
            if (op instanceof IRVariableOperand) {
                used.add(((IRVariableOperand) op).getName());
            }
        }

        return used;
    }

    public void copyPropagate(List<BasicBlock> blocks) {
        for (BasicBlock block : blocks) {
            Map<String, String> copies = new HashMap<>();

            for (IRInstruction inst : block.instructions) {
                replaceUsed(inst, copies);  // replace uses with known copies first

                String dst = getDestVar(inst);
                if (dst != null) {  // old map
                    copies.remove(dst);
                    removeMappingUse(copies, dst);
                    removeMappingDefine(copies, dst);
                }

                if (inst.opCode == IRInstruction.OpCode.ASSIGN
                        && inst.operands.length == 2
                        && inst.operands[0] instanceof IRVariableOperand
                        && inst.operands[1] instanceof IRVariableOperand) {

                    String dstVar = ((IRVariableOperand) inst.operands[0]).getName();
                    String srcVar = ((IRVariableOperand) inst.operands[1]).getName();

                    while (copies.containsKey(srcVar)) {
                        srcVar = copies.get(srcVar);
                    }

                    copies.put(dstVar, srcVar);
                }

                if (inst.opCode == IRInstruction.OpCode.CALL
                        || inst.opCode == IRInstruction.OpCode.CALLR) {
                    copies.clear();
                }
            }
        }
    }

    private void replaceUsed(IRInstruction inst, Map<String, String> copies) {
        int start = 0;
        // skip dest
        switch (inst.opCode) {
            case ASSIGN:
            case ADD:
            case SUB:
            case MULT:
            case DIV:
            case AND:
            case OR:
            case ARRAY_LOAD:
            case CALLR:
                start = 1;
                break;
            default:
                start = 0;
        }

        for (int i = start; i < inst.operands.length; i++) { // replace used vars
            IROperand op = inst.operands[i];
            if (op instanceof IRVariableOperand) {
                IRVariableOperand var = (IRVariableOperand) op;
                String name = var.getName();

                if (copies.containsKey(name)) {
                    inst.operands[i] = new IRVariableOperand(var.type, copies.get(name), inst);
                }
            }
        }
    }

    private void removeMappingUse(Map<String, String> copies, String var) {
        List<String> remove = new ArrayList<>();

        for (Map.Entry<String, String> entry : copies.entrySet()) { // remove map
            if (entry.getValue().equals(var)) {
                remove.add(entry.getKey());
            }
        }

        for (String key : remove) {
            copies.remove(key);
        }
    }

    private void removeMappingDefine(Map<String, String> copies, String var) {
        copies.remove(var); // remove direct mapping
    }
}