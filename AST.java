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
  ProgramAST(AST start) {
    super("__program__");
    this.addOperand("start", start);
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
