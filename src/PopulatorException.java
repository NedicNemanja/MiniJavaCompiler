public class PopulatorException extends  Exception {
    private String msg;

    PopulatorException(String msg) {
        this.msg = msg;
    }

    public String getMessage() {
        return "PopulatorError"+": "+msg;
    }
}
