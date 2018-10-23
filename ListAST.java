import java.util.ArrayList;

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

    public final boolean isEmpty;
    private T item;
    private ListAST<T> rest;
}
