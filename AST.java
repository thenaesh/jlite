import java.util.Arrays;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;

////////////////////////////////////////
///// BASE AST CLASSES & UTILITIES /////
////////////////////////////////////////

abstract class AST {
    public String kind;
    public String __type__;
    public MethodDescriptor __methoddesc__;
    public HashMap<String, Object> operands = new HashMap<>();

    public AST(String kind) {
        this.kind = kind;
        this.__type__ = "Void";
        this.__methoddesc__ = null;
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

    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        return lenv;
    }

    public ArrayList<IR3> genIR() {
        return new ArrayList<>();
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

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        throw new TypeCheckingException("Should not be runnign type checker on ListAST node, which should not exist in the AST in the first place!");
    }

    public final boolean isEmpty;
    private T item;
    private ListAST<T> rest;
}

////////////////////////////////
///// CONCRETE AST CLASSES /////
////////////////////////////////

class ProgramAST extends AST {
    ProgramAST(ClassAST mainclass, ListAST<ClassAST> classes) throws DistinctNamesCheckingException, TypeCheckingException {
        super("__program__");
        this.mainClass = mainclass;
        this.classes = classes.convertToArrayList();

        this.distinctNamesCheck();

        ClassDescriptors classDescriptors = buildClassDescriptors();
        LocalEnvironment localEnvironment = new LocalEnvironment();
        this.typeCheck(classDescriptors, localEnvironment);

        ArrayList<IR3> irs = this.genIR();

        SymbolTables.print();
        ClassTables.print();
        System.out.println("===== IR3 BEGIN =====\n");
        for (IR3 ir : irs) System.out.println(ir.toString());
        System.out.println("===== IR3 END =====\n");
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

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        this.mainClass.typeCheck(cdesc, lenv);
        for (ClassAST cls : this.classes) cls.typeCheck(cdesc, lenv);
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        irs.addAll(this.mainClass.genIR());
        for (ClassAST cls : this.classes) irs.addAll(cls.genIR());
        return irs;
    }

    private ClassDescriptors buildClassDescriptors() {
        ClassDescriptors classDescriptors = new ClassDescriptors();

        ArrayList<ClassAST> classesToCheck = new ArrayList<>();
        classesToCheck.add(this.mainClass);
        classesToCheck.addAll(this.classes);

        for (ClassAST cls : classesToCheck) {
            ClassDescriptor clsDesc = new ClassDescriptor(cls.name);

            for (VarDeclAST field : cls.members) {
                clsDesc.addField(field.name, field.type);
            }

            for (FuncDeclAST method : cls.methods) {
                MethodDescriptor mdDesc = new MethodDescriptor(method.returntype, method.name, cls.name);
                for (VarDeclAST param : method.params) {
                    mdDesc.addParam(param.name, param.type);
                }
                clsDesc.addMethod(method.name, mdDesc);
            }

            classDescriptors.add(cls.name, clsDesc);
        }

        ClassTables.generateFromClassDescriptors(classDescriptors);
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

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        LocalEnvironment lenvNew = new LocalEnvironment(lenv);
        lenvNew.currentClass = this.name;
        for (FuncDeclAST method : this.methods) method.typeCheck(cdesc, lenvNew);
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        for (FuncDeclAST method : this.methods) {
            method.classname = this.name;
            irs.addAll(method.genIR());
        }
        return irs;
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

    public String augmentedName() {
        return this.classname + "_" + this.name;
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

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        LocalEnvironment lenvNew = new LocalEnvironment(lenv);
        for (VarDeclAST param : this.params) lenvNew.extend(param.name, param.type);
        lenvNew.retType = returntype;
        lenvNew.methodName = name;
        this.body.typeCheck(cdesc, lenvNew);
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();

        SymbolTables.create();
        for (VarDeclAST var : this.body.vardecls) SymbolTables.currentTable.setEntry(var.name, Type.fromTypeString(var.type));
        for (VarDeclAST param : this.params) SymbolTables.currentTable.setEntry(param.name, Type.fromTypeString(param.type));
        SymbolTables.currentTable.setEntry("this", Type.fromTypeString(this.classname));
        ArrayList<IR3> bodyirs = body.genIR();
        SymbolTables.flush(augmentedName());

        FunctionStartIR3 funcStart = new FunctionStartIR3(returntype, augmentedName());
        FunctionEndIR3 funcEnd = new FunctionEndIR3();

        funcStart.addParam("this", this.classname);
        for (VarDeclAST param : params) {
            funcStart.addParam(param.name, param.type);
        }

        irs.add(funcStart);
        irs.addAll(bodyirs);
        irs.add(funcEnd);
        return irs;
    }

    public String classname; // must initialise before calling genIR
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

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        LocalEnvironment lenvNew = new LocalEnvironment(lenv);

        for (VarDeclAST var : this.vardecls) lenvNew.extend(var.name, var.type);
        for (StmtAST stmt : this.stmts) {
            stmt.typeCheck(cdesc, lenvNew);
            if (stmt.kind.equals("returnstmt") && !stmt.__type__.equals(lenvNew.retType)) {
                throw new TypeCheckingException("Expected return type of " + lenvNew.retType + " in method " + lenvNew.currentClass + "." + lenvNew.methodName + " but encountered " + stmt.__type__);
            }
        }
        return lenvNew;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        for (StmtAST stmt : this.stmts) {
            irs.addAll(stmt.genIR());
        }
        return irs;
    }

