import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

////////////////////////////////////////
///// BASE AST CLASSES & UTILITIES /////
////////////////////////////////////////

abstract class AST {
    public String kind;
    public HashMap<String, Object> operands = new HashMap<>();

    public AST(String kind) {
        this.kind = kind;
    }

    public void addOperand(String name, Object value) {
        this.operands.put(name, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print operator
        sb.append("\"kind\":\"" + this.kind + "\",");

        // recursively print operands
        this.operands.forEach((name, value) -> sb.append("\"" + name + "\":" + value.toString() + ","));

        // remove extra comma
        sb.deleteCharAt(sb.length() - 1);

        sb.append("}");
        return sb.toString();
    }

    public void distinctNamesCheck() throws DistinctNamesCheckingException {
    }
}

/**
 * Linked list AST class for temporary use while parsing various list productions.
 * Must NOT be present in the final AST.
 * Use convertToArrayList after the full list has been generated to ensure this.
 * @param <T>
 */
class ListAST<T> extends AST {
    ListAST(boolean isEmpty) {
        super("listnode");
        this.isEmpty = isEmpty;
    }

    ListAST() {
        this(true);
    }

    ListAST(T item, ListAST<T> rest) {
        this(false);
        this.item = item;
        this.rest = rest;
        this.addOperand("item", item);
        this.addOperand("rest", rest);
    }

    ListAST(T item) {
        this(item, new ListAST<T>());
    }

    private ArrayList<T> addToArrayList(ArrayList<T> xs) {
        if (this.isEmpty)
            return xs;
        xs.add(this.item);
        return this.rest.addToArrayList(xs);
    }

    public ArrayList<T> convertToArrayList() {
        return this.addToArrayList(new ArrayList<T>());
    }

    @Override
    public void distinctNamesCheck() throws DistinctNamesCheckingException {
        throw new DistinctNamesCheckingException("Should not be running distinct name checker on ListAST node, which should not exist in the AST in the first place!");
    }

