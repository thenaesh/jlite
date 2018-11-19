abstract class Type {
    public abstract boolean isValueType();
    public abstract boolean isVoid();

    public static Type JLINT = new JLInt();
    public static Type JLFLOAT = new JLFloat();
    public static Type JLBOOL = new JLBool();
    public static Type JLVOID = new JLVoid();
    public static Type JLSTRING = new JLString();

    public static Type fromTypeString(String str) {
        if (str.equals("Void")) return JLVOID;
        if (str.equals("Int")) return JLINT;
        if (str.equals("Float")) return JLFLOAT;
        if (str.equals("Bool")) return JLBOOL;
        if (str.equals("String")) return JLSTRING;
        return new RefType(str);
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass().equals(this.getClass());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().toString());
        sb.delete(0, 6);
        return sb.toString();
    }
}


class ValueType extends Type {
    @Override
    public boolean isValueType() {
        return true;
    }

    @Override
    public boolean isVoid() {
        return false;
    }
}

class RefType extends Type {
    public String classname;

    public RefType(String classname) {
        this.classname = classname;
    }

    @Override
    public boolean isValueType() {
        return false;
    }

    @Override
    public boolean isVoid() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof RefType) && this.classname.equals(((RefType)o).classname);
    }

    @Override
    public String toString() {
        return this.classname;
    }
}


class JLInt extends ValueType {
}
class JLFloat extends ValueType {
}
class JLBool extends ValueType {
}

class JLVoid extends Type {
    @Override
    public boolean isVoid() {
        return true;
    }

    @Override
    public boolean isValueType() {
        return false;
    }
}

class JLString extends Type {
    @Override
    public boolean isVoid() {
        return false;
    }

    @Override
    public boolean isValueType() {
        return false;
    }
}
