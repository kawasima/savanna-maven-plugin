package net.unit8.maven.plugins.smell;

public class TestSmell {
    private final SmellType type;
    private final String className;
    private final String methodName;
    private final int lineNumber;
    private final String message;
    private final boolean heuristic;

    public TestSmell(SmellType type, String className, String methodName, int lineNumber, String message) {
        this(type, className, methodName, lineNumber, message, false);
    }

    public TestSmell(SmellType type, String className, String methodName, int lineNumber, String message, boolean heuristic) {
        this.type = type;
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
        this.message = message;
        this.heuristic = heuristic;
    }

    public SmellType getType() {
        return type;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getMessage() {
        return message;
    }

    public boolean isHeuristic() {
        return heuristic;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getDisplayName());
        sb.append(" at line ").append(lineNumber);
        if (methodName != null) {
            sb.append(" in ").append(methodName).append("()");
        }
        sb.append(" - ").append(message);
        if (heuristic) {
            sb.append(" [heuristic]");
        }
        return sb.toString();
    }
}
