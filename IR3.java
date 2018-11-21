import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.AbstractMap;

abstract class IR3 {
    public static Integer labelCount = 0;
    public static Integer variableCount = 0;

    public static Integer mkLabel() {
        return IR3.labelCount++;
    }
    public static String mkVar(Type type) {
        String varName = "_tmp" + IR3.variableCount++;
        SymbolTables.currentTable.setLocal(varName, type);
        return varName;
    }

    public static String extractLvalue(ArrayList<IR3> irs) {
        if (irs.isEmpty()) {
            return "NO_L_VALUE";
        }
        return irs.get(irs.size() - 1).lvalue;
    }

    public static void printIR3(ArrayList<IR3> irs) {
        System.out.println("===== IR3 BEGIN =====\n");
        for (IR3 ir : irs) System.out.println(ir.toString());
        System.out.println("===== IR3 END =====\n");
    }

    public String lvalue;

    public ArrayList<ARMInstruction> toARMInstructions() {
        return new ArrayList<>();
    }
}

class PlaceholderIR3 extends IR3 {
    @Override
    public String toString() {
        return "PLACEHOLDER\n";
    }
}

class LabelIR3 extends IR3 {
    public Integer label;

    public LabelIR3() {
        this.label = IR3.mkLabel();
    }

    @Override
    public String toString() {
        return "L" + label + ":\n";
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();
        instructions.add(new ARMLabel(label));
        return instructions;
    }
}

class GotoIR3 extends IR3 {
    Integer label;
    String condition;

    public GotoIR3(Integer label, String condition) {
        this.condition = condition;
        this.label = label;
    }
    public GotoIR3(Integer label) {
        this(label, null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (condition != null) sb.append("If(" + condition + ") ");
        sb.append("Goto .L" + label + ";\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        if (condition == null) {
            instructions.add(new ARMJump(label));
            return instructions;
        }

        SymbolTableEntry conditionTableEntry = SymbolTables.currentTable.getEntry(condition);

        // place the boolean to check in v1
        if (conditionTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMMov("v1", conditionTableEntry.register));
        } else {
            instructions.add(new ARMSimpleMemoryLoad("v1", "sp", conditionTableEntry.offset));
        }

        // cmp v1, #1
        // beq Ln
        instructions.add(new ARMCmp("v1", 1));
        instructions.add(new ARMJump(label, "eq"));

        return instructions;
    }
}

class FunctionStartIR3 extends IR3 {
    public String returntype;
    public String name;
    public ArrayList<Map.Entry<String, Type>> params = new ArrayList<>();

    public FunctionStartIR3(String returntype, String name) {
        this.returntype = returntype;
        this.name = name;
    }

    public void addParam(String name, String type) {
        params.add(new AbstractMap.SimpleEntry(name, Type.fromTypeString(type)));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(returntype + " " + name);
        sb.append("(");
        for (Map.Entry<String, Type> entry : params) {
            sb.append(entry.getValue() + " " + entry.getKey().toString() + ", ");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.deleteCharAt(sb.length() - 1);
        sb.append(")");
        sb.append(" {\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        instructions.add(new ARMRawLabel(name));
        instructions.add(new ARMSTMFD("lr"));

        Integer sizeOfStackFrameNeeded = SymbolTables.currentTable.size;
        instructions.add(new ARMLoadLiteral("v5", sizeOfStackFrameNeeded));
        instructions.add(new ARMArithmetic("-", "sp", "sp", "v5"));

        return instructions;
    }
}

class FunctionEndIR3 extends IR3 {
    @Override
    public String toString() {
        return "}\n";
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        Integer sizeOfStackFrameNeeded = SymbolTables.currentTable.size;
        instructions.add(new ARMLoadLiteral("v5", sizeOfStackFrameNeeded));
        instructions.add(new ARMArithmetic("+", "sp", "sp", "v5"));

        instructions.add(new ARMLDMFD("pc"));
        instructions.add(new ARMNewline(1));

        return instructions;
    }
}

class FunctionCallIR3 extends IR3 {
    public String name;
    public Type returntype;
    public ArrayList<String> args;

    public FunctionCallIR3(String name, Type returntype, ArrayList<String> args) {
        this.lvalue = IR3.mkVar(returntype);
        this.name = name;
        this.returntype = returntype;
        this.args = args;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + name + "(");
        for (String arg : args) sb.append(arg + ",");
        sb.deleteCharAt(sb.length() - 1);
        sb.append(");\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        // save registers
        instructions.add(new ARMSTMFD("a1"));
        instructions.add(new ARMSTMFD("a2"));
        instructions.add(new ARMSTMFD("a3"));
        instructions.add(new ARMSTMFD("a4"));

        // load arguments
        int paramRegNum = 1;
        for (String arg : args) {
            if (paramRegNum > 4) break; // TODO: support more then 4 arguments!

            String paramReg = "a" + paramRegNum;
            SymbolTableEntry argEntry = SymbolTables.currentTable.getEntry(arg);

            if (argEntry.isRegisterAllocated()) {
                instructions.add(new ARMMov(paramReg, argEntry.register));
            } else {
                instructions.add(new ARMSimpleMemoryLoad(paramReg, "sp", argEntry.offset));
            }
        }

        // branch with link
        instructions.add(new ARMBranchLink(this.name));

        // move the return value from a1 into v1
        instructions.add(new ARMMov("v1", "a1"));

        // restore registers after return
        instructions.add(new ARMLDMFD("a4"));
        instructions.add(new ARMLDMFD("a3"));
        instructions.add(new ARMLDMFD("a2"));
        instructions.add(new ARMLDMFD("a1"));

        // save return value (that is now in a1)
        SymbolTableEntry lvalueTableEntry = SymbolTables.currentTable.getEntry(lvalue);

        if (lvalueTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMMov(lvalueTableEntry.register, "v1"));
        } else {
            instructions.add(new ARMSimpleMemoryStore("v1", "sp", lvalueTableEntry.offset));
        }

        return instructions;
    }
}

class PrintIR3 extends IR3 {
    String output;
    Boolean isInt;

