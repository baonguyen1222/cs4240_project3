import ir.*;
import ir.operand.*;
import java.io.*;
import java.util.*;
import ir.datatype.IRArrayType;

public class MipsBackend {
    private Map<String, Integer> stackOffsets = new HashMap<>();
    private int currentStackOffset = 0;
    private PrintWriter out;
    private IRFunction currentFunctionReference;
    private int currentStackSize;
    private Set<String> arrayVars = new HashSet<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: java MipsBackend <input.ir> <output.s> <mode>");
            return;
        }

        IRReader reader = new IRReader();
        IRProgram program = reader.parseIRFile(args[0]);
        boolean useGreedy = args[2].equals("--greedy");

        MipsBackend backend = new MipsBackend();
        backend.generateMIPS(program, args[1], useGreedy);
    }

    public void generateMIPS(IRProgram program, String outputFile, boolean useGreedy) {
        try {
            out = new PrintWriter(new FileWriter(outputFile));
            
            // MIPS header
            out.println(".data");
            out.println("  newline: .asciiz \"\\n\""); // For printing
            out.println(".text");
            out.println(".globl main");

            for (IRFunction function : program.functions) {
                // reset for each function
                stackOffsets.clear();
                arrayVars.clear();
                currentFunctionReference = function;
                currentStackOffset = 0;
                
                // function label
                out.println(function.name + ":");

                // preamble: calculate stack space needed
                // naive: pre-scan to find all unique variables
                currentStackSize = calculateStackSpace(function);
                int totalSpace = currentStackSize;
                out.println("  addi $sp, $sp, -" + totalSpace);
                out.println("  sw $ra, " + (totalSpace - 4) + "($sp)"); // Save Return Address

                for (int i = 0; i < function.parameters.size(); i++) {
                    if (i < 4) {
                        String paramName = function.parameters.get(i).getName();
                        // If using greedy and this param is in a regMap, move it there.
                        // Otherwise, store it to its stack offset.
                        int offset = getOffset(paramName);
                        out.println("  sw $a" + i + ", " + offset + "($sp)");
                    }
                }
                
                if (useGreedy) {
                    IROptimizer helper = new IROptimizer();
                    List<BasicBlock> blocks = helper.splitBlocks(function);
                    Map<BasicBlock, Set<String>> liveOutMap = computeLiveOut(blocks);
                    Map<BasicBlock, Set<String>> liveInMap = computeLiveIn(blocks, liveOutMap);

                    for (BasicBlock block : blocks) {
                        generateGreedyBlock(block, liveInMap.get(block), liveOutMap.get(block));
                    }
                } else {
                    for (IRInstruction inst : function.instructions) {
                        generateNaiveInst(inst);
                    }
                }

                String endLabel = function.name + "_exit";
                out.println(endLabel + ":");

                // function exit
                if (function.name.equals("main")) {
                    out.println("  li $v0, 10");
                    out.println("  syscall");
                } else {
                    out.println("  lw $ra, " + (totalSpace - 4) + "($sp)");
                    out.println("  addi $sp, $sp, " + totalSpace);
                    out.println("  jr $ra");
                }
            }
            out.println("\n# Helper Intrinsics");
            out.println("geti:\n  li $v0, 5\n  syscall\n  jr $ra");
            out.println("puti:\n  li $v0, 1\n  syscall\n  jr $ra");
            out.println("putc:\n  li $v0, 11\n  syscall\n  jr $ra");

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateNaiveInst(IRInstruction inst) {
        switch (inst.opCode) {
            case ASSIGN:
                loadOperand(inst.operands[1], "$t0");
                storeResult(inst.operands[0], "$t0");
                break;

            // arithmetic & logic
            case ADD:
                loadOperand(inst.operands[1], "$t0");
                loadOperand(inst.operands[2], "$t1");
                out.println("  add $t2, $t0, $t1");
                storeResult(inst.operands[0], "$t2");
                break;

            case SUB:
                loadOperand(inst.operands[1], "$t0");
                loadOperand(inst.operands[2], "$t1");
                out.println("  sub $t2, $t0, $t1");
                storeResult(inst.operands[0], "$t2");
                break;

            case MULT:
                loadOperand(inst.operands[1], "$t0");
                loadOperand(inst.operands[2], "$t1");
                out.println("  mul $t2, $t0, $t1");
                storeResult(inst.operands[0], "$t2");
                break;

            case DIV:
                loadOperand(inst.operands[1], "$t0");
                loadOperand(inst.operands[2], "$t1");
                out.println("  div $t0, $t1");
                out.println("  mflo $t2");
                storeResult(inst.operands[0], "$t2");
                break;

            case AND:
                loadOperand(inst.operands[1], "$t0");
                loadOperand(inst.operands[2], "$t1");
                out.println("  and $t2, $t0, $t1");
                storeResult(inst.operands[0], "$t2");
                break;

            case OR:
                loadOperand(inst.operands[1], "$t0");
                loadOperand(inst.operands[2], "$t1");
                out.println("  or $t2, $t0, $t1");
                storeResult(inst.operands[0], "$t2");
                break;

            // branching
            case BREQ:
                loadBranchOperands(inst);
                out.println("  beq $t0, $t1, " + getScopedLabel(inst.operands[0]));
                break;
            
            case BRNEQ:
                loadBranchOperands(inst);
                out.println("  bne $t0, $t1, " + getScopedLabel(inst.operands[0]));
                break;

            case BRGT:
                loadBranchOperands(inst);
                out.println("  bgt $t0, $t1, " + getScopedLabel(inst.operands[0]));
                break;

            case BRLT:
                loadBranchOperands(inst);
                out.println("  blt $t0, $t1, " + getScopedLabel(inst.operands[0]));
                break;

            case BRGEQ:
                loadBranchOperands(inst);
                out.println("  bge $t0, $t1, " + getScopedLabel(inst.operands[0]));
                break;

            // case BRLE:
            //     loadOperand(inst.operands[1], "$t0");
            //     loadOperand(inst.operands[2], "$t1");
            //     out.println("  ble $t0, $t1, " + ((IRLabelOperand)inst.operands[0]).getName());
            //     break;

            case LABEL:
                out.println(getScopedLabel(inst.operands[0]) + ":");
                break;

            case GOTO:
                out.println("  j " + getScopedLabel(inst.operands[0]));
                break;

            // function calls & return
            case RETURN:
                if (inst.operands.length > 0) {
                    loadOperand(inst.operands[0], "$v0");
                }
                out.println("  j " + currentFunctionReference.name + "_exit");
                return;

            case CALL:
            case CALLR:
                IROperand funcOp = (inst.opCode == IRInstruction.OpCode.CALLR)
                    ? inst.operands[1]
                    : inst.operands[0];

                String funcName = getIdentifier(funcOp);
    
                if (funcName.equals("print_int") || funcName.equals("puti")) {
                    loadOperand(inst.operands[1], "$a0");
                    out.println("  li $v0, 1");
                    out.println("  syscall");
                } else if (funcName.equals("putc")) {
                    loadOperand(inst.operands[1], "$a0");
                    out.println("  li $v0, 11");
                    out.println("  syscall");
                } else if (funcName.equals("println")) {
                    out.println("  li $a0, 10");
                    out.println("  li $v0, 11");
                    out.println("  syscall");
                } else if (funcName.equals("geti")) {
                    out.println("  li $v0, 5");
                    out.println("  syscall");
                    if (inst.opCode == IRInstruction.OpCode.CALLR) {
                        storeResult(inst.operands[0], "$v0");
                    }
                } else {
                    int argStart = (inst.opCode == IRInstruction.OpCode.CALLR) ? 2 : 1;
                    int argCount = inst.operands.length - argStart;
                    int stackArgs = Math.max(0, argCount - 4);

                    // Push stack args in reverse order
                    for (int i = argCount - 1; i >= 4; i--) {
                        loadOperand(inst.operands[i + argStart], "$t0");
                        out.println("  addi $sp, $sp, -4");
                        out.println("  sw $t0, 0($sp)");
                    }

                    // Load register args
                    for (int i = 0; i < Math.min(argCount, 4); i++) {
                        loadOperand(inst.operands[i + argStart], "$a" + i);
                    }

                    out.println("  jal " + funcName);

                    if (stackArgs > 0) {
                        out.println("  addi $sp, $sp, " + (stackArgs * 4));
                    }

                    if (inst.opCode == IRInstruction.OpCode.CALLR) {
                        storeResult(inst.operands[0], "$v0");
                    }
                }
                break;

            // array access
            case ARRAY_STORE:
                // array[index] = value
                loadOperand(inst.operands[1], "$t0");   // array base
                loadOperand(inst.operands[2], "$t1");   // index
                loadOperand(inst.operands[0], "$t2");   // value
                out.println("  sll $t1, $t1, 2");       // index * 4
                out.println("  add $t0, $t0, $t1");     // base + offset
                out.println("  sw $t2, 0($t0)");
                break;

            case ARRAY_LOAD:
                // dest = array[index]
                loadOperand(inst.operands[1], "$t0");   // array base
                loadOperand(inst.operands[2], "$t1");   // index
                out.println("  sll $t1, $t1, 2");       // index * 4
                out.println("  add $t0, $t0, $t1");     // base + offset
                out.println("  lw $t2, 0($t0)");
                storeResult(inst.operands[0], "$t2");
                break;
        }
    }

    private int getOffset(String varName) {
        Integer offset = stackOffsets.get(varName);
        if (offset == null) {
            throw new RuntimeException("Missing stack offset for variable: " + varName);
        }
        return offset;
    }

    private void loadOperand(IROperand op, String reg) {
        if (op instanceof IRConstantOperand) {
            out.println("  li " + reg + ", " + ((IRConstantOperand) op).getValueString());
            return;
        }

        if (op instanceof IRVariableOperand) {
            String name = ((IRVariableOperand) op).getName();
            int offset = getOffset(name);

            if (arrayVars.contains(name)) {
                boolean isParam = currentFunctionReference.parameters.stream()
                    .anyMatch(p -> ((IRVariableOperand) p).getName().equals(name));
                if (isParam) {
                    out.println("  lw " + reg + ", " + offset + "($sp)");   // load passed-in pointer
                } else {
                    out.println("  addiu " + reg + ", $sp, " + offset);     // compute local array address
                }
            } else {
                out.println("  lw " + reg + ", " + offset + "($sp)");       // normal scalar load
            }
            return;
        }

        if (op instanceof IRFunctionOperand) {
            return;
        }

        throw new RuntimeException("Unsupported operand type: " + op.getClass());
    }

    private void storeResult(IROperand op, String reg) {
        if (op instanceof IRVariableOperand) {
            int offset = getOffset(((IRVariableOperand) op).getName());
            out.println("  sw " + reg + ", " + offset + "($sp)");
        }
    }

    private int calculateStackSpace(IRFunction func) {
        Set<String> vars = new LinkedHashSet<>();
        Map<String, IRVariableOperand> varOperands = new LinkedHashMap<>();

        for (IROperand param : func.parameters) {
            IRVariableOperand v = (IRVariableOperand) param;
            vars.add(v.getName());
            varOperands.put(v.getName(), v);
        }

        for (IRInstruction inst : func.instructions) {
            for (IROperand op : inst.operands) {
                if (op instanceof IRVariableOperand) {
                    IRVariableOperand v = (IRVariableOperand) op;
                    String name = v.getName();
                    vars.add(name);
                    varOperands.put(name, v);

                    if (inst.opCode == IRInstruction.OpCode.ARRAY_LOAD ||
                        inst.opCode == IRInstruction.OpCode.ARRAY_STORE) {
                        if (inst.operands[1] instanceof IRVariableOperand) {
                            String arrName = ((IRVariableOperand) inst.operands[1]).getName();
                            arrayVars.add(arrName);
                        }
                    }
                }
            }
        }

        stackOffsets.clear();
        currentStackOffset = 0;

        for (String var : vars) {
            stackOffsets.put(var, currentStackOffset);

            if (arrayVars.contains(var)) {
                IRVariableOperand v = varOperands.get(var);
                int arraySize = 100; // fallback
                if (v.type instanceof IRArrayType) {
                    arraySize = ((IRArrayType) v.type).getSize();
                }
                currentStackOffset += arraySize * 4;
            } else {
                currentStackOffset += 4;
            }
        }

        int space = currentStackOffset + 4; // +4 for $ra
        return (space + 7) & ~7;
    }

    private void generateGreedyBlock(BasicBlock block, Set<String> liveIn, Set<String> liveOut) {
        Map<String, Integer> frequency = new HashMap<>();
        for (IRInstruction inst : block.instructions) {
            for (IROperand op : inst.operands) {
                if (op instanceof IRVariableOperand) {
                    String name = ((IRVariableOperand) op).getName();
                    frequency.put(name, frequency.getOrDefault(name, 0) + 1);
                }
            }
        }

        // 2. Assign top 8 vars to $t0-$t7 ($t8/$t9 reserved as scratch)
        List<String> sortedVars = new ArrayList<>(frequency.keySet());
        sortedVars.sort((a, b) -> frequency.get(b) - frequency.get(a));

        Map<String, String> regMap = new HashMap<>();
        int regIdx = 0;
        for (String var : sortedVars) {
            if (regIdx > 7) break;
            regMap.put(var, "$t" + regIdx++);
        }

        // 3. Only load vars that are live-in (have meaningful values entering this block)
        for (Map.Entry<String, String> entry : regMap.entrySet()) {
            if (liveIn.contains(entry.getKey())) {
                int offset = getOffset(entry.getKey());
                out.println("  lw " + entry.getValue() + ", " + offset + "($sp)");
            }
        }

        // 4. Generate instructions
        for (IRInstruction inst : block.instructions) {
            generateGreedyInst(inst, regMap);
        }

        // 5. Only store vars that are live-out (needed by successors)
        for (Map.Entry<String, String> entry : regMap.entrySet()) {
            if (liveOut.contains(entry.getKey())) {
                int offset = getOffset(entry.getKey());
                out.println("  sw " + entry.getValue() + ", " + offset + "($sp)");
            }
        }
    }

    private void generateGreedyInst(IRInstruction inst, Map<String, String> regMap) {
        switch (inst.opCode) {
            case ADD:
            case SUB:
            case MULT:
            case DIV:
            case AND:
            case OR:
                String left = getReg(inst.operands[1], "$t8", regMap, true);
                String right = getReg(inst.operands[2], "$t9", regMap, true);
                String dest = getReg(inst.operands[0], "$t8", regMap, false);
                
                String mipsOp = inst.opCode.toString().toLowerCase();
                if (inst.opCode == IRInstruction.OpCode.MULT) mipsOp = "mul";
                
                if (inst.opCode == IRInstruction.OpCode.DIV) {
                    out.println("  div " + left + ", " + right);
                    out.println("  mflo " + dest);
                } else {
                    out.println("  " + mipsOp + " " + dest + ", " + left + ", " + right);
                }
                
                // If the destination isn't in a greedy register, spill it back immediately
                if (isVar(inst.operands[0])) {
                    String name = getVarName(inst.operands[0]);
                    if (!regMap.containsKey(name)) {
                        storeResult(inst.operands[0], dest);
                    }
                }
                break;

            case ASSIGN:
                String src = getReg(inst.operands[1], "$t8", regMap, true);
                String target = getReg(inst.operands[0], "$t8", regMap, false);
                out.println("  move " + target + ", " + src);
                if (isVar(inst.operands[0])) {
                    String name = getVarName(inst.operands[0]);
                    if (!regMap.containsKey(name)) {
                        storeResult(inst.operands[0], target);
                    }
                }
                break;

            case BREQ:
            case BRNEQ:
            case BRGT:
            case BRLT:
            case BRGEQ:
            // case BRLE:
                String targetLabel = getScopedLabel(inst.operands[0]); 
                String bLeft = getReg(inst.operands[1], "$t8", regMap, true);
                String bRight = getReg(inst.operands[2], "$t9", regMap, true);

                String opStr = inst.opCode.toString().toLowerCase();
                String branchOp = "b" + opStr.substring(2); 
                if (branchOp.equals("bneq")) branchOp = "bne";
                if (branchOp.equals("bgeq")) branchOp = "bge";

                flushGreedyRegisters(regMap); // Save state because we might jump
                out.println("  " + branchOp + " " + bLeft + ", " + bRight + ", " + targetLabel);
                break;

            case CALL:
            case CALLR:
                boolean isCallR = (inst.opCode == IRInstruction.OpCode.CALLR);
                IROperand funcOp = isCallR ? inst.operands[1] : inst.operands[0];
                String funcName = getIdentifier(funcOp);
                
                if (funcName.equals("printi") || funcName.equals("puti") || funcName.equals("putc")) {
                    // For CALL: arg is at index 1. For CALLR: arg would be at index 2.
                    int argIndex = isCallR ? 2 : 1;
                    String val = getReg(inst.operands[argIndex], "$a0", regMap, true);
                    if (!val.equals("$a0")) out.println("  move $a0, " + val);
                    if (funcName.equals("putc")) {
                        out.println("  li $v0, 11");
                    } else {
                        out.println("  li $v0, 1");
                    }
                    out.println("  syscall");
                } 
                else {
                    flushGreedyRegisters(regMap); 
        
                    // Setup arguments (ensure you use the right indices from your IR)
                    int argStart = isCallR ? 2 : 1;
                    for (int i = argStart; i < inst.operands.length; i++) {
                        int argRegIdx = i - argStart;
                        if (argRegIdx < 4) {
                            String argReg = getReg(inst.operands[i], "$a" + argRegIdx, regMap, true);
                            if (!argReg.equals("$a" + argRegIdx)) out.println("  move $a" + argRegIdx + ", " + argReg);
                        }
                    }

                    out.println("  jal " + funcName);
                    
                    // Only reload variables that are actually live AFTER the call
                    for (Map.Entry<String, String> entry : regMap.entrySet()) {
                        int offset = getOffset(entry.getKey());
                        out.println("  lw " + entry.getValue() + ", " + offset + "($sp)");
                    }

                    if (isCallR) {
                        IROperand destOp = inst.operands[0];
                        String retDestReg = getReg(destOp, "$v0", regMap, false);
                        if (!retDestReg.equals("$v0")) out.println("  move " + retDestReg + ", $v0");
                        if (!regMap.containsKey(((IRVariableOperand)destOp).getName())) {
                            storeResult(destOp, "$v0");
                        }
                    }
                }
                break;

            case RETURN:
                if (inst.operands.length > 0) {
                    String retVal = getReg(inst.operands[0], "$v0", regMap, true);
                    if (!retVal.equals("$v0")) out.println("  move $v0, " + retVal);
                }
                out.println("  j " + currentFunctionReference.name + "_exit");
                break;

            case LABEL:
                flushGreedyRegisters(regMap);
                out.println(getScopedLabel(inst.operands[0]) + ":");
                break;

            case GOTO:
                flushGreedyRegisters(regMap); // Save state before jumping away
                out.println("  j " + getScopedLabel(inst.operands[0]));
                break;

            case ARRAY_STORE:
                String baseS = getReg(inst.operands[1], "$t8", regMap, true);  // array base → $t8
                String idxS  = getReg(inst.operands[2], "$t9", regMap, true);  // index → $t9
                out.println("  sll $t9, " + idxS + ", 2");                     // index * 4
                out.println("  add $t8, " + baseS + ", $t9");                  // address in $t8
                String valS  = getReg(inst.operands[0], "$t9", regMap, true);  // value → $t9
                out.println("  sw " + valS + ", 0($t8)");
                break;

            case ARRAY_LOAD:
                String baseL = getReg(inst.operands[1], "$t8", regMap, true);
                String idxL  = getReg(inst.operands[2], "$t9", regMap, true);
                String destL = getReg(inst.operands[0], "$t8", regMap, false);
                out.println("  sll $t9, " + idxL + ", 2");
                out.println("  add $t9, " + baseL + ", $t9");
                out.println("  lw " + destL + ", 0($t9)");
                if (isVar(inst.operands[0]) && !regMap.containsKey(getVarName(inst.operands[0]))) {
                    storeResult(inst.operands[0], destL);
                }
                break;
        }
    }

    private String getReg(IROperand op, String tempReg, Map<String, String> regMap, boolean isLoad) {
        if (op instanceof IRConstantOperand) {
            out.println("  li " + tempReg + ", " + ((IRConstantOperand) op).getValueString());
            return tempReg;
        } 
        
        // Safety check: ensure it's actually a variable before casting
        if (op instanceof IRVariableOperand) {
            String varName = ((IRVariableOperand) op).getName();
            if (regMap.containsKey(varName)) {
                return regMap.get(varName); 
            } else {
                if (isLoad) {
                    loadOperand(op, tempReg); 
                }
                return tempReg;
            }
        }

        
        if (op instanceof IRFunctionOperand) {
            return tempReg;
        }
        
        // If it's a label operand being used as a value (which shouldn't happen in valid IR arithmetic)
        return tempReg; 
    }

    private String getDestVar(IRInstruction inst) {
        switch (inst.opCode) {
            case ASSIGN:
            case ADD:
            case SUB:
            case MULT:
            case DIV:
            case AND:
            case OR:
            case CALLR:
            case ARRAY_LOAD:
                if (inst.operands.length > 0 && inst.operands[0] instanceof IRVariableOperand) {
                    return ((IRVariableOperand) inst.operands[0]).getName();
                }
                break;
            default:
                return null;
        }
        return null;
    }

    private String getIdentifier(IROperand op) {
        if (op instanceof IRFunctionOperand) return ((IRFunctionOperand) op).getName();
        if (op instanceof IRLabelOperand) return ((IRLabelOperand) op).getName();
        if (op instanceof IRVariableOperand) return ((IRVariableOperand) op).getName();
        return op.toString();
    }

    private String getScopedLabel(IROperand op) {
        String label = ((IRLabelOperand)op).getName();
        return currentFunctionReference.name + "_" + label;
    }

    private void flushGreedyRegisters(Map<String, String> regMap) {
        for (Map.Entry<String, String> entry : regMap.entrySet()) {
            int offset = getOffset(entry.getKey());
            out.println("  sw " + entry.getValue() + ", " + offset + "($sp)");
        }
    }

    private void loadGreedyRegisters(Map<String, String> regMap) {
        for (Map.Entry<String, String> entry : regMap.entrySet()) {
            int offset = getOffset(entry.getKey());
            out.println("  lw " + entry.getValue() + ", " + offset + "($sp)");
        }
    }

    private String getLabelFromAnywhere(IRInstruction inst) {
        for (IROperand op : inst.operands) {
            if (op instanceof IRLabelOperand) {
                return ((IRLabelOperand) op).getName();
            }
        }
        // Fallback: use the last operand if no Label type is found
        return inst.operands[inst.operands.length - 1].toString();
    }

    private String getLabel(IRInstruction inst) {
        for (IROperand op : inst.operands) {
            if (op instanceof IRLabelOperand) return ((IRLabelOperand) op).getName();
            if (op instanceof IRFunctionOperand) return ((IRFunctionOperand) op).getName();
        }
        // If no specific operand is found, the label is usually the last one
        return inst.operands[inst.operands.length - 1].toString();
    }

    private void loadBranchOperands(IRInstruction inst) {
        // Always load the first operand into $t0
        loadOperand(inst.operands[1], "$t0");
        
        // If there is a second variable before the label, load it into $t1
        // Otherwise, compare against zero
        if (inst.operands.length > 2) {
            loadOperand(inst.operands[2], "$t1");
        } else {
            out.println("  li $t1, 0");
        }
    }
    
    private boolean isVar(IROperand op) {
        return op instanceof IRVariableOperand;
    }

    private String getVarName(IROperand op) {
        return ((IRVariableOperand) op).getName();
    }

    private Map<BasicBlock, Set<String>> computeLiveOut(List<BasicBlock> blocks) {
        Map<BasicBlock, Set<String>> liveIn  = new HashMap<>();
        Map<BasicBlock, Set<String>> liveOut = new HashMap<>();

        for (BasicBlock b : blocks) {
            liveIn.put(b,  new HashSet<>());
            liveOut.put(b, new HashSet<>());
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = blocks.size() - 1; i >= 0; i--) {
                BasicBlock b = blocks.get(i);

                // liveOut[b] = union of liveIn of all successors
                Set<String> newOut = new HashSet<>();
                for (BasicBlock succ : b.successors) {
                    newOut.addAll(liveIn.get(succ));
                }

                // compute use and def for this block
                Set<String> use = new HashSet<>();
                Set<String> def = new HashSet<>();
                for (IRInstruction inst : b.instructions) {
                    int srcStart = hasDest(inst) ? 1 : 0;
                    for (int j = srcStart; j < inst.operands.length; j++) {
                        if (inst.operands[j] instanceof IRVariableOperand) {
                            String name = ((IRVariableOperand) inst.operands[j]).getName();
                            if (!def.contains(name)) use.add(name);
                        }
                    }
                    if (hasDest(inst) && inst.operands[0] instanceof IRVariableOperand) {
                        def.add(((IRVariableOperand) inst.operands[0]).getName());
                    }
                }

                // liveIn[b] = use union (liveOut[b] - def)
                Set<String> newIn = new HashSet<>(use);
                Set<String> outMinusDef = new HashSet<>(newOut);
                outMinusDef.removeAll(def);
                newIn.addAll(outMinusDef);

                if (!newOut.equals(liveOut.get(b)) || !newIn.equals(liveIn.get(b))) {
                    changed = true;
                    liveOut.put(b, newOut);
                    liveIn.put(b, newIn);
                }
            }
        }

        return liveOut;
    }

    private Map<BasicBlock, Set<String>> computeLiveIn(List<BasicBlock> blocks, Map<BasicBlock, Set<String>> liveOutMap) {
        Map<BasicBlock, Set<String>> liveInMap = new HashMap<>();

        for (BasicBlock b : blocks) {
            Set<String> use = new HashSet<>();
            Set<String> def = new HashSet<>();
            for (IRInstruction inst : b.instructions) {
                int srcStart = hasDest(inst) ? 1 : 0;
                for (int j = srcStart; j < inst.operands.length; j++) {
                    if (inst.operands[j] instanceof IRVariableOperand) {
                        String name = ((IRVariableOperand) inst.operands[j]).getName();
                        if (!def.contains(name)) use.add(name);
                    }
                }
                if (hasDest(inst) && inst.operands[0] instanceof IRVariableOperand) {
                    def.add(((IRVariableOperand) inst.operands[0]).getName());
                }
            }
            Set<String> liveIn = new HashSet<>(use);
            Set<String> outMinusDef = new HashSet<>(liveOutMap.get(b));
            outMinusDef.removeAll(def);
            liveIn.addAll(outMinusDef);
            liveInMap.put(b, liveIn);
        }

        return liveInMap;
    }

    private boolean hasDest(IRInstruction inst) {
        switch (inst.opCode) {
            case ASSIGN: case ADD: case SUB: case MULT: case DIV:
            case AND: case OR: case CALLR: case ARRAY_LOAD:
                return true;
            default:
                return false;
        }
    }
}