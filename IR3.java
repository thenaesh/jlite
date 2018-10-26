import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

class IR3 {
    public static Integer labelCount = 0;
    public static Integer variableCount = 0;

    public static Integer mkLabel() {
        return IR3.labelCount++;
    }
    public static String mkVar() {
        return "__v" + IR3.variableCount++;
    }

    public static String extractLvalue(ArrayList<IR3> irs) {
        if (irs.isEmpty()) {
            return "NO_L_VALUE";
        }
        return irs.get(irs.size() - 1).lvalue;
    }

    public String lvalue;
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
        return "LABEL_" + label + ":\n";
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
        sb.append("Goto " + label + ";\n");
        return sb.toString();
    }
}

class ClassDataIR3 extends IR3 {
    public ClassDescriptors cdesc;
    public ClassDataIR3(ClassDescriptors cdesc) {
        this.cdesc = cdesc;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(cdesc.toString());
        return sb.toString();
    }
}

class FunctionStartIR3 extends IR3 {
    public String classname;
    public String returntype;
    public String name;
    public HashMap<String, String> params = new HashMap<>(); // maps name to type

    public FunctionStartIR3(String classname, String returntype, String name) {
        this.classname = classname;
        this.returntype = returntype;
        this.name = name;
    }

    public void addParam(String name, String type) {
        params.put(name, type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(returntype + " " + name);
        sb.append("(");
        sb.append(classname + " this,");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(" " + entry.getValue() + " " + entry.getKey() + ",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(")");
        sb.append(" {\n");
        return sb.toString();
    }
}

class FunctionEndIR3 extends IR3 {
    @Override
    public String toString() {
        return "}\n";
    }
}

class FunctionCallIR3 extends IR3 {
    public MethodDescriptor md;
    public String obj;
    public ArrayList<String> args;

    public FunctionCallIR3(MethodDescriptor md, String obj, ArrayList<String> args) {
        this.lvalue = IR3.mkVar();
        this.md = md;
        this.obj = obj;
        this.args = args;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + md.name + "(" + obj + ",");
        for (String arg : args) sb.append(arg + ",");
        sb.deleteCharAt(sb.length() - 1);
        sb.append(");\n");
        return sb.toString();
    }
}

class PrintIR3 extends IR3 {
    String output;

    PrintIR3(String output) {
        this.output = output;
    }

    @Override
    public String toString() {
        return "println(" + output + ");\n";
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

class VarDeclIR3 extends IR3 {
    public String name;
    public String type;

    public VarDeclIR3(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        sb.append(" ");
        sb.append(name);
        sb.append(";\n");
        return sb.toString();
    }
}

class ConstructionIR3 extends IR3 {
    public String cls;

    public ConstructionIR3(String cls) {
        this.lvalue = IR3.mkVar();
        this.cls = cls;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = new " + cls + "();\n");
        return sb.toString();
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
}

class MemberAccessIR3 extends IR3 {
    public String obj;
    public String field;

    public MemberAccessIR3(String obj, String field) {
        this.lvalue = IR3.mkVar();
        this.obj = obj;
        this.field = field;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + obj + "." + field + ";\n");
        return sb.toString();
    }
}

class UnOpIR3 extends IR3 {
    public String op;
    public String operand;

    public UnOpIR3(String op, String operand) {
        this.lvalue = IR3.mkVar();
        this.op = op;
        this.operand = operand;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + op + operand + ";\n");
        return sb.toString();
    }
}

class BinOpIR3 extends IR3 {
    public String op;
    public String left;
    public String right;

    public BinOpIR3(String op, String left, String right) {
        this.lvalue = IR3.mkVar();
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
}

class IntIR3 extends IR3 {
    public Integer val;

    public IntIR3(Integer val) {
        this.lvalue = IR3.mkVar();
        this.val = val;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + val + ";\n");
        return sb.toString();
    }
}
class BoolIR3 extends IR3 {
    public Boolean val;

    public BoolIR3(Boolean val) {
        this.lvalue = IR3.mkVar();
        this.val = val;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + val + ";\n");
        return sb.toString();
    }
}
class StringIR3 extends IR3 {
    public String val;

    public StringIR3(String val) {
        this.lvalue = IR3.mkVar();
        this.val = val;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lvalue + " = " + val + ";\n");
        return sb.toString();
    }
}