    PrintIR3(String output, Boolean isInt) {
        this.output = output;
        this.isInt = isInt;
    }

    @Override
    public String toString() {
        return "println(" + output + "); " + (isInt ? "as Int" : "") + "\n";
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        SymbolTableEntry outputTableEntry = SymbolTables.currentTable.getEntry(output);

        // instructions.add(new ARMPush("a2")); // save a2
        // instructions.add(new ARMPush("a1")); // save a1
        instructions.add(new ARMMov("v1", "a1"));
        instructions.add(new ARMMov("v2", "a2"));

        if (isInt) {
            instructions.add(new ARMLoadLabel("a1", DataTable.PRINT_INT_FORMAT_STR_LABEL));
            if (outputTableEntry.isRegisterAllocated()) {
                instructions.add(new ARMMov("a2", outputTableEntry.register));
            } else {
                instructions.add(new ARMSimpleMemoryLoad("a2", "sp", outputTableEntry.offset));
            }
        } else {
            if (outputTableEntry.isRegisterAllocated()) {
                instructions.add(new ARMMov("a1", outputTableEntry.register));
            } else {
                instructions.add(new ARMSimpleMemoryLoad("a1", "sp", outputTableEntry.offset));
            }
        }

        instructions.add(new ARMPrintf());

        instructions.add(new ARMMov("a2", "v2"));
        instructions.add(new ARMMov("a1", "v1"));
        // instructions.add(new ARMPop("a1")); // restore a1
        // instructions.add(new ARMPop("a2")); // restore a2

        return instructions;
    }
}

class ReadIR3 extends IR3 {
    String input;

    ReadIR3(String input) {
        this.input = input;
    }

    @Override
    public String toString() {
        return "readln(" + input + ");\n";
    }
}

class ConstructionIR3 extends IR3 {
    public String cls;
    public Integer size;

    public ConstructionIR3(String cls) {
        this.lvalue = IR3.mkVar(new RefType(cls));
        this.cls = cls;

        this.size = ClassTables.get(cls).size;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = new " + cls + "();\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        instructions.add(new ARMLoadLiteral("a1", size));
        instructions.add(new ARMMalloc());
        instructions.add(new ARMMov("v1", "a1"));

        SymbolTableEntry lvalueTableEntry = SymbolTables.currentTable.getEntry(lvalue);

        if (lvalueTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMMov(lvalueTableEntry.register, "v1"));
        } else {
            instructions.add(new ARMSimpleMemoryStore("v1", "sp", lvalueTableEntry.offset));
        }

        return instructions;
    }
}

class ReturnIR3 extends IR3 {
    public String retval;

