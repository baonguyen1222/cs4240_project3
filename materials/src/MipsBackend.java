import ir.*;
import ir.operand.*;
import java.io.*;
import java.util.*;

public class MipsBackend {
    private Map<String, Integer> stackOffsets = new HashMap<>();
    private int currentStackOffset = 0;
    private PrintWriter out;
    private IRFunction currentFunctionReference;

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
                currentFunctionReference = function;
                currentStackOffset = 0;
                
                // function label
                out.println(function.name + ":");

                // preamble: calculate stack space needed
                // naive: pre-scan to find all unique variables
                int totalSpace = calculateStackSpace(function);
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
                    for (BasicBlock block : blocks) {
                        generateGreedyBlock(block);
                    }
                } else {
                    for (IRInstruction inst : function.instructions) {
                        generateNaiveInst(inst);
                    }
                }

                String endLabel = function.name + "_end";

                out.println(endLabel + ":");

                // function exit
                if (function.name.equals("main")) {
                    out.println("  li $v0, 10");
                    out.println("  syscall");
                } else {
                    // out.println("  addi $sp, $sp, " + totalSpace);
                    // out.println("  jr $ra");
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
                out.println("  beq $t0, $t1, " + currentFunctionReference.name + "_" + getLabel(inst));
                break;
            
            case BRNEQ:
                loadBranchOperands(inst);
                out.println("  bne $t0, $t1, " + currentFunctionReference.name + "_" + getLabel(inst));
                break;

            case BRGT:
                loadBranchOperands(inst);
                out.println("  bgt $t0, $t1, " + currentFunctionReference.name + "_" + getLabel(inst));
                break;

            case BRLT:
                loadBranchOperands(inst);
                out.println("  blt $t0, $t1, " + currentFunctionReference.name + "_" + getLabel(inst));
                break;

            case BRGEQ:
                loadBranchOperands(inst);
                out.println("  bge $t0, $t1, " + currentFunctionReference.name + "_" + getLabel(inst));
                break;

            // case BRLE:
            //     loadOperand(inst.operands[1], "$t0");
            //     loadOperand(inst.operands[2], "$t1");
            //     out.println("  ble $t0, $t1, " + ((IRLabelOperand)inst.operands[0]).getName());
            //     break;

            case LABEL:
                String label = ((IRLabelOperand)inst.operands[0]).getName();
                out.println(currentFunctionReference.name + "_" + label + ":");
                break;

            case GOTO:
                String target = currentFunctionReference.name + "_" + getLabel(inst);
                out.println("  j " + target);
                break;

            // function calls & return
            case RETURN:
                if (inst.operands.length > 0) {
                    loadOperand(inst.operands[0], "$v0");
                }
                out.println("  j " + currentFunctionReference.name + "_end");
                return;

            case CALL:
            case CALLR:
                String funcName = getLabel(inst);
    
                if (funcName.equals("print_int") || funcName.equals("puti")) {
                    loadOperand(inst.operands[1], "$a0");
                    out.println("  li $v0, 1");
                    out.println("  syscall");
                } else if (funcName.equals("println")) {
                    out.println("  li $a0, 10");
                    out.println("  li $v0, 11"); 
                    out.println("  syscall");
                } else {
                    int argCount = inst.operands.length - 1;

                    for (int i = 0; i < argCount; i++) {
                        if (i < 4) {
                            loadOperand(inst.operands[i + 1], "$a" + i);
                        } else {
                            loadOperand(inst.operands[i + 1], "$t0");
                            out.println("  addi $sp, $sp, -4");
                            out.println("  sw $t0, 0($sp)");
                        }
                    }

                    out.println("  addi $sp, $sp, -4");
                    out.println("  sw $ra, 0($sp)");
                    out.println("  jal " + funcName);
                    out.println("  lw $ra, 0($sp)");
                    out.println("  addi $sp, $sp, 4");
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
        if (!stackOffsets.containsKey(varName)) {
            stackOffsets.put(varName, currentStackOffset);
            currentStackOffset += 4;
        }
        return stackOffsets.get(varName);
    }

    private void loadOperand(IROperand op, String reg) {
        if (op instanceof IRConstantOperand) {
            out.println("  li " + reg + ", " + ((IRConstantOperand) op).getValueString());
        } else if (op instanceof IRVariableOperand) {
            int offset = getOffset(((IRVariableOperand) op).getName());
            out.println("  lw " + reg + ", " + offset + "($sp)");
        }
    }

    private void storeResult(IROperand op, String reg) {
        if (op instanceof IRVariableOperand) {
            int offset = getOffset(((IRVariableOperand) op).getName());
            out.println("  sw " + reg + ", " + offset + "($sp)");
        }
    }

    private int calculateStackSpace(IRFunction func) {
        Set<String> vars = new HashSet<>();
    
        // Include parameters in the stack count
        for (IROperand param : func.parameters) {
            vars.add(((IRVariableOperand)param).getName());
        }

        for (IRInstruction inst : func.instructions) {
            for (IROperand op : inst.operands) {
                if (op instanceof IRVariableOperand) {
                    vars.add(((IRVariableOperand) op).getName());
                }
            }
        }
        
        int space = 4; // for $ra

        for (String var : vars) {
            // TEMP: detect arrays (adjust if your IR stores size differently)
            if (var.contains("[")) {
                space += 400; // assume A[100]
            } else {
                space += 4;
            }
        }

        // align to 8 bytes
        return (space + 7) & ~7;
    }

    private void generateGreedyBlock(BasicBlock block) {
        // 1. Count variable frequency in this block
        Map<String, Integer> frequency = new HashMap<>();
        for (IRInstruction inst : block.instructions) {
            String dest = getDestVar(inst);
            if (dest != null) frequency.put(dest, frequency.getOrDefault(dest, 0) + 1);
            
            for (IROperand op : inst.operands) {
                if (op instanceof IRVariableOperand) {
                    String name = ((IRVariableOperand) op).getName();
                    frequency.put(name, frequency.getOrDefault(name, 0) + 1);
                }
            }
        }

        // 2. Map top 10 variables to $t0-$t9
        List<String> sortedVars = new ArrayList<>(frequency.keySet());
        sortedVars.sort((a, b) -> frequency.get(b) - frequency.get(a));
        
        Map<String, String> regMap = new HashMap<>();
        int regIdx = 0;
        for (String var : sortedVars) {
            if (regIdx > 9) break;
            regMap.put(var, "$t" + regIdx++);
        }

        // 3. Load top variables from stack into registers at start of block
        for (Map.Entry<String, String> entry : regMap.entrySet()) {
            int offset = getOffset(entry.getKey());
            out.println("  lw " + entry.getValue() + ", " + offset + "($sp)");
        }

        // 4. Generate instructions using mapped registers
        for (IRInstruction inst : block.instructions) {
            // Logic: modified loadOperand/storeResult to check regMap first
            generateGreedyInst(inst, regMap);
        }

        // 5. Store top variables back to stack at end of block
        for (Map.Entry<String, String> entry : regMap.entrySet()) {
            int offset = getOffset(entry.getKey());
            out.println("  sw " + entry.getValue() + ", " + offset + "($sp)");
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
                String left = getReg(inst.operands[1], "$v0", regMap, true);
                String right = getReg(inst.operands[2], "$v1", regMap, true);
                String dest = getReg(inst.operands[0], "$t9", regMap, false);
                
                String mipsOp = inst.opCode.toString().toLowerCase();
                if (inst.opCode == IRInstruction.OpCode.MULT) mipsOp = "mul";
                
                if (inst.opCode == IRInstruction.OpCode.DIV) {
                    out.println("  div " + left + ", " + right);
                    out.println("  mflo " + dest);
                } else {
                    out.println("  " + mipsOp + " " + dest + ", " + left + ", " + right);
                }
                
                // If the destination isn't in a greedy register, spill it back immediately
                if (!regMap.containsKey(((IRVariableOperand)inst.operands[0]).getName())) {
                    storeResult(inst.operands[0], dest);
                }
                break;

            case ASSIGN:
                String src = getReg(inst.operands[1], "$v0", regMap, true);
                String target = getReg(inst.operands[0], "$at", regMap, false);
                out.println("  move " + target + ", " + src);
                if (!regMap.containsKey(((IRVariableOperand)inst.operands[0]).getName())) {
                    storeResult(inst.operands[0], target);
                }
                break;

            case BREQ:
            case BRNEQ:
            case BRGT:
            case BRLT:
            case BRGEQ:
            // case BRLE:
                String targetLabel = getScopedLabel(inst.operands[0]); 
                String bLeft = getReg(inst.operands[1], "$v0", regMap, true);
                String bRight = getReg(inst.operands[2], "$v1", regMap, true);

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
                    
                    // 3. RELOAD after call because child function wiped $t0-$t9
                    loadGreedyRegisters(regMap);

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
                int space = calculateStackSpace(currentFunctionReference);
                out.println("  lw $ra, " + (space - 4) + "($sp)"); // Restore Return Address
                out.println("  addi $sp, $sp, " + space);
                out.println("  jr $ra");
                break;

            case LABEL:
                out.println(getScopedLabel(inst.operands[0]) + ":");
                break;

            case GOTO:
                flushGreedyRegisters(regMap); // Save state before jumping away
                out.println("  j " + getScopedLabel(inst.operands[0]));
                break;

            case ARRAY_STORE:
                // IR typically looks like: array_store, base, index, value
                String valS = getReg(inst.operands[0], "$t2", regMap, true);
                String baseS = getReg(inst.operands[1], "$t0", regMap, true);
                String idxS = getReg(inst.operands[2], "$t1", regMap, true);
                
                out.println("  sll $at, " + idxS + ", 2");
                out.println("  add $at, " + baseS + ", $at");
                out.println("  sw " + valS + ", 0($at)");
                break;

            case ARRAY_LOAD:
                // dest = array[index]
                String baseL = getReg(inst.operands[1], "$t0", regMap, true);
                String idxL = getReg(inst.operands[2], "$t1", regMap, true);
                String destL = getReg(inst.operands[0], "$at", regMap, false);

                out.println("  sll $v0, " + idxL + ", 2");
                out.println("  add $v0, " + baseL + ", $v0");
                out.println("  lw " + destL + ", 0($v0)");

                if (!regMap.containsKey(((IRVariableOperand)inst.operands[0]).getName())) {
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
}