    public ArrayList<VarDeclAST> vardecls; // should only exist in function bodies
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

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        assignee.typeCheck(cdesc, lenv);
        val.typeCheck(cdesc, lenv);
        if (!assignee.__type__.equals(val.__type__)) {
            throw new TypeCheckingException("Assigning value of type " + val.__type__ + " to variable of type " + assignee.__type__);
        }
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        ArrayList<IR3> valirs = val.genIR();
        irs.addAll(valirs);
        if (assignee.kind.equals("reference")) {
            // doesn't make sense to store raw id in tmp variable
            irs.add(new AssignmentIR3(((RefAST)assignee).id, IR3.extractLvalue(valirs)));
            return irs;
        }
        if (assignee.kind.equals("memberaccess") && ((MemberAccessAST)assignee).obj.kind.equals("reference")) {
            // if indexing an object directly without any additional indirection, shortcut
            MemberAccessAST assigneeAfterCast = (MemberAccessAST)assignee;
            irs.add(new MemberAssignmentIR3(((RefAST)assigneeAfterCast.obj).id, assigneeAfterCast.field, IR3.extractLvalue(valirs)));
            return irs;
        }
        ArrayList<IR3> assigneeirs = assignee.genIR();
        irs.addAll(assigneeirs);
        irs.add(new AssignmentIR3(IR3.extractLvalue(assigneeirs), IR3.extractLvalue(valirs)));
        return irs;
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

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        retval.typeCheck(cdesc, lenv);
        this.__type__ = retval.__type__;
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        if (retval == null || retval.__type__.equals("Void")) {
            irs.add(new ReturnIR3());
            return irs;
        }
        ArrayList<IR3> retvalirs = retval.genIR();
        irs.addAll(retvalirs);
        irs.add(new ReturnIR3(IR3.extractLvalue(retvalirs)));
        return irs;
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

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        condition.typeCheck(cdesc, lenv);
        if (!condition.__type__.equals("Bool")) throw new TypeCheckingException("If stmt condition must be Bool, but encountered " + condition.__type__);
        successblock.typeCheck(cdesc, lenv);
        failureblock.typeCheck(cdesc, lenv);
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();

        ArrayList<IR3> conditionirs = condition.genIR();
        ArrayList<IR3> successirs = successblock.genIR();
        ArrayList<IR3> failureirs = failureblock.genIR();

        LabelIR3 successlabelir = new LabelIR3();
        LabelIR3 endlabelir = new LabelIR3();
        GotoIR3 successgotoir = new GotoIR3(successlabelir.label, IR3.extractLvalue(conditionirs));
        GotoIR3 endgotoir = new GotoIR3(endlabelir.label);

        irs.addAll(conditionirs);
        irs.add(successgotoir);
        irs.addAll(failureirs);
        irs.add(endgotoir);
        irs.add(successlabelir);
        irs.addAll(successirs);
        irs.add(endlabelir);

        return irs;
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

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        condition.typeCheck(cdesc, lenv);
        if (!condition.__type__.equals("Bool")) throw new TypeCheckingException("While loop condition must be Bool, but encountered " + condition.__type__);
        block.typeCheck(cdesc, lenv);
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();

        ArrayList<IR3> conditionirs = condition.genIR();
        ArrayList<IR3> blockirs = block.genIR();

        LabelIR3 startlabelir = new LabelIR3();
        LabelIR3 successlabelir = new LabelIR3();
        LabelIR3 endlabelir = new LabelIR3();
        GotoIR3 successgotoir = new GotoIR3(successlabelir.label, IR3.extractLvalue(conditionirs));
        GotoIR3 endgotoir = new GotoIR3(endlabelir.label);
        GotoIR3 startgotoir = new GotoIR3(startlabelir.label);

