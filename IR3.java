import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.AbstractMap;

class IR3 {
    public static Integer labelCount = 0;
    public static Integer variableCount = 0;

    public static Integer mkLabel() {
        return IR3.labelCount++;
    }
    public static String mkVar(Type type) {
        String varName = "_v" + IR3.variableCount++;
        SymbolTables.currentTable.setLocal(varName, type);
        return varName;
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
        return "L" + label + ":\n";
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
}

class FunctionEndIR3 extends IR3 {
    @Override
    public String toString() {
        return "}\n";
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

    public ConstructionIR3(String cls) {
        this.lvalue = IR3.mkVar(new RefType(cls));
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
}