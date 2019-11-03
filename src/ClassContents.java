import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class ClassContents {
    private String type;  //name of this class
    private ClassContents superClass;

    private Map<String, String> fieldMap = new LinkedHashMap<>();   //field identifier -> type
    private Map<String, MethodContents> methodMap = new LinkedHashMap<>(); //method identifier -> method

    private Integer fieldOffset = null;
    private Integer methodOffset = null;
    private Map<String,Integer> fieldOffsetMap = new LinkedHashMap<>(); //fieldId -> fieldOffset
    private Map<String,Integer> methodOffsetMap = new LinkedHashMap<>(); //methodId -> fieldOffset


    ClassContents(String type) {
        this.type = type;
    }

    ClassContents(String type, ClassContents superClass) {
        this.type = type;
        this.superClass = superClass;
    }

    /** Insert a field in fieldMap. Check that the fieldMap doesnt have this field already
     * @param fieldId
     * @param fieldType
     * @throws PopulatorException
     */
    public void insertField(String fieldId, String fieldType) throws PopulatorException {
        if (fieldMap.containsKey(fieldId)) {
            throw new PopulatorException("Class field with identifier \"" + fieldId + "\" already exists in " + type + "'s scope.");
        } else {
            fieldMap.put(fieldId, fieldType);
        }
    }

    /**Insert method in this class's methodMap. Check that it doesn't already exist.
     * NOTE: since we do not have the parameterList here yet, the polymorphic check will be done later. It is the
     * programmers responsibility to call the polymorphic check function for a method.
     * @param method
     * @throws PopulatorException
     */
    public void insertMethod(MethodContents method) throws PopulatorException {
        if (methodMap.containsKey(method.getId())) {
            throw new PopulatorException("Method with identifier \"" + method.getId() + "\" already exists in " + type + "'s scope.");
        } else {
            methodMap.put(method.getId(), method);
        }
    }

    /*********************  Getters  ********************************/
    public String getType() {
        return type;
    }

    public Map<String, String> getFieldMap() {
        return fieldMap;
    }

    public Map<String, MethodContents> getMethodMap() {
        return methodMap;
    }

    public ClassContents getSuper() {
        return superClass;
    }

    /**
     * Check if this class has method (locally, dont look to super classes)
     * @param methodId
     * @return
     */
    public MethodContents getMethod(String methodId) {
        if(methodMap.containsKey(methodId)) {
            return methodMap.get(methodId); //local method
        }
        else {
            return null;
        }
    }

    /**
     * Check if this class has field (locally, dont look to super classes)
     * @param fieldId
     * @return field type if found, null else
     */
    public String getField(String fieldId) {
        if(fieldMap.containsKey(fieldId)) {
            return  fieldMap.get(fieldId);
        }
        else {
            return null;
        }
    }

    /** Climb the inheritance hierarchy recursively and find the first occurrence of method (if there is any occurrence)
     * @param methodId
     * @return
     */
    public MethodContents getInheritedMethod(String methodId) {
        //first try ang get method locally
        MethodContents method = getMethod(methodId);
        if(method != null) {
            return method;
        }
        //if its not here locally, then look into inheritance
        if (superClass != null) {
            MethodContents superMethod = superClass.getMethod(methodId);
            if (superMethod != null) {
                return superMethod; //found first occurrence
            }
            else {
                return superClass.getInheritedMethod(methodId);    //call recursively until the top of inheritance hierarchy
            }
        }
        return null;   //no super class to inherit from
    }

    /** Climb the inheritance hierarchy recursively and find the first occurrence of field (if there is any occurrence)
     * @param fieldId
     * @return field type if found, else null
     */
    public String getInheritedField(String fieldId) {
        //first try and find the field locally
        String fieldType = getField(fieldId);
        if(fieldType != null) {
            return fieldType;
        }
        //if its not here locally, then look into inheritance
        if (superClass != null) {
            String superFieldType = superClass.getField(fieldId);
            if (superFieldType!= null) {
                return superFieldType; //found first occurrence
            }
            else {
                return superClass.getInheritedField(fieldId);    //call recursively until the top of inheritance hierarchy
            }
        }
        return null;   //no super class to inherit from
    }

    /**
     * Climb hierarchy to find if this class inherits from superType.
     * @param superType
     * @return True if it inherits from superType.
     */
    boolean isOfSuperType(String superType) {
        if(superClass != null) {
            if(superClass.getType() == superType) {
                return true;    //found it
            }
            else {
                return superClass.isOfSuperType(superType);    //continue climbing
            }
        }
        return false;
    }

    /**
     * total numbers of methods, both local and inherited
     * @return
     */
    public int getNumOfMethods() {
        if(superClass != null)
            return methodMap.size()+superClass.getNumOfMethods();
        return methodMap.size();
    }


    /**
     *
     * @return List of all methods, local or inherited. Starting from the top of inhreitance first.
     */
    public List<MethodContents> getAllMethods() {
        List<MethodContents> list;
        if(superClass == null) {
            //base case where there is no super class to get methods from
            list = new LinkedList<>();
        }
        else {
            //get parent methods first
            list = superClass.getAllMethods();
        }
        //add local methods
        for(Map.Entry<String, MethodContents> entry : methodMap.entrySet()) {
            list.add(entry.getValue());
        }
        return list;
    }


    /****************  Methods for offsets  ************************/
    public Integer getFieldOffset() {
        if(fieldOffset == null) {
            fieldOffset = calcFieldOffset();
        }
        return fieldOffset;
    }

    public Integer getMethodOffset() {
        if(methodOffset == null) {
            methodOffset = calcMethodOffset();
        }
        return methodOffset;
    }

    private Integer calcFieldOffset() {
        Integer totalOffset = superClass.getFieldOffset();
        for (Map.Entry<String, String> field : fieldMap.entrySet()) {
            fieldOffsetMap.put(field.getKey(), totalOffset);
            totalOffset += SymbolTable.findFieldOffset(field.getValue());
        }
        return totalOffset;
    }

    private Integer calcMethodOffset()  {
        Integer totalOffset = superClass.getMethodOffset();
        for (Map.Entry<String,MethodContents> method: methodMap.entrySet()) {
            if(superClass.getInheritedMethod(method.getKey()) != null) {
                //this method is inherited, skip it
                continue;
            }
            methodOffsetMap.put(method.getKey(), totalOffset);
            totalOffset += 8;
        }
        return  totalOffset;
    }

    public void printOffsets() {
        System.out.println("----------------------Class "+type+"-------------------------------");
        printFieldOffset();

        printMethodOffset();
    }

    private void printMethodOffset() {
        System.out.println("---methods---");
        for(Map.Entry<String, Integer> method : methodOffsetMap.entrySet()) {
            System.out.println(type+"."+method.getKey()+" : "+method.getValue());
        }
    }

    private void printFieldOffset() {
        System.out.println("---fields---");
        for(Map.Entry<String, Integer> field : fieldOffsetMap.entrySet()) {
            System.out.println(type+"."+field.getKey()+" : "+field.getValue());
        }
    }

    public void saveOffsets() {
        calcFieldOffset();
        calcMethodOffset();
    }

    public Integer getFieldOffset(String fieldId) {
        return fieldOffsetMap.get(fieldId);
    }

    public Integer getMethodOffset(String methodId) { return methodOffsetMap.get(methodId); }

}