        irs.add(startlabelir);
        irs.addAll(conditionirs);
        irs.add(successgotoir);
        irs.add(endgotoir);
        irs.add(successlabelir);
        irs.addAll(blockirs);
        irs.add(startgotoir);
        irs.add(endlabelir);

        return irs;
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

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        output.typeCheck(cdesc, lenv);
        if (!output.__type__.equals("String")) {
            throw new TypeCheckingException("Can only print a String, not " + output.__type__);
        }
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        ArrayList<IR3> outputir = output.genIR();
        PrintIR3 printir = new PrintIR3(IR3.extractLvalue(outputir));
        irs.addAll(outputir);
        irs.add(printir);
        return irs;
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

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        input.typeCheck(cdesc, lenv);
        if (!input.__type__.equals("String")) {
            throw new TypeCheckingException("Can only read into a String, not " + input.__type__);
        }
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        irs.add(new ReadIR3(input.id));
        return irs;
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
        sb.append("\"__type__\":\"" + this.__type__ + "\",");
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"name\":\"" + this.name + "\",");
        sb.append("\"operand\":" + this.operand.toString());

        sb.append("}");
        return sb.toString();
    }

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        operand.typeCheck(cdesc, lenv);
        if (name.equals("!") && !operand.__type__.equals("Bool")) {
            throw new TypeCheckingException("Unary operator ! can only be used with Bool, not " + operand.__type__);
        }
        if (name.equals("-") && !operand.__type__.equals("Int")) {
            throw new TypeCheckingException("Unary operator - can only be used with Int, not " + operand.__type__);
        }
        if (name.equals("!")) this.__type__ = "Bool";
        if (name.equals("-")) this.__type__ = "Int";
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        ArrayList<IR3> operandirs = operand.genIR();
        irs.addAll(operandirs);
        irs.add(new UnOpIR3(name, IR3.extractLvalue(operandirs)));
        return irs;
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
        sb.append("\"__type__\":\"" + this.__type__ + "\",");
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"name\":\"" + this.name + "\",");
        sb.append("\"left\":" + this.left.toString() + ",");
        sb.append("\"right\":" + this.right.toString());

        sb.append("}");
        return sb.toString();
    }

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        boolean isIntExp = name.equals("+") || name.equals("-") || name.equals("*") || name.equals("/");
        boolean isIntCmp = name.equals("<") || name.equals(">") || name.equals("<=") || name.equals(">=") || name.equals("==") || name.equals("!=");
        left.typeCheck(cdesc, lenv);
        right.typeCheck(cdesc, lenv);
        if (isIntExp || isIntCmp) {
            if (!left.__type__.equals("Int") || !right.__type__.equals("Int")) {
                throw new TypeCheckingException("Binary operation " + name + " can only be used on Ints");
            }
            this.__type__ = isIntCmp ? "Bool" : "Int";
        } else {
            if (!left.__type__.equals("Bool") || !right.__type__.equals("Bool")) {
                throw new TypeCheckingException("Binary operation " + name + " can only be used on Bools");
            }
            this.__type__ = "Bool";
        }
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        ArrayList<IR3> leftirs = left.genIR();
        ArrayList<IR3> rightirs = right.genIR();
        irs.addAll(leftirs);
        irs.addAll(rightirs);
        irs.add(new BinOpIR3(name, IR3.extractLvalue(leftirs), IR3.extractLvalue(rightirs)));
        return irs;
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
        sb.append("\"__type__\":\"" + this.__type__ + "\",");
        sb.append("\"kind\":\"" + this.kind + "\"");


        sb.append("}");
        return sb.toString();
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        irs.add(new AssignmentIR3(IR3.mkVar(Type.fromTypeString(this.__type__)), "this"));
        return irs;
    }

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) {
        this.__type__ = lenv.currentClass;
        return lenv;
    }
}

