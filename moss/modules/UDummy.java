package moss.modules;


import java.util.*;
import moss.user.*;
import moss.fs.MFileOps;


public class UDummy implements MUserProcess{

	public int main (String argv[], MEnv envp)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "U DUMMY!\n");
		MPosixIf.exit (0);
		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "UHelloWorld signalled with " + signo + "!\n");
	}
	
}
