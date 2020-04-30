package pac.test;

/** 
 * This is a test class used for testing post-method-call instruction insertion.
 * @author ppiselli
 *
 */
public class FooClass {
	
	public String foo(String string1, String string2) {
		String string3 = string1.concat(string2);
		return string3;
	}
	
	public String fooMethod() {
		return new String("foo");
	}
	
}