class ConstructionAST extends AST {
    ConstructionAST(String classname) {
        super("construction");
        this.classname = classname;
    }

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) {
        this.__type__ = classname;
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        irs.add(new ConstructionIR3(classname));
        return irs;
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
        sb.append("\"__type__\":\"" + this.__type__ + "\",");
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"id\":\"" + this.id + "\"");

        sb.append("}");
        return sb.toString();
    }

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        if (!lenv.contains(id)) throw new TypeCheckingException(id + " not found");
        this.__type__ = lenv.getType(id);
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        irs.add(new AssignmentIR3(IR3.mkVar(Type.fromTypeString(this.__type__)), id));
        return irs;
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
        sb.append("\"__type__\":\"" + this.__type__ + "\",");
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

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        func.typeCheck(cdesc, lenv);
        if (!func.__type__.equals("FUNCTION")) throw new TypeCheckingException("Can only call a function!");
        MethodDescriptor md = func.__methoddesc__;
        if (args.size() != md.params.size()) throw new TypeCheckingException("Wrong number of arguments in function " + md.name);
        for (int i=0; i<md.params.size(); i++) {
            AST arg = args.get(i);
            SimpleEntry<String, String> param = md.params.get(i);
            arg.typeCheck(cdesc, lenv);
            if (!arg.__type__.equals(param.getValue())) throw new TypeCheckingException("Parameter type mismatch for parameter " + param.getKey() + " in function " + md.name);
        }
        this.__type__ = md.returntype;
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        ArrayList<String> argVarNames = new ArrayList<>();

        // put the object name (this) into args
        ArrayList<IR3> funcirs = ((MemberAccessAST)func).obj.genIR();
        irs.addAll(funcirs);
        argVarNames.add(IR3.extractLvalue(funcirs));

        for (AST arg : args) {
            ArrayList<IR3> argirs = arg.genIR();
            String argVarName = IR3.extractLvalue(argirs);
            irs.addAll(argirs);
            argVarNames.add(argVarName);
        }

        irs.add(new FunctionCallIR3(augmentedName(), returnType(), argVarNames));
        return irs;
    }

    public String augmentedName() {
        return func.__methoddesc__.classname + "_" + func.__methoddesc__.name;
    }

    public Type returnType() {
        return Type.fromTypeString(func.__methoddesc__.returntype);
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
        sb.append("\"__type__\":\"" + this.__type__ + "\",");
        sb.append("\"kind\":\"" + this.kind + "\",");

        sb.append("\"obj\":" + this.obj.toString() + ",");
        sb.append("\"field\":\"" + this.field + "\"");

        sb.append("}");
        return sb.toString();
    }

    @Override
    public LocalEnvironment typeCheck(ClassDescriptors cdesc, LocalEnvironment lenv) throws TypeCheckingException {
        obj.typeCheck(cdesc, lenv);
        if (cdesc.isPrimitive(obj.__type__)) throw new TypeCheckingException("Cannot perform member access on primitive value!");
        if (!cdesc.has(obj.__type__)) throw new TypeCheckingException("No such type: " + obj.__type__);

        ClassDescriptor cd = cdesc.getClassDescriptor(obj.__type__);
        if (cd == null) throw new TypeCheckingException("No such class!");
        if (cd.hasField(field)) {
            this.__type__ = cd.getFieldType(field);
        } else if (cd.hasMethod(field)) {
            this.__type__ = "FUNCTION";
            this.__methoddesc__ = cd.getMethodDescriptor(field);
        } else throw new TypeCheckingException("No such member!");
        return lenv;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        ArrayList<IR3> objirs = obj.genIR();
        irs.addAll(objirs);
        irs.add(new MemberAccessIR3(IR3.extractLvalue(objirs), field));
        return irs;
    }

    public AST obj;
    public String field;
}

class IntAST extends AST {
    public IntAST(Integer val) {
        super("const");
        this.val = val;
        this.__type__ = "Int";
    }

    @Override
    public String toString() {
        return "" + val;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        irs.add(new IntIR3(val));
        return irs;
    }

    public Integer val;
}
class BoolAST extends AST {
    public BoolAST(Boolean val) {
        super("const");
        this.val = val;
        this.__type__ = "Bool";
    }

    @Override
    public String toString() {
        return "" + val;
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        irs.add(new BoolIR3(val));
        return irs;
    }

    public Boolean val;
}
class StringAST extends AST {
    public StringAST(String val) {
        super("const");
        this.val = val;
        this.__type__ = "String";
    }

    @Override
    public String toString() {
        return "\"" + val + "\"";
    }

    @Override
    public ArrayList<IR3> genIR() {
        ArrayList<IR3> irs = new ArrayList<>();
        irs.add(new StringIR3(val));
        return irs;
    }

    public String val;
}
class VoidAST extends AST {
    public static VoidAST value = new VoidAST();
    private VoidAST() {
        super("const");
        this.__type__ = "Void";
    }
}