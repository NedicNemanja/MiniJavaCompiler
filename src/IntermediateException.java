public class IntermediateException extends RuntimeException {
    private String msg;
    private int line;

    IntermediateException(String msg) {
        this.msg = msg;
        this.line = -1;
    }

    IntermediateException(String msg, int line) {
        this.msg = msg;
        this.line = line;
    }

    public String getMessage() {
        String ret = "Intermediate code generator exception: ";
        if(line != -1) {
            ret += " (at line "+line+")";
        }
        return ret+"\n\t"+msg;
    }
}


