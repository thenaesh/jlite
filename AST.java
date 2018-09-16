import java.util.HashMap;
import java.util.Arrays;
import java.util.stream.Collectors;

class AST {
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
}

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

class ListAST<T> extends AST {
  ListAST() {
    super("listnode");
  }
  ListAST(T item, ListAST<T> rest) {
    this();
    this.addOperand("item", item);
    this.addOperand("rest", rest);
  }
  ListAST(T item) {
    this(item, new ListAST<T>());
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
class ReturnStmtAST extends StmtAST {
  ReturnStmtAST() {
    super("return");
  }
  ReturnStmtAST(Object retval) {
    this();
    this.addOperand("retval", retval);
  }
}

class UnOpAST extends AST {
  UnOpAST(String name, Object operand) {
    super("unaryoperation");
    this.addOperand("operator", "\"" + name + "\"");
    this.addOperand("operand", operand);
  }
}

class BinOpAST extends AST {
  BinOpAST(String name, Object left, Object right) {
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
  FuncCallAST(Object func, ListAST<AST> args) {
    super("funccall");
    this.addOperand("function", func);
    this.addOperand("args", args);
  }
}

class MemberAccessAST extends AST {
  MemberAccessAST(Object obj, String field) {
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
