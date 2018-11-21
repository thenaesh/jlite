import java.util.ArrayList;
import java.nio.file.Paths;
import java.nio.file.Files;

class ARMInstruction {
    public static ArrayList<ARMInstruction> generateARMInstructions(ArrayList<IR3> irs) {
        ArrayList<ARMInstruction> instructions = new ArrayList<>();

        SymbolTables.currentTable = null;
        for (IR3 ir : irs) {
            if (ir instanceof FunctionStartIR3) {
                FunctionStartIR3 funcstartir = (FunctionStartIR3)ir;
                String functionName = funcstartir.name;
                SymbolTables.currentTable = SymbolTables.get(functionName);
            }
            instructions.addAll(ir.toARMInstructions());
            if (ir instanceof FunctionEndIR3) {
                SymbolTables.currentTable = null;
            }
        }

        return instructions;
    }

    public static void writeASMToFile(ArrayList<ARMInstruction> instructions, String filename) {
        try {
            Files.write(Paths.get(filename), getASMString(instructions).getBytes());
        } catch (Exception e) {
            System.out.println("Cannot write out assembly file!");
        }
    }

    public static void printASM(ArrayList<ARMInstruction> instructions) {
        System.out.println(getASMString(instructions));
    }

    public static String getASMString(ArrayList<ARMInstruction> instructions) {
        StringBuilder sb = new StringBuilder();

        sb.append(getLeadingBoilerplate());
        for (ARMInstruction instruction : instructions) sb.append(instruction.toString());
        sb.append(getTrailingBoilerplate());

        return sb.toString();
    }

    public static String getLeadingBoilerplate() {
        StringBuilder sb = new StringBuilder();
        sb.append(".data\n");
        sb.append(DataTable.getTableString());
        sb.append("\n\n\n.text\n");
        sb.append(".global main\n");
        sb.append("\n");
        sb.append("main:\n");
        sb.append("bl " + ClassTables.nameOfMainClass + "_main\n");
        sb.append("b end\n\n");
        return sb.toString();
    }

    public static String getTrailingBoilerplate() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nend:\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "{PLACEHOLDER FOR " + this.getClass().getSimpleName() + "}";
    }
}

class ARMNewline extends ARMInstruction {
    int num;

    public ARMNewline(int num) {
        this.num = num;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < num; i++) sb.append("\n");
        return sb.toString();
    }
}

class ARMSimpleMemoryLoad extends ARMInstruction {
    String src; // register containing (base) address
    String dest; // register
    Integer offset; // from base address

    public ARMSimpleMemoryLoad(String dest, String src, Integer offset) {
        this.src = src;
        this.dest = dest;
        this.offset = offset;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        String setupOffset = String.format("ldr r11, =%d\n", offset);
        String mainInstruction = String.format("ldr %s, [%s, r11]\n", dest, src);

        sb.append(setupOffset);
        sb.append(mainInstruction);

        return sb.toString();
    }
}

class ARMSimpleMemoryStore extends ARMInstruction {
    String src; // register
    String dest; // register containing (base) address
    Integer offset; // from base address

    public ARMSimpleMemoryStore(String src, String dest, Integer offset) {
        this.src = src;
        this.dest = dest;
        this.offset = offset;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        String setupOffset = String.format("ldr r11, =%d\n", offset);
        String mainInstruction = String.format("str %s, [%s, r11]\n", src, dest);

        sb.append(setupOffset);
        sb.append(mainInstruction);

        return sb.toString();
    }
}

class ARMMov extends ARMInstruction {
    String src; // register
    String dest; // register

    public ARMMov(String dest, String src) {
        this.src = src;
        this.dest = dest;
    }

    @Override
    public String toString() {
        return String.format("mov %s, %s\n", dest, src);
    }
}

class ARMLoadLiteral extends ARMInstruction {
    Integer literalSrc;
    String dest; // register

    public ARMLoadLiteral(String dest, Integer literalSrc) {
        this.literalSrc = literalSrc;
        this.dest = dest;
    }

    @Override
    public String toString() {
        return String.format("ldr %s, =%d\n", dest, literalSrc);
    }
}

class ARMLoadLabel extends ARMInstruction {
    Integer label; // source label
    String dest; // register

    public ARMLoadLabel(String dest, Integer label) {
        this.label = label;
        this.dest = dest;
    }

    @Override
    public String toString() {
        return String.format("ldr %s, =L%d\n", dest, label);
    }
}

class ARMArithmetic extends ARMInstruction {
    String opinstr;
    String src0;
    String src1;
    String dest;
    Boolean isNeg = false;
    Boolean isCmp = false;
    String cmpModifier;

