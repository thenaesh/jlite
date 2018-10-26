import java.util.HashMap;
import java.util.Map;

class LocalEnvironment {
    public HashMap<String, String> vars = new HashMap<>(); // maps variable identifier to type
    public String currentClass = "NONE";
    public String retType = "Void";
    public String methodName = "";

    public LocalEnvironment() {
    }
    public LocalEnvironment(LocalEnvironment oldEnv) {
        for (Map.Entry<String, String> entry : oldEnv.vars.entrySet()) {
            this.vars.put(entry.getKey(), entry.getValue());
        }
        this.currentClass = oldEnv.currentClass;
        this.retType = oldEnv.retType;
        this.methodName = oldEnv.methodName;
    }

    public void extend(String name, String type) {
        vars.put(name, type);
    }

    public boolean contains(String name) {
        return vars.containsKey(name);
    }

    public String getType(String name) {
        return vars.get(name);
    }
}