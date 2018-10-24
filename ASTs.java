import java.util.ArrayList;

class ProgramAST extends AST {
    ProgramAST(ClassAST mainclass, ListAST<ClassAST> classes) {
        super("__program__");
        this.addOperand("mainclass", mainclass);
        this.addOperand("classes", classes);
        this.mainClass = mainclass;
        this.classes = classes.convertToArrayList();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        // print main class
        sb.append("\"mainClass\":");
        sb.append(this.mainClass.toString());
        sb.append(",");

        // print other classes
        sb.append("\"classes\":[");
        for (ClassAST cls : this.classes) {
            sb.append(cls.toString());
            sb.append(',');
        }
        if (!this.classes.isEmpty()) sb.deleteCharAt(sb.length() - 1);
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    public ClassAST mainClass;
    public ArrayList<ClassAST> classes;
}

class ClassAST extends AST {
    ClassAST(String name, ListAST<VarDeclAST> members, ListAST<FuncDeclAST> methods) {
        super("classdecl");
        this.addOperand("name", "\"" + name + "\"");
        this.addOperand("members", members);
        this.addOperand("methods", methods);
        this.name = name;
        this.members = members.convertToArrayList();
        this.methods = methods.convertToArrayList();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        // print class name
        sb.append("\"classname\":\"" + this.name + "\",");

        // print members 
        sb.append("\"members\":[");
        for (VarDeclAST var: this.members) {
            sb.append(var.toString());
            sb.append(',');
        }
        if (!this.members.isEmpty()) sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        sb.append(",");

        // print methods
        sb.append("\"methods\":[");
        for (FuncDeclAST func : this.methods) {
            sb.append(func.toString());
            sb.append(',');
        }
        if (!this.methods.isEmpty()) sb.deleteCharAt(sb.length() - 1);
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    public String name;
    public ArrayList<VarDeclAST> members;
    public ArrayList<FuncDeclAST> methods;
}

class FuncDeclAST extends AST {
    FuncDeclAST(String returntype, String name, ListAST<VarDeclAST> params, BlockAST body) {
        super("funcdecl");
        this.addOperand("returntype", "\"" + returntype + "\"");
        this.addOperand("name", "\"" + name + "\"");
        this.addOperand("params", params);
        this.addOperand("body", body);
        this.name = name;
        this.returntype = returntype;
        this.params = params.convertToArrayList();
        this.body = body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        // print function name
        sb.append("\"funcname\":\"" + this.name + "\",");

        // print return type
        sb.append("\"returntype\":\"" + this.returntype + "\",");

        // print params
        sb.append("\"members\":[");
        for (VarDeclAST var: this.params) {
            sb.append(var.toString());
            sb.append(',');
        }
        if (!this.params.isEmpty()) sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        sb.append(",");

        // print body
        sb.append("\"body\":");
        sb.append(this.body.toString());

        sb.append("}");
        return sb.toString();
    }

    public String name;
    public String returntype;
    public ArrayList<VarDeclAST> params;
    public BlockAST body;
}

class PrintlnAST extends AST {
    PrintlnAST(AST output) {
        super("printdecl");
        this.addOperand("output", output);
    }
}

class ReadlnAST extends AST {
    ReadlnAST(RefAST input) {
        super("readdecl");
        this.addOperand("input", input);
    }
}

class BlockAST extends AST {
    BlockAST(ListAST<VarDeclAST> vardecls, ListAST<StmtAST> stmts) {
        super("block");
        this.addOperand("vardecls", vardecls);
        this.addOperand("stmts", stmts);
    }
}

class VarDeclAST extends AST {
    VarDeclAST(String type, String name) {
        super("vardecl");
        this.addOperand("type", "\"" + type + "\"");
        this.addOperand("name", "\"" + name + "\"");
    }
}

class StmtAST extends AST {
    StmtAST(String type) {
        super(type + "stmt");
    }
}

class TmpStmtAST extends AST {
    TmpStmtAST() {
        super("__TMP__");
    }
}

class AssignStmtAST extends StmtAST {
    AssignStmtAST(AST assignee, AST val) {
        super("assignment");
        this.addOperand("assignee", assignee);
        this.addOperand("val", val);
    }
}

class ReturnStmtAST extends StmtAST {
    ReturnStmtAST() {
        super("return");
    }

    ReturnStmtAST(AST retval) {
        this();
        this.addOperand("retval", retval);
    }
}

class IfStmtAST extends StmtAST {
    IfStmtAST(AST condition, ListAST<StmtAST> successblock, ListAST<StmtAST> failureblock) {
        super("if");
        this.addOperand("condtion", condition);
        this.addOperand("successblock", successblock);
        this.addOperand("failureblock", failureblock);
    }
}

class WhileStmtAST extends AST {
    WhileStmtAST(AST condition, ListAST<StmtAST> block) {
        super("while");
        this.addOperand("condition", condition);
        this.addOperand("block", block);
    }
}

class UnOpAST extends AST {
    UnOpAST(String name, AST operand) {
        super("unaryoperation");
        this.addOperand("operator", "\"" + name + "\"");
        this.addOperand("operand", operand);
    }
}

class BinOpAST extends AST {
    BinOpAST(String name, AST left, AST right) {
        super("binaryoperation");
        this.addOperand("operator", "\"" + name + "\"");
        this.addOperand("left", left);
        this.addOperand("right", right);
    }
}

class NullPtrAST extends AST {
    NullPtrAST() {
        super("nullptr");
    }
}

class ThisPtrAST extends AST {
    ThisPtrAST() {
        super("this");
    }
}

class ConstructionAST extends AST {
    ConstructionAST(String classname) {
        super("construction");
        this.addOperand("classname", "\"" + classname + "\"");
    }
}

class RefAST extends AST {
    RefAST(String id) {
        super("reference");
        this.addOperand("id", "\"" + id + "\"");
    }
}

class FuncCallAST extends AST {
    FuncCallAST(AST func, ListAST<AST> args) {
        super("funccall");
        this.addOperand("function", func);
        this.addOperand("args", args);
    }
}

class MemberAccessAST extends AST {
    MemberAccessAST(AST obj, String field) {
        super("memberaccess");
        this.addOperand("obj", obj);
        this.addOperand("field", "\"" + field + "\"");
    }
}

class ConstAST<T> extends AST {
    ConstAST(T val) {
        super("const");
        this.addOperand("val", val);
    }
}