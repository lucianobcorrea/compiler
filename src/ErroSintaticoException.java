public class ErroSintaticoException extends Exception {
 
    private static final long serialVersionUID = -2346384470483785588L;
 
    public ErroSintaticoException() {
        super("Erro sint�tico!");
    }
 
    public ErroSintaticoException(String message) {
        super(message);
    }
 
    public ErroSintaticoException(Throwable t) {
        super(t);
    }
}
