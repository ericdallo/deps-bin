/**
 * A test jar implementation useful for testing jvm options and
 * parameter passing of the jar wrapper script.
 */
public class TestJar
{
    /**
     * Prints out the system property values of each arg in ARGS on a
     * separate line.
     *
     * Line format is as follows. If there is no value for ARG, then
     * PROPERTY-VALUE is printed as NULL.
     *
     * :prop ARG :v PROPERTY-VALUE
     *
     * The program terminates with exit code 0, unless there is an arg
     * in ARGS of `testjar.return', in which case it exists with that
     * property's integer value.
     *
     */
    public static void main (String[] args)
    {
	int ret = 0;
	for (int i = 0; i < args.length; i++)
	    {
		String prop = args[i];
		String v = System.getProperty(args[i]);
		if (prop.equals("testjar.return"))
		    ret = Integer.parseInt(v);
		System.out.println(":prop " + prop + " :v " + v);
	    }
	System.exit(ret);
    }
}