    public ARMArithmetic(String op, String dest, String src) {
        switch (op) {
            case "-":
                isNeg = true;
                opinstr = "neg";
                this.dest = dest;
                this.src0 = src;
                break;
            case "!":
                opinstr = "eor";
                this.dest = dest;
                this.src0 = src;
                this.src1 = "#1";
                break;
        }
    }

    public ARMArithmetic(String op, String dest, String src0, String src1) {
        this.src0 = src0;
        this.src1 = src1;
        this.dest = dest;

        switch (op) {
            case "+":
                opinstr = "add";
                break;
            case "-":
                opinstr = "sub";
                break;
            case "*":
                opinstr = "mul";
                break;
            case "||":
                opinstr = "orr";
                break;
            case "&&":
                opinstr = "and";
                break;
            case "==":
                isCmp = true;
                cmpModifier = "eq";
                break;
            case "!=":
                isCmp = true;
                cmpModifier = "ne";
                break;
            case "<=":
                isCmp = true;
                cmpModifier = "le";
                break;
            case ">=":
                isCmp = true;
                cmpModifier = "ge";
                break;
            case "<":
                isCmp = true;
                cmpModifier = "lt";
                break;
            case ">":
                isCmp = true;
                cmpModifier = "gt";
                break;
            default:
                opinstr = "<UNSUPPORTED>";
        }
    }

    @Override
    public String toString() {
        if (isCmp){
            return String.format("mov %s, #0\ncmp %s, %s\nadd%s %s, #1\n", dest, src0, src1, cmpModifier, dest);
        }
        if (isNeg) {
            return String.format("%s %s, %s\n", opinstr, dest, src0);
        }
        return String.format("%s %s, %s, %s\n", opinstr, dest, src0, src1);
    }
}

class ARMPrintf extends ARMInstruction {
    @Override
    public String toString() {
        return "bl printf(PLT)\n";
    }
}

class ARMMalloc extends ARMInstruction {
    @Override
    public String toString() {
        return "bl malloc(PLT)\n";
    }
}

class ARMCmp extends ARMInstruction {
    String reg0;
    String val1;

    public ARMCmp(String reg0, String reg1) {
        this.reg0 = reg0;
        this.val1 = reg1;
    }
    // don't compare with a large literal!
    public ARMCmp(String reg0, Integer literal) {
        this.reg0 = reg0;
        this.val1 = "#" + literal;
    }

    @Override
    public String toString() {
        return String.format("cmp %s, %s\n", reg0, val1);
    }
}

class ARMPush extends ARMInstruction {
    public String reg;

    public ARMPush(String reg) {
        this.reg = reg;
    }

    @Override
    public String toString() {
        return "push {" + reg + "}\n";
    }
}

class ARMPop extends ARMInstruction {
    public String reg;

    public ARMPop(String reg) {
        this.reg = reg;
    }

    @Override
    public String toString() {
        return "pop {" + reg + "}\n";
    }
}

class ARMJump extends ARMInstruction {
    public String condition;
    public Integer toLabel;

    public ARMJump(Integer toLabel, String condition) {
        this.toLabel = toLabel;
        this.condition = condition;
    }
    public ARMJump(Integer toLabel) {
        this(toLabel, "");
    }

    @Override
    public String toString() {
        return String.format("b%s L%d\n", condition, toLabel);
    }
}

class ARMBranchLink extends ARMInstruction {
    public String condition;
    public String toLabel;

    public ARMBranchLink(String toLabel, String condition) {
        this.toLabel = toLabel;
        this.condition = condition;
    }
    public ARMBranchLink(String toLabel) {
        this(toLabel, "");
    }

    @Override
    public String toString() {
        return String.format("bl%s %s\n", condition, toLabel);
    }
}

class ARMLabel extends ARMInstruction {
    public Integer label;

    public ARMLabel(Integer label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("L%d:\n", label);
    }
}

class ARMRawLabel extends ARMInstruction {
    public String label;

    public ARMRawLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("%s:\n", label);
    }
}

class ARMSTMFD extends ARMInstruction {
    public String reg;

    public ARMSTMFD(String reg) {
        this.reg = reg;
    }

    @Override
    public String toString() {
        return String.format("stmfd sp!, {%s}\n", reg);
    }
}

class ARMLDMFD extends ARMInstruction {
    public String reg;

    public ARMLDMFD(String reg) {
        this.reg = reg;
    }

    @Override
    public String toString() {
        return String.format("ldmfd sp!, {%s}\n", reg);
    }
}