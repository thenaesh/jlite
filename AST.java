import java.util.HashMap;
import java.util.Arrays;
import java.util.stream.Collectors;

class AST {
  public String operator;
  public HashMap<String, Object> operands = new HashMap<>();

  public AST(String operator) {
    this.operator = operator;
  }

  public void addOperand(String name, Object value) {
    this.operands.put(name, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");

    // print operator
    sb.append("\"operator\":\"" + this.operator + "\",");

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
  ListAST(T item, ListAST rest) {
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
  StmtAST() {
    super("stmt");
  }
}

class ConstAST<T> extends AST {
  ConstAST(T val) {
    super("const");
    this.addOperand("val", val);
  }
}

class BinOpAST extends AST {
  BinOpAST(String operator, AST left, AST right) {
    super(operator);
    this.addOperand("left", left);
    this.addOperand("right", right);
  }
}
