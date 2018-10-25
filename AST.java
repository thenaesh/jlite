import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

////////////////////////////////////////
///// BASE AST CLASSES & UTILITIES /////
////////////////////////////////////////

abstract class AST {
    public String kind;
    public String __type__;
    public HashMap<String, Object> operands = new HashMap<>();

    public AST(String kind) {
        this.kind = kind;
        this.__type__ = "NONE";
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
        sb.append("\"params\":[");
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
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"name\":\"" + this.name + "\",");
        sb.append("\"type\":\"" + this.type + "\"");

        sb.append("}");
        return sb.toString();
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
        this.assignee = assignee;
        this.val = val;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"assignee\":" + this.assignee.toString() + ",");
        sb.append("\"val\":" + this.val.toString());

        sb.append("}");
        return sb.toString();
    }

    public AST assignee;
    public AST val;
}

class ReturnStmtAST extends StmtAST {
    ReturnStmtAST() {
        this(VoidAST.value);
    }

    ReturnStmtAST(AST retval) {
        super("return");
        this.retval = retval;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"retval\":" + this.retval.toString());

        sb.append("}");
        return sb.toString();
    }

    public AST retval;
}

class IfStmtAST extends StmtAST {
    IfStmtAST(AST condition, ListAST<StmtAST> successblock, ListAST<StmtAST> failureblock) {
        super("if");
        this.condition = condition;
        this.successblock = new BlockAST(new ListAST<>(), successblock);
        this.failureblock = new BlockAST(new ListAST<>(), failureblock);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        // print condition
        sb.append("\"condition\":" + this.condition.toString() + ",");

        // print success block
        sb.append("\"successblock\":" + this.successblock.toString() + ",");

        // print failure block
        sb.append("\"failureblock\":" + this.failureblock.toString());

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
        sb.append("\"block\":" + this.block.toString());

        sb.append("}");
        return sb.toString();
    }

    public AST condition;
    public BlockAST block;
}

class PrintlnAST extends StmtAST {
    PrintlnAST(AST output) {
        super("print");
        this.__type__ = "Void";
        this.output = output;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"output\":" + this.output.toString());

        sb.append("}");
        return sb.toString();
    }

    public AST output;
}

class ReadlnAST extends StmtAST {
    ReadlnAST(RefAST input) {
        super("read");
        this.__type__ = "Void";
        this.input = input;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"input\":" + this.input.toString());

        sb.append("}");
        return sb.toString();
    }

    public RefAST input;
}

class UnOpAST extends AST {
    UnOpAST(String name, AST operand) {
        super("unaryoperation");
        this.name = name;
        this.operand = operand;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"name\":\"" + this.name + "\",");
        sb.append("\"operand\":" + this.operand.toString());

        sb.append("}");
        return sb.toString();
    }

    public String name;
    public AST operand;
}

class BinOpAST extends AST {
    BinOpAST(String name, AST left, AST right) {
        super("binaryoperation");
        this.name = name;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"name\":\"" + this.name + "\",");
        sb.append("\"left\":" + this.left.toString() + ",");
        sb.append("\"right\":" + this.right.toString());

        sb.append("}");
        return sb.toString();
    }

    public String name;
    public AST left;
    public AST right;
}

class NullPtrAST extends AST {
    NullPtrAST() {
        super("nullptr");
        this.__type__ = "*";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\"");


        sb.append("}");
        return sb.toString();
    }
}

class ThisPtrAST extends AST {
    ThisPtrAST() {
        super("this");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\"");


        sb.append("}");
        return sb.toString();
    }
}

class ConstructionAST extends AST {
    ConstructionAST(String classname) {
        super("construction");
        this.classname = classname;
    }

    public String classname;
}

class RefAST extends AST {
    RefAST(String id) {
        super("reference");
        this.id = id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"id\":\"" + this.id + "\"");

        sb.append("}");
        return sb.toString();
    }

    public String id;
}

class FuncCallAST extends AST {
    FuncCallAST(AST func, ListAST<AST> args) {
        super("funccall");
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
        this.obj = obj;
        this.field = field;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // print kind
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"obj\":" + this.obj.toString() + ",");
        sb.append("\"field\":\"" + this.field + "\"");

        sb.append("}");
        return sb.toString();
    }

    public AST obj;
    public String field;
}

class IntAST extends AST {
    public IntAST(Integer val) {
        super("intconst");
        this.val = val;
        this.__type__ = "Int";
    }

    @Override
    public String toString() {
        return "" + val;
    }

    public Integer val;
}
class BoolAST extends AST {
    public BoolAST(Boolean val) {
        super("boolconst");
        this.val = val;
        this.__type__ = "Bool";
    }

    @Override
    public String toString() {
        return "" + val;
    }

    public Boolean val;
}
class StringAST extends AST {
    public StringAST(String val) {
        super("stringconst");
        this.val = val;
        this.__type__ = "String";
    }

    @Override
    public String toString() {
        return "\"" + val + "\"";
    }

    public String val;
}
class VoidAST extends AST {
    public static VoidAST value = new VoidAST();
    private VoidAST() {
        super("voidconst");
        this.__type__ = "Void";
    }
}