class ProgramAST extends AST {
  ProgramAST(ClassAST mainclass, ListAST<ClassAST> classes) {
    super("__program__");
    this.addOperand("mainclass", mainclass);
    this.addOperand("classes", classes);
  }
}

class ClassAST extends AST {
  ClassAST(String name, ListAST<VarDeclAST> members, ListAST<FuncDeclAST> methods) {
    super("classdecl");
    this.addOperand("name", "\"" + name + "\"");
    this.addOperand("members", members);
    this.addOperand("methods", methods);
  }
}

class FuncDeclAST extends AST {
  FuncDeclAST(String returntype, String name, ListAST<VarDeclAST> params, BlockAST body) {
    super("funcdecl");
    this.addOperand("returntype", "\"" + returntype + "\"");
    this.addOperand("name", "\"" + name + "\"");
    this.addOperand("params", params);
    this.addOperand("body", body);
  }
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