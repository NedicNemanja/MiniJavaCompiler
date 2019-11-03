public class TypeCheckerException extends Exception {

    private String msg;
    private int line;

    TypeCheckerException(String msg, int line) {
        this.msg = msg;
        this.line = line;
    }

    public String getMessage() {
        if(line == -1) {
            return "TypeError: "+msg;
        }
        return "TypeError at line "+line+": "+msg;
    }
}
