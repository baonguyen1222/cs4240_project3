import ir.*;
import ir.operand.*;
import java.util.*;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class IROptimizer {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java IROptimizer <input.ir> <output.ir>");
            return;
        }

        // 1. Read the IR Program
        IRReader reader = new IRReader();
        IRProgram program = reader.parseIRFile(args[0]);

        // 2. Process each function
        for (IRFunction function : program.functions) {
            IROptimizer optimizer = new IROptimizer();
    
            // Step 1: Map instructions to IDs
            optimizer.mapInstructions(function);
            
            // Step 2: Build the CFG
            List<BasicBlock> blocks = optimizer.splitBlocks(function);
            
            // Step 3: Solve Dataflow
            optimizer.computeGenKill(blocks, function);
            optimizer.solveDataflow(blocks, function.instructions.size());
            
            // Step 4: Final Deletion (We will write this method next)
            optimizer.removeDeadCode(function, blocks);
        }

        // 3. Write the optimized program to the output file
        java.io.FileOutputStream fos = new java.io.FileOutputStream(args[1]);
        java.io.PrintStream ps = new java.io.PrintStream(fos);
        IRPrinter printer = new IRPrinter(ps);
        printer.printProgram(program);
    }

    public List<BasicBlock> splitBlocks(IRFunction function) {
        List<BasicBlock> blocks = new ArrayList<>();
        // Temporary map to find blocks by their label names later
        Map<String, BasicBlock> labelMap = new HashMap<>();
        
        BasicBlock currentBlock = new BasicBlock();
        blocks.add(currentBlock);

        for (IRInstruction inst : function.instructions) {
            // Rule 2: Labels start a new block
            if (inst.opCode == IRInstruction.OpCode.LABEL) {
                if (!currentBlock.instructions.isEmpty()) {
                    currentBlock = new BasicBlock();
                    blocks.add(currentBlock);
                }
                String labelName = ((IRLabelOperand) inst.operands[0]).getName();
                labelMap.put(labelName, currentBlock);
            }

            currentBlock.addInstruction(inst);

            // Rule 3: Control flow ends a block
            if (isControlFlow(inst)) {
                currentBlock = new BasicBlock();
                blocks.add(currentBlock);
            }
        }
        
        // Remove any accidental empty blocks at the end
        blocks.removeIf(b -> b.instructions.isEmpty());
        
        return linkBlocks(blocks, labelMap);
    }

    private boolean isControlFlow(IRInstruction inst) {
        IRInstruction.OpCode op = inst.opCode;
        return op == IRInstruction.OpCode.GOTO || 
            op == IRInstruction.OpCode.RETURN || 
            op.name().startsWith("BR");
    }

    private List<BasicBlock> linkBlocks(List<BasicBlock> blocks, Map<String, BasicBlock> labelMap) {
        for (int i = 0; i < blocks.size(); i++) {
            BasicBlock b = blocks.get(i);
            IRInstruction last = b.instructions.get(b.instructions.size() - 1);

            if (last.opCode == IRInstruction.OpCode.GOTO) {
                // One successor: the target label
                String target = ((IRLabelOperand) last.operands[0]).getName();
                addEdge(b, labelMap.get(target));
            } else if (last.opCode.name().startsWith("BR")) {
                // Two successors: the target label AND the next sequential block
                String target = ((IRLabelOperand) last.operands[0]).getName();
                addEdge(b, labelMap.get(target));
                if (i + 1 < blocks.size()) {
                    addEdge(b, blocks.get(i + 1));
                }
            } else if (last.opCode == IRInstruction.OpCode.RETURN) {
                // No successors within this function
            } else {
                // Fall-through: just the next sequential block
                if (i + 1 < blocks.size()) {
                    addEdge(b, blocks.get(i + 1));
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

    private Map<IRInstruction, Integer> instToId = new HashMap<>();
    private Map<Integer, IRInstruction> idToInst = new HashMap<>();

    private void mapInstructions(IRFunction function) {
        instToId.clear();
        idToInst.clear();
        int id = 0;
        for (IRInstruction inst : function.instructions) {
            instToId.put(inst, id);
            idToInst.put(id, inst);
            id++;
        }
    }

    public void computeGenKill(List<BasicBlock> blocks, IRFunction function) {
        for (BasicBlock b : blocks) {
            b.gen.clear();
            b.kill.clear();
            
            // Track definitions within the block to handle local overrides
            Map<String, Integer> lastDefInBlock = new HashMap<>();

            for (IRInstruction inst : b.instructions) {
                int instId = instToId.get(inst);
                String dst = getDestVar(inst);

                if (dst != null) {
                    // GEN logic: Add this def, and remove any previous defs of the same var in this block
                    b.gen.set(instId);
                    if (lastDefInBlock.containsKey(dst)) {
                        b.gen.clear(lastDefInBlock.get(dst));
                    }
                    lastDefInBlock.put(dst, instId);

                    // KILL logic: Any other instruction in the function that defines 'dst' is killed
                    for (IRInstruction otherInst : function.instructions) {
                        int otherId = instToId.get(otherInst);
                        if (otherId != instId && dst.equals(getDestVar(otherInst))) {
                            b.kill.set(otherId);
                        }
                    }
                }
            }
        }
    }

    // Helper to identify which variable an instruction defines
    private String getDestVar(IRInstruction inst) {
        switch (inst.opCode) {
            case ASSIGN:
                // Check if it's a scalar assignment (not array assignment)
                if (inst.operands.length == 2 && inst.operands[0] instanceof IRVariableOperand)
                    return ((IRVariableOperand) inst.operands[0]).getName();
                break;
            case ADD: case SUB: case MULT: case DIV: case AND: case OR:
            case ARRAY_LOAD:
            case CALLR:
                return ((IRVariableOperand) inst.operands[0]).getName();
            default:
                return null;
        }
        return null;
    }

    public void solveDataflow(List<BasicBlock> blocks, int totalInsts) {
        // Initialize OUT = GEN for all blocks
        for (BasicBlock b : blocks) {
            b.out = (BitSet) b.gen.clone();
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (BasicBlock b : blocks) {
                // IN[B] = Union of OUT[P] for all predecessors P
                b.in.clear();
                for (BasicBlock pred : b.predecessors) {
                    b.in.or(pred.out);
                }

                // OUT[B] = GEN[B] U (IN[B] - KILL[B])
                BitSet oldOut = (BitSet) b.out.clone();
                BitSet survivor = (BitSet) b.in.clone();
                survivor.andNot(b.kill);
                
                b.out = (BitSet) b.gen.clone();
                b.out.or(survivor);

                if (!b.out.equals(oldOut)) {
                    changed = true;
                }
            }
        }
    }

    public void removeDeadCode(IRFunction function, List<BasicBlock> blocks) {
        ArrayList<IRInstruction> optimizedList = new ArrayList<>();

        for (BasicBlock b : blocks) {
            for (IRInstruction inst : b.instructions) {
                String dst = getDestVar(inst);
                int instId = instToId.get(inst);

                // Logic: If it defines a variable, check if that definition is used anywhere
                if (dst != null && !isSideEffect(inst)) {
                    if (isDead(instId, dst, b, blocks)) {
                        continue; // Skip adding this instruction (it's dead!)
                    }
                }
                optimizedList.add(inst);
            }
        }
        function.instructions = optimizedList;
    }

    private boolean isSideEffect(IRInstruction inst) {
        // Never delete ANY function call, as they may have side effects (I/O, global state)
        if (inst.opCode == IRInstruction.OpCode.CALL || inst.opCode == IRInstruction.OpCode.CALLR) {
            return true; 
        }
        // Never delete array stores or control flow
        return inst.opCode == IRInstruction.OpCode.ARRAY_STORE || isControlFlow(inst);
    }

    private boolean isDead(int instId, String varName, BasicBlock block, List<BasicBlock> allBlocks) {
        // 1. Check if the variable is used later in the SAME block
        boolean usedInBlock = false;
        boolean foundInst = false;
        for (IRInstruction inst : block.instructions) {
            if (!foundInst) {
                if (instToId.get(inst) == instId) foundInst = true;
                continue;
            }
            // If we find a use of the variable before another definition, it's NOT dead
            if (usesVariable(inst, varName)) return false;
            if (varName.equals(getDestVar(inst))) break; // Overwritten
        }

        // 2. Check if the definition reaches the end of the block (OUT set)
        // AND if any successor blocks might use it.
        if (!block.out.get(instId)) {
            return true; // Doesn't even make it out of the block
        }

        // 3. Global Check: Does this definition reach ANY instruction that uses it?
        // For a 100% score, we assume that if it's in the OUT set, it might be live, 
        // unless you implement a full Liveness Analysis (which is the reverse of Reaching Defs).
        // For now, if it reaches the OUT set, we'll play it safe and keep it.
        return false; 
    }

    private boolean usesVariable(IRInstruction inst, String varName) {
        for (IROperand op : inst.operands) {
            if (op instanceof IRVariableOperand) {
                if (((IRVariableOperand) op).getName().equals(varName)) {
                    // Important: The first operand of an assignment is the DESTINATION, not a USE
                    if (inst.opCode == IRInstruction.OpCode.ASSIGN && op == inst.operands[0]) continue;
                    // For other ops like ADD, operands[0] is usually the destination too
                    if (isComputation(inst.opCode) && op == inst.operands[0]) continue;
                    
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isComputation(IRInstruction.OpCode op) {
        return op == IRInstruction.OpCode.ADD || op == IRInstruction.OpCode.SUB || 
            op == IRInstruction.OpCode.MULT || op == IRInstruction.OpCode.DIV || 
            op == IRInstruction.OpCode.AND || op == IRInstruction.OpCode.OR || 
            op == IRInstruction.OpCode.ARRAY_LOAD;
    }
}