    public ReturnIR3(String retval) {
        this.retval = retval;
    }
    public ReturnIR3() {
        this(null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Return");
        if (retval != null) sb.append(" " + retval);
        sb.append(";\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        SymbolTableEntry retvalTableEntry = SymbolTables.currentTable.getEntry(retval);

        if (retvalTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMMov("a1", retvalTableEntry.register));
        } else {
            instructions.add(new ARMSimpleMemoryLoad("a1", "sp", retvalTableEntry.offset));
        }

        // leave the function
        Integer sizeOfStackFrameNeeded = SymbolTables.currentTable.size;
        instructions.add(new ARMLoadLiteral("v5", sizeOfStackFrameNeeded));
        instructions.add(new ARMArithmetic("+", "sp", "sp", "v5"));

        instructions.add(new ARMLDMFD("pc"));

        return instructions;
    }
}

class AssignmentIR3 extends IR3 {
    public String val;

    public AssignmentIR3(String assignee, String val) {
        this.lvalue = assignee;
        this.val = val;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + val + ";\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        SymbolTableEntry lvalueTableEntry = SymbolTables.currentTable.getEntry(lvalue);
        SymbolTableEntry rvalueTableEntry = SymbolTables.currentTable.getEntry(val);

        if (lvalueTableEntry.isRegisterAllocated() && rvalueTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMMov(lvalueTableEntry.register, rvalueTableEntry.register));
        } else if (lvalueTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMSimpleMemoryLoad(lvalueTableEntry.register, "sp", rvalueTableEntry.offset));
        } else if (rvalueTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMSimpleMemoryStore(rvalueTableEntry.register, "sp", lvalueTableEntry.offset));
        } else {
            instructions.add(new ARMSimpleMemoryLoad("v1", "sp", rvalueTableEntry.offset));
            instructions.add(new ARMSimpleMemoryStore("v1", "sp", lvalueTableEntry.offset));
        }

        return instructions;
    }
}

class LabelAssignmentIR3 extends IR3 {
    public Integer label;

    public LabelAssignmentIR3(String assignee, Integer label) {
        this.lvalue = assignee;
        this.label = label;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = =L" + label + ";\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        SymbolTableEntry lvalueTableEntry = SymbolTables.currentTable.getEntry(lvalue);

        if (lvalueTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMLoadLabel(lvalueTableEntry.register, label));
        } else {
            instructions.add(new ARMLoadLabel("v1", label));
            instructions.add(new ARMSimpleMemoryStore("v1", "sp", lvalueTableEntry.offset));
        }

        return instructions;
    }
}

class MemberAssignmentIR3 extends IR3 {
    public String field;
    public String val;
    public Integer offset;

    public MemberAssignmentIR3(String assigneeObject, String assigneeField, String val) {
        this.lvalue = assigneeObject;
        this.field = assigneeField;
        this.val = val;

        // a current table is expected to be active during IR3 construction
        Type assigneeObjType = SymbolTables.currentTable.getEntry(assigneeObject).type;
        this.offset = ClassTables.get(assigneeObjType).getFieldOffset(assigneeField);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + "[" + offset + "]" + " = " + val + ";");
        sb.append(" {" + lvalue + "." + field + " = " + val + "}\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        SymbolTableEntry valTableEntry = SymbolTables.currentTable.getEntry(val);

        // put value in v2
        if (valTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMMov("v2", valTableEntry.register));
        } else {
            instructions.add(new ARMSimpleMemoryLoad("v2", "sp", valTableEntry.offset));
        }

        SymbolTableEntry objTableEntry = SymbolTables.currentTable.getEntry(lvalue);

        // put object reference in v1
        if (objTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMMov("v1", objTableEntry.register));
        } else {
            instructions.add(new ARMSimpleMemoryLoad("v1", "sp", objTableEntry.offset));
        }

        // store the contents of v2 into the memory address pointed to by v1 with offset
        instructions.add(new ARMSimpleMemoryStore("v2", "v1", offset));

        return instructions;
    }
}

class MemberAccessIR3 extends IR3 {
    public String obj;
    public String field;
    public Integer offset;

    public MemberAccessIR3(String obj, String field) {
        // a current table is expected to be active during IR3 construction
        RefType objType = (RefType) SymbolTables.currentTable.getEntry(obj).type;

        this.lvalue = IR3.mkVar(ClassTables.get(objType.classname).getFieldType(field));
        this.obj = obj;
        this.field = field;

        this.offset = ClassTables.get(objType).getFieldOffset(field);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + obj + "[" + offset + "];");
        sb.append(" {" + lvalue + " = " + obj + "." + field + "}\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        SymbolTableEntry objTableEntry = SymbolTables.currentTable.getEntry(obj);

        // put object reference in v1
        if (objTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMMov("v1", objTableEntry.register));
        } else {
            instructions.add(new ARMSimpleMemoryLoad("v1", "sp", objTableEntry.offset));
        }

        // load the memory address pointed to by v1 with offset and put it in v2
        instructions.add(new ARMSimpleMemoryLoad("v2", "v1", offset));

