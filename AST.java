import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

class AST {
  public String name;
  public Object val = null;
  public List<AST> children = null;

  public AST(String name, Object val, AST... children) {
    this.name = name;

    if (val != null) {
      this.val = val;
    }

    if (children != null && children.length != 0) {
      this.children = Arrays.asList(children);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");

    // print AST node type
    sb.append("\"name\":\"" + this.name + "\"");

    if (this.val != null) {
      sb.append(",");
      String valStr = this.val == null ? "null" : val.toString();
      sb.append("\"val\":\"" + valStr + "\"");
    }

    // recursively print child ASTs if there are children
    if (this.children != null) {
      sb.append(",");
      List<String> childStrs = this.children.stream().map(c -> c.toString()).collect(Collectors.toList());
      sb.append("\"children\":[");
      for (String childStr: childStrs) {
        sb.append(childStr);
        sb.append(",");
      }
      // remove the last comma
      sb.deleteCharAt(sb.length() - 1);
      sb.append("]");
    }

    sb.append("}");
    return sb.toString();
  }
}
