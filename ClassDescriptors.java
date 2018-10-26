import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.AbstractMap;

class ClassDescriptors {
    public HashSet<String> primitiveTypes = new HashSet<>();
    public HashMap<String, ClassDescriptor> classes = new HashMap<>();

    public ClassDescriptors() {
        primitiveTypes.add("Int");
        primitiveTypes.add("Bool");
        primitiveTypes.add("String");
        primitiveTypes.add("Void");
    }

    public boolean has(String type) {
        return primitiveTypes.contains(type) || classes.containsKey(type);
    }

    public boolean isPrimitive(String type) {
        return primitiveTypes.contains(type);
    }

    public void add(String type, ClassDescriptor classDescriptor) {
        classes.put(type, classDescriptor);
    }

    public ClassDescriptor getClassDescriptor(String type) {
        return classes.get(type);
    }

    public void debugPrint() {
        System.out.println("START ClassDescriptors Debug Print");
        for (Map.Entry<String, ClassDescriptor> entry : classes.entrySet()) {
            System.out.println(entry.getKey());
            entry.getValue().debugPrint();
        }
        System.out.println("END ClassDescriptors Debug Print");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ClassDescriptor> entry : classes.entrySet()) {
            sb.append("class " + entry.getKey() + " {\n");
            sb.append(entry.getValue().toString());
            sb.append("}\n");
        }
        return sb.toString();
    }
}

class ClassDescriptor {
    public String name;
    public HashMap<String, String> fields = new HashMap<>(); // maps each field name to its type
    public HashMap<String, MethodDescriptor> methods = new HashMap<>(); // maps each method name to its descriptor object

    public ClassDescriptor(String name) {
        this.name = name;
    }

    public void addField(String name, String type) {
        fields.put(name, type);
    }

    public void addMethod(String name, MethodDescriptor md) {
        methods.put(name, md);
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }
    public String getFieldType(String name) {
        return fields.get(name);
    }

    public boolean hasMethod(String name) {
        return methods.containsKey(name);
    }
    public MethodDescriptor getMethodDescriptor(String name) {
        return methods.get(name);
    }

    public void debugPrint() {
        System.out.println("START Class " + name + " Fields Debug Print");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            System.out.print(entry.getKey() + " : ");
            System.out.println(entry.getValue());
        }
        System.out.println("END Class " + name + " Fields Debug Print");
        System.out.println("START Class " + name + " Methods Debug Print");
        for (Map.Entry<String, MethodDescriptor> entry : methods.entrySet()) {
            System.out.println(entry.getKey());
            entry.getValue().debugPrint();
        }
        System.out.println("END Class " + name + " Methods Debug Print");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            sb.append("    " + entry.getKey() + " : ");
            sb.append(entry.getValue() + "\n");
        }
        return sb.toString();
    }
}

class MethodDescriptor {
    public String returntype;
    public String name;
    public ArrayList<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>(); // [(name, type)]

    public MethodDescriptor(String returntype, String name) {
        this.returntype = returntype;
        this.name = name;
    }

    public void addParam(String name, String type) {
        params.add(new AbstractMap.SimpleEntry<String,String>(name, type));
    }

    public void debugPrint() {
        System.out.println("START Method " + name + " :: ??? -> " + returntype + " Debug Print");
        int i = 0;
        for (Map.Entry<String, String> entry : params) {
            System.out.print("[" + i + "] " + entry.getKey() + " : ");
            System.out.println(entry.getValue());
            i++;
        }
        System.out.println("END Method " + name + " :: ??? -> " + returntype + " Debug Print");
    }
}