        SymbolTableEntry lvalueTableEntry = SymbolTables.currentTable.getEntry(lvalue);

        // write contents of v2 into lvalue mem/reg
        if (lvalueTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMMov(lvalueTableEntry.register, "v2"));
        } else {
            instructions.add(new ARMSimpleMemoryStore("v2", "sp", lvalueTableEntry.offset));
        }

        return instructions;
    }
}

class UnOpIR3 extends IR3 {
    public String op;
    public String operand;

    public UnOpIR3(String op, String operand) {
        this.lvalue = IR3.mkVar(SymbolTables.currentTable.getEntry(operand).type);
        this.op = op;
        this.operand = operand;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + op + operand + ";\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        SymbolTableEntry operandTableEntry = SymbolTables.currentTable.getEntry(operand);

        if (operandTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMArithmetic(op, "v1", operandTableEntry.register));
        } else {
            instructions.add(new ARMSimpleMemoryLoad("v2", "sp", operandTableEntry.offset));
            instructions.add(new ARMArithmetic(op, "v1", "v2"));
        }

        SymbolTableEntry lvalueTableEntry = SymbolTables.currentTable.getEntry(lvalue);

        if (lvalueTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMMov(lvalueTableEntry.register, "v1"));
        } else {
            instructions.add(new ARMSimpleMemoryStore("v1", "sp", lvalueTableEntry.offset));
        }

        return instructions;
    }
}

class BinOpIR3 extends IR3 {
    public String op;
    public String left;
    public String right;

    public BinOpIR3(String op, String left, String right) {
        this.lvalue = IR3.mkVar(SymbolTables.currentTable.getEntry(left).type);
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + left + " " + op + " " + right + ";\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        SymbolTableEntry leftTableEntry = SymbolTables.currentTable.getEntry(left);
        SymbolTableEntry rightTableEntry = SymbolTables.currentTable.getEntry(right);

        if (leftTableEntry.isRegisterAllocated() && rightTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMArithmetic(op, "v1", leftTableEntry.register, rightTableEntry.register));
        } else if (leftTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMSimpleMemoryLoad("v3", "sp", rightTableEntry.offset));
            instructions.add(new ARMArithmetic(op, "v1", "v2", "v3"));
        } else if (rightTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMSimpleMemoryLoad("v2", "sp", leftTableEntry.offset));
            instructions.add(new ARMArithmetic(op, "v1", "v2", "v3"));
        } else {
            instructions.add(new ARMSimpleMemoryLoad("v2", "sp", leftTableEntry.offset));
            instructions.add(new ARMSimpleMemoryLoad("v3", "sp", rightTableEntry.offset));
            instructions.add(new ARMArithmetic(op, "v1", "v2", "v3"));
        }

        SymbolTableEntry lvalueTableEntry = SymbolTables.currentTable.getEntry(lvalue);

        if (lvalueTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMMov(lvalueTableEntry.register, "v1"));
        } else {
            instructions.add(new ARMSimpleMemoryStore("v1", "sp", lvalueTableEntry.offset));
        }

        return instructions;
    }
}

class IntIR3 extends IR3 {
    public Integer val;

    public IntIR3(Integer val) {
        this.lvalue = IR3.mkVar(Type.JLINT);
        this.val = val;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + val + ";\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        SymbolTableEntry lvalueTableEntry = SymbolTables.currentTable.getEntry(lvalue);

        if (lvalueTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMLoadLiteral(lvalueTableEntry.register, val));
        } else {
            instructions.add(new ARMLoadLiteral("v1", val));
            instructions.add(new ARMSimpleMemoryStore("v1", "sp", lvalueTableEntry.offset));
        }

        return instructions;
    }
}
class BoolIR3 extends IR3 {
    public Boolean val;

    public BoolIR3(Boolean val) {
        this.lvalue = IR3.mkVar(Type.JLBOOL);
        this.val = val;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + val + ";\n");
        return sb.toString();
    }

    @Override
    public ArrayList<ARMInstruction> toARMInstructions() {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        SymbolTableEntry lvalueTableEntry = SymbolTables.currentTable.getEntry(lvalue);

        if (lvalueTableEntry.isRegisterAllocated()) {
            instructions.add(new ARMLoadLiteral(lvalueTableEntry.register, val ? 1 : 0));
        } else {
            instructions.add(new ARMLoadLiteral("v1", val ? 1 : 0));
            instructions.add(new ARMSimpleMemoryStore("v1", "sp", lvalueTableEntry.offset));
        }

        return instructions;
    }
}