import java.util.HashMap;
import java.util.Arrays;

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