    public final boolean isEmpty;
    private T item;
    private ListAST<T> rest;
}

////////////////////////////////
///// CONCRETE AST CLASSES /////
////////////////////////////////

class ProgramAST extends AST {
    ProgramAST(ClassAST mainclass, ListAST<ClassAST> classes) throws DistinctNamesCheckingException {
        super("__program__");
        this.addOperand("mainclass", mainclass);
        this.addOperand("classes", classes);
        this.mainClass = mainclass;
        this.classes = classes.convertToArrayList();

        this.distinctNamesCheck();

        ClassDescriptors classDescriptors = buildClassDescriptors();
        classDescriptors.debugPrint();
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

    @Override
    public void distinctNamesCheck() throws DistinctNamesCheckingException {
        // check for distinct class names
        HashSet<String> classnames = new HashSet<>();
        for (ClassAST cls : this.classes) {
            if (cls.name.equals("Main")) {
                throw new DistinctNamesCheckingException("Only one Main class allowed!");
            }
            if (classnames.contains(cls.name)) {
                throw new DistinctNamesCheckingException("Duplicate class declaration for " + cls.name + " found!");
            }
            classnames.add(cls.name);
        }

        // recursively check the classes themselves
        this.mainClass.distinctNamesCheck();
        for (ClassAST cls : this.classes) cls.distinctNamesCheck();
    }

    private ClassDescriptors buildClassDescriptors() {
        ClassDescriptors classDescriptors = new ClassDescriptors();

        for (ClassAST cls : this.classes) {
            ClassDescriptor clsDesc = new ClassDescriptor(cls.name);

            for (VarDeclAST field : cls.members) {
                clsDesc.addField(field.name, field.type);
            }

            for (FuncDeclAST method : cls.methods) {
                MethodDescriptor mdDesc = new MethodDescriptor(method.returntype, method.name);
                for (VarDeclAST param : method.params) {
                    mdDesc.addParam(param.name, param.type);
                }
                clsDesc.addMethod(method.name, mdDesc);
            }

            classDescriptors.add(cls.name, clsDesc);
        }

        return classDescriptors;
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

    @Override
    public void distinctNamesCheck() throws DistinctNamesCheckingException {
        // check for duplicate members
        HashSet<String> membernames = new HashSet<>();
        for (VarDeclAST var : this.members) {
            if (membernames.contains(var.name)) {
                throw new DistinctNamesCheckingException("Duplicate member declaration for " + var.name + " found in class " + this.name + "!");
            }
            membernames.add(var.name);
        }

        // check for duplicate methods
        HashSet<String> methodnames = new HashSet<>();
        for (FuncDeclAST func : this.methods) {
            if (methodnames.contains(func.name)) {
                throw new DistinctNamesCheckingException("Duplicate method declaration for " + func.name + " found in class " + this.name + "!");
            }
            methodnames.add(func.name);
        }

        // recursively check the methods themselves
        for (FuncDeclAST func : this.methods) func.distinctNamesCheck();
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

    @Override
    public void distinctNamesCheck() throws DistinctNamesCheckingException {
        HashSet<String> paramnames = new HashSet<>();
        for (VarDeclAST param : this.params) {
            if (paramnames.contains(param.name)) {
                throw new DistinctNamesCheckingException("Duplicate parameter declaration for " + param.name + " found in parameter list for " + this.name + "!");
            }
            paramnames.add(param.name);
        }
    }

    public String name;
    public String returntype;
    public ArrayList<VarDeclAST> params;
    public BlockAST body;
}

class BlockAST extends AST {
    BlockAST(ListAST<VarDeclAST> vardecls, ListAST<StmtAST> stmts) {
        super("block");
        this.addOperand("vardecls", vardecls);
        this.addOperand("stmts", stmts);
        this.vardecls = vardecls.convertToArrayList();
        this.stmts = stmts.convertToArrayList();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        // print var declarations
        sb.append("\"vardecls\":[");
        for (VarDeclAST var: this.vardecls) {
            sb.append(var.toString());
            sb.append(',');
        }
        if (!this.vardecls.isEmpty()) sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        sb.append(",");

        // print statements
        sb.append("\"stmts\":[");
        for (StmtAST stmt : this.stmts) {
            sb.append(stmt.toString());
            sb.append(',');
        }
        if (!this.stmts.isEmpty()) sb.deleteCharAt(sb.length() - 1);
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    public ArrayList<VarDeclAST> vardecls;
    public ArrayList<StmtAST> stmts;
}

class VarDeclAST extends AST {
    VarDeclAST(String type, String name) {
        super("vardecl");
        this.addOperand("type", "\"" + type + "\"");
        this.addOperand("name", "\"" + name + "\"");
        this.type = type;
        this.name = name;
    }

    public String type;
    public String name;
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
        this.condition = condition;
        this.successblock = new BlockAST(new ListAST<>(), successblock);
        this.failureblock = new BlockAST(new ListAST<>(), failureblock);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":" + this.kind + ",");

        // print condition
        sb.append("\"condition\":" + this.condition.toString() + ",");

        // print success block
        sb.append("\"successblock\":" + this.successblock.toString() + ",");

        // print failure block
        sb.append("\"failureblock\":" + this.failureblock.toString() + ",");

        sb.append("}");
        return sb.toString();
    }

    public AST condition;
    public BlockAST successblock;
    public BlockAST failureblock;
}

class WhileStmtAST extends StmtAST {
    WhileStmtAST(AST condition, ListAST<StmtAST> block) {
        super("while");
        this.addOperand("condition", condition);
        this.addOperand("block", block);
        this.condition = condition;
        this.block = new BlockAST(new ListAST<>(), block);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        // print condition
        sb.append("\"condition\":" + this.condition.toString() + ",");

        // print block
        sb.append("\"block\":" + this.block.toString() + ",");

        sb.append("}");
        return sb.toString();
    }

    public AST condition;
    public BlockAST block;
}

class PrintlnAST extends StmtAST {
    PrintlnAST(AST output) {
        super("print");
        this.addOperand("output", output);
    }
}

class ReadlnAST extends StmtAST {
    ReadlnAST(RefAST input) {
        super("read");
        this.addOperand("input", input);
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
        this.func = func;
        this.args = args.convertToArrayList();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        // print func
        sb.append("\"func\":" + this.func.toString() + ",");

        // print args
        sb.append("\"args\":[");
        for (AST arg : this.args) {
            sb.append(arg.toString());
            sb.append(',');
        }
        if (!this.args.isEmpty()) sb.deleteCharAt(sb.length() - 1);
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    public AST func;
    public ArrayList<AST> args;
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