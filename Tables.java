import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

class SymbolTableEntry {
    public Type type;
    public Integer offset; // stack pointer offset
    public String register;

    public boolean isRegisterAllocated() {
        return register != null;
    }

    public boolean isStackAllocated() {
        return offset != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.toString());
        if (isStackAllocated()) sb.append(" | stack offset = " + offset);
        if (isRegisterAllocated()) sb.append(" | register = %" + register);
        return sb.toString();
    }
}

class SymbolTable {
    public LinkedHashMap<String, SymbolTableEntry> locals = new LinkedHashMap<>();
    public LinkedHashMap<String, SymbolTableEntry> params = new LinkedHashMap<>();
    public Integer size = 0; // depends on whether there are other things on the stack before local variables

    public SymbolTableEntry getEntry(String name) {
        return locals.containsKey(name) ? locals.get(name) : params.get(name); // local vars shadow params
    }

    public void setLocal(String name, Type type) {
        SymbolTableEntry entry = new SymbolTableEntry();
        entry.type = type;
        locals.put(name, entry);
    }

    public void removeLocal(String name) {
        locals.remove(name);
    }

    public void setParam(String name, Type type) {
        SymbolTableEntry entry = new SymbolTableEntry();
        entry.type = type;
        params.put(name, entry);
    }

    public void generateOffsetsAndRegisters() {
        this.generateParameterOffsetsAndRegisters();
        this.generateLocalVariableOffsetsAndRegisters();
    }

    private void generateParameterOffsetsAndRegisters() {
        int count = 0;
        for (Map.Entry<String, SymbolTableEntry> p : this.params.entrySet()) {
            SymbolTableEntry entry = p.getValue();
            if (count++ < 4) {
                entry.register = "a" + count;
            } else {
                break; // TODO: SUPPORT MORE THAN 4 PARAMETERS!
                // entry.offset = size;
                // size += entry.type.width();
            }
        }
    }

    private void generateLocalVariableOffsetsAndRegisters() {
        for (Map.Entry<String, SymbolTableEntry> p : this.locals.entrySet()) {
            SymbolTableEntry entry = p.getValue();
            entry.offset = size;
            size += entry.type.width();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("    Parameters\n");
        for (Map.Entry<String, SymbolTableEntry> p : this.params.entrySet()) {
            String name = p.getKey();
            SymbolTableEntry entry = p.getValue();
            sb.append("        ");
            sb.append(name + " : " + entry.toString());
            sb.append("\n");
        }

        sb.append("    Local Variables\n");
        for (Map.Entry<String, SymbolTableEntry> p : this.locals.entrySet()) {
            String name = p.getKey();
            SymbolTableEntry entry = p.getValue();
            sb.append("        ");
            sb.append(name + " : " + entry.toString());
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
        currentTable.generateOffsetsAndRegisters();
        tables.put(functionName, currentTable);
        currentTable = null;
    }

    public static SymbolTable get(String functionName) {
        return tables.get(functionName);
    }

    public static void print() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Symbol Tables BEGIN =====\n");

        for (HashMap.Entry<String, SymbolTable> p : tables.entrySet()) {
            sb.append("size = " + p.getValue().size + "\n");
            sb.append("function " + p.getKey() + "\n");
            sb.append(p.getValue().toString());
            sb.append("\n");
        }

        sb.append("===== Symbol Tables END =====\n");
        System.out.println(sb.toString());
    }
}

class ClassTableEntry {
    Type type;
    Integer offset; // offset from object start address in heap

    public ClassTableEntry(Type type, Integer offset) {
        this.type = type;
        this.offset = offset;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.toString());
        sb.append(" | heap offset = " + offset);
        return sb.toString();
    }
}

class ClassTable {
    public String name;
    public HashMap<String, ClassTableEntry> fields = new HashMap<>();
    public Integer size = 0;

    public ClassTable(ClassDescriptor cdesc) {
        this.name = cdesc.name;
        for (Map.Entry<String, String> field : cdesc.fields.entrySet()) {
            Type type = Type.fromTypeString(field.getValue());
            Integer offset = size;
            size += type.width();

            ClassTableEntry entry = new ClassTableEntry(type, offset);
            this.fields.put(field.getKey(), entry);
        }
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }
    public Type getFieldType(String name) {
        return fields.get(name).type;
    }
    
    public Integer getFieldOffset(String name) {
        return fields.get(name).offset;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ClassTableEntry> entry : fields.entrySet()) {
            sb.append("    " + entry.getKey() + " : ");
            sb.append(entry.getValue().toString() + "\n");
        }
        return sb.toString();
    }
}

class ClassTables {
    public static HashMap<String, ClassTable> tables = new HashMap<>();
    public static String nameOfMainClass = "Main";

    public static void generateFromClassDescriptors(ClassDescriptors cdescs) {
        for (Map.Entry<String, ClassDescriptor> entry : cdescs.classes.entrySet()) {
            if (entry.getValue().hasMethod("main")) nameOfMainClass = entry.getKey();
            tables.put(entry.getKey(), new ClassTable(entry.getValue()));
        }
    }

    public static ClassTable get(String className) {
        return tables.get(className);
    }
    public static ClassTable get(Type classNameType) {
        return tables.get(classNameType.toString());
    }

    public static void print() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Class Table BEGIN =====\n");

        for (Map.Entry<String, ClassTable> p : tables.entrySet()) {
            sb.append(p.getKey() + " size = " + p.getValue().size + " {\n");
            sb.append(p.getValue().toString());
            sb.append("}\n");
        }

        sb.append("===== Class Table END =====\n");
        System.out.println(sb.toString());
    }
}

class DataTableEntry {
    String directive;
    String item;

    public DataTableEntry(String directive, String item) {
        this.directive = directive;
        this.item = item;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (char c : item.toCharArray()) {
            if (c == '\n') {
                sb.append("\\n");
            } else {
                sb.append(c);
            }
        }
        return directive + " \"" + sb.toString() + "\\n\"";
    }
}

class DataTable {
    public static HashMap<Integer, DataTableEntry> data = new HashMap<>();

    public static void create(Integer label, DataTableEntry entry) {
        data.put(label, entry);
    }

    public static boolean has(Integer label) {
        return data.containsKey(label);
    }

    public static DataTableEntry get(Integer label) {
        return data.get(label);
    }

    public static void print() {
        System.out.println(getTableString());
    }

    public static String getTableString() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Integer, DataTableEntry> p : data.entrySet()) {
            sb.append("L" + p.getKey() + ":\n    ");
            sb.append(p.getValue().toString());
            sb.append("\n");
        }

        return sb.toString();
    }

    public static Integer PRINT_INT_FORMAT_STR_LABEL;
    public static void init() {
        PRINT_INT_FORMAT_STR_LABEL = IR3.mkLabel();
        DataTable.create(PRINT_INT_FORMAT_STR_LABEL, new DataTableEntry(".asciz", "%i"));
    }
}