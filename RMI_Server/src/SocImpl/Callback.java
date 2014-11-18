package SocImpl;

public class Callback {
	public String methodName;
	public Object[] arguments;

	public Callback() {
		this.methodName = null;
		this.arguments = null;
	}

	public Callback(String methodName, Object[] arguments) {
		this.methodName = methodName;
		this.arguments = arguments;
	}
}
