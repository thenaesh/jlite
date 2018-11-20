import java.util.ArrayList;

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
            if (ir instanceof FunctionEndIR3) {
                SymbolTables.currentTable = null;
            }
            instructions.addAll(ir.toARMInstructions());
        }

        return instructions;
    }

    public static void printASM(ArrayList<ARMInstruction> instructions) {
        System.out.println("===== ASM BEGIN =====\n");
        printASMPreamble();
        for (ARMInstruction instruction : instructions) System.out.println(instruction.toString());
        System.out.println("===== ASM END =====\n");
    }

    public static void printASMPreamble() {
        System.out.println(".data\n");
        DataTable.print();
        System.out.println(".text\n");
        System.out.println(".global main\n.type main, %function\n");
    }

    @Override
    public String toString() {
        return "{PLACEHOLDER FOR " + this.getClass().getSimpleName() + "}";
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

        String setupOffset = String.format("ldr v5, =%d\n", offset);
        String mainInstruction = String.format("ldr %s, [%s, v5]\n", dest, src);

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

        String setupOffset = String.format("ldr v5, =%d\n", offset);
        String mainInstruction = String.format("str %s, [%s, v5]\n", src, dest);

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
            default:
                opinstr = "<UNSUPPORTED>";
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s, %s, %s\n", opinstr, dest, src0, src1);
    }
}

class ARMPrintf extends ARMInstruction {
    @Override
    public String toString() {
        return "b printf(PLT)\n";
    }
}