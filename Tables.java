import java.util.HashMap;

class SymbolTableEntry {
    public Type type;

    @Override
    public String toString() {
        return type.toString();
    }
}

class SymbolTable {
    public HashMap<String, SymbolTableEntry> table = new HashMap<>();

    public SymbolTableEntry getEntry(String name) {
        return table.get(name);
    }

    public void setEntry(String name, Type type) {
        SymbolTableEntry entry = new SymbolTableEntry();
        entry.type = type;
        table.put(name, entry);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (HashMap.Entry<String, SymbolTableEntry> p : this.table.entrySet()) {
            sb.append(p.getKey() + " : " + p.getValue());
            sb.append("\n");
        }

        return sb.toString();
    }
}

class SymbolTables {
    public static HashMap<String, SymbolTable> tables = new HashMap<>();
    public static SymbolTable currentTable;

    public static void create() {
        currentTable = new SymbolTable();
    }

    public static void flush(String functionName) {
        tables.put(functionName, currentTable);
        currentTable = null;
    }

    public static SymbolTable get(String functionName) {
        return tables.get(functionName);
    }

    public static void print() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Symbol Table BEGIN =====\n");

        for (HashMap.Entry<String, SymbolTable> p : tables.entrySet()) {
            sb.append("function " + p.getKey() + "\n");
            sb.append(p.getValue().toString());
            sb.append("\n");
        }

        sb.append("===== Symbol Table END =====\n");
        System.out.println(sb.toString());
    }
}

class ClassTable {
    public String name;
    public HashMap<String, Type> fields = new HashMap<>();

    public ClassTable(ClassDescriptor cdesc) {
        this.name = cdesc.name;
        for (HashMap.Entry<String, String> field : cdesc.fields.entrySet()) {
            this.fields.put(field.getKey(), Type.fromTypeString(field.getValue()));
        }
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }
    public Type getFieldType(String name) {
        return fields.get(name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (HashMap.Entry<String, Type> entry : fields.entrySet()) {
            sb.append("    " + entry.getKey() + " : ");
            sb.append(entry.getValue().toString() + "\n");
        }
        return sb.toString();
    }
}

class ClassTables {
    public static HashMap<String, ClassTable> tables = new HashMap<>();

    public static void generateFromClassDescriptors(ClassDescriptors cdescs) {
        for (HashMap.Entry<String, ClassDescriptor> entry : cdescs.classes.entrySet()) {
            tables.put(entry.getKey(), new ClassTable(entry.getValue()));
        }
    }

    public static ClassTable get(String className) {
        return tables.get(className);
    }

    public static void print() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Class Table BEGIN =====\n");

        for (HashMap.Entry<String, ClassTable> p : tables.entrySet()) {
            sb.append(p.getKey() + " {\n");
            sb.append(p.getValue().toString());
            sb.append("}\n");
        }

        sb.append("===== Class Table END =====\n");
        System.out.println(sb.toString());
    }
}