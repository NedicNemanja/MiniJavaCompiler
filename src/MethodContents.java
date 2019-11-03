import java.util.*;

public class MethodContents {

    private String id;
    private ClassContents ownerClass;
    private String returnType;

    private List<String> parameterTypeList = new ArrayList<>();
    private Map<String, String> variableTypeMap = new HashMap<>();

    MethodContents(String id, ClassContents ownerClass, String returnType) {
        this.id = id;
        this.ownerClass = ownerClass;
        this.returnType = returnType;
    }

    public void insertVariable(String varId, String varType) throws PopulatorException {
        if (variableTypeMap.containsKey(varId)) {
            throw new PopulatorException("Variable with identifier \"" + varId + "\" already exists in " + id + "'s scope.");
        } else {
            variableTypeMap.put(varId, varType);
        }
    }

    public void insertParameter(String parId, String parType) throws PopulatorException {
        parameterTypeList.add(parType);
        this.insertVariable(parId, parType);
    }

    /**
     * if any super class in inheritance hierarchy has a method with the same name then this method has to have the same
     * return type and  parameter list
     *
     * @throws PopulatorException
     */
    public void polymorphicCheck() throws PopulatorException {
        MethodContents superMethod = ownerClass.getInheritedMethod(id);
        if (superMethod != null) {
            if (returnType != superMethod.getReturnType()) {
                throw new PopulatorException("Different return type for polymorphic method: " + ownerClass.getType() + "." + id + " inherited from " + ownerClass.getSuper().getType());
            }
            this.compareParameterLists(superMethod);
        }
    }

    public void compareParameterLists(MethodContents methodToCheck) throws PopulatorException {
        List<String> paramListToCheck = methodToCheck.getParameterList();
        //check list length
        if (parameterTypeList.size() != paramListToCheck.size()) {
            throw new PopulatorException("Paramters lists differ in length for " + this.getName() + " and " + methodToCheck.getName());
        }
        //check list types
        for (int i = 0; i < parameterTypeList.size(); i++) {
            if (parameterTypeList.get(i) != paramListToCheck.get(i)) {
                throw new PopulatorException("Parameter lists don't match.\n" + this.parameterListAsString() + "\n" + methodToCheck.parameterListAsString());
            }
        }
    }

    /**
     * Look for this variavle in variableTypeMap. If its there return its Type, else null.
     *
     * @param varId
     * @return
     */
    public String getVariableType(String varId) {
        if (variableTypeMap.containsKey(varId)) {
            return variableTypeMap.get(varId);
        } else {
            return null;
        }
    }

    /************  Getters  ***********************************/
    public String getId() {
        return id;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameterList() {
        return parameterTypeList;
    }

    /*************  For printing *************************************/
    public String getName() {
        return ownerClass.getType() + "." + id;
    }

    public String parameterListAsString() {
        String ret = id + "(";
        ListIterator it = parameterTypeList.listIterator();
        while (it.hasNext()) {
            if (it.hasPrevious()) {
                ret = ret + ",";
            }
            ret = ret + it;
        }
        return ret + ")";
    }


}
