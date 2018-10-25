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

    public void add(String type, ClassDescriptor classDescriptor) {
        classes.put(type, classDescriptor);
    }

    public void debugPrint() {
        System.out.println("START ClassDescriptors Debug Print");
        for (Map.Entry<String, ClassDescriptor> entry : classes.entrySet()) {
            System.out.println(entry.getKey());
            entry.getValue().debugPrint();
        }
        System.out.println("END ClassDescriptors Debug Print");
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