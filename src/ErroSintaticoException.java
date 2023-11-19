public class ErroSintaticoException extends Exception {
 
    private static final long serialVersionUID = -2346384470483785588L;
 
    public ErroSintaticoException() {
        super("Erro sintático!");
    }
 
    public ErroSintaticoException(String message) {
        super(message);
    }
 
    public ErroSintaticoException(Throwable t) {
        super(t);
    }
}
