import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SymbolTable {
    private static final int intOffset = 4;
    private static final int booleanOffset = 1;
    private static final int pointerOffset = 8;
    private static final Set<String> primitiveTypes = Set.of("int", "boolean", "int[]");


    static Map<String, ClassContents> classMap = new HashMap<>();  //class name -> ClassContents
    ClassContents classScope;
    MethodContents methodScope;

    public void insertClass(ClassContents c) throws PopulatorException {
        if (classMap.containsKey(c.getType())) {
            throw new PopulatorException("Class with identifier \"" + c.getType() + "\" already exists.");
        } else {
            classMap.put(c.getType(), c);
        }
    }

    public void insertVariable(String varId, String varType) throws PopulatorException {
        if (methodScope != null) {
            methodScope.insertVariable(varId, varType);
        } else if (methodScope == null && classScope != null) {
            classScope.insertField(varId, varType);
        } else {
            throw new PopulatorException("Variable Declaration \"" + varId + "\" without class/method scope.");
        }
    }

    void insertMethod(MethodContents method) throws PopulatorException {
        if(classScope != null) {
            classScope.insertMethod(method);
        }
        else {
            throw new PopulatorException("Method Delcaration outside of a class scope.");
        }
    }

    /**
     * Check if expr represents a defined type. Either a primtive or a Class.
     * @param expr
     * @return
     */
    public boolean isValidType(String expr) {
        if(primitiveTypes.contains(expr) || classMap.containsKey(expr)) {
            return true;
        }
        return false;
    }

    public boolean isPrimitive(String type) {
        if(primitiveTypes.contains(type)) {
            return true;
        }
        return false;
    }

    /*****************  Getters  ************************/
    public ClassContents getClassScope() {
        return classScope;
    }

    public MethodContents getMethodScope() {
        return methodScope;
    }

    public Map<String, ClassContents> getClassMap() {
        return classMap;
    }

    /**
     * Shortcut for getClassMap().get(classType) that also makes sure to check if it exists before getting
     * @param classType
     * @return ClassContents or null if undefined
     */
    public ClassContents getClassDefinition(String classType) {
        if(classMap.containsKey(classType)){
            return classMap.get(classType);
        }
        else {
            return null;
        }
    }

    /**
     * Look for Method Definition of methodId in this scope
     * @param methodId
     * @return
     */
    public MethodContents getMethodFromScope(String methodId) throws PopulatorException {
        if(classScope == null){
            return null;
        }
        return classScope.getInheritedMethod(methodId);
    }

    /**
     * Look for variable/class declaration in this scope (method,class,superClasses)
     * @param varId
     * @return the type of the variable if found, else null
     */
    public String getIdTypeFromScope(String varId) {
        //check if in current method scope
        if(methodScope != null) {
            String type = methodScope.getVariableType(varId);
            if(type != null) {
                return type;
            }
        }
        //check if this is a new allocation, then its ok if its not in the scope yet. Just return the type.
        if(varId.endsWith("()")) {
            return varId.replaceAll("[\\(\\)]","");   //remove "()"
        }
        //if not then check the class scope and its inheritance
        if(classScope != null) {
            return classScope.getInheritedField(varId);
        }
        return null;
    }

    /***************  Methods for Scope control  **********/
    public void setScope(ClassContents c) {
        classScope = c;
    }

    public void setScope(MethodContents m) {
        methodScope = m;
    }

    public void resetClassScope() {
        classScope = null;
    }

    public void resetMethodScope() {
        methodScope = null;
    }

    /***********  Methods for printing offsets  **************************************************/
    void printOffsets() {
        for (String classType : classMap.keySet()) {
            classMap.get(classType).saveOffsets();
            classMap.get(classType).printOffsets();
            System.out.println("");
        }
    }

    static int findFieldOffset(String type) {
        switch (type) {
            case "int":
                return 4;
            case "boolean":
                return 1;
            case "int[]":
                return 8;
            default: {
                ClassContents clazz = classMap.get(type);
                return clazz.getFieldOffset();
            }
        }
    }
}